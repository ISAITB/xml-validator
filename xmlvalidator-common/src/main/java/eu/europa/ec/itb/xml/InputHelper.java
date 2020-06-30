package eu.europa.ec.itb.xml;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.xml.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.Utils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class InputHelper extends BaseInputHelper<FileManager, DomainConfig, ApplicationConfig> {

    @Override
    public List<FileInfo> getExternalArtifactInfo(AnyContent containerContent, DomainConfig domainConfig, String validationType, String artifactType, String artifactContentInputName, String artifactEmbeddingMethodInputName, File parentFolder) {
        List<FileInfo> filesContent = new ArrayList<>();
        /*
          This is a map with three items:
          - "content": The content to consider.
          - "type": For schemas this is "zip" or "xsd" whereas for schematron this is "sch" or "xsl".
          - "embeddingMethod": The way to interpet the "content" input
         */
        // Get content input.
        List<AnyContent> contentInput =  Utils.getInputFor(containerContent.getItem(), ValidationConstants.INPUT_EXTERNAL_ARTIFACT_CONTENT);
        if (contentInput.size() != 1) {
            throw new IllegalArgumentException("A single \""+ValidationConstants.INPUT_EXTERNAL_ARTIFACT_CONTENT+"\" input is expected per provided validation artifact");
        }
        // Get value embedding method.
        ValueEmbeddingEnumeration method = getEmbeddingMethodInput(containerContent.getItem());
        String type = null;
        List<AnyContent> typeInput =  Utils.getInputFor(containerContent.getItem(), ValidationConstants.INPUT_EXTERNAL_ARTIFACT_TYPE);
        if (!typeInput.isEmpty()) {
            type = typeInput.get(0).getValue();
        }
        if (type == null) {
            if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
                type = "xsd";
            } else if (DomainConfig.ARTIFACT_TYPE_SCHEMATRON.equals(artifactType)) {
                type = "sch";
            }
        }
        if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType) && !"xsd".equals(type) && !"zip".equals(type)) {
            throw new IllegalArgumentException("Invalid type value for provided XSD ["+type+"]");
        } else if (DomainConfig.ARTIFACT_TYPE_SCHEMATRON.equals(artifactType) && !"sch".equals(type) && !"xsl".equals(type)) {
            throw new IllegalArgumentException("Invalid type value for provided schematron ["+type+"]");
        }
        try {
            FileInfo fileContent = getExternalFileInfo(contentInput.get(0), type, artifactType, method, parentFolder);
            if (fileContent.getFile() != null) {
                filesContent.add(fileContent);
            }
            return filesContent;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to extract the content for a provided validation artifact", e);
        }
    }

    private ValueEmbeddingEnumeration getEmbeddingMethodInput(List<AnyContent> inputs) {
        ValueEmbeddingEnumeration result = null;
        if (inputs != null) {
            List<AnyContent> foundInputs = Utils.getInputFor(inputs, ValidationConstants.INPUT_EMBEDDING_METHOD);
            if (!foundInputs.isEmpty()) {
                result = ValueEmbeddingEnumeration.fromValue(foundInputs.get(0).getValue());
            }
        }
        return result;
    }

    private FileInfo getExternalFileInfo(AnyContent content, String type, String artifactType, ValueEmbeddingEnumeration method, File parentFolder) throws IOException, URISyntaxException {
        FileInfo fileContent;
        if (DomainConfig.ARTIFACT_TYPE_SCHEMATRON.equals(artifactType)) {
            fileContent = new FileInfo(fileManager.storeFileContent(parentFolder, content.getValue(), method, artifactType));
            String mimeType = fileManager.checkContentType(fileContent.getFile());
            if (!appConfig.getAcceptedSchematronMimeType().contains(mimeType)) {
                throw new IllegalArgumentException("Unsupported mime type ["+mimeType+"] for provided schematron");
            }
        } else {
            if ("xsd".equals(type)) {
                fileContent = new FileInfo(fileManager.storeFileContent(parentFolder, content.getValue(), method, artifactType));
                String mimeType = fileManager.checkContentType(fileContent.getFile());
                if (!appConfig.getAcceptedSchemaMimeType().contains(mimeType)) {
                    throw new IllegalArgumentException("Unsupported mime type ["+mimeType+"] for provided schema");
                }
            } else {
                // zip - can only be provided as URI or BASE64
                ValueEmbeddingEnumeration contentType = (method!=null)?method:content.getEmbeddingMethod();
                if (ValueEmbeddingEnumeration.STRING.equals(contentType)) {
                    throw new IllegalArgumentException("A zip archive containing the XSD cannot be provided with an embedding method of STRING");
                } else {
                    File zipFile;
                    if (ValueEmbeddingEnumeration.BASE_64.equals(contentType)) {

                        byte[] contentBytes = Base64.getDecoder().decode(content.getValue());
                        String mimeType = fileManager.checkContentType(contentBytes);
                        if (appConfig.getAcceptedZipMimeType().contains(mimeType)) {
                            throw new IllegalArgumentException("Unexpected mime type ["+mimeType+"] for XSD zip archive");
                        }
                        zipFile = fileManager.unzipFile(parentFolder, contentBytes);
                    } else {
                        String mimeType = fileManager.checkContentTypeUrl(content.getValue());
                        if (appConfig.getAcceptedZipMimeType().contains(mimeType)) {
                            throw new IllegalArgumentException("Unexpected mime type ["+mimeType+"] for XSD zip archive");
                        }
                        zipFile = fileManager.unzipFile(parentFolder, fileManager.getFileFromURL(parentFolder, content.getValue(), null));
                    }
                    if (validateSchemaZip(zipFile)) {
                        fileContent = new FileInfo(zipFile);
                    } else {
                        throw new IllegalArgumentException("When XSD configuration is provided as a ZIP archive it needs to include a single XSD at its root (and any other folders with imported XSDs)");
                    }
                }
            }
        }
        return fileContent;
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
