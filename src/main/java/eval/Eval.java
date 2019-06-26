package eval;

import ch.qos.logback.classic.Logger;
import endpoint.SPARQLEndpoint;
import org.slf4j.LoggerFactory;
import preprocess.ShapeParser;
import shape.Schema;
import util.Output;
import valid.Validation;
import valid.impl.RuleBasedValidation;
import valid.impl.UnfoldingBasedValidation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static ch.qos.logback.classic.Level.INFO;

public class Eval {

    enum Format {
        JSON,
        SHACL
    }
    private static Logger log = (Logger) LoggerFactory.getLogger(Eval.class);
    private static final String usage =
            "Usage: \n" + Eval.class.getSimpleName() +
                    "[-s] [-j] [-d shapeDir] [-g graphName] endpoint outputDir\n" +
                    "with:\n" +
                    "- s\t\t             Shapes format: SHACL/RDF (Turtle)\n" +
                    "- j\t\t             Shapes format: JSON\n" +
                    "- shapeDir\t\t      Directory containing the shapes (one shape per file, extension \".ttl\" for SHACL/RDF format,\".json\" for JSON format)\n"+
                    "- graphName\t\t     Name of the RDF graph to be validated (using the SPARQL \"GRAPH\" operator)\n" +
                    "- endpoint          SPARQL endpoint exposing the graph to be validated\n" +
                    "- outputDir\t\t     Output directory (validation results statistics and logs)\n" +
                    "";

    private static SPARQLEndpoint endpoint;
    private static Optional<String> graph;
    private static Optional<Path> singleQuery = Optional.empty();
    private static Optional<Schema> schema = Optional.empty();
    private static Path outputDir;
    private static Format shapeFormat;

    public static void main(String[] args) {
        setLoggers();
//        args = new String[]{"-s", "./release/data/shapes/nonRec/2/", "http://dbpedia.org/sparql","./release/data/shapes/nonRec/2/output"};
        parseArguments(args);
        schema.ifPresent(s -> s.getShapes().forEach(sh -> sh.computeConstraintQueries(s, graph)));
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

    private static void setLoggers() {
        Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http", "o.e.r.q.r"));

        for (String log : loggers) {
            Logger logger = (Logger) LoggerFactory.getLogger(log);
            logger.setLevel(INFO);
            logger.setAdditive(false);
        }
    }

    private static void parseArguments(String[] args) {
        Optional<Path> shapeDir = Optional.empty();
        graph = Optional.empty();
        Iterator<String> it = Stream.of(args).iterator();
        String currentOpt = it.next();
        while (currentOpt.startsWith("-")) {
            switch (currentOpt) {
                case "-d":
                    Path path = Paths.get(it.next());
                    shapeDir = Optional.of(path);
                    schema = Optional.of(ShapeParser.parseSchema(path));
                    break;
                case "-q":
                    singleQuery = Optional.of(Paths.get(it.next()));
                    break;
                case "-g":
                    graph = Optional.of(it.next());
                    break;
                case "-s":
                    shapeFormat = Format.SHACL;
                    break;
                case "-j":
                    shapeFormat = Format.JSON;
                    break;
                default:
                    throw new RuntimeException("Invalid option " + currentOpt + "\n+" + usage);
            }
            currentOpt = it.next();
        }
        endpoint = new SPARQLEndpoint(currentOpt);
        outputDir = Paths.get(it.next());
        log.info("endPoint: |" + endpoint.getURL() + "|");
        shapeDir.ifPresent(d -> log.info("shapeDir: |" + d + "|"));
        log.info("outputDir: |" + outputDir + "|");
    }
}
