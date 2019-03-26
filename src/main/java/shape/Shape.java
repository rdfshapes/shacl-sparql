package shape;

import com.google.common.collect.ImmutableSet;
import core.RulePattern;

import java.util.Optional;
import java.util.stream.Stream;

public interface Shape {

    String getId();
    Optional<String> getTargetQuery();
    ImmutableSet<ConstraintConjunction> getDisjuncts();

    ImmutableSet<RulePattern> getRulePatterns();
    void computeConstraintQueries(Schema schema, Optional<String> graph);

    ImmutableSet<String> getPredicates();

    ImmutableSet<String> computePredicateSet();
}
