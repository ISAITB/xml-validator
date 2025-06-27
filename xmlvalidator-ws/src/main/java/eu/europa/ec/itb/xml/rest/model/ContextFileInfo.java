/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.xml.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.ec.itb.validation.commons.FileContent;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A user-provided context file to use in the validation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A user-provided context file to use in the validation.")
public class ContextFileInfo {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The file's content.")
    private String content;
    @Schema(description = "The way in which to interpret the value for content. If not provided, the method will be determined from the content value itself.", allowableValues = FileContent.EMBEDDING_STRING+","+FileContent.EMBEDDING_URL+","+FileContent.EMBEDDING_BASE_64)
    private String embeddingMethod;

    /**
     * @return The context file's content.
     */
    public String getContent() { return this.content; }

    /**
     * @return The way in which to interpret the value for content. If not provided, the method will be determined from
     * the content value itself.
     */
    public String getEmbeddingMethod() { return this.embeddingMethod; }

    /**
     * @param content The context file's content.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @param embeddingMethod The way in which to interpret the value for content. If not provided, the method will be
     *                        determined from the content value itself.
     */
    public void setEmbeddingMethod(String embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    /**
     * Wrap the rule set's information metadata into a FileContent instance.
     *
     * @return The rule set information.
     */
    public FileContent toFileContent() {
        FileContent content = new FileContent();
        content.setContent(getContent());
        content.setEmbeddingMethod(FileContent.embeddingMethodFromString(getEmbeddingMethod()));
        return content;
    }

}
