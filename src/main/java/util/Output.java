package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Output {

    private final File outputFile;
    private final BufferedWriter writer;
    private Instant previous;
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
//            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start(String s) {
        try {
            previous= Instant.now();
            writer.write("\n"+formatter.format(previous)+ ":\n");
            writer.write(s + "\n");
//            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public long elapsed() {
        Instant now = Instant.now();
        long elapsed = Duration.between(previous, now).toMillis();
        try {
            writer.write("elapsed: " + elapsed+" ms\n");
//            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return elapsed;
    }
}
