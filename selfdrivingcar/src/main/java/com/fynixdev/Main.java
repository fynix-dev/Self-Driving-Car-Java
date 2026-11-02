package com.fynixdev;

import com.fynixdev.Game.Game;
import com.fynixdev.builder.RoadBuilder;

import javafx.application.Application;

public class Main {
    private static boolean game = true;
    public static void main(String[] args) {
        Application.launch(game ? Game.class : RoadBuilder.class ,args);
    }
}