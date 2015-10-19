package mj.ocraptor.database.dao;

/**
 *
 *
 * @author
 *  <li>{@link #PARSING}</li>
 *  <li>{@link #KILLED}</li>
 */
public enum ResultError {
  /**
   *
   */
  TIMEOUT        ("neefPavA"),
  /**
   *
   */
  PARSING        ("myghojVitt"),
  /**
   *
   */
  OCR            ("JafUdjer"),
  /**
   *
   */
  EMPTY          ("wiHijucJed"),
  /**
   * The parsing process was killed by the user.
   * --> do not save to the database
   */
  KILLED         ("NinBoghfo"),
  /**
   *
   */
  KILLED_FORCED  ("andemFebA"),
  /**
   *
   */
  HASH_KNOWN     ("shybrilfUb"),
  /**
   *
   */
  NOT_SUPPORTED  ("EajivWeci");

  private String errorCode;
  public static final String PREFIX = "error-";

  /**
   * @param errorCode
   *
   */
  private ResultError(final String errorCode) {
    this.errorCode = errorCode;
  }


  /**
   * @return the errorCode
   */
  public String getErrorCode() {
    return PREFIX + errorCode;
  }

  /**
   *
   *
   * @param code
   * @return
   */
  public static ResultError getByCode(final String code) {
    for(ResultError error: ResultError.values()) {
      if (error.getErrorCode().equals(code) || code.endsWith(error.getErrorCode())) {
        return error;
      }
    }
    return null;
  }

}
