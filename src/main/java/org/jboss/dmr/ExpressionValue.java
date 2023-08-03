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
final class ExpressionValue extends ModelValue {

    /**
     * JSON Key used to identify ExpressionValue.
     */
    public static final String TYPE_KEY = "EXPRESSION_VALUE";

    private final ValueExpression valueExpression;

    ExpressionValue(final String expressionString) {
        this(new ValueExpression(expressionString));
    }

    ExpressionValue(final ValueExpression valueExpression) {
        super(ModelType.EXPRESSION);
        this.valueExpression = valueExpression;
    }

    @Override
    void writeExternal(final DataOutput out) throws IOException {
        out.write(ModelType.EXPRESSION.typeChar);
        out.writeUTF(valueExpression.getExpressionString());
    }

    @Override
    String asString() {
        return valueExpression.getExpressionString();
    }

    @Override
    ValueExpression asExpression() {
        return valueExpression;
    }

    @Override
    void format(final PrintWriter writer, final int indent, final boolean multiLine) {
        writer.append("expression ");
        writer.append(quote(valueExpression.getExpressionString()));
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

    @Override
    public boolean equals(final Object other) {
        return other instanceof ExpressionValue && equals((ExpressionValue) other);
    }

    public boolean equals(final ExpressionValue other) {
        return this == other || other != null && valueExpression.equals(other.valueExpression);
    }

    @Override
    public int hashCode() {
        return valueExpression.hashCode();
    }

    @Override
    ModelValue resolve() {
        return new StringModelValue(valueExpression.resolveString());
    }

    @Override
    void write(final ModelWriter writer) throws IOException, ModelException {
        writer.writeExpression(valueExpression.getExpressionString());
    }

}
