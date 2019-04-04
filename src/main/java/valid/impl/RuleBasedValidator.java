package valid.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.Atom;
import core.Query;
import core.RulePattern;
import core.global.RuleMap;
import endpoint.QueryEvaluation;
import endpoint.SPARQLEndpoint;
import org.eclipse.rdf4j.query.BindingSet;
import shape.ConstraintConjunction;
import shape.Schema;
import shape.Shape;
import util.ImmutableCollectors;
import util.Output;
import valid.Validator;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class RuleBasedValidator implements Validator {

    private final SPARQLEndpoint endpoint;
    private final Schema schema;
    private final ImmutableSet<Shape> targetShapes;
    private final ImmutableSet<String> targetShapePredicates;
    private final Output logOutput;
    private final Output validTargetsOuput;
    private final Output invalidTargetsOuput;
    private int maxRuleNumber;


    public RuleBasedValidator(SPARQLEndpoint endpoint, Schema schema, Output logOutput, Output validTargetsOuput, Output invalidTargetsOuput) {
        this.endpoint = endpoint;
        this.schema = schema;
        this.validTargetsOuput = validTargetsOuput;
        this.invalidTargetsOuput = invalidTargetsOuput;
        this.logOutput = logOutput;
        targetShapes = extractTargetShapes();
        targetShapePredicates = targetShapes.stream()
                .map(s -> s.getId())
                .collect(ImmutableCollectors.toSet());
        this.maxRuleNumber = 0;
    }

    public void validate() {
        Instant start = Instant.now();
        validate(
                0,
                new EvalState(
                        extractTargetAtoms(),
                        new RuleMap(),
                        new HashSet<>(),
                        new HashSet<>(),
                        new HashSet<>(),
                        new HashSet<>()
                ),
                targetShapes
        );
        Instant finish = Instant.now();
        long elapsed = Duration.between(start, finish).toMillis();
        System.out.println("Total execution time: " + elapsed);
        logOutput.write("\nMaximal number or rules in memory: " + maxRuleNumber);
        logOutput.write("Total execution time: " + elapsed);
        try {
            logOutput.close();
            validTargetsOuput.close();
            invalidTargetsOuput.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Atom> extractTargetAtoms() {
        return targetShapes.stream()
                .filter(s -> s.getTargetQuery().isPresent())
                .flatMap(s -> extractTargetAtoms(s).stream())
                .collect(Collectors.toSet());
    }

    private Set<Atom> extractTargetAtoms(Shape shape) {
        return endpoint.runQuery(
                shape.getId(),
                shape.getTargetQuery().get()
        ).getBindingSets().stream()
                .map(b -> b.getBinding("x").getValue().stringValue())
                .map(i -> new Atom(shape.getId(), i, true))
                .collect(Collectors.toSet());
    }

    private ImmutableSet<Shape> extractTargetShapes() {
        return schema.getShapes().stream()
                .filter(s -> s.getTargetQuery().isPresent())
                .collect(ImmutableCollectors.toSet());
    }

    private void validate(int depth, EvalState state, ImmutableSet<Shape> focusShapes) {

        // termination condition 1: all shapes have been visited
        if (state.visitedShapes.size() == schema.getShapeNames().size()) {
            return;
        }
        // termination condition 2: all targets are validated/violated
        if (state.remainingTargets.isEmpty()) {
            return;
        }

        logOutput.start("Starting validation at depth :"+depth);
        validateFocusShapes(state, focusShapes, depth);

        // Set<Atom> assignment = state.assignment;

        printLog(depth, state);


        validate(depth + 1, state, updateFocusShapes(state));
    }

    private ImmutableSet<Shape> updateFocusShapes(EvalState state) {
        return state.ruleMap.getAllBodyAtoms()
                .map(a -> schema.getShape(a.getPredicate()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(s -> !state.visitedShapes.contains(s.getId()))
                .collect(ImmutableCollectors.toSet());
    }

    private void printLog(int depth, EvalState assignment) {
    }

    private void saturate(EvalState state, int depth, Shape s) {
        boolean negated = negateUnMatchableHeads(state, depth, s);
        boolean inferred = applyRules(state, depth, s);
        if (negated || inferred) {
            saturate(state, depth, s);
        }
    }

    private boolean applyRules(EvalState state, int depth, Shape s) {
        RuleMap retainedRules = new RuleMap();
        ImmutableList<Atom> freshAtoms = state.ruleMap.entrySet().stream()
                .map(e -> applyRules(e.getKey(), e.getValue(), state, retainedRules))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableCollectors.toList());
        state.ruleMap = retainedRules;
        state.assignment.addAll(freshAtoms);
        if (freshAtoms.isEmpty()) {
            return false;
        }

        ImmutableSet<Atom> candidateValidTargets = freshAtoms.stream()
                .filter(a -> targetShapePredicates.contains(a.getPredicate()))
                .collect(ImmutableCollectors.toSet());

        Map<Boolean, List<Atom>> part = state.remainingTargets.stream()
                .collect(Collectors.partitioningBy(a -> candidateValidTargets.contains(a)));
        state.remainingTargets = ImmutableSet.copyOf(part.get(false));
        state.validTargets.addAll(part.get(true));
        part.get(true).forEach(t -> validTargetsOuput.write(t.toString()+", depth "+depth+", focus shape "+s.getId()));

        logOutput.write("Remaining targets :" + state.remainingTargets.size());
        return true;
    }

    private boolean isValid(Atom t, ImmutableList<Atom> freshAtoms) {
        if (freshAtoms.contains(t)) {
            validTargetsOuput.write(t.toString());
            return true;
        }
        return false;
    }

    private Optional<Atom> applyRules(Atom head, Set<ImmutableSet<Atom>> bodies, EvalState state, RuleMap retainedRules) {
        if (bodies.stream()
                .anyMatch(b -> applyRule(head, b, state, retainedRules))) {
            state.assignment.add(head);
            return Optional.of(head);
        }
        return Optional.empty();
    }

    private boolean applyRule(Atom head, ImmutableSet<Atom> body, EvalState state, RuleMap retainedRules) {
        if (state.assignment.containsAll(body)) {
            return true;
        }
        if (body.stream()
                .noneMatch(a -> state.assignment.contains(a.getNegation()))) {
            retainedRules.addRule(head, body);
        }
        return false;
    }

    private void validateFocusShapes(EvalState state, ImmutableSet<Shape> focusShapes, int depth) {
        focusShapes.forEach(s -> evalShape(state, s, depth));
    }

    private void evalShape(EvalState state, Shape s, int depth) {
        logOutput.start("evaluating queries for shape " + s.getId());
        s.getDisjuncts().forEach(d -> evalDisjunct(state, d, s));
        state.visitedShapes.addAll(s.getPredicates());
        saveRuleNumber(state);

        logOutput.start("saturation ...");
        saturate(state, depth, s);
        logOutput.elapsed();
        saveRuleNumber(state);

        logOutput.write("\nvalid targets: " + state.validTargets.size());
        logOutput.write("\nInvalid targets: " + state.invalidTargets.size());
        logOutput.write("\nRemaining targets: " + state.remainingTargets.size());
    }

    private void saveRuleNumber(EvalState state) {
        int ruleNumber = state.ruleMap.getRuleNumber();
        logOutput.write("Number of rules " + ruleNumber);
        maxRuleNumber = ruleNumber > maxRuleNumber ?
                ruleNumber :
                maxRuleNumber;
    }

    private void evalDisjunct(EvalState state, ConstraintConjunction d, Shape s) {
        evalQuery(state, d.getMinQuery(), s);
        d.getMaxQueries()
                .forEach(q -> evalQuery(state, q, s));
    }

    private void evalQuery(EvalState state, Query q, Shape s) {
        logOutput.start("Evaluating query\n" + q.getSparql());
        QueryEvaluation eval = endpoint.runQuery(q.getId(), q.getSparql());
        logOutput.elapsed();
        logOutput.write("Number of solution mappings: " + eval.getBindingSets().size());
        logOutput.start("Grounding rules ...");
        eval.getBindingSets().forEach(
                b -> evalBindingSet(state, b, q.getRulePattern(), s.getRulePatterns())
        );
        logOutput.elapsed();
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

    private boolean negateUnMatchableHeads(EvalState state, int depth, Shape s) {
        Set<Atom> ruleHeads = state.ruleMap.keySet();


        Set<Atom> negatedUnmatchableAtoms = new HashSet();
        negatedUnmatchableAtoms.addAll(
                state.ruleMap.getAllBodyAtoms()
                        .filter(a -> !isSatisfiable(a, state, ruleHeads))
                        .map(Atom::getNegation)
                        .collect(Collectors.toSet()));

        Map<Boolean, List<Atom>> part = state.remainingTargets.stream().
                collect(Collectors.partitioningBy(
                        a -> !isSatisfiable(a, state, ruleHeads)
                ));
        negatedUnmatchableAtoms.addAll(
                part.get(true).stream()
                        .map(Atom::getNegation)
                        .collect(ImmutableCollectors.toSet())
        );
        List<Atom> inValidTargets = part.get(true);
        state.invalidTargets.addAll(inValidTargets);
        inValidTargets.forEach(t -> invalidTargetsOuput.write(t.toString()+", depth "+depth+", focus shape "+s.getId()));

        state.remainingTargets = new HashSet<>(part.get(false));

        state.assignment.addAll(negatedUnmatchableAtoms);
        return !negatedUnmatchableAtoms.isEmpty();
    }

    private boolean isSatisfiable(Atom a, EvalState state, Set<Atom> ruleHeads) {
        return !state.visitedShapes.contains(a.getPredicate()) ||
                ruleHeads.contains(a);
    }


    private class EvalState {

        Set<String> visitedShapes;
        RuleMap ruleMap;
        Set<Atom> remainingTargets;
        Set<Atom> validTargets;
        Set<Atom> invalidTargets;
        Set<Atom> assignment;

        private EvalState(Set<Atom> targetAtoms, RuleMap ruleMap, Set<Atom> assignment, Set<String> visitedShapes, Set<Atom> validTargets, Set<Atom> invalidTargets) {
            this.remainingTargets = targetAtoms;
            this.ruleMap = ruleMap;
            this.assignment = assignment;
            this.visitedShapes = visitedShapes;
            this.validTargets = validTargets;
            this.invalidTargets = invalidTargets;
        }
    }
}
