package shape.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import shape.Schema;
import shape.Shape;

import java.util.Optional;

public class SchemaImpl implements Schema {

    private final ImmutableMap<String, Shape> shapeMap;

    public SchemaImpl(ImmutableMap<String, Shape> shapeMap) {
        this.shapeMap = shapeMap;
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
}
