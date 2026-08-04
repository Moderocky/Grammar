package mx.kenzie.grammar.unwrap;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grammar.GrammarException;

import java.lang.constant.Constable;

public interface Unwrapper<Type> {

    Function<Type, Constable, GrammarException> marshal();

    Function<Constable, Type, GrammarException> unmarshal();

}
