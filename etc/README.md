# Docker build

For the docker image build:
1. Build the war file for docker from the main project.
2. Copy the resulting war file into the /etc/docker folder, selecting the folder for the image you want to build.
3. Build the image with:
    - xml-validator: `docker build -t isaitb/xml-validator:latest .` 
    - \[DEPRECATED\] validators: `docker build -t isaitb/validators:latest .`

Deprecation note: The `validators` image was used in the past to define a validator that supports numerous distinct 
validation domains. This approach remains viable but given the specific image, results in distinct applications that
consume significant resources. The new approach for multi-domain validation is to use a single application (based on
`xml-validator`) that includes in its resource root multiple domain sub-folders, each with its own configuration. 