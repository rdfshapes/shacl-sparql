package preprocess;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.Atom;
import core.Query;
import core.RulePattern;
import core.global.SPARQLPrefixHandler;
import core.global.VariableGenerator;
import shape.Constraint;
import shape.Schema;

import java.util.*;
import java.util.stream.Collectors;

public class SPARQLGenerator {

    public static Query generateQuery(String id, ImmutableSet<Constraint> constraints, Schema schema, Optional<String> graph) {
        QueryBuilder builder = new QueryBuilder(id, graph);
        constraints.forEach(c -> buildClause(builder, c, schema));
        return builder.buildQuery();
    }

    private static void buildClause(QueryBuilder builder, Constraint c, Schema schema) {

        if (c.getValue().isPresent()) {
            builder.addTriple(c.getPath(), c.getValue().get());
            return;
        }

        ImmutableSet<String> variables = c.getVariables();
        variables.forEach(v -> builder.addTriple(c.getPath(), "?" + v));

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


    private static class QueryBuilder {
        Set<Atom> ruleBody;
        List<String> filters;
        List<String> triples;
        Set<String> variables;
        Set<Constraint> constraints;
        private final String id;
        private final Optional<String> graph;

        public QueryBuilder(String id, Optional<String> graph) {
            this.id = id;
            this.graph = graph;
            this.ruleBody = new HashSet<>();
            this.filters = new ArrayList<>();
            this.triples = new ArrayList<>();
        }

        // mutable
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

        void addRuleAtom(String v, String s, boolean idPos, Schema schema) {
            ruleBody.add(new Atom(s, v, idPos));
        }

        String getSparql() {
            return SPARQLPrefixHandler.getPrexixString() +
                    "SELECT * WHERE{" +
                    (graph.isPresent() ?
                            "\nGRAPH " + graph.get() + "{" :
                            ""
                    ) +
                    "\n\n" +
                    getTriplePatterns() +
                    "\n" +
                    (graph.isPresent() ?
                            "\n}" :
                            ""
                    ) +
                    "\n}";
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

        public ImmutableSet<RulePattern> computeRulePatterns() {

        }

        public Query buildQuery() {
            return new Query(
                    id,
                    computeRulePatterns(),
                    getSparql()
            );
        }
    }
}

