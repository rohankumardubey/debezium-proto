/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.message;

import java.util.Objects;

import org.debezium.annotation.Immutable;
import org.debezium.message.Document.Field;
import org.debezium.util.Strings;

/**
 * Package-level implementation of a {@link Document.Field} inside a {@link Document}.
 * 
 * @author Randall Hauch
 */
@Immutable
final class BasicField implements Document.Field, Comparable<Document.Field> {

    private final CharSequence name;
    private final Value value;

    BasicField(CharSequence name, Value value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public CharSequence getName() {
        return name;
    }

    @Override
    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if ( obj == this ) return true;
        if ( obj instanceof Field ) {
            Field that = (Field)obj;
            return this.getName().equals(that.getName()) && Objects.equals(this.getValue(),that.getValue());
        }
        return false;
    }
    
    @Override
    public int compareTo(Field that) {
        if ( this == that ) return 0;
        int diff = Strings.compareTo(this.getName(),that.getName());
        if ( diff != 0 ) return diff;
        return Value.compareTo(this.getValue(),that.getValue());
    }
}
