/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.core.doc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import org.debezium.api.doc.Array;
import org.debezium.api.doc.Value;
import org.debezium.core.util.Iterators;
import org.debezium.core.util.Sequences;

/**
 * @author Randall Hauch
 *
 */
public final class BasicArray implements Array {

    private static final BiFunction<Integer, Value, Entry> CONVERT_PAIR_TO_ENTRY = new BiFunction<Integer, Value, Entry>() {
        @Override
        public Entry apply(Integer index, Value value) {
            return new BasicEntry(index.intValue(), value);
        }
    };

    private final List<Value> values;

    public BasicArray() {
        this.values = new ArrayList<>();
    }

    public BasicArray(Value[] values) {
        if (values == null || values.length == 0) {
            this.values = new ArrayList<>();
        } else {
            this.values = new ArrayList<>(values.length);
            for (Value value : values) {
                this.values.add(value != null ? value : Value.nullValue());
            }
        }
    }

    protected final int indexFrom(CharSequence name) {
        return Integer.parseInt(name.toString());
    }

    protected final boolean isValidIndex(int index) {
        return index >= 0 && index < size();
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }
    
    @Override
    public int compareTo(Array that) {
        if (that == null)
            return 1;
        int size = this.size();
        if (size != that.size()) {
            return size - that.size();
        }
        Array thatArray = that;
        for (int i = 0; i != size; ++i) {
            Value thatValue = thatArray.get(i);
            Value thisValue = get(i);
            int diff = thatValue.compareTo(thisValue);
            if (diff != 0)
                return diff;
        }
        return 0;
    }

    @Override
    public Iterator<Entry> iterator() {
        return Iterators.around(Sequences.infiniteIntegers(0), values, CONVERT_PAIR_TO_ENTRY);
    }
    
    @Override
    public Value remove(int index) {
        if (isValidIndex(index)) {
            // The index is in bounds ...
            return values.remove(index);
        }
        return null;
    }

    @Override
    public Array removeAll() {
        this.values.clear();
        return this;
    }

    @Override
    public boolean has(int index) {
        return isValidIndex(index);
    }

    @Override
    public Value get(int index) {
        return isValidIndex(index) ? values.get(index) : null;
    }

    @Override
    public Array setValue(int index, Value value) {
        if (value == null) value = Value.nullValue();
        if (isValidIndex(index)) {
            // The index is in bounds ...
            values.set(index, value);
        } else if (isValidIndex(index - 1)) {
            // The index is the next valid one, so go ahead and add it ...
            values.add(value);
        } else {
            // The index is invalid ...
            throw new IllegalArgumentException("The index " + index + " is too large for this array, which has only " + size() + " values");
        }
        return this;
    }
    
    @Override
    public Array add(Value value) {
        if ( value == null ) value = Value.nullValue();
        this.values.add(value);
        return this;
    }
    
    @Override
    public  Iterable<Value> values() {
        return values;
    }
    
    @Override
    public Array clone() {
        return new BasicArray().addAll(this.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicArray) {
            BasicArray that = (BasicArray) obj;
            return values.equals(that.values);
        }
        return false;
    }

    @Override
    public String toString() {
        return values.toString();
    }

}
