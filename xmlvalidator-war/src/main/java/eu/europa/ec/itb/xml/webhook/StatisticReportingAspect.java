package eu.europa.ec.itb.xml.webhook;

import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import eu.europa.ec.itb.validation.commons.war.webhook.StatisticReporting;
import eu.europa.ec.itb.validation.commons.war.webhook.UsageData;
import eu.europa.ec.itb.xml.validation.XMLValidator;
import eu.europa.ec.itb.xml.ws.ValidationServiceImpl;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.handler.MessageContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Aspect that advises the application's entry points to extract and send usage statistics (if enabled).
 */
@Aspect
@Component
@ConditionalOnProperty(name = "validator.webhook.statistics")
public class StatisticReportingAspect extends StatisticReporting {

    private static final Logger logger = LoggerFactory.getLogger(StatisticReportingAspect.class);
    private static final ThreadLocal<Map<String, String>> adviceContext = new ThreadLocal<>();

    /**
     * Pointcut for minimal WEB validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.xml.upload.UploadController.handleUploadMinimal(..))")
    private void minimalUploadValidation() {}

    /**
     * Pointcut for regular WEB validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.xml.upload.UploadController.handleUpload(..))")
    private void uploadValidation() {}

    /**
     * Pointcut for minimal WEB validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.xml.upload.UploadController.handleUploadMinimalEmbedded(..))")
    private void minimalEmbeddedUploadValidation(){}

    /**
     * Pointcut for regular WEB validation.
     */
    @Pointcut("execution(public * eu.europa.ec.itb.xml.upload.UploadController.handleUploadEmbedded(..))")
    private void embeddedUploadValidation(){}

    /**
     * Advice to obtain the arguments passed to the web upload API call.
     *
     * @param joinPoint The original call's information.
     */
    @Before("minimalUploadValidation() || uploadValidation() || minimalEmbeddedUploadValidation() || embeddedUploadValidation()")
    public void getUploadContext(JoinPoint joinPoint) {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("api", StatisticReportingConstants.WEB_API);
        if (config.getWebhook().isStatisticsEnableCountryDetection()) {
            HttpServletRequest request = getHttpRequest(joinPoint);
            if (request != null) {
                String ip = extractIpAddress(request);
                contextParams.put("ip", ip);
            }
        }
        adviceContext.set(contextParams);
    }

    /**
     * Advice to obtain the arguments passed to the SOAP API call.
     *
     * @param joinPoint The original call's information.
     */
    @Before(value = "execution(public * eu.europa.ec.itb.xml.ws.ValidationServiceImpl.validate(..))")
    public void getSoapCallContext(JoinPoint joinPoint) {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("api", StatisticReportingConstants.SOAP_API);
        if (config.getWebhook().isStatisticsEnableCountryDetection()) {
            ValidationServiceImpl validationService = (ValidationServiceImpl) joinPoint.getTarget();
            HttpServletRequest request = (HttpServletRequest) validationService.getWebServiceContext()
                    .getMessageContext().get(MessageContext.SERVLET_REQUEST);
            String ip = extractIpAddress(request);
            contextParams.put("ip", ip);
        }
        adviceContext.set(contextParams);
    }

    /**
     * Advice to obtain the arguments passed to the email API call.
     *
     * @param joinPoint The original call's information.
     */
    @Before(value = "execution(public * eu.europa.ec.itb.xml.email.MailHandler.getValidationType(..))")
    public void getEmailContext(JoinPoint joinPoint) {
        Map<String, String> contextParams = new HashMap<>();
        contextParams.put("api", StatisticReportingConstants.EMAIL_API);
        adviceContext.set(contextParams);
    }

    /**
     * Advice to send the usage report.
     *
     * @param joinPoint The original call's information.
     */
    @Around("execution(public * eu.europa.ec.itb.xml.validation.XMLValidator.validateAll(..))")
    public Object reportValidatorDataUsage(ProceedingJoinPoint joinPoint) throws Throwable {
        XMLValidator validator = (XMLValidator) joinPoint.getTarget();
        Object report = joinPoint.proceed();
        try {
            Map<String, String> usageParams = adviceContext.get();
            String validatorId = config.getIdentifier();
            String domain = validator.getDomain();
            String validationType = validator.getValidationType();
            String api = usageParams.get("api");
            // obtain the result of the model
            String ip = usageParams.get("ip");
            TAR reportTAR = (TAR) report;
            UsageData.Result result = extractResult(reportTAR);
            // Send the usage data
            sendUsageData(validatorId, domain, api, validationType, result, ip);
        } catch (Exception ex) {
            // Ensure unexpected errors never block validation processing
            logger.warn("Unexpected error during statistics reporting", ex);
        }
        return report;
    }

    /**
     * Method that obtains a TAR object and obtains the result of the validation to
     * be reported.
     *
     * @param report The report to consider.
     * @return The validation result.
     */
    private UsageData.Result extractResult(TAR report) {
        TestResultType tarResult = report.getResult();
        if (tarResult == TestResultType.SUCCESS) {
            return UsageData.Result.SUCCESS;
        } else if (tarResult == TestResultType.WARNING) {
            return UsageData.Result.WARNING;
        } else {
            return UsageData.Result.FAILURE;
        }
    }

}
