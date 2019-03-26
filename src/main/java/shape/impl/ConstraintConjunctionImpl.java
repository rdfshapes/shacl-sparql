package shape.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import core.Atom;
import core.Query;
import core.RulePattern;
import core.global.SPARQLPrefixHandler;
import core.global.VariableGenerator;
import shape.Constraint;
import shape.ConstraintConjunction;
import shape.Schema;
import shape.Shape;
import util.ImmutableCollectors;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConstraintConjunctionImpl implements ConstraintConjunction {

    private final String id;
    private final ImmutableSet<Constraint> minConstraints;
    private final ImmutableSet<Constraint> maxConstraints;

//    private RulePattern rulePattern;
    //    private ImmutableSet<String> localViolationVars;
//    private ImmutableSet<String> distantViolationVars;
//    private ImmutableMap<String, Atom> validationRule;
//    private ImmutableList<ImmutableMap<String, Atom>> violationRules;
    private Query minQuery;


    public ConstraintConjunctionImpl(String id, ImmutableSet<Constraint> minConstraints, ImmutableSet<Constraint> maxConstraints) {
        this.id = id;
        this.minConstraints = minConstraints;
        this.maxConstraints = maxConstraints;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Query getMinQuery() {
        return minQuery;
    }

    @Override
    public void computeRulePatterns(Schema s) {
        minConstraints.forEach(Constraint::computeRulePattern);
        maxConstraints.forEach(Constraint::computeRulePattern);

        String focusNodeVar = VariableGenerator.getFocusNodeVar();
        this.rulePattern = new RulePattern(
                new Atom(id, focusNodeVar, true),
                minConstraints.stream()
                        .map(c -> c.getRulePattern().getHead())
                        .collect(ImmutableCollectors.toSet()),
                maxConstraints.stream()
                        .map(c -> c.getRulePattern().getHead())
                        .collect(ImmutableCollectors.toSet()),
                ImmutableSet.of(focusNodeVar)
        );
    }

//    @Override
//    public ImmutableSet<String> getLocalViolationVars() {
//        return localViolationVars;
//    }
//
//    @Override
//    public ImmutableSet<String> getDistantViolationVars() {
//        return distantViolationVars;
//    }
//
//    @Override
//    public ImmutableMap<String, Atom> getValidationRulePattern() {
//        return validationRule;
//    }
//
//    @Override
//    public ImmutableList<ImmutableMap<String, Atom>> getViolationRulePatterns() {
//        return violationRules;
//    }

    public void computeQueries(Schema schema, Optional<String> graphName) {

        // Split constraints into min constraints and max constraints
//        Map<Boolean, List<Constraint>> part = constraints.stream()
//                .collect(Collectors.partitioningBy(
//                        c -> c.getMin().isPresent()
//                ));

        // Build a unique set of triples (+ filter) for all min constraints
        ClauseBuilder validationClause = buildClause(minConstraints, schema);

        // Build one set of triples (+ filter) for each max constraint
        ImmutableList<ClauseBuilder> violationClauses = maxConstraints.stream()
                .map(c -> buildClause(ImmutableSet.of(c), schema))
                .collect(ImmutableCollectors.toList());

        this.minQuery = generateQueryString(validationClause, violationClauses, graphName);
        return minQuery;
    }


    @Override
    public ImmutableSet<RulePattern> getRulePatterns() {
        return Stream.of(
                Stream.of(this.rulePattern),
                minConstraints.stream()
                        .map(Constraint::getRulePattern),
                maxConstraints.stream()
                        .map(Constraint::getRulePattern)
        ).flatMap(s -> s)
                .collect(ImmutableCollectors.toSet());
    }

    private String generateQueryString(ClauseBuilder validationClause, ImmutableList<ClauseBuilder> violationClauses, Optional<String> graph) {
        return SPARQLPrefixHandler.getPrexixString()+
                "SELECT * WHERE{" +
                (graph.isPresent() ?
                        "\nGRAPH " + graph.get() + "{" :
                        ""
                ) +
                "\n\n" +
                validationClause.getSparql() +
                "\n" +
                getViolationClausesSparql(violationClauses) +
                (graph.isPresent() ?
                        "\n}" :
                        ""
                ) +
                "\n}";
    }

    private String getViolationClausesSparql(ImmutableList<ClauseBuilder> violationClauses) {
        return violationClauses.stream()
                .map(this::getViolationClauseSparql)
                .collect(Collectors.joining("\n"));
    }

    private String getViolationClauseSparql(ClauseBuilder c) {
        return "\nOPTIONAL {" +
                "\n" +
                c.getSparql()
                + "}";
    }

    private ImmutableMap<String, Atom> generateRule(ClauseBuilder validationClause) {
        return validationClause.ruleBody.stream()
                .collect(ImmutableCollectors.toMap(
                        Atom::getArg,
                        a -> a
                ));
    }

    private ImmutableList<ImmutableMap<String, Atom>> generateViolationRules(ImmutableList<ClauseBuilder> violationClauses) {
        return violationClauses.stream()
                .map(this::generateRule)
                .collect(ImmutableCollectors.toList());
    }

    private ClauseBuilder buildClause(ImmutableSet<Constraint> constraints, Schema schema) {
        ClauseBuilder builder = new ClauseBuilder();
        constraints.forEach(c -> buildClause(builder, c, schema));
        return builder;
    }

    private void buildClause(ClauseBuilder builder, Constraint c, Schema schema) {

        if (c.getValue().isPresent()) {
            builder.addTriple(c.getPath(), c.getValue().get());
            return;
        }

        ImmutableSet<String> variables = c.getVariables();
        variables.forEach(v -> builder.addTriple(c.getPath(), "?"+v));

        if (c.getDatatype().isPresent()) {
            variables.forEach(v -> builder.addDatatypeFilter(v, c.getDatatype().get(), c.isPos()));
        }
        if (c.getShapeRef().isPresent()) {
            variables.forEach(v -> builder.addRuleAtom(v, c.getShapeRef().get(), c.isPos(), schema));
        }
        if (variables.size() > 1) {
            builder.addCardinalityFilter(variables);
        }
    }


    private Map<String, Atom> createRule(ImmutableList<String> variables, Shape shape, boolean isPos) {
        return variables.stream()
                .collect(ImmutableCollectors.toMap(
                        v -> v,
                        v -> new Atom(shape.getName(), v, isPos)
                ));
    }

    // mutable
    private class ClauseBuilder {
        Set<Atom> ruleBody;
        List<String> filters;
        List<String> triples;
        Set<String> variables;

        public ClauseBuilder() {
            this.ruleBody = new HashSet<>();
            this.filters = new ArrayList<>();
            this.triples = new ArrayList<>();
        }

        void addTriple(String path, String object) {
            triples.add(
                    "?"+VariableGenerator.getFocusNodeVar() + " " +
                            path + " " +
                            object + "."
            );
        }

        void addVariables(ImmutableSet<String> variables) {
            this.variables.addAll(variables);
        }

        void addDatatypeFilter(String variable, String datatype, Boolean isPos) {
            String s = getDatatypeFilter(variable, datatype);
            filters.add(
                    (isPos) ?
                            s :
                            "!(" + s + ")"
            );
        }

        private String getDatatypeFilter(String variable, String datatype) {
            return "datatype(?"+ variable + ") = " + datatype;
        }

        void addRuleAtom(String v, String s, boolean idPos, Schema schema) {
            ruleBody.add(new Atom(s, v, idPos));
        }

        String getSparql() {

            String tripleString = triples.stream()
                    .collect(Collectors.joining("\n"));

            if (filters.isEmpty()) {
                return tripleString;
            }
            return tripleString +
                    generateFilterString();
        }

        private String generateFilterString() {
            if (filters.isEmpty()) {
                return "";
            }
            return "\nFILTER(\n" +
                    (filters.size() == 1 ?
                            filters.iterator().next() :
                            filters.stream()
                                    .collect(Collectors.joining(" AND\n"))
                    )
                    + ")";
        }

        void addCardinalityFilter(ImmutableSet<String> variables) {
            ImmutableList<String> list = ImmutableList.copyOf(variables);
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    filters.add("?"+list.get(i) + " != ?" + list.get(j));
                }
            }
        }
    }
}

