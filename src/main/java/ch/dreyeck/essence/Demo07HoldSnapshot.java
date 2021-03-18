package ch.dreyeck.essence;

import nz.sodium.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Demo07HoldSnapshot {
    public static void main(String[] args) throws IOException {

        StreamSink<String> input = new StreamSink<>();

        Cell<Integer> outputCell =
                input.filter(x -> isNaturalNumber(x)).map(x -> str2Integer(x)).hold(0);
        Stream<Integer> outputStream = input.filter(x -> x.equals("snapshot")).snapshot(outputCell);

        Stream<String> inc = input.filter(x -> x.contains("increment counter"));

        Transaction.runVoid(() -> {
            CellLoop<Integer> counter = new CellLoop<>();
            counter.loop(inc.snapshot(counter, (__, n_) -> n_ + 1).hold(0));

            Stream<Integer> snapshot_of_counter = input.filter(x -> x.contains("take snapshot")).snapshot(counter);
            counter.listen(x -> System.out.println("counter = " + x));
        
        });

        outputCell.listen(x -> System.out.println("outputCell: " + x));
        outputStream.listen(x -> System.out.println("outputStream: " + x));

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String str = br.readLine();
            input.send(str);
        }
    }

    private static boolean isNaturalNumber(String str) {
        return str.matches("\\d+");
    }

    private static Integer str2Integer(String str) {
        return Integer.parseInt(str);
    }
}