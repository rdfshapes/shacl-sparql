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

public class ShapeImpl implements Shape {

    private final String name;
    private final Optional<String> targetQuery;
    private final ImmutableSet<ConstraintConjunction> disjuncts;
    private final ImmutableSet<RulePattern> rulePatterns;
//    private final RulePattern violationPattern;

    public ShapeImpl(String name, Optional<String> targetQuery, ImmutableSet<ConstraintConjunction> disjuncts) {
        this.name = name;
        this.targetQuery = targetQuery;
        this.disjuncts = disjuncts;
//        violationPattern = computeViolationPattern();
        rulePatterns = computeRulePatterns();
    }

    private ImmutableSet<RulePattern> computeRulePatterns() {
        String focusNodeVar = VariableGenerator.getFocusNodeVar();
        Atom head = new Atom(name, focusNodeVar, true);
        return disjuncts.stream()
                .map(ConstraintConjunction::getId)
                .map(p -> new Atom(p, focusNodeVar, true))
                .map(a -> new RulePattern(
                        head,
                        ImmutableSet.of(a),
                        ImmutableSet.of(a.getNegation()),
//                        disjuncts.stream()
//                                .map(ConstraintConjunction::getId)
//                                .map(p -> new Atom(p, focusNodeVar, false))
//                                .collect(ImmutableCollectors.toSet()),
                        ImmutableSet.of(focusNodeVar)))
                .collect(ImmutableCollectors.toSet());
    }

//    private RulePattern computeViolationPattern() {
//        String focusNodeVar = VariableGenerator.getFocusNodeVar();
//               return new RulePattern(
//                       new Atom(name, focusNodeVar, false),
//                       ImmutableSet.of(),
//                        disjuncts.stream()
//                                .map(ConstraintConjunction::getId)
//                                .map(p -> new Atom(p, focusNodeVar, false))
//                                .collect(ImmutableCollectors.toSet()),
//                       ImmutableSet.of(focusNodeVar)
//                );
//    }

    public String getName() {
        return name;
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
        disjuncts.forEach(c -> c.computeQueries(schema, graph));
    }
}
