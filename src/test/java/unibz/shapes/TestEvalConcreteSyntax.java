package unibz.shapes;

import org.junit.Test;
import unibz.shapes.convert.Parser;
import unibz.shapes.eval.Eval;
import unibz.shapes.shape.Schema;

import java.io.File;

public class TestEvalConcreteSyntax {

    @Test
    public void nonRec_2_shacl() {
        Eval.main(new String[]{"-r", "-d", "ex/shapes/nonRec/2/shacl", "http://shacl.inf.unibz.it:8443/sparql","./output"});
    }

    @Test
    public void rec_2_shacl() {
        Eval.main(new String[]{"-r", "-d", "ex/shapes/rec/2/shacl", "http://dbpedia.org/sparql","./output"});
    }


    @Test
    public void rec_3_shacl() {
        Eval.main(new String[]{"-r", "-d", "ex/shapes/rec/3/shacl", "http://dbpedia.org/sparql","./output"});
    }
}
