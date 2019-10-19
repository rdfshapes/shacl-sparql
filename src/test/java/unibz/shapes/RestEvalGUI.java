package unibz.shapes;

import org.junit.Test;
import unibz.shapes.eval.Eval;

public class RestEvalGUI {

    @Test
    public void nonRec3_json() {
        Eval.main(new String[]{"-d", "ex/shapes/nonRec/2/", "http://dbpedia.org/sparql","./output"});
    }
}
