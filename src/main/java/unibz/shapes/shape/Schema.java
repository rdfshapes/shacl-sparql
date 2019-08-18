package unibz.shapes.shape;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;

public interface Schema {

    Optional<Shape> getShape(String name);

    ImmutableSet<Shape> getShapes();

    ImmutableSet<String> getShapeNames();

    ImmutableSet<Shape> getShapesReferencedBy(Shape s);
}
