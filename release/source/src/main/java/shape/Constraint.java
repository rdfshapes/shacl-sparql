package shape;


import com.google.common.collect.ImmutableSet;
import core.Literal;

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

        ImmutableSet<Literal> computeRulePatternBody();
}
