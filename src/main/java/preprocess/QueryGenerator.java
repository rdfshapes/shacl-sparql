package preprocess;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import core.Atom;
import core.Query;
import core.RulePattern;
import core.global.SPARQLPrefixHandler;
import core.global.VariableGenerator;
import shape.Constraint;
import util.ImmutableCollectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryGenerator {

    public static Query generateQuery(String id, ImmutableList<Constraint> constraints, Optional<String> graph, Optional<String> subquery) {
        if(constraints.size() > 1 && constraints
                .stream().anyMatch(c -> c.getMax().isPresent())){
            throw new RuntimeException("Only one max constraint per query is allowed");
        }
        RulePattern rp = computeRulePattern(constraints, id);

        QueryBuilder builder = new QueryBuilder(id, graph, subquery, rp.getVariables());
        constraints.forEach(c -> builder.buildClause(c));

        return builder.buildQuery(rp);
    }

    private static RulePattern computeRulePattern(ImmutableList<Constraint> constraints, String id) {
        UnmodifiableIterator<Constraint> it = constraints.iterator();
        return new RulePattern(
                new Atom(
                        id,
                        VariableGenerator.getFocusNodeVar(),
                        !(it.hasNext() && it.next().getMax().isPresent())
                ),
                constraints.stream()
                .flatMap(c -> c.computeRulePatternBody().stream())
                .collect(ImmutableCollectors.toSet())
        );
    }

    public static Optional<String> generateLocalSubquery(Optional<String> graphName, ImmutableList<Constraint> posConstraints) {

        ImmutableList<Constraint> localPosConstraints = posConstraints.stream()
            .filter(c -> !c.getShapeRef().isPresent())
            .collect(ImmutableCollectors.toList());

        if(localPosConstraints.isEmpty()){
            return Optional.empty();
        }
        QueryBuilder builder = new QueryBuilder(
                "tmp",
                graphName,
                Optional.empty(),
                ImmutableSet.of(VariableGenerator.getFocusNodeVar())
        );
        localPosConstraints.forEach(c -> builder.buildClause(c));
        return Optional.of(builder.getSparql(false));
    }

    // mutable
    private static class QueryBuilder {
        List<String> filters;
        List<String> triples;
        Set<String> variables;
        private final String id;
        private final Optional<String> subQuery;
        private final Optional<String> graph;
        private final ImmutableSet<String> projectedVariables;

        public QueryBuilder(String id, Optional<String> graph, Optional<String> subquery, ImmutableSet<String> projectedVariables) {
            this.id = id;
            this.graph = graph;
            this.projectedVariables = projectedVariables;
            this.filters = new ArrayList<>();
            this.triples = new ArrayList<>();
            subQuery = subquery;
        }

        void addTriple(String path, String object) {
            triples.add(
                    "?" + VariableGenerator.getFocusNodeVar() + " " +
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
            return "datatype(?" + variable + ") = " + datatype;
        }

//        void addRuleAtom(String v, String s, boolean idPos, Schema schema) {
//            ruleBody.add(new Atom(s, v, idPos));
//        }

        String getSparql(boolean includePrefixes) {
            return (includePrefixes?
                    SPARQLPrefixHandler.getPrexixString():
                    "")+
                    getProjectionString()+
                    " WHERE{" +
                    (graph.map(s -> "\nGRAPH " + s + "{").orElse("")
                    ) +
                    "\n\n" +
                    getTriplePatterns() +
                    "\n" +
                    (subQuery.isPresent() ?
                            "{\n"+subQuery.get()+"\n}\n" :
                            ""
                    ) +
                    (graph.isPresent() ?
                            "\n}" :
                            ""
                    ) +
                    "\n}";
        }

        private String getProjectionString() {
            return "SELECT "+
                    projectedVariables.stream()
                    .map(v -> "?"+v)
                    .collect(Collectors.joining(", "));
        }

        String getTriplePatterns() {
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
                    filters.add("?" + list.get(i) + " != ?" + list.get(j));
                }
            }
        }

        private void buildClause(Constraint c) {

            if (c.getValue().isPresent()) {
                addTriple(c.getPath(), c.getValue().get());
                return;
            }

            ImmutableSet<String> variables = c.getVariables();
            variables.forEach(v -> addTriple(c.getPath(), "?" + v));

            if (c.getDatatype().isPresent()) {
                variables.forEach(v -> addDatatypeFilter(v, c.getDatatype().get(), c.isPos()));
            }

            if (variables.size() > 1) {
                addCardinalityFilter(variables);
            }
        }

        Query buildQuery(RulePattern rulePattern) {
            return new Query(
                    id,
                    rulePattern,
                    getSparql(true)
            );
        }
    }
}

