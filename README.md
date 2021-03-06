# SHACL2SPARQL

SHACL2SPARQL is a prototype Java implementation of the algorithm described in
*Validating SHACL constraints over a SPARQL endpoint* (Corman, FLorenzano, Reutter and Savkovic), ISWC 19.

It allows validating an RDF exposed as a SPARQL endpoint against a possibly recursive [SHACL](https://www.w3.org/TR/shacl/) schema,
based on the semantics for recursive SHACL shapes defined in
[*Semantics and validation of recursive SHACL*](https://www.inf.unibz.it/krdb/KRDB%20files/tech-reports/KRDB18-01.pdf).


## Syntax of input shapes ##

SHACL2SPARQL is primarily meant to be used with a dedicated JSON syntax for input shapes (documented in `doc/jsonSyntax.pdf`).

Alternatively, it offers a limited support for shapes written in RDF, namely the fragment of SHACL that corresponds to the JSON syntax.  
Among other restrictions, shape constraints are assumed to be in conjunctive normal form, and constraints on SHACL "value nodes" are limited to string equality and xsd datatypes.  
The SHACL parser is written in Scala, because it relies on [Shaclex](https://github.com/weso/shaclex).


## Installation ##

A jar is provided with the release.

Alternatively, scripts are provided to build:
* the validation engine (in Java)
* the validation engine (in Java) and the parser (in Scala)


## Validate an RDF graph with SHACL2SPARQL ##

Java 8 is required.

To validate a graph with SHACL2SPARQL:

```
java -jar <jarPath> [-j] [-r] [-d schemaDir] [-f schemaFile] [-s schemaString] [-g graphName] endpoint outputDir
```

where `<jarPath>` is the path to the .jar `build/valid<version>.jar`

with:
* `-j`: Select the JSON input shape format, described in the document `doc/jsonSyntax.pdf` (default format if none of -j or -r is specified).
* `-r`: Select the SHACL/RDF (Turtle) input shape format
* `schemaDir`: Directory containing the shape schema (one shape per file)
    - SHACL/RDF format: extension ".ttl"
    - JSON format: one shape per file, extension ".json"
* `schemaFile`: Unique file containing the whole schema (for the SHACL/RDF format only, extension ".ttl")
* `schemaString`: Whole schema as a string (for the SHACL/RDF format only)
* `graphName`: Name of the RDF graph to be validated (using the SPARQL "GRAPH" operator)
* `endpoint`: SPARQL endpoint exposing the graph to be validated
* `outputDir`: Output directory (validation results, statistics and logs)
 
For instance (from the current directory, assuming that the jar is in the `build` directory):
```
java -jar build/valid-1.0-SNAPSHOT.jar -d ./ex/shapes/nonRec/2/ "http://dbpedia.org/sparql"  ./output/
```

Note that the validation results for the above command are incorrect, because the SPARQL endpoint "http://dbpedia.org/sparql" only returns the 10 000 first answers to a query.




## Build from source (Linux or Mac OS)

Maven 3 is required.

### Build the validation engine only (Java source)

The SHACL parser (written in Scala) will not be rebuilt. 

From the current directory (must contain the `pom.xml` file and `src` directory):
```
./build.sh
```
The jar is `build/valid<version>.jar`.


### Build the SHACL parser (Scala source) and validation engine (Java source)

SBT 1.2.6 is required (or modify the version in `project/build.properties`).

From the current directory (must contain the `pom.xml` file, `build.sbt` file and `src` directory):
```
./build_full.sh
```
The parser depends on [Shaclex version 0.1.24](https://github.com/weso/shaclex/releases/tag/0.1.24) (the release contains compilation instructions).
