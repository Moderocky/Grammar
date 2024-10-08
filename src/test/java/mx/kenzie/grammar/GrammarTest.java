package mx.kenzie.grammar;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GrammarTest {

    private static Field field1, field2;
    protected transient String unseen = "hello";
    protected String seen = "there";

    @BeforeClass
    public static void startup() throws Throwable {
        field1 = GrammarTest.class.getDeclaredField("unseen");
        field2 = GrammarTest.class.getDeclaredField("seen");
    }

    @Test
    public void testMarshal() {
        final Object thing = new Object() {
            final int number = 10;
            final String word = "hello";
        };
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = grammar.marshal(thing);
        assert map != null;
        assert !map.isEmpty();
        assert map.size() == 2;
        assert map.get("number").equals(10);
        assert map.get("word").equals("hello");
    }

    @Test
    public void testUnmarshal() {
        class Thing {
            int number = 5;
            String word = "there";
        }
        final Thing thing = new Thing();
        thing.number = 10;
        thing.word = "hello";
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = Map.of("number", 10, "word", "hello");
        final Thing result = grammar.unmarshal(thing, map);
        assert result != null;
        assert result == thing;
        assert result.number == 10;
        assert result.word != null;
        assert Objects.equals(result.word, "hello");
        assert !Objects.equals(result.word, "there");
    }

    @Test
    public void testParametrizedList() {
        class Foo {
            int bar;
            Foo(int bar) {
                this.bar = bar;
            }
        }

        class Thing {
            List<Foo> list;
        }

        final Thing thing = new Thing();
        thing.list = List.of(new Foo(13), new Foo(37));

        final Grammar grammar = new Grammar();
        final Map<String, Object> map = grammar.marshal(thing);
        final Thing result = grammar.unmarshal(Thing.class, map);
        assert result.list.get(0).bar == 13;
        assert result.list.get(1).bar == 37;
    }

    @Test
    public void testShouldSkip() {
        final Grammar grammar = new Grammar();
        assert grammar.shouldSkip(field1);
        assert !grammar.shouldSkip(field2);
    }

    @Test
    public void testAnySubType() {
        class Bean {
        }
        class Child extends Bean {
            final int number = 5;
        }
        class Result {
            final @Any Bean child = new Child();
        }
        {
            final Grammar grammar = new Grammar();
            final Map<String, Object> map = grammar.marshal(new Result());
            assert map != null;
            assert map.containsKey("child");
            assert map.get("child") instanceof Map<?, ?> child && child.containsKey("number");
            assert map.get("child") instanceof Map<?, ?> child && child.get("number").equals(5);
        }
        {
            final Grammar grammar = new Grammar();
            final Map<String, Object> map = new LinkedHashMap<>();
            grammar.marshal(new Result(), Result.class, map);
            assert map.containsKey("child");
            assert map.get("child") instanceof Map<?, ?> child && child.containsKey("number");
            assert map.get("child") instanceof Map<?, ?> child && child.get("number").equals(5);
        }
    }

    @Test
    @SuppressWarnings("FieldMayBeFinal")
    public void complexArrayTest() {
        class Child {
            int a = 1;
        }
        class Result {
            Child[] children = {new Child(), new Child()};
        }
        final Result result = new Result();
        result.children[0].a = 2;
        final Grammar grammar = new Grammar();
        final Map<String, Object> map = grammar.marshal(result);
        assert map != null;
        assert map.containsKey("children");
        if (!(map.get("children") instanceof List<?> list)) throw new GrammarException();
        assert list.size() == 2;
        assert list.get(0) instanceof Map<?, ?> child && (int) child.get("a") == 2;
        assert list.get(1) instanceof Map<?, ?> child && (int) child.get("a") == 1;
        final Result test = grammar.unmarshal(new Result(), Result.class, map);
        assert test != null;
        assert test.children != null;
        assert test.children.length == 2;
        assert test.children[0].a == 2;
        assert test.children[1].a == 1;
    }

    @Test
    public void noConstructorTest() {
        final Grammar grammar = new Grammar();
        class Thing {
            Thing(Void unused) {
            }
        }
        assert grammar.noSimplexConstructor(Thing.class);
        final Thing thing = grammar.createObject(Thing.class);
        assert thing != null;
    }

    @Test
    public void testAnyType() {
        class Bean {
            String name = "foo";
        }
        class Thing {
            @Any({int.class, String.class, Bean.class}) Object value;
        }
        final Grammar grammar = new Grammar();
        {
            final Map<String, Object> map = Map.of("value", 10);
            final Thing result = grammar.unmarshal(new Thing(), map);
            assert result.value != null;
            assert result.value instanceof Integer integer
                && integer == 10;
        }
        {
            final Map<String, Object> map = Map.of("value", "foo");
            final Thing result = grammar.unmarshal(new Thing(), map);
            assert result.value != null;
            assert result.value instanceof String string
                && string.equals("foo");
        }
        {
            final Map<String, Object> map = Map.of("value", Map.of("name", "blob"));
            final Thing result = grammar.unmarshal(new Thing(), map);
            assert result.value != null;
            assert result.value instanceof Bean bean
                && bean.name != null && bean.name.equals("blob");
        }
        {
            final Map<String, Object> map = Map.of("value", Map.of());
            final Thing result = grammar.unmarshal(new Thing(), map);
            assert result.value != null;
            assert result.value instanceof Bean bean
                && bean.name == null;
        }
    }

    @Test
    public void testPrimitiveArray() {
        class Thing {
            public double[] foo = new double[3];
        }
        class Thing2 {
            public Thing thing;
        }
        final Grammar grammar = new Grammar();
        {
            final Map<String, Object> map = Map.of("thing", Map.of("foo", List.of(0.5, 1.0)));
            final Thing2 result = grammar.unmarshal(new Thing2(), map);
            assert result.thing != null;
            assert result.thing.foo != null;
            assert result.thing.foo[0] == 0.5
                && result.thing.foo[1] == 1.0;
        }

    }

}
