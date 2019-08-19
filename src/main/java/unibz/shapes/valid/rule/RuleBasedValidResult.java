package unibz.shapes.valid.rule;

import com.google.common.collect.ImmutableSet;
import unibz.shapes.core.Literal;

import java.util.ArrayList;
import java.util.List;

public class RuleBasedValidResult {

    /* Validation results grouped by depth
        results[0]: depth 0,
        results[1]: depth 1,
        etc.
    */
    private List<List<TargetValidationResult>> results;

    private List<TargetValidationResult> resultsAtCurrentDepth;


    public RuleBasedValidResult() {
        this.results = new ArrayList<>();
        this.resultsAtCurrentDepth = new ArrayList<>();
    }

    public void addValidTarget(Literal target) {
        addTarget(target, ImmutableSet.of(), true);
    }

    public void addInValidTarget(Literal target, ImmutableSet<EvalPath> paths) {
        addTarget(target, paths, false);
    }

    public void incrementDepth() {
        results.add(resultsAtCurrentDepth);
        resultsAtCurrentDepth = new ArrayList<>();
    }

    private void addTarget(Literal target, ImmutableSet<EvalPath> paths, boolean isValid) {
        resultsAtCurrentDepth.add(new TargetValidationResult(
                target,
                paths,
                isValid
        ));
    }

    private class TargetValidationResult {

        private final Literal target;
        private final ImmutableSet<EvalPath> paths;
        private final boolean isValid;

        public TargetValidationResult(Literal target, ImmutableSet<EvalPath> paths, boolean isValid) {
            this.target = target;
            this.paths = paths;
            this.isValid = isValid;
        }
    }
}
