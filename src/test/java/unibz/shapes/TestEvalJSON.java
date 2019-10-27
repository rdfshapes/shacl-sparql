package unibz.shapes;

import org.junit.Test;
import unibz.shapes.convert.Parser;
import unibz.shapes.eval.Eval;
import unibz.shapes.shape.Schema;

import java.io.File;

public class TestEvalJSON {
    @Test
    public void nonRec1_json() {
        Eval.main(new String[]{"-d", "ex/shapes/nonRec/1/", "http://dbpedia.org/sparql","./output"});
    }

    @Test
    public void varia1_json() {
        Eval.main(new String[]{"-d", "ex/shapes/varia/", "http://dbpedia.org/sparql","./output"});
    }
}
