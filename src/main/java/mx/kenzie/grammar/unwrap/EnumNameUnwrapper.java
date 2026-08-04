package mx.kenzie.grammar.unwrap;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grammar.GrammarException;

import java.lang.constant.Constable;

public class EnumNameUnwrapper<Type extends Enum<Type>> implements Unwrapper<Type> {
    protected final Class<Type> type;
    protected final Enum<?>[] constants;

    public EnumNameUnwrapper(Class<Type> type) {
        this.type = type;
        this.constants = type.getEnumConstants();
    }

    @Override
    public Function<Type, Constable, GrammarException> marshal() {
        return Enum::name;
    }

    @Override
    public Function<Constable, Type, GrammarException> unmarshal() {
        return data -> data instanceof String string ? Enum.valueOf(type, string) : null;
    }
}
