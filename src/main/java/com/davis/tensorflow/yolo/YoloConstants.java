package com.davis.tensorflow.yolo;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 11/12/17.
 */
public class YoloConstants {

  //"inp_size": [ 608,608,3]
  public static final int YOLO_V2_INPUT_SIZE = 608;
  //out_size": [19,19,425]
  public static final int YOLO_V2_OUTPUT_SIZE = 608;

  public static final int MAX_RESULTS = 5;

  public static final int NUM_CLASSES = 80;

  public static final int NUM_BOXES_PER_BLOCK = 5;
  public static final double[] TINY_ANCHORS = {
    0.738768, 0.874946,
    2.42204, 2.65704,
    4.30971, 7.04493,
    10.246, 4.59428,
    12.6868, 11.8741
  };
  public static final double[] YOLO_V2_ANCHORS = {
    0.57273, 0.677385, 1.87446, 2.06253, 3.33843, 5.47434, 7.88282, 3.52778, 9.77052, 9.16828
  };

  public static final double[] YOLO_VOC_ANCHORS = {
    1.08, 1.19,
    3.42, 4.41,
    6.63, 11.38,
    9.42, 5.11,
    16.62, 10.52
  };

  public static final String[] COCO_LABELS = {
    "person",
    "bicycle",
    "car",
    "motorbike",
    "aeroplane",
    "bus",
    "train",
    "truck",
    "boat",
    "traffic light",
    "fire hydrant",
    "stop sign",
    "parking meter",
    "bench",
    "bird",
    "cat",
    "dog",
    "horse",
    "sheep",
    "cow",
    "elephant",
    "bear",
    "zebra",
    "giraffe",
    "backpack",
    "umbrella",
    "handbag",
    "tie",
    "suitcase",
    "frisbee",
    "skis",
    "snowboard",
    "sports ball",
    "kite",
    "baseball bat",
    "baseball glove",
    "skateboard",
    "surfboard",
    "tennis racket",
    "bottle",
    "wine glass",
    "cup",
    "fork",
    "knife",
    "spoon",
    "bowl",
    "banana",
    "apple",
    "sandwich",
    "orange",
    "broccoli",
    "carrot",
    "hot dog",
    "pizza",
    "donut",
    "cake",
    "chair",
    "sofa",
    "pottedplant",
    "bed",
    "diningtable",
    "toilet",
    "tvmonitor",
    "laptop",
    "mouse",
    "remote",
    "keyboard",
    "cell phone",
    "microwave",
    "oven",
    "toaster",
    "sink",
    "refrigerator",
    "book",
    "clock",
    "vase",
    "scissors",
    "teddy bear",
    "hair drier",
    "toothbrush"
  };

  public static final String[] YOLO_VOC_LABELS = {
    "aeroplane",
    "bicycle",
    "bird",
    "boat",
    "bottle",
    "bus",
    "car",
    "cat",
    "chair",
    "cow",
    "diningtable",
    "dog",
    "horse",
    "motorbike",
    "person",
    "pottedplant",
    "sheep",
    "sofa",
    "train",
    "tvmonitor"
  };

  public static final int YOLO_INPUT_SIZE = 416;
  public static final String YOLO_INPUT_NAME = "input";
  public static final String YOLO_OUTPUT_NAMES = "output";
  public static final int YOLO_BLOCK_SIZE = 32;
}
