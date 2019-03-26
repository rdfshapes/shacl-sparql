package core;

import java.util.Objects;

public class Atom {

    private final String pred;
    private final String arg;
    private final boolean isPos;

    public Atom(String pred, String arg, boolean isPos) {
        this.pred = pred;
        this.arg = arg;
        this.isPos = isPos;
    }

    public String getPredicate() {
        return pred;
    }

    public String getArg() {
        return arg;
    }

    public boolean isPos() {
        return isPos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Atom atom = (Atom) o;
        return isPos == atom.isPos &&
                Objects.equals(pred, atom.pred) &&
                Objects.equals(arg, atom.arg);
    }

    @Override
    public int hashCode() {

        return Objects.hash(pred, arg, isPos);
    }

    public Atom getNegation() {
        return new Atom(pred, arg, !isPos);
    }

    @Override
    public String toString() {
        return (isPos?
                "":
                "!")+
                pred+"("+
                arg+")";
    }
}
