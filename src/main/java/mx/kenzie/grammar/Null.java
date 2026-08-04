package mx.kenzie.grammar;

import org.valross.constantine.Canonical;
import org.valross.constantine.Constant;

import java.lang.constant.Constable;

public class Null implements Canonical<Null>, Constant.UnitConstant {

    public static final Null INSTANCE = new Null();

    private Null() {
    }

    public static boolean isNull(Object object) {
        return object == null || object instanceof Null;
    }

    public static Null valueOf() {
        return INSTANCE;
    }

    public static Constable safe(Constable value) {
        return value == null ? INSTANCE : value;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public Null intern() {
        return INSTANCE;
    }

}
