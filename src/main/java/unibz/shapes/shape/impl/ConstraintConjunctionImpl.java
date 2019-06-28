package unibz.shapes.shape.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.Query;
import unibz.shapes.shape.preprocess.QueryGenerator;
import unibz.shapes.shape.ConstraintConjunction;
import unibz.shapes.shape.LocalConstraint;
import unibz.shapes.shape.MaxOnlyConstraint;
import unibz.shapes.shape.MinOnlyConstraint;
import util.ImmutableCollectors;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConstraintConjunctionImpl implements ConstraintConjunction {

    private final String id;
    private final ImmutableList<MinOnlyConstraint> minConstraints;
    private final ImmutableList<MaxOnlyConstraint> maxConstraints;
    private final String minQueryPredicate;
    private final ImmutableList<LocalConstraint> localConstraints;
    private final ImmutableList<String> maxQueryPredicates;

    private Query minQuery;
    private ImmutableSet<Query> maxQueries;


    public ConstraintConjunctionImpl(String id, ImmutableList<MinOnlyConstraint> minConstraints,
                                     ImmutableList<MaxOnlyConstraint> maxConstraints, ImmutableList<LocalConstraint> localConstraints) {
        this.id = id;
        this.minConstraints = minConstraints;
        this.maxConstraints = maxConstraints;
        this.localConstraints = localConstraints;
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

    public void computeQueries(Optional<String> graphName) {

        // Create a subquery for all local (i.e. without shape propagation) and positive constraints
        // Every other query for this conjunct will contain this as a subquery.
        // This is unnecessary in theory, but does not compromise soundness, and makes queries more selective.
        Optional<String> subquery = QueryGenerator.generateLocalSubquery(
                graphName,
                Stream.concat(
                        minConstraints.stream(),
                        localConstraints.stream()
                ).collect(ImmutableCollectors.toList())
        );

        // Build a unique set of triples (+ filter) for all min constraints (note that the local ones are handled by the subquery)
        this.minQuery = QueryGenerator.generateQuery(
                this.minQueryPredicate,
                minConstraints.stream()
                        .filter(c -> c.getShapeRef().isPresent())
                        .collect(ImmutableCollectors.toList()),
                graphName,
                subquery
        );

        // Build one set of triples (+ filter) for each max constraint
        AtomicInteger i = new AtomicInteger(0);
        this.maxQueries = maxConstraints.stream()
                .map(c -> QueryGenerator.generateQuery(
                        maxQueryPredicates.get(i.getAndIncrement()),
                        ImmutableList.of(c),
                        graphName,
                        subquery
                ))
                .collect(ImmutableCollectors.toSet());
    }
}

