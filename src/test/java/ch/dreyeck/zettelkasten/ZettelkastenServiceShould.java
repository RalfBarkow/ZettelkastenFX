package ch.dreyeck.zettelkasten;

import org.junit.jupiter.api.Test;

public class ZettelkastenServiceShould {
    @Test
    void return_ZIP_file_entries() {

        ZettelkastenService zettelkastenService = new ZettelkastenService();

        /*
         https://stackoverflow.com/questions/23260378/tdd-steps-to-write-junit-tests-for-decorator-pattern#23270462
         ethanfar:
         You could try to write a test method
         that receives the input stream decorating a mocked ZipFile that you will provide.
         Create the mock so that it returns fake ZipEntry instances that you can later
         verify against the ZipInputStream's getNextEntry() method.
         This way you can test that the returned ZipInputStream is really decorating the provided ZipFile
         (ethanfar would suggest a name such as testReturnedZipInputStreamDecoratesProvidedZipFile()
         */

        /*
         What is a "decorating input stream" ?
         Examples:
         - Wrap/Decorate the JmsInputStream with a DataInputStream
              The getInputStream Method […] or any other <read> method on any decorating input stream
         - Wrap/Decorate the JmsOutputStream with a DataOutputStream
         - Decorate the output stream with PrintWriter
         - Decorating input stream with Scanner
        */

    }
}
