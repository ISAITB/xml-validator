package eu.europa.ec.itb.xml.util;

import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfig;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by simatosc on 12/08/2016.
 */
@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

	@Override
	public String getFileExtension(String contentType) {
		return null;
	}

	@Override
	protected boolean isAcceptedArtifactFile(File file, String artifactType) {
		if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
			return config.getAcceptedSchemaExtensions().contains(FilenameUtils.getExtension(file.getName().toLowerCase()));
		} else {
			return config.getAcceptedSchematronExtensions().contains(FilenameUtils.getExtension(file.getName().toLowerCase()));
		}
	}

	public String writeXML(String domain, String xml) throws IOException {
        UUID fileUUID = UUID.randomUUID();
        String xmlID = domain+"_"+fileUUID.toString();
        File outputFile = new File(getReportFolder(), getInputFileName(xmlID));
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

    public boolean checkFileType(File file) throws IOException {
        return config.getAcceptedMimeTypes().contains(checkContentType(file));
    }
    
    public String checkContentTypeUrl(String url) throws IOException, URISyntaxException {
        Tika tika = new Tika();
        URI uri = new URI(url);
        return tika.detect(uri.toURL());
    }
    
    public String checkContentType(String content) throws IOException {
        Tika tika = new Tika();
        String mimeType = "";
        try(InputStream in = new ByteArrayInputStream(content.getBytes())){
            mimeType = tika.detect(in);
		}
        return mimeType;
    }

	public String checkContentType(byte[] content) throws IOException {
		Tika tika = new Tika();
		String mimeType = "";
		try (InputStream in = new ByteArrayInputStream(content)){
			mimeType = tika.detect(in);
		}
		return mimeType;
	}

	public String checkContentType(File content) throws IOException {
		Tika tika = new Tika();
		String mimeType = "";
		try (InputStream in = Files.newInputStream(content.toPath())){
			mimeType = tika.detect(in);
		}
		return mimeType;
	}

	private boolean getZipFiles(ZipInputStream zis, File tmpFolder) throws IOException{
		byte[] buffer = new byte[1024];
        ZipEntry zipEntry = zis.getNextEntry();
        
        if(zipEntry == null) {
        	return false;
        }
		
        while (zipEntry != null) {
			Path tmpPath = createFile(tmpFolder, null, zipEntry.getName());

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

	public File unzipFile(File parentFolder, byte[] zipContent){
		File unzipFiles;
		boolean isZip;
		try {
			ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipContent));
			File rootFolder = createTemporaryFolderPath(parentFolder);
			rootFolder.mkdirs();

			isZip = getZipFiles(zis, rootFolder);

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

	public File unzipFile(File parentFolder, File zipFile){
		File unzipFiles;
		boolean isZip;
		try {
	        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			File rootFolder = createTemporaryFolderPath(parentFolder);
			rootFolder.mkdirs();
	        isZip = getZipFiles(zis, rootFolder);
	        unzipFiles = rootFolder;
	        zis.closeEntry();
	        zis.close();
		} catch (Exception e) {
			return null;
		}
		if (isZip) {
			return unzipFiles;
		} else {
			return null;
		}
	}

	@Override
	public File getFileFromURL(File targetFolder, String url, String extension, String fileName, File preprocessorFile, String preprocessorOutputExtension, String artifactType) throws IOException {
		File savedFile = super.getFileFromURL(targetFolder, url, extension, fileName, preprocessorFile, preprocessorOutputExtension, artifactType);
		if (DomainConfig.ARTIFACT_TYPE_SCHEMA.equals(artifactType)) {
			retrieveSchemasForImports(url, new File(savedFile.getParent(), "import"));
		}
		return savedFile;
	}

	private void retrieveSchemasForImports(String rootURI, File rootFolder) {
		XMLSchemaLoader xsdLoader = new XMLSchemaLoader();
		XSModel xsdModel = xsdLoader.loadURI(rootURI);
		XSNamespaceItemList xsdNamespaceItemList = xsdModel.getNamespaceItems();
		Set<String> documentLocations = new HashSet<>();
		for (int i=0; i<xsdNamespaceItemList.getLength(); i++) {
			XSNamespaceItem xsdItem = (XSNamespaceItem) xsdNamespaceItemList.get(i);
			StringList sl = xsdItem.getDocumentLocations();
			for(int k=0; k<sl.getLength(); k++) {
				if(!documentLocations.contains(sl.item(k))) {
					String currentLocation = (String)sl.get(k);
					try {
						getFileFromURL(rootFolder, currentLocation);
						documentLocations.add(currentLocation);
					} catch (IOException e) {
						throw new IllegalStateException("Error to loading remote schemas for imports", e);
					}
				}
			}
		}
	}

}
