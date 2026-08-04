package mx.kenzie.grammar.unwrap;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grammar.Container;
import mx.kenzie.grammar.Grammar;
import mx.kenzie.grammar.GrammarException;

import java.lang.annotation.Annotation;
import java.lang.constant.Constable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractClassUnwrapper<Type> implements Unwrapper<Type> {
    private static final Unwrap emptyUnwrap = unwrap(true, "", void.class, void.class);
    protected final Grammar.Access grammar;
    protected final Class<Type> type;
    protected final Map<String, Function<Type, Constable, GrammarException>> transformers;
    protected final Map<String, Function<Constable, Object, GrammarException>> detransformers;
    protected Function.Array<Object, Type, GrammarException> constructor;

    public AbstractClassUnwrapper(Grammar grammar, Class<Type> type) {
        this.grammar = grammar.new Access();
        this.type = type;
        checkTypeOk(type);

        // These need to be linked so that element order is preserved
        this.transformers = new LinkedHashMap<>();
        this.detransformers = new LinkedHashMap<>();
    }

    private static Unwrap merge(Unwrap base, Unwrap other) {
        boolean value = base.value() && other.value();
        String name = base.name().isBlank() ? other.name() : base.name();
        Class<?> marshalAs = base.marshalAs() == void.class ? other.marshalAs() : base.marshalAs();
        Class<?> componentType = base.componentType() == void.class ? other.componentType() : base.componentType();
        return unwrap(value, name, marshalAs, componentType);
    }

    private static Unwrap unwrap(boolean value, String name, Class<?> marshalAs, Class<?> componentType) {
        return new Unwrap() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Unwrap.class;
            }

            @Override
            public boolean value() {
                return value;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public Class<?> marshalAs() {
                return marshalAs;
            }

            @Override
            public Class<?> componentType() {
                return componentType;
            }
        };
    }

    protected Function<Constable, Object, GrammarException> createDetransformer(Unwrap unwrap) {
        Class<?> as = unwrap.marshalAs(), component = unwrap.componentType();
        Function<Constable, Object, GrammarException> function;
        if (as.isArray()) // noinspection unchecked
            function = data -> grammar.unmarshalArray(as, component, (Collection<Constable>) data);
        else if (Collection.class.isAssignableFrom(as)) //noinspection unchecked,rawtypes
            function = data -> grammar.unmarshalSeries(component, (Collection<Constable>) data, (Collection) grammar.create(as));
        else if (Map.class.isAssignableFrom(as)) //noinspection unchecked,rawtypes
            function = data -> grammar.unmarshalMap(component, (Container) data, (Map) grammar.create(as));
        else
            function = data -> grammar.unmarshal(as, data);
        return function;
    }

    protected Function<Type, Constable, GrammarException> createTransformer(Function<Type, Object, Throwable> getter, Unwrap unwrap) {
        Class<?> as = unwrap.marshalAs(), component = unwrap.componentType();
        Function<Type, Constable, Throwable> function;
        if (as.isArray())
            function = getter.andThen(object -> grammar.marshalArray(component, object));
        else if (Collection.class.isAssignableFrom(as)) //noinspection unchecked,rawtypes
            function = getter.andThen(object -> grammar.marshalCollection((Class) component, (Collection) object));
        else if (Map.class.isAssignableFrom(as)) //noinspection unchecked,rawtypes
            function = getter.andThen(object -> grammar.marshalMap(component, (Map) object));
        else
            function = getter.andThen(object -> grammar.marshal(as, object));
        return function.hide(GrammarException::new);
    }

    Unwrap unwrap(AnnotatedElement object) {
        Unwrap annotation = object.getAnnotation(Unwrap.class);
        Unwrap other = switch (object) {
            case Field field -> this.defaultUnwrapFrom(field);
            case RecordComponent component -> this.defaultUnwrapFrom(component);
            case Method method -> this.defaultUnwrapFrom(method);
            default -> throw new IllegalArgumentException("Unknown element type: " + object);
        };
        return merge(annotation != null ? annotation : emptyUnwrap, other);
    }

    Unwrap defaultUnwrapFrom(Field field) {
        Class<?> fieldType = field.getType();
        return unwrap(field.trySetAccessible(), field.getName(), fieldType, fieldType.isArray() ? fieldType.getComponentType() : void.class);
    }

    Unwrap defaultUnwrapFrom(RecordComponent field) {
        Class<?> fieldType = field.getType();
        return unwrap(true, field.getName(), fieldType, fieldType.isArray() ? fieldType.getComponentType() : void.class);
    }

    Unwrap defaultUnwrapFrom(Method method) {
        boolean setter = method.getParameterCount() == 1 && method.getReturnType() == void.class;
        Class<?> valueType = setter ? method.getParameterTypes()[0] : method.getReturnType();
        return unwrap(method.trySetAccessible(), method.getName(), valueType, valueType.isArray() ? valueType.getComponentType() : void.class);
    }

    protected abstract void checkTypeOk(Class<?> type);

}
