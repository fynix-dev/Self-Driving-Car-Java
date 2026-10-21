package com.fynixdev.Game.neuralnetwork.activationfunction;

public class ReLU implements ActivationFunction {
    @Override
    public double activate(double x){
        return Math.max(0, x);
    }
}
