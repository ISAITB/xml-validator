package eu.europa.ec.itb.xml;

import com.gitb.vs.ValidateRequest;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.xml.util.FileManager;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class InputHelper extends BaseInputHelper<FileManager, DomainConfig, ApplicationConfig> {

    public List<FileInfo> validateExternalArtifacts(DomainConfig domainConfig, ValidateRequest validateRequest, String artifactContainerInputName, String artifactContentInputName, String artifactEmbeddingMethodInputName, String validationType, String artifactType, File parentFolder) {
        List<FileInfo> artifactContents = super.validateExternalArtifacts(domainConfig, validateRequest, artifactContainerInputName, artifactContentInputName, artifactEmbeddingMethodInputName, validationType, artifactType, parentFolder);
        List<FileInfo> artifactsToReturn = new ArrayList<>();
        for (FileInfo fileInfo: artifactContents) {
            if (fileInfo.getFile() != null) {
                File rootFile = fileManager.unzipFile(parentFolder, fileInfo.getFile());
                if (rootFile == null) {
                    artifactsToReturn.add(new FileInfo(fileManager.preprocessFileIfNeeded(domainConfig, validationType, artifactType, fileInfo.getFile(), true)));
                } else {
                    // ZIP File
                    boolean proceed = false;
                    if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
                        proceed = validateSchemaZip(rootFile);
                    } else {
                        proceed = true;
                    }
                    if (proceed) {
                        artifactsToReturn.addAll(fileManager.getLocalValidationArtifacts(rootFile, DomainConfig.ARTIFACT_TYPE_SCHEMA));
                    } else {
                        throw new IllegalStateException("An error occurred during the validation of the external XSD ZIP File: XSD configuration needs to include a single XSD at its root (and any folders with sub-folders and other imported XSDs).");
                    }
                }
            }
        }
        return artifactsToReturn;
    }

    public boolean validateSchemaZip(File rootFolder) {
        int iRootFiles = 0;
        //1 file as root, other files in a folder.
        if (rootFolder.isFile()) {
            iRootFiles++;
        } else {
            // List all files.
            File[] files = rootFolder.listFiles();
            if (files != null) {
                for (File aSchemaFile: files) {
                    if (aSchemaFile.isFile()) {
                        iRootFiles++;
                    }
                }
            }
        }
        return iRootFiles == 1;
    }
}
