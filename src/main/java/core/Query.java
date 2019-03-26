package core;

import com.google.common.collect.ImmutableSet;

public class Query {

    private final ImmutableSet<RulePattern> rulePatterns;
    private final String sparql;
    private final String id;


    public Query(String id, ImmutableSet<RulePattern> rulePattern, String sparql) {
        this.rulePatterns = rulePattern;
        this.sparql = sparql;
        this.id = id;
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
