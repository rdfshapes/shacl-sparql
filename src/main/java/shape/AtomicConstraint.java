package shape;


import com.google.common.collect.ImmutableSet;
import com.sun.org.apache.xpath.internal.operations.Variable;
import core.Literal;

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
