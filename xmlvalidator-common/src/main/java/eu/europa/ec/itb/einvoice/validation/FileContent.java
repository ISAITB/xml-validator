package eu.europa.ec.itb.einvoice.validation;

import com.gitb.core.ValueEmbeddingEnumeration;

public class FileContent {

    private String content;
    private String embeddingMethod;
    private String syntax;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmbeddingMethod() {
        return embeddingMethod;
    }

    public void setEmbeddingMethod(String embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }
    
    public static boolean isValid(String type) {
    	return ValueEmbeddingEnumeration.BASE_64.equals(type) || ValueEmbeddingEnumeration.URI.equals(type)  || ValueEmbeddingEnumeration.STRING.equals(type);
    }

}
