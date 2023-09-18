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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ObjectModelValue extends ModelValue {
    private final Map<String, ModelNode> map;

    protected ObjectModelValue() {
        super(ModelType.OBJECT);
        map = new LinkedHashMap<String, ModelNode>();
    }

    private ObjectModelValue(final Map<String, ModelNode> map) {
        super(ModelType.OBJECT);
        this.map = map;
    }

    ObjectModelValue(final DataInput in) throws IOException {
        super(ModelType.OBJECT);
        final int count = in.readInt();
        final LinkedHashMap<String, ModelNode> map = new LinkedHashMap<String, ModelNode>();
        for (int i = 0; i < count; i++) {
            final String key = in.readUTF();
            final ModelNode value = new ModelNode();
            value.readExternal(in);
            map.put(key, value);
        }
        this.map = map;
    }

    @Override
    void writeExternal(final DataOutput out) throws IOException {
        out.write(ModelType.OBJECT.typeChar);
        final Map<String, ModelNode> map = this.map;
        final int size = map.size();
        out.writeInt(size);
        for (final Map.Entry<String, ModelNode> entry : map.entrySet()) {
            out.writeUTF(entry.getKey());
            entry.getValue().writeExternal(out);
        }
    }

    @Override
    ModelValue protect() {
        final Map<String, ModelNode> map = this.map;
        for (final ModelNode node : map.values()) {
            node.protect();
        }
        return map.getClass() == LinkedHashMap.class ? new ObjectModelValue(Collections.unmodifiableMap(map)) : this;
    }

    @Override
    ModelNode asObject() {
        return new ModelNode(copy());
    }

    @Override
    ModelNode getChild(final String name) {
        if (name == null) {
            return null;
        }
        final ModelNode node = map.get(name);
        if (node != null) {
            return node;
        }
        final ModelNode newNode = new ModelNode();
        map.put(name, newNode);
        return newNode;
    }

    @Override
    ModelNode removeChild(final String name) {
        if (name == null) {
            return null;
        }
        return map.remove(name);
    }

    @Override
    int asInt() {
        return map.size();
    }

    @Override
    int asInt(final int defVal) {
        return asInt();
    }

    @Override
    long asLong() {
        return asInt();
    }

    @Override
    long asLong(final long defVal) {
        return asInt();
    }

    @Override
    boolean asBoolean() {
        return !map.isEmpty();
    }

    @Override
    boolean asBoolean(final boolean defVal) {
        return !map.isEmpty();
    }

    @Override
    Property asProperty() {
        if (map.size() == 1) {
            final Map.Entry<String, ModelNode> entry = map.entrySet().iterator().next();
            return new Property(entry.getKey(), entry.getValue());
        }
        return super.asProperty();
    }

    @Override
    List<Property> asPropertyList() {
        final List<Property> propertyList = new ArrayList<Property>();
        for (final Map.Entry<String, ModelNode> entry : map.entrySet()) {
            propertyList.add(new Property(entry.getKey(), entry.getValue()));
        }
        return propertyList;
    }

    @Override
    ModelValue copy() {
        return copy(false);
    }

    @Override
    ModelValue resolve() {
        return copy(true);
    }

    ModelValue copy(final boolean resolve) {
        final LinkedHashMap<String, ModelNode> newMap = new LinkedHashMap<String, ModelNode>();
        for (final Map.Entry<String, ModelNode> entry : map.entrySet()) {
            newMap.put(entry.getKey(), resolve ? entry.getValue().resolve() : entry.getValue().clone());
        }
        return new ObjectModelValue(newMap);
    }

    @Override
    List<ModelNode> asList() {
        final ArrayList<ModelNode> nodes = new ArrayList<ModelNode>();
        for (final Map.Entry<String, ModelNode> entry : map.entrySet()) {
            final ModelNode node = new ModelNode();
            node.set(entry.getKey(), entry.getValue());
            nodes.add(node);
        }
        return nodes;
    }

    @Override
    Set<String> getKeys() {
        return map.keySet();
    }

    @Override
    String asString() {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter, true);
        format(writer, 0, false);
        return stringWriter.toString();
    }

    @Override
    void format(final PrintWriter writer, final int indent, final boolean multiLineRequested) {
        writer.append('{');
        final boolean multiLine = multiLineRequested && map.size() > 1;
        if (multiLine) {
            indent(writer.append('\n'), indent + 1);
        }
        final Iterator<Map.Entry<String, ModelNode>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, ModelNode> entry = iterator.next();
            writer.append(quote(entry.getKey()));
            final ModelNode value = entry.getValue();
            writer.append(" => ");
            value.format(writer, multiLine ? indent + 1 : indent, multiLineRequested);
            if (iterator.hasNext()) {
                if (multiLine) {
                    indent(writer.append(",\n"), indent + 1);
                } else {
                    writer.append(',');
                }
            }
        }
        if (multiLine) {
            indent(writer.append('\n'), indent);
        }
        writer.append('}');
    }

    @Override
    void formatAsJSON(final PrintWriter writer, final int indent, final boolean multiLineRequested) {
        writer.append('{');
        final boolean multiLine = multiLineRequested && map.size() > 1;
        if (multiLine) {
            indent(writer.append('\n'), indent + 1);
        }
        final Iterator<Map.Entry<String, ModelNode>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, ModelNode> entry = iterator.next();
            writer.append(quote(entry.getKey()));
            writer.append(" : ");
            final ModelNode value = entry.getValue();
            value.formatAsJSON(writer, multiLine ? indent + 1 : indent, multiLineRequested);
            if (iterator.hasNext()) {
                if (multiLine) {
                    indent(writer.append(",\n"), indent + 1);
                } else {
                    writer.append(", ");
                }
            }
        }
        if (multiLine) {
            indent(writer.append('\n'), indent);
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
        return other instanceof ObjectModelValue && equals((ObjectModelValue) other);
    }

    /**
     * Determine whether this object is equal to another.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final ObjectModelValue other) {
        return this == other || other != null && other.map.equals(map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    boolean has(final String key) {
        return map.containsKey(key);
    }

    @Override
    ModelNode requireChild(final String name) throws NoSuchElementException {
        final ModelNode node = map.get(name);
        if (node != null) {
            return node;
        }
        return super.requireChild(name);
    }

    @Override
    void write(final ModelWriter writer) throws IOException, ModelException {
        writer.writeObjectStart();
        final Iterator<Map.Entry<String, ModelNode>> iterator = map.entrySet().iterator();
        Map.Entry<String, ModelNode> entry;
        while (iterator.hasNext()) {
            entry = iterator.next();
            writer.writeString(entry.getKey());
            entry.getValue().write(writer);
        }
        writer.writeObjectEnd();
    }

}
