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
import com.helger.schematron.resolve.ISchematronIncludeResolver;
import eu.europa.ec.itb.validation.commons.URLReader;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolver of the includes of "pure" Schematron rules that checks each included resource before it is loaded.
 * <p/>
 * References are resolved relative to the root Schematron file. This is also the case for references made from within
 * included resources, as the resolver is not made aware of the including resource.
 */
public class SchematronIncludeResolver implements ISchematronIncludeResolver {

    private final File schematronFile;
    private final String baseHref;
    private final SchematronReferenceAuthorizer authorizer;
    private final URLReader urlReader;
    private final HttpClient.Version httpVersion;
    private final List<Path> downloadedIncludes = new ArrayList<>();
    private boolean includesResolved = false;

    /**
     * Constructor.
     *
     * @param schematronFile The root Schematron file.
     * @param authorizer The authorizer to check included resources with.
     */
    public SchematronIncludeResolver(File schematronFile, SchematronReferenceAuthorizer authorizer) {
        this(schematronFile, authorizer, new URLReader(), HttpClient.Version.HTTP_2);
    }

    /**
     * Constructor.
     *
     * @param schematronFile The root Schematron file.
     * @param authorizer The authorizer to check included resources with.
     * @param urlReader The reader to use to load remote includes.
     * @param httpVersion The HTTP version to use to load remote includes.
     */
    public SchematronIncludeResolver(File schematronFile, SchematronReferenceAuthorizer authorizer, URLReader urlReader, HttpClient.Version httpVersion) {
        this.schematronFile = schematronFile;
        this.baseHref = Objects.requireNonNull(new FileSystemResource(schematronFile).getAsURL()).toExternalForm();
        this.authorizer = authorizer;
        this.urlReader = urlReader;
        this.httpVersion = httpVersion;
    }

    /**
     * Resolve and check the included resource. The resource is never null, to ensure that the (unchecked) default
     * resolution is never applied.
     *
     * @param href The reference to resolve.
     * @return The resolved resource.
     */
    @Override
    @NonNull
    public IReadableResource getResolvedSchematronResource(@NonNull String href) {
        includesResolved = true;
        IReadableResource resource = DefaultResourceResolver.getResolvedResource(href, baseHref);
        authorizer.check(resource);
        if (authorizer.isRestrictedRemoteResource(resource)) {
            // Load the include ourselves so that redirects are also checked.
            return download(((URLResource) resource).getAsURI());
        }
        return resource;
    }

    /**
     * Download a remote include to a temporary file next to the root Schematron file.
     *
     * @param uri The URI of the include.
     * @return The resource for the downloaded include.
     */
    private IReadableResource download(URI uri) {
        var streamInfo = urlReader.stream(uri, null, httpVersion, null, authorizer.remoteGuard());
        try (var in = streamInfo.stream()) {
            Path downloaded = Files.createTempFile(schematronFile.toPath().toAbsolutePath().getParent(), "include-", ".sch");
            downloadedIncludes.add(downloaded);
            Files.copy(in, downloaded, StandardCopyOption.REPLACE_EXISTING);
            return new FileSystemResource(downloaded.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load the Schematron include [%s]".formatted(uri), e);
        }
    }

    /**
     * Delete the files created for remote includes (to be called once the includes have been processed).
     */
    public void cleanup() {
        downloadedIncludes.forEach(path -> FileUtils.deleteQuietly(path.toFile()));
        downloadedIncludes.clear();
    }

    /**
     * @return Whether at least one include was resolved.
     */
    public boolean hasResolvedIncludes() {
        return includesResolved;
    }

}
