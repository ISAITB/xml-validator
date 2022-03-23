package eu.europa.ec.itb.xml.validation;

import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * URI resolver to lookup references resources from the local file system during Schematron processing.
 */
@Component
@Scope("prototype")
public class URIResolver implements javax.xml.transform.URIResolver {

    private static final Logger LOG = LoggerFactory.getLogger(URIResolver.class);
    private final DomainConfig domainConfig;
    private final String validationType;
    private final File schematronFile;

    @Autowired
    ApplicationConfig config;

    /**
     * Constructor.
     *
     * @param validationType The current validation type.
     * @param schematronFile The root Schematron file.
     * @param domainConfig The domain configuration.
     */
    public URIResolver(String validationType, File schematronFile, DomainConfig domainConfig) {
        this.validationType = validationType;
        this.schematronFile = schematronFile;
        this.domainConfig = domainConfig;
    }

    /**
     * Get the root Schematron file.
     *
     * @return The root file.
     */
    private File getBaseFile() {
        File baseFile = Paths.get(config.getResourceRoot(), domainConfig.getDomain(), domainConfig.getSchematronInfo(validationType).getLocalPath()).toFile();
        if (baseFile.exists()) {
            if (baseFile.isDirectory()) {
                return baseFile;
            } else { // File
                return baseFile.getParentFile();
            }
        } else {
            LOG.error("The root Schematron file could not be loaded [{}]", baseFile.getAbsolutePath());
            throw new IllegalStateException("The root Schematron file could not be loaded");
        }
    }

    /**
     * @see URIResolver#resolve(String, String)
     *
     * @param href The href value.
     * @param base The base.
     * @return The loaded resource.
     * @throws TransformerException If an error occurs.
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        if (StringUtils.isBlank(base) && StringUtils.isBlank(href)) {
            try {
                return new StreamSource(new FileInputStream(schematronFile));
            } catch (FileNotFoundException e) {
                var message = String.format("Base schematron file not found base[%s] href[%s] file [%s]", base, href, schematronFile);
                LOG.error(message);
                throw new IllegalStateException(message);
            }
        } else {
            File baseFile;
            if (StringUtils.isBlank(base)) {
                baseFile = getBaseFile();
            } else {
                try {
                    URI uri = new URI(base);
                    baseFile = new File(uri);
                    baseFile = baseFile.getParentFile();
                } catch (URISyntaxException e) {
                    throw new IllegalStateException(e);
                }
            }
            // Lookup file directly under the base.
            File referencedFile = new File(baseFile, href);
            if (!referencedFile.exists()) {
                // Try next to the current XSLT.
                referencedFile = new File(schematronFile.getParent(), href);
            }
            if (referencedFile.exists()) {
                try {
                    return new StreamSource(new FileInputStream(referencedFile));
                } catch (FileNotFoundException e) {
                    LOG.error("Referenced file not found base[{}] href[{}] file [{}]", base, href, referencedFile);
                    throw new IllegalStateException(String.format("Referenced file not found base[%s] href[%s]", base, href));
                }
            } else {
                LOG.error("Referenced file not found base[{}] href[{}] file [{}]", base, href, referencedFile);
                throw new IllegalStateException(String.format("Referenced file not found base[%s] href[%s]", base, href));
            }
        }
    }
}
