package com.fynixdev.Game.neuralnetwork.activationfunction;

public class Linear implements ActivationFunction, java.io.Serializable {
    @Override
    public double activate(double x){
        return x;
    }
}
