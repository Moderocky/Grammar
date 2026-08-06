package mx.kenzie.grammar.unwrap;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grammar.GrammarException;

import java.lang.constant.Constable;

/// A provider for the marshalling and unmarshalling functions for a type.
public interface Unwrapper<Type> {

    /// The marshalling function.
    Function<Type, Constable, GrammarException> marshal();

    /// The unmarshalling function.
    Function<Constable, Type, GrammarException> unmarshal();

}
