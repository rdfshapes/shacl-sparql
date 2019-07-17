package unibz.shapes.eval;

import ch.qos.logback.classic.Logger;
import unibz.shapes.endpoint.SPARQLEndpoint;
import org.slf4j.LoggerFactory;
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

import static ch.qos.logback.classic.Level.INFO;

public class Eval {

    private static Logger log = (Logger) LoggerFactory.getLogger(Eval.class);
    private static final String usage =
            "Usage: \n" + Eval.class.getSimpleName() +
                    "[-r] [-j] [-s schemaString] [-d schemaDir] [-g graphName] endpoint outputDir\n" +
                    "with:\n" +
                    "- r\t\t             Shapes format: RDF (Turtle)\n" +
                    "- j\t\t             Shapes format: JSON\n" +
                    "- schemaString\t\t  Schema as a string (either a JSON array or RDF triples)\n" +
                    "- schemaDir\t\t     Directory containing the schema (extension \".ttl\" for the RDF format,\".json\" for the JSON format." +
                    "For the JSON format, each file should contain one shape.)\n"+
                    "- graphName\t\t     Name of the RDF graph to be validated (using the SPARQL \"GRAPH\" operator)\n" +
                    "- endpoint          SPARQL endpoint exposing the graph to be validated\n" +
                    "- outputDir\t\t     Output directory (validation results statistics and logs)\n" +
                    "";

    private static SPARQLEndpoint endpoint;
    private static Optional<String> graph;
    private static Optional<Path> singleQuery = Optional.empty();
    private static Optional<Schema> schema = Optional.empty();
    private static Path outputDir;
    private static ShapeParser.Format shapeFormat;

    public static void main(String[] args) {
        setLoggers();
        args = new String[]{"-j", "-d", "./ex/shapes/nonRec/2/", "http://dbpedia.org/sparql","./ex/shapes/nonRec/2/output"};
        parseArguments(args);
        schema.ifPresent(s -> s.getShapes().forEach(sh -> sh.computeConstraintQueries(s, graph)));
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
                            schema.get(),
                            new Output(Paths.get(outputDir.toString(), "validation.log").toFile()),
                            new Output(Paths.get(outputDir.toString(), "targets_valid.txt").toFile()),
                            new Output(Paths.get(outputDir.toString(), "targets_violated.txt").toFile()),
                            new Output(Paths.get(outputDir.toString(), "stats.txt").toFile())
                    );
            Instant start = Instant.now();
            validation.exec();
            Instant finish = Instant.now();
            long elapsed = Duration.between(start, finish).toMillis();
            System.out.println("Total execution time: " + elapsed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createOutputDir(Path outputDir) {
        File dir = outputDir.toFile();
        if(!dir.exists())
            dir.mkdirs();
    }

    private static void setLoggers() {
        Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http", "o.e.r.q.r"));

        for (String log : loggers) {
            Logger logger = (Logger) LoggerFactory.getLogger(log);
            logger.setLevel(INFO);
            logger.setAdditive(false);
        }
    }

    private static void parseArguments(String[] args) {
        Optional<Path> schemaDir = Optional.empty();
        Optional<String> schemaString = Optional.empty();
        graph = Optional.empty();
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
                    shapeFormat = ShapeParser.Format.RDF;
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
        if(schemaDir.isPresent()){
            schema = Optional.of(ShapeParser.parseSchemaFromDir(schemaDir.get(), shapeFormat));
        } else if(schemaString.isPresent()){
            schema = Optional.of(ShapeParser.parseSchemaFromString(schemaString.get(), shapeFormat));
        }
        log.info("endPoint: |" + endpoint.getURL() + "|");
        schemaDir.ifPresent(d -> log.info("schemaDir: |" + d + "|"));
        log.info("outputDir: |" + outputDir + "|");
    }


}
