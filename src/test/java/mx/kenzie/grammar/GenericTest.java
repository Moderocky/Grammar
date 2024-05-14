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

    @Test
    public void testMarshalRecord() {
        record Thing(String name, int age) {
        }
        final Thing thing = new Thing("Jeremy", 66);
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = grammar.marshal(thing);
        assert map != null;
        assert !map.isEmpty();
        assert map.size() == 2;
        assert map.get("name").equals("Jeremy");
        assert map.get("age").equals(66);
    }

    @Test
    public void testUnmarshalRecord() {
        record Thing(String name, int age) {
        }
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = Map.of("age", 61, "name", "Bearimy");
        final Thing thing = grammar.unmarshal(Thing.class, map);
        assert thing.age == 61;
        assert thing.name.equals("Bearimy");
    }

    @Test
    public void testMarshalComplexRecord() {
        record Foo(int number) {
        }
        record Bar(int number) {
        }
        record Thing(String name, Foo foo, @Name("bars") Bar... array) {
        }
        final Thing thing = new Thing("Jeremy", new Foo(3), new Bar(1));
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = grammar.marshal(thing);
        assert map != null;
        assert !map.isEmpty();
        assert map.size() == 3;
        assert map.get("name").equals("Jeremy");
        assert map.get("foo") instanceof Map<?, ?> : map.get("foo");
        assert map.get("foo") instanceof Map<?, ?> foo
            && foo.get("number").equals(3);
        assert map.get("bars") instanceof List<?> bars
            && bars.size() == 1
            && bars.get(0) instanceof Map<?, ?> bar
            && bar.get("number").equals(1) : map.get("bars");
    }

    @Test
    public void testUnmarshalComplexRecord() {
        record Foo(int number) {
        }
        record Bar(int number) {
        }
        record Thing(String name, Foo foo, @Name("bars") Bar... array) {
        }
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = Map.of("name", "Jeremy", "foo", Map.of("number", -5),
            "bars", List.of(Map.of("number", 1), Map.of("number", 3)));
        final Thing thing = grammar.unmarshal(Thing.class, map);
        assert thing.name.equals("Jeremy");
        assert thing.foo != null;
        assert thing.foo.number == -5;
        assert thing.array != null;
        assert thing.array.length == 2;
        assert thing.array[0].number == 1;
        assert thing.array[1].number == 3;
    }

    public enum Blob {
        FOO, BAR
    }

}
