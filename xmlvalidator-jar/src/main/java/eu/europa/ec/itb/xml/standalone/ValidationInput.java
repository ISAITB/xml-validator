package eu.europa.ec.itb.xml.standalone;

import java.io.File;

/**
 * Created by simatosc on 12/08/2016.
 */
public class ValidationInput {

    private File inputFile;
    private String fileName;

    public ValidationInput(File inputFile, String validationType) {
        this.inputFile = inputFile;
        this.fileName = validationType;
    }

    public File getInputFile() {
        return inputFile;
    }

    public String getFileName() {
        return fileName;
    }
}
