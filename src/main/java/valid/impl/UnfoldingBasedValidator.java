package valid.impl;

import core.Query;
import endpoint.QueryEvaluation;
import endpoint.SPARQLEndpoint;
import shape.Schema;
import shape.Shape;
import util.Output;
import valid.Validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class UnfoldingBasedValidator implements Validator {


    private final SPARQLEndpoint endpoint;
    private final Schema schema;
    private final Optional<Shape> targetShape;
    private final Output logOutput;
    private final Output validTargetsOuput;
    private final Output violatedTargetsOuput;


    public UnfoldingBasedValidator(SPARQLEndpoint endpoint, Schema schema, Output logOutput, Output validTargetsOuput, Output violatedTargetsOuput) {
        this.endpoint = endpoint;
        this.schema = schema;
        this.validTargetsOuput = validTargetsOuput;
        this.violatedTargetsOuput = violatedTargetsOuput;
        this.logOutput = logOutput;
        targetShape = Optional.empty();
    }


    private void evalQuery(Query q) {
        logOutput.start("Evaluating query\n" + q.getSparql());
        QueryEvaluation eval = endpoint.runQuery(q.getId(), q.getSparql());
        logOutput.elapsed();
        logOutput.write("Number of solution mappings: " + eval.getBindingSets().size());
//        eval.getBindingSets().forEach(
//                b -> evalBindingSet(state, b, q.getRulePattern(), s.getRulePatterns())
//        );
//        eval.getBindingSets()
//                .forEach(bs -> logOutput.write(bs.toString()));
        logOutput.elapsed();
    }

    @Override
    public void validate() throws IOException {

        String cwd = System.getProperty("user.dir");
        Query q = new Query(
                "q",
                null,
                new String(Files.readAllBytes(
                        Paths.get(cwd, "../tests/queries/2/test.rq")
        )
                ));

        evalQuery(q);
        logOutput.close();
        validTargetsOuput.close();
        violatedTargetsOuput.close();


    }
}
