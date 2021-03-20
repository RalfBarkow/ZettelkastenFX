package ch.dreyeck.essence;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class Demo07HoldSnapshotTest {

    @Test
    void first_call() throws IOException {
        Object output = Demo07HoldSnapshot.allowSendToBeCalled();
        Assertions.assertEquals("counter = 0\n" +
                "outputCell: 0", (String) output);

    }

    @Test
    void send() {

        // 1) create the desired String

        // 2) convert that String to an InputStream

        // 3) send that InputStream to Demo07HoldSnapshot

    }
}