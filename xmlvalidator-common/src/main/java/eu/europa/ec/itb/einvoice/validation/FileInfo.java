package eu.europa.ec.itb.einvoice.validation;

import java.io.File;

public class FileInfo {

    private File file;
    private String contentLang;

    public FileInfo(File file, String contentLang) {
        this.file = file;
        this.contentLang = contentLang;
    }

    public FileInfo() {
	}

	public File getFile() {
        return file;
    }
	public void setFile(File f) {
		file = f;
	}

    public String getContentLang() {
        return contentLang;
    }
    public void setContentLang(String lang) {
    	contentLang = lang;
    }
}
