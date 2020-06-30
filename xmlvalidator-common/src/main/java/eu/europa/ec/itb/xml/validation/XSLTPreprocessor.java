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

@Component
public class XSLTPreprocessor implements ArtifactPreprocessor {

    private static final Logger LOG = LoggerFactory.getLogger(XSLTPreprocessor.class);

    @Autowired
    private ApplicationConfig config;

    private ConcurrentHashMap<String, Templates> templateCache = new ConcurrentHashMap<>();

    @Override
    public File preprocessFile(File fileToProcess, File preProcessorFile, String outputFileExtension) {
        Source inputSource = new StreamSource(fileToProcess);
        File outputFile = new File(fileToProcess.getParentFile(), UUID.randomUUID().toString()+"."+outputFileExtension);
        try {
            LOG.info("Pre-processing ["+fileToProcess.getName()+"] using ["+preProcessorFile.getName()+"] to produce ["+outputFile.getName()+"]");
            getTransformer(preProcessorFile).transform(inputSource, new StreamResult(outputFile));
        } catch (TransformerException e) {
            throw new IllegalArgumentException("Error while performing transformation", e);
        }
        return outputFile;
    }

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
