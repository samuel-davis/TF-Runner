/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.davis.tensorflow.object_detect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import com.davis.tensorflow.carry.RectF;
import com.davis.tensorflow.inference.Recognition;
import com.davis.tensorflow.inference.TensorFlowInferenceInterface;
import com.davis.tensorflow.yolo.YoloAndroidRunner;
import com.davis.tensorflow.yolo.YoloImage;
import org.graalvm.compiler.core.common.alloc.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.Graph;
import org.tensorflow.Operation;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * github.com/tensorflow/models/tree/master/research/object_detection
 */
public class TensorFlowObjectDetectionAPIModel {
  private static final Logger LOGGER = LoggerFactory.getLogger(TensorFlowInferenceInterface.class.getName());
  private static final int TF_OD_API_INPUT_SIZE = 300;
  // Only return this many results.
  private static final int MAX_RESULTS = 100;

  // Config values.
  private String inputName;
  private int inputSize;

  // Pre-allocated buffers.
  private Vector<String> labels = new Vector<String>();
  private int[] intValues;
  private byte[] byteValues;
  private float[] outputLocations;
  private float[] outputScores;
  private float[] outputClasses;
  private float[] outputNumDetections;
  private String[] outputNames;

  private boolean logStats = false;

  private TensorFlowInferenceInterface inferenceInterface;
  public static void main(String[] args) {
   // TensorFlowObjectDetectionAPIModel apiModel =create();

   // apiModel.recognizeImage("/home/sam/Pictures/profile.jpg");
   // apiModel.close();
  }
  /**
   * Initializes a native TensorFlow session for classifying images.
   *
   * @param modelFilename The filepath of the model GraphDef protocol buffer.
   * @param labelFilename The filepath of label file for classes.
   */
  public static TensorFlowObjectDetectionAPIModel  create(
      final String modelFilename,
      final String labelFilename,
      final int inputSize) throws IOException {
    final TensorFlowObjectDetectionAPIModel d = new TensorFlowObjectDetectionAPIModel();

    InputStream labelsInput = null;
    String actualFilename = labelFilename.split("file:///android_asset/")[1];
    // labelsInput = assetManager.open(actualFilename);
    BufferedReader br = null;
    br = new BufferedReader(new InputStreamReader(labelsInput));
    String line;
    while ((line = br.readLine()) != null) {
      LOGGER.warn(line);
      d.labels.add(line);
    }
    br.close();


    d.inferenceInterface = new TensorFlowInferenceInterface(modelFilename);

    final Graph g = d.inferenceInterface.graph();

    d.inputName = "image_tensor";
    // The inputName node has a shape of [N, H, W, C], where
    // N is the batch size
    // H = W are the height and width
    // C is the number of channels (3 for our purposes - RGB)
    final Operation inputOp = g.operation(d.inputName);
    if (inputOp == null) {
      throw new RuntimeException("Failed to find input Node '" + d.inputName + "'");
    }
    d.inputSize = inputSize;
    // The outputScoresName node has a shape of [N, NumLocations], where N
    // is the batch size.
    final Operation outputOp1 = g.operation("detection_scores");
    if (outputOp1 == null) {
      throw new RuntimeException("Failed to find output Node 'detection_scores'");
    }
    final Operation outputOp2 = g.operation("detection_boxes");
    if (outputOp2 == null) {
      throw new RuntimeException("Failed to find output Node 'detection_boxes'");
    }
    final Operation outputOp3 = g.operation("detection_classes");
    if (outputOp3 == null) {
      throw new RuntimeException("Failed to find output Node 'detection_classes'");
    }

    // Pre-allocate buffers.
    d.outputNames = new String[] {"detection_boxes", "detection_scores",
                                  "detection_classes", "num_detections"};
    d.intValues = new int[d.inputSize * d.inputSize];
    d.byteValues = new byte[d.inputSize * d.inputSize * 3];
    d.outputScores = new float[MAX_RESULTS];
    d.outputLocations = new float[MAX_RESULTS * 4];
    d.outputClasses = new float[MAX_RESULTS];
    d.outputNumDetections = new float[1];
    return d;
  }

  private TensorFlowObjectDetectionAPIModel() {}

  public List<Recognition> recognizeImage(String imagePath) {
    // Preprocess the image data from 0-255 int to normalized float based
    // on the provided parameters.
    YoloImage yoloImage = null;
    int[] pixels = null;
    try {
      yoloImage = new YoloImage(imagePath, inputSize);
      pixels = yoloImage.getPixels();
    } catch (IOException | NullPointerException e) {
      LOGGER.error("Unable to load image for detection {}", e);
    }

    for (int i = 0; i < pixels.length; ++i) {
      byteValues[i * 3 + 2] = (byte) (pixels[i] & 0xFF);
      byteValues[i * 3 + 1] = (byte) ((pixels[i] >> 8) & 0xFF);
      byteValues[i * 3 + 0] = (byte) ((pixels[i] >> 16) & 0xFF);
    }
    inferenceInterface.feed(inputName, byteValues, 1, inputSize, inputSize, 3);
    // Run the inference call.
    inferenceInterface.run(outputNames, logStats);
    // Copy the output Tensor back into the output array.
    outputLocations = new float[MAX_RESULTS * 4];
    outputScores = new float[MAX_RESULTS];
    outputClasses = new float[MAX_RESULTS];
    outputNumDetections = new float[1];
    inferenceInterface.fetch(outputNames[0], outputLocations);
    inferenceInterface.fetch(outputNames[1], outputScores);
    inferenceInterface.fetch(outputNames[2], outputClasses);
    inferenceInterface.fetch(outputNames[3], outputNumDetections);
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

    // Scale them back to the input size.
    for (int i = 0; i < outputScores.length; ++i) {
      final RectF detection =
          new RectF(
              outputLocations[4 * i + 1] * inputSize,
              outputLocations[4 * i] * inputSize,
              outputLocations[4 * i + 3] * inputSize,
              outputLocations[4 * i + 2] * inputSize);
      pq.add(
          new Recognition("" + i, labels.get((int) outputClasses[i]), outputScores[i], detection));
    }

    final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
    for (int i = 0; i < Math.min(pq.size(), MAX_RESULTS); ++i) {
      recognitions.add(pq.poll());
    }
    return recognitions;
  }

  public void enableStatLogging(final boolean logStats) {
    this.logStats = logStats;
  }

  public String getStatString() {
    return inferenceInterface.getStatString();
  }

  public void close() {
    inferenceInterface.close();
  }
}