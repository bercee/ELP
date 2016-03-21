# ELP - Europeana Language Processor
## Nataural Language Processing of Europeana's metadata in Hungarian

### Running program 
Execute runnable ELP.jar desktop application, or import the project into eclipse. 

### Software requirements: 
- Java 8 
- magyarlánc 3.0 (download from http://www.inf.u-szeged.hu/rgai/magyarlanc)
- API key for Europeana's REST API. (Get an API key here: http://labs.europeana.eu/api/registration)
 
It is safer to run ELP with bigger heap memory. E.g.: java -Xmx1G -jar ELP.jar

 
### Program Usage
#### Download metadata
- API key: use your valid API key
- Search query: do not use special characters (e.g. kosztolanyi)
- Query refinements: e.g. LANGUAGE:hu
- Output: 
 * None: metadata is not saved, but directly submitted to parsing instead.
 * JSON_OneFile: all downloaded metadata is saved in a single file in JSON format.
 * JSON_SeparateFiles: each JSON response page is saved in a separate file in the specified directory.
 * LineByLine_OneFile: all downloaded objects are saved in a single file, one object in one line.
 * LineByLine_SeparateFile: each downloaded object is saved in a separate file in the specified directory.

 
#### Get Description
- Metadata input:
 * FromDownload: previously downloaded metadata is taken as input
 * FromFileOrFolder: metadata is taken from a single file or from a directory by reading all files in it. File format can be JSON or LineByLine.
- Description output:
 * None: descriptions are directly submitted to natural language processing.
 * OneFile: all descriptions are saved in a single file, one object in one line. 
 * SeparateFiles: each desciption is saved in a separate file in the specified format. 
 
 

#### NLP (Natural Langauge Processing)
- Magyarlánc path: the full path to magyarlanc-3.0.jar file
- Descriptions input: 
 * FromMetadata: previuosly downloaded and parsed metadata is taken as input
 * FromFileOrFolder: descriptions are taken from a single file or from a directory by reading all files in it.
- Output: NOT WORKING YET
