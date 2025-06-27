/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.xml.util;

import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.util.XMLCatalogResolver;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages file-system operations.
 */
@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

    /** Flag to indicate that the given file is externally provided. */
    public static final String EXTERNAL_FILE = "external";

    /**
     * @see BaseFileManager#getFileExtension(String)
     *
     * @param contentType The content type (ignored).
     * @return Always "xml" or null for external files.
     */
    @Override
    public String getFileExtension(String contentType) {
        if (EXTERNAL_FILE.equals(contentType)) {
            return null;
        }
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
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    public String getReportFileNameXml(String uuid, boolean aggregate) {
        return config.getReportFilePrefix()+uuid+(aggregate?"_aggregate":"")+".xml";
    }

    /**
     * Returns the name of a PDF report file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    public String getReportFileNamePdf(String uuid, boolean aggregate) {
        return config.getReportFilePrefix()+uuid+(aggregate?"_aggregate":"")+".pdf";
    }

    /**
     * Returns the name of a CSV report file based on the provided identifier.
     *
     * @param uuid The UUID to consider.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    public String getReportFileNameCsv(String uuid, boolean aggregate) {
        return config.getReportFilePrefix()+uuid+(aggregate?"_aggregate":"")+".csv";
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
        String mimeType;
        try (InputStream in = Files.newInputStream(content.toPath())){
            var metadata = new Metadata();
            metadata.set(HttpHeaders.CONTENT_TYPE, "application/xml");
            mimeType = tika.detect(in, metadata);
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
        if (zipEntry == null) {
            return false;
        }
        while (zipEntry != null) {
            Path tmpPath = createFile(tmpFolder, null, zipEntry.getName());

            if(zipEntry.isDirectory()) {
                tmpPath.toFile().mkdirs();
            }else {
                File f = tmpPath.toFile();
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
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
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            File rootFolder = createTemporaryFolderPath(parentFolder);
            rootFolder.mkdirs();
            isZip = getZipFiles(zis, rootFolder);
            unzipFiles = rootFolder;
            zis.closeEntry();
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
     * @see BaseFileManager#getExternalValidationArtifacts(eu.europa.ec.itb.validation.commons.config.DomainConfig, String, String, File, List, HttpClient.Version)
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
     * @param acceptedContentTypes A (nullable) list of content types to accept for the request.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    @Override
    public FileInfo getFileFromURL(File targetFolder, String url, String extension, String fileName, File preprocessorFile, String preprocessorOutputExtension, String artifactType, List<String> acceptedContentTypes, HttpClient.Version httpVersion) throws IOException {
        FileInfo savedFile = super.getFileFromURL(targetFolder, url, extension, fileName, preprocessorFile, preprocessorOutputExtension, artifactType, acceptedContentTypes, httpVersion);
        if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
            retrieveSchemasForImports(url, new File(savedFile.getFile().getParent(), "import"), httpVersion);
        }
        return savedFile;
    }

    /**
     * Retrieve the schema for the provided URI and store it in the target folder.
     *
     * @param rootURI The URI.
     * @param rootFolder The folder.
     * @param httpVersion The HTTP version to use.
     */
    private void retrieveSchemasForImports(String rootURI, File rootFolder, HttpClient.Version httpVersion) {
        XMLSchemaLoader xsdLoader = new XMLSchemaLoader();
        Set<String> documentLocations = new HashSet<>();
        // Use a custom resolver as this will handle XSDs as well as DTDs.
        xsdLoader.setEntityResolver(new XMLCatalogResolver() {
            @Override
            public String resolveIdentifier(XMLResourceIdentifier resourceIdentifier) throws IOException, XNIException {
                String expandedLocation = resourceIdentifier.getExpandedSystemId();
                if (expandedLocation != null && !documentLocations.contains(expandedLocation)) {
                    try {
                        getFileFromURL(rootFolder, expandedLocation, httpVersion);
                        documentLocations.add(expandedLocation);
                    } catch (IOException e) {
                        throw new ValidatorException("validator.label.exception.loadingRemoteSchemas", e);
                    }
                }
                return super.resolveIdentifier(resourceIdentifier);
            }
        });
        try {
            // Iterate also over the namespaces XSD imports and includes to ensure we haven't missed anything from the custom resolver.
            XSModel xsdModel = xsdLoader.loadURI(rootURI);
            XSNamespaceItemList xsdNamespaceItemList = xsdModel.getNamespaceItems();
            for (int i=0; i<xsdNamespaceItemList.getLength(); i++) {
                XSNamespaceItem xsdItem = (XSNamespaceItem) xsdNamespaceItemList.get(i);
                StringList sl = xsdItem.getDocumentLocations();
                for (int k=0; k<sl.getLength(); k++) {
                    if (!documentLocations.contains(sl.item(k))) {
                        String currentLocation = (String)sl.get(k);
                        getFileFromURL(rootFolder, currentLocation, httpVersion);
                        documentLocations.add(currentLocation);
                    }
                }
            }
        } catch (ValidatorException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidatorException("validator.label.exception.loadingRemoteSchemas", e);
        }
    }

}
