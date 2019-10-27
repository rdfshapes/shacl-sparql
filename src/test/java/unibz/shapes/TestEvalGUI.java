package unibz.shapes;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import unibz.shapes.eval.EvalGUI;
import unibz.shapes.eval.EvalGUIJson;
import unibz.shapes.shape.preprocess.ShapeParser;
import unibz.shapes.valid.result.gui.GUIOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestEvalGUI {

    @Test
    public void nonRec() {
        Path dir = Paths.get("ex/shapes/nonRec/2/shacl");
        String schemaString = ShapeParser.concatenateTtlShapesDefs(
                FileUtils.listFiles(
                        dir.toFile(),
                        new String[]{"ttl"},
                        false
                ));
        GUIOutput output =  EvalGUI.eval(schemaString, "http://dbpedia.org/sparql");
        System.out.println(output.getStats()+"\n");
        System.out.println(output.getTargetViolations());
        System.out.println(output.getLog());
        System.out.println(output.isValid());
        System.out.println(output.numberOfValidTargets());
        System.out.println(output.numberOfInValidTargets());

    }

    private String readFile(Path p) {
        try {
            return new String(Files.readAllBytes(p));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void json() {
        Path file = Paths.get("ex/shapes/nonRec/1/MovieShape.json");
        String jsonString = readFile(file);
        GUIOutput output = EvalGUIJson.eval(jsonString, "http://dbpedia.org/sparql");

        System.out.println(output.getStats()+"\n");
        System.out.println(output.getTargetViolations());
        System.out.println(output.getLog());
        System.out.println(output.isValid());
        System.out.println(output.numberOfValidTargets());
        System.out.println(output.numberOfInValidTargets());

    }

    @Test
    public void json2() {
        Path file = Paths.get("ex/shapes/varia/DirectorShape.json");
        String jsonString = readFile(file);
        //GUIOutput output = EvalGUIJson.eval(jsonString, "http://dbpedia.org/sparql");
        GUIOutput output = EvalGUIJson.eval(jsonString, "http://shacl.inf.unibz.it:8443/sparql");

        System.out.println(output.getStats()+"\n");
        System.out.println(output.getTargetViolations());
        System.out.println(output.getLog());
        System.out.println(output.isValid());
        System.out.println(output.numberOfValidTargets());
        System.out.println(output.numberOfInValidTargets());

    }
}
