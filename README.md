# J-AST

Your tool for managing, annotating, and analyzing disk diffusion assays and E-tests

**Upload your data**

Use the uploader component to store your data into a J-AST project.
You can later at any time download your data as *.zip.

**Annotate and arrange**

Annotate your data with essential metadata by either doing the work manually or using our automated tool that extracts the information from the file name.
Then you can proceed to either arrange your time lines manually via drag and drop or use the included automated tool that utilizes the metadata.

**Automated processing**

Select which data to process and run a variety of automated image processing and analysis algorithms directly from within J-AST.

**Manually guide the automated analysis**

The automated detection algorithms don't work or yield unsatisfactory results? Don't worry - all annotations can be edited manually directly within J-AST.

**Review results**

Annotate your data with essential metadata by either doing the work manually or using our automated tool that extracts the information from the file name.
Then you can proceed to either arrange your time lines manually via drag and drop or use the included automated tool that utilizes the metadata.

## Setting up a development environment

1. Create a mariadb database `jast-dev` with user `jast-dev` and password `jast`
2. Setup JIPipe in `/bin/jipipe-linux`, so the ImageJ executable is located in this directory (currently only Linux)
3. Start the frontend 
4. Start the backend

The frontend will proxy the backend, and you can start using the tool as-is.

## Deploying (Docker)

Requirements: Maven, Java 21, Node 21 (use nvm)

```bash 
cd ./dist/docker
./prepare.sh
```

This will generate a \*.zip file with the Dockerfile and docker-compose.yml

**Caution:** Update the passwords!