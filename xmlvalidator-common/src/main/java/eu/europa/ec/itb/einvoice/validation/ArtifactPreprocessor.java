package eu.europa.ec.itb.einvoice.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ArtifactPreprocessor {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactPreprocessor.class);

    private ConcurrentHashMap<String, Templates> templateCache = new ConcurrentHashMap<>();

    /**
     * Process the provided file to generate a new file next to the provided one.
     *
     * @param fileToProcess The file to proprocess.
     * @param preProcessorFile The path to the pre-processing artifact (typically an XSLT file).
     * @param outputFileExtension The file extension to use for the output file.
     * @return The file resulting from the processing (created next to the fileToProcess.
     */
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
            templateCache.put(fullPath, template);
        }
        return template.newTransformer();
    }

}
