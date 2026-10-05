/*
 * Copyright (C) 2026 European Union
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

package eu.europa.ec.itb.xml.validation;

import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;
import com.helger.io.resource.URLResource;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;

import java.net.URI;
import java.util.Locale;

/**
 * Component used to check the resources referenced from Schematron rules (e.g. imports and includes) before these
 * are loaded.
 */
public class SchematronReferenceAuthorizer {

    private final ImportedUriAuthorizer importUriAuthorizer;
    private final ImportedFileAuthorizer importFileAuthorizer;

    /**
     * Constructor.
     *
     * @param importUriAuthorizer The authorizer to use for imported URI resources (may be null).
     * @param importFileAuthorizer The authorizer to use for imported file resources (may be null).
     */
    public SchematronReferenceAuthorizer(ImportedUriAuthorizer importUriAuthorizer, ImportedFileAuthorizer importFileAuthorizer) {
        this.importUriAuthorizer = importUriAuthorizer;
        this.importFileAuthorizer = importFileAuthorizer;
    }

    /**
     * Check a referenced resource to see if loading can proceed.
     *
     * @param resource The resource to check.
     * @throws ValidatorException If the resource is not allowed to be loaded.
     */
    public void check(IReadableResource resource) {
        if (importUriAuthorizer != null || importFileAuthorizer != null) {
            if (resource instanceof URLResource urlResource) {
                URI resourceUri = urlResource.getAsURI();
                String scheme = resourceUri == null || resourceUri.getScheme() == null ? "" : resourceUri.getScheme().toLowerCase(Locale.ROOT);
                if (scheme.equals("http") || scheme.equals("https")) {
                    if (importUriAuthorizer != null) {
                        importUriAuthorizer.isUriAllowed(resourceUri);
                    }
                } else if (!scheme.equals("file")) {
                    // Other schemes (e.g. "jar:file:" or "zip:file:") would be treated as local resources and bypass the file checks.
                    throw new ValidatorException("validator.label.exception.notAllowedToReadImportedUri", String.valueOf(resourceUri));
                }
            } else if (importFileAuthorizer != null && resource instanceof FileSystemResource fileResource) {
                importFileAuthorizer.isPathAllowed(fileResource.getAsFile().toPath());
            }
        }
    }

}
