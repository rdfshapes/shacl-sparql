package shape.impl;

import com.google.common.collect.ImmutableSet;
import core.Atom;
import core.RulePattern;
import core.global.VariableGenerator;
import shape.ConstraintConjunction;
import shape.Schema;
import shape.Shape;
import util.ImmutableCollectors;

import java.util.Optional;
import java.util.stream.Stream;

public class ShapeImpl implements Shape {

    private final String id;
    private final Optional<String> targetQuery;
    private final ImmutableSet<ConstraintConjunction> disjuncts;
    private ImmutableSet<RulePattern> rulePatterns;
    private ImmutableSet<String> predicates;

    public ShapeImpl(String id, Optional<String> targetQuery, ImmutableSet<ConstraintConjunction> disjuncts) {
        this.id = id;
        this.targetQuery = targetQuery;
        this.disjuncts = disjuncts;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getTargetQuery() {
        return targetQuery;
    }

    @Override
    public ImmutableSet<ConstraintConjunction> getDisjuncts() {
        return disjuncts;
    }

    @Override
    public ImmutableSet<RulePattern> getRulePatterns() {
        return rulePatterns;
    }

    @Override
    public void computeConstraintQueries(Schema schema, Optional<String> graph) {
        disjuncts.forEach(c -> c.computeQueries(graph));
        this.rulePatterns = computeRulePatterns();
    }

    @Override
    public ImmutableSet<String> getPredicates() {
        return predicates;
    }

    @Override
    public ImmutableSet<String> computePredicateSet() {
        this.predicates = Stream.concat(
                Stream.of(id),
                disjuncts.stream()
                        .flatMap(d -> d.getPredicates())
        ).collect(ImmutableCollectors.toSet());
        return predicates;
    }

    private ImmutableSet<RulePattern> computeRulePatterns() {
        String focusNodeVar = VariableGenerator.getFocusNodeVar();
        Atom head = new Atom(id, focusNodeVar, true);
        return disjuncts.stream()
                .map(d -> new RulePattern(
                        head,
                        getDisjunctRPBody(d)
                ))
                .collect(ImmutableCollectors.toSet());
    }

    private ImmutableSet<Atom> getDisjunctRPBody(ConstraintConjunction d) {
        String focusNodeVar = VariableGenerator.getFocusNodeVar();
        return Stream.concat(
                Stream.of(
                        new Atom(
                                d.getMinQuery().getId(),
                                focusNodeVar,
                                true
                        )),
                d.getMaxQueries().stream()
                        .map(q -> q.getId())
                        .map(s -> new Atom(
                                s,
                                focusNodeVar,
                                false
                        ))
        ).collect(ImmutableCollectors.toSet());
    }
}
