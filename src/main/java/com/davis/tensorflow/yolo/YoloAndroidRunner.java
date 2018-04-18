package com.davis.tensorflow.yolo;

import com.davis.tensorflow.inference.Recognition;
import com.davis.tensorflow.utils.SplitTimer;
import com.davis.tensorflow.inference.TensorFlowInferenceInterface;
import com.davis.tensorflow.carry.RectF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static com.davis.tensorflow.yolo.YoloConstants.COCO_LABELS;
import static com.davis.tensorflow.yolo.YoloConstants.MAX_RESULTS;
import static com.davis.tensorflow.yolo.YoloConstants.NUM_BOXES_PER_BLOCK;
import static com.davis.tensorflow.yolo.YoloConstants.NUM_CLASSES;
import static com.davis.tensorflow.yolo.YoloConstants.YOLO_BLOCK_SIZE;
import static com.davis.tensorflow.yolo.YoloConstants.YOLO_INPUT_NAME;
import static com.davis.tensorflow.yolo.YoloConstants.YOLO_INPUT_SIZE;
import static com.davis.tensorflow.yolo.YoloConstants.YOLO_OUTPUT_NAMES;
import static com.davis.tensorflow.yolo.YoloConstants.YOLO_V2_ANCHORS;
import static com.davis.tensorflow.yolo.YoloConstants.YOLO_V2_INPUT_SIZE;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 11/8/17.
 */
public class YoloAndroidRunner {
  private static final Logger logger = LoggerFactory.getLogger(YoloAndroidRunner.class.getName());

  private static final String YOLO_MODEL_FILE =
      "/home/sam/projects/dev-personal/ml-uilities/TF-Runner/src/main/resources/yolo.pb";

  // Config values.
  private String inputName;
  private int inputSize;
  private TensorFlowInferenceInterface inferenceInterface;
  // Pre-allocated buffers.
  private int[] intValues;
  private float[] floatValues;
  private String[] outputNames;

  private int blockSize;

  private boolean logStats = false;

  public static void main(String[] args) {
    YoloAndroidRunner runner = new YoloAndroidRunner();
    setupYoloRunner(
        YOLO_MODEL_FILE,
        YOLO_V2_INPUT_SIZE,
        YOLO_INPUT_NAME,
        YOLO_OUTPUT_NAMES,
        YOLO_BLOCK_SIZE,
        runner);
    runner.recognizeImage("/home/sam/Pictures/profile.jpg");
    runner.close();
  }

  public static void setupYoloRunner(
      final String modelFilename,
      final int inputSize,
      final String inputName,
      final String outputName,
      final int blockSize,
      YoloAndroidRunner runner) {
    runner.inputName = inputName;
    runner.inputSize = inputSize;
    // Pre-allocate buffers.
    runner.outputNames = outputName.split(",");
    runner.intValues = new int[inputSize * inputSize];
    runner.floatValues = new float[inputSize * inputSize * 3];
    runner.blockSize = blockSize;
    runner.inferenceInterface = new TensorFlowInferenceInterface(modelFilename);
  }

  public void enableStatLogging(final boolean logStats) {
    this.logStats = logStats;
  }

  public void close() {
    this.inferenceInterface.close();
  }

  public String getStatString() {
    return inferenceInterface.getStatString();
  }

  public List<Recognition> recognizeImage(String imagePath) {
    final SplitTimer timer = new SplitTimer("recognizeImage");

    // Log this method so that it can be analyzed with systrace.
    // Preprocess the image data from 0-255 int to normalized float based
    // on the provided parameters.

    YoloImage yoloImage = null;
    int[] pixels = null;
    try {
      yoloImage = new YoloImage(imagePath, YOLO_V2_INPUT_SIZE);
      pixels = yoloImage.getPixels();
    } catch (IOException | NullPointerException e) {
      logger.error("Unable to load image for detection {}", e);
    }
    //this.intValues = new int[yoloImage.getHeight() * yoloImage.getWidth()];
    this.floatValues = new float[pixels.length * 3];

    for (int i = 0; i < pixels.length; ++i) {
      floatValues[i * 3 + 0] = ((pixels[i] >> 16) & 0xFF) / 255.0f;
      floatValues[i * 3 + 1] = ((pixels[i] >> 8) & 0xFF) / 255.0f;
      floatValues[i * 3 + 2] = (pixels[i] & 0xFF) / 255.0f;
    }

    // Copy the input data into TensorFlow.
    inferenceInterface.feed(inputName, floatValues, 1, yoloImage.getWidth(), yoloImage.getHeight(), 3);
    //inferenceInterface.feed(inputName, floatValues, 1, YOLO_INPUT_SIZE, YOLO_INPUT_SIZE, 3);
    //Trace.endSection();

    timer.endSplit("ready for inference");

    // Run the inference call.
    inferenceInterface.run(outputNames, logStats);
    //Trace.endSection();

    timer.endSplit("ran inference");

    // Copy the output Tensor back into the output array.
    final int gridWidth = yoloImage.getWidth() / blockSize;
    final int gridHeight = yoloImage.getHeight() / blockSize;
    final float[] output =
        new float[gridWidth * gridHeight * (NUM_CLASSES + 5) * NUM_BOXES_PER_BLOCK];
    inferenceInterface.fetch(outputNames[0], output);

    // Find the best detections.
    final PriorityQueue<Recognition> pq =
        new PriorityQueue<Recognition>(
            1,
            new Comparator<Recognition>() {
              @Override
              public int compare(final Recognition lhs, final Recognition rhs) {
                // Intentionally reversed to put high confidence at the head of the queue.
                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
              }
            });

    for (int y = 0; y < gridHeight; ++y) {
      for (int x = 0; x < gridWidth; ++x) {
        for (int b = 0; b < NUM_BOXES_PER_BLOCK; ++b) {
          final int offset =
              (gridWidth * (NUM_BOXES_PER_BLOCK * (NUM_CLASSES + 5))) * y
                  + (NUM_BOXES_PER_BLOCK * (NUM_CLASSES + 5)) * x
                  + (NUM_CLASSES + 5) * b;

          final float xPos = (x + expit(output[offset + 0])) * blockSize;
          final float yPos = (y + expit(output[offset + 1])) * blockSize;

          final float w = (float) (Math.exp(output[offset + 2]) * YOLO_V2_ANCHORS[2 * b + 0]) * blockSize;
          final float h = (float) (Math.exp(output[offset + 3]) * YOLO_V2_ANCHORS[2 * b + 1]) * blockSize;

          final RectF rect =
              new RectF(
                  Math.max(0, xPos - w / 2),
                  Math.max(0, yPos - h / 2),
                  Math.min(yoloImage.getWidth() - 1, xPos + w / 2),
                  Math.min(yoloImage.getHeight() - 1, yPos + h / 2));
          final float confidence = expit(output[offset + 4]);

          int detectedClass = -1;
          float maxClass = 0;

          final float[] classes = new float[NUM_CLASSES];
          for (int c = 0; c < NUM_CLASSES; ++c) {
            classes[c] = output[offset + 5 + c];
          }
          softmax(classes);

          for (int c = 0; c < NUM_CLASSES; ++c) {
            if (classes[c] > maxClass) {
              detectedClass = c;
              maxClass = classes[c];
            }
          }

          final float confidenceInClass = maxClass * confidence;
          if (confidenceInClass > 0.01) {
            logger.info(
                "{} ({}) {} {}", COCO_LABELS[detectedClass], detectedClass, confidenceInClass, rect);
            pq.add(new Recognition("" + offset, COCO_LABELS[detectedClass], confidenceInClass, rect));
          }
        }
      }
    }
    timer.endSplit("decoded results");

    final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
    for (int i = 0; i < Math.min(pq.size(), MAX_RESULTS); ++i) {
      recognitions.add(pq.poll());
    }
    //Trace.endSection(); // "recognizeImage"

    timer.endSplit("processed results");

    return recognitions;
  }

  private float expit(final float x) {
    return (float) (1. / (1. + Math.exp(-x)));
  }

  private void softmax(final float[] vals) {
    float max = Float.NEGATIVE_INFINITY;
    for (final float val : vals) {
      max = Math.max(max, val);
    }
    float sum = 0.0f;
    for (int i = 0; i < vals.length; ++i) {
      vals[i] = (float) Math.exp(vals[i] - max);
      sum += vals[i];
    }
    for (int i = 0; i < vals.length; ++i) {
      vals[i] = vals[i] / sum;
    }
  }
}
