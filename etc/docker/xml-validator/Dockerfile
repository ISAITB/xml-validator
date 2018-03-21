FROM java:openjdk-8u72-jre

RUN mkdir /validator
COPY validator.war /validator
RUN sh -c 'touch /validator/validator.war'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/validator/validator.war"]
EXPOSE 8080
WORKDIR /validator