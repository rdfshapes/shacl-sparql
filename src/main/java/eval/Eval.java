package eval;

import ch.qos.logback.classic.Logger;
import endpoint.SPARQLEndpoint;
import org.slf4j.LoggerFactory;
import preprocess.ShapeParser;
import shape.Schema;
import shape.Shape;
import util.Output;
import valid.impl.RuleBasedValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static ch.qos.logback.classic.Level.INFO;

public class Eval {


    static Logger log = (Logger) LoggerFactory.getLogger(Eval.class);
    private static final String usage =
            "USAGE: \n" + Eval.class.getSimpleName() +
                    " [-t targetShape] endpoint shapeDir outputDir";

    private static SPARQLEndpoint endpoint;
    private static Optional<String> graph;
    private static Optional<Shape> targetShape;
    private static Schema schema;
    private static Path outputDir;

    public static void main(String[] args) {
        setLoggers();
//        parseArguments(args);
        readHardCodedArguments();
        schema.getShapes().forEach(s -> s.computeConstraintQueries(schema, graph));
        try {
//            UnfoldingBasedValidator validator = new UnfoldingBasedValidator(
            RuleBasedValidator validator = new RuleBasedValidator(
                    endpoint,
                    schema,
                    new Output(Paths.get(outputDir.toString(), "validation.log").toFile()),
                    new Output(Paths.get(outputDir.toString(), "targets_valid.txt").toFile()),
                    new Output(Paths.get(outputDir.toString(), "targets_violated.txt").toFile())
            );
            Instant start = Instant.now();
            validator.validate();
            Instant finish = Instant.now();
            long elapsed = Duration.between(start, finish).toMillis();
            System.out.println("Total execution time: " + elapsed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setLoggers() {
        Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http", "o.e.r.q.r"));

    for(String log:loggers) {
        Logger logger = (Logger) LoggerFactory.getLogger(log);
    logger.setLevel(INFO);
    logger.setAdditive(false);
    }
    }

    private static void parseArguments(String[] args) {
        Optional<String> targetShapeName = Optional.empty();
        Iterator<String> it = Stream.of(args).iterator();
        String currentOpt = it.next();
        while (currentOpt.startsWith("-")) {
            switch (currentOpt) {
                case "-t":
                    targetShapeName = Optional.of(it.next());
                    break;
                case "-g":
                    graph = Optional.of(it.next());
                    break;
                default:
                    throw new RuntimeException("Invalid option " + currentOpt + "\n+" + usage);
            }
            currentOpt = it.next();
        }
        endpoint = new SPARQLEndpoint(it.next());
        Path shapeDir = Paths.get(it.next());
        log.info("endPoint: |" + endpoint.getURL() + "|");
        log.info("shapeDir: |" + shapeDir + "|");
        log.info("outputDir: |" + outputDir + "|");
        schema = ShapeParser.parseSchema(shapeDir);
        targetShapeName.ifPresent(n -> targetShape = Optional.of(schema.getShape(n).get()));
    }

    private static void readHardCodedArguments() {
        String cwd = System.getProperty("user.dir");
        System.out.println(cwd);
        String resourceDir = Paths.get(cwd, "/tests").toString();
        endpoint = new SPARQLEndpoint("http://obdalin.inf.unibz.it:8890/sparql");
//        endpoint = new SPARQLEndpoint("http://dbpedia.org/sparql");
        graph = Optional.of("<dbpedia-person.org>");
//        graph = Optional.empty();
//        schema = ShapeParser.parseSchema(Paths.get(resourceDir, "shapes/light"));
//        schema = ShapeParser.parseSchema(Paths.get(resourceDir, "shapes/nonRec/2/official/tmp"));
        schema = ShapeParser.parseSchema(Paths.get(resourceDir, "shapes/toy/"));
//        outputDir = Paths.get(resourceDir, "output");
//        outputDir = Paths.get(resourceDir, "shapes/nonRec/2/official/tmp/output");
        outputDir = Paths.get(resourceDir, "shapes/toy/output");
        targetShape = Optional.empty();
        //targetShape = Optional.of(schema.getShape("JapaneseMovieRec").get());
    }

}
