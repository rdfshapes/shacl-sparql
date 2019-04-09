package core.global;

import com.google.common.collect.ImmutableMap;

import java.util.stream.Collectors;

public class SPARQLPrefixHandler {

    static ImmutableMap<String, String> prefixes = ImmutableMap.of(
            "dbo", "<http://dbpedia.org/ontology/>",
            "dbr", "<http://dbpedia.org/resource/>",
            "foaf", "<http://xmlns.com/foaf/0.1/>"
    );
    static String prexixString = prefixes.entrySet().stream()
            .map(e -> "PREFIX " + e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining("\n"))
            + "\n";

    public static String getPrexixString() {
        return prexixString;
    }
}
