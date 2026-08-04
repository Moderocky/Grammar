package mx.kenzie.grammar;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.valross.constantine.Array;
import org.valross.constantine.Constant;
import org.valross.constantine.Constantive;
import org.valross.constantine.RecordConstant;

import java.lang.constant.Constable;
import java.util.*;
import java.util.function.Consumer;

/// A container of names mapped to constant values.
/// This is used to store an object's data during the marshalling phase.
public interface Container extends Map<String, @NotNull Constable>, Constantive {

    static Container empty() {
        return new SimpleContainer(new LinkedHashMap<>());
    }

    static Container of(Map<String, Constable> map) {
        if (map instanceof Container container) return container;
        return new SimpleContainer(map);
    }

    static Container ofUnknown(Map<?, ?> map) {
        boolean dirty = false;
        Map<String, Constable> safe = null;
        for (Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (entry.getKey() instanceof String && value instanceof Constable) continue;
            if (!dirty) {
                safe = new LinkedHashMap<>();
                dirty = true;
            }
            if (!(value instanceof Constable constable)) continue;
            safe.put(Objects.toString(entry.getKey()), constable);
        }
        if (!dirty) // noinspection unchecked
            safe = (Map<String, Constable>) map;
        return new SimpleContainer(safe);
    }

    static Frozen of() {
        return new FrozenContainer(new Array(), new Array());
    }

    static Frozen of(String key, Constable value) {
        return new FrozenContainer(new Array(key), new Array(Null.safe(value)));
    }

    static Frozen of(String key1, Constable value1, String key2, Constable value2) {
        return new FrozenContainer(new Array(key1, key2), new Array(Null.safe(value1), Null.safe(value2)));
    }

    static Frozen of(String key1, Constable value1, String key2, Constable value2, String key3, Constable value3) {
        return new FrozenContainer(new Array(key1, key2, key3), new Array(Null.safe(value1), Null.safe(value2), Null.safe(value3)));
    }

    static Frozen of(String key1, Constable value1, String key2, Constable value2, String key3, Constable value3, String key4, Constable value4) {
        return new FrozenContainer(new Array(key1, key2, key3, key4), new Array(Null.safe(value1), Null.safe(value2), Null.safe(value3), Null.safe(value4)));
    }

    /// @param key     The key name
    /// @param type    The expected value type
    /// @param <Value> The expected value type (parameter)
    /// @return The discovered value, potentially null
    /// @throws ClassCastException if this was made to complete exceptionally
    /// and the discovered value was of the wrong type
    default <Value extends Constable> Value get(String key, Class<Value> type, boolean completeExceptionally) throws ClassCastException {
        Constable constable = this.get((Object) key);
        if (completeExceptionally) return type.cast(constable);
        try {
            return type.cast(constable);
        } catch (ClassCastException _) {
            return null;
        }
    }

    /// A helper method for casting the discovered value to a desired type.
    default <Value extends Constable> Value get(String key) throws ClassCastException {
        Constable constable = this.get((Object) key);
        //noinspection unchecked
        return (Value) constable;
    }

    @Contract(pure = true, value = "_, null -> null")
    default <Value extends Constable> Value getOrDefault(String key, Value defaultValue) throws ClassCastException {
        Constable constable = this.getOrDefault((Object) key, defaultValue);
        //noinspection unchecked
        return (Value) constable;
    }

    /// A helper method for running an action if the desired key is present in the container.
    default <Value extends Constable> void ifPresent(String key, Consumer<Value> ifPresent) {
        if (this.containsKey(key)) ifPresent.accept(this.get(key));
    }

    @Override
    Frozen constant();

    /// An unmodifiable edition of a container.
    interface Frozen extends Container, Constant {

        @Override
        default Frozen constant() {
            return this;
        }

    }

}

interface BackedContainer extends Container {

    Map<String, Constable> backing();

    @Override
    default int size() {
        return this.backing().size();
    }

    @Override
    default boolean isEmpty() {
        return this.backing().isEmpty();
    }

    @Override
    default boolean containsKey(Object key) {
        return this.backing().containsKey(key);
    }

    @Override
    default boolean containsValue(Object value) {
        return this.backing().containsValue(value);
    }

    @Override
    default Constable get(Object key) {
        return this.backing().get(key);
    }

    @Override
    default Constable put(String key, Constable value) {
        return this.backing().put(key, Null.safe(value));
    }

    @Override
    default Constable remove(Object key) {
        return this.backing().remove(key);
    }

    @Override
    default void putAll(Map<? extends String, ? extends Constable> m) {
        this.backing().putAll(m);
    }

    @Override
    default void clear() {
        this.backing().clear();
    }

    @Override
    default Set<String> keySet() {
        return this.backing().keySet();
    }

    @Override
    default Collection<Constable> values() {
        return this.backing().values();
    }

    @Override
    default Set<Entry<String, Constable>> entrySet() {
        return this.backing().entrySet();
    }

    @Override
    default Frozen constant() {
        Array keys = new Array(this.backing().keySet());
        Array values = new Array(this.backing().values());
        assert keys.size() == values.size();
        return new FrozenContainer(keys, values);
    }

}

record SimpleContainer(Map<String, Constable> backing) implements BackedContainer {

    @Override
    public boolean equals(Object object) {
        return backing.equals(object);
    }
}

record SimpleUnmodifiableContainer(Map<String, Constable> backing)
        implements BackedContainer, Constant, Container.Frozen {

    public SimpleUnmodifiableContainer(Array keys, Array values) {
        this(unwrap(keys, values));
    }

    static Map<String, Constable> unwrap(Array keys, Array values) {
        Constable[] a = keys.toArray();
        Constable[] b = values.toArray();
        assert a.length == b.length;
        //noinspection DataFlowIssue user could input bad data
        int length = Math.min(a.length, b.length);
        Map<String, Constable> backing = new LinkedHashMap<>(length);
        for (int i = 0; i < length; i++) {
            backing.put(a[i].toString(), b[i]);
        }
        return Collections.unmodifiableMap(backing);
    }

    @Override
    public Constable[] serial() {
        return new Constable[]{new Array(this.backing().keySet()), new Array(this.backing().values())};
    }

    @Override
    public Class<?>[] canonicalParameters() {
        return new Class[]{Array.class, Array.class};
    }

    @Override
    public SimpleUnmodifiableContainer constant() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return backing.equals(obj);
    }
}

record FrozenContainer(Array keyArray, Array valueArray) implements RecordConstant, Container.Frozen, Container {

    @Override
    public int size() {
        return keyArray.size();
    }

    @Override
    public boolean isEmpty() {
        return keyArray.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return keyArray.contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return valueArray.contains(value);
    }

    @Override
    public Constable get(Object key) {
        int index = 0;
        for (Constable constable : keyArray.toArray()) {
            if (Objects.equals(constable, key)) return valueArray.toArray()[index];
            ++index;
        }
        return null;
    }

    @Override
    public Constable put(String key, Constable value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Constable remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Constable> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        //noinspection unchecked is fine because we're backing it
        return (Set<String>) (Object) Set.of(keyArray.toArray());
    }

    @Override
    public Collection<Constable> values() {
        return valueArray;
    }

    @Override
    public FrozenContainer constant() {
        return this;
    }

    @Override
    public Set<Entry<String, Constable>> entrySet() {
        return new Set<>() {
            private final int size = FrozenContainer.this.size();

            @Override
            public int size() {
                return size;
            }

            @Override
            public boolean isEmpty() {
                return size < 1;
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof PairedEntry(int index, Array keys, Array values))) return false;
                if (index < 0 || index >= size) return false;
                return Objects.equals(keys, keyArray) && Objects.equals(values, valueArray);
            }

            @Override
            public Iterator<Entry<String, Constable>> iterator() {
                return new Iterator<>() {
                    int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < size;
                    }

                    @Override
                    public Entry<String, Constable> next() {
                        return new PairedEntry(index++, keyArray, valueArray);
                    }
                };
            }

            @Override
            public Object[] toArray() {
                PairedEntry[] entries = new PairedEntry[size];
                for (int i = 0; i < size; i++) {
                    entries[i] = new PairedEntry(i, keyArray, valueArray);
                }
                return entries;
            }

            @Override
            @SafeVarargs
            public final <T> T[] toArray(T... a) throws ClassCastException {
                //noinspection unchecked there is really only one useful kind of array this can be
                return (T[]) this.toArray();
            }

            @Override
            public boolean add(Entry<String, Constable> stringConstableEntry) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return c.stream().allMatch(this::contains);
            }

            @Override
            public boolean addAll(Collection<? extends Entry<String, Constable>> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Map)) return false;
        return new HashMap<>(this).equals(obj);
    }

    record PairedEntry(int index, Array keyArray, Array valueArray) implements Entry<String, Constable> {

        @Override
        public String getKey() {
            return keyArray.toArray()[index].toString();
        }

        @Override
        public Constable getValue() {
            return valueArray.toArray()[index];
        }

        @Override
        public Constable setValue(Constable value) {
            throw new UnsupportedOperationException();
        }
    }
}
