package mj.extraction.image_processing.language;

// import com.cybozu.labs.langdetect.Detector;
// import com.cybozu.labs.langdetect.DetectorFactory;
// import com.cybozu.labs.langdetect.LangDetectException;

public class LanguageDetector {
  // private static LanguageDetector detector;
  // private static int LENGTH_THRESHOLD = 300;

  /**
   *
   */
  // private LanguageDetector() {
  //   this.initLanguages();
  // }

  /**
   *
   *
   * @return
   */
  // public static LanguageDetector instance() {
  //   if (detector == null) {
  //     detector = new LanguageDetector();
  //   }
  //   return detector;
  // }

  /**
   *
   *
   */
  // private void initLanguages() {
  //   File profiles = new File(Config.getLangDetectProfilesFolder());
  //   try {
  //     FileTools.directoryIsValid(profiles, false,
  //         "Language Detector Profiles Directory");
  //     DetectorFactory.loadProfile(profiles.getAbsolutePath());
  //   } catch (LangDetectException e) {
  //     // e.printStackTrace();
  //   } catch (FileNotFoundException e) {
  //     e.printStackTrace();
  //   }
  // }

  /**
   *
   *
   * @param stringToCheck
   * @return
   */
  // public Language guessLanguage(String stringToCheck) {
  //   Language currentLanguage = Language.UNKNOWN;

  //   if (stringToCheck.length() > LENGTH_THRESHOLD) {
  //     try {
  //       Detector detector = DetectorFactory.create();
  //       detector.append(stringToCheck);
  //       ArrayList<com.cybozu.labs.langdetect.Language> langProbabilities = detector
  //           .getProbabilities();
  //       if (langProbabilities.size() > 0) {
  //         if (langProbabilities.get(0).prob > 0.8) {
  //           currentLanguage = Language.get(langProbabilities.get(0).lang);
  //         }
  //       }
  //     } catch (Exception e) {
  //       // e.printStackTrace();
  //     }
  //   }
  //   return currentLanguage;
  // }
}
