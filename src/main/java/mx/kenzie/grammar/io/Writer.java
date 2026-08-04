package mx.kenzie.grammar.io;

import mx.kenzie.grammar.Container;
import org.jetbrains.annotations.NotNull;

import java.lang.constant.Constable;
import java.util.Collection;
import java.util.Iterator;

public interface Writer<Output, WritingProblem extends Throwable> {

    void writeValue(Output output, @NotNull Constable value) throws WritingProblem;

    void writeSeriesOpen(Output output) throws WritingProblem;

    void writeSeriesAnd(Output output, boolean hasFollowing) throws WritingProblem;

    default void writeSeriesValue(Output output, @NotNull Constable value) throws WritingProblem {
        this.writeValue(output, value);
    }

    void writeSeriesClose(Output output) throws WritingProblem;

    default void writeSeries(Output output, Collection<@NotNull Constable> serial) throws WritingProblem {
        Iterator<@NotNull Constable> iterator = serial.iterator();
        this.writeSeriesOpen(output);
        while (iterator.hasNext()) {
            Constable next = iterator.next();
            this.writeSeriesValue(output, next);
            this.writeSeriesAnd(output, iterator.hasNext());
        }
        this.writeSeriesClose(output);
    }

    void writeContainerOpen(Output output) throws WritingProblem;

    void writeContainerKey(Output output, String key) throws WritingProblem;

    default void writeContainerValue(Output output, Constable value) throws WritingProblem {
        this.writeValue(output, value);
    }

    void writeContainerSeparator(Output output) throws WritingProblem;

    default void writeContainerPair(Output output, String key, @NotNull Constable value) throws WritingProblem {
        this.writeContainerKey(output, key);
        this.writeContainerSeparator(output);
        this.writeContainerValue(output, value);
    }

    void writeContainerAnd(Output output, boolean hasFollowing) throws WritingProblem;

    void writeContainerClose(Output output) throws WritingProblem;

    default void writeContainer(Output output, Container container) throws WritingProblem {
        final var iterator = container.entrySet().iterator();
        this.writeContainerOpen(output);
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final String key = entry.getKey();
            final Constable value = entry.getValue();
            this.writeContainerPair(output, key, value);
            this.writeContainerAnd(output, iterator.hasNext());
        }
        this.writeContainerClose(output);
    }

}
