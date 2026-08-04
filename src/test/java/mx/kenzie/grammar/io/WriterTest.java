package mx.kenzie.grammar.io;

import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Null;
import mx.kenzie.grammar.Series;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.valross.constantine.Array;

import java.lang.constant.Constable;

import static org.junit.Assert.assertEquals;

public class WriterTest {

    @Test
    public void simple() {
        //<editor-fold desc="Test" defaultstate="collapsed">
        class JsonWriter implements Writer<StringBuilder, RuntimeException> {

            @Override
            public void writeValue(StringBuilder builder, @NotNull Constable value) throws RuntimeException {
                switch (value) {
                    case String string -> builder.append('"').append(string).append('"');
                    case Number number -> builder.append(number);
                    case Boolean b -> builder.append(b);
                    case Character character -> builder.append('"').append(character).append('"');
                    case Null _ -> builder.append("null");
                    case Array array -> this.writeSeries(builder, array);
                    case Container container -> this.writeContainer(builder, container);
                    default -> throw new IllegalStateException("Unexpected value: " + value);
                }
            }

            @Override
            public void writeSeriesOpen(StringBuilder builder) throws RuntimeException {
                builder.append('[');
            }

            @Override
            public void writeSeriesAnd(StringBuilder builder, boolean hasFollowing) throws RuntimeException {
                if (hasFollowing) builder.append(',');
            }

            @Override
            public void writeSeriesClose(StringBuilder builder) throws RuntimeException {
                builder.append(']');
            }

            @Override
            public void writeContainerOpen(StringBuilder builder) throws RuntimeException {
                builder.append('{');
            }

            @Override
            public void writeContainerKey(StringBuilder builder, String key) throws RuntimeException {
                builder.append('"').append(key).append('"');
            }

            @Override
            public void writeContainerSeparator(StringBuilder builder) throws RuntimeException {
                builder.append(':');
            }

            @Override
            public void writeContainerAnd(StringBuilder builder, boolean hasFollowing) throws RuntimeException {
                if (hasFollowing) builder.append(',');
            }

            @Override
            public void writeContainerClose(StringBuilder builder) throws RuntimeException {
                builder.append('}');
            }
        }
        //</editor-fold>
        JsonWriter jsonWriter = new JsonWriter();
        StringBuilder builder = new StringBuilder();
        Container.Frozen container = Container.of("foo bar", "foo baz", "b", 23.5, "test", Series.of(), "test 2", Series.of("foom", Null.INSTANCE, 1));
        jsonWriter.writeContainer(builder, container);
        assertEquals("{\"foo bar\":\"foo baz\",\"b\":23.5,\"test\":[],\"test 2\":[\"foom\",null,1]}", builder.toString());
    }

}