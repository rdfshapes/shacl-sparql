package unibz.shapes;

import org.junit.Test;
import unibz.shapes.convert.Parser;
import unibz.shapes.eval.Eval;
import unibz.shapes.shape.Schema;

import java.io.File;

public class TestEval {

    @Test
    public void nonRec2_json() {
            Eval.main(new String[]{"-d", "ex/shapes/nonRec/2/", "http://dbpedia.org/sparql","./output"});
    }

    @Test
    public void nonRec2_shacl() {
        Eval.main(new String[]{"-d", "ex/shapes/nonRec/2/", "http://dbpedia.org/sparql","./output"});
    }
    @Test
    public void rdf() {

        Schema schema = Parser.parse(new File("src/test/resources/good1.ttl"));
        int i = 1;
    }
}
