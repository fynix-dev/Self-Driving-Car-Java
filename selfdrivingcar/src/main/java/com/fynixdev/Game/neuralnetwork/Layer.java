package com.fynixdev.Game.neuralnetwork;

import com.fynixdev.Game.neuralnetwork.activationfunction.ActivationFunction;

public class Layer {
    Neuron[] neurons;
    double[] outputs;
    ActivationFunction activationFunction;

    public Layer(int neuronCount, int prevLayerCount, ActivationFunction aFunction){
        this.neurons = new Neuron[neuronCount];
        this.activationFunction = aFunction;
        for(int i = 0; i < neuronCount; i++){
            this.neurons[i] = new Neuron(prevLayerCount, aFunction);
        }
    }

    public double[] feedForward(double[] inputs){
        outputs = new double[neurons.length];
        for(int i = 0; i < neurons.length; i++){
            outputs[i] = neurons[i].feedForward(inputs);
        }
        return outputs;
    }
}
