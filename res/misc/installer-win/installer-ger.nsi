# --------------------------------

Unicode true
!include "MUI2.nsh"

# --------------------------------

# TODO: change compression at upload
SetCompressor /SOLID lzma
SetCompressorDictSize 8

# --------------------------------

# VERSIONMAJOR - VERSIONMINOR - VERSIONBUILD
!define APPVERSION         "0.5.1-alpha" #MARKER:HIODLEVA
!define APPNAME            "OCRaptor"
!define BASEDIR            "..\..\.."
!define ORIGINAL_ICON      "${BASEDIR}\res\help\img\favicon.ico"
!define INSTALLER_LANG     "German"
!define INSTALLER_LANG_TR  "Deutsch"
!define GIT_URL            "https://github.com/kolbasa"

# TODO: CHANGELOG
!define UPDATEURL          "${HELPURL}/${APPNAME}" # "Product Updates" link
!define HELPURL            "${HELPURL}/${APPNAME}" # "Support Information" link
!define ABOUTURL           "${HELPURL}"            # "Publisher" link

!define COMPANYNAME        "Kolbasa"
!define INSTALLSIZE        313344

!define DESCRIPTION        "OCRaptor allows you to create a full-text index of your \
                           document files in a specified folder. You can search that \
                           index rather than running a full-text search of each \
                           individual document file in your catalog."


!define REG_ROOT           "HKLM"
!define REG_POSITION       "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"

!define MUI_FINISHPAGE_RUN "$INSTDIR\${APPNAME}.exe"

!define MUI_ICON           "${ORIGINAL_ICON}"
!define MUI_UNICON         "${ORIGINAL_ICON}"

# --------------------------------

function .onInit
  setShellVarContext all
functionEnd

function un.onInit
  SetShellVarContext all
functionEnd

# --------------------------------

# The name of the installer
Name "${APPNAME}"

# The file to write
OutFile "${BASEDIR}\${APPNAME}-${APPVERSION}-${INSTALLER_LANG_TR}.exe"

# The default installation directory
InstallDir $PROGRAMFILES64\${APPNAME}
InstallDirRegKey ${REG_ROOT} "${REG_POSITION}" "InstallDir"

# logo
Icon          "${ORIGINAL_ICON}"
UninstallIcon "${ORIGINAL_ICON}"

# --------------------------------

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

# Uninstaller pages
!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

# Used by installer and uninstaller
!insertmacro MUI_LANGUAGE ${INSTALLER_LANG}

# --------------------------------

BrandingText "Build ${APPVERSION}"

# The text to prompt the user to enter a directory
DirText "This will install OCRaptor on your computer.$\r$\n$\r$\n\
         OCRaptor allows you to create a full-text index of your \
         document files in a specified folder. You can search that \
         index rather than running a full-text search of each \
         individual document file in your catalog.$\r$\n$\r$\n\
         Choose a directory."

# show details
ShowInstDetails   Show
ShowUninstDetails Show

# --------------------------------

section "install"
  # Files for the install directory - to build the installer, 
  # these should be in the same directory as the install script (this file)
  setOutPath $INSTDIR
  # Files added here should be removed by the uninstaller (see section "uninstall")
  File /r "${BASEDIR}\bin-win64\*"
  # File /r "${BASEDIR}\bin-win64\${APPNAME}.exe"

  CreateShortCut "$DESKTOP\${APPNAME}.lnk" "$INSTDIR\${APPNAME}.exe" ""

  # Uninstaller - See function un.onInit and section "uninstall" for configuration
  writeUninstaller "$INSTDIR\Uninstall.exe"

  # Start Menu
  createDirectory "$SMPROGRAMS\${APPNAME}"
  createShortCut  "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk" \
    "$INSTDIR\${APPNAME}.exe" "" "$INSTDIR\icon.ico"
  createShortCut  "$SMPROGRAMS\${APPNAME}\${APPNAME} \
    Uninstall.lnk" "$INSTDIR\Uninstall.exe"

  # Registry information for add/remove programs
  WriteRegStr HKLM   ${REG_POSITION} "DisplayName"          "${APPNAME}"
  WriteRegStr HKLM   ${REG_POSITION} "UninstallString"      "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM   ${REG_POSITION} "QuietUninstallString" "$INSTDIR\Uninstall.exe /S"
  WriteRegStr HKLM   ${REG_POSITION} "InstallLocation"      "$INSTDIR"
  WriteRegStr HKLM   ${REG_POSITION} "DisplayIcon"          "$INSTDIR\${APPNAME}.exe"
  WriteRegStr HKLM   ${REG_POSITION} "Publisher"            "${COMPANYNAME}"
  WriteRegStr HKLM   ${REG_POSITION} "HelpLink"             "${HELPURL}"
  WriteRegStr HKLM   ${REG_POSITION} "URLUpdateInfo"        "${UPDATEURL}"
  WriteRegStr HKLM   ${REG_POSITION} "URLInfoAbout"         "${ABOUTURL}"
  WriteRegStr HKLM   ${REG_POSITION} "DisplayVersion"       "${APPVERSION}"
  # There is no option for modifying or repairing the install
  WriteRegDWORD HKLM ${REG_POSITION} "NoModify" 1
  WriteRegDWORD HKLM ${REG_POSITION} "NoRepair" 1
  # Set the INSTALLSIZE constant (!defined at the top of this script) 
  # so Add/Remove Programs can accurately report the size
  WriteRegDWORD HKLM ${REG_POSITION} "EstimatedSize" ${INSTALLSIZE}
sectionEnd

# --------------------------------

section "uninstall"

  # Remove Start Menu launcher
  delete "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk"
  delete "$SMPROGRAMS\${APPNAME}\${APPNAME} Uninstall.lnk"

  # Remove desktop launcher
  delete "$DESKTOP\${APPNAME}.lnk"

  # Try to remove the Start Menu folder - this will only happen if it is empty
  rmDir "$SMPROGRAMS\${APPNAME}"

  # Remove files
  rmDir /R /REBOOTOK $INSTDIR

  # Always delete uninstaller as the last action
  delete $INSTDIR\Uninstall.exe

  # Try to remove the install directory - this will only happen if it is empty
  rmDir $INSTDIR

  # Remove uninstaller information from the registry
  DeleteRegKey HKLM ${REG_POSITION}
sectionEnd

# --------------------------------
