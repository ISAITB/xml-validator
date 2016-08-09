package eu.europa.ec.itb.einvoice.validation;

import eu.europa.ec.itb.einvoice.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by simatosc on 07/03/2016.
 */
@Component
@Scope("prototype")
public class XSDResolver implements LSResourceResolver {

    private String validationType;

    public XSDResolver(String validationType) {
        this.validationType = validationType;
    }

/*
    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }
*/

    @Autowired
    ApplicationConfig config;

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        File baseURIFile;
        if (baseURI == null) {
            baseURIFile = config.getSchemaFile().get(validationType).getParentFile();
        } else {
            try {
                URI uri = new URI(baseURI);
                baseURIFile = new File(uri);
                baseURIFile = baseURIFile.getParentFile();
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        File referencedSchemaFile = new File(baseURIFile, systemId);
        baseURI = referencedSchemaFile.getParentFile().toURI().toString();
        systemId = referencedSchemaFile.getName();
        try {
            return new LSInputImpl(publicId, systemId, baseURI, new InputStreamReader(new FileInputStream(referencedSchemaFile)));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("The referenced schema with system ID ["+systemId+"] could not be located at ["+referencedSchemaFile.getAbsolutePath()+"].", e);
        }
    }

    public static class LSInputImpl implements LSInput {

        private String baseURI;
        private String publicId;
        private String systemId;
        private Reader characterStream;

        public LSInputImpl(String publicId, String systemId, String baseURI, Reader characterStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.baseURI  = baseURI;
            this.characterStream = characterStream;
        }

        @Override
        public Reader getCharacterStream() {
            return characterStream;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
        }

        @Override
        public InputStream getByteStream() {
            return null;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String stringData) {
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        @Override
        public String getPublicId() {
            return publicId;
        }

        @Override
        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        @Override
        public String getBaseURI() {
            return baseURI;
        }

        @Override
        public void setBaseURI(String baseURI) {
            this.baseURI = baseURI;
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
        }
    }

}
