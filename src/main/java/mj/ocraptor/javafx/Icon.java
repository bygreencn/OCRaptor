package mj.ocraptor.javafx;

public enum Icon {
  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //
  ADD("add.png"), //
  BACK("back.png"), //
  CLIPBOARD("clipboard.png"), //
  COMPRESS("compress.png"), //
  DELETE("delete.png"), //
  DELETE_WHITE("delete_white.png"), //
  FILE("file.png"), //
  FOLDER_OPEN("folder_o.png"), //
  FOLDER("folder.png"), //
  GITHUB("github.png"), //
  HAND("hand.png"), //
  HAND_WHITE("hand_white.png"), //
  HELP("help.png"), //
  LINK("link.png"), //
  LIST("list.png"), //
  LIST_WHITE("list_white.png"), //
  MORE("more.png"), //
  PAUSE("pause.png"), //
  PLAY("play.png"), //
  POWER_OFF("power-off.png"), //
  RELOAD("reload.png"), //
  RIGHT("right.png"), //
  SAVE("save.png"), //
  SEARCH("search.png"), //
  SEARCH_WHITE("search_white.png"), //
  SETTINGS("settings.png"), //
  TRASH("trash.png"), //
  TRASH_WHITE("trash_white.png"), //
  YES("yes.png"), //
  YES_WHITE("yes_white.png"), //
  INFO("info.png"), //
  INFO_SMALL("info_circle.png"), //
  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //
  IMAGE("image.png"), //
  DOCX("docx.png"), //
  ODP("odp.png"), //
  ODS("ods.png"), //
  ODT("odt.png"), //
  PDF("pdf.png"), //
  RTF("rtf.png"), //
  PPTX("pptx.png"), //
  XLSX("xlsx.png"), //
  XPS("xps.png"), //
  PS("ps.png"), //
  TEXT("txt.png"), //
  ADDON("addon.png"), //
  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //
  // TODO: replace with .ico-file?
  STAGE_ICON("icon.png"), //
  SPLASH_SCREEN("splashscreen.png") //
  ;

  private String fileName;

  /**
   *
   */
  Icon(String fileName) {
    this.fileName = fileName;
  }

  /**
   * @return the fileName
   */
  public String getFileName() {
    return this.fileName;
  }

  /**
   * {@inheritDoc}
   *
   * @see Object#toString()
   */
  public String toString() {
    return this.fileName;
  }
}
