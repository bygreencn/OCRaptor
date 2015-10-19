package mj.ocraptor.configuration;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import mj.ocraptor.configuration.properties.ConfigString;

import org.apache.commons.lang3.LocaleUtils;

public class Localization {

  private ResourceBundle resourceBundle;
  private Config cfg;
  private static final String LANGUAGE_RESOURCE_FOLDER = "mj.ocraptor.javafx.controllers.text";
  private static Localization instance;
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Localization.class);


  /**
   *
   */
  private Localization() {
    this.cfg = Config.inst();
    this.initResourceBundle();
  }

  /**
   *
   *
   */
  public static Localization instance() {
    if (instance == null) {
      instance = new Localization();
    }
    return instance;
  }


  /**
   *
   *
   */
  private void initResourceBundle() {
    Locale defaultLocale = Locale.ENGLISH;
    String defaultLocaleFromProperties = this.cfg.getProp(ConfigString.DEFAULT_LOCALE);
    if (!defaultLocaleFromProperties.isEmpty()) {
      defaultLocale = LocaleUtils.toLocale(defaultLocaleFromProperties);
    }
    this.resourceBundle = ResourceBundle.getBundle(LANGUAGE_RESOURCE_FOLDER, defaultLocale);
  }

  /**
   *
   *
   * @return
   */
  public Locale getLocale() {
    return this.resourceBundle.getLocale();
  }

  /**
   *
   *
   * @param locale
   */
  public void setLocale(Locale locale) {
    this.resourceBundle = ResourceBundle.getBundle(LANGUAGE_RESOURCE_FOLDER, locale);
  }

  /**
   *
   *
   * @param property
   * @param placeHolders
   * @return
   */
  public String getText(final String property, final Object... placeHolders) {
    String[] stringList = new String[placeHolders.length];
    try {
      for (int i = 0; i < placeHolders.length; i++) {
        stringList[i] = String.valueOf(placeHolders[i]);
      }
    } catch (Exception e) {
      return "ERROR";
    }
    return getText(property, stringList);
  }

  /**
   *
   *
   * @param property
   * @param placeHolders
   * @return
   */
  public String getText(final String property, final String... placeHolders) {
    try {
      if (placeHolders.length == 0) {
        return this.resourceBundle.getString(property);
      } else {
        return MessageFormat.format(this.resourceBundle.getString(property), placeHolders);
      }
    } catch (Exception e) {
      LOGGER.error("Property not found: \"" + property + "\"");
      return "ERROR";
    }
  }

}
