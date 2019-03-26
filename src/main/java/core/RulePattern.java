package core;

import com.google.common.collect.ImmutableSet;
import org.eclipse.rdf4j.query.BindingSet;
import util.ImmutableCollectors;

import java.util.stream.Collectors;

public class RulePattern {
    private final Atom head;
    private final ImmutableSet<Atom> positiveAtoms;
    private final ImmutableSet<Atom> negatedAtoms;

    // If a value for each variable is produced (by a solution mapping), then the rule pattern can be instantiated.
    // note that it may be the case that these variables do not appear in the the body of the rule (because there is no constraint to propagate on these values, they only need to exist)
    private final ImmutableSet<String> variables;

    public RulePattern(Atom head, ImmutableSet<Atom> positiveAtoms, ImmutableSet<Atom> negatedAtoms, ImmutableSet<String> variables) {
        this.head = head;
        this.positiveAtoms = positiveAtoms;
        this.negatedAtoms = negatedAtoms;
        this.variables = variables;
    }

    public Atom getHead() {
        return head;
    }

    public ImmutableSet<String> getVariables() {
        return variables;
    }

    public ImmutableSet<Atom> getPositiveAtoms() {
        return positiveAtoms;
    }

    public ImmutableSet<Atom> getNegatedAtoms() {
        return negatedAtoms;
    }

    @Override
    public String toString() {
        return head + ": - " +
                getPosAtomsString() + ", " +
                getNegAtomsString() + ", " +
                getVariablesString();
    }

    private String getVariablesString() {
        return "var: (" + variables.stream()
                .collect(Collectors.joining(", "))
                + ")";
    }

    private String getNegAtomsString() {
        if (negatedAtoms.isEmpty()) {
            return "neg: ()";
        }
        return "neg:(" + negatedAtoms.stream()
                .map(Atom::toString)
                .collect(Collectors.joining(", ")) +
                ")";
    }

    private String getPosAtomsString() {
        if (positiveAtoms.isEmpty()) {
            return "pos: ()";
        }
        return "pos(" + positiveAtoms.stream()
                .map(Atom::toString)
                .collect(Collectors.joining(", ")) +
                ")";
    }

    public Atom instantiateAtom(Atom a, BindingSet bs) {
        return new Atom(
                a.getPredicate(),
                bs.getValue(a.getArg()).stringValue(),
                a.isPos()
        );

    }

    public RuleBody instantiateBody(BindingSet bs) {
        return new RuleBody(
                this.positiveAtoms.stream()
                        .map(a -> instantiateAtom(a, bs))
                        .collect(ImmutableCollectors.toSet()),
                this.negatedAtoms.stream()
                        .map(a -> instantiateAtom(a, bs))
                        .collect(ImmutableCollectors.toSet())
        );
    }
}
