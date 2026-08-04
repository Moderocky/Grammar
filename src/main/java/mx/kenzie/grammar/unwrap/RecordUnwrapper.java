package mx.kenzie.grammar.unwrap;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Grammar;
import mx.kenzie.grammar.GrammarException;

import java.lang.constant.Constable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

public class RecordUnwrapper<Type extends Record> extends AbstractClassUnwrapper<Type> {

    protected final int length;
    protected final RecordComponent[] components;

    public RecordUnwrapper(Grammar grammar, Class<Type> type) throws GrammarException.StrategyException {
        super(grammar, type);
        this.components = type.getRecordComponents();

        this.length = components.length;

        Class<?>[] parameters = new Class<?>[length];
        for (int i = 0; i < length; i++) {
            final RecordComponent component = components[i];
            parameters[i] = component.getType();
            Unwrap unwrap = this.unwrap(component);
            Method accessor = component.getAccessor();
            accessor.trySetAccessible();
            Function<Type, Constable, GrammarException> transformer = this.createTransformer(accessor::invoke, unwrap);
            this.transformers.put(unwrap.name(), transformer);
            Function<Constable, Object, GrammarException> detransformer = this.createDetransformer(unwrap);
            this.detransformers.put(unwrap.name(), detransformer);
        }
        try {
            Constructor<? extends Record> found = type.getDeclaredConstructor(parameters);
            boolean result = found.trySetAccessible();
            assert result || found.canAccess(null);
            Function<Object[], Record, Throwable> newInstance = found::newInstance;
            this.constructor = newInstance.<Type>uncheckResult().hide(GrammarException.UnmarshallingException::new)::apply;
        } catch (NoSuchMethodException | SecurityException e) {
            throw new GrammarException.StrategyException("Cannot use canonical record constructor for " + type, e);
        }
    }

    @Override
    protected void checkTypeOk(Class<?> type) {
        if (type == null) throw new NullPointerException();
        if (!type.isRecord()) throw new IllegalArgumentException("Not a record type: " + type);
    }

    @Override
    public Function<Type, Constable, GrammarException> marshal() {
        return record -> {
            Container container = Container.empty();
            this.transformers.forEach((key, value) -> container.put(key, value.apply(record)));
            return container;
        };
    }

    @Override
    public Function<Constable, Type, GrammarException> unmarshal() {
        Function<Container, Type, GrammarException> function = container -> {
            List<Object> arguments = new ArrayList<>(length);
            for (final var entry : detransformers.entrySet()) {
                if (!container.containsKey(entry.getKey()))
                    throw new GrammarException("Data is missing key '" + entry.getKey() + "'");
                arguments.add(entry.getValue().apply(container.get(entry.getKey())));
            }
            return constructor.apply(arguments);
        };
        return function.compose(Grammar::assertIsContainer);
    }

}
