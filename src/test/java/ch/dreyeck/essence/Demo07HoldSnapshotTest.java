package ch.dreyeck.essence;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class Demo07HoldSnapshotTest {

    @Test
    void first_call() throws IOException {
        Object output = Demo07HoldSnapshot.push();
        Assert.assertEquals("counter = 0\n" +
                "outputCell: 0", (String) output);

    }
}