package ch.dreyeck.zettelkasten.input;

import ch.dreyeck.zettelkasten.xml.Zettelkasten;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import nz.sodium.*;
import swidgets.SButton;
import swidgets.STextArea;
import swidgets.STextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Optional;

public class ZettelkastenFile {

    private static final
    ObjectProperty<Zettelkasten> ZETTELKASTEN_OBJECT_PROPERTY =
            new SimpleObjectProperty<>(new Zettelkasten());

    // TODO
    // Input should be a Zettelkasten zknFile.xml containing the <zettelkasten> with <zettel>s,
    // and the output value should be Optional<Zettelkasten> indicating
    //   either the <zettelkasten> retrieved from the ZettelkastenService (server)
    //   or an error if the value isn't present in the Optional.
    public static final
    Lambda1<Stream<String>, Stream<Optional<String>>> load =
            sPathname -> {
                StreamSink<Optional<String>> sZettel = new StreamSink<>();
                Listener l = sPathname.listenWeak(pathname -> new Thread(() -> {
                    Optional<String> zknFileXML = Optional.empty();
                    try {
                        zknFileXML = loadZknFileXML(pathname, zknFileXML);
                    } catch (JAXBException | NullPointerException e) {
                        e.printStackTrace();
                    } finally {
                        sZettel.send(zknFileXML);
                    }
                }).start());
                return sZettel.addCleanup(l);
            };

    private static Optional<String> loadZknFileXML(String pathname, Optional<String> zknFileXML) throws JAXBException {
        // loadZknFileXML() ; see ZettelkastenController.java
        final Unmarshaller unmarshaller =
                JAXBContext.newInstance(Zettelkasten.class).createUnmarshaller();
        ZETTELKASTEN_OBJECT_PROPERTY.set((Zettelkasten) unmarshaller.unmarshal(new File(pathname)));
        zknFileXML = Optional.ofNullable(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel().toString());
        // FIXME zettelListView.setItems(FXCollections.<Zettel>observableList(ZETTELKASTEN_OBJECT_PROPERTY.getValue().getZettel()));
        return zknFileXML;
    }

    public static void main(String[] args) {
        JFrame view = new JFrame("ZettelkastenFile load");
        GridBagLayout gridbag = new GridBagLayout();
        view.setLayout(gridbag);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0.0;
        c.weightx = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        Transaction.runVoid(() -> {
            STextField pathname = new STextField("", 25);
            CellLoop<Boolean> enabled = new CellLoop<>();
            SButton button = new SButton("load", enabled);
            Stream<String> sPathname = button.sClicked.snapshot(pathname.text);
            IsBusy<String, Optional<String>> ib =
                    new IsBusy<>(load, sPathname);
            Stream<String> sZettel = ib.sOut
                    .map(o -> o.orElse("ERROR!"));
            Cell<String> zettel = sZettel.hold("");
            Cell<String> output = zettel.lift(ib.busy, (def, bsy) ->
                    bsy ? "Loading ..." : def);
            enabled.loop(ib.busy.map(b -> !b));
            STextArea outputArea = new STextArea(output, enabled);
            view.add(pathname, c);
            c.gridx = 1;
            view.add(button, c);
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1.0;
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 1;
            view.add(new JScrollPane(outputArea), c);
        });

        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        view.setSize(500, 250);
        view.setVisible(true);
    }
}
