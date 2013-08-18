Anvil Mapper
============

Usage Instructions:
* Copy the contents of the world folder that you want mapped to 'world'.
* The '*.mca' region files should now be in the 'world\region' folder.
* Run 'run.bat'.
* Generated region images will be output to 'images'.
* Open 'index.html' in a web browser to view the map.

Compilation Instructions:
* Depends on the region file code in MapWriter so you will need to clone MapWriter from GitHub to build AnvilMapper.
* Compile using javac:
    javac src\anvilmapper\AnvilMapper.java -sourcepath src -sourcepath ..\mapwriter\src -d bin
* Make sure that you replace "..\mapwriter\src" with the directory you cloned mapwriter to.

