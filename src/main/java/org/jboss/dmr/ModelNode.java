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
import org.jboss.dmr.stream.ModelStreamFactory;
import org.jboss.dmr.stream.ModelWriter;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;



/**
 * A dynamic model representation node object.
 * <p>
 * A node can be of any type specified in the {@link ModelType} enumeration.  The type can
 * be queried via {@link #getType()} and updated via any of the {@code set*()} methods.  The
 * value of the node can be acquired via the {@code as<type>()} methods, where {@code <type>} is
 * the desired value type.  If the type is not the same as the node type, a conversion is attempted between
 * the types.
 * <p>A node can be made read-only by way of its {@link #protect()} method, which will prevent
 * any further changes to the node or its sub-nodes.
 * <p>Instances of this class are <b>not</b> thread-safe and need to be synchronized externally.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@SuppressWarnings("SameParameterValue")
public class ModelNode implements Externalizable, Cloneable {

    private static final long serialVersionUID = 2030456323088551487L;

    /** An {@link #protect() unmodifiable} node of {@link ModelType#BOOLEAN} with a value of {@code true} */
    public static final ModelNode TRUE  = new ModelNode(true);
    /** An {@link #protect() unmodifiable} node of {@link ModelType#BOOLEAN} with a value of {@code false} */
    public static final ModelNode FALSE = new ModelNode(false);
    /** An {@link #protect() unmodifiable} node of {@link ModelType#INT} with a value of {@code 0} */
    public static final ModelNode ZERO  = new ModelNode(0);
    /** An {@link #protect() unmodifiable} node of {@link ModelType#LONG} with a value of {@code 0L} */
    public static final ModelNode ZERO_LONG  = new ModelNode(0L);

    static {
        TRUE.protect();
        FALSE.protect();
        ZERO.protect();
        ZERO_LONG.protect();
    }

    private boolean protect = false;
    private ModelValue value = ModelValue.UNDEFINED;

    /**
     * Creates a new {@code ModelNode} with an undefined value.
     */
    public ModelNode() {
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public ModelNode(final BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.value = new BigDecimalModelValue(value);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public ModelNode(final BigInteger value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.value = new BigIntegerModelValue(value);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value.
     */
    public ModelNode(final boolean value) {
        this(value ? BooleanModelValue.TRUE : BooleanModelValue.FALSE);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public ModelNode(final byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.value = new BytesModelValue(value);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value.
     */
    public ModelNode(final double value) {
        this.value = new DoubleModelValue(value);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value.
     */
    public ModelNode(final int value) {
        this.value = new IntModelValue(value);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value.
     */
    public ModelNode(final long value) {
        this.value = new LongModelValue(value);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public ModelNode(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.value = new StringModelValue(value);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public ModelNode(final ValueExpression value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.value = new ExpressionValue(value);
    }

    /**
     * Creates a new {@code ModelNode} with the given {@code value}.
     *
     * @param value the value. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public ModelNode(final ModelType value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.value = TypeModelValue.of(value);
    }

    ModelNode(final ModelValue value) {
        this.value = value;
    }

    /**
     * Prevent further modifications to this node and its sub-nodes.  Note that copies
     * of this node made after this method call will not be protected.
     */
    public void protect() {
        if (! protect) {
            protect = true;
            value = value.protect();
        }
    }

    /**
     * Returns whether this node has been {@link #protect() protected}.
     *
     * @return {@code true} if {@link #protect()} has been invoked on this node
     */
    public boolean isProtected() {
        return protect;
    }

    /**
     * Get the value of this node as a {@code long}.  Collection types will return the size
     * of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the long value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public long asLong() throws IllegalArgumentException {
        return value.asLong();
    }

    /**
     * Get the value of this node as a {@code long}.  Collection types will return the size
     * of the collection for this value.  Other types may attempt a string conversion.
     *
     * @param defVal the default value to return if this node is not {@link #isDefined() defined}
     * @return the long value
     *
     * @throws NumberFormatException if this node's {@link #getType() type} is {@link ModelType#STRING} and a numeric conversion of the string value is not possible
     * @throws IllegalArgumentException if this node's {@link #getType() type} is one where no numeric conversion is possible
     */
    public long asLong(final long defVal) {
        return value.asLong(defVal);
    }

    /**
     * Get the value of this node as a {@code Long}, or {@code null} if this node is not {@link #isDefined() defined}.
     * Collection types will return the size of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the long value or {@code null}
     *
     * @throws NumberFormatException if this node's {@link #getType() type} is {@link ModelType#STRING} and a numeric conversion of the string value is not possible
     * @throws IllegalArgumentException if this node's {@link #getType() type} is one where no numeric conversion is possible
     */
    public Long asLongOrNull() {
        return isDefined() ? asLong() : null;
    }

    /**
     * Get the value of this node as an {@code int}.  Collection types will return the size
     * of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the int value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public int asInt() throws IllegalArgumentException {
        return value.asInt();
    }

    /**
     * Get the value of this node as an {@code int}.  Collection types will return the size
     * of the collection for this value.  Other types may attempt a string conversion.
     *
     * @param defVal the default value to return if this node is not {@link #isDefined() defined}
     * @return the int value
     *
     * @throws NumberFormatException if this node's {@link #getType() type} is {@link ModelType#STRING} and a numeric conversion of the string value is not possible
     * @throws IllegalArgumentException if this node's {@link #getType() type} is one where no numeric conversion is possible
     */
    public int asInt(final int defVal) {
        return value.asInt(defVal);
    }

    /**
     * Get the value of this node as an {@code int}, or {@code null} if this node is not {@link #isDefined() defined}.
     * Collection types will return the size of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the int value or {@code null}
     * @throws IllegalArgumentException if no conversion is possible
     */
    public Integer asIntOrNull() {
        return isDefined() ? asInt() : null;
    }

    /**
     * Get the value of this node as a {@code boolean}.  Collection types return {@code true} for non-empty
     * collections.  Numerical types return {@code true} for non-zero values.
     *
     * @return the boolean value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public boolean asBoolean() throws IllegalArgumentException {
        return value.asBoolean();
    }

    /**
     * Get the value of this node as a {@code boolean}.  Collection types return {@code true} for non-empty
     * collections.  Numerical types return {@code true} for non-zero values.
     *
     * @param defVal the default value to return if this node is not {@link #isDefined() defined}
     * @return the boolean value
     *
     * @throws IllegalArgumentException if this node's {@link #getType() type} is one where no numeric conversion is possible or if the type is {@link ModelType#STRING} and the string value is not equal, ignoring case, to the literal {@code true} or {@code false}
     */
    public boolean asBoolean(final boolean defVal) {
        return value.asBoolean(defVal);
    }


    /**
     * Get the value of this node as a {@code boolean}, or {@code null} if this node is not {@link #isDefined() defined}.
     * Collection types return {@code true} for non-empty collections.  Numerical types return {@code true} for non-zero values.
     *
     * @return the boolean value or {@code null}
     * @throws IllegalArgumentException if no conversion is possible
     */
    public Boolean asBooleanOrNull() throws IllegalArgumentException {
        return isDefined() ? value.asBoolean() : null;
    }

    /**
     * Get the value as a string.  This is the literal value of this model node.  More than one node type may
     * yield the same value for this method.
     *
     * @return the string value. A node that is not {@link #isDefined() defined} returns the literal string {@code undefined}
     */
    public String asString() {
        return value.asString();
    }

    /**
     * Get the value as a string.  This is the literal value of this model node.  More than one node type may
     * yield the same value for this method.
     *
     * @param defVal the default value to return if this node is not {@link #isDefined() defined}
     * @return the string value.
     */
    public String asString(String defVal) {
        return isDefined() ? value.asString() : defVal;
    }

    /**
     * Get the value as a string or {@code null} if this node is not {@link #isDefined() defined}.  This is the literal value of this model node.  More than one node type may
     * yield the same value for this method.
     *
     * @return the string value or {@code null}
     */
    public String asStringOrNull() {
        return isDefined() ? value.asString() : null;
    }

    /**
     * Get the value of this node as a {@code double}.  Collection types will return the size
     * of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the double value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public double asDouble() throws IllegalArgumentException {
        return value.asDouble();
    }

    /**
     * Get the value of this node as an {@code double}.  Collection types will return the size
     * of the collection for this value.  Other types may attempt a string conversion.
     *
     * @param defVal the default value to return if this node is not {@link #isDefined() defined}
     * @return the int value
     *
     * @throws NumberFormatException if this node's {@link #getType() type} is {@link ModelType#STRING} and a numeric conversion of the string value is not possible
     * @throws IllegalArgumentException if this node's {@link #getType() type} is one where no numeric conversion is possible
     */
    public double asDouble(final double defVal) {
        return value.asDouble(defVal);
    }

    /**
     * Get the value of this node as a {@code double} or {@code null} if this node is not {@link #isDefined() defined}.
     * Collection types will return the size of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the double value or {@code null}
     * @throws IllegalArgumentException if no conversion is possible
     */
    public Double asDoubleOrNull() throws IllegalArgumentException {
        return isDefined() ? value.asDouble() : null;
    }

    /**
     * Get the value of this node as a type, expressed using the {@code ModelType} enum.  The string
     * value of this node must be convertible to a type.
     *
     * @return the {@code ModelType} value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public ModelType asType() throws IllegalArgumentException {
        return value.asType();
    }

    /**
     * Get the value of this node as a {@code BigDecimal}. Collection types will return the size
     * of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the {@code BigDecimal} value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public BigDecimal asBigDecimal() throws IllegalArgumentException {
        return value.asBigDecimal();
    }

    /**
     * Get the value of this node as a {@code BigDecimal} or {@code null} if this node is not {@link #isDefined() defined}.
     * Collection types will return the size of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the {@code BigDecimal} value or {@code null}
     * @throws IllegalArgumentException if no conversion is possible
     */
    public BigDecimal asBigDecimalOrNull() throws IllegalArgumentException {
        return isDefined() ? value.asBigDecimal() : null;
    }

    /**
     * Get the value of this node as a {@code BigInteger}.  Collection types will return the size
     * of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the {@code BigInteger} value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public BigInteger asBigInteger() throws IllegalArgumentException {
        return value.asBigInteger();
    }

    /**
     * Get the value of this node as a {@code BigInteger} or {@code null} if this node is not {@link #isDefined() defined}.
     * Collection types will return the size of the collection for this value.  Other types may attempt a string conversion.
     *
     * @return the {@code BigInteger} value or {@code null}
     * @throws IllegalArgumentException if no conversion is possible
     */
    public BigInteger asBigIntegerOrNull() throws IllegalArgumentException {
        return isDefined() ? value.asBigInteger() : null;
    }

    /**
     * Get the value of this node as a byte array.  Strings and string-like values will return
     * the UTF-8 encoding of the string.  Numerical values will return the byte representation of the
     * number.
     *
     * @return the bytes
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public byte[] asBytes() throws IllegalArgumentException {
        return value.asBytes();
    }

    /**
     * Get the value of this node as a byte array or {@code null} if this node is not {@link #isDefined() defined}.
     * Strings and string-like values will return the UTF-8 encoding of the string.  Numerical values will return the
     * byte representation of the number.
     *
     * @return the bytes or {@code null}
     * @throws IllegalArgumentException if no conversion is possible
     */
    public byte[] asBytesOrNull() throws IllegalArgumentException {
        return isDefined() ? value.asBytes() : null;
    }

    /**
     * Get the value of this node as an expression.
     *
     * @return the expression
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public ValueExpression asExpression() throws IllegalArgumentException {
        return value.asExpression();
    }

    /**
     * Get the value of this node as a property.  Object values will return a property if there is exactly one
     * property in the object.  List values will return a property if there are exactly two items in the list,
     * and if the first is convertible to a string.
     *
     * @return the property value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public Property asProperty() throws IllegalArgumentException {
        return value.asProperty();
    }

    /**
     * Get the value of this node as a property list.  Object values will return a list of properties representing
     * each key-value pair in the object.  List values will return all the values of the list, failing if any of the
     * values are not convertible to a property value.
     *
     * @return the property list value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public List<Property> asPropertyList() throws IllegalArgumentException {
        return value.asPropertyList();
    }

    /**
     * Get the value of this node as a property list.  Object values will return a list of properties representing
     * each key-value pair in the object.  List values will return all the values of the list, failing if any of the
     * values are not convertible to a property value.
     *
     * @return the property list value or an empty list if not {@link #isDefined() defined}.
     * @throws IllegalArgumentException if no conversion is possible
     */
    public List<Property> asPropertyListOrEmpty() throws IllegalArgumentException {
        return asPropertyList(Collections.emptyList());
    }

    /**
     * Get the value of this node as a property list.  Object values will return a list of properties representing
     * each key-value pair in the object.  List values will return all the values of the list, failing if any of the
     * values are not convertible to a property value.
     *
     * @param defaultValue the value to return if not {@link #isDefined() defined}.
     * @return the property list value or the specified default value if not {@link #isDefined() defined}.
     * @throws IllegalArgumentException if no conversion is possible
     */
    public List<Property> asPropertyList(List<Property> defaultValue) throws IllegalArgumentException {
        return isDefined() ? value.asPropertyList() : defaultValue;
    }

    /**
     * Get a copy of this value as an object.  Object values will simply copy themselves as by the {@link #clone()} method.
     * Property values will return a single-entry object whose key and value are copied from the property key and value.
     * List values will attempt to interpolate the list into an object by iterating each item, mapping each property
     * into an object entry and otherwise taking pairs of list entries, converting the first to a string, and using the
     * pair of entries as a single object entry.  If an object key appears more than once in the source object, the last
     * key takes precedence.
     *
     * @return the object value
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined} or if no conversion is possible
     */
    public ModelNode asObject() throws IllegalArgumentException {
        return value.asObject();
    }

    /**
     * Determine whether this node is defined.  Equivalent to the expression: {@code getType() != ModelType.UNDEFINED}.
     *
     * @return {@code true} if this node's value is defined
     */
    public boolean isDefined() {
        return getType() != ModelType.UNDEFINED;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final int newValue) {
        checkProtect();
        value = new IntModelValue(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final long newValue) {
        checkProtect();
        value = new LongModelValue(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final double newValue) {
        checkProtect();
        value = new DoubleModelValue(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final boolean newValue) {
        checkProtect();
        value = BooleanModelValue.valueOf(newValue);
        return this;
    }

    /**
     * Change this node's value to the given expression value.
     *
     * @param newValue the new value
     * @return this node
     * @deprecated Use {@link #set(ValueExpression)} instead.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public ModelNode setExpression(final String newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        value = new ExpressionValue(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final ValueExpression newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        value = new ExpressionValue(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final String newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        value = new StringModelValue(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final BigDecimal newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        value = new BigDecimalModelValue(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final BigInteger newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        value = new BigIntegerModelValue(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.  The value is copied from the parameter.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final ModelNode newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        value = newValue.value.copy();
        return this;
    }

    void setNoCopy(final ModelNode child) {
        value = child.value;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final byte[] newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        value = new BytesModelValue(newValue.length == 0 ? newValue : newValue.clone());
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final ModelType newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        value = TypeModelValue.of(newValue);
        return this;
    }

    /**
     * Change this node's value to the given value.
     *
     * @param newValue the new value
     * @return this node
     */
    public ModelNode set(final Property newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        set(newValue.getName(), newValue.getValue());
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final ModelNode propertyValue) {
        checkProtect();
        value = new PropertyModelValue(propertyName, propertyValue, true);
        return this;
    }

    ModelNode setNoCopy(final String propertyName, final ModelNode propertyValue) {
        value = new PropertyModelValue(propertyName, propertyValue, false);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final int propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final long propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final double propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final boolean propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final String propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and expression value.
     *
     * @param propertyName the property name
     * @param propertyValue the property expression value
     * @return this node
     * @deprecated Use {@link #set(String,ValueExpression)} instead.
     */
    @Deprecated
    public ModelNode setExpression(final String propertyName, final String propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(new ValueExpression(propertyValue));
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final ValueExpression propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final BigDecimal propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final BigInteger propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final byte[] propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a property with the given name and value.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     */
    public ModelNode set(final String propertyName, final ModelType propertyValue) {
        checkProtect();
        final ModelNode node = new ModelNode();
        node.set(propertyValue);
        value = new PropertyModelValue(propertyName, node);
        return this;
    }

    /**
     * Change this node's value to a list whose values are copied from the given collection.
     *
     * @param newValue the list value
     * @return this node
     */
    public ModelNode set(final Collection<ModelNode> newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        checkProtect();
        final ArrayList<ModelNode> list = new ArrayList<>(newValue.size());
        for (final ModelNode node : newValue) {
            if (node == null) {
                list.add(new ModelNode());
            } else {
                list.add(node.clone());
            }
        }
        value = new ListModelValue(list);
        return this;
    }

    /**
     * Change this node's value to an empty list.
     *
     * @return this node
     */
    public ModelNode setEmptyList() {
        checkProtect();
        value = new ListModelValue();
        return this;
    }

    /**
     * Change this node's value to an empty object.
     *
     * @return this node
     */
    public ModelNode setEmptyObject() {
        checkProtect();
        value = new ObjectModelValue();
        return this;
    }

    /**
     * Clear this node's value and change its type to {@link ModelType#UNDEFINED}.
     *
     * @return this node
     */
    public ModelNode clear() {
        checkProtect();
        value = ModelValue.UNDEFINED;
        return this;
    }

    /**
     * Get the child of this node with the given name.  If no such child exists, create it.  If the node is undefined,
     * it will be initialized to be of type {@link ModelType#OBJECT}.
     * <p>
     * When called on property values, the name must match the property name.
     *
     * @param name the child name
     * @return the child
     * @throws IllegalArgumentException if this node does not support getting a child with the given name
     */
    public ModelNode get(final String name) {
        ModelValue value = this.value;
        if (value == ModelValue.UNDEFINED) {
            checkProtect();
            return (this.value = new ObjectModelValue()).getChild(name);
        }
        return value.getChild(name);
    }

    /**
     * Require the existence of a child of this node with the given name, returning the child.  If no such child exists,
     * an exception is thrown.
     * <p>
     * When called on property values, the name must match the property name.
     *
     * @param name the child name
     * @return the child
     * @throws NoSuchElementException if the element does not exist
     */
    public ModelNode require(final String name) throws NoSuchElementException {
        return value.requireChild(name);
    }

    /**
     * Remove a child of this node, returning the child.  If no such child exists,
     * {@code null} is returned.
     * <p>
     * When called on property values, the name must match the property name.
     *
     * @param name the child name
     * @return the child, or {@code null} if no child with the given {@code name} exists
     *
     */
    public ModelNode remove(final String name) throws NoSuchElementException {
        return value.removeChild(name);
    }

    /**
     * Remove a child of this list, returning the child.  If no such child exists,
     * an exception is thrown.
     * <p>
     * When called on property values, the name must match the property name.
     *
     * @param index the child index
     * @return the child
     * @throws NoSuchElementException if the element does not exist
     */
    public ModelNode remove(final int index) throws NoSuchElementException {
        return value.removeChild(index);
    }

    /**
     * Get the child of this node with the given index.  If no such child exists, create it (adding list entries as needed).
     * If the node is undefined, it will be initialized to be of type {@link ModelType#LIST}.
     * <p>
     * When called on property values, the index must be zero.
     *
     * @param index the child index
     * @return the child
     * @throws IllegalArgumentException if this node does not support getting a child with the given index
     */
    public ModelNode get(final int index) {
        final ModelValue value = this.value;
        if (value == ModelValue.UNDEFINED) {
            checkProtect();
            return (this.value = new ListModelValue()).getChild(index);
        }
        return value.getChild(index);
    }

    /**
     * Require the existence of a child of this node with the given index, returning the child.  If no such child exists,
     * an exception is thrown.
     * <p>
     * When called on property values, the index must be zero.
     *
     * @param index the child index
     * @return the child
     * @throws NoSuchElementException if the element does not exist
     */
    public ModelNode require(final int index) {
        return value.requireChild(index);
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final int newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final long newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final double newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final boolean newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add the given expression to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     * @deprecated Use {@link #add(ValueExpression)} instead.
     */
    @Deprecated
    public ModelNode addExpression(final String newValue) {
        add().set(new ValueExpression(newValue));
        return this;
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final ValueExpression newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final BigDecimal newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final BigInteger newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add a copy of the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final ModelNode newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * insert copy of the given value to provided index of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}. An index equal to the current number of child elements
     * held by this node is allowed (thus adding a child) but an index greater than that is not allowed (i.e.
     * adding intervening elements is not supported.)
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IndexOutOfBoundsException if {@code index} is greater than zero and is greater than the number of child nodes currently stored in this node
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode insert(final ModelNode newValue, int index) {
        insert(index).set(newValue);
        return this;
    }

    ModelNode addNoCopy(final ModelNode child) {
        add().value = child.value;
        return this;
    }

    /**
     * Add the given value to the end of this node's value list.  If the node is undefined, it will be initialized to be
     * of type {@link ModelType#LIST}.
     *
     * @param newValue the new value to add
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final byte[] newValue) {
        add().set(newValue);
        return this;
    }

    /**
     * Add a property to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param property the property
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final Property property) {
        add().set(property);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final int propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final long propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final double propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final boolean propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final ValueExpression propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final String propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final BigDecimal propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final BigInteger propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final ModelNode propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a property with the given name and value to the end of this node's value list.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @param propertyName the property name
     * @param propertyValue the property value
     * @return this node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add(final String propertyName, final byte[] propertyValue) {
        add().set(propertyName, propertyValue);
        return this;
    }

    /**
     * Add a node to the end of this node's value list and return it.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}.
     *
     * @return the new node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode add() {
        checkProtect();
        ModelValue value = this.value;
        if (value == ModelValue.UNDEFINED) {
            return (this.value = new ListModelValue()).addChild();
        }
        return value.addChild();
    }

    /**
     * Insert a node at provided index of this node's value list and return it.  If the node is undefined, it
     * will be initialized to be of type {@link ModelType#LIST}. An index equal to the current number of child elements
     * held by this node is allowed (thus adding a child) but an index greater than that is not allowed (i.e.
     * adding intervening elements is not supported.)
     *
     * @param index where in list to put it
     * @return the new node
     *
     * @throws IndexOutOfBoundsException if {@code index} is greater than zero and is greater than the number of child nodes currently stored in this node
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode insert(final int index) {
        checkProtect();
        ModelValue value = this.value;
        if (value == ModelValue.UNDEFINED) {
            return (this.value = new ListModelValue()).insertChild(index);
        }
        return value.insertChild(index);
    }

    /**
     * Add a node of type {@link ModelType#LIST} to the end of this node's value list and return it.  If this node is
     * undefined, it will be initialized to be of type {@link ModelType#LIST}.
     *
     * @return the new node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode addEmptyList() {
        final ModelNode node = add();
        node.setEmptyList();
        return node;
    }

    /**
     * Add a node of type {@link ModelType#OBJECT} to the end of this node's value list and return it.  If this node is
     * undefined, it will be initialized to be of type {@link ModelType#LIST}.
     *
     * @return the new node
     *
     * @throws IllegalArgumentException if this node is {@link #isDefined() defined} and its {@link #getType() type} is not {@link ModelType#LIST}
     */
    public ModelNode addEmptyObject() {
        final ModelNode node = add();
        node.setEmptyObject();
        return node;
    }

    /**
     * Determine whether this node has a child with the given index.  Property node types always contain exactly one
     * value.
     *
     * @param index the index
     * @return {@code true} if there is a (possibly undefined) node at the given index
     */
    public boolean has(final int index) {
        return value.has(index);
    }

    /**
     * Determine whether this node has a child with the given name.  Property node types always contain exactly one
     * value with a key equal to the property name.
     *
     * @param key the name
     * @return {@code true} if there is a (possibly undefined) node at the given key
     */
    public boolean has(final String key) {
        return value.has(key);
    }

    /**
     * Recursively determine whether this node has children with the given names. If any child along the path does not
     * exist, return {@code false}.
     *
     * @param names the child names
     * @return {@code true} if a call to {@link #get(String...)} with the given {@code names} would succeed without
     *         needing to create any new nodes; {@code false} otherwise
     */
    public boolean has(final String... names) {
        ModelNode current = this;
        for (final String part : names) {
            if (current.has(part)) {
                current = current.get(part);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine whether this node has a defined child with the given index.  Property node types always contain exactly one
     * value.
     *
     * @param index the index
     * @return {@code true} if there is a node at the given index and its {@link #getType() type} is not {@link ModelType#UNDEFINED}
     */
    public boolean hasDefined(int index) {
        return value.has(index) && get(index).isDefined();
    }

    /**
     * Determine whether this node has a defined child with the given name.  Property node types always contain exactly one
     * value with a key equal to the property name.
     *
     * @param key the name
     * @return {@code true} if there is a node at the given index and its {@link #getType() type} is not {@link ModelType#UNDEFINED}
     */
    public boolean hasDefined(String key) {
        return value.has(key) && get(key).isDefined();
    }

    /**
     * Recursively determine whether this node has defined children with the given names. If any child along the path does not
     * exist or is not defined, return {@code false}.
     *
     * @param names the child names
     * @return {@code true} if a call to {@link #get(String...)} with the given {@code names} would succeed without
     *         needing to create any new nodes and without traversing any undefined nodes; {@code false} otherwise
     */
    public boolean hasDefined(final String... names) {
        ModelNode current = this;
        for (final String part : names) {
            if (current.hasDefined(part)) {
                current = current.get(part);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the set of keys contained in this object.  Property node types always contain exactly one value with a key
     * equal to the property name.  Other non-object types will throw an exception.
     *
     * @return the key set
     * @throws IllegalArgumentException if this node's {@link #getType() type} is not {@link ModelType#OBJECT} or {@link ModelType#PROPERTY}
     */
    public Set<String> keys() {
        return value.getKeys();
    }

    /**
     * Get the list of entries contained in this object.  Property node types always contain exactly one entry (itself).
     * Lists will return an unmodifiable view of their contained list.  Objects will return a list of properties corresponding
     * to the mappings within the object.  Other {@link #isDefined()} types will return an empty list.
     *
     * @return the entry list
     * @throws IllegalArgumentException if this node is not {@link #isDefined() defined}
     */
    public List<ModelNode> asList() {
        return value.asList();
    }

    /**
     * Get the value as a list as per {@link #asList}, or return the given default if this node is not {@linkplain #isDefined() defined}.
     *
     * @param defVal the default value to return if this node is not {@linkplain #isDefined() defined}
     * @return the list value or an empty list
     */
    public List<ModelNode> asList(List<ModelNode> defVal) {
        return isDefined() ? value.asList() : defVal;
    }

    /**
     * Get the value as a list as per {@link #asList}, or return an empty list if this node is not {@linkplain #isDefined() defined}.
     *
     * @return the list value or an empty list (not {@code null})
     */
    public List<ModelNode> asListOrEmpty() {
        return asList(Collections.emptyList());
    }

    /**
     * Recursively get the children of this node with the given names.  If any child along the path does not exist,
     * create it.  If any node is the path is undefined, it will be initialized to be of type {@link ModelType#OBJECT}.
     *
     * @param names the child names
     * @return the child
     * @throws IllegalArgumentException if a node does not support getting a child with the given name path
     */
    public ModelNode get(final String... names) {
        ModelNode current = this;
        for (final String part : names) {
            current = current.get(part);
        }
        return current;
    }

    /**
     * Get a human-readable string representation of this model node, formatted nicely (possibly on multiple lines).
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return value.toString();
    }

    /**
     * Output the DMR string representation of this model node, formatted nicely, if requested to the supplied PrintWriter
     * instance.
     *
     * @param writer A PrintWriter instance used to output the DMR string.
     * @param compact Flag that indicates whether or not the string should be all on one line (i.e. {@code true}) or should be
     *        printed on multiple lines ({@code false}).
     */
    public void writeString(final PrintWriter writer, final boolean compact) {
        if (compact) {
            final ModelWriter modelWriter = ModelStreamFactory.getInstance(false).newModelWriter(writer);
            //noinspection TryWithIdenticalCatches
            try {
                value.write(modelWriter);
                modelWriter.flush();
                modelWriter.close();
            } catch (final IOException e) {
                throw new RuntimeException(e); // should never happen because PrintWriter swallows IOExceptions
            } catch (final ModelException e) {
                throw new RuntimeException(e); // should never happen because this model serialization is always correct
            }
        } else {
            value.writeString(writer, compact);
        }
    }

    /**
     * Get a JSON string representation of this model node, formatted nicely, if requested.
     * @param compact Flag that indicates whether or not the string should be all on
     * one line (i.e. {@code true}) or should be printed on multiple lines ({@code false}).
     * @return The JSON string.
     */
    public String toJSONString(final boolean compact) {
        return value.toJSONString(compact);
    }

    /**
     * Output the JSON string representation of this model node, formatted nicely, if requested to the supplied PrintWriter
     * instance.
     *
     * @param writer A PrintWriter instance used to output the JSON string.
     * @param compact Flag that indicates whether or not the string should be all on one line (i.e. {@code true}) or should be
     *        printed on multiple lines ({@code false}).
     */
    public void writeJSONString(final PrintWriter writer, final boolean compact) {
        if (compact) {
            final ModelWriter modelWriter = ModelStreamFactory.getInstance(true).newModelWriter(writer);
            //noinspection TryWithIdenticalCatches
            try {
                value.write(modelWriter);
                modelWriter.flush();
                modelWriter.close();
            } catch (final IOException e) {
                throw new RuntimeException(e); // should never happen because PrintWriter swallows IOExceptions
            } catch (final ModelException e) {
                throw new RuntimeException(e); // should never happen because this model serialization is always correct
            }
        } else {
            value.writeJSONString(writer, compact);
        }
    }

    final void write(final ModelWriter writer) throws IOException, ModelException {
        value.write(writer);
    }

    /**
     * Get a model node from a string representation of the model node.
     *
     * @param input the input string
     * @return the model node
     */
    public static ModelNode fromString(final String input) {
        //noinspection TryWithIdenticalCatches
        try {
            return ModelNodeFactory.INSTANCE.readFrom(input, false);
        } catch (final IOException e) {
            final IllegalArgumentException n = new IllegalArgumentException(e.getMessage());
            n.setStackTrace(e.getStackTrace());
            throw n;
        }  catch (final ModelException e) {
            final IllegalArgumentException n = new IllegalArgumentException(e.getMessage());
            n.setStackTrace(e.getStackTrace());
            throw n;
        }
    }

    public static ModelNode fromJSONString(final String input) {
        //noinspection TryWithIdenticalCatches
        try {
            return ModelNodeFactory.INSTANCE.readFrom(input, true);
        } catch (final IOException e) {
            final IllegalArgumentException n = new IllegalArgumentException(e.getMessage());
            n.setStackTrace(e.getStackTrace());
            throw n;
        } catch (final ModelException e) {
            final IllegalArgumentException n = new IllegalArgumentException(e.getMessage());
            n.setStackTrace(e.getStackTrace());
            throw n;
        }
    }

    /**
     * Get a model node from a text representation of the model node.  The stream must be decodable using
     * the UTF-8 charset.
     *
     * @param stream the source stream
     * @return the model node
     */
    public static ModelNode fromStream(final InputStream stream) throws IOException {
        try {
            return ModelNodeFactory.INSTANCE.readFrom(stream, false);
        } catch (ModelException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get a model node from a JSON text representation of the model node. The stream should be encoded in UTF-8.
     *
     * @param stream the source stream
     * @return the model node
     */
    public static ModelNode fromJSONStream(final InputStream stream) throws IOException {
        try {
            return ModelNodeFactory.INSTANCE.readFrom(stream, true);
        } catch (ModelException e) {
            throw new IOException(e);
        }
    }

    /**
     * Reads base64 data from the passed stream,
     * and deserializes the decoded result.
     *
     * @see #writeBase64(OutputStream)
     * @return the decoded model node
     * @throws IOException if the passed stream has an issue
     */
    public static ModelNode fromBase64(InputStream stream) throws IOException {
        Base64.InputStream bstream = new Base64.InputStream(stream);
        ModelNode node = new ModelNode();
        node.readExternal(bstream);
        bstream.close();
        return node;
    }

    /**
     * Reads base64 data from the passed string,
     * and deserializes the decoded result.
     *
     * @see #writeBase64(OutputStream)
     * @return the decoded model node
     * @throws IOException if the passed stream has an issue
     */
    public static ModelNode fromBase64String(String encoded) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(encoded));
        ModelNode node = new ModelNode();
        node.readExternal(bais);
        bais.close();
        return node;
    }

    /**
     * Return a copy of this model node, with all values of type {@link ModelType#EXPRESSION} locally resolved.
     * The caller must have permission to access all of the system properties named in the node tree. If an expression
     * begins with {@code ${env.} then a system property named {@code env.@lt;remainder of expression@gt;} will be
     * checked, and if not present a {@link System#getenv(String) system environment variable named @lt;remainder of expression@gt;}
     * will be checked. In that case the caller must have permission to access the environment variable.
     *
     * @return the resolved copy
     *
     * @throws IllegalStateException if there is a value of type {@link ModelType#EXPRESSION} in the node tree and
     *                               there is no system property or environment variable that matches the expression
     * @throws SecurityException
     *         if a security manager exists and its
     *         {@link SecurityManager#checkPermission checkPermission}
     *         method doesn't allow access to the relevant system property or environment variable
     */
    public ModelNode resolve() {
        final ModelNode newNode = new ModelNode();
        newNode.value = value.resolve();
        return newNode;
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object other) {
        return other instanceof ModelNode && equals((ModelNode)other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final ModelNode other) {
        return this == other || other != null && other.value.equals(value);
    }

    /**
     * Get the hash code of this node object.  Note that unless the value is {@link #protect()}ed, the hash code may
     * change over time, thus making unprotected nodes unsuitable for use as hash table keys.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        //noinspection NonFinalFieldReferencedInHashCode
        return value.hashCode();
    }

    /**
     * Clone this model node.
     *
     * @return the clone
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ModelNode clone() {
        final ModelNode clone = new ModelNode();
        clone.value = value.copy();
        return clone;
    }

    void format(final PrintWriter writer, final int indent, final boolean multiLine) {
        value.format(writer, indent, multiLine);
    }

    void formatAsJSON(final PrintWriter writer, final int indent, final boolean multiLine) {
        value.formatAsJSON(writer, indent, multiLine);
    }

    /**
     * Get the current type of this node.
     *
     * @return the node type
     */
    public ModelType getType() {
        return value.getType();
    }

    /**
     * Write this node's content in binary format to the given target.
     *
     * @param out the target to which the content should be written
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        writeExternal((DataOutput) out);
    }

    /**
     * Write this node's content in binary format to the given target.
     *
     * @param out the target to which the content should be written
     * @throws IOException if an I/O error occurs
     */
    public void writeExternal(final OutputStream out) throws IOException {
        writeExternal((DataOutput) new DataOutputStream(out));
    }

    /**
     * Write this node's content in binary format to the given target.
     *
     * @param out the target to which the content should be written
     * @throws IOException if an I/O error occurs
     */
    public void writeExternal(final DataOutputStream out) throws IOException {
        writeExternal((DataOutput) out);
    }

    /**
     * Write this node's content in binary format to the given target.
     *
     * @param out the target to which the content should be written
     * @throws IOException if an I/O error occurs
     */
    public void writeExternal(final DataOutput out) throws IOException {
        value.writeExternal(out);
    }

    /**
     * Read this node's content in binary format from the given source.
     *
     * @param in the source from which the content should be read
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        readExternal((DataInput) in);
    }

    /**
     * Read this node's content in binary format from the given source.
     *
     * @param in the source from which the content should be read
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("unused")
    public void readExternal(final DataInputStream in) throws IOException {
        readExternal((DataInput) in);
    }

    /**
     * Read this node's content in binary format from the given source.
     *
     * @param in the source from which the content should be read
     * @throws IOException if an I/O error occurs
     */
    public void readExternal(final InputStream in) throws IOException {
        readExternal((DataInput) new DataInputStream(in));
    }

    /**
     * Read this node's content in binary format from the given source.
     *
     * @param in the source from which the content should be read
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("WeakerAccess")
    public void readExternal(final DataInput in) throws IOException {
        checkProtect();
        try {
            final char c = (char) (in.readByte() & 0xff);
            final ModelType type = ModelType.forChar(c);
            switch (type) {
                case UNDEFINED: value = ModelValue.UNDEFINED; return;
                case BIG_DECIMAL: value = new BigDecimalModelValue(in); return;
                case BIG_INTEGER: value = new BigIntegerModelValue(in); return;
                case BOOLEAN: value = BooleanModelValue.valueOf(in.readBoolean()); return;
                case BYTES: value = new BytesModelValue(in); return;
                case DOUBLE: value = new DoubleModelValue(in.readDouble()); return;
                case EXPRESSION: value = new ExpressionValue(in.readUTF()); return;
                case INT: value = new IntModelValue(in.readInt()); return;
                case LIST: value = new ListModelValue(in); return;
                case LONG: value = new LongModelValue(in.readLong()); return;
                case OBJECT: value = new ObjectModelValue(in); return;
                case PROPERTY: value = new PropertyModelValue(in); return;
                case STRING: value = new StringModelValue(c, in); return;
                case TYPE: value = TypeModelValue.of(ModelType.forChar((char) (in.readByte() & 0xff))); return;
                default: throw new InvalidObjectException("Invalid type read: " + type);
            }
        } catch (final IllegalArgumentException e) {
            final InvalidObjectException ne = new InvalidObjectException(e.getMessage());
            ne.initCause(e.getCause());
            throw ne;
        }
    }


    /**
     * Encodes the serialized representation in base64 form
     * and writes it to the specified output stream.
     *
     * @param stream the stream to write to
     * @throws IOException if the specified stream has an issue
     */
    public void writeBase64(OutputStream stream) throws IOException {
        Base64.OutputStream bstream = new Base64.OutputStream(stream);
        writeExternal(bstream);
        bstream.flushBase64(); // Required to ensure last block is written to stream.
    }

    private void checkProtect() {
        if (protect) {
            throw new UnsupportedOperationException();
        }
    }
}
