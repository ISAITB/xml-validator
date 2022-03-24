package eu.europa.ec.itb.xml.webhook;

/**
 * Constants linked to statistics reporting.
 */
public class StatisticReportingConstants {

    /**
     * Constructor to prevent instantiation.
     */
    private StatisticReportingConstants() { throw new IllegalStateException("Utility class"); }

    /** The web API. */
    public static final String WEB_API = "web";
    /** The SOAP API. */
    public static final String SOAP_API = "soap";
    /** The email API. */
    public static final String EMAIL_API = "email";
    
}
