package mj.ocraptor.javafx;

public enum FXMLFile {

  SELECT_DATABASE("SelectDatabase.fxml"), //
  ADD_DATABASE("AddDatabase.fxml"), //
  EDIT_DATABASE("EditDatabase.fxml"), //
  CONFIRMATION("Confirmation.fxml"), //
  SETTINGS_MANAGER("SettingsManager.fxml"), //
  MESSAGE_DIALOG("MessageDialog.fxml"), //
  SEARCH_DIALOG("SearchDialog.fxml"), //
  SEARCH_RESULT("SearchResult.fxml"), //
  HELP_BROWSER("HelpBrowser.fxml"), //
  LOAD_SCREEN("LoadingScreen.fxml");

  private String fileName;

  /**
   *
   */
  FXMLFile(String fileName) {
    this.fileName = fileName;
  }

  /**
   * @return the fileName
   */
  public String getFileName() {
    return fileName;
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
