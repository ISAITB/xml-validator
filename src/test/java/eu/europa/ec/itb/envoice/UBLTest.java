package eu.europa.ec.itb.envoice;

import com.gitb.utils.XMLUtils;
import eu.europa.ec.itb.einvoice.ws.ValidationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.*;
import java.util.Stack;

/**
 * Created by simatosc on 25/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public class UBLTest {

//    @Autowired
//    ValidationService validationService;

    private String str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\" xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\" xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\" xmlns:ccts=\"urn:un:unece:uncefact:documentation:2\" xmlns:qdt=\"urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2\" xmlns:udt=\"urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2\">\n" +
            "\t<cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n" +
            "\t<cbc:CustomizationID>urn:www.cenbii.eu:transaction:biitrns010:ver2.0:extended:urn:www.edelivery.eu:bis:edelivery4a:ver2.0</cbc:CustomizationID>\n" +
            "\t<cbc:ProfileID>urn:www.cenbii.eu:profile:bii04:ver2.0</cbc:ProfileID>\n" +
            "\t<cbc:ID>TOSL110</cbc:ID>\n" +
            "\t<cbc:IssueDate>2013-04-10</cbc:IssueDate>\n" +
            "\t<cbc:InvoiceTypeCode listID=\"UNCL1001\">380</cbc:InvoiceTypeCode>\n" +
            "\t<cbc:Note>Ordered through catalogue</cbc:Note>\n" +
            "\t<cbc:DocumentCurrencyCode listID=\"ISO4217\">DKK</cbc:DocumentCurrencyCode>\n" +
            "\t<cbc:AccountingCost>54321</cbc:AccountingCost>\n" +
            "\t<cac:OrderReference>\n" +
            "\t\t<cbc:ID>ORD544</cbc:ID>\n" +
            "\t</cac:OrderReference>\n" +
            "\t<cac:ContractDocumentReference>\n" +
            "\t\t<cbc:ID>CON123</cbc:ID>\n" +
            "\t\t<cbc:DocumentTypeCode listID=\"UNCL1001\">916</cbc:DocumentTypeCode>\n" +
            "\t\t<cbc:DocumentType>Contract - related document</cbc:DocumentType>\n" +
            "\t</cac:ContractDocumentReference>\n" +
            "\t<cac:AdditionalDocumentReference>\n" +
            "\t\t<cbc:ID>AGR321</cbc:ID>\n" +
            "\t\t<cbc:DocumentType>Additional agreement</cbc:DocumentType>\n" +
            "\t</cac:AdditionalDocumentReference>\n" +
            "\t<cac:AccountingSupplierParty>\n" +
            "\t\t<cac:Party>\n" +
            "\t\t\t<cbc:EndpointID schemeID=\"GLN\">5790989675432</cbc:EndpointID>\n" +
            "\t\t\t<cac:PartyIdentification>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"DK:CVR\">DK16356706\"</cbc:ID>\n" +
            "\t\t\t</cac:PartyIdentification>\n" +
            "\t\t\t<cac:PartyName>\n" +
            "\t\t\t\t<cbc:Name>SellerCompany</cbc:Name>\n" +
            "\t\t\t</cac:PartyName>\n" +
            "\t\t\t<cac:PostalAddress>\n" +
            "\t\t\t\t<cbc:StreetName>Main street 2, Building 4</cbc:StreetName>\n" +
            "\t\t\t\t<cbc:CityName>Big city</cbc:CityName>\n" +
            "\t\t\t\t<cbc:PostalZone>54321</cbc:PostalZone>\n" +
            "\t\t\t\t<cac:Country>\n" +
            "\t\t\t\t\t<cbc:IdentificationCode listID=\"ISO3166-1:Alpha2\">DK</cbc:IdentificationCode>\n" +
            "\t\t\t\t</cac:Country>\n" +
            "\t\t\t</cac:PostalAddress>\n" +
            "\t\t\t<cac:PartyTaxScheme>\n" +
            "\t\t\t\t<cbc:CompanyID schemeID=\"DK:SE\">DK16356706</cbc:CompanyID>\n" +
            "\t\t\t\t<cac:TaxScheme>\n" +
            "\t\t\t\t\t<cbc:ID>VAT</cbc:ID>\n" +
            "\t\t\t\t</cac:TaxScheme>\n" +
            "\t\t\t</cac:PartyTaxScheme>\n" +
            "\t\t\t<cac:PartyLegalEntity>\n" +
            "\t\t\t\t<cbc:RegistrationName>The Sellercompany Incorporated</cbc:RegistrationName>\n" +
            "\t\t\t\t<cbc:CompanyID schemeID=\"DK:CVR\">DK16356706</cbc:CompanyID>\n" +
            "\t\t\t\t<cac:RegistrationAddress>\n" +
            "\t\t\t\t\t<cbc:CityName>Big city</cbc:CityName>\n" +
            "\t\t\t\t</cac:RegistrationAddress>\n" +
            "\t\t\t</cac:PartyLegalEntity>\n" +
            "\t\t\t<cac:Contact>\n" +
            "\t\t\t\t<cbc:Name>Anthon Larsen</cbc:Name>\n" +
            "\t\t\t\t<cbc:Telephone>+4598989898</cbc:Telephone>\n" +
            "\t\t\t\t<cbc:ElectronicMail>Anthon@SellerCompany.dk</cbc:ElectronicMail>\n" +
            "\t\t\t</cac:Contact>\n" +
            "\t\t</cac:Party>\n" +
            "\t</cac:AccountingSupplierParty>\n" +
            "\t<cac:AccountingCustomerParty>\n" +
            "\t\t<cac:Party>\n" +
            "\t\t\t<cbc:EndpointID schemeID=\"GLN\">5790989876765</cbc:EndpointID>\n" +
            "\t\t\t<cac:PartyIdentification>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"DK:CVR\">DK57356709\"</cbc:ID>\n" +
            "\t\t\t</cac:PartyIdentification>\n" +
            "\t\t\t<cac:PartyName>\n" +
            "\t\t\t\t<cbc:Name>Buyercompany ltd</cbc:Name>\n" +
            "\t\t\t</cac:PartyName>\n" +
            "\t\t\t<cac:PostalAddress>\n" +
            "\t\t\t\t<cbc:StreetName>Anystreet, Building 1</cbc:StreetName>\n" +
            "\t\t\t\t<cbc:CityName>Anytown</cbc:CityName>\n" +
            "\t\t\t\t<cbc:PostalZone>101</cbc:PostalZone>\n" +
            "\t\t\t\t<cac:Country>\n" +
            "\t\t\t\t\t<cbc:IdentificationCode listID=\"ISO3166-1:Alpha2\">DK</cbc:IdentificationCode>\n" +
            "\t\t\t\t</cac:Country>\n" +
            "\t\t\t</cac:PostalAddress>\n" +
            "\t\t\t<cac:PartyLegalEntity>\n" +
            "\t\t\t\t<cbc:RegistrationName>Buyer Inc</cbc:RegistrationName>\n" +
            "\t\t\t\t<cbc:CompanyID schemeID=\"DK:CVR\">DK57356709</cbc:CompanyID>\n" +
            "\t\t\t</cac:PartyLegalEntity>\n" +
            "\t\t\t<cac:Contact>\n" +
            "\t\t\t\t<cbc:Name>John Hansen</cbc:Name>\n" +
            "\t\t\t\t<cbc:Telephone>+4522446688</cbc:Telephone>\n" +
            "\t\t\t\t<cbc:ElectronicMail>John@buyerinc.dk</cbc:ElectronicMail>\n" +
            "\t\t\t</cac:Contact>\n" +
            "\t\t</cac:Party>\n" +
            "\t</cac:AccountingCustomerParty>\n" +
            "\t<cac:PayeeParty>\n" +
            "\t\t<cac:PartyIdentification>\n" +
            "\t\t\t<cbc:ID schemeID=\"DK:CVR\">DK57355804></cbc:ID>\n" +
            "\t\t</cac:PartyIdentification>\n" +
            "\t\t<cac:PartyName>\n" +
            "\t\t\t<cbc:Name>Payee part</cbc:Name>\n" +
            "\t\t</cac:PartyName>\n" +
            "\t\t<cac:PartyLegalEntity>\n" +
            "\t\t\t<cbc:CompanyID schemeID=\"DK:CVR\">DK57355804</cbc:CompanyID>\n" +
            "\t\t</cac:PartyLegalEntity>\n" +
            "\t</cac:PayeeParty>\n" +
            "\t<cac:Delivery>\n" +
            "\t\t<cbc:ActualDeliveryDate>2013-04-15</cbc:ActualDeliveryDate>\n" +
            "\t\t<cac:DeliveryLocation>\n" +
            "\t\t\t<cbc:ID schemeID=\"GLN\">5790989865761</cbc:ID>\n" +
            "\t\t\t<cac:Address>\n" +
            "\t\t\t\t<cbc:StreetName>Deliverystreet</cbc:StreetName>\n" +
            "\t\t\t\t<cbc:CityName>Deliverycity</cbc:CityName>\n" +
            "\t\t\t\t<cbc:PostalZone>9000</cbc:PostalZone>\n" +
            "\t\t\t\t<cac:Country>\n" +
            "\t\t\t\t\t<cbc:IdentificationCode listID=\"ISO3166-1:Alpha2\">DK</cbc:IdentificationCode>\n" +
            "\t\t\t\t</cac:Country>\n" +
            "\t\t\t</cac:Address>\n" +
            "\t\t</cac:DeliveryLocation>\n" +
            "\t</cac:Delivery>\n" +
            "\t<cac:PaymentMeans>\n" +
            "\t\t<cbc:PaymentMeansCode listID=\"UNCL4461\">42</cbc:PaymentMeansCode>\n" +
            "\t\t<cbc:PaymentDueDate>2013-05-10</cbc:PaymentDueDate>\n" +
            "\t\t<cbc:PaymentID>Payref1</cbc:PaymentID>\n" +
            "\t\t<cac:PayeeFinancialAccount>\n" +
            "\t\t\t<cbc:ID schemeID=\"IBAN\">DK1212341234123412</cbc:ID>\n" +
            "\t\t\t<cac:FinancialInstitutionBranch>\n" +
            "\t\t\t\t<cac:FinancialInstitution>\n" +
            "\t\t\t\t\t<cbc:ID schemeID=\"BIC\">DKXDABCD</cbc:ID>\n" +
            "\t\t\t\t</cac:FinancialInstitution>\n" +
            "\t\t\t</cac:FinancialInstitutionBranch>\n" +
            "\t\t</cac:PayeeFinancialAccount>\n" +
            "\t</cac:PaymentMeans>\n" +
            "\t<cac:PaymentTerms>\n" +
            "\t\t<cbc:Note>Payment should be completed before due date</cbc:Note>\n" +
            "\t</cac:PaymentTerms>\n" +
            "\t<cac:AllowanceCharge>\n" +
            "\t\t<cbc:ChargeIndicator>true</cbc:ChargeIndicator>\n" +
            "\t\t<cbc:AllowanceChargeReasonCode listID=\"UNCL4465\">93</cbc:AllowanceChargeReasonCode>\n" +
            "\t\t<cbc:AllowanceChargeReason>Invoicing fee</cbc:AllowanceChargeReason>\n" +
            "\t\t<cbc:Amount currencyID=\"DKK\">100.00</cbc:Amount>\n" +
            "\t\t<cac:TaxCategory>\n" +
            "\t\t\t<cbc:ID schemeID=\"UNCL5305\">S</cbc:ID>\n" +
            "\t\t\t<cbc:Percent>25</cbc:Percent>\n" +
            "\t\t\t<cac:TaxScheme>\n" +
            "\t\t\t\t<cbc:ID>VAT</cbc:ID>\n" +
            "\t\t\t</cac:TaxScheme>\n" +
            "\t\t</cac:TaxCategory>\n" +
            "\t</cac:AllowanceCharge>\n" +
            "\t<cac:TaxTotal>\n" +
            "\t\t<cbc:TaxAmount currencyID=\"DKK\">705.00</cbc:TaxAmount>\n" +
            "\t\t<cac:TaxSubtotal>\n" +
            "\t\t\t<cbc:TaxableAmount currencyID=\"DKK\">1500.00</cbc:TaxableAmount>\n" +
            "\t\t\t<cbc:TaxAmount currencyID=\"DKK\">375.00</cbc:TaxAmount>\n" +
            "\t\t\t<cac:TaxCategory>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"UNCL5305\">S</cbc:ID>\n" +
            "\t\t\t\t<cbc:Percent>25</cbc:Percent>\n" +
            "\t\t\t\t<cac:TaxScheme>\n" +
            "\t\t\t\t\t<cbc:ID>VAT</cbc:ID>\n" +
            "\t\t\t\t</cac:TaxScheme>\n" +
            "\t\t\t</cac:TaxCategory>\n" +
            "\t\t</cac:TaxSubtotal>\n" +
            "\t\t<cac:TaxSubtotal>\n" +
            "\t\t\t<cbc:TaxableAmount currencyID=\"DKK\">2750.00</cbc:TaxableAmount>\n" +
            "\t\t\t<cbc:TaxAmount currencyID=\"DKK\">330.00</cbc:TaxAmount>\n" +
            "\t\t\t<cac:TaxCategory>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"UNCL5305\">AA</cbc:ID>\n" +
            "\t\t\t\t<cbc:Percent>12</cbc:Percent>\n" +
            "\t\t\t\t<cac:TaxScheme>\n" +
            "\t\t\t\t\t<cbc:ID>VAT</cbc:ID>\n" +
            "\t\t\t\t</cac:TaxScheme>\n" +
            "\t\t\t</cac:TaxCategory>\n" +
            "\t\t</cac:TaxSubtotal>\n" +
            "\t</cac:TaxTotal>\n" +
            "\t<cac:LegalMonetaryTotal>\n" +
            "\t\t<cbc:LineExtensionAmount currencyID=\"DKK\">415000.00</cbc:LineExtensionAmount>\n" +
            "\t\t<cbc:TaxExclusiveAmount currencyID=\"DKK\">4250.00</cbc:TaxExclusiveAmount>\n" +
            "\t\t<cbc:TaxInclusiveAmount currencyID=\"DKK\">4956.00</cbc:TaxInclusiveAmount>\n" +
            "\t\t<cbc:ChargeTotalAmount currencyID=\"DKK\">100.00</cbc:ChargeTotalAmount>\n" +
            "\t\t<cbc:PayableRoundingAmount currencyID=\"DKK\">1.00</cbc:PayableRoundingAmount>\n" +
            "\t\t<cbc:PayableAmount currencyID=\"DKK\">4956.00</cbc:PayableAmount>\n" +
            "\t</cac:LegalMonetaryTotal>\n" +
            "\t<cac:InvoiceLine>\n" +
            "\t\t<cbc:ID>1</cbc:ID>\n" +
            "\t\t<cbc:InvoicedQuantity unitCode=\"C62\" unitCodeListID=\"UNECERec20\">1000</cbc:InvoicedQuantity>\n" +
            "\t\t<cbc:LineExtensionAmount currencyID=\"DKK\">900.00</cbc:LineExtensionAmount>\n" +
            "\t\t<cbc:AccountingCost>BookingCode001</cbc:AccountingCost>\n" +
            "\t\t<cac:OrderLineReference>\n" +
            "\t\t\t<cbc:LineID>1</cbc:LineID>\n" +
            "\t\t</cac:OrderLineReference>\n" +
            "\t\t<cac:AllowanceCharge>\n" +
            "\t\t\t<cbc:ChargeIndicator>false</cbc:ChargeIndicator>\n" +
            "\t\t\t<cbc:AllowanceChargeReason>Discount</cbc:AllowanceChargeReason>\n" +
            "\t\t\t<cbc:Amount currencyID=\"DKK\">100.00</cbc:Amount>\n" +
            "\t\t</cac:AllowanceCharge>\n" +
            "\t\t<cac:TaxTotal>\n" +
            "\t\t\t<cbc:TaxAmount currencyID=\"DKK\">250.00</cbc:TaxAmount>\n" +
            "\t\t</cac:TaxTotal>\n" +
            "\t\t<cac:Item>\n" +
            "\t\t\t<cbc:Description>Printing paper, 2mm</cbc:Description>\n" +
            "\t\t\t<cbc:Name>Printing paper</cbc:Name>\n" +
            "\t\t\t<cac:SellersItemIdentification>\n" +
            "\t\t\t\t<cbc:ID>JB007</cbc:ID>\n" +
            "\t\t\t</cac:SellersItemIdentification>\n" +
            "\t\t\t<cac:StandardItemIdentification>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"GTIN\">05704368643453</cbc:ID>\n" +
            "\t\t\t</cac:StandardItemIdentification>\n" +
            "\t\t\t<cac:CommodityClassification>\n" +
            "\t\t\t\t<cbc:ItemClassificationCode listID=\"UNSPSC\">12344321</cbc:ItemClassificationCode>\n" +
            "\t\t\t</cac:CommodityClassification>\n" +
            "\t\t\t<cac:ClassifiedTaxCategory>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"UNCL5305\">S</cbc:ID>\n" +
            "\t\t\t\t<cbc:Percent>25</cbc:Percent>\n" +
            "\t\t\t\t<cac:TaxScheme>\n" +
            "\t\t\t\t\t<cbc:ID>VAT</cbc:ID>\n" +
            "\t\t\t\t</cac:TaxScheme>\n" +
            "\t\t\t</cac:ClassifiedTaxCategory>\n" +
            "\t\t</cac:Item>\n" +
            "\t\t<cac:Price>\n" +
            "\t\t\t<cbc:PriceAmount currencyID=\"DKK\">1.00</cbc:PriceAmount>\n" +
            "\t\t</cac:Price>\n" +
            "\t</cac:InvoiceLine>\n" +
            "\t<cac:InvoiceLine>\n" +
            "\t\t<cbc:ID>2</cbc:ID>\n" +
            "\t\t<cbc:InvoicedQuantity unitCode=\"C62\" unitCodeListID=\"UNECERec20\">100</cbc:InvoicedQuantity>\n" +
            "\t\t<cbc:LineExtensionAmount currencyID=\"DKK\">500.00</cbc:LineExtensionAmount>\n" +
            "\t\t<cbc:AccountingCost>BookingCode002</cbc:AccountingCost>\n" +
            "\t\t<cac:OrderLineReference>\n" +
            "\t\t\t<cbc:LineID>2</cbc:LineID>\n" +
            "\t\t</cac:OrderLineReference>\n" +
            "\t\t<cac:TaxTotal>\n" +
            "\t\t\t<cbc:TaxAmount currencyID=\"DKK\">125.00</cbc:TaxAmount>\n" +
            "\t\t</cac:TaxTotal>\n" +
            "\t\t<cac:Item>\n" +
            "\t\t\t<cbc:Description>Parker Pen, Black, model Sansa</cbc:Description>\n" +
            "\t\t\t<cbc:Name>Parker Pen</cbc:Name>\n" +
            "\t\t\t<cac:SellersItemIdentification>\n" +
            "\t\t\t\t<cbc:ID>JB008</cbc:ID>\n" +
            "\t\t\t</cac:SellersItemIdentification>\n" +
            "\t\t\t<cac:StandardItemIdentification>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"GTIN\">05704368876486</cbc:ID>\n" +
            "\t\t\t</cac:StandardItemIdentification>\n" +
            "\t\t\t<cac:CommodityClassification>\n" +
            "\t\t\t\t<cbc:ItemClassificationCode listID=\"UNSPSC\">44121702</cbc:ItemClassificationCode>\n" +
            "\t\t\t</cac:CommodityClassification>\n" +
            "\t\t\t<cac:ClassifiedTaxCategory>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"UNCL5305\">S</cbc:ID>\n" +
            "\t\t\t\t<cbc:Percent>25</cbc:Percent>\n" +
            "\t\t\t\t<cac:TaxScheme>\n" +
            "\t\t\t\t\t<cbc:ID>VAT</cbc:ID>\n" +
            "\t\t\t\t</cac:TaxScheme>\n" +
            "\t\t\t</cac:ClassifiedTaxCategory>\n" +
            "\t\t</cac:Item>\n" +
            "\t\t<cac:Price>\n" +
            "\t\t\t<cbc:PriceAmount currencyID=\"DKK\">5.00</cbc:PriceAmount>\n" +
            "\t\t</cac:Price>\n" +
            "\t</cac:InvoiceLine>\n" +
            "\t<cac:InvoiceLine>\n" +
            "\t\t<cbc:ID>3</cbc:ID>\n" +
            "\t\t<cbc:InvoicedQuantity unitCode=\"C62\" unitCodeListID=\"UNECERec20\">500.00</cbc:InvoicedQuantity>\n" +
            "\t\t<cbc:LineExtensionAmount currencyID=\"DKK\">2500.00</cbc:LineExtensionAmount>\n" +
            "\t\t<cbc:AccountingCost>BookingCode003</cbc:AccountingCost>\n" +
            "\t\t<cac:OrderLineReference>\n" +
            "\t\t\t<cbc:LineID>3</cbc:LineID>\n" +
            "\t\t</cac:OrderLineReference>\n" +
            "\t\t<cac:TaxTotal>\n" +
            "\t\t\t<cbc:TaxAmount currencyID=\"DKK\">60.00</cbc:TaxAmount>\n" +
            "\t\t</cac:TaxTotal>\n" +
            "\t\t<cac:Item>\n" +
            "\t\t\t<cbc:Name>American Cookies</cbc:Name>\n" +
            "\t\t\t<cac:SellersItemIdentification>\n" +
            "\t\t\t\t<cbc:ID>JB009</cbc:ID>\n" +
            "\t\t\t</cac:SellersItemIdentification>\n" +
            "\t\t\t<cac:StandardItemIdentification>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"GTIN\">05704368124358</cbc:ID>\n" +
            "\t\t\t</cac:StandardItemIdentification>\n" +
            "\t\t\t<cac:CommodityClassification>\n" +
            "\t\t\t\t<cbc:ItemClassificationCode listID=\"UNSPSC\">50181905</cbc:ItemClassificationCode>\n" +
            "\t\t\t</cac:CommodityClassification>\n" +
            "\t\t\t<cac:ClassifiedTaxCategory>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"UNCL5305\">AA</cbc:ID>\n" +
            "\t\t\t\t<cbc:Percent>12</cbc:Percent>\n" +
            "\t\t\t\t<cac:TaxScheme>\n" +
            "\t\t\t\t\t<cbc:ID>VAT</cbc:ID>\n" +
            "\t\t\t\t</cac:TaxScheme>\n" +
            "\t\t\t</cac:ClassifiedTaxCategory>\n" +
            "\t\t</cac:Item>\n" +
            "\t\t<cac:Price>\n" +
            "\t\t\t<cbc:PriceAmount currencyID=\"DKK\">5.00</cbc:PriceAmount>\n" +
            "\t\t</cac:Price>\n" +
            "\t</cac:InvoiceLine>\n" +
            "\t<cac:InvoiceLine>\n" +
            "\t\t<cbc:ID>4</cbc:ID>\n" +
            "\t\t<cbc:InvoicedQuantity unitCode=\"C62\" unitCodeListID=\"UNECERec20\">500</cbc:InvoicedQuantity>\n" +
            "\t\t<cbc:LineExtensionAmount currencyID=\"DKK\">250.00</cbc:LineExtensionAmount>\n" +
            "\t\t<cbc:AccountingCost>BookingCode004</cbc:AccountingCost>\n" +
            "\t\t<cac:OrderLineReference>\n" +
            "\t\t\t<cbc:LineID>4</cbc:LineID>\n" +
            "\t\t</cac:OrderLineReference>\n" +
            "\t\t<cac:TaxTotal>\n" +
            "\t\t\t<cbc:TaxAmount currencyID=\"DKK\">30.00</cbc:TaxAmount>\n" +
            "\t\t</cac:TaxTotal>\n" +
            "\t\t<cac:Item>\n" +
            "\t\t\t<cbc:Name>Crunchy cookies</cbc:Name>\n" +
            "\t\t\t<cac:SellersItemIdentification>\n" +
            "\t\t\t\t<cbc:ID>JB009</cbc:ID>\n" +
            "\t\t\t</cac:SellersItemIdentification>\n" +
            "\t\t\t<cac:StandardItemIdentification>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"GTIN\">05704368876486</cbc:ID>\n" +
            "\t\t\t</cac:StandardItemIdentification>\n" +
            "\t\t\t<cac:CommodityClassification>\n" +
            "\t\t\t\t<cbc:ItemClassificationCode listID=\"UNSPSC\">50181905</cbc:ItemClassificationCode>\n" +
            "\t\t\t</cac:CommodityClassification>\n" +
            "\t\t\t<cac:ClassifiedTaxCategory>\n" +
            "\t\t\t\t<cbc:ID schemeID=\"UNCL5305\">AA</cbc:ID>\n" +
            "\t\t\t\t<cbc:Percent>12</cbc:Percent>\n" +
            "\t\t\t\t<cac:TaxScheme>\n" +
            "\t\t\t\t\t<cbc:ID>VAT</cbc:ID>\n" +
            "\t\t\t\t</cac:TaxScheme>\n" +
            "\t\t\t</cac:ClassifiedTaxCategory>\n" +
            "\t\t</cac:Item>\n" +
            "\t\t<cac:Price>\n" +
            "\t\t\t<cbc:PriceAmount currencyID=\"DKK\">0.5</cbc:PriceAmount>\n" +
            "\t\t</cac:Price>\n" +
            "\t</cac:InvoiceLine>\n" +
            "</Invoice>";

    @Test
    public void testXML() throws Exception {
        ValidationService validationService = new ValidationService();
//        validationService.validate();
    }

    @Test
    public void testReadXML() throws IOException, SAXException {
        System.out.println("Running");
        Document doc1 = readXMLWithLineNumbers(new ByteArrayInputStream(str.getBytes()));
        System.out.println(doc1.getDocumentElement());
        Document doc2 = readXMLWithLineNumbers(new FileInputStream("D:\\git\\itb-webform\\src\\test\\resources\\xml2\\invoice.xml"));
        System.out.println(doc2.getDocumentElement());
    }

    public static Document readXMLWithLineNumbers(InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            SAXParserFactory elementStack = SAXParserFactory.newInstance();
            parser = elementStack.newSAXParser();
            DocumentBuilderFactory textBuffer = DocumentBuilderFactory.newInstance();
            DocumentBuilder handler = textBuffer.newDocumentBuilder();
            doc = handler.newDocument();
        } catch (ParserConfigurationException var6) {
            throw new RuntimeException("Can\'t create SAX parser / DOM builder.", var6);
        }

        final Stack elementStack1 = new Stack();
        final StringBuilder textBuffer1 = new StringBuilder();
        DefaultHandler handler1 = new DefaultHandler() {
            private Locator locator;

            public void setDocumentLocator(Locator locator) {
                this.locator = locator;
            }

            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                this.addTextIfNeeded();
                Element el = doc.createElement(qName);

                for(int i = 0; i < attributes.getLength(); ++i) {
                    el.setAttribute(attributes.getQName(i), attributes.getValue(i));
                }

                el.setUserData(XMLUtils.LINE_NUMBER_KEY_NAME, String.valueOf(this.locator.getLineNumber()), (UserDataHandler)null);
                elementStack1.push(el);
            }

            public void endElement(String uri, String localName, String qName) {
                this.addTextIfNeeded();
                Element closedEl = (Element)elementStack1.pop();
                if(elementStack1.isEmpty()) {
                    doc.appendChild(closedEl);
                } else {
                    Element parentEl = (Element)elementStack1.peek();
                    parentEl.appendChild(closedEl);
                }

            }

            public void characters(char[] ch, int start, int length) throws SAXException {
                textBuffer1.append(ch, start, length);
            }

            private void addTextIfNeeded() {
                if(textBuffer1.length() > 0) {
                    Element el = (Element)elementStack1.peek();
                    Text textNode = doc.createTextNode(textBuffer1.toString());
                    el.appendChild(textNode);
                    textBuffer1.delete(0, textBuffer1.length());
                }

            }
        };
        parser.parse(is, handler1);
        return doc;
    }


}
