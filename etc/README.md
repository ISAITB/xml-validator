# Docker build

For the docker image build:
1. Build the war file for docker from the main project.
2. Copy the resulting war file into the /etc/docker folder, selecting the folder for the image you want to build.
3. Build the image with:
    - validators: `docker build -t isaitb/validators:latest .`
    - xml-validator: `docker build -t isaitb/xml-validator:latest .` 