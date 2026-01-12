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

package eu.europa.ec.itb.xml.upload;

import eu.europa.ec.itb.validation.commons.web.BaseFileController;
import eu.europa.ec.itb.xml.ApplicationConfig;
import eu.europa.ec.itb.xml.DomainConfigCache;
import eu.europa.ec.itb.xml.util.FileManager;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller used for the manipulation of user inputs and produced reports.
 */
@RestController
public class FileController extends BaseFileController<FileManager, ApplicationConfig, DomainConfigCache> {

    /**
     * @see BaseFileController#getInputFileName(String)
     *
     * @param id The UUID.
     * @return The file name.
     */
    @Override
    public String getInputFileName(String id) {
        return fileManager.getInputFileName(id);
    }

    /**
     * @see BaseFileController#getReportFileNameXml(String, boolean)
     *
     * @param id The UUID.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    @Override
    public String getReportFileNameXml(String id, boolean aggregate) {
        return fileManager.getReportFileNameXml(id, aggregate);
    }

    /**
     * @see BaseFileController#getReportFileNamePdf(String, boolean)
     *
     * @param id The UUID.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    @Override
    public String getReportFileNamePdf(String id, boolean aggregate) {
        return fileManager.getReportFileNamePdf(id, aggregate);
    }

    /**
     * @see BaseFileController#getReportFileNameCsv(String, boolean)
     *
     * @param id The UUID.
     * @param aggregate Whether the report is an aggregate.
     * @return The file name.
     */
    @Override
    public String getReportFileNameCsv(String id, boolean aggregate) {
        return fileManager.getReportFileNameCsv(id, aggregate);
    }

}
