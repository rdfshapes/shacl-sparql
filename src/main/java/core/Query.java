package core;

import com.google.common.collect.ImmutableSet;

public class Query {

    private final ImmutableSet<RulePattern> rulePatterns;
    private final String sparql;


    public Query(ImmutableSet<RulePattern> rulePattern, String sparql) {
        this.rulePatterns = rulePattern;
        this.sparql = sparql;
    }

    public ImmutableSet<RulePattern> getRulePatterns() {
        return rulePatterns;
    }

    public String getSparql() {
        return sparql;
    }

    public String getId() {
    }
}
