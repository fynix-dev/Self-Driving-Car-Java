package com.fynixdev.Game.neuralnetwork;

import java.util.ArrayList;

public class NeuralNetwork implements java.io.Serializable{
    private final ArrayList<Layer> layers = new ArrayList<>();
    public double bestFitness = 0.0;

    public NeuralNetwork(double bestFitness){
        this.bestFitness = bestFitness;
    }

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

    public void mutate(double mutationRate){
        for(Layer layer: layers){
            layer.mutate(mutationRate);
        }
    }

    public void copy(NeuralNetwork other){
        for(int i = 0; i < layers.size(); i++){
            this.layers.get(i).copy(other.layers.get(i));
        }
    }

}
