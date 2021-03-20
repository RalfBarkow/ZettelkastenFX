package ch.dreyeck.essence;

import org.junit.jupiter.api.Test;

class Demo07HoldSnapshotTest {

    @Test
    void send_snapshot_command_and_get_outputStream() {

        // 1) create the desired String

        // 2) convert that String to an InputStream

        // 3) send that InputStream to Demo07HoldSnapshot
        Demo07HoldSnapshot.createEngine().send("snapshot");

    }

    @Test
    void send_increment_counter_command_and_get_counter(){
        Demo07HoldSnapshot.createEngine().send("increment counter");
    }

    @Test
    void send_take_snapshot_command_and_get_snapshot_of_counter(){
        Demo07HoldSnapshot.createEngine().send("take snapshot");
    }

    @Test
    void increment_counter_and_snapshot_of_counter(){
        Demo07HoldSnapshot.createEngine().send("increment counter and take snapshot");
    }

}