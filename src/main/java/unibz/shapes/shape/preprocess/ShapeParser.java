package unibz.shapes.shape.preprocess;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import unibz.shapes.core.global.SPARQLPrefixHandler;
import unibz.shapes.shape.*;
import unibz.shapes.shape.impl.*;
import unibz.shapes.util.ImmutableCollectors;
import unibz.shapes.util.StreamUt;

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


    public enum Format {
        JSON,
        SHACL
    }

    public static Schema parseSchemaFromDir(Path dir, Format shapeFormat) {
        String fileExtension = getFileExtension(shapeFormat);
        ImmutableSet<Shape> shapes = FileUtils.listFiles(
                dir.toFile(),
                new String[]{fileExtension},
                false
        ).stream()
                .map(f -> parse(Paths.get(f.getAbsolutePath()), shapeFormat))
                .collect(ImmutableCollectors.toSet());

        return new SchemaImpl(
                shapes.stream()
                        .collect(ImmutableCollectors.toMap(
                                Shape::getId,
                                s -> s
                        )),
                shapes.stream()
                        .flatMap(s -> s.computePredicateSet().stream())
                        .collect(ImmutableCollectors.toSet())
        );
    }

    private static String getFileExtension(Format shapeFormat) {
        switch (shapeFormat) {
            case SHACL:
                return "ttl";
            case JSON:
                return "json";
        }
        throw new RuntimeException("Unexpected format: " + shapeFormat);
    }


    public static Schema parseSchemaFromString(String s, Format shapeFormat) {
        return null;
    }

    private static Shape parse(Path path, Format shapeFormat) {
        switch (shapeFormat) {
            case SHACL:
                return parseTtl(path);
            case JSON:
                return parseJson(path);
        }
        throw new RuntimeException("Unexpected format: " + shapeFormat);

    }

    private static Shape parseJson(Path path) {
        Optional<String> targetQuery = Optional.empty();
        try {
            JsonObject obj = new JsonParser().parse(new FileReader(path.toFile())).getAsJsonObject();
            JsonElement targetDef = obj.get("targetDef");
            if (targetDef != null) {
                JsonElement query = targetDef.getAsJsonObject().get("query");
                if (query != null) {
                    targetQuery = Optional.of(SPARQLPrefixHandler.getPrefixString() + query.getAsString());
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

    private static Shape parseTtl(Path path) {
        return null;
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
        ImmutableList<AtomicConstraint> constraints = StreamUt.toStream(array.iterator())
                .map(JsonElement::getAsJsonObject)
                .map(a -> parseConstraint(a, id + "_c" + i.incrementAndGet()))
                // Duplicate the constraints that have both min and max
                .flatMap(ShapeParser::duplicate)
                .collect(ImmutableCollectors.toList());

        Map<Boolean, List<AtomicConstraint>> part = StreamUt.toStream(array.iterator())
                .map(JsonElement::getAsJsonObject)
                .map(a -> parseConstraint(a, id + "_c" + i.incrementAndGet()))
                // Duplicate the constraints that have both min and max
                .flatMap(ShapeParser::duplicate)
                .collect(Collectors.partitioningBy(
                        c -> c instanceof MinOnlyConstraint
                ));
        return new ConstraintConjunctionImpl(
                id,
                constraints.stream()
                        .filter(c -> c instanceof MinOnlyConstraint)
                        .map(c -> (MinOnlyConstraint) c)
                        .collect(ImmutableCollectors.toList()),
                constraints.stream()
                        .filter(c -> c instanceof MaxOnlyConstraint)
                        .map(c -> (MaxOnlyConstraint) c)
                        .collect(ImmutableCollectors.toList()),
                constraints.stream()
                        .filter(c -> c instanceof LocalConstraint)
                        .map(c -> (LocalConstraint) c)
                        .collect(ImmutableCollectors.toList())
        );
    }

    private static Stream<AtomicConstraint> duplicate(AtomicConstraint c) {
        if(c instanceof MinAndMaxConstraint){
            MinAndMaxConstraint cc = (MinAndMaxConstraint) c;
            return Stream.of(
                    new MinOnlyConstraintImpl(cc.getId() + "_1", cc.getPath(), cc.getMin(), cc.getDatatype(), cc.getValue(), cc.getShapeRef(), cc.isPos()),
                    new MaxOnlyConstraintImpl(cc.getId() + "_2", cc.getPath(), cc.getMax(), cc.getDatatype(), cc.getValue(), cc.getShapeRef(), cc.isPos())
            );
        }
        return Stream.of(c);
    }

    private static AtomicConstraint parseConstraint(JsonObject obj, String id) {

        JsonElement min = obj.get("min");
        JsonElement max = obj.get("max");
        JsonElement shapeRef = obj.get("shape");
        JsonElement datatype = obj.get("datatype");
        JsonElement value = obj.get("value");
        JsonElement path = obj.get("path");
        JsonElement negated = obj.get("negated");

        Optional<Integer> oMin = (min == null) ?
                Optional.empty() :
                Optional.of(min.getAsInt());
        Optional<Integer> oMax = (max == null) ?
                Optional.empty() :
                Optional.of(max.getAsInt());
        Optional<String> oShapeRef = (shapeRef == null) ?
                Optional.empty() :
                Optional.of(shapeRef.getAsString());
        Optional<String> oDatatype = (datatype == null) ?
                Optional.empty() :
                Optional.of(datatype.getAsString());
        Optional<String> oValue = (value == null) ?
                Optional.empty() :
                Optional.of(value.getAsString());
        Optional<String> oPath = (path == null) ?
                Optional.empty() :
                Optional.of(path.getAsString());
        boolean oNeg = (negated == null) ?
                true :
                !negated.getAsBoolean();

        if (oPath.isPresent()) {
            if (oMin.isPresent()) {
                if (oMax.isPresent()) {
                    return new MinAndMaxConstraintImpl(id, oPath.get(), oMin.get(), oMax.get(), oDatatype, oValue, oShapeRef, oNeg);
                }
                return new MinOnlyConstraintImpl(id, oPath.get(), oMin.get(), oDatatype, oValue, oShapeRef, oNeg);
            }
            if (oMax.isPresent()) {
                return new MaxOnlyConstraintImpl(id, oPath.get(), oMax.get(), oDatatype, oValue, oShapeRef, oNeg);
            }
            throw new RuntimeException("min or max cardinality expected with a path");
        }
        return new LocalConstraintImpl(id, oDatatype, oValue, oShapeRef, oNeg);
    }
}
