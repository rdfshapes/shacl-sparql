package core;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Assignment {

    private Set<Atom> atoms;
//    private Set<Atom> negatedAtoms;

    public Assignment() {
        this.atoms = new HashSet<>();
//        this.negatedAtoms = new HashSet<>();
    }

    public void addAtom(Atom atom){
        atoms.add(atom);
//        negatedAtoms.add(atom.getNegation());
    }

//    public Set<Atom> getNegatedAtoms() {
//        return negatedAtoms;
//    }

    public boolean contains(Atom a){
        return atoms.contains(a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return Objects.equals(atoms, that.atoms); //&&
//                Objects.equals(negatedAtoms, that.negatedAtoms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(atoms);
    }
}
