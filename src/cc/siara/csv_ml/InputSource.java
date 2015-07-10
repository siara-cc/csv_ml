/*
 * Copyright (C) 2015 Siara Logics (cc)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Arundale R.
 *
 */
package cc.siara.csv_ml;

import java.io.InputStream;
import java.io.Reader;

/**
 * InputSource enables the Input to be given either as a character stream
 * (java.io.Reader) or a byte stream (InputStream)
 * 
 * @author Arundale R.
 * @since 1.0
 */
public class InputSource {

    public static final short IS_BYTE_STREAM = 0;
    public static final short IS_CHAR_STREAM = 1;

    // Members - type determines which one
    // is used (Reader or InputStream)
    short type = 0;
    Reader reader = null;
    InputStream is = null;

    /**
     * Sets source as r
     * 
     * @param r
     *            java.io.Reader
     */
    public InputSource(Reader r) {
        this.reader = r;
        type = IS_CHAR_STREAM;
    }

    /**
     * Sets source as i
     * 
     * @param i
     *            java.io.InputStream
     */
    public InputSource(InputStream i) {
        this.is = i;
        type = IS_BYTE_STREAM;
    }

    /**
     * Returns reader
     * 
     * @return java.io.Reader
     */
    public Reader getReader() {
        return reader;
    }

    /**
     * Returns is
     * 
     * @return java.io.InputStream
     */
    public InputStream getInputStream() {
        return is;
    }

    /**
     * Returns the type of input
     * 
     * @return type
     */
    public short getType() {
        return type;
    }

}
