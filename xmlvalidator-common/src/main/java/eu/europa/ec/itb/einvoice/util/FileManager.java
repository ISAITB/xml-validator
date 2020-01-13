package eu.europa.ec.itb.einvoice.util;

import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.ApplicationConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

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
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    
    public File getURLFile(String url) throws IOException {
    	return getURLFile(config.getTmpFolder(), url);
    }
	public File getURLFile(String targetFolder, String URLConvert) throws IOException {
		URL url = new URL(URLConvert);
		
		String extension = FilenameUtils.getExtension(url.getFile());
		
		
		return getURLFile(targetFolder, URLConvert, extension);
	}
	public File getURLFile(String targetFolder, String URLConvert, String contentSyntax) throws IOException {
		Path tmpPath;

		if(contentSyntax!=null) {
			contentSyntax = "." + contentSyntax;
		}
		
		tmpPath = getFilePath(targetFolder, contentSyntax);

		try(InputStream in = getURIInputStream(URLConvert)){
			Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
		}

		return tmpPath.toFile();
	}

	private Path getFilePath(String folder, String extension) {
		Path tmpPath = Paths.get(folder, UUID.randomUUID().toString() + extension);
		tmpPath.toFile().getParentFile().mkdirs();

		return tmpPath;
	}

	private Path getFilePathFilename(String folder, String fileName) {
		Path tmpPath = Paths.get(folder, fileName);
		tmpPath.toFile().getParentFile().mkdirs();

		return tmpPath;
	}
	
	public File getInputStreamFile(String targetFolder, InputStream stream, String fileName) throws IOException {
		Path tmpPath = getFilePathFilename(targetFolder, fileName);
		
		Files.copy(stream, tmpPath, StandardCopyOption.REPLACE_EXISTING);

		return tmpPath.toFile();
	}
	
	public List<File> unzipFile(File zipFile){
		List<File> unzipFiles = new ArrayList<>();		
		byte[] buffer = new byte[1024];
		
		try {
	        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
	        ZipEntry zipEntry = zis.getNextEntry();
	        while (zipEntry != null) {
	            Path tmpPath = getFilePathFilename(config.getTmpFolder(), zipEntry.getName());
	            File f = tmpPath.toFile();
	            FileOutputStream fos = new FileOutputStream(f);
	            int len;
	            while ((len = zis.read(buffer)) > 0) {
	                fos.write(buffer, 0, len);
	            }
	            fos.close();
	            
	            unzipFiles.add(f);
	            zipEntry = zis.getNextEntry();
	        }
	        zis.closeEntry();
	        zis.close();
		}catch(Exception e) {
			return new ArrayList<>();
		}
        
        return unzipFiles;
	}
}
