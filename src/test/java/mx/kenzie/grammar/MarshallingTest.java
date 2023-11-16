package mx.kenzie.grammar;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MarshallingTest {

    @Test
    public void testMarshal() {
        class Thing implements Marshalled {
            public int number;
            protected String word = "test";

            @Override
            public Map<String, Object> serialise() throws GrammarException {
                return new HashMap<>(Map.of("number", number, "foo", word));
            }

            @Override
            public void deserialise(Map<String, Object> data) throws GrammarException {
                this.number = (int) data.getOrDefault("number", number);
                this.word = (String) data.getOrDefault("foo", word);
            }
        }
        final Thing thing = new Thing();
        thing.number = 6;
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = grammar.marshal(thing);
        assert map != null;
        assert !map.isEmpty();
        assert map.size() == 2;
        assert map.get("number").equals(6);
        assert map.containsKey("foo");
        assert !map.containsKey("word");
        assert map.get("foo").equals("test");
        map.put("foo", "blob");
        final Thing repeat = grammar.unmarshal(Thing.class, map);
        assert repeat != thing;
        assert repeat.number == 6;
        assert repeat.word != null;
        assert repeat.word.equals("blob");
    }

}
