package mx.kenzie.grammar;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GenericTest {

    @Test
    public void testMarshal() {
        final Object thing = new Object() {
            final Collection<Blob> collection = List.of(Blob.BAR, Blob.FOO);
            final int number = 10;
        };
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = grammar.marshal(thing);
        assert map != null;
        assert !map.isEmpty();
        assert map.size() == 2;
        assert map.get("number").equals(10);
        assert map.get("collection").equals(List.of("BAR", "FOO"));
    }

    @Test
    public void testUnmarshal() {
        class Thing {
            public int number = 5;
            protected Collection<Blob> collection = new ArrayList<>();
        }
        final Thing thing = new Thing();
        thing.number = 6;
        thing.collection = null;
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = Map.of("number", 10, "collection", List.of("BAR", "FOO"));
        grammar.unmarshal(thing, map);
        assert thing.number == 10;
        assert thing.collection.equals(List.of(Blob.BAR, Blob.FOO));
    }

    public enum Blob {
        FOO, BAR
    }

}
