package unibz.shapes.core.global;

import com.google.common.collect.ImmutableMap;

import java.util.stream.Collectors;

public class SPARQLPrefixHandler {

    private static ImmutableMap<String, String> prefixes = ImmutableMap.of(
            "dbo", "<http://dbpedia.org/ontology/>",
            "dbr", "<http://dbpedia.org/resource/>",
            "yago", "<http://dbpedia.org/class/yago/>",
            "foaf", "<http://xmlns.com/foaf/0.1/>",
            "", "<http://example.org/>"
    );
    private static String prefixString = prefixes.entrySet().stream()
            .map(e -> "PREFIX " + e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining("\n"))
            + "\n";

    public static String getPrefixString() {
        return prefixString;
    }
}
