# SHACL2SPARQL

SHACL2SPARQL is a prototype Java implementation of the algorithm described in
*Validating SHACL constraints over a SPARQL endpoint* (Corman, FLorenzano, Reutter and Savkovic), ISWC 19 (to appear).

It allows validating an RDF exposed as a SPARQL endpoint against a possibly recursive [SHACL](https://www.w3.org/TR/shacl/) schema,
based on the semantics for recursive SHACL shapes defined in
[*Semantics and validation of recursive SHACL*](https://www.inf.unibz.it/krdb/KRDB%20files/tech-reports/KRDB18-01.pdf).


## Validate an RDF graph with SHACL2SPARQL ##

To validate a graph with SHACL2SPARQL:

```
java -jar <jar> [-g <graphName>] -d <shapeDirectory> <endpointUrl> <outputDirectory>
```

where:
* \<jar\> is the path to the .jar `build/validation-1.0-SNAPSHOT.jar`
* \<graphName\> is the (optional) name of the graph. If specified, the generated queries will use the SPARQL GRAPH keyword.
* \<shapeDirectory\> is the path to a folder containing the shapes to be validated.
There must be one json file per shape, following the syntax described in the document `doc/jsonSyntax.pdf`.
Support for the RDF (Turtle) SHACL syntax will be available soon. 
* \<shapeDirectory\> is the url of the sparql endpoint.
* \<outputDirectory\> is the path to the output directory. It will contain logs and validation results.

Java 8 is required.

For instance (from this directory):
```
java -jar build/validation-1.0-SNAPSHOT.jar -d ./ex/shapes/nonRec/2/ "http://dbpedia.org/sparql"  ./output/
```

Note that the validation results for the above command are incorrect, because the SPARQL endpoint "http://dbpedia.org/sparql" only returns the 10 000 first answers to a query.


## Build from source (Linux or Mac OS)

Maven 3 is required.

### Build the validation engine only (Java source)

This is the recommended build.
All features will be available, but the SHACL parser (written in Scala) will not be rebuilt. 

* From the current directory (must contain the `pom.xml` file and `src` directory):
```
mvn clean package
```
* The jar is `src/target/validation-1.0-SNAPSHOT.jar`.


### Build the SHACL parser (Scala source) and validation engine (Java source)

SBT is required.

* From the current directory (must contain the `pom.xml` file, `build.sbt` file and `src` directory):
```
./build_full.sh
```
