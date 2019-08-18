package unibz.shapes.shape.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import unibz.shapes.shape.DependencyGraph;
import unibz.shapes.shape.Schema;
import unibz.shapes.shape.Shape;
import unibz.shapes.util.ImmutableCollectors;

import java.util.Optional;

public class SchemaImpl implements Schema {

    private final ImmutableMap<String, Shape> shapeMap;
    private final ImmutableSet<String> shapeNames;
    private final DependencyGraph dependencyGraph;

    public SchemaImpl(ImmutableSet<Shape> shapes) {

        this.shapeMap = shapes.stream()
                .collect(ImmutableCollectors.toMap(
                        Shape::getId,
                        s -> s
                ));
        this.shapeNames = shapes.stream()
                .flatMap(s -> s.computePredicateSet().stream())
                .collect(ImmutableCollectors.toSet());
        dependencyGraph = DependencyGraph.computeDependecyGraph(shapeMap);
    }

    @Override
    public Optional<Shape> getShape(String name) {
        Shape s = shapeMap.get(name);
        return (s == null) ?
                Optional.empty() :
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
