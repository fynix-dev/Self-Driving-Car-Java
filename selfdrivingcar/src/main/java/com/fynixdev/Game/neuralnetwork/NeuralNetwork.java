package com.fynixdev.Game.neuralnetwork;

import java.util.ArrayList;

public class NeuralNetwork {
    private final ArrayList<Layer> layers = new ArrayList<>();

    public void addLayer(Layer layer){
        layers.add(layer);
    }

    public double[] feedForward(double[] inputs){
        double[] currentOutput = inputs;
        for(Layer layer: layers){
            currentOutput = layer.feedForward(currentOutput);
        }
        return currentOutput;
    }
}
