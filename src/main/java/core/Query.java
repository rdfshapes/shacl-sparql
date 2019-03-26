package core;

import com.google.common.collect.ImmutableSet;

public class Query {

    private final RulePattern rulePattern;
    private final String sparql;
    private final String id;


    public Query(String id, RulePattern rulePattern, String sparql) {
        this.rulePattern = rulePattern;
        this.sparql = sparql;
        this.id = id;
    }

    public RulePattern getRulePattern() {
        return rulePattern;
    }

    public String getSparql() {
        return sparql;
    }

    public String getId() {
        return id;
    }
}
