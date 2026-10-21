package com.fynixdev.Game.neuralnetwork.activationfunction;

public class Linear implements ActivationFunction {
    @Override
    public double activate(double x){
        return x;
    }
}
