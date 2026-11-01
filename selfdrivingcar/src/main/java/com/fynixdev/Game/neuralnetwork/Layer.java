package com.fynixdev.Game.neuralnetwork;

import com.fynixdev.Game.neuralnetwork.activationfunction.ActivationFunction;

public class Layer implements java.io.Serializable {
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

    public void mutate(double mutationRate, double mutationStrength){
        for(Neuron neuron: neurons){
            neuron.mutate(mutationRate, mutationStrength);
        }
    }

    public void copy(Layer other){
        for(int i = 0; i < neurons.length; i++){
            this.neurons[i].copy(other.neurons[i]);
        }
    }

}
