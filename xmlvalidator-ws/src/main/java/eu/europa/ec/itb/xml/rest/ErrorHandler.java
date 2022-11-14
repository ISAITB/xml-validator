package eu.europa.ec.itb.xml.rest;

import eu.europa.ec.itb.validation.commons.web.rest.BaseErrorHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Error handler for REST API calls.
 *
 * @see BaseErrorHandler
 */
@ControllerAdvice(assignableTypes = {RestValidationController.class})
public class ErrorHandler extends BaseErrorHandler {
}
