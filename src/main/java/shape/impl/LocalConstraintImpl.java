package shape.impl;

import shape.LocalConstraint;

import java.util.Optional;

public class LocalConstraintImpl extends AtomicConstraintImpl implements LocalConstraint {

    public LocalConstraintImpl(String id, Optional<String> datatype, Optional<String> value, Optional<String> shapeRef, boolean isPos) {
        super(id, datatype, value, shapeRef, isPos);
    }
}
