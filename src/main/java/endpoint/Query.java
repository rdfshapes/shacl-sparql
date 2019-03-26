package endpoint;

public class Query {


    private final String sparqlString;
    private final String id;

    public Query(String sparqlString, String id) {
        this.sparqlString = sparqlString;
        this.id = id;
    }

    public String getSparqlString() {
        return sparqlString;
    }

    public String getId() {
        return id;
    }
}
