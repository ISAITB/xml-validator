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

import com.helger.commons.error.IError;
import com.helger.schematron.pure.errorhandler.LoggingPSErrorHandler;

import javax.annotation.Nonnull;

/**
 * Custom error handler that is used in the case of pure schematron resources to detect whether errors are due to
 * the presence of external functions.
 * <p/>
 * In this case, the pure Schematron file should first be converted to XSLT format to avoid doing this on the fly for
 * each validation.
 */
public class PureSchematronErrorHandler extends LoggingPSErrorHandler {

    private boolean dueToExternalFunctionCall = false;

    @Override
    protected void handleInternally(@Nonnull IError aError) {
        super.handleInternally(aError);
        if (!dueToExternalFunctionCall && aError.getLinkedException() != null && aError.getLinkedException().getMessage() != null) {
            dueToExternalFunctionCall = aError.getLinkedException().getMessage().contains("External function calls have been disabled");
        }
    }

    public boolean isDueToExternalFunctionCall() {
        return dueToExternalFunctionCall;
    }

}
