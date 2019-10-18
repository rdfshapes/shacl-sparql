package unibz.shapes.valid.rule.result;

import com.google.common.collect.ImmutableSet;
import unibz.shapes.core.Literal;
import unibz.shapes.shape.Shape;
import unibz.shapes.valid.result.InvalidTargetResult;
import unibz.shapes.valid.rule.EvalPath;

public class RuleBasedInvalidTargetResult extends RuleBasedResult implements InvalidTargetResult {

    private final Shape focusShape;

    public RuleBasedInvalidTargetResult(Literal target, int depth, Shape focusShape, ImmutableSet<EvalPath> evalPaths) {
        super(target, depth, evalPaths);
        this.focusShape = focusShape;
    }

    public Shape getFocusShape() {
        return focusShape;
    }
}
