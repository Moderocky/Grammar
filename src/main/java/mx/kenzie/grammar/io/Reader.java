package mx.kenzie.grammar.io;

import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Series;

import java.lang.constant.Constable;

public interface Reader<Input, ReadingProblem extends Throwable> {

    Constable readValue(Input input) throws ReadingProblem;

    void look(Input input) throws ReadingProblem;

    Series readSeries(Input input) throws ReadingProblem;

    void readSeriesOpen(Input input) throws ReadingProblem;

    default Constable readSeriesValue(Input input) throws ReadingProblem {
        return readValue(input);
    }

    default void readSeriesValue(Input input, Series series) throws ReadingProblem {
        series.add(this.readSeriesValue(input));
    }

    void readSeriesAnd(Input input) throws ReadingProblem;

    void readSeriesClose(Input input) throws ReadingProblem;

    Container readContainer(Input input) throws ReadingProblem;

    void readContainerOpen(Input input) throws ReadingProblem;

    String readContainerKey(Input input) throws ReadingProblem;

    default Constable readContainerValue(Input input) throws ReadingProblem {
        return this.readValue(input);
    }

    void readContainerSeparator(Input input) throws ReadingProblem;

    default void readContainerPair(Input input, Container container) throws ReadingProblem {
        String key = this.readContainerKey(input);
        this.readContainerSeparator(input);
        Constable value = readContainerValue(input);
        container.put(key, value);
    }

    void readContainerAnd(Input input) throws ReadingProblem;

    void readContainerClose(Input input) throws ReadingProblem;


}
