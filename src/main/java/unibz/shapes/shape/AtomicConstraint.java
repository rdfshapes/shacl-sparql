package unibz.shapes.shape;


import com.google.common.collect.ImmutableSet;
import unibz.shapes.core.Literal;

import java.util.Optional;

public interface AtomicConstraint {
//        String getPath();
//        Optional<Integer> getMin();
//        Optional<Integer> getMax();
        Optional<String> getDatatype();
        Optional<String> getValue();
        Optional<String> getShapeRef();

        String getId();
        boolean isPos();

        ImmutableSet<String> getVariables();

//        ImmutableSet<String> computeVariables();
        ImmutableSet<Literal> computeRulePatternBody();
}
