package mx.kenzie.grammar;

import org.jetbrains.annotations.NotNull;
import org.valross.constantine.Array;
import org.valross.constantine.Constant;
import org.valross.constantine.Constantive;
import org.valross.constantine.RecordConstant;

import java.lang.constant.Constable;
import java.util.*;

public interface Series extends Collection<Constable>, List<Constable>, Constantive {

    static Series empty() {
        return new BackedSeries(new ArrayList<>());
    }

    static Array of() {
        return new Array();
    }

    static Array of(Constable value) {
        return new Array(value);
    }

    static Array of(Constable... values) {
        return new Array(values);
    }

    static Series of(int... primitives) {
        return new IntSeries(primitives);
    }

    static Series of(float... primitives) {
        return new FloatSeries(primitives);
    }

    static Series of(double... primitives) {
        return new DoubleSeries(primitives);
    }

    static Series of(boolean... primitives) {
        return new BooleanSeries(primitives);
    }

    static Series of(long... primitives) {
        return new LongSeries(primitives);
    }

    static Series backedBy(List<Constable> collection) {
        return new BackedSeries(collection);
    }

    static boolean equals(Series a, Collection<?> b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        Iterator<?> iterator = b.iterator();
        for (Constable constable : a) {
            assert iterator.hasNext();
            if (!Objects.equals(constable, iterator.next())) return false;
        }
        assert !iterator.hasNext();
        return true;
    }

    @Override
    default Constable @NotNull [] toArray() {
        Constable[] array = new Constable[this.size()];
        int index = 0;
        for (Constable constable : this) array[index++] = constable;
        return array;
    }

    default <Value> Value at(int index) {
        //noinspection unchecked
        return (Value) this.get(index);
    }

    @Override
    Array constant();

}

interface PrimitiveSeries extends Series, Constant {

    Object backingArray();

    @Override
    default int size() {
        return java.lang.reflect.Array.getLength(backingArray());
    }

    @Override
    default boolean isEmpty() {
        return size() == 0;
    }

    @Override
    default boolean contains(Object o) {
        for (Constable constable : this) {
            if (Objects.equals(constable, o)) return true;
        }
        return false;
    }

    @Override
    default @NotNull Iterator<Constable> iterator() {
        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public @NotNull Constable next() {
                return (Constable) java.lang.reflect.Array.get(backingArray(), index++);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    default <T> T @NotNull [] toArray(T @NotNull [] a) {
        Object array = backingArray();
        if (a.length == 0)
            a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), this.size());
        final int min = Math.min(a.length, this.size());
        for (int i = 0; i < min; i++) {
            a[i] = (T) java.lang.reflect.Array.get(array, i);
        }
        return a;
    }

    @Override
    default boolean add(Constable constable) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean containsAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean addAll(@NotNull Collection<? extends Constable> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean addAll(int index, @NotNull Collection<? extends Constable> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Constable get(int index) {
        return (Constable) java.lang.reflect.Array.get(backingArray(), index);
    }

    @Override
    default Constable set(int index, Constable element) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void add(int index, Constable element) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Constable remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    default int indexOf(Object o) {
        int index = 0;
        for (Constable constable : this) {
            if (Objects.equals(constable, o)) return index;
            ++index;
        }
        return -1;
    }

    @Override
    default int lastIndexOf(Object o) {
        int index = 0;
        for (Constable constable : this.reversed()) {
            if (Objects.equals(constable, o)) return index;
            ++index;
        }
        return -1;
    }

    @Override
    default @NotNull ListIterator<Constable> listIterator() {
        return this.listIterator(0);
    }

    @Override
    default @NotNull ListIterator<Constable> listIterator(int startIndex) {
        return new ListIterator<>() {
            int index = startIndex;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public Constable next() {
                return get(index++);
            }

            @Override
            public boolean hasPrevious() {
                return index > 0;
            }

            @Override
            public Constable previous() {
                return get(--index);
            }

            @Override
            public int nextIndex() {
                return index;
            }

            @Override
            public int previousIndex() {
                return index - 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(Constable constable) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(Constable constable) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    default @NotNull List<Constable> subList(int fromIndex, int toIndex) {
        return new ArrayList<>(this).subList(fromIndex, toIndex);
    }

    @Override
    default Array constant() {
        return new Array(this.toArray());
    }
}

record IntSeries(int... backingArray) implements PrimitiveSeries, RecordConstant {
    @Override
    public int size() {
        return backingArray.length;
    }

    @Override
    public Array constant() {
        return PrimitiveSeries.super.constant();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Collection<?> collection)
            return Series.equals(this, collection);
        return false;
    }
}

record FloatSeries(float... backingArray) implements PrimitiveSeries, RecordConstant {
    @Override
    public int size() {
        return backingArray.length;
    }

    @Override
    public Array constant() {
        return PrimitiveSeries.super.constant();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Collection<?> collection)
            return Series.equals(this, collection);
        return false;
    }
}

record BooleanSeries(boolean... backingArray) implements PrimitiveSeries, RecordConstant {
    @Override
    public int size() {
        return backingArray.length;
    }

    @Override
    public Array constant() {
        return PrimitiveSeries.super.constant();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Collection<?> collection)
            return Series.equals(this, collection);
        return false;
    }
}

record DoubleSeries(double... backingArray) implements PrimitiveSeries, RecordConstant {
    @Override
    public int size() {
        return backingArray.length;
    }

    @Override
    public Array constant() {
        return PrimitiveSeries.super.constant();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Collection<?> collection)
            return Series.equals(this, collection);
        return false;
    }
}

record LongSeries(long... backingArray) implements PrimitiveSeries, RecordConstant {
    @Override
    public int size() {
        return backingArray.length;
    }

    @Override
    public Array constant() {
        return PrimitiveSeries.super.constant();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Collection<?> collection)
            return Series.equals(this, collection);
        return false;
    }
}

record BackedSeries(List<Constable> backing) implements Series {

    @Override
    public Array constant() {
        return new Array(backing);
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backing.contains(o);
    }

    @Override
    public @NotNull Iterator<Constable> iterator() {
        return backing.iterator();
    }

    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        return backing.toArray(a);
    }

    @Override
    public boolean add(Constable constable) {
        return backing.add(Null.safe(constable));
    }

    @Override
    public boolean remove(Object o) {
        return backing.remove(o);
    }

    @SuppressWarnings("SlowListContainsAll")
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return backing.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Constable> c) {
        return backing.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends Constable> c) {
        return backing.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return backing.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return backing.retainAll(c);
    }

    @Override
    public void clear() {
        this.backing.clear();
    }

    @Override
    public Constable get(int index) {
        return backing.get(index);
    }

    @Override
    public Constable set(int index, Constable element) {
        return backing.set(index, Null.safe(element));
    }

    @Override
    public void add(int index, Constable element) {
        this.backing.add(index, Null.safe(element));
    }

    @Override
    public Constable remove(int index) {
        return backing.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return backing.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return backing.lastIndexOf(o);
    }

    @Override
    public @NotNull ListIterator<Constable> listIterator() {
        return backing.listIterator();
    }

    @Override
    public @NotNull ListIterator<Constable> listIterator(int index) {
        return backing.listIterator(index);
    }

    @Override
    public @NotNull List<Constable> subList(int fromIndex, int toIndex) {
        return backing.subList(fromIndex, toIndex);
    }

    @SuppressWarnings("EqualsDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return backing.equals(obj);
    }
}
