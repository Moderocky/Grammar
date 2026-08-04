package mx.kenzie.grammar;

import mx.kenzie.grammar.unwrap.ObjectUnwrapper;
import mx.kenzie.grammar.unwrap.Unwrap;
import org.junit.Test;

import java.lang.constant.Constable;

import static org.junit.Assert.*;

public class UnwrapTest {

    @Test
    public void recordSimple() {
        record Foo(@Unwrap(name = "foo") String a, @Unwrap(name = "bar") int b) {
        }
        Grammar grammar = new Grammar();
        grammar.registerRecord(Foo.class);
        Constable marshal = grammar.marshal(new Foo("a", 1));
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        assertEquals(2, container.size());
        assertFalse(container.containsKey("a"));
        assertFalse(container.containsKey("b"));
        assertTrue(container.containsKey("foo"));
        assertTrue(container.containsKey("bar"));
        assertEquals("a", container.get("foo"));
        assertEquals(1, container.<Integer>get("bar").intValue());
        Constable marshal1 = grammar.marshal(new Foo(null, -1));
        assertTrue(marshal1 instanceof Container);
        container = (Container) marshal1;
        assertEquals(2, container.size());
        assertFalse(container.containsKey("a"));
        assertFalse(container.containsKey("b"));
        assertTrue(container.containsKey("foo"));
        assertTrue(container.containsKey("bar"));
        assertEquals(Null.INSTANCE, container.get("foo"));
        assertEquals(-1, container.<Integer>get("bar").intValue());
    }

    @Test
    public void object() {
        class Bar {
            @Unwrap
            String pqr;
        }
        class Foo {
            @Unwrap(name = "test")
            String name;
            @Unwrap(marshalAs = int.class)
            Integer age;
            Bar bar;
            @Unwrap(marshalAs = Integer.class, name = "blob")
            int foo;
        }
        Grammar grammar = new Grammar();
        grammar.register(Foo.class, new ObjectUnwrapper<>(grammar, Foo.class));
        grammar.registerUncheckedObject(Bar.class);

        Foo foo = new Foo();
        foo.age = 42;
        foo.bar = new Bar();
        foo.bar.pqr = "test";
        foo.name = "hello";
        foo.foo = -4;

        Constable marshal = grammar.marshal(foo);
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        assertEquals(4, container.size());
        assertFalse(container.containsKey("foo"));
        assertFalse(container.containsKey("name"));
        assertTrue(container.containsKey("test"));
        assertTrue(container.containsKey("blob"));
        assertTrue(container.containsKey("age"));
        assertTrue(container.containsKey("bar"));
        Foo object = grammar.unmarshal(Foo.class, container);

        assertNotNull(object);
        assertEquals(42, (int) object.age);
        assertNotNull(object.bar);
        assertEquals("test", object.bar.pqr);
        assertEquals("hello", object.name);
        assertEquals(-4, object.foo);
    }

}