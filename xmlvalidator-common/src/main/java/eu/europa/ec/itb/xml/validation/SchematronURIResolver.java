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
import com.helger.io.resourceresolver.DefaultResourceResolver;
import com.helger.xml.transform.DefaultTransformURIResolver;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * URI resolver to lookup references resources from the local file system during Schematron processing.
 */
public class SchematronURIResolver extends DefaultTransformURIResolver {

    private static final Logger LOG = LoggerFactory.getLogger(SchematronURIResolver.class);
    private final File schematronFile;
    private final ImportedFileAuthorizer importFileAuthorizer;
    private final ImportedUriAuthorizer importUriAuthorizer;

    /**
     * Constructor.
     *
     * @param schematronFile The root Schematron file.
     * @param importUriAuthorizer The authorizer to use for imported URI resources.
     * @param importFileAuthorizer The authorizer to use for imported file resources.
     */
    public SchematronURIResolver(File schematronFile, ImportedUriAuthorizer importUriAuthorizer, ImportedFileAuthorizer importFileAuthorizer) {
        this.schematronFile = schematronFile;
        this.importFileAuthorizer = importFileAuthorizer;
        this.importUriAuthorizer = importUriAuthorizer;
    }

    /**
     * Get the root Schematron file.
     *
     * @return The root file.
     */
    private File getBaseFile() {
        if (schematronFile.exists()) {
            if (schematronFile.isDirectory()) {
                return schematronFile;
            } else { // File
                return schematronFile.getParentFile();
            }
        } else {
            LOG.error("The root Schematron file could not be loaded [{}]", schematronFile.getAbsolutePath());
            throw new IllegalStateException("The root Schematron file could not be loaded");
        }
    }

    /**
     * Checks a remote URL resource reference to see if loading can proceed.
     *
     * @param href The href.
     * @param base The base.
     */
    private void checkOkToResolve(String href, String base) {
        if (importUriAuthorizer != null || importFileAuthorizer != null) {
            String baseToUse = base;
            if (StringUtils.isEmpty(baseToUse)) {
                baseToUse = getDefaultBase();
            }
            IReadableResource resource = DefaultResourceResolver.getResolvedResource(href, baseToUse);
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

    /**
     * Resolve a URI resource.
     *
     * @param href The href value.
     * @param base The base.
     * @return The loaded resource.
     * @throws TransformerException If an error occurs.
     */
    @Nullable
    @Override
    protected Source internalResolve(String href, String base) throws TransformerException {
        checkOkToResolve(href, base);
        Source result = super.internalResolve(href, base);
        if (result != null) {
            return result;
        } else {
            if (StringUtils.isBlank(base) && StringUtils.isBlank(href)) {
                try {
                    return new StreamSource(new FileInputStream(schematronFile));
                } catch (FileNotFoundException e) {
                    var message = String.format("Base schematron file not found base[%s] href[%s] file [%s]", base, href, schematronFile);
                    LOG.error(message);
                    throw new IllegalStateException(message);
                }
            } else {
                File baseFile;
                if (StringUtils.isBlank(base)) {
                    baseFile = getBaseFile();
                } else {
                    try {
                        URI uri = new URI(base);
                        baseFile = new File(uri);
                        baseFile = baseFile.getParentFile();
                    } catch (URISyntaxException e) {
                        throw new IllegalStateException(e);
                    }
                }
                // Lookup file directly under the base.
                File referencedFile = new File(baseFile, href);
                if (importFileAuthorizer != null) importFileAuthorizer.isPathAllowed(referencedFile.toPath());
                if (!referencedFile.exists()) {
                    // Try next to the current XSLT.
                    referencedFile = new File(schematronFile.getParent(), href);
                }
                if (referencedFile.exists()) {
                    try {
                        return new StreamSource(new FileInputStream(referencedFile));
                    } catch (FileNotFoundException e) {
                        LOG.error("Referenced file not found base[{}] href [{}] file [{}]", base, href, referencedFile);
                        throw new IllegalStateException(String.format("Referenced file not found base[%s] href[%s]", base, href));
                    }
                } else {
                    LOG.error("Referenced file not found base[{}] href [{}] file [{}]", base, href, referencedFile);
                    throw new IllegalStateException(String.format("Referenced file not found base[%s] href[%s]", base, href));
                }
            }
        }
    }

}
