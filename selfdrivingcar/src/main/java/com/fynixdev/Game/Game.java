package com.fynixdev.Game;

import java.util.EnumSet;
import java.util.Set;

import com.fynixdev.Game.Car.Car;
import com.fynixdev.Game.road.Road;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Game extends Application {
    private Car car;
    private Car[] traffic;
    private Road road;
    private GraphicsContext gc;
    private HBox infoBox = new HBox(10);
    private Label speedLabel = new Label("Speed: 0");
    private Label xLabel = new Label("X: 0");
    private Label yLabel = new Label("Y: 0");
    private Label angleLabel = new Label("Angle: 0");
    private Label predictedActionLabel = new Label("Predicted Outputs: [0, 0, 0, 0]");
    private Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private Car[] population = generatePopulation(20);
    
    
    // start method is the main entry point for all JavaFX applications where we create the basic screen
	@Override
	public void start(Stage mainStage) throws Exception {
        System.out.println("Pressed keys list: " + pressedKeys);
        Canvas canvas = new Canvas(800 ,600);

        // GraphicsContext is used to draw shapes, text, and images on the canvas
        gc = canvas.getGraphicsContext2D();
        Group root = new Group();
        
        // create a new road object
        road = new Road(canvas.getWidth()/2, 3, canvas.getWidth() * 0.5);

        // create the player car and traffic
        car = new Car(road.getLaneCenter(2), 100.0, 50, 100, 3, "CAR");
        traffic = new Car[]{
            new Car(road.getLaneCenter(1), 100, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(2), -100, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(0), -200, 50, 100, 2, "DUMMY"),
        };

        // update the traffic cars before drawing the player car
        for(Car car: traffic){
            car.update(0, 0, road.borders, null);
        }
        car.update(0, 0, road.borders, traffic);

        // draw the initialized cars
        road.draw(gc);
        car.draw(gc, road.borders, traffic);
        for(Car car: traffic){
            car.draw(gc, road.borders, null);
        }
        root.getChildren().add(canvas);
        infoBox.getChildren().addAll(speedLabel, xLabel, yLabel, angleLabel, predictedActionLabel);
        infoBox.setLayoutX(10);
        root.getChildren().add(infoBox);

        Scene mainScene = new Scene(root);
        
        mainStage.setScene(mainScene);
        mainStage.setTitle("Self Driving Car");
        mainStage.show();

        setUpEventhandling(mainScene);

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000/60), e -> {
            // clear the canvas
            gc.clearRect(0, 0, 800, 600);
            gc.save();

            // update the state
            int dy, dx = 0;
            if (pressedKeys.contains(KeyCode.UP)) {
                dy = -1;
            } else if (pressedKeys.contains(KeyCode.DOWN)) {
                dy = 1;
            } else {
                dy = 0;
            }
            if (pressedKeys.contains(KeyCode.LEFT)) {
                dx = -1;
            } else if (pressedKeys.contains(KeyCode.RIGHT)) {
                dx = 1;
            } else {
                dx = 0;
            }
            for(Car car: traffic){
                car.update(0, 1, road.borders, null);
            }
            car.update(dx, dy, road.borders, traffic);
            gc.translate(0, -car.y + canvas.getHeight() * 0.7); // Move the canvas up based on the car's speed

            // draw on the canvas
            road.draw(gc);
            for(Car car: traffic){
                car.draw(gc, road.borders, null);
            }
            car.draw(gc, road.borders, traffic);

            setUpDisplayInfo();

            gc.restore();

        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

    }

    private void setUpEventhandling(Scene scene){
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
    }

    private void setUpDisplayInfo(){
        speedLabel.setText(String.format("Speed: %.2f", car.speed));
        xLabel.setText(String.format("X: %.2f", car.x));
        yLabel.setText(String.format("Y: %.2f", car.y));
        angleLabel.setText(String.format("Angle: %.2f", car.angle));
        predictedActionLabel.setText(String.format("Predicted Action: %s", 
            car.predictedActionIndex == 0 ? "UP" :
            car.predictedActionIndex == 1 ? "DOWN" :
            car.predictedActionIndex == 2 ? "LEFT" :
            car.predictedActionIndex == 3 ? "RIGHT" :
            "NULL"
        ));
    }

    private Car[] generatePopulation(int N){
        Car[] cars = new Car[N];
        for(int i = 0; i < N; i++){
            cars[i] = new Car(road.getLaneCenter(2), 100.0, 50, 100, 3, "CAR");
        }
        return cars;
    }
}
