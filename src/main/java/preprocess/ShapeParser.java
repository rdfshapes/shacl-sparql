package preprocess;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.global.SPARQLPrefixHandler;
import org.apache.commons.io.FileUtils;
import shape.Constraint;
import shape.ConstraintConjunction;
import shape.Schema;
import shape.Shape;
import shape.impl.ConstraintConjunctionImpl;
import shape.impl.ConstraintImpl;
import shape.impl.SchemaImpl;
import shape.impl.ShapeImpl;
import util.ImmutableCollectors;
import util.StreamUt;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ShapeParser {

    public static Schema parseSchema(Path dir) {
        ImmutableSet<Shape> shapes = FileUtils.listFiles(
                dir.toFile(),
                new String[]{"json"},
                false
        ).stream()
                .map(f -> parse(Paths.get(f.getAbsolutePath())))
                .collect(ImmutableCollectors.toSet());

        return new SchemaImpl(
                shapes.stream()
                        .collect(ImmutableCollectors.toMap(
                                s -> s.getId(),
                                s -> s
                        )),
                shapes.stream()
                        .flatMap(s -> s.computePredicateSet().stream())
                        .collect(ImmutableCollectors.toSet())
        );
    }

    private static Shape parse(Path path) {
        Optional<String> targetQuery = Optional.empty();
        try {
            JsonObject obj = new JsonParser().parse(new FileReader(path.toFile())).getAsJsonObject();
            JsonElement targetDef = obj.get("targetDef");
            if (targetDef != null) {
                JsonElement query = targetDef.getAsJsonObject().get("query");
                if (query != null) {
                    targetQuery = Optional.of(SPARQLPrefixHandler.getPrexixString() + query.getAsString());
                }
            }
            String name = obj.get("name").getAsString();
            return new ShapeImpl(
                    name,
                    targetQuery,
                    parseConstraints(name, obj.get("constraintDef").getAsJsonObject().get("conjunctions").getAsJsonArray())
            );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ImmutableSet<ConstraintConjunction> parseConstraints(String shapeName, JsonArray array) {
        AtomicInteger i = new AtomicInteger(0);
        return StreamUt.toStream(array.iterator()).sequential()
                .map(JsonElement::getAsJsonArray)
                .map(a -> parseDisjunct(a, shapeName + "_d" + i.incrementAndGet()))
                .collect(ImmutableCollectors.toSet());
    }

    private static ConstraintConjunction parseDisjunct(JsonArray array, String id) {
        AtomicInteger i = new AtomicInteger(0);
        Map<Boolean, List<Constraint>> part = StreamUt.toStream(array.iterator())
                .map(JsonElement::getAsJsonObject)
                .map(a -> parseConstraint(a, id + "_c" + i.incrementAndGet()))
                // Duplicate the constraints that have both min and max
                .flatMap(ShapeParser::duplicate)
                .collect(Collectors.partitioningBy(
                        c -> c.getMin().isPresent()
                ));
        return new ConstraintConjunctionImpl(
                id,
                ImmutableList.copyOf(part.get(true)),
                ImmutableList.copyOf(part.get(false))
        );
    }

    private static Stream<Constraint> duplicate(Constraint c) {
        return (c.getMin().isPresent()) && (c.getMax().isPresent()) ?
                Stream.of(
                        new ConstraintImpl(c.getId() + "_1", c.getPath(), c.getMin(), Optional.empty(), c.getDatatype(), c.getValue(), c.getShapeRef(), c.isPos()),
                        new ConstraintImpl(c.getId() + "_2", c.getPath(), Optional.empty(), c.getMax(), c.getDatatype(), c.getValue(), c.getShapeRef(), c.isPos())
                ) :
                Stream.of(c);
    }

    private static Constraint parseConstraint(JsonObject obj, String id) {
        JsonElement min = obj.get("min");
        JsonElement max = obj.get("max");
        JsonElement shapeRef = obj.get("shape");
        JsonElement datatype = obj.get("datatype");
        JsonElement value = obj.get("value");
        JsonElement negated = obj.get("negated");

        return new ConstraintImpl(
                id,
                obj.get("path").getAsString(),
                (min == null) ?
                        Optional.empty() :
                        Optional.of(min.getAsInt()),
                (max == null) ?
                        Optional.empty() :
                        Optional.of(max.getAsInt()),
                (datatype == null) ?
                        Optional.empty() :
                        Optional.of(datatype.getAsString()),
                (value == null) ?
                        Optional.empty() :
                        Optional.of(value.getAsString()),
                (shapeRef == null) ?
                        Optional.empty() :
                        Optional.of(shapeRef.getAsString()),
                (negated == null) ?
                        true :
                        !negated.getAsBoolean()
        );
    }
}
