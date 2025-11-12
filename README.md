# J-AST

The JIPipe Antimicrobial Susceptibility Test (J-AST) platform is
your tool for managing, annotating, and analyzing disk diffusion assays and E-tests.

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

## Extending J-AST

### Creating plugins

You can create JIPipe project files that will be recognized by J-AST as plugins. Place such files into the 
`share/plugins` directory.

For the plugin to work, you will need to adhere to specific constraints in your JIPipe workflow:

* You must have one project-wide directory with the key `tmp_dir`. This key is automatically set by J-AST and will point to the directory that contains all inputs and outputs 
* You must create various project-wide parameters that J-AST will recognize (see table below)
* Set a name and a description using the project overview. Those will be used by J-AST.
* While supported, we recommend to **avoid** using parameter references. Instead, use global parameters. The reason behind this is that in JIPipe 5.3.0 there's not yet a way to recover the parameter type from a reference.

| Type        | Key                                | Description                                                                                                         | Allowed values                                                        |
|-------------|------------------------------------|---------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| Boolean     | jast_plugin                        | Must be true for J-AST to recognize the plugin                                                                      | true/false                                                            |
| String      | jast_plugin_category               | The menu category where the operation will be placed                                                                | Can be empty                                                          |
| String      | jast_plugin_assay_type_restriction | Allows to restrict the operation to a specific assay type                                                           | `DDA`/`ETest`/`Unknown`/Empty (Defaults to accepting all assay types) |
| String      | jast_plugin_mode                   | Determines how images are related to each other (each processed by itself, alway process whole timeline row/column) | `Single`/`FullRow`/`FullColumn` (Defaults to single)                  |
| String      | jast_plugin_view_mode_restriction  | Allows to hide the operation from certain project modes                                                             | `Timeline`/`Grid`/Empty (Defaults to all view modes)                  |
| Boolean     | jast_plugin_generates_results      | Must be checked if results should be extracted by J-AST                                                             | true/false                                                            |
| String List | jast_plugin_inputs                 | List of inputs that will be available in the temporary directory                                                    | `plate`/`strip`/`disk`/`strip-disk`/`zoi-shape`      |
| String List | jast_plugin_outputs                | Lists of outputs that will be generated in the temporary directory                                                  | `raw`/`plate`/`strip`/`disk`/`strip-disk`/`zoi-shape`/`pixelSize`                                                                      |

##### Inputs

The pipeline receives inputs in form of files/directories located in a temporary directory.

* `metadata.csv` contains all text metadata for each image. The header is `"#ImageId","#Experiment","#Sample","#TimePoint","#AssayType",PixelSize,GroupRow,GroupColumn`
* `raw` is always created and contains the raw image files named according to their unique database ID (#ImageId)
* Mask annotations (0 = background, 255 = foreground) are placed in directories named after the unique annotation ID and named according to the related image ID.

The supported mask annotations are:

| ID         | Description                          |
|------------|--------------------------------------|
| plate      | The plate                            |
| strip-disk | The DDA disk or the E-Test strip     |
| zoi-shape  | The E-Test ZOI shape (black for DDA) |

We recommend to use `Folder list` nodes that contain only the mask annotation ID / `raw`.

#### Outputs

All outputs must be written relative to the `tmp_path` path.

* Write into a directory named after the mask annotation ID (for raw images, or if you update an annotation, use `*_updated`, e.g., `raw_updated`). Name the images according to the database ID
* To write results, place all files into a `results` subdirectory. J-AST will automatically explore the hierarchy.

### Adding new backend tasks

If you need more flexibility, you can also develop new backend tasks using the Java API. 
All tasks can be found in the `org.hkijena.jast.tasks.workloads` package and will be automatically discovered 
by the task registry by their `@BackendTaskType` annotation.

The frontend does not need to be changed.

 

