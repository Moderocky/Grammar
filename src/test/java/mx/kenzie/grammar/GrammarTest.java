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

}
