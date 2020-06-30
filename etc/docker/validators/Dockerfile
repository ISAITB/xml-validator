FROM tomcat:8.0.38-jre8

RUN mkdir /validator_setup
RUN mkdir /validator
RUN mkdir -p /usr/local/tomcat/conf/Catalina/localhost/
COPY ./validator.jar /validator_setup
COPY ./setup.sh /validator_setup
RUN sh -c 'touch /validator_setup/validator.jar'
RUN sh -c 'chmod +x /validator_setup/setup.sh'
RUN sh -c 'rm -rf /usr/local/tomcat/webapps/*'
RUN sh -c 'ls -l /validator_setup'
ONBUILD COPY resources/ /validator/
ONBUILD RUN sh -c 'ls -l /validator_setup'
ONBUILD RUN sh -c 'ls -l /validator_setup'
ONBUILD RUN /validator_setup/setup.sh
