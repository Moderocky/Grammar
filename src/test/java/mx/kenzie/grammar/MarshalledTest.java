package mx.kenzie.grammar;

import org.junit.Test;

import java.lang.constant.Constable;

import static org.junit.Assert.*;

public class MarshalledTest {

    @Test
    public void marshal() {
        class Foo implements Marshalled {
            int x;
            String y;

            @Override
            public Container marshal() throws GrammarException {
                return Container.of("x", x, "y", y);
            }
        }
        Grammar grammar = new Grammar();
        Foo foo = new Foo();
        foo.x = -20;
        foo.y = "test";
        Constable marshal = grammar.marshal(foo);
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        assertFalse(container.isEmpty());
        assertEquals(2, container.size());
        assertEquals(-20, (int) container.get("x"));
        assertEquals("test", container.get("y"));
    }

    @Test
    public void unmarshalNoRegistration() {
        class Foo implements Marshalled, Marshalled.Unmarshalled {
            int x;
            String y;

            @Override
            public Container marshal() throws GrammarException {
                return Container.of("x", x, "y", y);
            }

            @Override
            public void unmarshal(Container data) throws GrammarException {
                x = data.get("x");
                y = data.get("y");
            }
        }
        Grammar grammar = new Grammar();
        Foo foo = new Foo();
        foo.x = -20;
        foo.y = "test";
        Constable marshal = grammar.marshal(foo);
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        assertFalse(container.isEmpty());
        Foo unmarshalled = grammar.unmarshal(Foo.class, container);
        assertNotNull(unmarshalled);
        assertNotSame(foo, unmarshalled);
        assertEquals(unmarshalled.x, foo.x);
        assertEquals(unmarshalled.y, foo.y);
    }


    @Test
    public void unmarshalWithCreatorRegistration() {
        class Foo implements Marshalled, Marshalled.Unmarshalled {
            int x;
            String y;

            @Override
            public Container marshal() throws GrammarException {
                return Container.of("x", x, "y", y);
            }

            @Override
            public void unmarshal(Container data) throws GrammarException {
                x = data.get("x");
                y = data.get("y");
            }
        }
        Grammar grammar = new Grammar();
        grammar.registerConstructor(Foo.class, Foo::new);
        Foo foo = new Foo();
        foo.x = -20;
        foo.y = "test";
        Constable marshal = grammar.marshal(foo);
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        assertFalse(container.isEmpty());
        Foo unmarshalled = grammar.unmarshal(Foo.class, container);
        assertNotNull(unmarshalled);
        assertNotSame(foo, unmarshalled);
        assertEquals(unmarshalled.x, foo.x);
        assertEquals(unmarshalled.y, foo.y);
    }


    @Test
    public void unmarshalWithStrategy() {
        record Foo(int x, String y) implements Marshalled {

            @Override
                    public Container marshal() throws GrammarException {
                        return Container.of("x", x, "y", y);
                    }
                }
        Grammar grammar = new Grammar();
        grammar.createUnmarshallingStrategy(Foo.class)
                .get("x", int.class)
                .get("y", String.class)
                .create(Foo::new);
        Foo foo = new Foo(-20, "test");
        Constable marshal = grammar.marshal(foo);
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        assertFalse(container.isEmpty());
        Foo unmarshalled = grammar.unmarshal(Foo.class, container);
        assertNotNull(unmarshalled);
        assertNotSame(foo, unmarshalled);
        assertEquals(unmarshalled.x, foo.x);
        assertEquals(unmarshalled.y, foo.y);
    }


    @Test(expected = GrammarException.class)
    public void unmarshalNotRegisteredFailure() {
        class Foo implements Marshalled {
            int x;
            String y;

            @Override
            public Container marshal() throws GrammarException {
                return Container.of("x", x, "y", y);
            }
        }
        Grammar grammar = new Grammar();
        Foo foo = new Foo();
        Constable marshal = grammar.marshal(foo);
        assertTrue(marshal instanceof Container);
        Container container = (Container) marshal;
        Foo _ = grammar.unmarshal(Foo.class, container);
    }

}