package core;

import com.google.common.collect.ImmutableSet;
import org.eclipse.rdf4j.query.BindingSet;
import util.ImmutableCollectors;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleBody {
    private final ImmutableSet<Atom> positiveAtoms;
    private final ImmutableSet<Atom> negatedAtoms;

    public RuleBody(ImmutableSet<Atom> positiveAtoms, ImmutableSet<Atom> negatedAtoms) {
        this.positiveAtoms = positiveAtoms;
        this.negatedAtoms = negatedAtoms;
    }

    public ImmutableSet<Atom> getPositiveAtoms() {
        return positiveAtoms;
    }

    public ImmutableSet<Atom> getNegatedAtoms() {
        return negatedAtoms;
    }

    public ImmutableSet<Atom> getPositiveAndNegatedAtoms() {
        return Stream.concat(
                positiveAtoms.stream(),
                negatedAtoms.stream()
        ).collect(ImmutableCollectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleBody rule = (RuleBody) o;
        return Objects.equals(positiveAtoms, rule.positiveAtoms) &&
                Objects.equals(negatedAtoms, rule.negatedAtoms);
    }

    @Override
    public int hashCode() {

        return Objects.hash(positiveAtoms, negatedAtoms);
    }

    @Override
    public String toString() {
        return getPosAtomsString()+", "+
                getNegAtomsString();
    }

    private String getNegAtomsString() {
        return "neg:("+negatedAtoms.stream()
                .map(Atom::toString)
                .collect(Collectors.joining(", "))+")";
    }

    private String getPosAtomsString() {
        if(positiveAtoms.isEmpty()){
            return "pos()";
        }
        return "pos:("+positiveAtoms.stream()
                .map(Atom::toString)
                .collect(Collectors.joining(", "))+
                ")";
    }

    public RuleBody instantiate(BindingSet bs) {
        return new RuleBody(
                instantiate(positiveAtoms, bs),
                instantiate(negatedAtoms, bs)
        );
    }

    private ImmutableSet instantiate(ImmutableSet<Atom> atoms, BindingSet bs){
        return atoms.stream()
                .map(a -> instantiate(a, bs))
                .collect(ImmutableCollectors.toSet());
    }

    private Atom instantiate(Atom a, BindingSet bs) {
        return new Atom(
                a.getPredicate(),
                bs.getValue(a.getArg()).stringValue(),
                a.isPos()
        );
    }
}
