package ch.dreyeck.essence;

import nz.sodium.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Demo07HoldSnapshot {

    public Demo07HoldSnapshot() {
        createEngine();
    }

    public static void main(String[] args) throws IOException {

        /*
         Construct a StreamSink, which is a subclass of Stream that adds a method called send(),
         allowing you to push or send values into the stream.
        */
        StreamSink<String> in = createEngine();

        // System.in – the "standard" input stream
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {

            // Read a line of text and send that String
            do {
                String str = br.readLine();
                in.send(str);
            } while (true);
        }
    }

    /*
     A stream that allows values to be pushed into it,
     acting as an interface between the world of I/O and the world of FRP.

     Code that exports StreamSinks for read-only use should downcast to Stream.
    */
    public static StreamSink<String> createEngine() {

        StreamSink<String> input = new StreamSink<>();

        Cell<Integer> outputCell =
                input.filter(x -> isNaturalNumber(x)).map(x -> str2Integer(x)).hold(0);
        Stream<Integer> outputStream = input.filter(x -> x.equals("snapshot")).snapshot(outputCell);

        Stream<String> inc = input.filter(x -> x.contains("increment counter"));

        Transaction.runVoid(() -> {
            CellLoop<Integer> counter = new CellLoop<>();
            counter.loop(inc.snapshot(counter, (__, n_) -> n_ + 1).hold(0));

            Stream<Integer> snapshot_of_counter = input.filter(x -> x.contains("take snapshot")).snapshot(counter);
            counter.listen(x -> logMessage(x, "counter = "));
            snapshot_of_counter.listen(x -> logMessage(x, "snapshot of counter = "));
        });

        outputCell.listen(x -> logMessage(x, "outputCell: "));
        outputStream.listen(x -> logMessage(x, "outputStream: "));
        return input;
    }

    private static void logMessage(Integer x, String s) {
        System.out.println(s + x);
    }

    private static boolean isNaturalNumber(String str) {
        return str.matches("\\d+");
    }

    private static Integer str2Integer(String str) {
        return Integer.parseInt(str);
    }
}