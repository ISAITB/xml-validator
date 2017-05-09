<!-- Schematron rules generated automatically by Validex Generator Midran ltd -->
<!-- Abstract rules for model -->
<!-- Timestamp: 2017-02-14 10:09:43 +0100 -->
<pattern xmlns="http://purl.oclc.org/dsdl/schematron" abstract="true" id="model">
  <rule context="$Additional_supporting_documents ">
    <assert test="$BR-52" flag="fatal" id="BR-52">[BR-52]-Each additional supporting document shall contain a Supporting document identifier. </assert>
  </rule>
  <rule context="$Amount_due">
    <assert test="$BR-CO-25" flag="fatal" id="BR-CO-25">[BR-CO-25]-In case the Amount due for payment (BT-115) is positive, either the Payment due date (BT-9) or the Payment terms (BT-20) shall be present.</assert>
  </rule>
  <rule context="$Buyer_electronic_address">
    <assert test="$BR-63" flag="fatal" id="BR-63">[BR-63]-The Buyer electronic address shall have a Scheme identifier</assert>
  </rule>
  <rule context="$Buyer_postal_address">
    <assert test="$BR-11" flag="fatal" id="BR-11">[BR-11]-A Buyer postal address shall contain a Buyer country code</assert>
  </rule>
  <rule context="$Card_information ">
    <assert test="$BR-51" flag="fatal" id="BR-51">[BR-51]-The last 4 to 6 digits of the Payment card primary account number shall be present if payment card information is provided in the Invoice. </assert>
  </rule>
  <rule context="$Deliver_to_address">
    <assert test="$BR-57" flag="fatal" id="BR-57">[BR-57]-Each Deliver to address shall contain a Deliver to country code</assert>
  </rule>
  <rule context="$Document_level_allowances ">
    <assert test="$BR-31" flag="fatal" id="BR-31">[BR-31]-Each document level allowance shall have a Document level allowance amount. </assert>
    <assert test="$BR-32" flag="fatal" id="BR-32">[BR-32]-Each document level allowance shall have a Document level allowance VAT category code. </assert>
    <assert test="$BR-33" flag="fatal" id="BR-33">[BR-33]-Each document level allowance shall have a Document level allowance reason or a document level allowance reason code.</assert>
    <assert test="$BR-CO-21" flag="fatal" id="BR-CO-21">[BR-CO-21]-Each Document level allowance (BG-20) shall contain a Document level allowance reason or a Document level allowance reason code, or both.</assert>
  </rule>
  <rule context="$Document_level_charges ">
    <assert test="$BR-36" flag="fatal" id="BR-36">[BR-36]-Each document level charge shall have a Document level charge amount. </assert>
    <assert test="$BR-37" flag="fatal" id="BR-37">[BR-37]-Each document level charge shall have a Document level charge VAT category code. </assert>
    <assert test="$BR-38" flag="fatal" id="BR-38">[BR-38]-Each document level charge shall have a Document level charge reason or a Document level charge reason code.</assert>
    <assert test="$BR-CO-22" flag="fatal" id="BR-CO-22">[BR-CO-22]-Each Document level charge (BG-21) shall contain a Document level charge reason or a Document level charge reason code, or both.</assert>
  </rule>
  <rule context="$Document_totals ">
    <assert test="$BR-12" flag="fatal" id="BR-12">[BR-12]-An Invoice shall have the Sum of Invoice line net amount. </assert>
    <assert test="$BR-13" flag="fatal" id="BR-13">[BR-13]-An Invoice shall have the Invoice total amount without VAT. </assert>
    <assert test="$BR-14" flag="fatal" id="BR-14">[BR-14]-An Invoice shall have the Invoice total amount with VAT. </assert>
    <assert test="$BR-15" flag="fatal" id="BR-15">[BR-15]-An Invoice shall have the Amount due for payment. </assert>
    <assert test="$BR-CO-10" flag="fatal" id="BR-CO-10">[BR-CO-10]-Sum of Invoice line net amount = Σ Invoice line net amount. </assert>
    <assert test="$BR-CO-11" flag="fatal" id="BR-CO-11">[BR-CO-11]-Sum of allowances on document level = Σ Document level allowance amount. </assert>
    <assert test="$BR-CO-12" flag="fatal" id="BR-CO-12">[BR-CO-12]-Sum of charges on document level = Σ Document level charge amount. </assert>
    <assert test="$BR-CO-13" flag="fatal" id="BR-CO-13">[BR-CO-13]-Invoice total amount without VAT = Σ Invoice line net amount - Sum of allowances on document level + Sum of charges on document level. </assert>
    <assert test="$BR-CO-16" flag="fatal" id="BR-CO-16">[BR-CO-16]-Amount due for payment = Invoice total VAT amount-Paid amount + Rounding amount (BT-114).</assert>
  </rule>
  <rule context="$Invoice ">
    <assert test="$BR-01" flag="fatal" id="BR-01">[BR-01]-An Invoice shall have a Specification identification. </assert>
    <assert test="$BR-02" flag="fatal" id="BR-02">[BR-02]-An Invoice shall have an Invoice number. </assert>
    <assert test="$BR-03" flag="fatal" id="BR-03">[BR-03]-An Invoice shall have an Invoice issue date. </assert>
    <assert test="$BR-04" flag="fatal" id="BR-04">[BR-04]-An Invoice shall have an Invoice type code. </assert>
    <assert test="$BR-05" flag="fatal" id="BR-05">[BR-05]-An Invoice shall have an Invoice currency code. </assert>
    <assert test="$BR-06" flag="fatal" id="BR-06">[BR-06]-An Invoice shall contain Seller name. </assert>
    <assert test="$BR-07" flag="fatal" id="BR-07">[BR-07]-An Invoice shall contain Buyer name. </assert>
    <assert test="$BR-08" flag="fatal" id="BR-08">[BR-08]-An Invoice shall contain the Seller postal address. </assert>
    <assert test="$BR-10" flag="fatal" id="BR-10">[BR-10]-An Invoice shall contain the Buyer postal address. </assert>
    <assert test="$BR-16" flag="fatal" id="BR-16">[BR-16]-An Invoice shall have at least one Invoice line. </assert>
    <assert test="$BR-53" flag="fatal" id="BR-53">[BR-53]-If the VAT accounting currency code is present, then the Invoice total VAT amount in accounting currency shall be provided. </assert>
    <assert test="$BR-AE-01" flag="fatal" id="BR-AE-01">[BR-AE-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “Reverse charge” shall contain in the VAT breakdown (BG-23) exactly one VAT category code (BT-118) equal with "Reverse charge".</assert>
    <assert test="$BR-AE-02" flag="fatal" id="BR-AE-02">[BR-AE-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “Reverse charge” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63) and the Buyer VAT identifier or the Buter tax registration identifier.</assert>
    <assert test="$BR-AE-03" flag="fatal" id="BR-AE-03">[BR-AE-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “Reverse charge” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63) and the Buyer VAT identifier or the Buyer tax registration identifier.</assert>
    <assert test="$BR-AE-04" flag="fatal" id="BR-AE-04">[BR-AE-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “Reverse charge” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63) and the Buyer VAT identifier (BT-48).</assert>
    <assert test="$BR-CO-03" flag="fatal" id="BR-CO-03">[BR-CO-03]-Value added tax point date and Value added tax point date code are mutually exclusive. </assert>
    <assert test="$BR-CO-15" flag="fatal" id="BR-CO-15">[BR-CO-15]-Invoice total amount with VAT = Invoice total amount without VAT + Invoice total VAT amount. </assert>
    <assert test="$BR-CO-18" flag="fatal" id="BR-CO-18">[BR-CO-18]-An invoice shall at least have one VAT breakdown group (BG-23)</assert>
    <assert test="$BR-E-01" flag="fatal" id="BR-E-01">[BR-E-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “Exempt from VAT” shall contain exactly one a VAT breakdown (BG-23) with VAT category code (BT-118) equal to "Exempt from VAT". </assert>
    <assert test="$BR-E-02" flag="fatal" id="BR-E-02">[BR-E-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “Exempt from VAT” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-E-03" flag="fatal" id="BR-E-03">[BR-E-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “Exempt from VAT” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-E-04" flag="fatal" id="BR-E-04">[BR-E-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “Exempt from VAT” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-G-01" flag="fatal" id="BR-G-01">[BR-G-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “Export outside the EU” shall contain in the VAT breakdown (BG-23) exactly one VAT category code (BT-118) equal with "Export outside the EU".</assert>
    <assert test="$BR-G-02" flag="fatal" id="BR-G-02">[BR-G-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “Export outside the EU” shall contain the Sellers VAT Identifier (BT-31) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-G-03" flag="fatal" id="BR-G-03">[BR-G-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “Export outside the EU” shall contain the Sellers VAT Identifier (BT-31) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-G-04" flag="fatal" id="BR-G-04">[BR-G-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “Export outside the EU” shall contain the Sellers VAT Identifier (BT-31) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-IC-01" flag="fatal" id="BR-IC-01">[BR-IC-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “Intra-community supply” shall contain in the VAT breakdown (BG-23) exactly one VAT category code (BT-118) equal with "Intra-community supply".</assert>
    <assert test="$BR-IC-02" flag="fatal" id="BR-IC-02">[BR-IC-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “Intra-community supply” shall contain the Sellers VAT Identifier (BT-31) or the Seller tax representative VAT identifier (BT-63) and the Buyer VAT identifier.</assert>
    <assert test="$BR-IC-03" flag="fatal" id="BR-IC-03">[BR-IC-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “Intra-community supply” shall contain the Sellers VAT Identifier (BT-31) or the Seller tax representative VAT identifier (BT-63) and the Buyer VAT identifier.</assert>
    <assert test="$BR-IC-04" flag="fatal" id="BR-IC-04">[BR-IC-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “Intra-community supply” shall contain the Sellers VAT Identifier (BT-31) or the Seller tax representative VAT identifier (BT-63) and the Buyer VAT identifier (BT-48).</assert>
    <assert test="$BR-IC-11" flag="fatal" id="BR-IC-11">[BR-IC-11]-In an Invoice with a VAT breakdown (BG-23) where the VAT category code (BT-118) is "Intra-community supply" the actual delivery date (BT-72) or the delivery period (BG-14) shall not be blank.</assert>
    <assert test="$BR-IC-12" flag="fatal" id="BR-IC-12">[BR-IC-12]-In an Invoice with a VAT breakdown (BG-23) where the VAT category code (BT-118) is "Intra-community supply" the deliver to country code (BT-80) shall not be blank.</assert>
    <assert test="$BR-IG-01" flag="fatal" id="BR-IG-01">[BR-IG-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “IGIC” shall contain in the VAT breakdown (BG-23) at least one VAT category code (BT-118) equal with "IGIC".</assert>
    <assert test="$BR-IG-02" flag="fatal" id="BR-IG-02">[BR-IG-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “IGIC” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-IG-03" flag="fatal" id="BR-IG-03">[BR-IG-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “IGIC” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-IG-04" flag="fatal" id="BR-IG-04">[BR-IG-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “IGIC” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-IP-01" flag="fatal" id="BR-IP-01">[BR-IP-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “IPSI” shall contain in the VAT breakdown (BG-23) at least one VAT category code (BT-118) equal with "IPSI".</assert>
    <assert test="$BR-IP-02" flag="fatal" id="BR-IP-02">[BR-IP-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “IPSI” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-IP-03" flag="fatal" id="BR-IP-03">[BR-IP-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “IPSI” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-IP-04" flag="fatal" id="BR-IP-04">[BR-IP-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “IPSI” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-O-01" flag="fatal" id="BR-O-01">[BR-O-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “Not subject to VAT” shall contain exactly one VAT breakdown group (BG-23) with category code (BT-118) equal to “Not subject to VAT”.</assert>
    <assert test="$BR-O-02" flag="fatal" id="BR-O-02">[BR-O-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “Not subject to VAT” shall not contain the Seller's VAT identifier (BT-31), the Seller tax representative VAT identifier (BT-63) or the Buyer's VAT identifier (BT-46).</assert>
    <assert test="$BR-O-03" flag="fatal" id="BR-O-03">[BR-O-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “Not subject to VAT” shall not contain the Seller's VAT identifier (BT-31), the Seller tax representative VAT identifier (BT-63) or the Buyer's VAT identifier (BT-48).</assert>
    <assert test="$BR-O-04" flag="fatal" id="BR-O-04">[BR-O-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “Not subject to VAT” shall not contain the Seller's VAT identifier (BT-31), the Seller tax representative VAT identifier (BT-63) or the Buyer's VAT identifier (BT-48).</assert>
    <assert test="$BR-O-11" flag="fatal" id="BR-O-11">[BR-O-11]-An Invoice that contains a VAT breakdown group (BG-23) with a VAT category code (BT-118) as "Not subject to VAT" shall not contain other VAT breakdown groups (BG-23).</assert>
    <assert test="$BR-O-12" flag="fatal" id="BR-O-12">[BR-O-12]-An Invoice that contains a VAT breakdown group (BG-23) with a VAT category code (BT-118) "Not subject to VAT"  shall not contain lines where the Invoiced item VAT category code (BT-151) is not "Not subject to VAT".</assert>
    <assert test="$BR-O-13" flag="fatal" id="BR-O-13">[BR-O-13]-An Invoice that contains a VAT breakdown group (BG-23) with a VAT category code (BT-118) "Not subject to VAT" shall not contain a document level allowances group (BG-20) where document level allowance VAT category code (BT-95) is not "Not subject to VAT".</assert>
    <assert test="$BR-O-14" flag="fatal" id="BR-O-14">[BR-O-14]-An Invoice that contains a VAT breakdown group (BG-23) with a VAT category code (BT-118) "Not subject to VAT" shall not contain a document Level charge group (BG-21) where document level charge VAT category code (BT-102) is not "Not subject to VAT".</assert>
    <assert test="$BR-S-01" flag="fatal" id="BR-S-01">[BR-S-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “Standard rated” shall contain in the VAT breakdown (BG-23) at least one VAT category code (BT-118) equal with "Standard rated".</assert>
    <assert test="$BR-S-02" flag="fatal" id="BR-S-02">[BR-S-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “Standard rated” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-S-03" flag="fatal" id="BR-S-03">[BR-S-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “Standard rated” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-S-04" flag="fatal" id="BR-S-04">[BR-S-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “Standard rated” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-Z-01" flag="fatal" id="BR-Z-01">[BR-Z-01]-An Invoice that contains a line, a document level allowance or a document level  charge where the Invoiced item VAT category code (BT-151, BT-95 or BT-102) is “Zero rated” shall contain in the VAT breakdown (BG-23) exactly one VAT category code (BT-118) equal with "Zero rated".</assert>
    <assert test="$BR-Z-02" flag="fatal" id="BR-Z-02">[BR-Z-02]-An Invoice that contains a line where the Invoiced item VAT category code (BT-151) is “Zero rated” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-Z-03" flag="fatal" id="BR-Z-03">[BR-Z-03]-An Invoice that contains a document level allowance where the Invoiced item VAT category code (BT-95) is “Zero rated” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
    <assert test="$BR-Z-04" flag="fatal" id="BR-Z-04">[BR-Z-04]-An Invoice that contains a document level charge where the Invoiced item VAT category code (BT-102) is “Zero rated” shall contain the Sellers VAT Identifier (BT-31), the Seller Tax registration identifier (BT-32) or the Seller tax representative VAT identifier (BT-63).</assert>
  </rule>
  <rule context="$Invoice_Line ">
    <assert test="$BR-21" flag="fatal" id="BR-21">[BR-21]-Each Invoice line shall have an Invoice line identifier. </assert>
    <assert test="$BR-22" flag="fatal" id="BR-22">[BR-22]-Each Invoice line shall have an Invoiced quantity. </assert>
    <assert test="$BR-23" flag="fatal" id="BR-23">[BR-23]-An invoiced quantity shall have an Invoice quantity unit of measure. </assert>
    <assert test="$BR-24" flag="fatal" id="BR-24">[BR-24]-Each Invoice line shall have an Invoice line net amount. </assert>
    <assert test="$BR-25" flag="fatal" id="BR-25">[BR-25]-Each Invoice line shall contain the Item name. </assert>
    <assert test="$BR-26" flag="fatal" id="BR-26">[BR-26]-Each Invoice line shall contain the Item net price. </assert>
    <assert test="$BR-27" flag="fatal" id="BR-27">[BR-27]-The item net price shall NOT be negative. </assert>
    <assert test="$BR-28" flag="fatal" id="BR-28">[BR-28]-Invoice line item gross price shall NOT be negative. </assert>
    <assert test="$BR-CO-04" flag="fatal" id="BR-CO-04">[BR-CO-04]-Each Invoice line shall be categorized with an Invoiced item VAT category code. </assert>
  </rule>
  <rule context="$Invoice_line_allowances ">
    <assert test="$BR-41" flag="fatal" id="BR-41">[BR-41]-Each Invoice line allowance shall have an Invoice line allowance amount. </assert>
    <assert test="$BR-42" flag="fatal" id="BR-42">[BR-42]-Each Invoice line allowance shall have an Invoice line allowance reason or an Invoice line allowance reason code.</assert>
    <assert test="$BR-CO-23" flag="fatal" id="BR-CO-23">[BR-CO-23]-Each Invoice line allowance (BG-27) shall contain an Invoice line allowance reason or an Invoice line allowance reason code, or both.</assert>
  </rule>
  <rule context="$Invoice_line_charges ">
    <assert test="$BR-43" flag="fatal" id="BR-43">[BR-43]-Each Invoice line charge shall have an Invoice line charge amount. </assert>
    <assert test="$BR-44" flag="fatal" id="BR-44">[BR-44]-Each Invoice line charge shall have an Invoice line charge reason or an invoice line allowance reason code. </assert>
    <assert test="$BR-CO-24" flag="fatal" id="BR-CO-24">[BR-CO-24]-Each Invoice line charge (BG-28) shall contain an Invoice line charge reason or an Invoice line charge reason code, or both.</assert>
  </rule>
  <rule context="$Invoice_Line_Period ">
    <assert test="$BR-30" flag="fatal" id="BR-30">[BR-30]-If both Invoice line period start date and Invoice line period end date are given then the Invoice line period end date shall be later or equal to the Invoice line period start date.</assert>
    <assert test="$BR-CO-20" flag="fatal" id="BR-CO-20">[BR-CO-20]-If Invoice line period (BG-26) is used, the Invoice line period start date or the Invoice line period end date shall be filled, or both.</assert>
  </rule>
  <rule context="$Invoice_Period ">
    <assert test="$BR-29" flag="fatal" id="BR-29">[BR-29]-If both invoicing period start date and invoicing period end date are given then the invoicing period end date shall be later or equal to the invoicing period start date.</assert>
    <assert test="$BR-CO-19" flag="fatal" id="BR-CO-19">[BR-CO-19]-If Delivery or invoice period (BG-14) is used, the Delivery period start date or the Delivery period end date shall be filled, or both.</assert>
  </rule>
  <rule context="$Item_attributes ">
    <assert test="$BR-54" flag="fatal" id="BR-54">[BR-54]-Each Item attribute shall contain an Item attribute name and an Item attribute value. </assert>
  </rule>
  <rule context="$Item_classification_identifier">
    <assert test="$BR-65" flag="fatal" id="BR-65">[BR-65]-The Item classification identifier shall have a Scheme identifier</assert>
  </rule>
  <rule context="$Item_standard_identifier">
    <assert test="$BR-64" flag="fatal" id="BR-64">[BR-64]-The Item standard identifier shall have a Scheme identifier</assert>
  </rule>
  <rule context="$Payee">
    <assert test="$BR-17" flag="fatal" id="BR-17">[BR-17]-The Payee name shall be provided in the Invoice, if the Payee is different from the Seller. </assert>
  </rule>
  <rule context="$Payee_Financial_Account">
    <assert test="$BR-50" flag="fatal" id="BR-50">[BR-50]-A payment account identifier shall be present if Credit Transfer identification information is provided in the Invoice. </assert>
  </rule>
  <rule context="$Payment_instructions ">
    <assert test="$BR-49" flag="fatal" id="BR-49">[BR-49]-A payment instruction shall specify the Payment means type code. </assert>
    <assert test="$BR-61" flag="fatal" id="BR-61">[BR-61]-If the Payment means type is SEPA credit transfer, Local credit transfer or Non-SEPA international credit transfer, the Payment account identifier shall be present.</assert>
  </rule>
  <rule context="$Preceding_Invoice">
    <assert test="$BR-55" flag="fatal" id="BR-55">[BR-55]-Each Preceding invoice reference shall contain a preceding invoice number.</assert>
  </rule>
  <rule context="$Seller">
    <assert test="$BR-CO-26" flag="fatal" id="BR-CO-26">[BR-CO-26]-In order for the Buyer to automatically identify a supplier, either the Seller identifier (BT-29), the Seller legal registration identifier (BT-30) or the Seller VAT identifier (BT-31) shall be present.</assert>
  </rule>
  <rule context="$Seller_electronic_address">
    <assert test="$BR-62" flag="fatal" id="BR-62">[BR-62]-The Seller electronic address shall have a Scheme identifier</assert>
  </rule>
  <rule context="$Seller_postal_address">
    <assert test="$BR-09" flag="fatal" id="BR-09">[BR-09]-A Seller postal address shall contain a Seller country code</assert>
  </rule>
  <rule context="$Tax_Representative">
    <assert test="$BR-18" flag="fatal" id="BR-18">[BR-18]-The Seller tax representative name shall be provided in the Invoice, if the Seller has a tax representative party. </assert>
    <assert test="$BR-19" flag="fatal" id="BR-19">[BR-19]-The Seller tax representative postal address shall be provided in the Invoice, if the Seller has a tax representative party. </assert>
    <assert test="$BR-56" flag="fatal" id="BR-56">[BR-56]-Each Seller tax representative shall have a Seller tax representative VAT identifier</assert>
  </rule>
  <rule context="$Tax_Representative_postal_address">
    <assert test="$BR-20" flag="fatal" id="BR-20">[BR-20]-The Seller tax representative postal address shall contain a Tax representative country code, if the Seller has a tax representative party.</assert>
  </rule>
  <rule context="$Tax_Total">
    <assert test="$BR-CO-14" flag="fatal" id="BR-CO-14">[BR-CO-14]-Invoice total VAT amount = Σ VAT category tax amount. </assert>
  </rule>
  <rule context="$VAT_breakdown ">
    <assert test="$BR-45" flag="fatal" id="BR-45">[BR-45]-Each VAT breakdown shall have a VAT category taxable amount. </assert>
    <assert test="$BR-46" flag="fatal" id="BR-46">[BR-46]-Each VAT breakdown shall have a VAT category tax amount. </assert>
    <assert test="$BR-47" flag="fatal" id="BR-47">[BR-47]-Each VAT breakdown shall be defined through a VAT category code. </assert>
    <assert test="$BR-48" flag="fatal" id="BR-48">[BR-48]-Each VAT breakdown shall have a VAT category rate, except if the Invoice is not subject to VAT. </assert>
    <assert test="$BR-CO-17" flag="fatal" id="BR-CO-17">[BR-CO-17]-VAT category tax amount = VAT category taxable amount x (VAT category rate / 100), rounded to two decimals. </assert>
  </rule>
  <rule context="$VAT_identifiers ">
    <assert test="$BR-CO-09" flag="fatal" id="BR-CO-09">[BR-CO-09]-The Seller VAT identifier, Seller tax representative VAT identifier, Buyer VAT identifier shall have a prefix in accordance with ISO code ISO 3166-1 alpha-2 by which the country of issue may be identified. Nevertheless, Greece may use the prefix ‘EL’. </assert>
  </rule>
  <rule context="$VATAE">
    <assert test="$BR-AE-08" flag="fatal" id="BR-AE-08">[BR-AE-08]-In a VAT breakdown (BG-23) where VAT category code (BT-118) is "Reverse charge" the VAT category taxable amount (BT-116) shall equal the sum of Invoice line net amounts (BT-131) minus document level allowance amounts (BT-92) plus document level charge amounts (BT-99) where the VAT category codes (BT-151, BT-95, BT-102) are “Reverse charge".</assert>
    <assert test="$BR-AE-09" flag="fatal" id="BR-AE-09">[BR-AE-09]-The VAT category tax amount (BT-117) in a VAT breakdown (BG-23) where the VAT category code (BT-118) is “Reverse charge” shall be 0 (zero).</assert>
    <assert test="$BR-AE-10" flag="fatal" id="BR-AE-10">[BR-AE-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "Reverse charge" shall have a VAT Exemption reason code (BT-121), meaning "Reverse charge" or the VAT Exemption reason text (BT-120) "Reverse charge" (or the equivalent standard text in another language).</assert>
  </rule>
  <rule context="$VATAE_Allowance">
    <assert test="$BR-AE-06" flag="fatal" id="BR-AE-06">[BR-AE-06]-In a document level allowance where the Invoice item VAT category code (BT-95) is "Reverse charge" the Invoiced item VAT rate (BT-96) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATAE_Charge">
    <assert test="$BR-AE-07" flag="fatal" id="BR-AE-07">[BR-AE-07]-In a document level charge  where the Invoice item VAT category code (BT-102) is "Reverse charge" the Invoiced item VAT rate (BT-103) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATAE_Line">
    <assert test="$BR-AE-05" flag="fatal" id="BR-AE-05">[BR-AE-05]-In an Invoice line where the Invoice item VAT category code (BT-151) is "Reverse charge" the Invoiced item VAT rate (BT-152) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATE">
    <assert test="$BR-E-08" flag="fatal" id="BR-E-08">[BR-E-08]-In a VAT breakdown (BG-23) where VAT category code (BT-118) is "Exempt from VAT" the VAT category taxable amount (BT-116) shall equal the sum of Invoice line net amounts (BT-131) minus document level allowance amounts (BT-92) plus document level charge amounts (BT-99) where the VAT category codes (BT-151, BT-95, BT-102) are “Exempt from VAT".</assert>
    <assert test="$BR-E-09" flag="fatal" id="BR-E-09">[BR-E-09]-The VAT category tax amount (BT-117) In a VAT breakdown (BG-23) where the VAT category code (BT-118) equals "Exempt from VAT" shall equal 0 (zero).</assert>
    <assert test="$BR-E-10" flag="fatal" id="BR-E-10">[BR-E-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "Exempt from VAT" shall have a VAT Exemption reason code (BT-121) or a VAT Exemption reason text (BT-120).</assert>
  </rule>
  <rule context="$VATE_Allowance">
    <assert test="$BR-E-06" flag="fatal" id="BR-E-06">[BR-E-06]-In a document level allowance where the Invoice item VAT category code (BT-95) is "Exempt from VAT", the Invoiced item VAT rate (BT-96) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATE_Charge">
    <assert test="$BR-E-07" flag="fatal" id="BR-E-07">[BR-E-07]-In a document level charge  where the Invoice item VAT category code (BT-102) is "Exempt from VAT", the Invoiced item VAT rate (BT-103) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATE_Line">
    <assert test="$BR-E-05" flag="fatal" id="BR-E-05">[BR-E-05]-In an Invoice line where the Invoice item VAT category code (BT-151) is "Exempt from VAT", the Invoiced item VAT rate (BT-152) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATG">
    <assert test="$BR-G-08" flag="fatal" id="BR-G-08">[BR-G-08]-In a VAT breakdown (BG-23) where VAT category code (BT-118) is "Export outside the EU" the VAT category taxable amount (BT-116) shall equal the sum of Invoice line net amounts (BT-131) minus document level allowance amounts (BT-92) plus document level charge amounts (BT-99) where the VAT category codes (BT-151, BT-95, BT-102) are “Export outside the EU".</assert>
    <assert test="$BR-G-09" flag="fatal" id="BR-G-09">[BR-G-09]-The VAT category tax amount (BT-117) in a VAT breakdown (BG-23) where the VAT category code (BT-118) is “Export outside the EU” shall be 0 (zero).</assert>
    <assert test="$BR-G-10" flag="fatal" id="BR-G-10">[BR-G-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "Export outside the EU" shall have a VAT Exemption reason code (BT-121), meaning "Export outside the EU" or the VAT Exemption reason text (BT-120) "Export outside the EU" (or the equivalent standard text in another language).</assert>
  </rule>
  <rule context="$VATG_Allowance">
    <assert test="$BR-G-06" flag="fatal" id="BR-G-06">[BR-G-06]-In a document level allowance where the Invoice item VAT category code (BT-95) is "Export outside the EU" the Invoiced item VAT rate (BT-96) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATG_Charge">
    <assert test="$BR-G-07" flag="fatal" id="BR-G-07">[BR-G-07]-In a document level charge  where the Invoice item VAT category code (BT-102) is "Export outside the EU" the Invoiced item VAT rate (BT-103) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATG_Line">
    <assert test="$BR-G-05" flag="fatal" id="BR-G-05">[BR-G-05]-In an Invoice line where the Invoice item VAT category code (BT-151) is "Export outside the EU" the Invoiced item VAT rate (BT-152) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATIC">
    <assert test="$BR-IC-08" flag="fatal" id="BR-IC-08">[BR-IC-08]-In a VAT breakdown (BG-23) where VAT category code (BT-118) is "Intra-community supply" the VAT category taxable amount (BT-116) shall equal the sum of Invoice line net amounts (BT-131) minus document level allowance amounts (BT-92) plus document level charge amounts (BT-99) where the VAT category codes (BT-151, BT-95, BT-102) are “Intra-community supply".</assert>
    <assert test="$BR-IC-09" flag="fatal" id="BR-IC-09">[BR-IC-09]-The VAT category tax amount (BT-117) in a VAT breakdown (BG-23) where the VAT category code (BT-118) is “Intra-community supply” shall be 0 (zero).</assert>
    <assert test="$BR-IC-10" flag="fatal" id="BR-IC-10">[BR-IC-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "Intra-community supply" shall have a VAT Exemption reason code (BT-121), meaning "Intra-community supply" or the VAT Exemption reason text (BT-120) "Intra-community supply" (or the equivalent standard text in another language).</assert>
  </rule>
  <rule context="$VATIC_Allowance">
    <assert test="$BR-IC-06" flag="fatal" id="BR-IC-06">[BR-IC-06]-In a document level allowance where the Invoice item VAT category code (BT-95) is "Intra-community supply" the invoiced item VAT rate (BT-96) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATIC_Charge">
    <assert test="$BR-IC-07" flag="fatal" id="BR-IC-07">[BR-IC-07]-In a document level charge  where the Invoice item VAT category code (BT-102) is "Intra-community supply" the invoiced item VAT rate (BT-103) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATIC_Line">
    <assert test="$BR-IC-05" flag="fatal" id="BR-IC-05">[BR-IC-05]-In an Invoice line where the Invoice item VAT category code (BT-151) is "Intra-community supply" the invoiced item VAT rate (BT-152) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATIG">
    <assert test="$BR-IG-08" flag="fatal" id="BR-IG-08">[BR-IG-08]-For each different value of VAT category rate (BT-119) where the VAT category code (BT-118) is "IGIC", the VAT category taxable amount (BT-116) in a VAT breakdown (BG-23) shall equal the sum of Invoice line net amounts (BT-131) plus the sum of document level charge amounts (BT-99) minus the sum of document level allowance amounts (BT-92) where the VAT category code (BT-151, BT-102, BT-95) is “IGIC” and the VAT rate (BT-152, BT-103, BT-96) equals the VAT category rate (BT-119).</assert>
    <assert test="$BR-IG-09" flag="fatal" id="BR-IG-09">[BR-IG-09]-The VAT category tax amount (BT-117) in VAT breakdown (BG-23) where VAT category code (BT-118) is "IGIC" shall equal the VAT category taxable amount (BT-116) multiplied by the VAT category rate (BT-119).</assert>
    <assert test="$BR-IG-10" flag="fatal" id="BR-IG-10">[BR-IG-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "IGIC" shall not have a VAT Exemption reason code (BT-121) or VAT Exemption reason text (BT-120).</assert>
  </rule>
  <rule context="$VATIG_Allowance">
    <assert test="$BR-IG-06" flag="fatal" id="BR-IG-06">[BR-IG-06]-In a document level allowance where the Invoice item VAT category code (BT-95) is "IGIC" the invoiced item VAT rate (BT-96) shall be 0 (zero) or greater than zero.</assert>
  </rule>
  <rule context="$VATIG_Charge">
    <assert test="$BR-IG-07" flag="fatal" id="BR-IG-07">[BR-IG-07]-In a document level charge  where the Invoice item VAT category code (BT-102) is "IGIC" the invoiced item VAT rate (BT-103) shall be 0 (zero) or greater than zero.</assert>
  </rule>
  <rule context="$VATIG_Line">
    <assert test="$BR-IG-05" flag="fatal" id="BR-IG-05">[BR-IG-05]-In an Invoice line where the Invoice item VAT category code (BT-151) is "IGIC" the invoiced item VAT rate (BT-152) shall be 0 (zero) or greater than zero.</assert>
  </rule>
  <rule context="$VATIP">
    <assert test="$BR-IP-08" flag="fatal" id="BR-IP-08">[BR-IP-08]-For each different value of VAT category rate (BT-119) where the VAT category code (BT-118) is "IPSI", the VAT category taxable amount (BT-116) in a VAT breakdown (BG-23) shall equal the sum of Invoice line net amounts (BT-131) plus the sum of document level charge amounts (BT-99) minus the sum of document level allowance amounts (BT-92) where the VAT category code (BT-151, BT-102, BT-95) is “IPSI” and the VAT rate (BT-152, BT-103, BT-96) equals the VAT category rate (BT-119).</assert>
    <assert test="$BR-IP-09" flag="fatal" id="BR-IP-09">[BR-IP-09]-The VAT category tax amount (BT-117) in VAT breakdown (BG-23) where VAT category code (BT-118) is "IPSI" shall equal the VAT category taxable amount (BT-116) multiplied by the VAT category rate (BT-119).</assert>
    <assert test="$BR-IP-10" flag="fatal" id="BR-IP-10">[BR-IP-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "IPSI" shall not have a VAT Exemption reason code (BT-121) or VAT Exemption reason text (BT-120).</assert>
  </rule>
  <rule context="$VATIP_Allowance">
    <assert test="$BR-IP-06" flag="fatal" id="BR-IP-06">[BR-IP-06]-In a document level allowance where the Invoice item VAT category code (BT-95) is "IPSI" the invoiced item VAT rate (BT-96) shall be 0 (zero) or greater than zero.</assert>
  </rule>
  <rule context="$VATIP_Charge">
    <assert test="$BR-IP-07" flag="fatal" id="BR-IP-07">[BR-IP-07]-In a document level charge  where the Invoice item VAT category code (BT-102) is "IPSI" the invoiced item VAT rate (BT-103) shall be 0 (zero) or greater than zero.</assert>
  </rule>
  <rule context="$VATIP_Line">
    <assert test="$BR-IP-05" flag="fatal" id="BR-IP-05">[BR-IP-05]-In an Invoice line where the Invoice item VAT category code (BT-151) is "IPSI" the invoiced item VAT rate (BT-152) shall be 0 (zero) or greater than zero.</assert>
  </rule>
  <rule context="$VATO">
    <assert test="$BR-O-08" flag="fatal" id="BR-O-08">[BR-O-08]-In a VAT breakdown (BG-23) where VAT category code (BT-118) is "Not subject to VAT" the VAT category taxable amount (BT-116) shall equal the sum of Invoice line net amounts (BT-131) minus document level allowance amounts (BT-92) plus document level charge amounts (BT-99) where the VAT category codes (BT-151, BT-95, BT-102) are “Not subject to VAT".</assert>
    <assert test="$BR-O-09" flag="fatal" id="BR-O-09">[BR-O-09]-The VAT category tax amount (BT-117) in a VAT breakdown (BG-23) where the VAT category code (BT-118) is “Not subject to VAT” shall be 0 (zero).</assert>
    <assert test="$BR-O-10" flag="fatal" id="BR-O-10">[BR-O-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "Not subject to VAT" shall have a VAT Exemption reason code (BT-121), meaning "Not subject to VAT" or a VAT Exemption reason text (BT-120) "Not subject to VAT" (or the equivalent standard text in another language).</assert>
  </rule>
  <rule context="$VATO_Allowance">
    <assert test="$BR-O-06" flag="fatal" id="BR-O-06">[BR-O-06]-A document level  allowance  where VAT category code (BT-95) is "Not subject to VAT" shall not contain an invoiced item VAT rate (BT-96).</assert>
  </rule>
  <rule context="$VATO_Charge">
    <assert test="$BR-O-07" flag="fatal" id="BR-O-07">[BR-O-07]-A document level  charge  where VAT category code (BT-102) is "Not subject to VAT" shall not contain an invoiced item VAT rate (BT-103).</assert>
  </rule>
  <rule context="$VATO_Line">
    <assert test="$BR-O-05" flag="fatal" id="BR-O-05">[BR-O-05]-An Invoice line where VAT category code (BT-151) is "Not subject to VAT" shall not contain an invoiced item VAT rate (BT-152).</assert>
  </rule>
  <rule context="$VATS">
    <assert test="$BR-S-08" flag="fatal" id="BR-S-08">[BR-S-08]-For each different value of VAT category rate (BT-119) where the VAT category code (BT-118) is "Standard rated", the VAT category taxable amount (BT-116) in a VAT breakdown (BG-23) shall equal the sum of Invoice line net amounts (BT-131) plus the sum of document level charge amounts (BT-99) minus the sum of document level allowance amounts (BT-92) where the VAT category code (BT-151, BT-102, BT-95) is “Standard rated” and the VAT rate (BT-152, BT-103, BT-96) equals the VAT category rate (BT-119).</assert>
    <assert test="$BR-S-09" flag="fatal" id="BR-S-09">[BR-S-09]-The VAT category tax amount (BT-117) in VAT breakdown (BG-23) where VAT category code (BT-118) is "Standard rated" shall equal the VAT category taxable amount (BT-116) multiplied by the VAT category rate (BT-119).</assert>
    <assert test="$BR-S-10" flag="fatal" id="BR-S-10">[BR-S-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "Standard rate" shall not have a VAT Exemption reason code (BT-121) or VAT Exemption reason text (BT-120).</assert>
  </rule>
  <rule context="$VATS_Allowance">
    <assert test="$BR-S-06" flag="fatal" id="BR-S-06">[BR-S-06]-In a document level allowance where the Invoice item VAT category code (BT-95) is "Standard rated" the Invoiced item VAT rate (BT-96) shall be greater than zero.</assert>
  </rule>
  <rule context="$VATS_Charge">
    <assert test="$BR-S-07" flag="fatal" id="BR-S-07">[BR-S-07]-In a document level charge where the Invoice item VAT category code (BT-102) is "Standard rated" the Invoiced item VAT rate (BT-103) shall be greater than zero.</assert>
  </rule>
  <rule context="$VATS_Line">
    <assert test="$BR-S-05" flag="fatal" id="BR-S-05">[BR-S-05]-In an Invoice line where the Invoice item VAT category code (BT-151) is "Standard rated" the Invoiced item VAT rate (BT-152) shall be greater than zero.</assert>
  </rule>
  <rule context="$VATZ">
    <assert test="$BR-Z-08" flag="fatal" id="BR-Z-08">[BR-Z-08]-In a VAT breakdown (BG-23) where VAT category code (BT-118) is "Zero rated" the VAT category taxable amount (BT-116) shall equal the sum of Invoice line net amounts (BT-131) minus document level allowance amounts (BT-92) plus document level charge amounts (BT-99) where the VAT category codes (BT-151, BT-95, BT-102) are “Zero rated".</assert>
    <assert test="$BR-Z-09" flag="fatal" id="BR-Z-09">[BR-Z-09]-The VAT category tax amount (BT-117) in a VAT breakdown (BG-23) where VAT category code (BT-118) is "Zero rated" shall equal 0 (zero).</assert>
    <assert test="$BR-Z-10" flag="fatal" id="BR-Z-10">[BR-Z-10]-A VAT Breakdown (BG-23) with VAT Category code (BT-118) "Zero rated" shall not have a VAT Exemption reason code (BT-121) or VAT Exemption reason text (BT-120).</assert>
  </rule>
  <rule context="$VATZ_Allowance">
    <assert test="$BR-Z-06" flag="fatal" id="BR-Z-06">[BR-Z-06]-In a document level allowance where the Invoice item VAT category code (BT-95) is "Zero rated" the Invoiced item VAT rate (BT-96) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATZ_Charge">
    <assert test="$BR-Z-07" flag="fatal" id="BR-Z-07">[BR-Z-07]-In a document level charge where the Invoice item VAT category code (BT-102) is "Zero rated" the Invoiced item VAT rate (BT-103) shall be 0 (zero).</assert>
  </rule>
  <rule context="$VATZ_Line">
    <assert test="$BR-Z-05" flag="fatal" id="BR-Z-05">[BR-Z-05]-In an Invoice line where the Invoice item VAT category code (BT-151) is "Zero rated" the Invoiced item VAT rate (BT-152) shall be 0 (zero).</assert>
  </rule>
  <rule context="$Note">
    <assert test="$BR-CL-19" flag="fatal" id="BR-CL-19">[BR-CL-19]-Invoiced note subject code SHOULD be coded using UNCL4451</assert>
  </rule>
</pattern>
