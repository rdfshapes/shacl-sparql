package shape;


import com.google.common.collect.ImmutableSet;
import core.RuleBody;
import core.RulePattern;

import java.util.Optional;

public interface Constraint {
        String getPath();
        Optional<Integer> getMin();
        Optional<Integer> getMax();
        Optional<String> getDatatype();
        Optional<String> getValue();
        Optional<String> getShapeRef();
        String getId();
        boolean isPos();

        ImmutableSet<String> getVariables();

        RulePattern computeRulePattern();
        RulePattern getRulePattern();
}
