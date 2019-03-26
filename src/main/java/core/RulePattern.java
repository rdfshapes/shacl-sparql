package core;

import com.google.common.collect.ImmutableSet;
import org.eclipse.rdf4j.query.BindingSet;
import util.ImmutableCollectors;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RulePattern {
    private final Atom head;
    private final ImmutableSet<Atom> atoms;
    private final ImmutableSet<String> variables;

    // If a value for each variable is produced (by a solution mapping), then the rule pattern can be instantiated.
    // note that it may be the case that these variables do not appear in the the body of the rule (because there is no constraint to propagate on these values, they only need to exist)
//    private final ImmutableSet<String> variables;

    public RulePattern(Atom head, ImmutableSet<Atom> body) {
        this.head = head;
        this.atoms = body;
        this.variables = Stream.concat(
                Stream.of(head.getArg()),
                body.stream()
                        .map(a -> a.getArg())
        ).collect(ImmutableCollectors.toSet());
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
                getBodyString();
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

    private String getBodyString() {
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
        return variables;
    }
}
