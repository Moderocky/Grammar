package mx.kenzie.grammar;

import mx.kenzie.grail.function.Function;
import mx.kenzie.grail.function.Supplier;
import mx.kenzie.grammar.unwrap.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.reflect.ReflectionFactory;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Grammar {

    private static final Constructor<?> rootOfAll = Supplier.get(Object.class::getConstructor, Error::new);
    protected final Grammar parent;
    protected Map<Class<?>, Function<?, Constable, GrammarException>> marshallingStrategiesByClass;
    protected Deque<PredicatedMarshallingStrategy<Object, ?>> marshallingStrategies;
    protected Map<Class<?>, Function<Constable, ?, GrammarException>> unmarshallingStrategies;
    protected Map<Class<?>, Supplier<?, GrammarException>> creatorFunctions;

    /// Creates a grammar with an inheritance system.
    /// Falls back to relying on the parent's registered strategies.
    ///
    /// This can be used to create global inheritance patterns, i.e. a central grammar system
    /// with common types registered, and local extenders with special situational types.
    protected Grammar(Grammar parent) {
        this.parent = parent;
        this.marshallingStrategies = new LinkedList<>();
        // These maps are weak to allow class unloading for anonymous types
        this.marshallingStrategiesByClass = new WeakHashMap<>();
        this.unmarshallingStrategies = new WeakHashMap<>();
        this.creatorFunctions = new WeakHashMap<>();
        this.registerMarshallingStrategy(String.class, self -> self);
        this.registerUnmarshallingStrategy(String.class, String::valueOf);
        // Numbers require conversions
        this.registerUnmarshallingStrategy(int.class, constable -> ((Number) constable).intValue());
        this.registerUnmarshallingStrategy(long.class, constable -> ((Number) constable).longValue());
        this.registerUnmarshallingStrategy(float.class, constable -> ((Number) constable).floatValue());
        this.registerUnmarshallingStrategy(double.class, constable -> ((Number) constable).doubleValue());
        this.registerUnmarshallingStrategy(short.class, constable -> ((Number) constable).shortValue());
        this.registerUnmarshallingStrategy(byte.class, constable -> ((Number) constable).byteValue());
    }

    protected Grammar() {
        this(null);
    }

    public static Container assertIsContainer(Object constant) {
        if (constant instanceof Container container) return container;
        throw new GrammarException.UnmarshallingException("Expected a primitive data container but found " + constant);
    }

    public static Collection<Constable> assertIsSeries(Object constant) {
        if (constant instanceof Collection<?> container)
            //noinspection unchecked
            return (Collection<Constable>) container;
        throw new GrammarException.UnmarshallingException("Expected a primitive data series but found " + constant);
    }

    /// Registers a no-arguments constructor (or similar provider function) for a serialisable class.
    /// The new instance will be modified with the unmarshalled data after creation.
    ///
    /// For types that should not be edited after creation (records) see [#registerUnmarshallingStrategy(Class, Function)]
    ///
    /// @param type              The registered type
    /// @param noArgsConstructor A function that creates a _new_ value of the type
    /// @param <Type>            The type
    public <Type extends Marshalled.Unmarshalled> void registerConstructor(Class<Type> type, Supplier<Type, GrammarException> noArgsConstructor) {
        this.creatorFunctions.put(type, noArgsConstructor);
    }

    public <Type extends Marshalled.Unmarshalled> void registerRiskyConstructor(Class<Type> type, Supplier<Type, Throwable> noArgsConstructor) {
        this.registerConstructor(type, noArgsConstructor.hide(GrammarException.UnmarshallingException::new));
    }

    public <Type extends Record> void registerRecord(Class<Type> recordType) {
        Unwrapper<Type> unwrapper = new RecordUnwrapper<>(this, recordType);
        this.register(recordType, unwrapper);
    }

    public <Type extends Enum<Type>> void registerEnum(Class<Type> enumType) {
        Unwrapper<Type> unwrapper = new EnumNameUnwrapper<>(enumType);
        this.register(enumType, unwrapper);
    }

    public <Type extends Enum<Type>> void registerEnumByOrdinal(Class<Type> enumType) {
        Unwrapper<Type> unwrapper = new EnumOrdinalUnwrapper<>(enumType);
        this.register(enumType, unwrapper);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <Type> void registerUncheckedObject(Class<Type> type) {
        if (type.isEnum()) this.registerEnum((Class) type);
        else if (type.isRecord()) this.registerRecord((Class) type);
        else this.register(type, new ObjectUnwrapper<>(this, type));
    }

    public <Type> void register(Class<Type> type, Unwrapper<Type> unwrapper) {
        this.registerMarshallingStrategy(type, unwrapper.marshal());
        this.registerUnmarshallingStrategy(type, unwrapper.unmarshal());
    }

    public <Type, Data extends Constable> void registerUnmarshallingStrategy(Class<Type> type, Class<Data> dataType, Function<Data, Type, GrammarException> strategy) {
        this.unmarshallingStrategies.put(type, strategy.compose(dataType::cast));
    }

    public <Type> void registerUnmarshallingStrategy(Class<Type> type, Function<Constable, Type, GrammarException> strategy) {
        this.unmarshallingStrategies.put(type, strategy);
    }

    public <Type> void registerMarshallingStrategy(Class<Type> type, Function<Type, Constable, GrammarException> strategy) {
        this.marshallingStrategiesByClass.put(type, strategy);
        this.registerMarshallingStrategy(type::isInstance, strategy);
    }

    public <Type> void registerMarshallingStrategy(Predicate<Object> predicate, Function<Type, Constable, GrammarException> strategy) {
        //noinspection unchecked
        this.marshallingStrategies.addFirst((PredicatedMarshallingStrategy<Object, ?>) new PredicatedMarshallingStrategy<>(predicate, strategy));
    }

    public <Type> void registerFallbackMarshallingStrategy(Predicate<Object> predicate, Function<Type, Constable, GrammarException> strategy) {
        //noinspection unchecked
        this.marshallingStrategies.addLast((PredicatedMarshallingStrategy<Object, ?>) new PredicatedMarshallingStrategy<>(predicate, strategy));
    }

    public <Type> Unmarshaller<Type> createUnmarshallingStrategy(Class<Type> type) {
        return new Unmarshaller<>(this, type);
    }

    /// Turns objects into data that has the potential to be written to a data structure.
    ///
    /// @param object The value to be marshalled
    /// @return The marshalled data, where possible
    @NotNull
    protected Constable marshal(Class<?> as, Object object) throws GrammarException {
        return this.marshalBySupertype(as, object);
    }

    @NotNull
    protected Constable marshal(@Nullable Object object) throws GrammarException {
        return switch (object) {
            case null -> Null.INSTANCE;
            case Container container -> container;
            case Series container -> container;
            case org.valross.constantine.Array container -> container;
            case Marshalled marshalled -> marshalled.marshal();
            default -> this.marshalUnchecked(object);
        };
    }

    @NotNull
    protected <Type> Constable marshalCollection(Class<Type> elementType, Collection<Type> collection) throws GrammarException {
        Series series = Series.empty();
        for (Type type : collection) {
            Constable constable = this.marshalBySupertype(elementType, type);
            series.add(constable);
        }
        return series;
    }

    @NotNull
    protected <Value> Constable marshalMap(Class<Value> valueType, Map<?, Value> map) throws GrammarException {
        Container container = Container.empty();
        map.forEach((key, value) -> container.put(String.valueOf(key), this.marshalBySupertype(valueType, value)));
        return container;
    }

    @NotNull
    protected Constable marshalArray(Class<?> elementType, Object array) throws GrammarException {
        final int length = Array.getLength(array);
        Function<Integer, Constable, RuntimeException> getter = i -> switch (array) {
            case Object[] values -> this.marshalBySupertype(elementType, values[i]);
            case byte[] values -> values[i];
            case short[] values -> values[i];
            case int[] values -> values[i];
            case long[] values -> values[i];
            case float[] values -> values[i];
            case double[] values -> values[i];
            case char[] values -> values[i];
            case boolean[] values -> values[i];
            default -> throw new GrammarException("Cannot marshal array of type " + array.getClass());
        };
        Constable[] values = new Constable[length];
        for (int i = 0; i < length; i++) {
            values[i] = getter.apply(i);
        }
        return Series.of(values);
    }

    protected @NotNull Constable marshalBySupertype(Class<?> as, @Nullable Object object) throws GrammarException {
        if (object == null) return Null.INSTANCE;
        if (as.isPrimitive()) return this.marshal(object);
        if (as.isArray()) return this.marshalArray(as.getComponentType(), object);
        // Try to use the provided type first
        Function<?, Constable, GrammarException> function = this.marshallingStrategiesByClass.get(as);
        if (function != null) return function.uncheckArgument().apply(object);
        // Otherwise fall back to anything we can
        return this.marshal(object);
    }

    /// Marshals an object using one of the provided marshalling strategies.
    protected Constable marshalUnchecked(Object object) throws GrammarException {
        if (object.getClass().isArray()) return this.marshalArray(object.getClass().getComponentType(), object);
        if (object instanceof ConstantDesc) return (Constable) object;
        for (PredicatedMarshallingStrategy<?, ?> strategy : this.getMarshallingStrategies()) {
            if (strategy.tester().test(object)) {
                return strategy.apply(object);
            }
        }
        try {
            if (parent != null) return parent.marshalUnchecked(object);
        } catch (GrammarException.NoMarshallingStrategy _) {
            // Suppress since we can keep going here
        }
        return this.failToMarshal(object);
    }

    protected <Value> Constable failToMarshal(Value object) throws GrammarException {
        if (object instanceof Constable constable) return constable;
        assert object != null;
        throw new GrammarException.NoMarshallingStrategy("Unable to marshall value: " + object + " (" + object.getClass() + ")");
    }

    /// Transforms constant data into a new value of the given type,
    /// provided the grammar has a strategy for un-marshalling that kind of object.
    protected <Value> Value unmarshal(Class<Value> type, Constable data) throws GrammarException {
        if (type.isPrimitive()) // noinspection unchecked
            return (Value) this.unmarshalPrimitive(type, data);
        if (type.isInstance(data)) return type.cast(data);
        if (Null.isNull(data)) return null;
        if (type.isArray()) return this.unmarshalArray(type, type.getComponentType(), assertIsSeries(data));
        if (Marshalled.Unmarshalled.class.isAssignableFrom(type) && data instanceof Container container) {
            Supplier<Value, GrammarException> constructor = this.creatorFunction(type);
            Value value = constructor.get();
            Marshalled.Unmarshalled unmarshalled = (Marshalled.Unmarshalled) value;
            unmarshalled.unmarshal(container);
            return value;
        }
        Function<Constable, Value, GrammarException> function = this.unmarshallingStrategyFor(type);
        return function.apply(data);
    }

    private Object unmarshalPrimitive(Class<?> type, Constable wrapped) {
        if (wrapped == null) return this.defaultValue(type);
        return wrapped;
    }

    protected <ArrayType, ComponentType> ArrayType unmarshalArray(Class<ArrayType> arrayType, Class<ComponentType> componentType, Collection<Constable> data) {
        Object array = Array.newInstance(componentType, data.size());
        BiConsumer<Integer, Constable> putter = (i, object) -> {
            switch (array) {
                case Object[] values -> values[i] = this.unmarshal(componentType, object);
                case byte[] values -> values[i] = this.unmarshal(byte.class, object);
                case short[] values -> values[i] = this.unmarshal(short.class, object);
                case int[] values -> values[i] = this.unmarshal(int.class, object);
                case long[] values -> values[i] = this.unmarshal(long.class, object);
                case float[] values -> values[i] = this.unmarshal(float.class, object);
                case double[] values -> values[i] = this.unmarshal(double.class, object);
                case char[] values -> values[i] = this.unmarshal(char.class, object);
                case boolean[] values -> values[i] = this.unmarshal(boolean.class, object);
                default -> throw new GrammarException("Cannot unmarshal array of type " + array.getClass());
            }
        };
        int index = 0;
        for (Constable datum : data)
            putter.accept(index++, datum);
        return arrayType.cast(array);
    }

    protected <ComponentType> void unmarshalSeries(Class<ComponentType> componentType, Collection<Constable> data, Consumer<ComponentType> storage) {
        for (Constable datum : data)
            storage.accept(this.unmarshal(componentType, datum));
    }

    protected <ComponentType> Collection<ComponentType> unmarshalSeries(Class<ComponentType> componentType, Collection<Constable> data) {
        Collection<ComponentType> collection = new ArrayList<>();
        this.unmarshalSeries(componentType, data, collection::add);
        return collection;
    }

    protected <ComponentType> Collection<ComponentType> unmarshalSeries(Class<ComponentType> componentType, Collection<Constable> data, Collection<ComponentType> storage) {
        this.unmarshalSeries(componentType, data, storage::add);
        return storage;
    }

    protected <ComponentType> void unmarshalMap(Class<ComponentType> componentType, Container data, BiConsumer<String, ComponentType> storage) {
        data.forEach((key, value) -> storage.accept(key, unmarshal(componentType, value)));
    }

    private <ComponentType> Map<?, ComponentType> unmarshalMap(Class<ComponentType> componentType, Container data, Map<?, ComponentType> storage) {
        //noinspection rawtypes,unchecked
        this.unmarshalMap(componentType, data, (BiConsumer<String, ComponentType>) ((Map) storage)::put);
        return storage;
    }

    protected <Value> Function<Constable, Value, GrammarException> unmarshallingStrategyFor(Class<Value> type) {
        return this.unmarshallingStrategyFor(type, false);
    }

    protected <Value> Function<Constable, Value, GrammarException> unmarshallingStrategyFor(Class<Value> type, boolean direct) {
        assert type != null;
        Map<Class<?>, Function<Constable, ?, GrammarException>> map = this.getUnmarshallingStrategies();
        Function<Constable, ?, GrammarException> strategy = map.get(type);
        if (strategy != null) return strategy.uncheckResult();
        // Check if the parent type has a direct strategy
        try {
            if (parent != null) return parent.unmarshallingStrategyFor(type, direct);
        } catch (GrammarException.NoUnmarshallingStrategy _) {
            // We can keep trying
        }
        if (direct)
            return this.failToUnmarshal(type);
        // Check for unregistered subtypes: e.g. an Integer satisfies a strategy for Number
        for (final var entry : map.entrySet()) {
            Class<?> key = entry.getKey();
            if (type.isAssignableFrom(key)) return entry.getValue().uncheckResult();
        }
        return this.failToUnmarshal(type);
    }

    protected <Value> Function<Constable, Value, GrammarException> failToUnmarshal(Class<Value> type) {
        throw new GrammarException.NoUnmarshallingStrategy("No registered unmarshalling strategy for " + type);
    }

    protected boolean knowsCreatorFunctionFor(Class<?> type) {
        return creatorFunctions.containsKey(type);
    }

    @SuppressWarnings("unchecked")
    protected <Type> @NotNull Type create(Class<Type> type) throws GrammarException {
        if (type.isInterface()) {
            /*
            Specifically for these three common field types we can make
            an assumption that these are safe extenders.
            Obviously, they lack special properties (e.g. immutability)
            but user should take responsibility for not labelling fields properly.
            The linked editions are used for ordering preservation.
             */
            if (type == List.class) return (Type) new ArrayList<>();
            if (type == Map.class) return (Type) new LinkedHashMap<>();
            if (type == Set.class) return (Type) new LinkedHashSet<>();
        }
        if (Modifier.isAbstract(type.getModifiers()))
            throw new GrammarException("Cannot create instance of abstract " + type.getSimpleName());
        Supplier<Type, GrammarException> function = this.creatorFunction(type);
        if (function == null) throw new GrammarException("Cannot create instance of " + type.getSimpleName());
        return function.get();
    }

    protected <Type> Supplier<Type, GrammarException> creatorFunction(Class<Type> type) throws GrammarException {
        this.creatorFunctions.computeIfAbsent(type, this::establishCreatorFunction);
        return creatorFunctions.get(type).uncheckResult();
    }

    protected <Type> Supplier<Type, GrammarException> establishCreatorFunction(Class<Type> type) {
        if (parent != null && parent.knowsCreatorFunctionFor(type)) return parent.creatorFunction(type);
        Supplier<Object, Throwable> constructor = this.findAppropriateConstructor(type);
        return constructor.hide(GrammarException::new).uncheckResult();
    }

    private Supplier<Object, Throwable> findAppropriateConstructor(Class<?> type) {
        try {
            // Try and use the available entry point
            Constructor<?> declaredConstructor = type.getConstructor();
            if (!declaredConstructor.canAccess(null)) throw new InaccessibleObjectException();
            return declaredConstructor::newInstance;
        } catch (NoSuchMethodException | InaccessibleObjectException | SecurityException _) {
            // Create our own
            ReflectionFactory factory = ReflectionFactory.getReflectionFactory();
            return factory.newConstructorForSerialization(type, rootOfAll)::newInstance;
        }
    }

    protected Map<Class<?>, Function<Constable, ?, GrammarException>> getUnmarshallingStrategies() {
        return unmarshallingStrategies;
    }

    protected Iterable<PredicatedMarshallingStrategy<Object, ?>> getMarshallingStrategies() {
        return marshallingStrategies;
    }

    protected Object defaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == boolean.class) return false;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0;
        if (type == long.class) return 0L;
        if (type == short.class) return (short) 0;
        if (type == char.class) return (char) 0;
        if (type == byte.class) return (byte) 0;
        return null;
    }

    public static class Unsafe extends Grammar {

        protected Unsafe() {
            super();
        }

        protected Unsafe(Grammar parent) {
            super(parent);
        }

        @Override
        public <Type extends Record> void registerRecord(Class<Type> recordType) {
            super.registerRecord(recordType);
        }

        @Override
        protected <Value> Constable failToMarshal(Value object) throws GrammarException {
            //noinspection unchecked
            Class<Value> type = (Class<Value>) object.getClass();
            if (!marshallingStrategiesByClass.containsKey(type)) {
                Unwrapper<Value> unwrapper = this.registerHandlerFor(type);
                if (unwrapper != null)
                    return unwrapper.marshal().apply(object);
            }
            return super.failToMarshal(object);
        }

        @Override
        protected <Value> Function<Constable, Value, GrammarException> failToUnmarshal(Class<Value> type) {
            if (!unmarshallingStrategies.containsKey(type)) {
                Unwrapper<Value> unwrapper = this.registerHandlerFor(type);
                if (unwrapper != null)
                    return unwrapper.unmarshal();
            }
            return super.failToUnmarshal(type);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        protected <Type> Unwrapper<Type> registerHandlerFor(Class<Type> typeClass) {
            Unwrapper unwrapper;
            if (typeClass.isRecord()) {
                unwrapper = new RecordUnwrapper<>(this, (Class) typeClass);
            } else if (typeClass.isEnum()) {
                unwrapper = new EnumNameUnwrapper<>((Class) typeClass);
            } else if (ObjectUnwrapper.isSuitable(typeClass)) {
                unwrapper = new ObjectUnwrapper<>(this, (Class) typeClass);
            } else return null;
            this.register((Class) typeClass, unwrapper);
            return unwrapper;
        }
    }

    public record PredicatedMarshallingStrategy<ObjectType, DataType extends Constable>(Predicate<Object> tester,
                                                                                        Function<ObjectType, DataType, GrammarException> strategy) implements Function<Object, DataType, GrammarException> {

        @Override
        public DataType apply(Object objectType) throws GrammarException {
            //noinspection unchecked assessed by the predicate
            return strategy.apply((ObjectType) objectType);
        }
    }

    public class Access {

        public @NotNull Constable marshalArray(Class<?> elementType, Object array) throws GrammarException {
            return Grammar.this.marshalArray(elementType, array);
        }

        public @NotNull Constable marshalBySupertype(Class<?> as, @Nullable Object object) throws GrammarException {
            return Grammar.this.marshalBySupertype(as, object);
        }

        public @NotNull <Type> Constable marshalCollection(Class<Type> elementType, Collection<Type> collection) throws GrammarException {
            return Grammar.this.marshalCollection(elementType, collection);
        }

        public @NotNull <Value> Constable marshalMap(Class<Value> valueType, Map<?, Value> map) throws GrammarException {
            return Grammar.this.marshalMap(valueType, map);
        }

        public Constable marshalUnchecked(Object object) throws GrammarException {
            return Grammar.this.marshalUnchecked(object);
        }

        public <Value> Value unmarshal(Class<Value> type, Constable data) throws GrammarException {
            return Grammar.this.unmarshal(type, data);
        }

        public <ComponentType> void unmarshalSeries(Class<ComponentType> componentType, Collection<Constable> data, Consumer<ComponentType> storage) {
            Grammar.this.unmarshalSeries(componentType, data, storage);
        }

        public <ComponentType> Collection<ComponentType> unmarshalSeries(Class<ComponentType> componentType, Collection<Constable> data, Collection<ComponentType> storage) {
            return Grammar.this.unmarshalSeries(componentType, data, storage);
        }

        public <ComponentType> void unmarshalMap(Class<ComponentType> componentType, Container data, BiConsumer<String, ComponentType> storage) {
            Grammar.this.unmarshalMap(componentType, data, storage);
        }

        public <ComponentType> Map<?, ComponentType> unmarshalMap(Class<ComponentType> componentType, Container data, Map<?, ComponentType> storage) {
            return Grammar.this.unmarshalMap(componentType, data, storage);
        }

        public Constable marshal(@Nullable Object object) {
            return Grammar.this.marshal(object);
        }

        public Constable marshal(Class<?> as, Object object) {
            return Grammar.this.marshal(as, object);
        }

        public <Type> Supplier<Type, GrammarException> creatorFunction(Class<Type> type) {
            return Grammar.this.creatorFunction(type);
        }

        public <ArrayType, ComponentType> ArrayType unmarshalArray(Class<ArrayType> arrayType, Class<ComponentType> componentType, Collection<Constable> data) {
            return Grammar.this.unmarshalArray(arrayType, componentType, data);
        }

        public <Value> @NotNull Value create(Class<Value> type) {
            return Grammar.this.create(type);
        }

        public <Type> Type defaultValue(Class<Type> type) {
            //noinspection unchecked
            return (Type) Grammar.this.defaultValue(type);
        }
    }
}
