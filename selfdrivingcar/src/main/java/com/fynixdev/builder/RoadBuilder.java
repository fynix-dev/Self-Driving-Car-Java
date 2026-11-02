package com.fynixdev.builder;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class RoadBuilder extends Application {
    private GraphicsContext gc;

    @Override 
    public void start(Stage mainStage){
        Canvas canvas = new Canvas(800 ,600);
        // GraphicsContext is used to draw shapes, text, and images on the canvas
        gc = canvas.getGraphicsContext2D();
        BorderPane root = new BorderPane();
        Scene mainScene = new Scene(root, 800, 600);

        root.setOnMouseClicked(event -> {
            System.out.println("Scene X: " + event.getSceneX());
            System.out.println("Scene Y: " + event.getSceneY());
            System.out.println("Screen X: " + event.getScreenX());
            System.out.println("Screen Y: " + event.getScreenY());
        });
        
        mainStage.setScene(mainScene);
        mainStage.setTitle("Road Builder");
        mainStage.show();
    }
}
