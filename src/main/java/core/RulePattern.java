package core;

import com.google.common.collect.ImmutableSet;
import org.eclipse.rdf4j.query.BindingSet;
import util.ImmutableCollectors;

import java.util.Collection;
import java.util.stream.Collectors;

public class RulePattern {
    private final Atom head;
    private final ImmutableSet<Atom> atoms;

    // If a value for each variable is produced (by a solution mapping), then the rule pattern can be instantiated.
    // note that it may be the case that these variables do not appear in the the body of the rule (because there is no constraint to propagate on these values, they only need to exist)
//    private final ImmutableSet<String> variables;

    public RulePattern(Atom head, ImmutableSet<Atom> atoms) {
        this.head = head;
        this.atoms = atoms;
//        this.variables = variables;
    }

    public Atom getHead() {
        return head;
    }

//    public ImmutableSet<String> getVariables() {
//        return variables;
//    }

    public ImmutableSet<Atom> getAtoms() {
        return atoms;
    }

    @Override
    public String toString() {
        return head + ": - " +
                getAtomString();
//                getNegAtomsString() + ", " +
//                getVariablesString();
    }

//    private String getVariablesString() {
//        return "var: (" + variables.stream()
//                .collect(Collectors.joining(", "))
//                + ")";
//    }

//    private String getNegAtomsString() {
//        if (negatedAtoms.isEmpty()) {
//            return "neg: ()";
//        }
//        return "neg:(" + negatedAtoms.stream()
//                .map(Atom::toString)
//                .collect(Collectors.joining(", ")) +
//                ")";
//    }

    private String getAtomString() {
        if (atoms.isEmpty()) {
            return "pos: ()";
        }
        return "pos(" + atoms.stream()
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

    public ImmutableSet<Atom> instantiateBody(BindingSet bs) {
                return atoms.stream()
                        .map(a -> instantiateAtom(a, bs))
                        .collect(ImmutableCollectors.toSet());
//                this.negatedAtoms.stream()
//                        .map(a -> instantiateAtom(a, bs))
//                        .collect(ImmutableCollectors.toSet())
    }

    public ImmutableSet<String> getVariables() {
    }
}
