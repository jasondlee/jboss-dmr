/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.dmr;

import org.jboss.dmr.stream.ModelException;
import org.jboss.dmr.stream.ModelWriter;

import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TypeModelValue extends ModelValue {

    /**
     * JSON Key used to identify TypeModelValue.
     */
    public static final String TYPE_KEY = "TYPE_MODEL_VALUE";

    private final ModelType value;

    private TypeModelValue(final ModelType value) {
        super(ModelType.TYPE);
        this.value = value;
    }

    @Override
    void writeExternal(final DataOutput out) throws IOException {
        out.write(ModelType.TYPE.typeChar);
        out.writeByte(value.getTypeChar());
    }

    private static final TypeModelValue BIG_DECIMAL = new TypeModelValue(ModelType.BIG_DECIMAL);
    private static final TypeModelValue BIG_INTEGER = new TypeModelValue(ModelType.BIG_INTEGER);
    private static final TypeModelValue BOOLEAN = new TypeModelValue(ModelType.BOOLEAN);
    private static final TypeModelValue BYTES = new TypeModelValue(ModelType.BYTES);
    private static final TypeModelValue DOUBLE = new TypeModelValue(ModelType.DOUBLE);
    private static final TypeModelValue EXPRESSION = new TypeModelValue(ModelType.EXPRESSION);
    private static final TypeModelValue INT = new TypeModelValue(ModelType.INT);
    private static final TypeModelValue LONG = new TypeModelValue(ModelType.LONG);
    private static final TypeModelValue LIST = new TypeModelValue(ModelType.LIST);
    private static final TypeModelValue OBJECT = new TypeModelValue(ModelType.OBJECT);
    private static final TypeModelValue PROPERTY = new TypeModelValue(ModelType.PROPERTY);
    private static final TypeModelValue STRING = new TypeModelValue(ModelType.STRING);
    private static final TypeModelValue TYPE = new TypeModelValue(ModelType.TYPE);
    private static final TypeModelValue UNDEFINED = new TypeModelValue(ModelType.UNDEFINED);

    static TypeModelValue of(final ModelType type) {
        switch (type) {
            case BIG_DECIMAL:
                return BIG_DECIMAL;
            case BIG_INTEGER:
                return BIG_INTEGER;
            case BOOLEAN:
                return BOOLEAN;
            case BYTES:
                return BYTES;
            case DOUBLE:
                return DOUBLE;
            case EXPRESSION:
                return EXPRESSION;
            case INT:
                return INT;
            case LIST:
                return LIST;
            case LONG:
                return LONG;
            case OBJECT:
                return OBJECT;
            case PROPERTY:
                return PROPERTY;
            case STRING:
                return STRING;
            case TYPE:
                return TYPE;
            default:
                return UNDEFINED;
        }
    }

    @Override
    boolean asBoolean() {
        return value != ModelType.UNDEFINED;
    }

    @Override
    boolean asBoolean(final boolean defVal) {
        return value != ModelType.UNDEFINED;
    }

    @Override
    String asString() {
        return value.toString();
    }

    @Override
    ValueExpression asExpression() {
        return new ValueExpression(asString());
    }

    @Override
    ModelType asType() {
        return value;
    }

    @Override
    void formatAsJSON(final PrintWriter writer, final int indent, final boolean multiLine) {
        writer.append('{');
        if (multiLine) {
            indent(writer.append('\n'), indent + 1);
        } else {
            writer.append(' ');
        }
        writer.append(jsonEscape(TYPE_KEY));
        writer.append(" : ");
        writer.append(jsonEscape(asString()));
        if (multiLine) {
            indent(writer.append('\n'), indent);
        } else {
            writer.append(' ');
        }
        writer.append('}');
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object other) {
        return other instanceof TypeModelValue && equals((TypeModelValue) other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final TypeModelValue other) {
        return this == other || other != null && other.value == value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    void write(final ModelWriter writer) throws IOException, ModelException {
        writer.writeType(value);
    }

}
