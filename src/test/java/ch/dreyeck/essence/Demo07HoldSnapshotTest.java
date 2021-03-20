package ch.dreyeck.essence;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static ch.dreyeck.essence.Demo07HoldSnapshot.createEngine;

class Demo07HoldSnapshotTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    void send_snapshot_command_and_get_outputStream() {

        // 1) create the desired String

        // 2) convert that String to an InputStream

        // 3) send that InputStream to Demo07HoldSnapshot

        createEngine().send("snapshot");

        Assertions.assertEquals(
                "counter = 0\noutputCell: 0\noutputStream: 0",
                outputStreamCaptor.toString()
                        .trim());
    }

    @Test
    void send_increment_counter_command_and_get_counter() {
        createEngine().send("increment counter");

        Assertions.assertEquals(
                "counter = 0\n" +
                        "outputCell: 0\n" +
                        "counter = 1",
                outputStreamCaptor.toString()
                        .trim());
    }

    @Test
    void send_take_snapshot_command_and_get_snapshot_of_counter() {
        createEngine().send("take snapshot");

        Assertions.assertEquals(
                "counter = 0\n" +
                        "outputCell: 0\n" +
                        "snapshot of counter = 0",
                outputStreamCaptor.toString()
                        .trim());
    }

    @Test
    void increment_counter_and_snapshot_of_counter() {
        createEngine().send("increment counter and take snapshot");

        Assertions.assertEquals(
                "counter = 0\n" +
                        "outputCell: 0\n" +
                        "snapshot of counter = 0\n" +
                        "counter = 1",
                outputStreamCaptor.toString()
                        .trim());
    }

}