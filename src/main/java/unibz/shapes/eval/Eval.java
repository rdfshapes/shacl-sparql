package unibz.shapes.eval;

//import ch.qos.logback.classic.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unibz.shapes.endpoint.SPARQLEndpoint;
import unibz.shapes.shape.preprocess.ShapeParser;
import unibz.shapes.shape.Schema;
import unibz.shapes.util.Output;
import unibz.shapes.valid.Validation;
import unibz.shapes.valid.impl.RuleBasedValidation;
import unibz.shapes.valid.impl.UnfoldingBasedValidation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

//import static ch.qos.logback.classic.Level.INFO;
import static unibz.shapes.shape.preprocess.ShapeParser.*;

public class Eval {

    private static Logger log = (Logger) LoggerFactory.getLogger(Eval.class);
    private static final String usage =
            "Usage: \n" + Eval.class.getSimpleName() +
                    "[-r] [-j] [-d schemaDir] [-f schemaFile] [-s schemaString] [-g graphName] endpoint outputDir\n" +
                    "with:\n" +
                    "-s\t\t             Shapes format: SHACL/RDF (Turtle)\n" +
                    "-j\t\t             Shapes format: JSON (default format if none of -s or -j is specified)\n" +
                    "-schemaDir\t\t     Directory containing the shape schema (for the JSON format only, one shape per file, extension \".json\")\n"+
                    "-schemaFile\t\t    File containing the shape schema (for the SHACL/RDF format only, extension \".ttl\")\n"+
                    "-schemaString\t\t  Shape schema as a string (for the SHACL/RDF format only)\n"+
                    "-graphName\t\t     Name of the RDF graph to be validated (using the SPARQL \"GRAPH\" operator)\n" +
                    "-endpoint          SPARQL endpoint exposing the graph to be validated\n" +
                    "-outputDir\t\t     Output directory (validation results statistics and logs)\n" +
                    "";

    private static SPARQLEndpoint endpoint;
    private static Schema schema;
    private static Optional<String> graph;
    private static Optional<Path> singleQuery = Optional.empty();
    private static Path outputDir;
    private static ShapeParser.Format shapeFormat;

    public static void main(String[] args) {
//        args = new String[]{"-d", "./ex/shapes/nonRec/2/", "http://dbpedia.org/sparql","./output"};
        parseArguments(args);
        schema.getShapes()
                .forEach(sh -> sh.computeConstraintQueries(schema, graph));
        createOutputDir(outputDir);
        try {
            Validation validation = singleQuery.isPresent() ?
                    new UnfoldingBasedValidation(
                            singleQuery.get(),
                            endpoint,
                            new Output(Paths.get(outputDir.toString(), "validation.log").toFile()),
                            new Output(Paths.get(outputDir.toString(), "targets_violated.txt").toFile())
                    ) :
                    new RuleBasedValidation(
                            endpoint,
                            schema,
                            new Output(Paths.get(outputDir.toString(), "validation.log").toFile()),
                            new Output(Paths.get(outputDir.toString(), "targets_valid.txt").toFile()),
                            new Output(Paths.get(outputDir.toString(), "targets_violated.txt").toFile()),
                            new Output(Paths.get(outputDir.toString(), "stats.txt").toFile())
                    );
            Instant start = Instant.now();
            validation.exec();
            Instant finish = Instant.now();
            long elapsed = Duration.between(start, finish).toMillis();
            log.info("Total execution time: " + elapsed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createOutputDir(Path outputDir) {
        File dir = outputDir.toFile();
        if(!dir.exists())
            dir.mkdirs();
    }

    private static void parseArguments(String[] args) {
        Optional<Path> schemaDir = Optional.empty();
        Optional<Path> schemaFile = Optional.empty();
        Optional<String> schemaString = Optional.empty();
        graph = Optional.empty();
        shapeFormat = ShapeParser.Format.JSON;
        Iterator<String> it = Stream.of(args).iterator();
        String currentOpt = it.next();
        while (currentOpt.startsWith("-")) {
            switch (currentOpt) {
                case "-d":
                    schemaDir = Optional.of(Paths.get(it.next()));
                    break;
                case "-s":
                    schemaString = Optional.of(Paths.get(it.next()).toString());
                    break;
                case "-q":
                    singleQuery = Optional.of(Paths.get(it.next()));
                    break;
                case "-g":
                    graph = Optional.of(it.next());
                    break;
                case "-r":
                    shapeFormat = Format.SHACL;
                    break;
                case "-j":
                    shapeFormat = ShapeParser.Format.JSON;
                    break;
                default:
                    throw new RuntimeException("Invalid option " + currentOpt + "\n+" + usage);
            }
            currentOpt = it.next();
        }
        endpoint = new SPARQLEndpoint(currentOpt);
        outputDir = Paths.get(it.next());
        schema = getSchema(schemaDir, schemaString, schemaFile, shapeFormat);
        log.info("endPoint: |" + endpoint.getURL() + "|");
        schemaDir.ifPresent(d -> log.info("shape directory: |" + d + "|"));
        log.info("output directory: |" + outputDir + "|");
    }

    private static Schema getSchema(Optional<Path> schemaDir, Optional<String> schemaString,
                                              Optional<Path> schemaFile, Format shapeFormat) {
        return schemaDir.isPresent()?
                ShapeParser.parseSchemaFromDir(schemaDir.get(), shapeFormat):
                schemaFile.isPresent()?
                        ShapeParser.parseSchemaFromDir(schemaFile.get(), shapeFormat):
                        ShapeParser.parseSchemaFromString(schemaString.get(), shapeFormat);
    }
}
