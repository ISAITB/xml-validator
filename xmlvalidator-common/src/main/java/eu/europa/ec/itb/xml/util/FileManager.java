/*
 * Copyright (C) 2026 European Union
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
import eu.europa.ec.itb.validation.commons.StreamInfo;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages file-system operations.
 */
@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

    /** Flag to indicate that the given file is externally provided. */
    public static final String EXTERNAL_FILE = "external";
    private static final Logger LOG = LoggerFactory.getLogger(FileManager.class);

    private final ConcurrentHashMap<String, Path> remoteResourceCache = new ConcurrentHashMap<>();

    /**
     * Retrieve an external resource from the cache.
     *
     * @param domain The current domain configuration.
     * @param uri The resource to retrieve.
     * @return The local resource.
     */
    public Path retrieveCachedRemoteResource(DomainConfig domain, URI uri) {
        String key = "%s|%s".formatted(domain.getDomain(), Objects.requireNonNull(uri));
        return remoteResourceCache.compute(key, (k, existingPath) -> {
            if (existingPath == null || !Files.exists(existingPath)) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No cached resource found for [{}]. Downloading...", uri);
                    }
                    return writeResourceToCache(domain, uri);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to read remote resource from [%s]".formatted(uri), e);
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Returning [{}] from cache", uri);
                }
                return existingPath;
            }
        });
    }

    /**
     * Write the provided resource to the cache.
     *
     * @param domain The current domain.
     * @param uri The remote resource to write.
     * @return The local cached path.
     * @throws IOException If an IO error occurs.
     */
    private Path writeResourceToCache(DomainConfig domain, URI uri) throws IOException {
        StreamInfo streamInfo = null;
        try {
            streamInfo = getInputStreamFromURL(uri.toString(), null, domain.getHttpVersion());
            Path parentFolder = getTempFolder().toPath().resolve("remote_cache").resolve(domain.getDomain());
            Files.createDirectories(parentFolder);
            Path targetFile = parentFolder.resolve(UUID.randomUUID().toString());
            Files.copy(streamInfo.stream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return targetFile;
        } finally {
            if (streamInfo != null) IOUtils.close(streamInfo.stream());
        }
    }

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

}
