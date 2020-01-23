package eu.europa.ec.itb.einvoice.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.DomainConfig;
import eu.europa.ec.itb.einvoice.util.FileManager;
import eu.europa.ec.itb.einvoice.util.Utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by simatosc on 26/02/2016.
 */
@Component
@Scope("prototype")
public class XMLValidator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(XMLValidator.class);
    private static JAXBContext SVRL_JAXB_CONTEXT;

    @Autowired
    private ApplicationConfig config;

    @Autowired
    private FileManager fileManager;
    
    private InputStream inputToValidate;
    private byte[] inputBytes;
    private ApplicationContext ctx;
    private final DomainConfig domainConfig;
    private String validationType;
    private ObjectFactory gitbTRObjectFactory = new ObjectFactory();
    List<FileInfo> externalSchema;
    List<FileInfo> externalSch;

    static {
        try {
            SVRL_JAXB_CONTEXT = JAXBContext.newInstance(SchematronOutputType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB content for SchematronOutputType", e);
        }
    }

    public XMLValidator(InputStream inputToValidate, String validationType, List<FileInfo> externalSchema, List<FileInfo> externalSch, DomainConfig domainConfig) {
        this.inputToValidate = inputToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.externalSchema = externalSchema;
        this.externalSch = externalSch;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }

    private InputStream getInputStreamForValidation() {
        if (inputBytes == null) {
            try {
                inputBytes = StreamUtils.copyToByteArray(inputToValidate);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return new ByteArrayInputStream(inputBytes);
    }

    private LSResourceResolver getXSDResolver(String xsdExternalPath) {
        return ctx.getBean(XSDFileResolver.class, validationType, domainConfig, xsdExternalPath);
    }

    private javax.xml.transform.URIResolver getURIResolver(File schematronFile) {
        return ctx.getBean(URIResolver.class, validationType, schematronFile, domainConfig);
    }

    public TAR validateAgainstSchema() {
        File schemaFile = getSchemaFile();
        List<TAR> reports = new ArrayList<>();
        List<File> schemaFiles = new ArrayList<>();
        if (schemaFile != null && schemaFile.exists()) {
            if (schemaFile.isFile()) {
                // We are pointing to a single master schema file.
                schemaFiles.add(schemaFile);
            } else {
                // All schemas are to be processed.
                for (File aSchemaFile: schemaFile.listFiles()) {
                    if (aSchemaFile.isFile()) {
                        schemaFiles.add(aSchemaFile);
                    }
                }
            }
        }
        if (schemaFiles.isEmpty()) {
            logger.info("No schemas to validate against [" + schemaFile + "]");
            return null;
        } else {
            for (File aSchemaFile: schemaFiles) {
                logger.info("Validating against ["+aSchemaFile.getName()+"]");
                TAR report = validateSchema(getInputStreamForValidation(), aSchemaFile);
                logReport(report, aSchemaFile.getName());
                reports.add(report);
                logger.info("Validated against ["+aSchemaFile.getName()+"]");
            }
            TAR report = mergeReports(reports.toArray(new TAR[reports.size()]));
            completeReport(report);
            return report;
        }
    }

    public TAR validateSchema(InputStream inputSource, File schemaFile) {
        // Create error handler.
        XSDReportHandler handler = new XSDReportHandler();
        // Resolve schema.
        SchemaFactory schemaFactory = XMLSchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(handler);
        schemaFactory.setResourceResolver(getXSDResolver(schemaFile.getParent()));
        Schema schema;
        try {
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            schema = schemaFactory.newSchema(new StreamSource(new FileInputStream(schemaFile)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        // Validate XML content against given XSD schema.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setSchema(schema);
        Validator validator = schema.newValidator();
        validator.setErrorHandler(handler);
        TAR report = null;
        try {
            // Use a StreamSource rather than a DomSource below to get the line & column number of possible errors.
            StreamSource source = new StreamSource(inputSource);
            validator.validate(source);
            report = handler.createReport();
        } catch (Exception e) {
            logger.warn("Error while validating XML ["+e.getMessage()+"]");
            report = createFailureReport();
        }
        completeReport(report);
        return report;
    }

    private TAR createEmptyReport() {
        TAR report = new TAR();
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.SUCCESS);
        return report;
    }

    private TAR createFailureReport() {
        TAR report = new TAR();
        report.setReports(new TestAssertionGroupReportsType());
        report.setResult(TestResultType.FAILURE);
        BAR error1 = new BAR();
        error1.setDescription("An error occurred due to a problem in given XML content.");
        error1.setLocation("XML:1:0");
        JAXBElement element1 = this.gitbTRObjectFactory.createTestAssertionGroupReportsTypeError(error1);
        report.getReports().getInfoOrWarningOrError().add(element1);
        return report;
    }

    private void completeReport(TAR report) {
        if (report != null) {
            if (report.getDate() == null) {
                try {
                    report.setDate(Utils.getXMLGregorianCalendarDateTime());
                } catch (DatatypeConfigurationException e) {
                    logger.error("Exception while creating XMLGregorianCalendar", e);
                }
            }
            if (report.getContext() == null) {
                report.setContext(new AnyContent());
                String inputXML = null;
                try {
                    inputXML = StreamUtils.copyToString(getInputStreamForValidation(), Charset.forName("UTF-8"));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                AnyContent input = new AnyContent();
                input.setValue(inputXML);
                input.setName(ValidationConstants.INPUT_XML);
                input.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
                report.getContext().getItem().add(input);
            }
            if (report.getCounters() == null) {
                report.setCounters(new ValidationCounters());
                int infos = 0;
                int warnings = 0;
                int errors = 0;
                for (JAXBElement<TestAssertionReportType> item: report.getReports().getInfoOrWarningOrError()) {
                    String itemName = item.getName().getLocalPart();
                    if (itemName == "info") {
                        infos += 1;
                    } else if (itemName == "warning") {
                        warnings += 1;
                    } else if (itemName == "error") {
                        errors += 1;
                    }
                }
                report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
                report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
                report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
            }
        }
    }
    
    private List<File> getAllSchematron(File schematronFile) {
    	List<File> schematronFiles = new ArrayList<>();
    	
    	if (schematronFile != null && schematronFile.exists()) {
            if (schematronFile.isFile()) {
                // We are pointing to a single master schematron file.
                schematronFiles.add(schematronFile);
            } else {
                // All schematrons are to be processed.
                File[] files = schematronFile.listFiles();
                if (files != null) {
                    for (File aSchematronFile: files) {
                        if (aSchematronFile.isFile() && config.getAcceptedSchematronExtensions().contains(FilenameUtils.getExtension(aSchematronFile.getName().toLowerCase()))) {
                            schematronFiles.add(aSchematronFile);
                        }
                    }
                }
            }
        }
    	
    	return schematronFiles;
    }

    public TAR validateAgainstSchematron() {
        File schematronFile = getSchematronFile();
        List<TAR> reports = new ArrayList<>();
        List<File> externalFiles = getExternalSchematronFiles();
        
        List<File> schematronFiles = getAllSchematron(schematronFile);
        List<File> remoteFiles = getAllSchematron(getRemoteSchematronFiles());
        
        schematronFiles.addAll(remoteFiles);
        
        for(File aSchematronFile: externalFiles) {
        	schematronFiles.addAll(getAllSchematron(aSchematronFile));
        }
        
        if (schematronFiles.isEmpty()) {
            logger.info("No schematrons to validate against ["+schematronFile+"]");
            return null;
        } else {
            for (File aSchematronFile: schematronFiles) {
                logger.info("Validating against ["+aSchematronFile.getName()+"]");
                TAR report = validateSchematron(getInputStreamForValidation(), aSchematronFile);
                logReport(report, aSchematronFile.getName());
                reports.add(report);
                logger.info("Validated against ["+aSchematronFile.getName()+"]");
            }
            TAR report = mergeReports(reports.toArray(new TAR[reports.size()]));
            completeReport(report);
            return report;
        }
    }

    private File getSchematronFile() {
        File file = null;
        if (domainConfig.getSchematronFile() != null && domainConfig.getSchematronFile().containsKey(validationType)) {
            file = Paths.get(config.getResourceRoot(), domainConfig.getDomain(), domainConfig.getSchematronFile().get(validationType)).toFile();
        }
        return file;
    }

    private List<File> getExternalSchematronFiles() {
        List<File> files = new ArrayList<>();

    	if(!domainConfig.getExternalSchematronFile().get(validationType).equals(DomainConfig.externalFile_none) && externalSch != null && !externalSch.isEmpty()) {
    		for(FileInfo fi: externalSch) {
    			files.add(fi.getFile());
    		}
    	}
        return files;
    }

	private File getRemoteSchematronFiles() {
		File remoteConfigFolder = new File(new File(new File(fileManager.getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType), "sch");
		
		
		if (remoteConfigFolder.exists()) {
			return remoteConfigFolder;
		} else {
			return null;
		}
	}

	private File getRemoteSchemaFiles() {
		File remoteConfigFolder = new File(new File(new File(fileManager.getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType), "xsd");
		
		if (remoteConfigFolder.exists() && remoteConfigFolder.listFiles().length>0) {
			return remoteConfigFolder;
		} else {
			return null;
		}
	}

    private File getSchemaFile() {
        File file = null;
        if (domainConfig.getSchemaFile() != null && domainConfig.getSchemaFile().containsKey(validationType)) {
            file = Paths.get(config.getResourceRoot(), domainConfig.getDomain(), domainConfig.getSchemaFile().get(validationType)).toFile();
        }else {
        	//Remote Schema file
        	File remotFile = getRemoteSchemaFiles();
    		if(remotFile!= null) {
    			file = getRootFile(remotFile.listFiles());
    		}else {
        		//External Schema file
        		if(!domainConfig.getExternalSchemaFile().get(validationType).equals(DomainConfig.externalFile_none) && externalSchema != null && !externalSchema.isEmpty()) {
            		File rootFolder = externalSchema.get(0).getFile();
            		
            		if(rootFolder.isFile()) {
            			file = rootFolder;
            		}else {
            			file = getRootFile(rootFolder.listFiles());
            		}
            	}
        	}
        }
        return file;
    }
    
    private File getRootFile(File[] listFiles) {
    	File rootFile = null;
		for(File f: listFiles) {
			if(f.isFile()) {
				rootFile = f;
			}
		}
		
		return rootFile;
    }

    private void logReport(TAR report, String name) {
        if (logger.isDebugEnabled()) {
            StringBuilder logOutput = new StringBuilder();
            logOutput.append("["+name+"]\n Result: ").append(report.getResult());
            if (report.getCounters() != null) {
                logOutput.append("\nOverview: total: ").append(report.getCounters().getNrOfAssertions())
                        .append(" errors: ").append(report.getCounters().getNrOfErrors())
                        .append(" warnings: ").append(report.getCounters().getNrOfWarnings());
            }
            logOutput.append("\nDetails");
            report.getReports().getInfoOrWarningOrError().forEach((item) -> {
                if (item.getValue() instanceof BAR) {
                    BAR reportItem = (BAR)item.getValue();
                    logOutput.append("\nDescription: ").append(reportItem.getDescription());
                }
            });
            logger.debug(logOutput.toString());
        }
    }


    public TAR validateAll() {
        TAR overallResult;
        TAR schemaResult = validateAgainstSchema();
        if (schemaResult == null) {
            // No schema.
            schemaResult = createEmptyReport();
        }
        if (schemaResult.getResult() != TestResultType.SUCCESS) {
            overallResult = schemaResult;
        } else {
            TAR schematronResult = validateAgainstSchematron();
            if (schematronResult != null) {
                overallResult = mergeReports(new TAR[] {schemaResult, schematronResult});
            } else {
                overallResult = mergeReports(new TAR[] {schemaResult});
            }
        }
        completeReport(overallResult);
        return overallResult;
    }

    private TAR mergeReports(TAR[] reports) {
        TAR mergedReport = reports[0];
        if (reports.length > 1) {
            for (int i=1; i < reports.length; i++) {
                TAR report = reports[i];
                if (report != null) {
                    if (report.getCounters() != null) {
                        if (mergedReport.getCounters() == null) {
                            mergedReport.setCounters(new ValidationCounters());
                            mergedReport.getCounters().setNrOfAssertions(BigInteger.ZERO);
                            mergedReport.getCounters().setNrOfWarnings(BigInteger.ZERO);
                            mergedReport.getCounters().setNrOfErrors(BigInteger.ZERO);
                        }
                        if (report.getCounters().getNrOfAssertions() != null) {
                            mergedReport.getCounters().setNrOfAssertions(mergedReport.getCounters().getNrOfAssertions().add(report.getCounters().getNrOfAssertions()));
                        }
                        if (report.getCounters().getNrOfWarnings() != null) {
                            mergedReport.getCounters().setNrOfWarnings(mergedReport.getCounters().getNrOfWarnings().add(report.getCounters().getNrOfWarnings()));
                        }
                        if (report.getCounters().getNrOfErrors() != null) {
                            mergedReport.getCounters().setNrOfErrors(mergedReport.getCounters().getNrOfErrors().add(report.getCounters().getNrOfErrors()));
                        }
                    }
                    if (report.getReports() != null) {
                        if (mergedReport.getReports() == null) {
                            mergedReport.setReports(new TestAssertionGroupReportsType());
                        }
                        mergedReport.getReports().getInfoOrWarningOrError().addAll(report.getReports().getInfoOrWarningOrError());
                    }
                    if (mergedReport.getResult() == null) {
                        mergedReport.setResult(TestResultType.UNDEFINED);
                    }
                    if (report.getResult() != null) {
                        if (mergedReport.getResult() == TestResultType.UNDEFINED || mergedReport.getResult() == TestResultType.SUCCESS) {
                            if (report.getResult() != TestResultType.UNDEFINED) {
                                mergedReport.setResult(report.getResult());
                            }
                        }
                    }
                    if (report.getContext() != null && mergedReport.getContext() == null) {
                        mergedReport.setContext(report.getContext());
                    }
                }
            }
        }
        return mergedReport;
    }

    public TAR validateSchematron(InputStream inputSource, File schematronFile) {
        Document schematronInput = null;
        SchematronOutputType svrlOutput = null;
        boolean convertXPathExpressions = false;
        String schematronFileName = schematronFile.getName().toLowerCase();
        if (schematronFileName.endsWith("xslt") || schematronFileName.endsWith("xsl")) {
            // Validate as XSLT.
            try {
                schematronInput = Utils.readXMLWithLineNumbers(inputSource);
                TransformerFactory factory = TransformerFactory.newInstance();
                factory.setURIResolver(getURIResolver(schematronFile));
                Transformer transformer = factory.newTransformer(new StreamSource(new FileInputStream(schematronFile)));
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                transformer.setURIResolver(factory.getURIResolver());
                transformer.transform(new DOMSource(schematronInput), new StreamResult(bos));
                bos.flush();
                Unmarshaller jaxbUnmarshaller = SVRL_JAXB_CONTEXT.createUnmarshaller();
                JAXBElement<SchematronOutputType> root = jaxbUnmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(bos.toByteArray())), SchematronOutputType.class);
                svrlOutput = root.getValue();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            // Validate as raw schematron.
            convertXPathExpressions = true;
            ISchematronResource schematron = SchematronResourcePure.fromFile(schematronFile);
            if(schematron.isValidSchematron()) {
                try {
                    schematronInput = Utils.readXMLWithLineNumbers(inputSource);
                    svrlOutput = schematron.applySchematronValidationToSVRL(new DOMSource(schematronInput));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                throw new IllegalStateException("Schematron file ["+schematronFile.getAbsolutePath()+"] is invalid");
            }
        }
        //handle invoice report
        SchematronReportHandler handler = new SchematronReportHandler(schematronInput, Utils.emptyDocument(), schematronInput, svrlOutput, convertXPathExpressions, domainConfig.isIncludeTestDefinition(), domainConfig.isReportsOrdered());
        return handler.createReport();
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }
}
