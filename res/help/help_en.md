OCRaptor
====
![Alt text](img/OCRaptorIcon.png)

This program allows you to create a full-text index of your document files in a specified folder.
You can search that index rather than running a full-text search of each individul document file in your catalog.
An index search produces a results list with links to the occurrences of the indexed documents.
The main focus of this application lies on [optical character recognition (OCR)](http://en.wikipedia.org/wiki/Optical_character_recognition).
It extracts text from your (embedded/standalone) image files and stores them in a searchable and portable database.
In addition, OCRaptor also stores plain text and metadata of your documents.

The application supports a [wide variety of document filetypes](#SupportedFiletypes)

<a name="Outline"></a>
# Outline

[TOC]

# System requirements
* Microsoft Windows 7/8 64Bit
* Linux 64Bit
* Apple OSX 10.8-10.10

Due to time constraints, 32Bit-Systems are not supported. OCRaptor comes with a build-in Java 8 Runtime Environment.

# Installation
The installation of OCRaptor is a straightforward process, requiring minimum user input. Just download the application from:

* [Microsoft Windows 7/8 64Bit](http://workupload.com/file/t7Us5fG2)
* Linux 64Bit (Not online yet)
* Apple OSX 10.8-10.10 (Not online yet)

<a name="SupportedFiletypes"></a>
# Supported filetypes
* Image files:
    * JPEG, PNG, TIFF, BMP, GIF
* Microsoft Office:
    * Word, Excel, Powerpoint,
        [XPS](http://windows.microsoft.com/en-us/windows7/products/features/xps),
        [RTF](http://en.wikipedia.org/wiki/Rich_Text_Format),
        [CHM](http://en.wikipedia.org/wiki/Microsoft_Compiled_HTML_Help)
* LibreOffice / OpenOffice:
    * Writer, Impress, Calc
* Apple iWork'09:
    * Pages, Numbers, Key
* Adobe PDF
* [Postscript](http://en.wikipedia.org/wiki/PostScript)
* XML, HTML
* [EPUB](http://en.wikipedia.org/wiki/EPUB)
* [Xournal](http://en.wikipedia.org/wiki/Xournal)
* Plain textfiles
* Planned filetypes:
    * Apple iWork'13
    * Archives-files (*.zip, *.rar...)
    * [DjVu](http://en.wikipedia.org/wiki/DjVu)
    * Microsoft Publisher
    * Microsoft OneNote
    * [Evernote](https://evernote.com/intl/de/)
    * [RSS-Feeds](http://en.wikipedia.org/wiki/RSS)

# Interface
<a name="SelectDatabase"></a>
## Quick Guide

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

![](img/SelectDatabase01-en.png)
![](img/AddDatabase01-en.png)
![](img/SelectDatabase02-en.png)
![](img/EditDatabase01-en.png)
![](img/EditDatabase02-en.png)
![](img/SettingsManager01-en.png)
![](img/LoadingScreen01-en.png)
![](img/LoadingScreen02-en.png)
![](img/SearchDialog01-en.png)
![](img/SearchDialog02-en.png)
![](img/SearchResult01-en.png)
![](img/SearchResult02-en.png)


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
### Option: 'Passwords'

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

<a name="TEXT_FILE_EXTENSIONS"></a>
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

# Command Line Version

TODO :: TODO :: TODO :: TODO :: TODO :: TODO

## Options

    **************************************************************************
    usage: indexer.jar -d <DIR> [-c <FILE>] [-i <DIR>] [-l <STRING>]
            [-p] [-q] [-r] [-s] [-v] [-g] [-h] [-H]
    **************************************************************************
     -d,--db-directory <DIR>   Path to your database directory [REQUIRED]
     -c,--config-file <FILE>   Path to your configuration file.
     -i,--index <DIR>          Path to the directory you want to index
     -l,--locate <STRING>      Search database for given string
     -p,--progress             Count files and show a progress-bar (takes
                               longer).
     -q,--quiet                Suppress any output.
     -r,--reset-db             Reset given database
     -s,--show-dialog          Show open-file dialog
     -v,--verbose              Show more progress-information
     -g,--gui                  Show GUI-Version.
     -h,--help                 Shows this infopage.
     -H,--extended-help        Shows a detailed infopage.
    **************************************************************************


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
