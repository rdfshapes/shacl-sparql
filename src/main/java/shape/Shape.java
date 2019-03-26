package shape;

import com.google.common.collect.ImmutableSet;
import core.RulePattern;

import java.util.Optional;

public interface Shape {

    String getName();
    Optional<String> getTargetQuery();
    ImmutableSet<ConstraintConjunction> getDisjuncts();

    ImmutableSet<RulePattern> getRulePatterns();
    void computeConstraintQueries(Schema schema, Optional<String> graph);
}
