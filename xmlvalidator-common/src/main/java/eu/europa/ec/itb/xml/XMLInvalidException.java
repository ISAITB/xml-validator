package eu.europa.ec.itb.xml;

/**
 * Class used to signal that the provided XML input could not be parsed as XML.
 */
public class XMLInvalidException extends Exception {

    /**
     * Constructor.
     *
     * @param cause The root cause.
     */
    public XMLInvalidException(Throwable cause) {
        super(cause);
    }
}
