package eu.europa.ec.itb.xml.validation;

import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.xml.util.FileManager;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

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
    private final String validationType;
    private final String xsdExternalPath;

    @Autowired
    ApplicationConfig config;

    /**
     * Constructor.
     *
     * @param validationType The validation type.
     * @param domainConfig The domain configuration.
     * @param xsdExternalPath The path used to store externally loaded XSDs.
     */
    public XSDFileResolver(String validationType, DomainConfig domainConfig, String xsdExternalPath) {
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.xsdExternalPath = xsdExternalPath;
    }

    /**
     * @see LSResourceResolver#resolveResource(String, String, String, String, String)
     *
     * @param type The resource type.
     * @param namespaceURI The URI.
     * @param publicId The public ID.
     * @param systemId The system ID.
     * @param baseURI The base URI.
     * @return The resolved resource.
     */
    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        File baseURIFile;
        boolean systemIdSet = false;
        if (baseURI == null) {
            ValidationArtifactInfo schemaInfo = domainConfig.getSchemaInfo(validationType);
        	if(schemaInfo.getLocalPath() != null) {
        		baseURIFile = Paths.get(config.getResourceRoot(), domainConfig.getDomain(), schemaInfo.getLocalPath()).toFile().getParentFile();
        	} else {
        		if (!schemaInfo.getRemoteArtifacts().isEmpty()) {
        			baseURIFile = Paths.get(fileManager.getRemoteFileCacheFolder().getAbsolutePath(), domainConfig.getDomainName(), validationType, DomainConfig.ARTIFACT_TYPE_SCHEMA).toFile();
        			systemId = "/import/" + new File(baseURIFile, systemId).getName();
                    systemIdSet = true;
        		} else {
        			baseURIFile = Paths.get(xsdExternalPath).toFile();
        			File currentFile = new File(baseURIFile, systemId);
        			if(!currentFile.exists()) {
        				systemId = "/import/" + currentFile.getName();
                        systemIdSet = true;
        			}
            	}
        	}
        } else {
            try {
                URI uri = new URI(baseURI);
                baseURIFile = new File(uri);
                baseURIFile = baseURIFile.getParentFile();
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        if (!systemIdSet) {
            if (!domainConfig.getSchemaInfo(validationType).getRemoteArtifacts().isEmpty()) {
                systemId = new File(baseURIFile, systemId).getName();
            } else {
                File currentFile = new File(baseURIFile, systemId);
                if (!currentFile.exists()) {
                    systemId = currentFile.getName();
                }
            }
        }

        File referencedSchemaFile = new File(baseURIFile, systemId);
        baseURI = referencedSchemaFile.getParentFile().toURI().toString();
        systemId = referencedSchemaFile.getName();
        
        try {
            return new LSInputImpl(publicId, systemId, baseURI, new InputStreamReader(new FileInputStream(referencedSchemaFile)));
        } catch (FileNotFoundException e) {
            LOG.error("The referenced schema with system ID ["+systemId+"] could not be located at ["+referencedSchemaFile.getAbsolutePath()+"].", e);
            throw new IllegalStateException("The referenced schema with system ID ["+systemId+"] could not be located.", e);
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
        }
    }

}
