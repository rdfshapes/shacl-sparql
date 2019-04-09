package shape.impl;

import com.google.common.collect.ImmutableSet;
import core.Literal;
import core.RulePattern;
import core.global.VariableGenerator;
import shape.Constraint;
import util.ImmutableCollectors;

import java.util.Optional;
import java.util.stream.IntStream;

import static core.global.VariableGenerator.VariableType;

public class ConstraintImpl implements Constraint {


    private final String id;
    private final String path;
    private final Optional<Integer> minCard;
    private final Optional<Integer> maxCard;
    private final Optional<String> datatype;
    private final Optional<String> value;
    private final Optional<String> shapeRef;
    private final ImmutableSet<String> variables;
    private final boolean isPos;
    private RulePattern rulePattern;

    public ConstraintImpl(String id, String path, Optional<Integer> minCard, Optional<Integer> maxCard, Optional<String> datatype, Optional<String> value, Optional<String> shapeRef, boolean isPos) {
        this.id = id;
        this.path = path;
        this.minCard = minCard;
        this.maxCard = maxCard;
        this.datatype = datatype;
        this.value = value;
        this.shapeRef = shapeRef;
        this.isPos = isPos;
        variables = computeVariables();
    }

    public String getPath() {
        return path;
    }

    public Optional<Integer> getMin() {
        return minCard;
    }

    public Optional<Integer> getMax() {
        return maxCard;
    }

    public Optional<String> getDatatype() {
        return datatype;
    }

    public Optional<String> getValue() {
        return value;
    }

    public Optional<String> getShapeRef() {
        return shapeRef;
    }

    @Override
    public String getId() {
        return id;
    }

    public boolean isPos() {
        return isPos;
    }

    private ImmutableSet<String> computeVariables() {
        return minCard.isPresent() ?
                generateVariables(VariableType.VALIDATION, minCard.get()) :
                generateVariables(VariableType.VIOLATION, maxCard.get() + 1);
    }


    private ImmutableSet<String> generateVariables(VariableType type, Integer numberOfVariables) {

        return IntStream.range(0, numberOfVariables)
                .boxed()
                .map(i -> VariableGenerator.generateVariable(type))
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public ImmutableSet<String> getVariables() {
        return variables;
    }



    @Override
    public ImmutableSet<Literal> computeRulePatternBody() {
              return shapeRef.isPresent() ?
                        variables.stream()
                                .map(v -> new Literal(
                                        shapeRef.get(),
                                        v,
                                        isPos
                                ))
                                .collect(ImmutableCollectors.toSet()) :
                      ImmutableSet.of();
    }
}
