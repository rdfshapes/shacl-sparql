package unibz.shapes;

import org.junit.Test;
import unibz.shapes.convert.Parser;
import unibz.shapes.eval.Eval;
import unibz.shapes.shape.Schema;

import java.io.File;

public class TestEvalConcreteSyntax {

    @Test
    public void nonRec2_shacl() {
        Eval.main(new String[]{"-r", "-d", "ex/shapes/nonRec/2/shacl", "http://dbpedia.org/sparql","./output"});
    }
    @Test
    public void tmp2() {

        Schema schema = Parser.parse(new File("ex/shapes/nonRec/2/tmp/Actor.ttl"));
        int i = 1;
    }
    @Test
    public void tmp3() {
        Schema schema = Parser.parse(new File("/home/julien/workspace/shapes/implem/valid/mvn/src/test/resources/good1.ttl"));
        int i = 1;
    }
    @Test
    public void rdf() {

        Schema schema = Parser.parse(new File("src/test/resources/good1.ttl"));
        int i = 1;
    }
}
