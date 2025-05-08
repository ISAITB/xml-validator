package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.validation.commons.FileInfo;

import java.io.File;

/**
 * Information on a Schematron file.
 */
public class SchematronFileInfo extends FileInfo {

    private final boolean supportPureValidation;

    /**
     * Constructor.
     *
     * @param fileInfo The schematron file information.
     * @param supportPureValidation Whether the schematron supports validation as 'pure' schematron.
     */
    public SchematronFileInfo(FileInfo fileInfo, boolean supportPureValidation) {
        this(fileInfo.getFile(), fileInfo.getType(), supportPureValidation);
    }

    /**
     * Constructor.
     *
     * @param file The schematron file.
     * @param type The schematron type.
     * @param supportPureValidation Whether the schematron supports validation as 'pure' schematron.
     */
    public SchematronFileInfo(File file, String type, boolean supportPureValidation) {
        super(file, type);
        this.supportPureValidation = supportPureValidation;
    }

    /**
     * @return Whether the schematron file supports validation via the 'pure' approach.
     */
    public boolean isSupportPureValidation() {
        return supportPureValidation;
    }
}
