package unibz.shapes;

import org.junit.Test;
import unibz.shapes.convert.Parser;
import unibz.shapes.eval.Eval;
import unibz.shapes.shape.Schema;

import java.io.File;

public class TestEvalJSON {
    @Test
    public void nonRec2_json() {
        Eval.main(new String[]{"-d", "ex/shapes/nonRec/2/", "http://dbpedia.org/sparql","./output"});
    }
}
