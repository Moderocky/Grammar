package mx.kenzie.grammar;

import org.junit.Test;


public class UnmarshallerTest {

    @Test
    public void testCreate() {
        record Foo(String a, int b) {
        }
        Grammar grammar = new Grammar();
        new Unmarshaller<>(grammar, Foo.class)
                .get("a", String.class)
                .get("b", int.class)
                .create(Foo::new);
        grammar.registerMarshallingStrategy(Foo.class, foo -> Container.of("a", foo.a, "b", foo.b));
    }
}