package shape.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import core.Atom;
import core.Query;
import core.RulePattern;
import core.global.VariableGenerator;
import preprocess.SPARQLGenerator;
import shape.Constraint;
import shape.ConstraintConjunction;
import shape.Schema;
import shape.Shape;
import util.ImmutableCollectors;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private ImmutableSet<Query> maxQueries;


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
    public ImmutableSet<Query> getMaxQueries() {
        return maxQueries;
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
        this.minQuery = SPARQLGenerator.generateQuery(this.id + "_pos", minConstraints, schema, graphName);

        // Build one set of triples (+ filter) for each max constraint
        AtomicInteger i = new AtomicInteger(0);
        this.maxQueries = maxConstraints.stream()
                .map(c -> SPARQLGenerator.generateQuery(
                        this.id + "_max_" + i.incrementAndGet(),
                        ImmutableSet.of(c),
                        schema,
                        graphName
                ))
                .collect(ImmutableCollectors.toSet());
    }



//    @Override
//    public ImmutableSet<RulePattern> getRulePatterns() {
//        return Stream.of(
//                Stream.of(this.rulePattern),
//                minConstraints.stream()
//                        .map(Constraint::getRulePattern),
//                maxConstraints.stream()
//                        .map(Constraint::getRulePattern)
//        ).flatMap(s -> s)
//                .collect(ImmutableCollectors.toSet());
//    }

//    private String generateQueryString(SPARQLGenerator validationClause, ImmutableList<SPARQLGenerator> violationClauses, Optional<String> graph) {
//        return SPARQLPrefixHandler.getPrexixString() +
//                "SELECT * WHERE{" +
//                (graph.isPresent() ?
//                        "\nGRAPH " + graph.get() + "{" :
//                        ""
//                ) +
//                "\n\n" +
//                validationClause.getSparql() +
//                "\n" +
//                getViolationClausesSparql(violationClauses) +
//                (graph.isPresent() ?
//                        "\n}" :
//                        ""
//                ) +
//                "\n}";
//    }

//    private String getViolationClausesSparql(ImmutableList<SPARQLGenerator> violationClauses) {
//        return violationClauses.stream()
//                .map(this::getViolationClauseSparql)
//                .collect(Collectors.joining("\n"));
//    }
//
//    private String getViolationClauseSparql(SPARQLGenerator c) {
//        return "\nOPTIONAL {" +
//                "\n" +
//                c.getSparql()
//                + "}";
//    }

//    private ImmutableMap<String, Atom> generateRule(SPARQLGenerator validationClause) {
//        return validationClause.ruleBody.stream()
//                .collect(ImmutableCollectors.toMap(
//                        Atom::getArg,
//                        a -> a
//                ));
//    }

//    private ImmutableList<ImmutableMap<String, Atom>> generateViolationRules(ImmutableList<SPARQLGenerator> violationClauses) {
//        return violationClauses.stream()
//                .map(this::generateRule)
//                .collect(ImmutableCollectors.toList());
//    }



//    private Map<String, Atom> createRule(ImmutableList<String> variables, Shape shape, boolean isPos) {
//        return variables.stream()
//                .collect(ImmutableCollectors.toMap(
//                        v -> v,
//                        v -> new Atom(shape.getName(), v, isPos)
//                ));
//    }

}

