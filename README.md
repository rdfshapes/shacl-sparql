# Validating recursive SHACL over a SPARQL endpoint #

The file `validation-1.0-SNAPSHOT.jar` is the implementation used for the experiments reported in the article "Validating SHACL constraints over a SPARQL endpoint", submitted at ISWC 2019.

It validates an RDF graph exposed as a SPARQL endpoint against (possibly recursive) SHACL shapes.

* The source code is in directory `source`.
* The shapes used for the experiments are in directory `data/shapes`.
* The SPARQL queries used as a baseline (for non-recursive shapes) are in directory `data/queries`.


### Using the validator ###


```
java -jar validation-1.0-SNAPSHOT.jar [-g <graphName>] -s <shapeDirectory> <endpointUrl> <outputDir>
```

where:
* <graphName> is the (optional) name of the graph. If specified, the generated queries will use the SPARQL GRAPH keyword.
* <shapeDirectory> is the folder containing the shapes to be validated.
There must be one json file per shape, following the syntax described by the document "jsonSyntax.pdf".
* <shapeDirectory> is the url of the sparql endpoint.
* <outputDir> is the output directory. It will contain logs and the validation results.

Java 8 is required.

For instance:
```
java -jar validation-1.0-SNAPSHOT.jar -s ./data/shapes/nonRec/2/ "http://dbpedia.org/sparql"  ./output
```

Note that the validation results for the above command are incorrect, because the SPARQL endpoint "http://dbpedia.org/sparql" only returns the 10 000 first answers to a query.


### Build the jar from source ###

Maven is required.

* Move to directory `source` (it must contain the `pom.xml` file and the `src` directory).
* Execute:
```
mvn package
```
* The jar is `source/src/target/validation-1.0-SNAPSHOT.jar`

