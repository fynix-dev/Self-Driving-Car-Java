package com.fynixdev.Game.neuralnetwork;

import java.util.Arrays;

import com.fynixdev.Game.neuralnetwork.activationfunction.ActivationFunction;
import com.fynixdev.utils.Utils;

public class Neuron implements java.io.Serializable {
    double[] weights;
    double bias, output;
    ActivationFunction activationFunction;

    public Neuron(int inputCount,  ActivationFunction aFunction){
        this.activationFunction = aFunction;
        this.weights = new double[inputCount];
        for(int i = 0; i < inputCount; i++){
            weights[i] = Math.random() * 2 - 1;
        }
        this.bias = Math.random() * 2 - 1;
    }

    public double feedForward(double[] inputs){
        double sum = bias;
        for(int i = 0; i < inputs.length; i++){
            sum += weights[i] * inputs[i];
        }
        this.output =  activationFunction.activate(sum);
        return output;
    }

    public void mutate(double mutationRate, double mutationStrength) {
        for (int i = 0; i < weights.length; i++) {
            if (Math.random() < mutationRate) {
                weights[i] += (Math.random() * 2 - 1) * mutationStrength;
            }
        }

        if (Math.random() < mutationRate) {
            bias += (Math.random() * 2 - 1) * mutationStrength;
        }
    }

    public void copy(Neuron other){
        this.weights = Arrays.copyOf(other.weights, other.weights.length);
        this.bias = other.bias;
        this.activationFunction = other.activationFunction;
    }

    public void crossOver(double[] parent1Weights, double[] parent2Weights, double parent1Bias, double parent2Bias) {
        for (int i = 0; i < weights.length; i++) {
            weights[i] = Math.random() < 0.5 ? parent1Weights[i] : parent2Weights[i];
        }
        bias = Math.random() < 0.5 ? parent1Bias : parent2Bias;
    }

}
