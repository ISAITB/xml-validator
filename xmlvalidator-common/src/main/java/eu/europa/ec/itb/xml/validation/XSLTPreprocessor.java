package eu.europa.ec.itb.xml.validation;

import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.ArtifactPreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component used to preprocess validation artifacts using XSLT transformations.
 */
@Component
public class XSLTPreprocessor implements ArtifactPreprocessor {

    private static final Logger LOG = LoggerFactory.getLogger(XSLTPreprocessor.class);

    @Autowired
    private ApplicationConfig config;

    private ConcurrentHashMap<String, Templates> templateCache = new ConcurrentHashMap<>();

    /**
     * @see ArtifactPreprocessor#preprocessFile(File, File, String)
     *
     * @param fileToProcess The file to process.
     * @param preProcessorFile The XSLT file to use.
     * @param outputFileExtension The file extension to set.
     * @return The resulting processed file.
     */
    @Override
    public File preprocessFile(File fileToProcess, File preProcessorFile, String outputFileExtension) {
        Source inputSource = new StreamSource(fileToProcess);
        File outputFile = new File(fileToProcess.getParentFile(), UUID.randomUUID() +"."+outputFileExtension);
        try {
            LOG.info("Pre-processing [{}] using [{}] to produce [{}]", fileToProcess.getName(), preProcessorFile.getName(), outputFile.getName());
            getTransformer(preProcessorFile).transform(inputSource, new StreamResult(outputFile));
        } catch (TransformerException e) {
            throw new IllegalArgumentException("Error while performing transformation", e);
        }
        return outputFile;
    }

    /**
     * Create an XSLT transformer for the provided XSLT file.
     *
     * @param xsltFile The XSLT file to consider.
     * @return The transformer to use.
     * @throws TransformerConfigurationException If an error occurs.
     */
    private Transformer getTransformer(File xsltFile) throws TransformerConfigurationException {
        String fullPath = xsltFile.getAbsolutePath();
        Templates template = templateCache.get(fullPath);
        if (template == null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            template = factory.newTemplates(new StreamSource(xsltFile));
            if (!config.isDisablePreprocessingCache()) {
                templateCache.put(fullPath, template);
            }
        }
        return template.newTransformer();
    }

}
