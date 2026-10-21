package com.fynixdev.Game.neuralnetwork;

import java.util.Arrays;

import com.fynixdev.Game.neuralnetwork.activationfunction.ActivationFunction;

public class Neuron {
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
}
