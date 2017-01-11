
# GeoCrunch Quick Start Guide


## What does it do?

GeoCrunch extracts, collects and visualizes spatial data and metadata 
from file collections of any media type. 
Though metadata can be retrieved from all file types, retrieval of information from 
file content bodies is limited to .csv and natural language .pdf files, yet. 
Supported languages include English and German. 

The software consists of 
1. an extraction tool and
2. a web browser application for visualization of the results.

Extraction is performed on three levels: 
1. file or folder names are evaluated for place names
2. metadata is examined for coordinates or place names
3. analysis of file content bodies on .csv (coordinates) or .pdf (place names)


## Getting started

### See the analysis results of sample data in the GeoCrunch viewer

Download and unpack the package *demo.zip*. 
Start the GeoCrunch Viewer by opening the file 
*demo/webapp/index.html*
in a standard conform web browser (Safari might have problems). 
An internet connection is required to render the map tiles. 

Browse the collection tree by clicking on the items on the left, 
see how the map display changes. 
Click on any marker on the map. The related file entry becomes 
highligted in the directory tree and a popup on the marker shows 
additional information on the context. 

Click on the button 'Filters' on top of the collection tree. 
Select the filter options you would like to see. 


### Create a spatial analysis of your own data collection

Remove the contents of the folder demo/webapp/data and insert 
the data you wish to analyse. 
There are restrictions on the amount of geocoding requests towards the 
various services, thus it is recommended to analyze no more than 
~10 scientific .pdf papers at once.

Have a look at the configuration file user.properties at GeoCrunch/config and 
adjust according to your needs.
**ATTENTION**: geocoding webservices have been turned off in that configuration file, 
as re-processing the provided example data would block the geocoding query limits of 
the provided API keys uneccessarily for at least one hour (for you and others). 
Turn this on only, if in fact you want to process new data. 
Consider creating your own API keys for the utilized geocoding services. 

The file default.properties holds the default settings suitable for most tasks 
and should not be changed in order to easily revert all adjustments by replacing 
user.properties by a fresh copy of default.properties. 

Open up a terminal and type 

```bash
java -jar geoCrunch.jar
```

Alternativey, just click on one of the starters suitable to your operating system. 
Check the results in the GeoCrunch viewer, after the process has terminated. 


## More information

Please read the related bachelor_thesis.pdf to learn about the scientific backgrounds 
and concerns of the software development. 

The full application sourcecode is available here: 
https://github.com/hashmich/GeoCrunch.git

For further questions, send an email to the developer: mail@hendrikschmeer.de










