package ch.dreyeck.essence;

import nz.sodium.Stream;
import nz.sodium.StreamSink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Demo02Filter {
    public static void main(String[] args) throws IOException {
        StreamSink<String> input = new StreamSink<>();
        Stream<String> output = input.filter(x -> x.contains("apple")).map(x -> "sweet " + x);
        output.listen(x -> System.out.println("ouput: " + x));

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String str = br.readLine();
            input.send(str);
        }

    }
}
