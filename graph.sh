python -m tensorflow.python.tools.optimize_for_inference \
  --input=/home/sam/projects/dev-personal/ml-uilities/TF-Runner/src/main/resources/NS_45000-TB_100-LR_0.01.pb \
  --output=/home/sam/projects/dev-personal/ml-uilities/TF-Runner/src/main/resources/optimized_NS_45000-TB_100-LR_0.01.pb \
  --input_names="Cast" \
  --output_names="final_result"



bazel build tensorflow/tools/graph_transforms:transform_graph
bazel-bin/tensorflow/tools/graph_transforms/transform_graph \
--in_graph=/home/sam/projects/dev-personal/ml-uilities/TF-Runner/src/main/resources/NS_45000-TB_100-LR_0.01.pb \
--out_graph=/home/sam/projects/dev-personal/ml-uilities/TF-Runner/src/main/resources/O_NS_45000-TB_100-LR_0.01.pb \
--inputs='Mul' \
--outputs='final_result'

bazel-bin/tensorflow/python/tools/optimize_for_inference \
--in_graph=/home/sam/projects/dev-personal/ml-uilities/TF-Runner/src/main/resources/NS_45000-TB_100-LR_0.01.pb \
--out_graph=/home/sam/projects/dev-personal/ml-uilities/TF-Runner/src/main/resources/O_NS_45000-TB_100-LR_0.01.pb \
--inputs='Mul' \
--outputs='final_result'