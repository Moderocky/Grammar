package mx.kenzie.grammar;

import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public class Grammar {

    private static final Map<Class<?>, Constructor<?>> constructors = new WeakHashMap<>();

    protected Map<String, Object> marshal(Object object) {
        return this.marshal(object, object.getClass(), new LinkedHashMap<>());
    }

    protected <Type, Container extends Map<String, Object>> Container marshal(Object object, Class<Type> type, Container container) {

    }

    protected <Type, Container extends Map<?, ?>> Type unmarshal(Type object, Class<?> type, Container container) {
        assert object != null : "Object was null.";
        assert object instanceof Class<?> ^ true : "Classes cannot be written to.";
        final Set<Field> fields = new HashSet<>();
        fields.addAll(List.of(type.getDeclaredFields()));
        fields.addAll(List.of(type.getFields()));
        for (final Field field : fields) {
            final int modifiers = field.getModifiers();
            if ((modifiers & 0x00000002) != 0) continue;
            if ((modifiers & 0x00000008) != 0) continue;
            if ((modifiers & 0x00000080) != 0) continue;
            if ((modifiers & 0x00001000) != 0) continue;
            final String key = this.getName(field);
            if (!container.containsKey(key)) continue;
            if (!field.canAccess(object)) field.trySetAccessible();
            final Object value = container.get(key);
            final Class<?> expected = field.getType();
            try {
                this.map(object, field, expected, value);
            } catch (Throwable ex) {
                throw new RuntimeException("Unable to write to object:", ex);
            }
        }
        return object;
    }

    private void map(Object source, Field field, Class<?> expected, Object value) throws IllegalAccessException {

    }

    private String getName(Field field) {
        if (field.isAnnotationPresent(Name.class)) return field.getAnnotation(Name.class).value();
        else return field.getName();
    }

    @SuppressWarnings("all")
    private Object createEnum(Class<?> type, Object value) {
        return Enum.valueOf((Class) type, value.toString());
    }

    @SuppressWarnings("unchecked")
    private <Type> Constructor<Type> createConstructor(Class<Type> type) throws NoSuchMethodException {
        final Constructor<?> shift = Object.class.getConstructor();
        return (Constructor<Type>) ReflectionFactory.getReflectionFactory().newConstructorForSerialization(type, shift);
    }

    @SuppressWarnings("unchecked")
    private <Type> Constructor<Type> getConstructor(Class<Type> type) throws NoSuchMethodException {
        if (constructors.containsKey(type)) return (Constructor<Type>) constructors.get(type);
        if (type.isLocalClass() || type.getEnclosingClass() != null) {
            final Constructor<Type> constructor = this.createConstructor(type);
            assert constructor != null;
            Grammar.constructors.put(type, constructor);
            return constructor;
        } else {
            final Constructor<Type> constructor = type.getDeclaredConstructor();
            Grammar.constructors.put(type, constructor);
            final boolean result = constructor.trySetAccessible();
            assert result || constructor.canAccess(null);
            return constructor;
        }
    }

    protected <Type> Type createObject(Class<Type> type) {
        try {
            final Constructor<Type> constructor = this.getConstructor(type);
            return constructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Unable to create '" + type.getSimpleName() + "' object.", e);
        }
    }

}
