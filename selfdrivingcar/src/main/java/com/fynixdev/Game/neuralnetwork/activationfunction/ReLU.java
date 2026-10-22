package com.fynixdev.Game.neuralnetwork.activationfunction;

public class ReLU implements ActivationFunction, java.io.Serializable {
    @Override
    public double activate(double x){
        return Math.max(0, x);
    }
}
