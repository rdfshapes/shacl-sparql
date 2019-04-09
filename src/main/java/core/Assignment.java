package core;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Assignment {

    private Set<Literal> literals;
//    private Set<Literal> negatedAtoms;

    public Assignment() {
        this.literals = new HashSet<>();
//        this.negatedAtoms = new HashSet<>();
    }

    public void addAtom(Literal literal){
        literals.add(literal);
//        negatedAtoms.add(literal.getNegation());
    }

//    public Set<Literal> getNegatedAtoms() {
//        return negatedAtoms;
//    }

    public boolean contains(Literal a){
        return literals.contains(a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return Objects.equals(literals, that.literals); //&&
//                Objects.equals(negatedAtoms, that.negatedAtoms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(literals);
    }
}
