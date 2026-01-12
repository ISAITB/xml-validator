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

package eu.europa.ec.itb.xml;

import eu.europa.ec.itb.validation.commons.FileInfo;

import java.io.File;

/**
 * Information on a Schematron file.
 */
public class SchematronFileInfo extends FileInfo {

    private final boolean supportPureValidation;

    /**
     * Constructor.
     *
     * @param fileInfo The schematron file information.
     * @param supportPureValidation Whether the schematron supports validation as 'pure' schematron.
     */
    public SchematronFileInfo(FileInfo fileInfo, boolean supportPureValidation) {
        this(fileInfo.getFile(), fileInfo.getType(), supportPureValidation);
    }

    /**
     * Constructor.
     *
     * @param file The schematron file.
     * @param type The schematron type.
     * @param supportPureValidation Whether the schematron supports validation as 'pure' schematron.
     */
    public SchematronFileInfo(File file, String type, boolean supportPureValidation) {
        super(file, type);
        this.supportPureValidation = supportPureValidation;
    }

    /**
     * @return Whether the schematron file supports validation via the 'pure' approach.
     */
    public boolean isSupportPureValidation() {
        return supportPureValidation;
    }
}
