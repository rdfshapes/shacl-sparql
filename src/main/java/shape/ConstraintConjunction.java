package shape;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import core.Query;
import core.RulePattern;

import java.util.Optional;

public interface ConstraintConjunction {

    String getId();
    void computeQueries(Schema schema, Optional<String> graph);
    Query getMinQuery();
    ImmutableList<Query> getMaxQueries();

//    void computeRulePatterns(Schema s);
//    ImmutableSet<RulePattern> getRulePatterns();
}
