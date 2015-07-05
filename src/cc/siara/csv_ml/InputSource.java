package cc.siara.csv_ml;

import java.io.InputStream;
import java.io.Reader;

public class InputSource {

    public static final short IS_BYTE_STREAM = 0;
    public static final short IS_CHAR_STREAM = 1;
    
    Reader reader = null;
    InputStream is = null;
    short type = 0;

    public InputSource(Reader r) {
        this.reader = r;
        type = IS_CHAR_STREAM;
    }
    public InputSource(InputStream i) {
        this.is = i;
        type = IS_BYTE_STREAM;
    }
    public Reader getReader() {
        return reader;
    }
    public InputStream getInputStream() {
        return is;
    }
    public short getType() {
        return type;
    }

}
