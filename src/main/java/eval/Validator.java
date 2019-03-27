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
import shape.ConstraintConjunction;
import shape.Schema;
import shape.Shape;
import util.ImmutableCollectors;
import util.Output;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class Validator {

    private final SPARQLEndpoint endpoint;
    private final Schema schema;
    private final Optional<Shape> targetShape;
    private final Output output;


    Validator(SPARQLEndpoint endpoint, Schema schema, Output output) throws IOException {
        this.endpoint = endpoint;
        this.schema = schema;
        targetShape = Optional.empty();
        this.output = output;
    }

    public void validate() throws IOException {
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
        output.close();
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
                shape.getId(),
                shape.getTargetQuery().get()
        ).getBindingSets().stream()
                .map(b -> b.getBinding("x").getValue().stringValue())
                .map(i -> new Atom(shape.getId(), i, true))
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
        if (state.visitedShapes.size() == schema.getShapeNames().size()) {
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
                .filter(s -> !state.visitedShapes.contains(s.getId()))
                .collect(ImmutableCollectors.toSet());
    }

    private void printLog(int depth, EvalState assignment) {
    }

    private Set<Atom> saturate(EvalState state, Set<Atom> inferredAtoms) {
        Set<Atom> freshAtoms = negateUnMatchableHeads(state);
        freshAtoms.addAll(applyRules(state));
        if (freshAtoms.isEmpty()) {
            return inferredAtoms;
        }
        inferredAtoms.addAll(freshAtoms);
        return saturate(state, inferredAtoms);
    }

    private ImmutableList<Atom> applyRules(EvalState state) {
        RuleMap retainedRules = new RuleMap();
        ImmutableList<Atom> freshAtoms = state.ruleMap.entrySet().stream()
                .map(e -> applyRules(e.getKey(), e.getValue(), state, retainedRules))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableCollectors.toList());
        state.ruleMap = retainedRules;
        state.assignment.addAll(freshAtoms);
        return freshAtoms;
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

    private void validateFocusShapes(EvalState state, ImmutableSet<Shape> focusShapes) {
        focusShapes.forEach(s -> evalShape(state, s));
    }

    private void evalShape(EvalState state, Shape s) {
        output.start("evaluating queries for shape " + s.getId());
        s.getDisjuncts().forEach(d -> evalDisjunct(state, d, s));
        output.elapsed();
        state.visitedShapes.addAll(s.getPredicates());

        output.start("saturation ...");
        Set<Atom> freshAtoms = saturate(state, new HashSet<>());
        output.elapsed();

        // partitions target atoms into atoms that have just been validated, and remaining ones
        Map<Boolean, List<Atom>> part1 = state.remainingTargetAtoms.stream()
                .collect(Collectors.partitioningBy(a -> freshAtoms.contains(a)));
        output.write("\nvalid targets: "+part1.get(true).size());

        Set<Atom> ruleHeads = state.ruleMap.keySet();
        // partitions non-validated atoms into atoms that have just been violated, and remaining ones
        Map<Boolean, List<Atom>> part2 = part1.get(false).stream()
                .collect(Collectors.partitioningBy(a -> freshAtoms.contains(a.getNegation())|| !ruleHeads.contains(a)));
        output.write("Invalid targets: "+part2.get(true).size());

        output.write("Remaining targets: "+part2.get(false).size());
        state.remainingTargetAtoms = part2.get(false);

    }


    private void evalDisjunct(EvalState state, ConstraintConjunction d, Shape s) {
        evalQuery(state, d.getMinQuery(), s);
        d.getMaxQueries()
                .forEach(q -> evalQuery(state, q, s));

    }

    private void evalQuery(EvalState state, Query q, Shape s) {
        output.start("Evaluating query "+q.getSparql());
        QueryEvaluation eval = endpoint.runQuery(q.getId(), q.getSparql());
        output.elapsed();
        output.write("Number of solution mappings: "+eval.getBindingSets().size());
        output.start("Grounding rules ...");
        eval.getBindingSets().forEach(
                b -> evalBindingSet(state, b, q.getRulePattern(), s.getRulePatterns())
        );
        output.elapsed();
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

    private Set<Atom> negateUnMatchableHeads(EvalState state) {
        Set<Atom> ruleHeads = state.ruleMap.keySet();

        Set<Atom> negatedUnmatchableAtoms = state.ruleMap.getAllBodyAtoms()
                .filter(a -> state.visitedShapes.contains(a.getPredicate()))
                .filter(a -> !ruleHeads.contains(a) && !state.assignment.contains(a))
                .map(Atom::getNegation)
                .collect(Collectors.toSet());

        state.assignment.addAll(negatedUnmatchableAtoms);
        return negatedUnmatchableAtoms;
    }


    private class EvalState {

        Set<String> visitedShapes;
        RuleMap ruleMap;
        List<Atom> remainingTargetAtoms;
        Set<Atom> assignment;

        private EvalState(ImmutableList<Atom> targetAtoms, RuleMap ruleMap, Set<Atom> assignment, Set<String> visitedShapes) {
            this.remainingTargetAtoms = targetAtoms;
            this.ruleMap = ruleMap;
            this.assignment = assignment;
            this.visitedShapes = visitedShapes;
        }
    }
}
