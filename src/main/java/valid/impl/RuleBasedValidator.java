package valid.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.Literal;
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
    private final Output statsOutput;
    private final Stats stats;
//    private int maxRuleNumber;


    public RuleBasedValidator(SPARQLEndpoint endpoint, Schema schema, Output logOutput, Output validTargetsOuput, Output invalidTargetsOuput, Output statsOuput) {
        this.endpoint = endpoint;
        this.schema = schema;
        this.validTargetsOuput = validTargetsOuput;
        this.invalidTargetsOuput = invalidTargetsOuput;
        this.logOutput = logOutput;
        targetShapes = extractTargetShapes();
        targetShapePredicates = targetShapes.stream()
                .map(s -> s.getId())
                .collect(ImmutableCollectors.toSet());
//        this.maxRuleNumber = 0;
        this.stats = new Stats();
        statsOutput = statsOuput;
    }

    public void validate() {
        Instant start = Instant.now();
        Set<Literal> targets = extractTargetAtoms();
        stats.recordInitialTargets(targets.size());
        validate(
                0,
                new EvalState(
                        targets,
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
        stats.recordTotalTime(elapsed);
        System.out.println("Total execution time: " + elapsed);
        logOutput.write("\nMaximal number or rules in memory: " + stats.maxRuleNumber);
        stats.writeAll(statsOutput);

        try {
            logOutput.close();
            validTargetsOuput.close();
            invalidTargetsOuput.close();
            statsOutput.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Literal> extractTargetAtoms() {
        return targetShapes.stream()
                .filter(s -> s.getTargetQuery().isPresent())
                .flatMap(s -> extractTargetAtoms(s).stream())
                .collect(Collectors.toSet());
    }

    private Set<Literal> extractTargetAtoms(Shape shape) {
        return endpoint.runQuery(
                shape.getId(),
                shape.getTargetQuery().get()
        ).getBindingSets().stream()
                .map(b -> b.getBinding("x").getValue().stringValue())
                .map(i -> new Literal(shape.getId(), i, true))
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
            state.remainingTargets.forEach(t -> validTargetsOuput.write(t.toString() + ", not violated"));
            return;
        }
        // termination condition 2: all targets are validated/violated
        if (state.remainingTargets.isEmpty()) {
            return;
        }

        logOutput.start("Starting validation at depth :" + depth);
        validateFocusShapes(state, focusShapes, depth);

        // Set<Literal> assignment = state.assignment;

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
        ImmutableList<Literal> freshLiterals = state.ruleMap.entrySet().stream()
                .map(e -> applyRules(e.getKey(), e.getValue(), state, retainedRules))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableCollectors.toList());
        state.ruleMap = retainedRules;
        state.assignment.addAll(freshLiterals);
        if (freshLiterals.isEmpty()) {
            return false;
        }

        ImmutableSet<Literal> candidateValidTargets = freshLiterals.stream()
                .filter(a -> targetShapePredicates.contains(a.getPredicate()))
                .collect(ImmutableCollectors.toSet());

        Map<Boolean, List<Literal>> part1 = state.remainingTargets.stream()
                .collect(Collectors.partitioningBy(a -> candidateValidTargets.contains(a)));

        state.remainingTargets = ImmutableSet.copyOf(part1.get(false));

        stats.recordDecidedTargets(part1.get(true).size());

        Map<Boolean, List<Literal>> part2 = part1.get(true).stream()
                .collect(Collectors.partitioningBy(a -> a.isPos()));

        state.validTargets.addAll(part2.get(true));
        part2.get(true).forEach(t -> validTargetsOuput.write(t.toString() + ", depth " + depth + ", focus shape " + s.getId()));
        state.invalidTargets.addAll(part2.get(false));
        part2.get(false).forEach(t -> invalidTargetsOuput.write(t.toString() + ", depth " + depth + ", focus shape " + s.getId()));

        logOutput.write("Remaining targets :" + state.remainingTargets.size());
        return true;
    }

    private boolean isValid(Literal t, ImmutableList<Literal> freshLiterals) {
        if (freshLiterals.contains(t)) {
            validTargetsOuput.write(t.toString());
            return true;
        }
        return false;
    }

    private Optional<Literal> applyRules(Literal head, Set<ImmutableSet<Literal>> bodies, EvalState state, RuleMap retainedRules) {
        if (bodies.stream()
                .anyMatch(b -> applyRule(head, b, state, retainedRules))) {
            state.assignment.add(head);
            return Optional.of(head);
        }
        return Optional.empty();
    }

    private boolean applyRule(Literal head, ImmutableSet<Literal> body, EvalState state, RuleMap retainedRules) {
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
        logOutput.write("evaluating queries for shape " + s.getId());
        s.getDisjuncts().forEach(d -> evalDisjunct(state, d, s));
        state.visitedShapes.addAll(s.getPredicates());
        saveRuleNumber(state);

        logOutput.start("saturation ...");
        saturate(state, depth, s);
        stats.recordSaturationTime(logOutput.elapsed());

        logOutput.write("\nvalid targets: " + state.validTargets.size());
        logOutput.write("\nInvalid targets: " + state.invalidTargets.size());
        logOutput.write("\nRemaining targets: " + state.remainingTargets.size());
    }

    private void saveRuleNumber(EvalState state) {
        int ruleNumber = state.ruleMap.getRuleNumber();
        logOutput.write("Number of rules " + ruleNumber);
        stats.recordNumberOfRules(ruleNumber);
    }

    private void evalDisjunct(EvalState state, ConstraintConjunction d, Shape s) {
        evalQuery(state, d.getMinQuery(), s);
        d.getMaxQueries()
                .forEach(q -> evalQuery(state, q, s));
    }

    private void evalQuery(EvalState state, Query q, Shape s) {
        logOutput.start("Evaluating query\n" + q.getSparql());
        QueryEvaluation eval = endpoint.runQuery(q.getId(), q.getSparql());
        stats.recordQueryExecTime(logOutput.elapsed());
        logOutput.write("Number of solution mappings: " + eval.getBindingSets().size());
        stats.recordNumberOfSolutionMappings(eval.getBindingSets().size());
        stats.recordQuery();
        logOutput.start("Grounding rules ...");
        eval.getBindingSets().forEach(
                b -> evalBindingSet(state, b, q.getRulePattern(), s.getRulePatterns())
        );
        stats.recordGroundingTime(logOutput.elapsed());
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
        Set<Literal> ruleHeads = state.ruleMap.keySet();

        int initialAssignmentSize = state.assignment.size();

        // first negate unmatchable body atoms
        state.ruleMap.getAllBodyAtoms().
                filter(a -> !isSatisfiable(a, state, ruleHeads))
                .map(this::getNegatedAtom)
                .forEach(a -> state.assignment.add(a));

        // then negate unmatchable targets
        Map<Boolean, List<Literal>> part2 = state.remainingTargets.stream().
                collect(Collectors.partitioningBy(
                        a -> isSatisfiable(a, state, ruleHeads)
                ));
        List<Literal> inValidTargets = part2.get(false);
        state.invalidTargets.addAll(inValidTargets);
        inValidTargets.forEach(t -> invalidTargetsOuput.write(t.toString() + ", depth " + depth + ", focus shape " + s.getId()));

        state.assignment.addAll(
                inValidTargets.stream()
                        .map(Literal::getNegation)
                        .collect(ImmutableCollectors.toSet())
        );
        state.remainingTargets = new HashSet<>(part2.get(true));

        return initialAssignmentSize != state.assignment.size();
    }

    private Literal getNegatedAtom(Literal a) {
        return a.isPos()?
                a.getNegation():
                a;
    }

    private boolean isSatisfiable(Literal a, EvalState state, Set<Literal> ruleHeads) {
       // boolean b = ruleHeads.contains(a);
//        b = (!state.visitedShapes.contains(a.getPredicate())) || ruleHeads.contains(a);
//        return b;
        return (!state.visitedShapes.contains(a.getPredicate())) || ruleHeads.contains(a.getAtom()) || state.assignment.contains(a);
    }


    private class EvalState {

        Set<String> visitedShapes;
        RuleMap ruleMap;
        Set<Literal> remainingTargets;
        Set<Literal> validTargets;
        Set<Literal> invalidTargets;
        Set<Literal> assignment;

        private EvalState(Set<Literal> targetLiterals, RuleMap ruleMap, Set<Literal> assignment, Set<String> visitedShapes, Set<Literal> validTargets, Set<Literal> invalidTargets) {
            this.remainingTargets = targetLiterals;
            this.ruleMap = ruleMap;
            this.assignment = assignment;
            this.visitedShapes = visitedShapes;
            this.validTargets = validTargets;
            this.invalidTargets = invalidTargets;
        }
    }


    private class Stats {

        public void writeAll(Output statsOutput) {
            statsOutput.write("targets:\n"+initialTargets);
            statsOutput.write("max number of solution mappings for a query:\n"+maxSolutionMappings);
            statsOutput.write("total number of solution mappings:\n"+totalSolutionMappings);
            statsOutput.write("max number of rules in memory:\n"+maxRuleNumber);
            statsOutput.write("number of queries:\n"+numberOfQueries);
            statsOutput.write("max exec time for a query:\n"+maxQueryExectime);
            statsOutput.write("total query exec time:\n"+totalQueryExectime);
            statsOutput.write("max grounding time for a query:\n"+maxGroundingTime);
            statsOutput.write("total grounding time:\n"+totalGroundingTime);
            statsOutput.write("max saturation time:\n"+maxSaturationTime);
            statsOutput.write("total saturation time:\n"+totalSaturationTime);
            statsOutput.write("total time:\n"+totalTime);
        }
        private int initialTargets = 0;

        private int maxRuleNumber = 0;
        private int totalSolutionMappings = 0;
        private int maxSolutionMappings = 0;

        private long totalQueryExectime = 0;
        private long maxQueryExectime = 0;
        private long totalGroundingTime = 0;
        private long maxGroundingTime = 0;
        private long totalSaturationTime = 0;
        private long maxSaturationTime = 0;
        private int numberOfQueries = 0;


        private long totalTime = 0;

        private Map<Integer,Integer> depth2DecidedTargets = new HashMap<>();

        void recordInitialTargets(int k){
            initialTargets = k;
        }
        void recordGroundingTime(long ms){
            if(ms > maxGroundingTime){
                maxGroundingTime = ms;
            }
            totalGroundingTime += ms;
        };
        void recordQueryExecTime(long ms){
            if(ms > maxQueryExectime){
                maxQueryExectime = ms;
            }
            totalQueryExectime += ms;

        };
        void recordSaturationTime(long ms){
            if(ms > maxSaturationTime){
                maxSaturationTime = ms;
            }
            totalSaturationTime += ms;
        };
        void recordNumberOfRules(int k){
            if(k > maxRuleNumber){
                maxRuleNumber = k;
            }
        };
        void recordNumberOfSolutionMappings(int k){
            if(k > maxSolutionMappings){
                maxSolutionMappings = k;
            }
            totalSolutionMappings += k;
        };
        void recordDecidedTargets(int numberOfargets){


        };
        void recordTotalTime(long ms){
            totalTime = ms;
        };

        void recordQuery(){
            numberOfQueries++;
        }

    }
}
