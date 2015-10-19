package mj.ocraptor.configuration.properties;

public enum ConfigInteger {
  // *INDENT-OFF*
  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //
  DIALOG_SNIPPET_MAX_STRING_LENGTH
  , DIALOG_LINE_MAX_LENGTH

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //
  , MAX_SEARCH_RESULTS
  , MAX_TEXT_SIZE_IN_KB
  , NUMBER_OF_CPU_CORES_TO_USE
  , PROCESSING_TIMEOUT_IN_SECONDS
  , PROCESS_XMS
  , PROCESS_XMX
  , RMI_SERVER_PORT

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //
  , MAX_FULLTEXT_LENGTH
  , MAX_IMAGE_SIZE_IN_KB
  , MIN_IMAGE_SIZE_IN_KB
  , MIN_IMAGE_WIDTH_FOR_OCR
  , MAX_IMAGE_WIDTH_FOR_OCR
  , MIN_IMAGE_HEIGHT_FOR_OCR
  , MAX_IMAGE_HEIGHT_FOR_OCR
  , MAX_IMAGE_PER_PAGE_RATIO
  ;
  // *INDENT-ON*

  public String property() {
    return this.name();
  }
}
