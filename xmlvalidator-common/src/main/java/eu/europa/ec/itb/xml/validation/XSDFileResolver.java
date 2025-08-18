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

package eu.europa.ec.itb.xml.validation;

import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * URI resolver for XSDs looking up resources from the local file system.
 */
@Component
@Scope("prototype")
public class XSDFileResolver implements LSResourceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(XSDFileResolver.class);

    @Autowired
    private final FileManager fileManager = null;

    private final DomainConfig domainConfig;
    private final URI schemaSource;

    @Autowired
    ApplicationConfig config;

    /**
     * Constructor.
     *
     * @param domainConfig The domain configuration.
     * @param schemaSource The source of the initial schema that triggered the validation.
     */
    public XSDFileResolver(DomainConfig domainConfig, URI schemaSource) {
        this.domainConfig = domainConfig;
        this.schemaSource = schemaSource;
    }

    /**
     * @see LSResourceResolver#resolveResource(String, String, String, String, String)
     *
     * @param type The resource type.
     * @param namespaceUri The URI.
     * @param publicId The public ID.
     * @param systemId The system ID.
     * @param baseUri The base URI.
     * @return The resolved resource.
     */
    @Override
    public LSInput resolveResource(String type, String namespaceUri, String publicId, String systemId, String baseUri) {
        URI baseUriToUse = parseUri(baseUri);
        if (baseUriToUse == null) {
            baseUriToUse = schemaSource;
        }
        URI systemIdAsUri;
        try {
            systemIdAsUri = URI.create(systemId);
        } catch (IllegalArgumentException e) {
            throw signalError(systemId, baseUri, e);
        }
        URI schemaResource = baseUriToUse.resolve(systemIdAsUri);
        LSInputImpl result;
        if (isAbsoluteRemoteUri(schemaResource)) {
            // Loaded remotely.
            result = resourceFromRemoteUri(schemaResource, systemId, baseUri, publicId, domainConfig);
        } else {
            // Loaded from file system.
            result = resourceFromLocalPath(schemaResource, systemId, baseUri, publicId);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolved resource [{}] for system ID [{}] with base URI [{}]", schemaResource, systemId, baseUri);
        }
        return result;
    }

    /**
     * Ensure that referenced files are loaded from legal locations (domain config folder or temp folder).
     *
     * @param schemaResource The schema resource to load.
     * @return The file path for the schema resource.
     */
    private Path readSchemaResourceFromFileSystem(URI schemaResource) {
        Path schemaPath = Path.of(schemaResource).toAbsolutePath().normalize();
        if (Utils.isUnderDomain(schemaPath, config, domainConfig)
                || schemaPath.startsWith(fileManager.getTempFolder().toPath().toAbsolutePath().normalize())) {
            return schemaPath;
        } else {
            throw new IllegalStateException("Resource [%s] is outside the expected folder hierarchy".formatted(schemaResource));
        }
    }

    /**
     * Resolve the schema from the local filesystem.
     *
     * @param schemaResource The schema to resolve.
     * @param requestedSystemId The system ID that was requested.
     * @param requestedBaseUri The base URI that was requested.
     * @param publicId The public ID that was requested.
     * @return The resource.
     */
    private LSInputImpl resourceFromLocalPath(URI schemaResource, String requestedSystemId, String requestedBaseUri, String publicId) {
        Path schemaPath = readSchemaResourceFromFileSystem(schemaResource);
        String baseUriToReport = schemaPath.getParent().toUri().toString();
        String systemIdToReport = schemaResource.toString();
        LSInputImpl result;
        try {
            result = new LSInputImpl(publicId, systemIdToReport, baseUriToReport, new InputStreamReader(Files.newInputStream(schemaPath)));
        } catch (IOException e) {
            LOG.error("The referenced schema with system ID [{}] could not be located at path [{}].", requestedSystemId, schemaPath.toAbsolutePath(), e);
            throw signalError(requestedSystemId, requestedBaseUri, e);
        }
        return result;
    }

    /**
     *
     * Resolve the schema from a remote location.
     *
     * @param schemaResource The schema to resolve.
     * @param requestedSystemId The system ID that was requested.
     * @param requestedBaseUri The base URI that was requested.
     * @param publicId The public ID that was requested.
     * @param domainConfig The domain configuration to consider.
     * @return The resource.
     */
    private LSInputImpl resourceFromRemoteUri(URI schemaResource, String requestedSystemId, String requestedBaseUri, String publicId, DomainConfig domainConfig) {
        String baseUriToReport;
        try {
            baseUriToReport = extractParentUri(schemaResource).toString();
        } catch (URISyntaxException e) {
            LOG.error("The referenced schema with system ID [{}] could not be located at URI [{}].", requestedSystemId, schemaResource, e);
            throw signalError(requestedSystemId, requestedBaseUri, e);
        }
        String systemIdToReport = schemaResource.toString();
        LSInputImpl result;
        try {
            result = new LSInputImpl(publicId, systemIdToReport, baseUriToReport, new InputStreamReader(readRemoteSchema(schemaResource, domainConfig)));
        } catch (IOException e) {
            LOG.error("The referenced schema with system ID [{}] could not be located at URI [{}].", requestedSystemId, schemaResource, e);
            throw signalError(requestedSystemId, requestedBaseUri, e);
        }
        return result;
    }

    /**
     * Read the provided resource as a stream.
     *
     * @param schemaResource The resource URI.
     * @param domainConfig The current domain configuration.
     * @return The resource's stream.
     * @throws IOException If an IO error occurs.
     */
    private InputStream readRemoteSchema(URI schemaResource, DomainConfig domainConfig) throws IOException {
        String uriAsString = schemaResource.toString();
        if (domainConfig.getRemoteSchemaImportMappings() != null && domainConfig.getRemoteSchemaImportMappings().containsKey(uriAsString)) {
            // Read from a local file mapping defined in the domain configuration.
            LOG.debug("Retrieving resource [{}] from local mapped file", schemaResource);
            return Files.newInputStream(domainConfig.getRemoteSchemaImportMappings().get(uriAsString));
        } else if (domainConfig.isSkipRemoteSchemaImportCaching()) {
            // Read from the remote URI directly.
            LOG.debug("Retrieving resource [{}] remotely due to disabled caching", schemaResource);
            return fileManager.getInputStreamFromURL(uriAsString, null, domainConfig.getHttpVersion()).stream();
        } else {
            // Go through our caching layer.
            LOG.debug("Retrieving resource [{}] from cache", schemaResource);
            return Files.newInputStream(fileManager.retrieveCachedRemoteResource(domainConfig, schemaResource));
        }
    }

    /**
     * Raise a resource resolution error.
     *
     * @param systemId The requested system ID.
     * @param baseUri The requested base URI.
     * @param cause The cause of the error.
     * @return The error to throw.
     */
    private IllegalStateException signalError(String systemId, String baseUri, Throwable cause) {
        String message = "The referenced schema with system ID [%s] and base URI [%s] could not be retrieved".formatted(systemId, Objects.requireNonNullElse(baseUri, this.schemaSource));
        if (cause != null) {
            return new IllegalStateException(message, cause);
        } else {
            return new IllegalStateException(message);
        }
    }

    /**
     * Extract the parent part of the provided URI.
     *
     * @param uri The URI.
     * @return The parent URI.
     * @throws URISyntaxException If the URI is invalid.
     */
    private URI extractParentUri(URI uri) throws URISyntaxException {
        String path = uri.getPath();
        int lastSlash = path.lastIndexOf('/');
        String parentPath = (lastSlash >= 0) ? path.substring(0, lastSlash + 1) : "/";
        return new URI(uri.getScheme(), uri.getAuthority(), parentPath, null, null);
    }

    /**
     * Check to see if the provided URI is an absolute remote resource reference.
     *
     * @param uri The URI to check.
     * @return The check result.
     */
    private boolean isAbsoluteRemoteUri(URI uri) {
        if (uri != null) {
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme)
                    || "ftp".equalsIgnoreCase(scheme)
                    || "ftps".equalsIgnoreCase(scheme));
        } else {
            return false;
        }
    }

    /**
     * Parse an optional URI.
     *
     * @param string The string to process.
     * @return The parsed URI (or null).
     */
    private URI parseUri(String string) {
        if (string != null) {
            try {
                return new URI(string);
            } catch (URISyntaxException ignored) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Resolved resource wrapper.
     *
     * @see LSInput
     */
    public static class LSInputImpl implements LSInput {

        private String baseURI;
        private String publicId;
        private String systemId;
        private final Reader characterStream;

        /**
         * Constructor.
         *
         * @param publicId The resource's public ID.
         * @param systemId The resource's system ID.
         * @param baseURI The resource's base URI.
         * @param characterStream The stream to read the resource from.
         */
        public LSInputImpl(String publicId, String systemId, String baseURI, Reader characterStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.baseURI  = baseURI;
            this.characterStream = characterStream;
        }

        /**
         * @see LSInput#getCharacterStream()
         */
        @Override
        public Reader getCharacterStream() {
            return characterStream;
        }

        /**
         * @see LSInput#setCharacterStream(Reader)
         *
         * Does nothing.
         */
        @Override
        public void setCharacterStream(Reader characterStream) {
            // No action.
        }

        /**
         * @see LSInput#getByteStream()
         *
         * @return Always null.
         */
        @Override
        public InputStream getByteStream() {
            return null;
        }

        /**
         * @see LSInput#setByteStream(InputStream)
         *
         * Does nothing.
         */
        @Override
        public void setByteStream(InputStream byteStream) {
            // Do nothing.
        }

        /**
         * @see LSInput#getStringData()
         *
         * @return Always null.
         */
        @Override
        public String getStringData() {
            return null;
        }

        /**
         * @see LSInput#setStringData(String)
         *
         * Does notning.
         */
        @Override
        public void setStringData(String stringData) {
            // No action.
        }

        /**
         * @see LSInput#getSystemId()
         *
         * @return The system ID.
         */
        @Override
        public String getSystemId() {
            return systemId;
        }

        /**
         * @see LSInput#setSystemId(String) (String)
         *
         * @param systemId The system ID.
         */
        @Override
        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        /**
         * @see LSInput#getPublicId()
         *
         * @return The public ID.
         */
        @Override
        public String getPublicId() {
            return publicId;
        }

        /**
         * @see LSInput#setPublicId(String) (String)
         *
         * @param publicId The public ID.
         */
        @Override
        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        /**
         * @see LSInput#getBaseURI()
         *
         * @return The base URI.
         */
        @Override
        public String getBaseURI() {
            return baseURI;
        }

        /**
         * @see LSInput#setBaseURI(String)
         *
         * @param baseURI The base URI.
         */
        @Override
        public void setBaseURI(String baseURI) {
            this.baseURI = baseURI;
        }

        /**
         * @see LSInput#getEncoding()
         *
         * @return Always null.
         */
        @Override
        public String getEncoding() {
            return null;
        }

        /**
         * @see LSInput#setEncoding(String)
         *
         * Does notning.
         */
        @Override
        public void setEncoding(String encoding) {
            // No action.
        }

        /**
         * @see LSInput#getCertifiedText()
         *
         * @return False.
         */
        @Override
        public boolean getCertifiedText() {
            return false;
        }

        /**
         * @see LSInput#setCertifiedText(boolean)
         *
         * Does notning.
         */
        @Override
        public void setCertifiedText(boolean certifiedText) {
            // Do nothing.
        }
    }

}
