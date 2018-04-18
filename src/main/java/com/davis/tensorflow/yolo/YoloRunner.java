package com.davis.tensorflow.yolo;

import com.davis.tensorflow.inference.TensorFlowInferenceInterface;
import com.davis.tensorflow.utils.NativeUtils;
import com.davis.tensorflow.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class YoloRunner {
  private static final Logger logger = LoggerFactory.getLogger(YoloRunner.class.getName());

  private TensorFlowInferenceInterface inference;


  public TensorFlowInferenceInterface getInference() {
    return inference;
  }

  public void setInference(TensorFlowInferenceInterface inference) {
    this.inference = inference;
  }

  public static void main(String[] args) {
    YoloRunner yoloRunner = new YoloRunner();
    String modelDir = YoloRunner.class.getClassLoader().getResource("yolo.pb").getFile();
    yoloRunner.setInference(new TensorFlowInferenceInterface(modelDir));
    String imageFile ="/home/sam/Pictures/ISIS.jpg";

    Path labelPath =
        Paths.get(
            YoloRunner.class
                .getClassLoader()
                .getResource("mobilenet-1-224-nextgen-5000-labels.txt")
                .getFile());
    List<String> labels = readAllLinesOrExit(labelPath);

      String outputJpegName = addPlaceHoldersAndJpegDecoding(yoloRunner.getInference().graph());
      List<byte[]> imageBytesFromDir = getImageBytesForImagesInDir("/home/sam/Pictures");
      //byte[] imageBytes = readAllBytesOrExit(Paths.get(imageFile));

      try (Session session = new Session(yoloRunner.getInference().graph())) {
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



  private static List<byte[]> getImageBytesForImagesInDir(String pathToDir) {
    List<byte[]> images = new ArrayList<>();
    List<Path> paths = PathUtils.getFilePaths(pathToDir);
    for (Path p : paths) {
      if (p.toFile().getName().endsWith("jpg") || p.toFile().getName().endsWith("png")) {
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
          s.runner().feed("input", image).fetch("output").run().get(0).expect(Float.class);

      final long[] rshape = result.shape();
      /*if (result.numDimensions() != 2 || rshape[0] != 1) {
        throw new RuntimeException(
            String.format(
                "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                Arrays.toString(rshape)));
      }*/
      int nlabels = (int) rshape[1];
      return result.copyTo(new float[1][nlabels])[0];
    }
  }


  private static String addPlaceHoldersAndJpegDecoding(Graph graph) {
    final int H = 224;
    final int W = 224;
    final float mean = 117f;
    final float scale = 1f;
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
