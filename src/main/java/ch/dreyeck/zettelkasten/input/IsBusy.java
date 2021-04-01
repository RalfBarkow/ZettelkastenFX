package ch.dreyeck.zettelkasten.input;

import nz.sodium.Cell;
import nz.sodium.Lambda1;
import nz.sodium.Stream;

public class IsBusy<A,B> {
    public IsBusy(Lambda1<Stream<A>, Stream<B>> action, Stream<A> sIn) {
        sOut = action.apply(sIn);
        busy = sOut.map(i -> false)
                .orElse(sIn.map(i -> true))
                .hold(false);
    }
    public final Stream<B> sOut;
    public final Cell<Boolean> busy;
}