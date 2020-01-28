package eu.europa.ec.itb.einvoice.util;

import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import eu.europa.ec.itb.einvoice.DomainConfig;
import eu.europa.ec.itb.einvoice.DomainConfig.RemoteFileInfo;
import eu.europa.ec.itb.einvoice.DomainConfigCache;
import eu.europa.ec.itb.einvoice.validation.ArtifactPreprocessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by simatosc on 12/08/2016.
 */
@Component
public class FileManager {

    private static JAXBContext REPORT_CONTEXT;
    private static ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static Logger logger = LoggerFactory.getLogger(FileManager.class);

    static {
        try {
            REPORT_CONTEXT = JAXBContext.newInstance(TAR.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB context for TAR class", e);
        }
    }

    @Autowired
    ApplicationConfig config;
    
	@Autowired
	private DomainConfigCache domainConfigCache;

	@Autowired
	private ArtifactPreprocessor preprocessor;

	private ConcurrentHashMap<String, ReadWriteLock> externalDomainFileCacheLocks = new ConcurrentHashMap<>();

    public String writeXML(String domain, String xml) throws IOException {
        UUID fileUUID = UUID.randomUUID();
        String xmlID = domain+"_"+fileUUID.toString();
        File outputFile = new File(config.getReportFolder(), getInputFileName(xmlID));
        outputFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(outputFile, xml);
        return xmlID;
    }

    public String getInputFileName(String uuid) {
        return config.getInputFilePrefix()+uuid+".xml";
    }

    public String getReportFileNameXml(String uuid) {
        return config.getReportFilePrefix()+uuid+".xml";
    }

    public String getReportFileNamePdf(String uuid) {
        return config.getReportFilePrefix()+uuid+".pdf";
    }

    public void saveReport(TAR report, String xmlID) {
        File outputFile = new File(config.getReportFolder(), getReportFileNameXml(xmlID));
        saveReport(report, outputFile);
    }

    public void saveReport(TAR report, File outputFile) {
        try {
            Marshaller m = REPORT_CONTEXT.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            outputFile.getParentFile().mkdirs();

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            Document document = docBuilderFactory.newDocumentBuilder().newDocument();
            m.marshal(OBJECT_FACTORY.createTestStepReport(report), document);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "{http://www.gitb.com/core/v1/}value");
            try (OutputStream fos = new FileOutputStream(outputFile)) {
                transformer.transform(new DOMSource(document), new StreamResult(fos));
                fos.flush();
            } catch(IOException e) {
                logger.warn("Unable to save XML report", e);
            }

        } catch (Exception e) {
            logger.warn("Unable to marshal XML report", e);
        }
    }

    public boolean checkFileType(InputStream stream) throws IOException {
        Tika tika = new Tika();
        String type = tika.detect(stream);
        return config.getAcceptedMimeTypes().contains(type);
    }

    /**
     * Get InputStream file from a URI.
     * @param URLConvert String URI to be downloaded.
     * @return The InputStream of the URI.
     */
    public InputStream getURIInputStream(String URLConvert) {
        // Read the string from the provided URI.
        URI uri = URI.create(URLConvert);
        Proxy proxy = null;
        List<Proxy> proxies = ProxySelector.getDefault().select(uri);
        if (proxies != null && !proxies.isEmpty()) {
            proxy = proxies.get(0);
        }
        
        try {
	        URLConnection connection;
	        if (proxy == null) {
	            connection = uri.toURL().openConnection();
	        } else {
	            connection = uri.toURL().openConnection(proxy);
	        }
	        
	        return connection.getInputStream();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read provided URI", e);
        }
        
	}
    
    public File getInputStreamFile(InputStream stream, String filename) throws IOException {
    	return getInputStreamFile(config.getTmpFolder(), stream, filename);
    }
	
	public File getInputStreamFile(String targetFolder, InputStream stream, String fileName) throws IOException {
		Path tmpPath = getFilePathFilename(targetFolder, fileName);
		
		Files.copy(stream, tmpPath, StandardCopyOption.REPLACE_EXISTING);

		return tmpPath.toFile();
	}
    
    public File getURLFile(String url, boolean isSchema) throws IOException {
        UUID folderUUID = UUID.randomUUID();
		Path tmpFolder = Paths.get(config.getTmpFolder(), folderUUID.toString());
		
    	return getURLFile(tmpFolder.toString(), url, isSchema, null, null);
    }
    /**
     * Returns a File in a specific folder from a URI. If the expected file is an XSD, it retrieves the imported/included schemas.
     * @param targetFolder Folder to save the File.
     * @param URLConvert URI from where to retrieve the File.
     * @param isSchema Whether the expected File is an XSD or Schematron.
     * @return File URI as File in  the specific folder.
     * @throws IOException
     */
	private File getURLFile(String targetFolder, String URLConvert, boolean isSchema, File preprocessorFile, String preprocessorOutputExtension) throws IOException {
		URL url = new URL(URLConvert);
		String filename = FilenameUtils.getName(url.getFile());
		File rootFile = getURLFile(targetFolder, URLConvert, filename);
		if (preprocessorFile != null) {
			File processedFile = preprocessor.preprocessFile(rootFile, preprocessorFile, preprocessorOutputExtension);
			FileUtils.deleteQuietly(rootFile);
			rootFile = processedFile;
		}
		if (isSchema) {
			retreiveImportSchema(URLConvert, rootFile.getParent()+"/import");
		}
		return rootFile;
	}
	
	private File getURLFile(String targetFolder, String URLConvert, String filename) throws IOException {
		Path tmpPath;
		
		tmpPath = getFilePathFilename(targetFolder, filename);

		try(InputStream in = getURIInputStream(URLConvert)){
			Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
		}

		return tmpPath.toFile();
	}

	private Path getFilePathFilename(String folder, String fileName) {
		Path tmpPath = Paths.get(folder, fileName);
		tmpPath.toFile().getParentFile().mkdirs();

		return tmpPath;
	}
	
	private void retreiveImportSchema(String rootURI, String rootFile) {
		XMLSchemaLoader xsdLoader = new XMLSchemaLoader();
		XSModel xsdModel = xsdLoader.loadURI(rootURI);
		XSNamespaceItemList xsdNamespaceItemList = xsdModel.getNamespaceItems();
		Set<String> documentLocations = new HashSet<>();
				
		for(int i=0; i<xsdNamespaceItemList.getLength(); i++) {
			XSNamespaceItem xsdItem = (XSNamespaceItem) xsdNamespaceItemList.get(i);
			StringList sl = xsdItem.getDocumentLocations();
			for(int k=0; k<sl.getLength(); k++) {
				if(!documentLocations.contains(sl.get(k))) {
					String currentLocation = (String)sl.get(k);
					
					try {
						getURLFile(rootFile, currentLocation, false, null, null);
						
						documentLocations.add(currentLocation);
					} catch (IOException e) {
						logger.error("Error to load the remote files", e);
						throw new IllegalStateException("Error to load the remote files", e);
					}
				}
			}
		}
	}
	
	private boolean getZipFiles(ZipInputStream zis, String tmpFolder) throws IOException{
		byte[] buffer = new byte[1024];
        ZipEntry zipEntry = zis.getNextEntry();
        
        if(zipEntry == null) {
        	return false;
        }
		
        while (zipEntry != null) {
            Path tmpPath = getFilePathFilename(tmpFolder, zipEntry.getName());
            
            if(zipEntry.isDirectory()) {
            	tmpPath.toFile().mkdirs();
            }else {
	            File f = tmpPath.toFile();
	            FileOutputStream fos = new FileOutputStream(f);
	            int len;
	            
	            while ((len = zis.read(buffer)) > 0) {
	                fos.write(buffer, 0, len);
	            }
	            fos.close();
            }
            
            zipEntry = zis.getNextEntry();
        }
        
        return true;
	}
	
	public File unzipFile(File zipFile){
		File unzipFiles = null;		
		boolean isZip = false;
        UUID folderUUID = UUID.randomUUID();
		Path tmpFolder = Paths.get(config.getTmpFolder(), folderUUID.toString());
		
		try {
	        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));	        
	        File rootFolder = tmpFolder.toFile();
	        rootFolder.mkdirs();
	        
	        isZip = getZipFiles(zis, tmpFolder.toString());
	        
	        unzipFiles = rootFolder;
        	
	        zis.closeEntry();
	        zis.close();
		}catch(Exception e) {
			return null;
		}
        
		if(isZip) {
			return unzipFiles;
		}else {
			return null;
		}
	}

    public File getRemoteFileCacheFolder() {
    	return new File(getTempFolder(), "remote_config");
	}

    private File getTempFolder() {
    	return new File(config.getTmpFolder());
	}

	@PostConstruct
	public void init() {
		FileUtils.deleteQuietly(getTempFolder());
		for (DomainConfig config: domainConfigCache.getAllDomainConfigurations()) {
			externalDomainFileCacheLocks.put(config.getDomainName(), new ReentrantReadWriteLock());
		}
		FileUtils.deleteQuietly(getRemoteFileCacheFolder());
		resetRemoteFileCache();
	}
	
	@Scheduled(fixedDelayString = "${validator.cleanupRate}")
	public void resetRemoteFileCache() {
		logger.debug("Resetting remote SCHEMATRON and SCHEMA files cache");
		for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
			try {
				// Get write lock for domain.
				logger.debug("Waiting for lock to reset cache for ["+domainConfig.getDomainName()+"]");
				externalDomainFileCacheLocks.get(domainConfig.getDomainName()).writeLock().lock();
				logger.debug("Locked cache for ["+domainConfig.getDomainName()+"]");
				try {
					for (String validationType: domainConfig.getType()) {
						// Empty cache folder.
						File remoteConfigFolder = new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType);
						FileUtils.deleteQuietly(remoteConfigFolder);
						File remoteSchFolder = new File(remoteConfigFolder, "sch");
						File remoteXsdFolder = new File(remoteConfigFolder, "xsd");
						remoteSchFolder.mkdirs();
						remoteXsdFolder.mkdirs();
						// Download remote SCH/XSD files (if needed).
						downloadRemoteFiles(domainConfig.getDomain(), domainConfig.getRemoteSchematronFile(), validationType, remoteSchFolder.getAbsolutePath(), false);
						downloadRemoteFiles(domainConfig.getDomain(), domainConfig.getRemoteSchemaFile(), validationType, remoteXsdFolder.getAbsolutePath(), true);
					}
				} catch (Exception e) {
					// Never allow configuration errors in one domain to prevent the others from being available.
					logger.error("Error while processing configuration for domain ["+domainConfig.getDomainName()+"]", e);
				}
			} finally {
				// Unlock domain.
				externalDomainFileCacheLocks.get(domainConfig.getDomainName()).writeLock().unlock();
				logger.debug("Reset remote SCHEMATRON and SCHEMA files cache for ["+domainConfig.getDomainName()+"]");
			}
		}
	}
	
	private void downloadRemoteFiles(String domain, Map<String, RemoteFileInfo> map, String validationType, String remoteConfigPath, boolean isSchema) throws IOException{
		// Download remote SCH/XSD files (if needed).
		List<DomainConfig.ValidationArtifactInfo> remoteFiles = map.get(validationType).getRemote();
		if (remoteFiles != null) {
			for (DomainConfig.ValidationArtifactInfo artifactInfo: remoteFiles) {
				File preprocessorFile = null;
				if (artifactInfo.getPreProcessorPath() != null) {
					preprocessorFile = Paths.get(config.getResourceRoot(), domain, artifactInfo.getPreProcessorPath()).toFile();
				}
				getURLFile(remoteConfigPath, artifactInfo.getPath(), isSchema, preprocessorFile, artifactInfo.getPreProcessorOutputExtension());
			}
		}
	}
}
