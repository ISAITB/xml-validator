package eu.europa.ec.itb.envoice;

import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.einvoice.Configuration;
import eu.europa.ec.itb.einvoice.validation.XMLValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;

/**
 * Created by simatosc on 09/03/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public class XMLValidatorTest {

    private static JAXBContext REPORT_CONTEXT;
    private static ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    @Test
    public void testValidation() throws FileNotFoundException, JAXBException {
        XMLValidator validator = new XMLValidator(new FileInputStream("D:\\tools\\probatron4j-0.7.4\\invoice.xml"));
        TAR report = validator.validateAll();
        printReport(report);
    }

    public void printReport(TAR report) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(TAR.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(OBJECT_FACTORY.createTestStepReport(report), System.out);
    }

}
