package valid.impl;

import core.Query;
import endpoint.QueryEvaluation;
import endpoint.SPARQLEndpoint;
import util.Output;
import valid.Validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UnfoldingBasedValidation implements Validation {


    private final Path query;
    private final SPARQLEndpoint endpoint;
    private final Output logOutput;
    private final Output violationOutput;


    public UnfoldingBasedValidation(Path query, SPARQLEndpoint endpoint, Output logOutput, Output violationOutput) {
        this.query = query;
        this.endpoint = endpoint;
        this.logOutput = logOutput;
        this.violationOutput = violationOutput;
    }


    private void evalQuery(Query q) {
        logOutput.start("Evaluating query\n" + q.getSparql());
        QueryEvaluation eval = endpoint.runQuery(q.getId(), q.getSparql());
        logOutput.elapsed();
        logOutput.write("Number of solution mappings: " + eval.getBindingSets().size());
        eval.getBindingSets().forEach(bs -> violationOutput.write(bs.getBinding("x1").getValue().stringValue()));
        logOutput.elapsed();
    }

    @Override
    public void exec() throws IOException {

        Query q = new Query(
                "q",
                null,
                new String(Files.readAllBytes(query))
        );

        evalQuery(q);
        logOutput.close();
        violationOutput.close();
    }
}