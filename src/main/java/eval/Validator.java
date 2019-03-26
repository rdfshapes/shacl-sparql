package eval;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.Atom;
import core.Query;
import core.RulePattern;
import core.global.RuleMap;
import endpoint.QueryEvaluation;
import endpoint.SPARQLEndpoint;
import org.eclipse.rdf4j.query.BindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shape.ConstraintConjunction;
import shape.Schema;
import shape.Shape;
import util.ImmutableCollectors;

import java.util.*;
import java.util.stream.Collectors;

class Validator {

    static Logger log = LoggerFactory.getLogger(Validator.class);

    private final SPARQLEndpoint endpoint;
    private final Schema schema;
    private final Optional<Shape> targetShape;


    Validator(SPARQLEndpoint endpoint, Schema schema) {
        this.endpoint = endpoint;
        this.schema = schema;
        targetShape = Optional.empty();
    }

    public void validate() {
        validate(
                0,
                new EvalState(
                        extractTargetAtoms(),
                        new RuleMap(),
                        new HashSet<>(),
                        new HashSet<>()
                ),
                extractInitialFocusShapes()
        );
    }

    private ImmutableList<Atom> extractTargetAtoms() {
        if (targetShape.isPresent()) {
            return extractTargetAtoms(targetShape.get());
        }
        return schema.getShapes().stream()
                .filter(s -> s.getTargetQuery().isPresent())
                .flatMap(s -> extractTargetAtoms(s).stream())
                .collect(ImmutableCollectors.toList());
    }

    private ImmutableList<Atom> extractTargetAtoms(Shape shape) {
        return endpoint.runQuery(
                shape.getName(),
                shape.getTargetQuery().get()
        ).getBindingSets().stream()
                .map(b -> b.getBinding("x").getValue().stringValue())
                .map(i -> new Atom(shape.getName(), i, true))
                .collect(ImmutableCollectors.toList());
    }

    private ImmutableSet<Shape> extractInitialFocusShapes() {
        if (targetShape.isPresent()) {
            return ImmutableSet.of(targetShape.get());
        }
        return schema.getShapes().stream()
                .filter(s -> s.getTargetQuery().isPresent())
                .collect(ImmutableCollectors.toSet());
    }

    private void validate(int depth, EvalState state, ImmutableSet<Shape> focusShapes) {

        // termination condition 1: all shapes have been visited
        if (state.visitedShapes.size() == schema.getShapes().size()) {
            return;
        }
        // termination condition 2: all targets are validated/violated
        if (state.remainingTargetAtoms.isEmpty()) {
            return;
        }

        validateFocusShapes(state, focusShapes);

        // Set<Atom> assignment = state.assignment;

        printLog(depth, state);


        validate(depth + 1, state, updateFocusShapes(state));
    }

    private ImmutableSet<Shape> updateFocusShapes(EvalState state) {
        return state.ruleMap.getAllBodyAtoms()
                .map(a -> schema.getShape(a.getPredicate()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(s -> !state.visitedShapes.contains(s.getName()))
                .collect(ImmutableCollectors.toSet());
    }

    private void printLog(int depth, EvalState assignment) {
    }

    private Set<Atom> saturate(EvalState state, Set<Atom> inferredAtoms) {

        negateUnMatchableHeads(state);
//        filterRules(state);
        List<Atom> freshAtoms = applyRules(state);
        if (freshAtoms.isEmpty()) {
            return inferredAtoms;
        }
        state.assignment.addAll(freshAtoms);
        freshAtoms.forEach(a -> state.ruleMap.remove(a));
        inferredAtoms.addAll(freshAtoms);
        return saturate(state, inferredAtoms);
    }

//    private void filterRules(EvalState state) {
//        boolean modified = discardViolatedRules(state);
//        if (modified) {
//            filterRules(state);
//        }
//    }
//
//    private boolean discardViolatedRules(EvalState state) {
//        boolean modified = false;
//        RuleMap updatedRuleMap = new RuleMap();
//        for (Map.Entry<Atom, Set<ImmutableSet<Atom>>> e : state.ruleMap.entrySet()) {
//            if (discardRuleBodies(e.getKey(), e.getValue(), state, updatedRuleMap)) {
//                modified = true;
//            }
//        }
//        state.ruleMap = updatedRuleMap;
//        return modified;
//    }
//
//    private boolean discardRuleBodies(Atom head, Set<RuleBody> ruleBodies, EvalState state, RuleMap updatedRuleMap) {
//        Set<RuleBody> remainingRuleBodies = ruleBodies.stream()
//                .filter(s -> s.getNegatedAtoms().isEmpty()
//                        || s.getNegatedAtoms().stream()
//                        .noneMatch(a -> state.assignment.contains(a)))
//                .collect(ImmutableCollectors.toSet());
//
//        if (!remainingRuleBodies.isEmpty()) {
//            updatedRuleMap.addRuleSet(head, remainingRuleBodies);
//        }
//        if (remainingRuleBodies.size() == ruleBodies.size()) {
//            return false;
//        }
//        return true;
//    }

//    private Optional<RuleBody> discardRuleBodies(Atom head, Set<RuleBody> ruleBodies, Assignment assignment){
//        return ruleBodies.stream()
//                .allMatch(b -> assignment.getAtoms().contains(b.getNegatedAtoms())?
//                        Optional.of(head),
//
//
//        return ruleBodies.stream()
//                .map(b -> applyRuleBody(head, b, assignment))
//                .filter(o -> o.isPresent())
//                .map(o -> o.get());
//    }


    private List<Atom> applyRules(EvalState state) {
        RuleMap retainedRules = new RuleMap();
        return state.ruleMap.entrySet().stream()
                .map(e -> applyRules(e.getKey(), e.getValue(), state, retainedRules))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableCollectors.toList());
    }

    private Optional<Atom> applyRules(Atom head, Set<ImmutableSet<Atom>> bodies, EvalState state, RuleMap retainedRules) {
        if(bodies.stream()
                .anyMatch(b -> applyRule(head, b, state, retainedRules))){
            state.assignment.add(head);
            return Optional.of(head);
        }
        return Optional.empty();
    }

    private boolean applyRule(Atom head, ImmutableSet<Atom> body, EvalState state, RuleMap retainedRules) {
        if(state.assignment.containsAll(body)){
            return true;
        }
        if(body.stream()
                .noneMatch(a -> state.assignment.contains(a.getNegation()))){
            retainedRules.addRule(head, body);
        }
        return false;
    }


//    private void applyRuleBodies(Atom head, Set<RuleBody> rules, Assignment assignment, Set<Atom> freshAtoms) {
//        Set<RuleBody> remainingRules = rules.stream()
//                //.filter(r -> isMatchable(r, assignment))
//                .filter(r -> !applyRulebodies(head, r, freshAtoms, assignment))
//                .collect(Collectors.toSet());
//        if(remainingRules.isEmpty()){
//            freshAtoms.add(head.getNegation());
//        }else {
//            filteredRuleMap.addRuleSet(head, remainingRules);
//        }
//    }

    private void validateFocusShapes(EvalState state, ImmutableSet<Shape> focusShapes) {
        focusShapes.forEach(s -> evalShape(state, s));
    }

    private void evalShape(EvalState state, Shape s) {
        s.getDisjuncts().forEach(d -> evalDisjunct(state, d, s));
        s.getDisjuncts().forEach(d -> state.visitedShapes.add(d.getId()));
        state.visitedShapes.add(s.getName());

        Set<Atom> freshAtoms = saturate(state, new HashSet<>());

        // partition target atoms into atoms that have just been validated/violated, and remaining ones
        Map<Boolean, List<Atom>> part = state.remainingTargetAtoms.stream()
                .collect(Collectors.partitioningBy(a -> freshAtoms.contains(a) || freshAtoms.contains(a.getNegation())));

        state.remainingTargetAtoms = ImmutableList.copyOf(part.get(false));

    }


    private void evalDisjunct(EvalState state, ConstraintConjunction d, Shape s) {
//        ImmutableSet<RulePattern> rulepatterns =
//                Stream.of(
//                        s.getRulePattern().stream(),
//                        d.getRulePattern().stream()
//                ).flatMap(st -> st)
//                        .collect(ImmutableCollectors.toSet());

        evalQuery(state, d.getMinQuery(), s);
        d.getMaxQueries()
                .forEach(q -> evalQuery(state, q, s));

    }

    private void evalQuery(EvalState state, Query q, Shape s) {
        QueryEvaluation eval = endpoint.runQuery(q.getId(), q.getSparql());
        eval.getBindingSets().forEach(
                b -> evalBindingSet(state, b, q.getRulePattern(), s.getRulePatterns())
        );
    }

    private void evalBindingSet(EvalState state, BindingSet bs, RulePattern queryRP, ImmutableSet<RulePattern> shapeRPs) {
        evalBindingSet(state, bs, queryRP);
        shapeRPs.forEach(p -> evalBindingSet(state, bs, p));

    }

    private void evalBindingSet(EvalState state, BindingSet bs, RulePattern pattern) {
        Set<String> bindingVars = bs.getBindingNames();
        if (bindingVars.containsAll(pattern.getVariables())) {
            state.ruleMap.addRule(
                    pattern.instantiateAtom(pattern.getHead(), bs),
                    pattern.instantiateBody(bs)
            );
        }
    }

    private void negateUnMatchableHeads(EvalState state) {
        Set<Atom> ruleHeads = state.ruleMap.keySet();

        ImmutableSet<Atom> unmatchableAtoms = state.ruleMap.getAllBodyAtoms()
                .filter(a -> state.visitedShapes.contains(a.getPredicate()))
                .filter(a -> !ruleHeads.contains(a))
                .collect(ImmutableCollectors.toSet());

        //state.ruleMap = dropUnmatchableNegatedAtoms(state, unmatchableAtoms);
        unmatchableAtoms.forEach(a -> state.assignment.add(a.getNegation()));

    }

//    private RuleMap dropUnmatchableNegatedAtoms(EvalState state, ImmutableSet<Atom> unmatchableAtoms) {
//        RuleMap ruleMap = new RuleMap();
//        for (Map.Entry<Atom, Set<RuleBody>> e : state.ruleMap.entrySet()) {
//            Set<RuleBody> updateRuleBodies = e.getValue().stream()
//                    .map(rb -> dropUnmatchableNegatedAtoms(rb, unmatchableAtoms))
//                    .collect(ImmutableCollectors.toSet());
//            ruleMap.addRuleSet(e.getKey(), updateRuleBodies);
//        }
//        return ruleMap;
//    }

//    private RuleBody dropUnmatchableNegatedAtoms(RuleBody rb, ImmutableSet<Atom> unmatchableAtoms) {
//        return new RuleBody(
//                rb.getPositiveAtoms(),
//                rb.getNegatedAtoms().stream()
//                        .filter(a -> !unmatchableAtoms.contains(a))
//                .collect(ImmutableCollectors.toSet())
//        );
//    }

//    private void evalBindingSet(EvalState state, BindingSet bindingSet, ConstraintConjunction c, Shape s) {
//        String focusNode = bindingSet.getBinding(c.getLocalNodeVar()).getValue().stringValue();
//        // instantiate the rule patterns of the shape
//        instantiateShapeValidationPatterns(state, focusNode, s);
//        instantiateShapeViolationPattern(state, focusNode, s);
//
//
//        if (c.getLocalViolationVars().stream().anyMatch(bindingSet::hasBinding)) {
//            state.assignment.add(new Atom(
//                    c.getId(),
//                    focusNode,
//                    false
//            ));
//            return;
//        }
//        c.getDistantViolationVars().stream()
//                .filter(v -> cannotBeMatched)
//
//
//        Atom posAtom = new Atom(c.getId(), focusNode, true);
//        Atom negAtom = posAtom.getNegation();
//
//        Set<String> bindingNames = bindingSet.getBindingNames();
//
//        ImmutableSet<Atom> validationRuleBody = bindingNames.containsAll(c.getValidationRulePattern().entrySet()) ?
//                ground(c.getValidationRulePattern(), bindingSet) :
//                ImmutableSet.of();
//
//        ImmutableList<ImmutableSet<Atom>> violationRuleBodies = c.getViolationRulePatterns().stream()
//                .filter(r -> bindingNames.containsAll(r.keySet()))
//                .map(r -> ground(r, bindingSet))
//                .collect(ImmutableCollectors.toList());
//
//        if (validationRuleBody.isEmpty()) {
//            if (violationRuleBodies.isEmpty()) {
//                // if there is no constraint to propagate, then the focus shape is verified
//                state.assignment.add(posAtom);
//                return;
//            }
//        }
//        state.ruleMap.add(new RuleBody(ImmutableSet.copyOf(validationRuleBody), posAtom));
//        if (!violationRuleBodies.isEmpty()) {
//            violationRuleBodies
//                    .forEach(b -> state.ruleMap.add(new RuleBody(b, negAtom)));
//        }
//    }
//
//    private void instantiateShapeValidationPatterns(EvalState state, String focusNode, Shape s) {
//        Atom head = new Atom(s.getName(), focusNode, true);
//        s.getDisjuncts().stream()
//                .map(ConstraintConjunction::getId)
//                .map(p -> new Atom(p, focusNode, true))
//                .map(a -> new RuleBody(ImmutableSet.of(a), head))
//                .forEach(r -> state.ruleMap.add(r));
//    }
//
//    private void instantiateShapeViolationPattern(EvalState state, String focusNode, Shape s) {
//        state.ruleMap.add(
//                new RuleBody(
//                        s.getDisjuncts().stream()
//                                .map(a -> a.getId())
//                                .map(p -> new Atom(p, focusNode, true))
//                                .collect(ImmutableCollectors.toSet()),
//                        new Atom(s.getName(), focusNode, false)
//                ));
//    }

//    private ImmutableSet<Atom> ground(ImmutableMap<String, Atom> rule, BindingSet bindingSet) {
//        return rule.entrySet().stream()
//                .map(e -> ground(e.getValue(), bindingSet.getBinding(e.getKey()).getValue()))
//                .collect(ImmutableCollectors.toSet());
//    }
//
//    private Atom ground(Atom atom, Value value) {
//        return new Atom(atom.getPredicate(), value.stringValue(), atom.isPos());
//    }

    private class EvalState {

        Set<String> visitedShapes;
        RuleMap ruleMap;
        ImmutableList<Atom> remainingTargetAtoms;
        Set<Atom> assignment;

        private EvalState(ImmutableList<Atom> targetAtoms, RuleMap ruleMap, Set<Atom> assignment, Set<String> visitedShapes) {
            this.remainingTargetAtoms = targetAtoms;
            this.ruleMap = ruleMap;
            this.assignment = assignment;
            this.visitedShapes = visitedShapes;
        }
    }
}
