<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
    <ns prefix="cac" uri="urn:test:cac"/>
    <ns prefix="cbc" uri="urn:test:cbc"/>
    <pattern id="quantity-check">
        <rule context="cac:Line">
            <assert test="cbc:Quantity > 0" id="quantity-positive">Quantity must be greater than zero.</assert>
        </rule>
    </pattern>
</schema>
