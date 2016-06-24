# Introduction

Application used for the validation of XML documents by means of:

* A GITB-compliance validation service (SOAP web service).
* A simple web form that can receive an XML file.
* Polling of an email address

The different application modes (above) can be disabled/enabled as Spring profiles (see `application.properties`).

The application is built using Spring-Boot. The project is currently configured for UBL 2.1 invoices but it can be overriden
at the level of configuration to accommodate any XML-based validation.

# Building

## For development

Update file `/etc/filters/dev-config.properties` with your environment values.

 ```
 mvn spring-boot:run
 ```

## For use in a docker container

 ```
 mvn -Pdocker package
 ```

# Running

The application is accessible at:

* Web form: http://localhost:8080/invoice/upload
* GITB-compliant WS: http://localhost:8080/invoice/api/validation?wsdl

The application also accepts a flag `config.path` to point to the exact location of the configuration file to use. This
can be passed either as a system property or be set as an environment variable.
