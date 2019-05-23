package shape;

import com.google.common.collect.ImmutableSet;
import core.Query;

import java.util.Optional;
import java.util.stream.Stream;

public interface ConstraintConjunction {

    String getId();
    void computeQueries(Optional<String> graph);
    Query getMinQuery();
    ImmutableSet<Query> getMaxQueries();

    Stream<String> getPredicates();
}
