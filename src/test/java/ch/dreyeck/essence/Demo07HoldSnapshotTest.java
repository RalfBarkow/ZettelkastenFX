package ch.dreyeck.essence;

import nz.sodium.StreamSink;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static ch.dreyeck.essence.Demo07HoldSnapshot.frpEngine;

class Demo07HoldSnapshotTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        // Reassign the "standard" output stream.
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    void send_snapshot_command_and_get_outputStream() {

        frpEngine().send("snapshot");

        Assertions.assertEquals(
                "counter = 0\noutputCell: 0\noutputStream: 0",
                outputStreamCaptor.toString()
                        .trim());
    }

    @Test
    void send_increment_counter_command_and_get_counter() {
        frpEngine().send("increment counter");
        Assertions.assertEquals(
                "counter = 0\n" +
                        "outputCell: 0\n" +
                        "counter = 1",
                outputStreamCaptor.toString()
                        .trim());
    }

    @Test
    void send_take_snapshot_command_and_get_snapshot_of_counter() {
        frpEngine().send("take snapshot");
        Assertions.assertEquals(
                "counter = 0\n" +
                        "outputCell: 0\n" +
                        "snapshot of counter = 0",
                outputStreamCaptor.toString()
                        .trim());
    }

    @Test
    void increment_counter_and_snapshot_of_counter() {
        frpEngine().send("increment counter and take snapshot");
        Assertions.assertEquals(
                "counter = 0\n" +
                        "outputCell: 0\n" +
                        "snapshot of counter = 0\n" +
                        "counter = 1",
                outputStreamCaptor.toString()
                        .trim());
    }

    @Test
    void more_than_one_command(){
        StreamSink<String> into = frpEngine();
        into.send("10");
        into.send("20");
        into.send("snapshot");

        Assertions.assertEquals(
                "counter = 0\n" +
                        "outputCell: 0\n" +
                        "outputCell: 10\n" +
                        "outputCell: 20\n" +
                        "outputStream: 20",
                outputStreamCaptor.toString()
                        .trim());
    }
}