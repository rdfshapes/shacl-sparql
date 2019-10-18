package unibz.shapes.eval;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unibz.shapes.endpoint.SPARQLEndpoint;
import unibz.shapes.shape.Schema;
import unibz.shapes.shape.preprocess.ShapeParser;
import unibz.shapes.util.StringOutput;
import unibz.shapes.valid.Validation;
import unibz.shapes.valid.result.ResultSet;
import unibz.shapes.valid.rule.RuleBasedValidation;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class EvalGUI {

    private static Logger log = (Logger) LoggerFactory.getLogger(Eval.class);

    public ImmutableList<String> eval(String schemaString, String endpointUrl) {
        Schema schema = ShapeParser.parseSchemaFromString(schemaString, ShapeParser.Format.SHACL);
        StringOutput validationLog = new StringOutput(), validTargetsLog = new StringOutput(), inValidTargetsLog = new StringOutput(), statsLog = new StringOutput();
        Validation validation = new RuleBasedValidation(
                new SPARQLEndpoint(endpointUrl),
                schema,
                validationLog,
                validTargetsLog,
                inValidTargetsLog,
                statsLog
        );
        Instant start = Instant.now();
        ResultSet rs;
        try {
            rs = validation.exec();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Instant finish = Instant.now();
        long elapsed = Duration.between(start, finish).toMillis();
        log.info("Total execution time: " + elapsed);
        return ImmutableList.of(
                validationLog.toString(),
                statsLog.toString(),
                rs.toString()
        );
    }
}
