package com.lookify.Lookify;

import com.google.gson.Gson;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.zoo.model.VGG16;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ImageRecognition {
    public static String StartImageRecognition(String imagePath) {
        String returnLablel = "";
        try {
            // Load the pre-trained VGG16 model
            ComputationGraph vgg16 = (ComputationGraph) VGG16.builder().build().initPretrained();
            // Load and preprocess the image
            NativeImageLoader loader = new NativeImageLoader(224, 224, 3);
            INDArray image = loader.asMatrix(new File(imagePath));
            DataNormalization scaler = new VGG16ImagePreProcessor();
            scaler.transform(image);
            // Perform forward pass on the model to get the predictions
            INDArray[] outputs = vgg16.output(false, image);
            // Get the labels for the predictions
            String jsonString = new Scanner(new File("labels.txt")).useDelimiter("\\Z").next();
            Gson gson = new Gson();
            List<String> words = new ArrayList<>();
            // Parse the JSON string and extract the words
            try {
                // Convert the JSON string to a Map
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, String[]>>() {
                }.getType();
                Map<String, String[]> data = gson.fromJson(jsonString, type);

                // Iterate over the entries and extract the words
                for (Map.Entry<String, String[]> entry : data.entrySet()) {
                    String word = entry.getValue()[1]; // Retrieve the second element of the array
                    words.add(word);
                }
                // Print the extracted words
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Get the predicted classes and probabilities
            INDArray predictions = outputs[0];
            int[] top5Classes = predictions.argMax(1).toIntVector();
            double[] top5Probabilities = predictions.getRow(0).toDoubleVector();
            // Print the top 5 predicted labels and probabilities
            int predictedClass = top5Classes[0];
            double probability = top5Probabilities[predictedClass];
            String label = words.get(predictedClass);
            returnLablel = label;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnLablel;
    }
}