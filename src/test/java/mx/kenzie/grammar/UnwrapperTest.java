package mx.kenzie.grammar;

import mx.kenzie.grammar.unwrap.ObjectUnwrapper;
import org.junit.Test;

import java.lang.constant.Constable;

import static org.junit.Assert.*;

public class UnwrapperTest {

    @Test
    public void recordSimple() {
        record Foo(String a, int b) {
        }
        Grammar grammar = new Grammar();
        grammar.registerRecord(Foo.class);
        grammar.marshal(new Foo("a", 1));
        grammar.marshal(new Foo(null, 1));
    }

    @Test(expected = GrammarException.class)
    public void recordNotRegistered() {
        record Foo(String a, int b) {
        }
        Grammar grammar = new Grammar();
        grammar.marshal(new Foo("a", 1));
    }

    @Test
    public void recordToContainer() {
        record Foo(String a, int b) {
        }
        Grammar grammar = new Grammar();
        grammar.registerRecord(Foo.class);
        Constable marshalled = grammar.marshal(new Foo("aaa", 1));
        assertNotNull(marshalled);
        Foo foo = grammar.unmarshal(Foo.class, marshalled);
        assertNotNull(foo);
        assertEquals(1, foo.b);
        assertEquals("aaa", foo.a);

        marshalled = grammar.marshal(new Foo(null, -10));
        assertNotNull(marshalled);
        foo = grammar.unmarshal(Foo.class, marshalled);
        assertEquals(-10, foo.b);
        assertNull(foo.a);
    }

    @Test
    public void enumName() {
        enum Foo {
            FOO, BAR, BAZ
        }
        Grammar grammar = new Grammar();
        grammar.registerEnum(Foo.class);
        Constable marshal = grammar.marshal(Foo.FOO);
        assertTrue(marshal instanceof String);
        Foo unmarshalled = grammar.unmarshal(Foo.class, marshal);
        assertEquals(Foo.FOO, unmarshalled);

        marshal = grammar.marshal(Foo.BAZ);
        assertTrue(marshal instanceof String);
        unmarshalled = grammar.unmarshal(Foo.class, marshal);
        assertEquals(Foo.BAZ, unmarshalled);
    }

    @Test
    public void enumOrdinal() {
        enum Foo {
            FOO, BAR, BAZ
        }
        Grammar grammar = new Grammar();
        grammar.registerEnumByOrdinal(Foo.class);
        Constable marshal = grammar.marshal(Foo.FOO);
        assertTrue(marshal instanceof Integer);
        Foo unmarshalled = grammar.unmarshal(Foo.class, marshal);
        assertEquals(Foo.FOO, unmarshalled);

        marshal = grammar.marshal(Foo.BAZ);
        assertTrue(marshal instanceof Integer);
        unmarshalled = grammar.unmarshal(Foo.class, marshal);
        assertEquals(Foo.BAZ, unmarshalled);
    }

    @Test(expected = IllegalArgumentException.class)
    public void objectNotEnum() {
        enum Foo {
        }
        Grammar grammar = new Grammar();
        grammar.register(Foo.class, new ObjectUnwrapper<>(grammar, Foo.class));
    }

    @Test
    public void object() {
        class Foo {

        }
        Grammar grammar = new Grammar();
        grammar.register(Foo.class, new ObjectUnwrapper<>(grammar, Foo.class));

    }

}