package shape.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.Query;
import preprocess.QueryGenerator;
import shape.Constraint;
import shape.ConstraintConjunction;
import shape.Schema;
import util.ImmutableCollectors;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConstraintConjunctionImpl implements ConstraintConjunction {

    private final String id;
    private final ImmutableList<Constraint> minConstraints;
    private final ImmutableList<Constraint> maxConstraints;
    private final String minQueryPredicate;
    private final ImmutableList<String> maxQueryPredicates;

    private Query minQuery;
    private ImmutableSet<Query> maxQueries;


    public ConstraintConjunctionImpl(String id, ImmutableList<Constraint> minConstraints, ImmutableList<Constraint> maxConstraints) {
        this.id = id;
        this.minConstraints = minConstraints;
        this.maxConstraints = maxConstraints;
        minQueryPredicate = id + "_pos";
        maxQueryPredicates = IntStream.range(1, maxConstraints.size()+1).boxed()
                .map(i -> id + "_max_" + i)
                .collect(ImmutableCollectors.toList());
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
    public Stream<String> getPredicates() {
            return Stream.of(
                    Stream.of(id, minQueryPredicate),
                    maxQueryPredicates.stream()
            ).flatMap(s -> s);
    }

//    @Override
//    public void computeRulePatterns(Schema s) {
//        minConstraints.forEach(Constraint::computeRulePatternBody);
//        maxConstraints.forEach(Constraint::computeRulePatternBody);
//
//        String focusNodeVar = VariableGenerator.getFocusNodeVar();
//        this.rulePattern = new RulePattern(
//                new Atom(id, focusNodeVar, true),
//                minConstraints.stream()
//                        .map(c -> c.getRulePatternBody().getHead())
//                        .collect(ImmutableCollectors.toSet()),
//                maxConstraints.stream()
//                        .map(c -> c.getRulePatternBody().getHead())
//                        .collect(ImmutableCollectors.toSet()),
//                ImmutableSet.of(focusNodeVar)
//        );
//    }

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

    public void computeQueries(Optional<String> graphName) {

        // Split constraints into min constraints and max constraints
//        Map<Boolean, List<Constraint>> part = constraints.stream()
//                .collect(Collectors.partitioningBy(
//                        c -> c.getMin().isPresent()
//                ));

        // Build a unique set of triples (+ filter) for all min constraints
        this.minQuery = QueryGenerator.generateQuery(this.minQueryPredicate, minConstraints, graphName, Optional.empty());

        // Build one set of triples (+ filter) for each max constraint
        AtomicInteger i = new AtomicInteger(0);
        this.maxQueries = maxConstraints.stream()
                .map(c -> QueryGenerator.generateQuery(
                        maxQueryPredicates.get(i.getAndIncrement()),
                        ImmutableList.of(c),
                        graphName,
                        getSubQuery(c.getMax().get(), minQuery)
                ))
                .collect(ImmutableCollectors.toSet());
    }

    private Optional<String> getSubQuery(Integer card, Query minQuery) {
        return card>0?
                Optional.of(minQuery.asSubQuery()):
                Optional.empty();
    }


//    @Override
//    public ImmutableSet<RulePattern> getRulePattern() {
//        return Stream.of(
//                Stream.of(this.rulePattern),
//                minConstraints.stream()
//                        .map(Constraint::getRulePatternBody),
//                maxConstraints.stream()
//                        .map(Constraint::getRulePatternBody)
//        ).flatMap(s -> s)
//                .collect(ImmutableCollectors.toSet());
//    }

//    private String generateQueryString(QueryGenerator validationClause, ImmutableList<QueryGenerator> violationClauses, Optional<String> graph) {
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

//    private String getViolationClausesSparql(ImmutableList<QueryGenerator> violationClauses) {
//        return violationClauses.stream()
//                .map(this::getViolationClauseSparql)
//                .collect(Collectors.joining("\n"));
//    }
//
//    private String getViolationClauseSparql(QueryGenerator c) {
//        return "\nOPTIONAL {" +
//                "\n" +
//                c.getSparql()
//                + "}";
//    }

//    private ImmutableMap<String, Atom> generateRule(QueryGenerator validationClause) {
//        return validationClause.ruleBody.stream()
//                .collect(ImmutableCollectors.toMap(
//                        Atom::getArg,
//                        a -> a
//                ));
//    }

//    private ImmutableList<ImmutableMap<String, Atom>> generateViolationRules(ImmutableList<QueryGenerator> violationClauses) {
//        return violationClauses.stream()
//                .map(this::generateRule)
//                .collect(ImmutableCollectors.toList());
//    }



//    private Map<String, Atom> createRule(ImmutableList<String> variables, Shape shape, boolean isPos) {
//        return variables.stream()
//                .collect(ImmutableCollectors.toMap(
//                        v -> v,
//                        v -> new Atom(shape.getId(), v, isPos)
//                ));
//    }
}

