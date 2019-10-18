package unibz.shapes.util;

import java.io.BufferedWriter;
import java.io.StringWriter;

public class StringOutput extends Output {

    public StringOutput() {
        this.writer = new BufferedWriter(new StringWriter());
    }
}
