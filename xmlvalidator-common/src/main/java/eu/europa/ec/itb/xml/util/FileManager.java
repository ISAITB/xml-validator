package eu.europa.ec.itb.xml.util;

import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages file-system operations.
 */
@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

    /**
     * @see BaseFileManager#getFileExtension(String)
     *
     * @param contentType The content type (ignored).
     * @return Always "xml".
     */
    @Override
    public String getFileExtension(String contentType) {
        return "xml";
    }

    /**
     * Check if the provided file's extension is accepted as being of the provided artifact type.
     *
     * @param file The file to check.
     * @param artifactType The artifact type to check for.
     * @return True if the file is accepted.
     */
    @Override
    protected boolean isAcceptedArtifactFile(File file, String artifactType) {
        if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
            return config.getAcceptedSchemaExtensions().contains(FilenameUtils.getExtension(file.getName().toLowerCase()));
        } else {
            return config.getAcceptedSchematronExtensions().contains(FilenameUtils.getExtension(file.getName().toLowerCase()));
        }
    }

    /**
     * Write the provided XML content to a file.
     *
     * @param domain The current validation domain.
     * @param xml The XML content.
     * @throws IOException If an error occurs writing the file.
     */
    public String writeXML(String domain, String xml) throws IOException {
        UUID fileUUID = UUID.randomUUID();
        String xmlID = domain+"_"+ fileUUID;
        File outputFile = new File(getReportFolder(), getInputFileName(xmlID));
        outputFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(outputFile, xml, Charset.defaultCharset());
        return xmlID;
    }

    /**
     * Returns the name of an input file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @return The file name.
     */
    public String getInputFileName(String uuid) {
        return config.getInputFilePrefix()+uuid+".xml";
    }

    /**
     * Returns the name of a XML report file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @return The file name.
     */
    public String getReportFileNameXml(String uuid) {
        return config.getReportFilePrefix()+uuid+".xml";
    }

    /**
     * Returns the name of a PDF report file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @return The file name.
     */
    public String getReportFileNamePdf(String uuid) {
        return config.getReportFilePrefix()+uuid+".pdf";
    }

    /**
     * Check if the provided file is accepted as input in terms of its content type.
     *
     * @param file The file to check.
     * @return True if it is accepted.
     * @throws IOException If a byte sampling error occurs.
     */
    public boolean checkFileType(File file) throws IOException {
        return config.getAcceptedMimeTypes().contains(checkContentType(file));
    }

    /**
     * Retrieve the content type of the provided file via byte sampling.
     *
     * @param content The file to check.
     * @return The mime type.
     * @throws IOException If an IO error occurs.
     */
    public String checkContentType(File content) throws IOException {
        Tika tika = new Tika();
        String mimeType = "";
        try (InputStream in = Files.newInputStream(content.toPath())){
            mimeType = tika.detect(in);
        }
        return mimeType;
    }

    /**
     * Extract the provided ZIP stream to the provided temp folder.
     *
     * @param zis The stream to unzip.
     * @param tmpFolder The folder to unzip to.
     * @return True if the ZIP file was not empty.
     * @throws IOException If an IO error occurs.
     */
    private boolean getZipFiles(ZipInputStream zis, File tmpFolder) throws IOException{
        byte[] buffer = new byte[1024];
        ZipEntry zipEntry = zis.getNextEntry();

        if(zipEntry == null) {
            return false;
        }

        while (zipEntry != null) {
            Path tmpPath = createFile(tmpFolder, null, zipEntry.getName());

            if(zipEntry.isDirectory()) {
                tmpPath.toFile().mkdirs();
            }else {
                File f = tmpPath.toFile();
                FileOutputStream fos = new FileOutputStream(f);
                int len;

                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }

            zipEntry = zis.getNextEntry();
        }

        return true;
    }

    /**
     * Unzip the provided file to the provided temp folder.
     *
     * @param parentFolder The folder to unzip in.
     * @param zipFile The ZIP file to unzip.
     * @return The unzipped folder.
     */
    public File unzipFile(File parentFolder, File zipFile){
        File unzipFiles;
        boolean isZip;
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            File rootFolder = createTemporaryFolderPath(parentFolder);
            rootFolder.mkdirs();
            isZip = getZipFiles(zis, rootFolder);
            unzipFiles = rootFolder;
            zis.closeEntry();
            zis.close();
        } catch (Exception e) {
            return null;
        }
        if (isZip) {
            return unzipFiles;
        } else {
            return null;
        }
    }

    /**
     * @see BaseFileManager#getFileFromURL(File, String, String, String, File, String, String)
     *
     * In case of XML schemas ensure that imported schemas are also downloaded and cached.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param extension The file extension for the created file.
     * @param fileName The name of the file to use.
     * @param preprocessorFile An optional file for a preprocessing resource to be used to determine the final loaded file.
     * @param preprocessorOutputExtension The file extension for the file produced via preprocessing (if applicable).
     * @param artifactType The type of validation artifact.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    @Override
    public File getFileFromURL(File targetFolder, String url, String extension, String fileName, File preprocessorFile, String preprocessorOutputExtension, String artifactType) throws IOException {
        File savedFile = super.getFileFromURL(targetFolder, url, extension, fileName, preprocessorFile, preprocessorOutputExtension, artifactType);
        if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
            retrieveSchemasForImports(url, new File(savedFile.getParent(), "import"));
        }
        return savedFile;
    }

    /**
     * Retrieve the schema for the provided URI and store it in the target folder.
     *
     * @param rootURI The URI.
     * @param rootFolder The folder.
     */
    private void retrieveSchemasForImports(String rootURI, File rootFolder) {
        XMLSchemaLoader xsdLoader = new XMLSchemaLoader();
        XSModel xsdModel = xsdLoader.loadURI(rootURI);
        XSNamespaceItemList xsdNamespaceItemList = xsdModel.getNamespaceItems();
        Set<String> documentLocations = new HashSet<>();
        for (int i=0; i<xsdNamespaceItemList.getLength(); i++) {
            XSNamespaceItem xsdItem = (XSNamespaceItem) xsdNamespaceItemList.get(i);
            StringList sl = xsdItem.getDocumentLocations();
            for(int k=0; k<sl.getLength(); k++) {
                if(!documentLocations.contains(sl.item(k))) {
                    String currentLocation = (String)sl.get(k);
                    try {
                        getFileFromURL(rootFolder, currentLocation);
                        documentLocations.add(currentLocation);
                    } catch (IOException e) {
                        throw new ValidatorException("validator.label.exception.loadingRemoteSchemas", e);
                    }
                }
            }
        }
    }

}
