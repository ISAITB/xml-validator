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

import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import net.sf.saxon.lib.CollectionFinder;
import net.sf.saxon.lib.ResourceCollection;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.TransformerFactory;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Collection finder that checks the location of a collection (as used by the XPath functions {@code collection()} and
 * {@code uri-collection()}) before delegating to the default implementation.
 * <p>
 * The URI resolver set for Schematron processing does not apply to collections, which Saxon resolves by itself. Without
 * this check, XSLT-based Schematron could list and read files and directories outside of those permitted.
 */
public class SchematronCollectionAuthorizer implements CollectionFinder {

    private final CollectionFinder delegate;
    private final ImportedUriAuthorizer importUriAuthorizer;
    private final ImportedFileAuthorizer importFileAuthorizer;

    /**
     * Constructor.
     *
     * @param delegate The finder to delegate to once a collection location is authorized.
     * @param importUriAuthorizer The authorizer to use for remote collection locations (may be null).
     * @param importFileAuthorizer The authorizer to use for local collection locations (may be null).
     */
    public SchematronCollectionAuthorizer(CollectionFinder delegate, ImportedUriAuthorizer importUriAuthorizer, ImportedFileAuthorizer importFileAuthorizer) {
        this.delegate = delegate;
        this.importUriAuthorizer = importUriAuthorizer;
        this.importFileAuthorizer = importFileAuthorizer;
    }

    /**
     * Restrict the collections that can be looked up by the provided transformer factory.
     *
     * @param factory The (Saxon) factory to configure.
     * @param importUriAuthorizer The authorizer to use for remote collection locations (may be null).
     * @param importFileAuthorizer The authorizer to use for local collection locations (may be null).
     * @throws IllegalStateException If the factory is not a Saxon factory (in which case we cannot restrict collections).
     */
    public static void install(TransformerFactory factory, ImportedUriAuthorizer importUriAuthorizer, ImportedFileAuthorizer importFileAuthorizer) {
        if (factory instanceof SaxonTransformerFactory saxonFactory) {
            var configuration = saxonFactory.getConfiguration();
            configuration.setCollectionFinder(new SchematronCollectionAuthorizer(configuration.getCollectionFinder(), importUriAuthorizer, importFileAuthorizer));
        } else {
            throw new IllegalStateException("Unable to restrict collection lookups for Schematron processing (unexpected transformer factory [%s])".formatted(factory.getClass().getName()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceCollection findCollection(XPathContext context, String collectionURI) throws XPathException {
        if (collectionURI != null) {
            check(collectionURI);
        }
        return delegate.findCollection(context, collectionURI);
    }

    /**
     * Check that the provided collection location can be accessed.
     *
     * @param collectionURI The location, which may include parameters (e.g. {@code file:///folder/?select=*.xml;recurse=yes}).
     * @throws ValidatorException If the location is not allowed.
     */
    private void check(String collectionURI) {
        // Parameters, that determine what is selected within the location, are not part of the location itself.
        int parametersStart = collectionURI.indexOf('?');
        String location = parametersStart < 0 ? collectionURI : collectionURI.substring(0, parametersStart);
        int fragmentStart = location.indexOf('#');
        if (fragmentStart >= 0) {
            location = location.substring(0, fragmentStart);
        }
        URI uri;
        try {
            uri = URI.create(location);
        } catch (IllegalArgumentException e) {
            throw notAllowed(collectionURI);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        switch (scheme) {
            case "file" -> {
                if (importFileAuthorizer != null) {
                    try {
                        importFileAuthorizer.isPathAllowed(Path.of(uri));
                    } catch (IllegalArgumentException | FileSystemNotFoundException e) {
                        throw notAllowed(collectionURI);
                    }
                }
            }
            case "http", "https" -> {
                if (importUriAuthorizer != null) {
                    importUriAuthorizer.isUriAllowed(uri);
                }
            }
            default -> {
                // Relative locations and other schemes (e.g. "jar:") are never allowed as they cannot be reliably checked.
                if (importUriAuthorizer != null || importFileAuthorizer != null) {
                    throw notAllowed(collectionURI);
                }
            }
        }
    }

    private ValidatorException notAllowed(String collectionURI) {
        return new ValidatorException("validator.label.exception.notAllowedToReadImportedUri", collectionURI);
    }

}
