package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Output {

    private final File outputFile;
    private final BufferedWriter writer;
    private long previous;
    private final DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime( FormatStyle.SHORT )
//            .withLocale( Locale.UK )
            .withZone( ZoneId.systemDefault() );

    public Output(File outputFile) throws IOException {
        this.outputFile = outputFile;
        writer = new BufferedWriter(new FileWriter(outputFile));

    }

    public void close() throws IOException {
        writer.close();
    }

    public void write(String s) {
        try {
            writer.write(s + "\n");
            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start(String s) {
        try {
            Instant instant = Instant.now();
            writer.write("\n"+formatter.format(instant)+ ":\n");
            previous = instant.toEpochMilli();
            writer.write(s + "\n");
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void elapsed() {
        long epoch = Instant.now().toEpochMilli();
        try {
            writer.write("elapsed: " +(epoch - previous)+" ms\n");
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
