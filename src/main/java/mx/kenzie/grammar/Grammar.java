package mx.kenzie.grammar;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sun.reflect.ReflectionFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

@SuppressWarnings("TypeParameterHidesVisibleType")
public class Grammar {

    private static final Map<Class<?>, Constructor<?>> constructors = new WeakHashMap<>();

    /**
     * Extracts the relevant data from an object's fields into a map of key-value pairs.
     *
     * @param object The object whose data should be unwrapped
     * @return A map containing the data
     */
    protected Map<String, Object> marshal(Object object) {
        return this.marshal(object, object.getClass(), new LinkedHashMap<>());
    }

    /**
     * Creates an object, and inserts data from a map of key-value pairs into an object's fields.
     * The {@param object} is returned.
     *
     * @param type The type to use for object construction
     * @param container The container from which to read the data
     * @return The new object
     * @param <Type> The object's type
     * @param <Container> The container type
     */
    @Contract("null, null -> fail")
    protected <Type, Container extends Map<?, ?>> Type unmarshal(Class<Type> type, Container container) {
        if (type.isRecord()) return this.createRecord(type, container);
        final Type object = this.createObject(type);
        this.unmarshal(object, type, container);
        return object;
    }

    /**
     * Inserts data from a map of key-value pairs into an object's fields.
     * The {@param object} is returned.
     *
     * @param object The object whose data is to be overwritten
     * @param container The container from which to read the data
     * @return The object, having been written to
     * @param <Type> The object's type
     * @param <Container> The container type
     */
    @Contract("null, null -> fail; _, _ -> param1")
    protected <Type, Container extends Map<?, ?>> Type unmarshal(Type object, Container container) {
        this.unmarshal(object, object.getClass(), container);
        return object;
    }

    /**
     * Extracts the relevant data from an object's fields into a map of key-value pairs.
     * The {@param container} is returned.
     *
     * @param object The object whose data is to be marshalled
     * @param type The type to use for data extraction (a supertype of {@param object})
     * @param container The container in which to store the data
     * @return The {@param container} with the data added
     * @param <Type> The type to marshal the object as
     * @param <Container> The container type
     */
    @Contract("null, null, null -> fail; _, _, _ -> param3")
    protected <Type, Container extends Map<String, Object>>
    Container marshal(Object object, Class<Type> type, Container container) {
        //<editor-fold desc="Getter classes" defaultstate="collapsed">
        interface Getter extends AnnotatedElement {

            Object get() throws IllegalAccessException, InvocationTargetException;

            int modifiers();

            Class<?> getType();

            String getName();

        }
        record RecordGetter(RecordComponent component, Method accessor, Object object) implements Getter {

            @Override
            public Object get() throws IllegalAccessException, InvocationTargetException {
                return accessor.invoke(object);
            }

            @Override
            public boolean isAnnotationPresent(@NotNull Class<? extends Annotation> annotation) {
                return component.isAnnotationPresent(annotation);
            }

            @Override
            public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
                return component.getAnnotation(annotationClass);
            }

            @Override
            public Annotation[] getAnnotations() {
                return component.getAnnotations();
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return component.getDeclaredAnnotations();
            }

            @Override
            public int modifiers() {
                return accessor.getModifiers();
            }

            @Override
            public Class<?> getType() {
                return component.getType();
            }

            @Override
            public String getName() {
                return component.getName();
            }

        }
        record FieldGetter(Field field, Object object) implements Getter {

            @Override
            public Object get() throws IllegalAccessException, InvocationTargetException {
                return field.get(object);
            }

            @Override
            public boolean isAnnotationPresent(@NotNull Class<? extends Annotation> annotation) {
                return field.isAnnotationPresent(annotation);
            }

            @Override
            public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
                return field.getAnnotation(annotationClass);
            }

            @Override
            public Annotation[] getAnnotations() {
                return field.getAnnotations();
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return field.getDeclaredAnnotations();
            }

            @Override
            public int modifiers() {
                return field.getModifiers();
            }

            @Override
            public Class<?> getType() {
                return field.getType();
            }

            @Override
            public String getName() {
                return field.getName();
            }

        }
        //</editor-fold>
        //<editor-fold desc="Object to Map" defaultstate="collapsed">
        assert object != null : "Object was null.";
        if (object instanceof Marshalled marshalled) {
            container.putAll(marshalled.serialise());
            return container;
        }
        final Set<Getter> fields = new HashSet<>();
        if (type.isRecord()) for (RecordComponent component : type.getRecordComponents()) {
            final Method accessor = component.getAccessor();
            if (this.shouldSkip(accessor.getModifiers())) continue;
            if (!accessor.canAccess(object)) accessor.trySetAccessible();
            fields.add(new RecordGetter(component, accessor, object));
        }
        else {
            for (Field field : type.getFields()) {
                if (this.shouldSkip(field)) continue;
                if (!field.canAccess(object)) field.trySetAccessible();
                fields.add(new FieldGetter(field, object));
            }
            for (Field field : type.getDeclaredFields()) {
                if (this.shouldSkip(field)) continue;
                if (!field.canAccess(object)) field.trySetAccessible();
                fields.add(new FieldGetter(field, object));
            }
        }
        for (final Getter field : fields) {
            try {
                final Object value = field.get();
                if (value == null && field.isAnnotationPresent(Optional.class)) continue;
                final Class<?> expected = field.getType();
                final String key = this.getName(field, field.getName());
                if (key.equals("__data")) continue;
                container.put(key, this.deconstruct(value, expected, field.isAnnotationPresent(Any.class)));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new GrammarException("Unable to read data '" + type.getSimpleName() + '.' + field + "' from " +
                    "object:", ex);
            }
        }
        return container;
        //</editor-fold>
    }

    /**
     * Inserts data from a map of key-value pairs into an object's fields.
     * The {@param object} is returned.
     *
     * @param object The object whose data is to be overwritten
     * @param type The type to use for data injection (a supertype of {@param object})
     * @param container The container from which to read the data
     * @return The object, having been written to
     * @param <Type> The object's type
     * @param <Container> The container type
     */
    @Contract("null, null, null -> fail; _, _, _ -> param1")
    @SuppressWarnings("unchecked")
    protected <Type, Container extends Map<?, ?>> Type unmarshal(Type object, Class<?> type, Container container) {
        //<editor-fold desc="Map to Object" defaultstate="collapsed">
        assert object != null : "Object was null.";
        assert !(object instanceof Class<?>) : "Classes cannot be written to.";
        if (object instanceof Marshalled marshalled) {
            marshalled.deserialise((Map<String, Object>) container);
            return object;
        }
        if (type.isRecord()) throw new GrammarException("Data cannot be written to an existing Record object.");
        final Set<Field> fields = new HashSet<>();
        fields.addAll(List.of(type.getDeclaredFields()));
        fields.addAll(List.of(type.getFields()));
        for (final Field field : fields) {
            final String key = this.getName(field);
            if (key.equals("__data")) try {
                if (!field.canAccess(object)) field.trySetAccessible();
                assert Map.class.isAssignableFrom(field.getType()) : "Dataset field must accept map.";
                final Map<String, Object> initial = (Map<String, Object>) field.get(object);
                if (initial != null) initial.putAll((Map<? extends String, ?>) container);
                else field.set(object, new LinkedHashMap<>(container));
                continue;
            } catch (IllegalAccessException ex) {
                throw new GrammarException("Unable to store dataset object.", ex);
            }
            if (this.shouldSkip(field)) continue;
            if (!container.containsKey(key)) continue;
            if (!field.canAccess(object)) field.trySetAccessible();
            final Object value = container.get(key);
            final Class<?> expected = field.getType();
            try {
                this.prepareFieldValue(object, field, expected, this.construct(value, expected));
            } catch (Throwable ex) {
                throw new GrammarException("Unable to write to object:", ex);
            }
        }
        return object;
        //</editor-fold>
    }

    /**
     * Whether {@param field} should be skipped when marshalling.
     */
    protected boolean shouldSkip(Field field) {
        return this.shouldSkip(field.getModifiers());
    }

    protected boolean shouldSkip(int modifiers) {
        if ((modifiers & 0x00000002) != 0) return true;
        if ((modifiers & 0x00000008) != 0) return true;
        if ((modifiers & 0x00000080) != 0) return true;
        return (modifiers & 0x00001000) != 0;
    }

    /**
     * Unmarshalls simple objects into the correct type to be inserted into a field.
     */
    @SuppressWarnings("RawUseOfParameterized")
    protected void prepareFieldValue(Object source, Field field, Class<?> expected, Object value)
        throws IllegalAccessException {
        //<editor-fold desc="Set Field Value" defaultstate="collapsed">
        if (expected.isPrimitive()) {
            if (value instanceof Boolean boo) field.setBoolean(source, boo);
            else if (value instanceof Number number) {
                if (expected == byte.class) field.setByte(source, number.byteValue());
                else if (expected == short.class) field.setShort(source, number.shortValue());
                else if (expected == int.class) field.setInt(source, number.intValue());
                else if (expected == long.class) field.setLong(source, number.longValue());
                else if (expected == double.class) field.setDouble(source, number.doubleValue());
                else if (expected == float.class) field.setFloat(source, number.floatValue());
            }
        } else if (value == null) field.set(source, null);
        else if (value instanceof Map<?, ?> child) {
            final Object sub, existing = field.get(source);
            if (existing == null) field.set(source, sub = this.createObject(expected));
            else sub = existing;
            this.unmarshal(sub, expected, child);
        } else if (Collection.class.isAssignableFrom(expected) && value instanceof Collection<?> list) {
            final Collection replacement;
            Class<?> expectedElement = Object.class;
            if (field.getGenericType() instanceof ParameterizedType parameterized) {
                final Type[] types = parameterized.getActualTypeArguments();
                if (types.length == 1) expectedElement = (Class<?>) types[0];
            }
            if (field.get(source) instanceof Collection current) (replacement = current).clear();
            else if (!Modifier.isAbstract(expected.getModifiers()))
                replacement = (Collection) this.createObject(field.getType());
            else if (Set.class.isAssignableFrom(expected)) replacement = new LinkedHashSet();
            else if (List.class.isAssignableFrom(expected)) replacement = new ArrayList();
            else replacement = new LinkedList();
            for (Object thing : list) //noinspection unchecked
                replacement.add(this.construct(thing, expectedElement));
            field.set(source, replacement);
        } else if (expected.isArray() && value instanceof Collection<?> list)
            field.set(source, this.constructArray(expected, list));
        else if (expected.isAssignableFrom(value.getClass())) field.set(source, value);
        else throw new GrammarException("Value of '" + field.getName() + "' (" + source.getClass()
                .getSimpleName() + ") could not be mapped to type " + expected.getSimpleName());
        //</editor-fold>
    }

    /**
     * Constructs a complex object from its marshalled type.
     */
    protected Object construct(Object data, Class<?> expected) {
        if (data == null) return null;
        else if (data instanceof Collection<?> list && expected.isArray()) return this.constructArray(expected, list);
        else if (data instanceof Map<?, ?> map && !Map.class.isAssignableFrom(expected)) {
            if (expected.isRecord())
                return this.createRecord(expected, map);
            else return this.unmarshal(this.createObject(expected), expected, map);
        } else if (expected.isEnum()) return this.createEnum(expected, data);
        else if (expected == UUID.class && data instanceof String text) return UUID.fromString(text);
        else return data;
    }

    @SuppressWarnings({"unchecked", "TypeParameterHidesVisibleType"})
    private <Type> Type createRecord(Class<Type> expected, @SuppressWarnings("rawtypes") Map data) {
        final RecordComponent[] components = expected.getRecordComponents();
        final Object[] parameters = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            final RecordComponent component = components[i];
            final Class<?> type = component.getType();
            final String name = this.getName(component, component.getName());
            if (type.isPrimitive()) parameters[i] = data.getOrDefault(name, this.getDefault(type));
            else {
                final Object value = data.get(name);
                if (type.isInstance(value)) parameters[i] = value;
                else parameters[i] = this.construct(value, type);
            }
        }
        final Constructor<Type> constructor = this.getCanonicalConstructor(expected);
        try {
            if (!constructor.canAccess(null)) constructor.trySetAccessible();
        } catch (SecurityException ignored) {}
        try {
            return constructor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new GrammarException(e);
        }
    }

    private Object getDefault(Class<?> type) {
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        if (type == char.class) return (char) 0;
        return null;
    }

    private <Type> Constructor<Type> getCanonicalConstructor(Class<Type> record) {
        Class<?>[] componentTypes = Arrays.stream(record.getRecordComponents())
            .map(RecordComponent::getType)
            .toArray(Class<?>[]::new);
        try {
            return record.getDeclaredConstructor(componentTypes);
        } catch (NoSuchMethodException e) {
            throw new GrammarException("Record's canonical constructor was missing.", e);
        }
    }

    /**
     * Constructs an array from a list of marshalled values.
     */
    protected Object constructArray(Class<?> type, Collection<?> list) {
        //<editor-fold desc="List to Array" defaultstate="collapsed">
        final Class<?> component = type.getComponentType();
        final Object object = Array.newInstance(component, list.size());
        final Object[] objects = list.toArray();
        if (component.isPrimitive()) {
            if (component == boolean.class) for (int i = 0; i < objects.length; i++)
                Array.setBoolean(object, i, (boolean) objects[i]);
            else if (component == int.class) for (int i = 0; i < objects.length; i++)
                Array.setInt(object, i, ((Number) objects[i]).intValue());
            else if (component == long.class) for (int i = 0; i < objects.length; i++)
                Array.setLong(object, i, ((Number) objects[i]).longValue());
            else if (component == double.class) for (int i = 0; i < objects.length; i++)
                Array.setDouble(object, i, ((Number) objects[i]).doubleValue());
            else if (component == float.class) for (int i = 0; i < objects.length; i++)
                Array.setFloat(object, i, ((Number) objects[i]).floatValue());
        } else if (component.isEnum()) for (int i = 0; i < objects.length; i++)
            Array.set(object, i, this.createEnum(component, objects[i]));
        else if (component == UUID.class) for (int i = 0; i < objects.length; i++)
            Array.set(object, i, UUID.fromString(objects[i].toString()));
        else {
            final Object[] array = (Object[]) object;
            for (int i = 0; i < objects.length; i++) array[i] = this.construct(objects[i], component);
        }
        return object;
        //</editor-fold>
    }

    /**
     * Deconstructs a complex object into its marshalled type.
     */
    protected Object deconstruct(Object value, Class<?> component, boolean any) {
        //<editor-fold desc="Complex to Simple" defaultstate="collapsed">
        if (value == null) return null;
        else if (value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        else if (value instanceof Collection<?> list) {
            final List<Object> replacement = new ArrayList<>(list.size());
            for (Object object : list)
                replacement.add(this.deconstruct(object, object == null ? null : object.getClass(), any));
            return replacement;
        } else if (value instanceof Map<?, ?> map) {
            final Map<String, Object> replacement = new LinkedHashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                final Object object = entry.getValue();
                replacement.put(String.valueOf(entry.getKey()), this.deconstruct(entry.getValue(), object == null ?
                    null : object.getClass(), any));
            }
            return replacement;
        }
        if (value.getClass().isEnum()) return ((Enum) value).name();
        else if (value.getClass().isArray()) {
            final List<Object> list = new ArrayList<>();
            this.deconstructArray(value, component.getComponentType(), list, any);
            return list;
        }
        final Map<String, Object> map = new LinkedHashMap<>();
        this.marshal(value, (Class<?>) (any ? value.getClass() : component), map);
        return map;
        //</editor-fold>
    }

    protected void deconstructArray(Object array, Class<?> component, List<Object> list, boolean any) {
        //<editor-fold desc="Array to List" defaultstate="collapsed">
        if (component.isPrimitive()) {
            if (array instanceof int[] numbers) for (int number : numbers) list.add(number);
            else if (array instanceof long[] numbers) for (long number : numbers) list.add(number);
            else if (array instanceof double[] numbers) for (double number : numbers) list.add(number);
            else if (array instanceof float[] numbers) for (float number : numbers) list.add(number);
            else if (array instanceof boolean[] numbers) for (boolean number : numbers) list.add(number);
        } else {
            final Object[] objects = (Object[]) array;
            if (any) for (final Object object : objects) list.add(this.deconstruct(object, object.getClass(), true));
            else for (final Object object : objects) list.add(this.deconstruct(object, component, false));
        }
        //</editor-fold>
    }

    protected String getName(Field field) {
        if (field.isAnnotationPresent(Name.class)) return field.getAnnotation(Name.class).value();
        else return field.getName();
    }

    protected String getName(AnnotatedElement field, String name) {
        if (field.isAnnotationPresent(Name.class)) return field.getAnnotation(Name.class).value();
        else return name;
    }

    @SuppressWarnings("all")
    protected Object createEnum(Class<?> type, Object value) {
        if (value instanceof Number number) return type.getEnumConstants()[number.intValue()];
        return Enum.valueOf((Class) type, value.toString());
    }

    @SuppressWarnings("unchecked")
    protected <Type> Constructor<Type> createConstructor(Class<Type> type) throws NoSuchMethodException {
        final Constructor<?> shift = Object.class.getConstructor();
        return (Constructor<Type>) ReflectionFactory.getReflectionFactory().newConstructorForSerialization(type, shift);
    }

    @SuppressWarnings("unchecked")
    protected <Type> Constructor<Type> getConstructor(Class<Type> type) throws NoSuchMethodException {
        if (constructors.containsKey(type)) return (Constructor<Type>) constructors.get(type);
        if (type.isLocalClass() || type.getEnclosingClass() != null || this.noSimplexConstructor(type)) {
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

    protected boolean noSimplexConstructor(Class<?> type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0) return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    protected <Type> Type createObject(Class<Type> type) {
        if (type.isArray()) return (Type) Array.newInstance(type, 0);
        try {
            final Constructor<Type> constructor = this.getConstructor(type);
            return constructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new GrammarException("Unable to create '" + type.getSimpleName() + "' object.", e);
        }
    }

}
