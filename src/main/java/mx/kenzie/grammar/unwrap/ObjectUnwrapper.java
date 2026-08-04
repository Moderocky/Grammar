package mx.kenzie.grammar.unwrap;

import mx.kenzie.grail.function.BiConsumer;
import mx.kenzie.grail.function.Function;
import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Grammar;
import mx.kenzie.grammar.GrammarException;

import java.lang.constant.Constable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ObjectUnwrapper<Type> extends AbstractClassUnwrapper<Type> {

    protected final Map<String, BiConsumer<Type, Object, Throwable>> fieldSetters;

    public ObjectUnwrapper(Grammar grammar, Class<Type> type) {
        super(grammar, type);

        this.fieldSetters = new HashMap<>();
        for (Field field : this.getEffectiveFields(type)) {
            Unwrap unwrap = this.unwrap(field);
            if (!unwrap.value()) continue;
            Function<Type, Constable, GrammarException> transformer = this.createTransformer(field::get, unwrap);
            this.transformers.put(unwrap.name(), transformer);
            Function<Constable, Object, GrammarException> detransformer = this.createDetransformer(unwrap);
            this.detransformers.put(unwrap.name(), detransformer);
            this.fieldSetters.put(unwrap.name(), field::set);
        }

    }

    public static boolean isSuitable(Class<?> type) {
        return type != null && !type.isArray() && !type.isRecord() && !type.isPrimitive() && !type.isEnum() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers());
    }

    @Override
    public Function<Type, Constable, GrammarException> marshal() {
        return object -> {
            Container container = Container.empty();
            this.transformers.forEach((key, value) -> container.put(key, value.apply(object)));
            return container;
        };
    }

    @Override
    public Function<Constable, Type, GrammarException> unmarshal() {
        Function<Container, Type, Throwable> function = container -> {
            Type object = grammar.create(type);

            for (final var entry : detransformers.entrySet()) {
                String key = entry.getKey();
                Constable argument = container.get(key);
                if (argument == null) continue;
                if (!container.containsKey(key)) continue;
                BiConsumer<Type, Object, Throwable> setter = fieldSetters.get(key);
                Object value = entry.getValue().apply(argument);
                setter.accept(object, value);
            }
            return object;
        };
        return function.compose(Grammar::assertIsContainer).hide(GrammarException::new).uncheckArgument();
    }

    protected Collection<Field> getEffectiveFields(Class<?> type) {
        if (type == null) return Collections.emptySet();
        Set<Field> fields = new LinkedHashSet<>();
        fields.addAll(List.of(type.getFields()));
        fields.addAll(List.of(type.getDeclaredFields()));
        fields.removeIf(this::shouldSkipDuringSerialisation);
        fields.addAll(this.getEffectiveFields(type.getSuperclass()));
        return fields;
    }

    protected boolean shouldSkipDuringSerialisation(Field field) {
        if (Modifier.isFinal(field.getModifiers()) && field.getName().startsWith("this$")) {
            // this field is typically synthetic and added by the compiler to inner classes
            return true;
        }
        return Modifier.isStatic(field.getModifiers()) || Modifier.isPrivate(field.getModifiers()) || Modifier.isTransient(field.getModifiers());
    }

    @Override
    protected void checkTypeOk(Class<?> type) {
        if (type == null) throw new NullPointerException();
        if (type.isRecord()) throw new IllegalArgumentException("Cannot unwrap record type " + type);
        if (type.isPrimitive()) throw new IllegalArgumentException("Cannot unwrap primitive type " + type);
        if (type.isArray()) throw new IllegalArgumentException("Cannot unwrap array type " + type);
        if (type.isEnum()) throw new IllegalArgumentException("Cannot unwrap enum type " + type);
        if (type.isInterface()) throw new IllegalArgumentException("Cannot unwrap interface type " + type);
    }

}
