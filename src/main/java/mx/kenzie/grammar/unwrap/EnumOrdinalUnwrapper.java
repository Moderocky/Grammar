package mx.kenzie.grammar.unwrap;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grammar.GrammarException;

import java.lang.constant.Constable;

public class EnumOrdinalUnwrapper<Type extends Enum<Type>> implements Unwrapper<Type> {
    protected final Class<Type> type;
    protected final Enum<?>[] constants;

    public EnumOrdinalUnwrapper(Class<Type> type) {
        this.type = type;
        this.constants = type.getEnumConstants();
    }

    @Override
    public Function<Type, Constable, GrammarException> marshal() {
        return Enum::ordinal;
    }

    @Override
    public Function<Constable, Type, GrammarException> unmarshal() {
        //noinspection unchecked
        return data -> data instanceof Integer integer ? (Type) constants[integer] : null;
    }
}
