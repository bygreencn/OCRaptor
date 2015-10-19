package mj.ocraptor.extraction.image_processing;

import ij.ImagePlus;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import javax.imageio.ImageIO;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.file_handler.utils.FileTools;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

public class ImageTools {

  private File tempFolder;
  private static final String TEMP_FILE = "t4j";
  private static final String IMAGE_TYPE = "png";
  public static final int SIZE_MULTIPLIER = 2;
  private final LinkedList<File> createdFiles;
  private Config cfg;

  /**
   *
   */
  public ImageTools() {
    this.tempFolder = FileUtils.getTempDirectory();
    this.createdFiles = new LinkedList<File>();
    this.cfg = Config.inst();
  }

  /**
   * Decode string to image
   *
   * @param imageString
   *          The string to decode
   * @return decoded image
   */
  public static BufferedImage decodeBase64ToImage(String imageString) {
    BufferedImage image = null;
    try {
      Base64 decoder = new Base64();
      byte[] imgBytes = decoder.decode(imageString);
      ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes);
      image = ImageIO.read(bis);
      bis.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return image;
  }

  /**
   *
   *
   * @param img
   * @return
   */
  public BufferedImage fileToBufferedImage(File img) {
    BufferedImage newImage = null;
    if (img != null) {
      try {
        BufferedImage in = ImageIO.read(img);
        // TODO: neccessary ???
        newImage = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return newImage;
  }

  /**
   *
   *
   * @param img
   * @return
   */
  public static BufferedImage toBufferedImage(Image img) {
    // TODO:
    if (img instanceof BufferedImage) {
      return (BufferedImage) img;
    }

    // Create a buffered image with transparency
    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null),
        BufferedImage.TYPE_INT_ARGB);

    // Draw the image on to the buffered image
    Graphics2D bGr = bimage.createGraphics();
    bGr.drawImage(img, 0, 0, null);
    bGr.dispose();

    // Return the buffered image
    return bimage;
  }

  /**
   *
   *
   */
  public void cleanCreatedFiles() {
    for (File file : this.createdFiles) {
      if (file.exists() && file.isFile()) {
        file.delete();
      }
    }
    this.createdFiles.clear();
  }

  /**
   *
   *
   * @param image
   * @param sizeMultiplier
   * @return
   *
   * @throws Exception
   */
  public BufferedImage preprocessForOCR(BufferedImage image, boolean resize) {
    try {
      if (this.cfg.getProp(ConfigBool.PRE_PROCESS_IMAGES_FOR_OCR)) {
        int targetWidth = image.getWidth() * SIZE_MULTIPLIER;
        int targetHeight = image.getHeight() * SIZE_MULTIPLIER;

        // imagej-api
        ImagePlus imageJImage = new ImagePlus("image", image);
        ImageConverter converter = new ImageConverter(imageJImage);
        converter.convertToGray8();

        // resizing image
        ImageProcessor ip = imageJImage.getProcessor();
        if (resize) {
          ip.setInterpolationMethod(ImageProcessor.BILINEAR);
          ip = ip.resize(targetWidth, targetHeight);
        }

        // // adjusting contrast
        // ContrastEnhancer contrast = new ContrastEnhancer();
        // contrast.stretchHistogram(ip, 2.0);

        // // unsharp mask
        // FloatProcessor fp = null;
        // UnsharpMask us = new UnsharpMask();
        // for (int i = 0; i < ip.getNChannels(); i++) {
        // fp = ip.toFloat(i, fp);
        // fp.snapshot();
        // us.sharpenFloat(fp, 4.0, (float) 0.7);
        // ip.setPixels(i, fp);
        // }

        // // blur for better ocr-results
        // GaussianBlur blur = new GaussianBlur();
        // blur.blurGaussian(ip, 0.5, 0.5, 0.01);

        // DEBUG:
        // if (this.cfg.getBooleanFromProperty(ConfigBool.DEBUG_MODE)) {
        // bufferedImageToFile(ip.getBufferedImage());
        // }

        return ip.getBufferedImage();
      }
    } catch (Exception e) {
      // TODO:
      if (this.cfg.getProp(ConfigBool.DEBUG_MODE)) {
        e.printStackTrace();
      }
    }
    return image;
  }

  /**
   *
   *
   * @param image
   * @return
   *
   * @throws IOException
   */
  public File bufferedImageToFile(BufferedImage image) {
    File outputFile = null;
    try {
      String randomFileName = TEMP_FILE + "_" + new Random().nextInt(Integer.MAX_VALUE) + "."
          + IMAGE_TYPE;
      outputFile = new File(tempFolder, randomFileName);

      // ImagePlus imageJImage = new ImagePlus("image", image);
      // FileSaver.setJpegQuality(90);
      // FileSaver saver = new FileSaver(imageJImage);
      // saver.saveAsJpeg(outputFile.getAbsolutePath());

      ImageIO.write(image, IMAGE_TYPE, outputFile);

      if (outputFile.exists()) {
        return outputFile;
      }
    } catch (Exception e1) {
      // e1.printStackTrace();
    } finally {
      if (outputFile != null) {
        this.createdFiles.add(outputFile);
      }
    }
    return null;
  }

  /**
   *
   *
   * @param file
   * @return
   */
  public static boolean imageFileSizeValid(File file) {
    if (file != null) {
      final int minSize = Config.inst().getProp(ConfigInteger.MIN_IMAGE_HEIGHT_FOR_OCR);
      final int maxSize = Config.inst().getProp(ConfigInteger.MAX_IMAGE_HEIGHT_FOR_OCR);
      final double fileSize = FileTools.getFileSizeInKB(file);
      if (fileSize > minSize && fileSize < maxSize) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   *
   * @param image
   * @return
   */
  public static boolean imageSizeValid(BufferedImage image) {
    return imageSizeValid(image.getHeight(), image.getWidth());
  }

  /**
   *
   *
   * @param height
   * @param width
   * @return
   */
  public static boolean imageSizeValid(int height, int width) {
    int minHeight = Config.inst().getProp(ConfigInteger.MIN_IMAGE_HEIGHT_FOR_OCR);
    int maxHeight = Config.inst().getProp(ConfigInteger.MAX_IMAGE_HEIGHT_FOR_OCR);

    int minWidth = Config.inst().getProp(ConfigInteger.MIN_IMAGE_WIDTH_FOR_OCR);
    int maxWidth = Config.inst().getProp(ConfigInteger.MAX_IMAGE_WIDTH_FOR_OCR);

    if (height > minHeight && width > minWidth && height < maxHeight && width < maxWidth) {
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param imageCount
   * @param pageCount
   * @return
   */
  public static boolean imagePerPageRatioValid(int imageCount, int pageCount) {
    if (imageCount / pageCount < Config.inst().getProp(ConfigInteger.MAX_IMAGE_PER_PAGE_RATIO)) {
      return true;
    }
    return false;
  }
}
