package ch.dreyeck.zettelkasten.input;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZipFilteredReader shows an example of filtering one or more matching
 * files from a ZipInputStream. Instead of expanding the whole archive
 * this uses the Function interface to only expand matching files.
 * <p>
 * Files are output to the OUTPUT_DIR directory.
 */
public class ZipFilteredReader {
    private final Path zipLocation;
    private final Path outputDirectory;

    /**
     * Constructs the filtered zip reader passing in the zip file to
     * be expanded by filter and the output directory
     *
     * @param zipLocation the zip file
     * @param outputDir   the output directory
     */
    public ZipFilteredReader(String zipLocation, String outputDir) {
        this.zipLocation = Paths.get(zipLocation);
        this.outputDirectory = Paths.get(outputDir);
    }

    /**
     * This method iterates through all entries in the zip archive. Each
     * entry is checked against the predicate (filter) that is passed to
     * the method. If the filter returns true, the entry is expanded,
     * otherwise it is ignored.
     *
     * @param filter the predicate used to compare each entry against
     */
    public void filteredExpandZipFile(Predicate<ZipEntry> filter) {
        // we open the zip file using a java 7 try with resources block
        try (ZipInputStream stream = new ZipInputStream(new FileInputStream(zipLocation.toFile()))) {

            // we now iterate through all files in the archive testing them
            // again the predicate filter that we passed in. Only items that
            // match the filter are expanded.
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (filter.test(entry)) {
                    extractFileFromArchive(stream, entry.getName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * We only get here when we the stream is located on a zip entry.
     * Now we can read the file data from the stream for this current
     * ZipEntry. Just like a normal input stream we continue reading
     * until read() returns 0 or less.
     */
    private void extractFileFromArchive(ZipInputStream stream, String outputName) {
        // build the path to the output file and then create the file
        String outpath = outputDirectory + "/" + outputName; //FIXME Remove this hard-coded path-delimiter.
        try (FileOutputStream output = new FileOutputStream(outpath)) {

            // create a buffer to copy through
            byte[] buffer = new byte[2048];

            // now copy out of the zip archive until all bytes are copied
            int len;
            while ((len = stream.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
