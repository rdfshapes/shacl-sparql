package eval;

import endpoint.SPARQLEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shape.Schema;
import shape.Shape;
import preprocess.ShapeParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public class Eval {


    static Logger log = LoggerFactory.getLogger(Eval.class);
    private static final String usage =
            "USAGE: \n" + Eval.class.getSimpleName() +
                    " [-t targetShape] endpoint shapeDir outputDir";

    private static SPARQLEndpoint endpoint;
    private static Optional<String> graph;
    private static Optional<Shape> targetShape;
    private static Schema schema;
    private static Path outputDir;

    public static void main(String[] args) {
//        parseArguments(args);
        readHardCodedArguments();
        schema.getShapes().forEach(s -> s.computeConstraintQueries(schema, graph));
        Validator validator = new Validator(endpoint, schema);

        Instant start = Instant.now();
        validator.validate();
        Instant finish = Instant.now();
        long elapsed = Duration.between(start, finish).toMillis();
        System.out.println("Total execution time: "+elapsed);
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
        String resourceDir = cwd + "/../tests";
//        endpoint = new SPARQLEndpoint("http://obdalin.inf.unibz.it:8890/sparql");
        endpoint = new SPARQLEndpoint("http://dbpedia.org/sparql");
        graph = Optional.of("<dbpedia-person.org>");
        graph = Optional.empty();
        schema = ShapeParser.parseSchema(Paths.get(resourceDir, "shapes/light"));
        outputDir = Paths.get(resourceDir, "output");
        targetShape = Optional.empty();
        //targetShape = Optional.of(schema.getShape("JapaneseMovieRec").get());
    }

}
