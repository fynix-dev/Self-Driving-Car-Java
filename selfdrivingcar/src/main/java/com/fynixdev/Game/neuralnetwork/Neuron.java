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

    // public void mutate(double mutationRate){
    //     for(int i = 0; i < weights.length; i++){
    //         weights[i] = Utils.lerp(weights[i], Math.random() * 2 - 1, mutationRate);
    //     }
    //     bias = Utils.lerp(bias, Math.random() * 2 - 1, mutationRate);
    // }

    public void mutate(double mutationRate) {
        for (int i = 0; i < weights.length; i++) {
            if (Math.random() < mutationRate) {
                weights[i] += Math.random() * 0.4 - 0.2;
            }
        }

        if (Math.random() < mutationRate) {
            bias += Math.random() * 0.4 - 0.2;
        }
    }

    public void copy(Neuron other){
        this.weights = Arrays.copyOf(other.weights, other.weights.length);
        this.bias = other.bias;
        this.activationFunction = other.activationFunction;
    }

}
