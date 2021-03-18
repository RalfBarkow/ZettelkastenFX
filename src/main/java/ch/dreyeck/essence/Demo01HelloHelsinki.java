package ch.dreyeck.essence;

import nz.sodium.Stream;
import nz.sodium.StreamSink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Demo01HelloHelsinki {
    public static void main(String[] args) throws IOException {
        StreamSink<String> input = new StreamSink<>();
        Stream<String> ouput = input.map(string -> "Hello " + string + " Helsinki !");
        ouput.listen(x -> System.out.println(x));

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String string = bufferedReader.readLine();
            input.send(string);
        }
    }
}
