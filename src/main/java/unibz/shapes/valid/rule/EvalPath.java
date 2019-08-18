package unibz.shapes.valid.rule;

import com.google.common.collect.ImmutableList;
import unibz.shapes.shape.Shape;
import unibz.shapes.util.ImmutableCollectors;

import java.util.stream.Stream;

public class EvalPath {

        private final ImmutableList<Shape> shapeNames;

        public EvalPath(Shape shape) {
            shapeNames = ImmutableList.of(shape);
        }

        public EvalPath(Shape shape, EvalPath path) {
            shapeNames = Stream.concat(
                    Stream.of(shape),
                    path.getShapes()
            ).collect(ImmutableCollectors.toList());
        }

        private Stream<Shape> getShapes(){
            return shapeNames.stream();
        }
}
