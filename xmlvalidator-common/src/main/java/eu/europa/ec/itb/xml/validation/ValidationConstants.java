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

package eu.europa.ec.itb.xml.validation;

/**
 * Constants used to name inputs.
 */
public class ValidationConstants {

    /**
     * Constructor to prevent instantiation.
     */
    private ValidationConstants() { throw new IllegalStateException("Utility class"); }

    /** The validation type. */
    public static final String INPUT_TYPE = "type";
    /** The XML content to validate. */
    public static final String INPUT_XML = "xml";
    /** The explicit content embedding method. */
    public static final String INPUT_EMBEDDING_METHOD = "embeddingMethod";
    /** User-provided XSD wrapper. */
    public static final String INPUT_EXTERNAL_SCHEMA = "externalSchema";
    /** User-provided Schematron wrapper. */
    public static final String INPUT_EXTERNAL_SCHEMATRON = "externalSchematron";
    /** User-provided context files. */
    public static final String INPUT_CONTEXT_FILES = "contextFiles";
    /** The content of the user-provided artifact. */
    public static final String INPUT_EXTERNAL_ARTIFACT_CONTENT = "content";
    /** The type of provided artifact. */
    public static final String INPUT_EXTERNAL_ARTIFACT_TYPE = "type";
    /** Whether location information in errors should be a XPath expression. */
    public static final String INPUT_LOCATION_AS_PATH = "locationAsPath";
    /** Whether the validated content should be added to the TAR report. */
    public static final String INPUT_ADD_INPUT_TO_REPORT = "addInputToReport";
    /** Whether a simplified XPath expression should be added to report item locations. */
    public static final String INPUT_SHOW_LOCATION_PATHS = "showLocationPaths";
    /** The locale string to consider. */
    public static final String INPUT_LOCALE = "locale";

}
