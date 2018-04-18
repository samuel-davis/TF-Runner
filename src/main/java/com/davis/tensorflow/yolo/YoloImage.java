package com.davis.tensorflow.yolo;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 11/8/17.
 */
public class YoloImage {

  private String imagePath;
  private BufferedImage bufferedImage;
  private int[][] pixelsXy;
  private Integer width = 0;
  private Integer height = 0;
  private int[] pixels;
  private boolean hasAlphaChannel;

  public YoloImage(String imagePath, Integer yoloInputSize) throws IOException {
    this.imagePath = imagePath;
    //this.bufferedImage = ImageIO.read(new File(imagePath));;
    this.bufferedImage = resizeImage(imagePath, yoloInputSize, yoloInputSize);
    //this.pixels = new int[image.getHeight() * image.getWidth()];
    this.pixels = new int[yoloInputSize * yoloInputSize];
    this.width = bufferedImage.getWidth();
    this.height = bufferedImage.getHeight();
    this.pixelsXy = convertTo2DWithoutUsingGetRGB(this.bufferedImage);
  }

  public static BufferedImage resizeImage(String imagePath, int targetWidth, int targetHeight) {
    try {
      BufferedImage srcImage = ImageIO.read(new File(imagePath));
      double determineImageScale =
          determineImageScale(srcImage.getWidth(), srcImage.getHeight(), targetWidth, targetHeight);
      BufferedImage dstImage =
          Scalr.resize(srcImage, Scalr.Mode.FIT_EXACT, targetWidth, targetHeight);
      BufferedImage bufferedImage =
              new BufferedImage(
                      dstImage.getWidth(null), dstImage.getHeight(null), BufferedImage.TYPE_3BYTE_BGR);
      Graphics g = bufferedImage.createGraphics();
      g.drawImage(bufferedImage, 0, 0, null);
      g.dispose();
      //BufferedImage dstImage = scaleImage(srcImage, determineImageScale);
      //ImageIO.write(dstImage, "jpg", new File(pathImage));
      return bufferedImage;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static BufferedImage scaleImage(BufferedImage sourceImage, double scaledWidth) {
    Image scaledImage =
        sourceImage.getScaledInstance(
            (int) (sourceImage.getWidth() * scaledWidth),
            (int) (sourceImage.getHeight() * scaledWidth),
            Image.SCALE_SMOOTH);
    BufferedImage bufferedImage =
        new BufferedImage(
            scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_3BYTE_BGR);
    Graphics g = bufferedImage.createGraphics();
    g.drawImage(scaledImage, 0, 0, null);
    g.dispose();
    return bufferedImage;
  }

  private static double determineImageScale(
      int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
    double scalex = (double) targetWidth / sourceWidth;
    double scaley = (double) targetHeight / sourceHeight;
    return Math.min(scalex, scaley);
  }

  public int[] getPixels() {
    return pixels;
  }

  public void setPixels(int[] pixels) {
    this.pixels = pixels;
  }

  private int[][] convertTo2DWithoutUsingGetRGB(BufferedImage image) {

    final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    this.pixels = new int[image.getHeight() * image.getWidth()];
    final int width = image.getWidth();
    final int height = image.getHeight();
    final boolean hasAlphaChannel = image.getAlphaRaster() != null;
    this.hasAlphaChannel = hasAlphaChannel;
    int[][] result = new int[height][width];
    if (hasAlphaChannel) {
      final int pixelLength = 4;
      for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
        int argb = 0;
        argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
        argb += ((int) pixels[pixel + 1] & 0xff); // blue
        argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
        argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
        result[row][col] = argb;
        col++;
        if (col == width) {
          col = 0;
          row++;
        }
      }
    } else {
      int count = 0;
      final int pixelLength = 3;
      for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
        int argb = 0;
        argb += -16777216; // 255 alpha
        argb += ((int) pixels[pixel] & 0xff); // blue
        argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
        argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
        result[row][col] = argb;
        this.pixels[count] = argb;
        col++;
        if (col == width) {
          col = 0;
          row++;
        }
        count++;
      }
    }
    return result;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public BufferedImage getBufferedImage() {
    return bufferedImage;
  }

  public void setBufferedImage(BufferedImage bufferedImage) {
    this.bufferedImage = bufferedImage;
  }

  public int[][] getPixelsXy() {
    return pixelsXy;
  }

  public void setPixelsXy(int[][] pixelsXy) {
    this.pixelsXy = pixelsXy;
  }

  public boolean isHasAlphaChannel() {
    return hasAlphaChannel;
  }

  public void setHasAlphaChannel(boolean hasAlphaChannel) {
    this.hasAlphaChannel = hasAlphaChannel;
  }

  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }
}
