package mx.kenzie.grammar;

import org.junit.Test;

import java.lang.constant.Constable;

import static org.junit.Assert.*;

public class GrammarTest {

    @Test
    public void testGrammar() {
        Grammar grammar = new Grammar();
        assertEquals(5, grammar.marshal(5));
        assertEquals("t", grammar.marshal("t"));
        assertSame(Null.INSTANCE, grammar.marshal(null));
    }

    @Test
    public void testPrimitiveTypesCanBeMarshalled() {
        Grammar grammar = new Grammar();
        assertEquals(5, grammar.marshal(5));
        assertEquals((byte) 1, grammar.marshal((byte) 1));
        assertEquals((short) 1, grammar.marshal((short) 1));
        assertEquals((long) 1, grammar.marshal((long) 1));
        assertEquals((float) 1, grammar.marshal((float) 1));
        assertEquals((double) 1, grammar.marshal((double) 1));
        assertEquals('c', grammar.marshal('c'));
        assertEquals(true, grammar.marshal(true));
        assertEquals("t", grammar.marshal("t"));
    }

    @Test(expected = GrammarException.class)
    public void testCannotMarshal() {
        class Foo {
        }
        Grammar grammar = new Grammar();
        grammar.marshal(new Foo());
    }

    @Test(expected = GrammarException.class)
    public void testCannotUnmarshal() {
        class Foo {
        }
        Grammar grammar = new Grammar();
        grammar.unmarshal(Foo.class, "t");
    }

    @Test
    public void testUnsafeMarshal() {
        class Foo {
            String bar;
            int baz;
        }

        Grammar grammar = new Grammar.Unsafe();
        Foo object = new Foo();
        object.bar = "bar";
        object.baz = 123;
        Constable marshal = grammar.marshal(object);
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        assertFalse(container.isEmpty());
        assertEquals(2, container.size());
        assertTrue(container.containsKey("bar"));
        assertTrue(container.containsKey("baz"));
        assertEquals("bar", container.get("bar"));
        assertEquals(123, (int) container.get("baz"));
    }

    @Test
    public void testUnsafeUnmarshal() {
        class Foo {
            String bar;
            int baz;
        }

        Grammar grammar = new Grammar.Unsafe();
        Container container = Container.of("bar", "blob", "baz", -128);
        Foo unmarshalled = grammar.unmarshal(Foo.class, container);
        assertNotNull(unmarshalled);
        assertEquals("blob", unmarshalled.bar);
        assertEquals(-128, unmarshalled.baz);

    }

    @Test
    public void testUnsafeBothWays() {
        class Foo {
            String bar;
            int baz;
        }

        Grammar grammar = new Grammar.Unsafe();
        Foo object = new Foo();
        object.bar = "bar";
        object.baz = 123;
        Constable marshal = grammar.marshal(object);
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        assertFalse(container.isEmpty());
        assertEquals(2, container.size());
        assertTrue(container.containsKey("bar"));
        assertTrue(container.containsKey("baz"));
        assertEquals("bar", container.get("bar"));
        assertEquals(123, (int) container.get("baz"));
        grammar.unmarshal(Foo.class, marshal);
        Foo unmarshalled = grammar.unmarshal(Foo.class, container);
        assertNotNull(unmarshalled);
        assertEquals("bar", unmarshalled.bar);
        assertEquals(123, unmarshalled.baz);
        assertNotSame(object, unmarshalled);

    }

}