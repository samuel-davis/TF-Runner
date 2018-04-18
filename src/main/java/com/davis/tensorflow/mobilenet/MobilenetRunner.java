package com.davis.tensorflow.mobilenet; /* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

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

import com.davis.tensorflow.inception.LabelImage;
import com.davis.tensorflow.utils.NativeUtils;
import com.davis.tensorflow.utils.PathUtils;
import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;
import org.tensorflow.types.UInt8;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Sample use of the TensorFlow Java API to label images using a pre-trained model. */
public class MobilenetRunner {
  static {
    try {
      NativeUtils.loadLibraryFromJar("/linux-x86_64/libtensorflow_jni.so");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void printUsage(PrintStream s) {
    final String url =
        "https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip";
    s.println(
        "Java program that uses a pre-trained Inception model (http://arxiv.org/abs/1512.00567)");
    s.println("to label JPEG images.");
    s.println("TensorFlow version: " + TensorFlow.version());
    s.println();
    s.println("Usage: label_image <model dir> <image file>");
    s.println();
    s.println("Where:");
    s.println("<model dir> is a directory containing the unzipped contents of the inception model");
    s.println("            (from " + url + ")");
    s.println("<image file> is the path to a JPEG image file");
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      printUsage(System.err);
      System.exit(1);
    }
    String modelDir =
            MobilenetRunner.class.getClassLoader().getResource("mobilenet-1-224-nextgen-5000.pb").getFile();
    Path modelPath = Paths.get(modelDir);
    byte[] graphDef = readAllBytesOrExit(modelPath);

    String imageFile = args[1];

    Path labelPath =
        Paths.get(
            LabelImage.class
                .getClassLoader()
                .getResource("mobilenet-1-224-nextgen-5000-labels.txt")
                .getFile());
    List<String> labels = readAllLinesOrExit(labelPath);
    try (Graph graph = loadGraph(graphDef)) {
      String outputJpegName = addPlaceHoldersAndJpegDecoding(graph);
      List<byte[]> imageBytesFromDir = getImageBytesForImagesInDir("/home/sam/Pictures");
      //byte[] imageBytes = readAllBytesOrExit(Paths.get(imageFile));

      try (Session session = new Session(graph)) {
        for (byte[] imageBytes : imageBytesFromDir) {
          float[] labelProbabilities = executeInference(session, imageBytes, outputJpegName);
          int bestLabelIdx = maxIndex(labelProbabilities);
          System.out.println(
              String.format(
                  "BEST MATCH: %s (%.2f%% likely)",
                  labels.get(bestLabelIdx), labelProbabilities[bestLabelIdx] * 100f));
        }
      }
    }
  }

  private static List<byte[]> getImageBytesForImagesInDir(String pathToDir) {
    List<byte[]> images = new ArrayList<>();
    List<Path> paths = PathUtils.getFilePaths(pathToDir);
    for (Path p : paths) {
      if(p.toFile().getName().endsWith("jpg") || p.toFile().getName().endsWith("png")){
        images.add(readAllBytesOrExit(p));
      }

    }
    return images;
  }

  private static float[] executeInference(Session s, byte[] imageBytes, String outputJpegName) {
    try (Tensor stringImagePathTensor = Tensor.create(imageBytes)) {
      Tensor<Float> image =
          s.runner()
              .feed("DecodeJPGInput", stringImagePathTensor)
              .fetch(outputJpegName)
              .run()
              .get(0)
              .expect(Float.class);
      //    Tensor<Float> result =s.runner().feed("input", image).fetch("output").run().get(0).expect(Float.class))

      Tensor<Float> result =
          s.runner().feed("input", image).fetch("final_result").run().get(0).expect(Float.class);

      final long[] rshape = result.shape();
      if (result.numDimensions() != 2 || rshape[0] != 1) {
        throw new RuntimeException(
            String.format(
                "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                Arrays.toString(rshape)));
      }
      int nlabels = (int) rshape[1];
      return result.copyTo(new float[1][nlabels])[0];
    }
  }

  private static Graph loadGraph(byte[] graphBytes) {
    Graph graph = new Graph();
    graph.importGraphDef(graphBytes);
    return graph;
  }

  private static String addPlaceHoldersAndJpegDecoding(Graph graph) {
    final int H = 224;
    final int W = 224;
    final float mean = 117f;
    final float scale = 1f;
    // jpeg_data = tf.placeholder(tf.string, name='DecodeJPGInput')
    // decoded_image = tf.image.decode_jpeg(jpeg_data, channels=input_depth)
    // decoded_image_as_float = tf.cast(decoded_image, dtype=tf.float32)
    // decoded_image_4d = tf.expand_dims(decoded_image_as_float, 0)
    // resize_shape = tf.stack([input_height, input_width])
    // resize_shape_as_int = tf.cast(resize_shape, dtype=tf.int32)
    // resized_image = tf.image.resize_bilinear(decoded_image_4d,resize_shape_as_int)
    // offset_image = tf.subtract(resized_image, input_mean)
    // mul_image = tf.multiply(offset_image, 1.0 / input_std)
    Output<String> jpegData =
        graph
            .opBuilder("Placeholder", "DecodeJPGInput")
            .setAttr("dtype", DataType.STRING)
            .build()
            .output(0);

    GraphBuilder b = new GraphBuilder(graph);

    Output<Float> out =
        b.div(
            b.sub(
                b.resizeBilinear(
                    b.expandDims(
                        b.cast(b.decodeJpeg(jpegData, 3), Float.class),
                        b.constant("make_batch", 0)),
                    b.constant("size", new int[] {H, W})),
                b.constant("mean", mean)),
            b.constant("scale", scale));

    return out.op().name();
  }

  private static Tensor<Float> constructAndExecuteGraphToNormalizeImage(byte[] imageBytes) {
    try (Graph g = new Graph()) {
      GraphBuilder b = new GraphBuilder(g);
      // Some constants specific to the pre-trained model at:
      // https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip
      //
      // - The model was trained with images scaled to 224x224 pixels.
      // - The colors, represented as R, G, B in 1-byte each were converted to
      //   float using (value - Mean)/Scale.
      final int H = 224;
      final int W = 224;
      final float mean = 117f;
      final float scale = 1f;

      // Since the graph is being constructed once per execution here, we can use a constant for the
      // input image. If the graph were to be re-used for multiple input images, a placeholder would
      // have been more appropriate.

      //Output<String> inputX = g.opBuilder("Placeholder","input_p").setAttr("dtype", DataType.FLOAT).build().output(0);

      final Output<String> input = b.constant("input", imageBytes);
      final Output<Float> output =
          b.div(
              b.sub(
                  b.resizeBilinear(
                      b.expandDims(
                          b.cast(b.decodeJpeg(input, 3), Float.class), b.constant("make_batch", 0)),
                      b.constant("size", new int[] {H, W})),
                  b.constant("mean", mean)),
              b.constant("scale", scale));
      try (Session s = new Session(g)) {
        return s.runner().fetch(output.op().name()).run().get(0).expect(Float.class);
      }
    }
  }

  private static float[] executeInceptionGraph(byte[] graphDef, Tensor<Float> image) {
    try (Graph g = new Graph()) {
      g.importGraphDef(graphDef);
      try (Session s = new Session(g);

          //    Tensor<Float> result =s.runner().feed("input", image).fetch("output").run().get(0).expect(Float.class))
          Tensor<Float> result =
              s.runner()
                  .feed("input", image)
                  .fetch("final_result")
                  .run()
                  .get(0)
                  .expect(Float.class)) {
        final long[] rshape = result.shape();
        if (result.numDimensions() != 2 || rshape[0] != 1) {
          throw new RuntimeException(
              String.format(
                  "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                  Arrays.toString(rshape)));
        }
        int nlabels = (int) rshape[1];
        return result.copyTo(new float[1][nlabels])[0];
      }
    }
  }

  private static int maxIndex(float[] probabilities) {
    int best = 0;
    for (int i = 1; i < probabilities.length; ++i) {
      if (probabilities[i] > probabilities[best]) {
        best = i;
      }
    }
    return best;
  }

  private static byte[] readAllBytesOrExit(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      System.err.println("Failed to read [" + path + "]: " + e.getMessage());
      System.exit(1);
    }
    return null;
  }

  private static List<String> readAllLinesOrExit(Path path) {
    try {
      return Files.readAllLines(path, Charset.forName("UTF-8"));
    } catch (IOException e) {
      System.err.println("Failed to read [" + path + "]: " + e.getMessage());
      System.exit(0);
    }
    return null;
  }

  // In the fullness of time, equivalents of the methods of this class should be auto-generated from
  // the OpDefs linked into libtensorflow_jni.so. That would match what is done in other languages
  // like Python, C++ and Go.
  static class GraphBuilder {
    private Graph g;

    GraphBuilder(Graph g) {
      this.g = g;
    }

    Output<Float> div(Output<Float> x, Output<Float> y) {
      return binaryOp("Div", x, y);
    }

    <T> Output<T> sub(Output<T> x, Output<T> y) {
      return binaryOp("Sub", x, y);
    }

    <T> Output<Float> resizeBilinear(Output<T> images, Output<Integer> size) {
      return binaryOp3("ResizeBilinear", images, size);
    }

    <T> Output<T> expandDims(Output<T> input, Output<Integer> dim) {
      return binaryOp3("ExpandDims", input, dim);
    }

    <T, U> Output<U> cast(Output<T> value, Class<U> type) {
      DataType dtype = DataType.fromClass(type);
      return g.opBuilder("Cast", "Cast")
          .addInput(value)
          .setAttr("DstT", dtype)
          .build()
          .<U>output(0);
    }

    Output<UInt8> decodeJpeg(Output<String> contents, long channels) {
      return g.opBuilder("DecodeJpeg", "DecodeJpeg")
          .addInput(contents)
          .setAttr("channels", channels)
          .build()
          .<UInt8>output(0);
    }

    <T> Output<T> constant(String name, Object value, Class<T> type) {
      try (Tensor<T> t = Tensor.<T>create(value, type)) {
        return g.opBuilder("Const", name)
            .setAttr("dtype", DataType.fromClass(type))
            .setAttr("value", t)
            .build()
            .<T>output(0);
      }
    }

    Output<String> constant(String name, byte[] value) {
      return this.constant(name, value, String.class);
    }

    Output<Integer> constant(String name, int value) {
      return this.constant(name, value, Integer.class);
    }

    Output<Integer> constant(String name, int[] value) {
      return this.constant(name, value, Integer.class);
    }

    Output<Float> constant(String name, float value) {
      return this.constant(name, value, Float.class);
    }

    private <T> Output<T> binaryOp(String type, Output<T> in1, Output<T> in2) {
      return g.opBuilder(type, type).addInput(in1).addInput(in2).build().<T>output(0);
    }

    private <T, U, V> Output<T> binaryOp3(String type, Output<U> in1, Output<V> in2) {
      return g.opBuilder(type, type).addInput(in1).addInput(in2).build().<T>output(0);
    }
  }
}
