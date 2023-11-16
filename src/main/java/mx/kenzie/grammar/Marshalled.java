package mx.kenzie.grammar;

import java.util.Map;

/**
 * Something that can be stored in a map of simple types.
 */
public interface Marshalled {

    Map<String, Object> serialise() throws GrammarException;

    void deserialise(Map<String, Object> data) throws GrammarException;

}
