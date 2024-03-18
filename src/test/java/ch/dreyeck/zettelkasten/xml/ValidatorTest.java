package ch.dreyeck.zettelkasten.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidatorTest {

    @Test
    void testValidateXML_ValidXML() {
        // Provide paths to a valid XML and XSD file
        String xmlFilePath = "/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/tekom/zknFile.xml";
        String xsdFilePath = "/Users/rgb/Projects/ZettelkastenFX/src/main/resources/ch/dreyeck/zettelkasten/xml/zknFile.xsd";

        // Validate XML against XSD
        boolean isValid = Validator.validateXML(xmlFilePath, xsdFilePath);

        // Assert that XML is valid
        assertTrue(isValid, "XML should be valid against XSD");
    }

    @Test
    void testValidateXML_InvalidXML() {
        // Provide paths to an invalid XML and XSD file
        String xmlFilePath = "/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb/zknFile.xml";
        String xsdFilePath = "/Users/rgb/Projects/ZettelkastenFX/src/main/resources/ch/dreyeck/zettelkasten/xml/zknFile.xsd";

        // Validate XML against XSD
        boolean isValid = Validator.validateXML(xmlFilePath, xsdFilePath);

        // Assert that XML is not valid
        assertTrue(!isValid, "XML should not be valid against XSD");
    }
}
