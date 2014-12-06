#!/bin/bash

# ---------------------------------------------------- #
# -- TODO:
# ---------------------------------------------------- #

BUILD_FROM_SOURCE=true
CLEAN_COMPILE=true
GENERATE_JAVACC_PARSER=false

BUILD_WINDOWS_INSTALLER=true
DOWNLOAD_DEPENDENCIES=true
INSTALL_DEPENDENCIES=true

if ! type "mvn" &> /dev/null; then
  echo "[ERROR] 'mvn' WAS NOT FOUND ON YOUR SYSTEM"
  echo "Instructions can be found at:"
  echo "http://maven.apache.org/download.cgi"
  exit 1
fi

if ! type "java" &> /dev/null; then
  echo "[ERROR] 'java' WAS NOT FOUND ON YOUR SYSTEM"
  echo "Instructions can be found at:"
  echo "http://www.oracle.com/technetwork/java/"\
"javase/downloads/jdk8-downloads-2133151.html"
  exit 1
fi

# ---------------------------------------------------- #
# -- TODO:
# ---------------------------------------------------- #

BASEDIR=$(pwd)
TEMP_FOLDER="$BASEDIR""/res/misc/online-resources"
USER_FOLDER="$BASEDIR""/res/cnfg/usr"
LOG_FOLDER="$BASEDIR""/res/cnfg/log"

# ---------------------------------------------------- #
# -- TODO:
# ---------------------------------------------------- #

if [ ! -d "$TEMP_FOLDER" ]; then
  mkdir -p "$TEMP_FOLDER"
fi

if [ ! -d "$USER_FOLDER" ]; then
  mkdir -p "$USER_FOLDER"
fi

if [ ! -d "$LOG_FOLDER" ]; then
  mkdir -p "$LOG_FOLDER"
fi

# ---------------------------------------------------- #
# -- TODO:
# ---------------------------------------------------- #

GIT_URL="https://github.com/kolbasa/"

WGEX_GIT="wgex"
TESS4J_NATIVES_GIT="tess4j-native-binaries"
GHOST4J_NATIVES_GIT="ghost4j-native-libraries"
TESS4J_WRAPPER="tess4j-wrapper"
JRE_VER_CHECKER_GIT="jre-version-checker"

# ---------------------------------------------------- #
# -- TODO:
# ---------------------------------------------------- #

TESS4J_API="http://kent.dl.sourceforge.net/project/"\
"tess4j/tess4j/1.3/Tess4J-1.3-src.zip"

SIGAR_API="http://repo1.maven.org/maven2/fr/inria/"\
"powerapi/sensor/sensor-sigar/1.5/sensor-sigar-1.5.jar"

XPS_PARSER="http://java-axp.googlecode.com/files/"\
"javaaxp-xps-viewer-0.2.1.linux.gtk.x86_64.zip"

TESS_LANG_OSD="https://tesseract-ocr.googlecode"\
".com/files/tesseract-ocr-3.01.osd.tar.gz"

TESS_LANG_DEU="https://tesseract-ocr.googlecode.com"\
"/files/tesseract-ocr-3.02.deu.tar.gz"

TESS_LANG_ENG="https://tesseract-ocr.googlecode.com"\
"/files/tesseract-ocr-3.02.eng.tar.gz"

# ---------------------------------------------------- #
# -- TODO:
# ---------------------------------------------------- #

if $DOWNLOAD_DEPENDENCIES ; then
    # check if git is available
    if ! type "git" &> /dev/null; then
      echo "[ERROR] 'git' WAS NOT FOUND ON YOUR SYSTEM"
      echo "Instructions can be found at:"
      echo "http://git-scm.com/book/en/Getting-Started-Installing-Git"
      exit 1
    fi
    cd "$TEMP_FOLDER"

    # download and build a simple file downloader
    # used for cross-platform building
    if [ ! -d "$WGEX_GIT" ]; then
      git clone "$GIT_URL""$WGEX_GIT" || exit 1
      cd "$WGEX_GIT"
      ./build-linux-osx-bin.sh || exit 1
      cd "$TEMP_FOLDER"
    fi

    # JRE_VER_CHECKER_GIT="jre-version-checker"

    # download tess4j native binaries
    if [ ! -d "$TESS4J_NATIVES_GIT" ]; then
      git clone "$GIT_URL""$TESS4J_NATIVES_GIT" || exit 1
    fi

    # download tess4j native binaries
    if [ ! -d "$GHOST4J_NATIVES_GIT" ]; then
      git clone "$GIT_URL""$GHOST4J_NATIVES_GIT" || exit 1
    fi

    # download tess4j wrapper
    if [ ! -d "$TESS4J_WRAPPER" ]; then
      git clone "$GIT_URL""$TESS4J_WRAPPER" || exit 1
      cd "$TESS4J_WRAPPER"
      ./build-linux-osx-bin.sh || exit 1
      cd "$TEMP_FOLDER"
    fi

    # download java version checker
    if [ ! -d "$JRE_VER_CHECKER_GIT" ]; then
      git clone "$GIT_URL""$JRE_VER_CHECKER_GIT" || exit 1
      cd "$JRE_VER_CHECKER_GIT"
      ./build-linux-osx-bin.sh || exit 1
      cd "$TEMP_FOLDER"
    fi

    # download dependencies not covered by maven repos:
    WGEX_BIN="wgex/bin/wgex.jar"

    EXT_FOLDER="sigar"
    echo -e "\n"
    if [ ! -d "$EXT_FOLDER" ]; then
      echo -e "--------------------------------------\n"
      java -jar "$WGEX_BIN" "$EXT_FOLDER" \
        "$SIGAR_API" || exit 1
    fi
    EXT_FOLDER="tess4j"
    if [ ! -d "$EXT_FOLDER" ]; then
      echo -e "--------------------------------------\n"
      java -jar "$WGEX_BIN" "$EXT_FOLDER" \
        "$TESS4J_API" || exit 1
    fi
    EXT_FOLDER="javaaxp"
    if [ ! -d "$EXT_FOLDER" ]; then
      echo -e "--------------------------------------\n"
      java -jar "$WGEX_BIN" "$EXT_FOLDER" \
        "$XPS_PARSER" || exit 1
    fi
    EXT_FOLDER="tess4j_lang"
    if [ ! -d "$EXT_FOLDER" ]; then
      echo -e "--------------------------------------\n"
      java -jar "$WGEX_BIN" "$EXT_FOLDER" \
        "$TESS_LANG_OSD" || exit 1
      echo -e "--------------------------------------\n"
      java -jar "$WGEX_BIN" "$EXT_FOLDER" \
        "$TESS_LANG_DEU" || exit 1
      echo -e "--------------------------------------\n"
      java -jar "$WGEX_BIN" "$EXT_FOLDER" \
        "$TESS_LANG_ENG" || exit 1
    fi
    echo -e "--------------------------------------\n"
fi

# ---------------------------------------------------- #
# -- TODO:
# ---------------------------------------------------- #

TESS4J_JAR="$TEMP_FOLDER""/tess4j/Tess4J"\
"/dist/tess4j.jar"

JAVAAXP_JAR="$TEMP_FOLDER""/javaaxp/java-axp"\
"/plugins/javaaxp.xps_core_0.2.0.jar"

cd "$BASEDIR"

if $INSTALL_DEPENDENCIES ; then
  mvn install:install-file -Dfile="$TESS4J_JAR" \
         -DgroupId=mj.tess4j \
         -DartifactId=tess4j \
         -Dversion=1.3 \
         -Dpackaging=jar | egrep -v "(^\[WARNING\])"

  # check if errors occured
  if [ ${PIPESTATUS[0]} -ne "0" ]; then exit 1; fi

  mvn install:install-file -Dfile="$JAVAAXP_JAR" \
         -DgroupId=mj.javaaxp \
         -DartifactId=javaaxp \
         -Dversion=0.2.0 \
         -Dpackaging=jar | egrep -v "(^\[WARNING\])"

  # check if errors occured
  if [ ${PIPESTATUS[0]} -ne "0" ]; then exit 1; fi
fi

# ---------------------------------------------------- #
# -- TODO:
# ---------------------------------------------------- #

if $CLEAN_COMPILE; then
  mvn clean || exit 1
fi

if $GENERATE_JAVACC_PARSER ; then
  mvn org.codehaus.mojo:javacc-maven-plugin:javacc \
    || exit 1
fi

if $BUILD_FROM_SOURCE; then
  mvn package || exit 1
fi
