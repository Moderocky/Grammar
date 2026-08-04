package mx.kenzie.grammar;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grail.function.Supplier;

import java.lang.constant.Constable;

/// A helper class for building unmarshalling strategies from a series of functional
/// interface calls.
/// Type inference can be used to call the constructor directly rather than having to
/// fetch each object and cast them to the correct type.
public class Unmarshaller<Type> {

    //<editor-fold desc="Unmarshalling Helper" defaultstate="collapsed">

    final Class<Type> type;
    private final Grammar grammar;

    Unmarshaller(Grammar grammar, Class<Type> type) {
        this.grammar = grammar;
        this.type = type;
    }

    static <Argument> ArgumentExtractor<Argument> get(Grammar grammar, String key, Class<Argument> type) {
        return container -> grammar.unmarshal(type, container.get(key));
    }

    static <Argument> ArgumentExtractor<Argument> get(Grammar grammar, String key, Class<Argument> type, Argument defaultValue) {
        return container -> {
            Constable data = container.get(key);
            if (data == null) return defaultValue;
            return grammar.unmarshal(type, data);
        };
    }

    public void create(Supplier<Type, GrammarException> supplier) {
        Function<Container, Type, GrammarException> strategy = _ -> supplier.get();
        grammar.registerUnmarshallingStrategy(type, strategy.compose(Grammar::assertIsContainer));
    }

    public <Argument> For1<Argument> arg(ArgumentExtractor<Argument> extractor) {
        return new For1<>(extractor);
    }

    public <Argument> For1<Argument> get(String key, Class<Argument> type, Argument defaultValue) {
        return this.arg(Unmarshaller.get(grammar, key, type, defaultValue));
    }

    public <Argument> For1<Argument> get(String key, Class<Argument> type) {
        return this.arg(Unmarshaller.get(grammar, key, type));
    }


    public interface ArgumentExtractor<Argument> extends Function<Container, Argument, GrammarException> {
    }

    public final class For1<Arg1> {
        private final ArgumentExtractor<Arg1> arg1;

        For1(ArgumentExtractor<Arg1> arg1) {
            this.arg1 = arg1;
        }

        public <Argument> For2<Arg1, Argument> arg(ArgumentExtractor<Argument> extractor) {
            return new For2<>(arg1, extractor);
        }

        public <Argument> For2<Arg1, Argument> get(String key, Class<Argument> type, Argument defaultValue) {
            return this.arg(Unmarshaller.get(grammar, key, type, defaultValue));
        }

        public <Argument> For2<Arg1, Argument> get(String key, Class<Argument> type) {
            return this.arg(Unmarshaller.get(grammar, key, type));
        }

        public void create(For1.For1Func<Arg1, Type> constructor) {
            Function<Container, Type, GrammarException> strategy = container -> constructor.apply(arg1.apply(container));
            grammar.registerUnmarshallingStrategy(type, strategy.compose(Grammar::assertIsContainer));
        }

        public interface For1Func<Arg1, Result> {
            Result apply(Arg1 arg) throws GrammarException;
        }

    }

    public final class For2<Arg1, Arg2> {
        private final ArgumentExtractor<Arg1> arg1;
        private final ArgumentExtractor<Arg2> arg2;

        For2(ArgumentExtractor<Arg1> arg1, ArgumentExtractor<Arg2> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        public <Argument> For3<Arg1, Arg2, Argument> arg(ArgumentExtractor<Argument> extractor) {
            return new For3<>(arg1, arg2, extractor);
        }

        public <Argument> For3<Arg1, Arg2, Argument> get(String key, Class<Argument> type, Argument defaultValue) {
            return this.arg(Unmarshaller.get(grammar, key, type, defaultValue));
        }

        public <Argument> For3<Arg1, Arg2, Argument> get(String key, Class<Argument> type) {
            return this.arg(Unmarshaller.get(grammar, key, type));
        }

        public void create(For2.For2Func<Arg1, Arg2, Type> constructor) {
            Function<Container, Type, GrammarException> strategy = container -> constructor.apply(arg1.apply(container), arg2.apply(container));
            grammar.registerUnmarshallingStrategy(type, strategy.compose(Grammar::assertIsContainer));
        }


        public interface For2Func<Arg1, Arg2, Result> {
            Result apply(Arg1 arg1, Arg2 arg2) throws GrammarException;
        }

    }

    public final class For3<Arg1, Arg2, Arg3> {
        private final ArgumentExtractor<Arg1> arg1;
        private final ArgumentExtractor<Arg2> arg2;
        private final ArgumentExtractor<Arg3> arg3;

        For3(ArgumentExtractor<Arg1> arg1, ArgumentExtractor<Arg2> arg2, ArgumentExtractor<Arg3> arg3) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
        }

        public <Argument> For4<Arg1, Arg2, Arg3, Argument> arg(ArgumentExtractor<Argument> extractor) {
            return new For4<>(arg1, arg2, arg3, extractor);
        }

        public <Argument> For4<Arg1, Arg2, Arg3, Argument> get(String key, Class<Argument> type, Argument defaultValue) {
            return this.arg(Unmarshaller.get(grammar, key, type, defaultValue));
        }

        public <Argument> For4<Arg1, Arg2, Arg3, Argument> get(String key, Class<Argument> type) {
            return this.arg(Unmarshaller.get(grammar, key, type));
        }

        public void create(For3.For3Func<Arg1, Arg2, Arg3, Type> constructor) {
            Function<Container, Type, GrammarException> strategy = container -> constructor.apply(arg1.apply(container), arg2.apply(container), arg3.apply(container));
            grammar.registerUnmarshallingStrategy(type, strategy.compose(Grammar::assertIsContainer));
        }


        public interface For3Func<Arg1, Arg2, Arg3, Result> {
            Result apply(Arg1 arg1, Arg2 arg2, Arg3 arg3) throws GrammarException;
        }

    }

    public final class For4<Arg1, Arg2, Arg3, Arg4> {
        private final ArgumentExtractor<Arg1> arg1;
        private final ArgumentExtractor<Arg2> arg2;
        private final ArgumentExtractor<Arg3> arg3;
        private final ArgumentExtractor<Arg4> arg4;

        public For4(ArgumentExtractor<Arg1> arg1, ArgumentExtractor<Arg2> arg2, ArgumentExtractor<Arg3> arg3, ArgumentExtractor<Arg4> arg4) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
        }

        public <Argument> For5<Arg1, Arg2, Arg3, Arg4, Argument> arg(ArgumentExtractor<Argument> extractor) {
            return new For5<>(arg1, arg2, arg3, arg4, extractor);
        }

        public <Argument> For5<Arg1, Arg2, Arg3, Arg4, Argument> get(String key, Class<Argument> type, Argument defaultValue) {
            return this.arg(Unmarshaller.get(grammar, key, type, defaultValue));
        }

        public <Argument> For5<Arg1, Arg2, Arg3, Arg4, Argument> get(String key, Class<Argument> type) {
            return this.arg(Unmarshaller.get(grammar, key, type));
        }

        public void create(For4.For4Func<Arg1, Arg2, Arg3, Arg4, Type> constructor) {
            Function<Container, Type, GrammarException> strategy = container -> constructor.apply(arg1.apply(container), arg2.apply(container), arg3.apply(container), arg4.apply(container));
            grammar.registerUnmarshallingStrategy(type, strategy.compose(Grammar::assertIsContainer));
        }


        public interface For4Func<Arg1, Arg2, Arg3, Arg4, Result> {
            Result apply(Arg1 arg1, Arg2 arg2, Arg3 arg3, Arg4 arg4) throws GrammarException;
        }

    }

    public final class For5<Arg1, Arg2, Arg3, Arg4, Arg5> {
        private final ArgumentExtractor<Arg1> arg1;
        private final ArgumentExtractor<Arg2> arg2;
        private final ArgumentExtractor<Arg3> arg3;
        private final ArgumentExtractor<Arg4> arg4;
        private final ArgumentExtractor<Arg5> arg5;

        public For5(ArgumentExtractor<Arg1> arg1, ArgumentExtractor<Arg2> arg2, ArgumentExtractor<Arg3> arg3, ArgumentExtractor<Arg4> arg4, ArgumentExtractor<Arg5> arg5) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
        }


        public void create(For5.For5Func<Arg1, Arg2, Arg3, Arg4, Arg5, Type> constructor) {
            Function<Container, Type, GrammarException> strategy = container -> constructor.apply(arg1.apply(container), arg2.apply(container), arg3.apply(container), arg4.apply(container), arg5.apply(container));
            grammar.registerUnmarshallingStrategy(type, strategy.compose(Grammar::assertIsContainer));
        }


        public interface For5Func<Arg1, Arg2, Arg3, Arg4, Arg5, Result> {
            Result apply(Arg1 arg1, Arg2 arg2, Arg3 arg3, Arg4 arg4, Arg5 arg5) throws GrammarException;
        }

    }
    //</editor-fold>

}
