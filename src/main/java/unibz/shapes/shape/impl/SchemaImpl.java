package unibz.shapes.shape.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import unibz.shapes.shape.Schema;
import unibz.shapes.shape.Shape;

import java.util.Optional;

public class SchemaImpl implements Schema {

    private final ImmutableMap<String, Shape> shapeMap;
    private final ImmutableSet<String> shapeNames;

    public SchemaImpl(ImmutableMap<String, Shape> shapeMap, ImmutableSet<String> shapeNames) {
        this.shapeMap = shapeMap;
        this.shapeNames = shapeNames;
    }

    @Override
    public Optional<Shape> getShape(String name){
        Shape s = shapeMap.get(name);
        return (s == null)?
                Optional.empty():
                Optional.of(s);
    }

    @Override
    public ImmutableSet<Shape> getShapes() {
        return ImmutableSet.copyOf(shapeMap.values());
    }

    @Override
    public ImmutableSet<String> getShapeNames() {
        return shapeNames;
    }
}
