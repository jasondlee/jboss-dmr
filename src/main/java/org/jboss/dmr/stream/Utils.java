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
final class Utils {

    static final char[] ONES;
    static final char[] TENS;
    static final byte[] EMPTY_BYTES = new byte[ 0 ];
    static final char[] BASE64_ENC_TABLE = new char[ 64 ];
    static final char[] BASE64_NEWLINE = "\\r\\n".toCharArray();
    static final int[] BASE64_DEC_TABLE = new int[ 256 ];
    static final int[] HEX_TABLE = new int[ 256 ];
    static final int INCORRECT_DATA = -1;

    static {
        // initialize matrices for fast numbers encoding
        ONES = new char[ 100 ];
        TENS = new char[ 100 ];
        for ( int i = 0; i < 100; i++ ) {
            ONES[ i ] = ( char ) ( '0' + ( i % 10 ) );
            TENS[ i ] = ( char ) ( '0' + ( i / 10 ) );
        }
        // initialize matrix for base64 encoding
        int j = 0;
        for ( char c = 'A'; c <= 'Z'; c++ ) {
            BASE64_ENC_TABLE[ j++ ] = c;
        }
        for ( char c = 'a'; c <= 'z'; c++ ) {
            BASE64_ENC_TABLE[ j++ ] = c;
        }
        for ( char c = '0'; c <= '9'; c++ ) {
            BASE64_ENC_TABLE[ j++ ] = c;
        }
        BASE64_ENC_TABLE[ j++ ] = '+';
        BASE64_ENC_TABLE[ j ] = '/';
        // initialize matrix for base64 decoding
        for ( int i = 0; i < 256; i++ ) {
            BASE64_DEC_TABLE[ i ] = INCORRECT_DATA;
        }
        j = 0;
        for ( char c = 'A'; c <= 'Z'; c++ ) {
            BASE64_DEC_TABLE[ c ] = j++;
        }
        for ( char c = 'a'; c <= 'z'; c++ ) {
            BASE64_DEC_TABLE[ c ] = j++;
        }
        for ( char c = '0'; c <= '9'; c++ ) {
            BASE64_DEC_TABLE[ c ] = j++;
        }
        BASE64_DEC_TABLE[ '+' ] = j++;
        BASE64_DEC_TABLE[ '/' ] = j;
        // initialize matrix for hex decoding
        for ( int i = 0; i < 256; i++ ) {
            HEX_TABLE[ i ] = INCORRECT_DATA;
        }
        for ( char c = 'A'; c <= 'Z'; c++ ) {
            HEX_TABLE[ c ] = c - 'A' + 10;
        }
        for ( char c = 'a'; c <= 'z'; c++ ) {
            HEX_TABLE[ c ] = c - 'a' + 10;
        }
        for ( char c = '0'; c <= '9'; c++ ) {
            HEX_TABLE[ c ] = c - '0';
        }
    }

    private Utils() {
        // forbidden instantiation
    }

    static boolean isControl( final int c ) {
        return c <= '\u001F';
    }

    static boolean isBase64Char( final int c ) {
        return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z' || '0' <= c && c <= '9' || c == '+' || c == '/' || c == '=';
    }

    static boolean isWhitespace( final int c ) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    static boolean isNumberChar( final int c ) {
        return '0' <= c && c <= '9' || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E';
    }

    static boolean isDigit( final int c ) {
        return '0' <= c && c <= '9';
    }

    static boolean isHexNumberChar( final int c ) {
        return '0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F' || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E';
    }

    static int stringSizeOf( long l ) {
        int signSize = 1;
        if ( l >= 0 ) {
            signSize = 0;
            l = -l;
        }
        long temp = -10;
        for ( int j = 1; j < 19; j++ ) {
            if ( l > temp ) return j + signSize;
            temp = 10 * temp;
        }
        return 19 + signSize;
    }

    static int stringSizeOf( int i ) {
        int signSize = 1;
        if ( i >= 0 ) {
            signSize = 0;
            i = -i;
        }
        int temp = -10;
        for ( int j = 1; j < 10; j++ ) {
            if ( i > temp ) return j + signSize;
            temp = 10 * temp;
        }
        return 10 + signSize;
    }

}
