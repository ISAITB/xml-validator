package eu.europa.ec.itb.einvoice;

import java.io.File;
import java.io.InputStream;

/**
 * Created by simatosc on 12/08/2016.
 */
public class ValidationInput {

    private File inputFile;
    private String validationType;

    public ValidationInput(File inputFile, String validationType) {
        this.inputFile = inputFile;
        this.validationType = validationType;
    }

    public File getInputFile() {
        return inputFile;
    }

    public String getValidationType() {
        return validationType;
    }
}
