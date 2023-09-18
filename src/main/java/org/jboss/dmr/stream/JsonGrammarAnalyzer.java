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

package org.jboss.dmr.stream;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JsonGrammarAnalyzer {

    private static final byte LIST_START = 1;
    private static final byte OBJECT_START = 2;
    private static final byte PROPERTY_START = 4;
    private static final byte STRING = 8;
    private static final byte COLON = 16;
    private boolean canWriteComma;
    private boolean canWriteColon;
    private boolean expectedPropertyEnd;
    private byte[] stack = new byte[ 8 ];
    private int index;
    ModelEvent currentEvent;
    boolean finished;

    JsonGrammarAnalyzer() {
    }

    boolean isColonExpected() {
        return canWriteColon;
    }

    boolean isCommaExpected() {
        return canWriteComma;
    }

    void putObjectEnd() throws ModelException {
        // preconditions
        if ( finished || index == 0 || stack[ index - 1 ] != OBJECT_START || currentEvent == null ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.OBJECT_END;
        index--;
        if ( index > 0 ) {
            if ( stack[ index - 1 ] == COLON ) {
                index -= 2;
                canWriteComma = ( stack[ index - 1 ] & ( OBJECT_START | LIST_START ) ) != 0;
                expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
            } else if ( stack[ index - 1 ] == LIST_START ) {
                canWriteComma = true;
            }
        }
        if ( index == 0 ) {
            finished = true;
        }
    }

    void putListEnd() throws ModelException {
        // preconditions
        if ( finished || index == 0 || stack[ index - 1 ] != LIST_START || currentEvent == null ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.LIST_END;
        index--;
        if ( index > 0 ) {
            if ( stack[ index - 1 ] == COLON ) {
                index -= 2;
                canWriteComma = ( stack[ index - 1 ] & ( OBJECT_START | LIST_START ) ) != 0;
                expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
            } else if ( stack[ index - 1 ] == LIST_START ) {
                canWriteComma = true;
            }
        } else {
            finished = true;
        }
    }

    void putPropertyEnd() throws ModelException {
        // preconditions
        if ( finished || index == 0 || stack[ index - 1 ] != PROPERTY_START || !expectedPropertyEnd || currentEvent == null ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.PROPERTY_END;
        expectedPropertyEnd = false;
        index--;
        if ( index > 0 ) {
            if ( stack[ index - 1 ] == COLON ) {
                index -= 2;
                canWriteComma = ( stack[ index - 1 ] & ( OBJECT_START | LIST_START ) ) != 0;
                expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
            } else if ( stack[ index - 1 ] == LIST_START ) {
                canWriteComma = true;
            }
        } else {
            finished = true;
        }
    }

    void putExpression() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.EXPRESSION;
        if ( index == 0 ) {
            finished = true;
            return;
        }
        if ( stack[ index - 1 ] == COLON ) {
            index -= 2;
        }
        canWriteComma = true;
        expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
    }

    void putNumber( final ModelEvent numberEvent ) throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = numberEvent;
        if ( index == 0 ) {
            finished = true;
            return;
        }
        if ( stack[ index - 1 ] == COLON ) {
            index -= 2;
        }
        canWriteComma = true;
        expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
    }

    void putBoolean() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.BOOLEAN;
        if ( index == 0 ) {
            finished = true;
            return;
        }
        if ( stack[ index - 1 ] == COLON ) {
            index -= 2;
        }
        canWriteComma = true;
        expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
    }

    void putBytes() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.BYTES;
        if ( index == 0 ) {
            finished = true;
            return;
        }
        if ( stack[ index - 1 ] == COLON ) {
            index -= 2;
        }
        canWriteComma = true;
        expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
    }

    void putUndefined() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.UNDEFINED;
        if ( index == 0 ) {
            finished = true;
            return;
        }
        if ( stack[ index - 1 ] == COLON ) {
            index -= 2;
        }
        canWriteComma = true;
        expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
    }

    void putType() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.TYPE;
        if ( index == 0 ) {
            finished = true;
            return;
        }
        if ( stack[ index - 1 ] == COLON ) {
            index -= 2;
        }
        canWriteComma = true;
        expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
    }

    void putString() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || expectedPropertyEnd || index != 0 && ( stack[ index - 1 ] & ( OBJECT_START | LIST_START | PROPERTY_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.STRING;
        if ( index == 0 ) {
            finished = true;
            return;
        }
        if ( stack[ index - 1 ] == OBJECT_START ) {
            if ( index == stack.length ) doubleStack();
            stack[ index++ ] = STRING;
            canWriteColon = true;
            return;
        }
        if ( stack[ index - 1 ] == PROPERTY_START ) {
            if ( index == stack.length ) doubleStack();
            stack[ index++ ] = STRING;
            canWriteColon = true;
            return;
        }
        if ( stack[ index - 1 ] == COLON ) {
            index -= 2;
        }
        canWriteComma = true;
        expectedPropertyEnd = stack[ index - 1 ] == PROPERTY_START;
    }

    void putObjectStart() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.OBJECT_START;
        if ( index == stack.length ) doubleStack();
        stack[ index++ ] = OBJECT_START;
    }

    void putListStart() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.LIST_START;
        if ( index == stack.length ) doubleStack();
        stack[ index++ ] = LIST_START;
    }

    void putPropertyStart() throws ModelException {
        // preconditions
        if ( finished || canWriteComma || index != 0 && ( stack[ index - 1 ] & ( LIST_START | COLON ) ) == 0 ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = ModelEvent.PROPERTY_START;
        if ( index == stack.length ) doubleStack();
        stack[ index++ ] = PROPERTY_START;
    }

    void putColon() throws ModelException {
        // preconditions
        if ( finished || index == 0 || stack[ index - 1 ] != STRING ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = null;
        if ( index == stack.length ) doubleStack();
        stack[ index++ ] = COLON;
        canWriteColon = false;
    }

    void putComma() throws ModelException {
        // preconditions
        if ( finished || !canWriteComma ) {
            throw newModelException( getExpectingTokensMessage() );
        }
        // implementation
        currentEvent = null;
        canWriteComma = false;
    }

    String getExpectingTokensMessage() {
        if ( index == 0 ) {
            if ( !finished ) {
                return "Expecting OBJECT_START or LIST_START or PROPERTY_START or STRING or EXPRESSION or BYTES or NUMBER or BOOLEAN or TYPE or UNDEFINED";
            } else {
                return "Expecting EOF";
            }
        }
        if ( stack[ index - 1 ] == OBJECT_START ) {
            if ( !canWriteComma ) {
                if ( currentEvent != null ) {
                    return "Expecting OBJECT_END or STRING";
                } else {
                    return "Expecting STRING";
                }
            } else {
                return "Expecting ',' or OBJECT_END";
            }
        }
        if ( stack[ index - 1 ] == PROPERTY_START ) {
            if ( !expectedPropertyEnd ) {
                return "Expecting STRING";
            } else {
                return "Expecting PROPERTY_END";
            }
        }
        if ( stack[ index - 1 ] == LIST_START ) {
            if ( !canWriteComma ) {
                if ( currentEvent != null ) {
                    return "Expecting LIST_END or OBJECT_START or LIST_START or PROPERTY_START or STRING or EXPRESSION or BYTES or NUMBER or BOOLEAN or TYPE or UNDEFINED";
                } else {
                    return "Expecting OBJECT_START or LIST_START or PROPERTY_START or STRING or EXPRESSION or BYTES or NUMBER or BOOLEAN or TYPE or UNDEFINED";
                }
            } else {
                return "Expecting ',' or LIST_END";
            }
        }
        if ( stack[ index - 1 ] == COLON ) {
            return "Expecting OBJECT_START or LIST_START or PROPERTY_START or STRING or EXPRESSION or BYTES or NUMBER or BOOLEAN or TYPE or UNDEFINED";
        }
        if ( stack[ index - 1 ] == STRING ) {
            return "Expecting ':'";
        }
        throw new IllegalStateException();
    }

    private void doubleStack() {
        final byte[] oldData = stack;
        stack = new byte[ oldData.length * 2 ];
        System.arraycopy( oldData, 0, stack, 0, oldData.length );
    }

    ModelException newModelException( final String s ) {
        finished = true;
        currentEvent = null;
        return new ModelException( s );
    }

    ModelException newModelException( final String s, final Throwable t ) {
        finished = true;
        currentEvent = null;
        return new ModelException( s, t );
    }

}
