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
import com.helger.io.resourceresolver.DefaultResourceResolver;
import com.helger.schematron.resolve.ISchematronIncludeResolver;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.Objects;

/**
 * Resolver of the includes of "pure" Schematron rules that checks each included resource before it is loaded.
 * <p/>
 * References are resolved relative to the root Schematron file. This is also the case for references made from within
 * included resources, as the resolver is not made aware of the including resource.
 */
public class SchematronIncludeResolver implements ISchematronIncludeResolver {

    private final String baseHref;
    private final SchematronReferenceAuthorizer authorizer;
    private boolean includesResolved = false;

    /**
     * Constructor.
     *
     * @param schematronFile The root Schematron file.
     * @param authorizer The authorizer to check included resources with.
     */
    public SchematronIncludeResolver(File schematronFile, SchematronReferenceAuthorizer authorizer) {
        this.baseHref = Objects.requireNonNull(new FileSystemResource(schematronFile).getAsURL()).toExternalForm();
        this.authorizer = authorizer;
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
        return resource;
    }

    /**
     * @return Whether at least one include was resolved.
     */
    public boolean hasResolvedIncludes() {
        return includesResolved;
    }

}
