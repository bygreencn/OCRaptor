OCRaptor
====
![Alt text](img/OCRaptorIcon.png)

**OCRaptor** allows you to create a **full-text index** of your document files in a specified folder.
You can search that index rather than running a full-text search of each individual document file in your catalog.
An index search produces a results list with links to the occurrences of the indexed documents.

The main focus of this application lies on [optical character recognition (OCR)](http://en.wikipedia.org/wiki/Optical_character_recognition).
It extracts text from your (embedded/standalone) image files and stores them in a **searchable and portable database**.
In addition, OCRaptor also stores plain text and metadata of your documents.

The application supports a [wide variety of document filetypes](#SupportedFiletypes), so you don't have to convert your files to one required format.

# System requirements
* **Microsoft Windows** 7/8 x64
* **Linux** x64
* **Apple OSX** 10.8-10.10

OCRaptor comes with a **build-in Java 8** Runtime Environment. Make
sure that your system has at least 2048 MB of RAM.

<a name="SupportedFiletypes"></a>
# Supported filetypes
* **Image files**
    * JPEG, PNG, TIFF, BMP, GIF
* **Microsoft Office**
    * Word, Excel, Powerpoint,
        [XPS](http://windows.microsoft.com/en-us/windows7/products/features/xps),
        [RTF](http://en.wikipedia.org/wiki/Rich_Text_Format),
* **LibreOffice / OpenOffice**
    * Writer, Impress, Calc
* **Apple iWork'09**
    * Pages, Numbers, Key
* Adobe PDF
* [Postscript](http://en.wikipedia.org/wiki/PostScript)
* XML, HTML
* [EPUB](http://en.wikipedia.org/wiki/EPUB)
* [Xournal](http://en.wikipedia.org/wiki/Xournal)
* Plain textfiles<br/><br/>
* **Planned filetypes**
    * Apple iWork'13
    * Archives-files (*.zip, *.rar...)
    * [DjVu](http://en.wikipedia.org/wiki/DjVu)
    * Microsoft Publisher
    * Microsoft OneNote

If you need **sample image scans** to test OCRaptor,
[here](https://github.com/kolbasa/OCRaptor/releases/download/v.0.5.1-alpha/OCRaptor-Testdocuments.7z)
are the files used in my test cases.<br/>
The 7zip-archive contains photos and scanned images of documents in English and German.

# Download and Installation
**Be aware that OCRaptor is a work in progress and therefore contains a variety of bugs and rough edges. I strongly advise you not to use
this application in a produtive environment.**

* **Microsoft Windows**<br/>
  The installation on Windows is a straightforward process, requiring minimum user input. Just download the application from
  [here](https://github.com/kolbasa/OCRaptor/releases/tag/v.0.5.1-alpha) and follow the steps of the installer (there are no hidden adware options).

* **Linux**<br/>
  At the moment there is no Linux installer available. Just download the 7zip-archive from
  [here](https://github.com/kolbasa/OCRaptor/releases/tag/v.0.5.1-alpha), extract it to your desired location and install the dependencies
  as shown below.

  There are some packages that are required to run OCRaptor in Linux:<br/>
  ``tesseract-ocr``, ``libtesseract3``, ``liblept4``, ``ghostscript``

  Shell command for distributions based on **Debian**:<br/>
  ``sudo apt-get install tesseract-ocr libtesseract3 liblept4 ghostscript``

  Shell command for **Arch Linux**:<br/>
  ``sudo pacman -S tesseract leptonica ghostscript``

  Shell command for the **RPM-based** distributions:<br/>
  ``TODO: will look into it soon``

* **Apple OSX**<br/>
  The OSX package **has not been thoroughly tested**, but it should work - theoretically.

  Just download the 7zip-archive from [here](https://github.com/kolbasa/OCRaptor/releases/tag/v.0.5.1-alpha), extract it to your desired location
  and install ``tesseract`` and ``ghostscript`` via [``brew``](http://brew.sh/).

  If you encounter problems running OCRaptor on a Mac, please let me
  [know](https://github.com/kolbasa/OCRaptor/issues).

# Starting
* **Microsoft Windows**<br/>
    Just double click the newly created desktop icon and the application should start.
  ![](img/Desktop-Icon-01-en.png)

* **Linux**<br/>

  Go to the extracted folder:<br/>
  ``cd OCRaptor-Linux``<br/>

  Before starting OCRaptor you should assign some execute permissions:<br/>
  ``chmod u+x ocraptor ocraptor-cl ocraptor-pl``<br/>

  Then execute the main shell script:<br/>
  ``./ocraptor``

* **Apple OSX**<br/>

  Go to the extracted folder:<br/>
  ``cd OCRaptor-Osx``<br/>

  Before starting OCRaptor you should assign some execute permissions:<br/>
  ``chmod u+x ocraptor ocraptor-cl ocraptor-pl``<br/>

  Then execute the main shell sript:<br/>
  ``./ocraptor``

# Interface
<a name="SelectDatabase"></a>
## Quick Guide

When you first open OCRaptor, you will see this screen.
To get started you need to add a new document database. Click on '**Add database**'.
<br/>![](img/SelectDatabase01-en.png)<br/>

Select an empty folder and name it, then click '**Save**'. You will
return to the previous screen with a new entry.
<br/>![](img/AddDatabase01-en.png)<br/>

In this scenario there are two databases: 'Incoming Invoices' and 'Study Documents'.
Simply mark your entry and click '**Select**' to load the database.
<br/>![](img/SelectDatabase02-en.png)<br/>

Now you have to select the directories you want to index. You can simply
drag them into the window or select them by clicking '**Add Folder**'.
<br/>![](img/EditDatabase01-en.png)<br/>

They should now appear in the list.
<br/>![](img/EditDatabase02-en.png)<br/>

In the '*Settings Manager*' window ('**Config**' button) you can configure
which filetypes you want to include (e.g. Adobe PDF, Microsoft Office).
<br/>![](img/SettingsManager01-en.png)<br/>

Now click the '**Index**' button to populate your file database. This step
may take awhile, depending on size and number of files in your document
folder. Text extraction from image files can be highly CPU-intensive.
However, you only have to do it once, after that you have a full
index of your scanned documents. If you add or modify files, then just repeat
this step and OCRaptor will automatically update its index while
skipping unchanged files.
<br/>![](img/LoadingScreen01-en.png)<br/>

Depending on your system's configuration, a firewall may request your
permission to allow access to public networks. Click cancel and
block all requests. **OCRaptor does not try to connect to extern
servers**. However, it does use a server-client model for processing document
files. This implementation is restricted to the local network. You can
change the default port (1098) in the Settings Manager.
<br/>![](img/WindowsFirewall01-en.png)<br/>

Now you can search your document database. Click **Search**.
<br/>![](img/LoadingScreen02-en.png)<br/>

Enter your search query.
<br/>![](img/SearchDialog01-en.png)<br/>

A simple search for a scanned invoice of a Raspberry Pi Infrared
Camera. OCRaptor supports Boolean Operators, Wildcard Searches,
Fuzzy Searches and Proximity Searches. I will update the documentation
soon on this topic.
<br/>![](img/SearchDialog02-en.png)<br/>

The search result. If you click '**Open**' your file will open in your default image viewer
application. '**Show**' will mark the file in your default file browser.
'**Fulltext**' will show you the complete text in your default web
browser ([](example);
<br/>![](img/SearchResult01-en.png)<br/>

The picture below shows the standard Windows 10 Photo
Viewer.
<br/>![](img/SearchResult02-en.png)<br/>


# Command Line Version

OCRaptor has a fully functional commandline interface.

## Options

    **************************************************************************
    usage: ocraptor-cl -d <DIR> [-c <FILE>] [-f <STRING>] [-i <DIR>] [-b] [-g]
           [-h] [-H] [-p] [-q] [-r] [-s] [-u] [-v]

    options:
     -d,--db-directory <DIR>   Path to your database directory [REQUIRED]
     -c,--config-file <FILE>   Path to your configuration file.
     -f,--find <STRING>        Search database for given string
     -i,--index <DIR>          Path to the directory you want to index
     -b,--build-in-jre         Use build-in JRE.
     -g,--gui                  Show GUI-Version.
     -h,--help                 Shows this infopage.
     -H,--extended-help        Shows a detailed infopage.
     -p,--progress             Count files and show a progress-bar (takes
                               longer).
     -q,--quiet                Suppress any output.
     -r,--reset-db             Reset given database
     -s,--show-dialog          Show open-file dialog
     -u,--userfolder           Copy config-files to user-folder.
     -v,--verbose              Show more progress-information
    **************************************************************************

<a name="AddDatabase"></a>
## Adding a new Database

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="EditDatabase"></a>
## Editing your Database

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="SearchDialog"></a>
## Searching your Database

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="SearchResult"></a>
## Search Result Screen

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="SettingsManager"></a>
## Settings Manager

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="ENABLE_IMAGE_OCR"></a>
### Option: 'Enable optical character recognition (OCR)'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="INCLUDE_METADATA"></a>
### Option: 'Include Metadata'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="INCLUDE_STANDALONE_IMAGE_FILES"></a>
### Option: 'Include standalone image files'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="INCLUDE_TEXT_FILES"></a>
### Option: 'Include text files'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="PRE_PROCESS_IMAGES_FOR_OCR"></a>
### Option: 'Preprocess images for OCR'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="NEW_FILES_NOTIFICATION_ONLY"></a>
### Option: 'Only show new files while indexing'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="DEFAULT_LANGUAGE_FOR_OCR"></a>
### Option: 'OCR Language'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="ENABLE_BUG_REPORT_SCREENS"></a>
### Option: 'Enable bug report screens'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="PAUSE_ON_ERROR"></a>
### Option: 'Pause indexing on error'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="ENABLE_USER_COMMAND_STDERR"></a>
### Option: 'Enable app command stderr output'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="ALWAYS_REMOVE_MISSING_FILES_FROM_DB"></a>
### Option: 'Always remove missing files from database'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="MAX_SEARCH_RESULTS"></a>
### Option: 'Max search results to show'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="NUMBER_OF_CPU_CORES_TO_USE"></a>
### Option: 'Number of CPU-Cores to use'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="PASSWORDS_TO_USE"></a>
### Option: 'Text file extensions'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="MIN_IMAGE_SIZE_IN_KB"></a>
<a name="MAX_IMAGE_SIZE_IN_KB"></a>
<a name="MIN_IMAGE_WIDTH_FOR_OCR"></a>
<a name="MAX_IMAGE_WIDTH_FOR_OCR"></a>
<a name="MIN_IMAGE_HEIGHT_FOR_OCR"></a>
<a name="MAX_IMAGE_HEIGHT_FOR_OCR"></a>
### Option: Image properties

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="DIRECTORY_OPEN_CMD"></a>
<a name="IMAGE_FILE_OPEN_CMD"></a>
### Option: 'Bash/Shell commands'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

# Build from source

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

# FAQ
## OCR

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

## Indexing

The technology behind OCRaptor is called indexing. When you install the application...


TODO :: TODO :: TODO :: TODO :: TODO :: TODO

# Release Notes

TODO :: TODO :: TODO :: TODO :: TODO :: TODO :: TODO

# Requested/Planed features

TODO :: TODO :: TODO :: TODO :: TODO :: TODO :: TODO

# Privacy Policy

TODO :: TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="Contact"></a>
# Contact me
    Name:   Michael Jedich
    E-mail: m.jedich@mail.de
    GitHub: https://github.com/kolbasa

<!---
<font color='white'>

    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .
    .

</font>
-->
