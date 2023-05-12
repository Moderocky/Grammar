package mx.kenzie.grammar;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
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

}
