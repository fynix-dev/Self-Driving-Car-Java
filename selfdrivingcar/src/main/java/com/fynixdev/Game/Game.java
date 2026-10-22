package com.fynixdev.Game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import com.fynixdev.Game.Car.Car;
import com.fynixdev.Game.neuralnetwork.NeuralNetwork;
import com.fynixdev.Game.road.Road;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Game extends Application {
    private Car[] traffic;
    private Road road;
    private Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private Car[] population;
    private int populationCount = 20;
    Car bestCar;
    private int framesInGeneration = 0;
    private static int MAX_FRAMES_IN_GENERATION = 600;
    private boolean allDead;
    private int currentGeneration = 1;
    private int remainingCars = populationCount;
    private double prevBestFitness = 0.0;

    // GUI
    private GraphicsContext gc;
    private VBox infoBox = new VBox(5);
    private VBox controlBox = new VBox(5);
    private HBox mainBox = new HBox();
    private Label speedLabel = new Label("Speed: 0");
    private Label xLabel = new Label("X: 0");
    private Label yLabel = new Label("Y: 0");
    private Label angleLabel = new Label("Angle: 0");
    private Label predictedActionLabel = new Label("Predicted Outputs: [0, 0, 0, 0]");
    private Button resetButton = new Button("Reset");
    private Button saveButton = new Button("Save");
    private Label remainingCarsLabel = new Label("20/20");
    private Label generationLabel = new Label("Generation: 1");
    
    // start method is the main entry point for all JavaFX applications where we create the basic screen
	@Override
	public void start(Stage mainStage) throws Exception {
        Canvas canvas = new Canvas(800 ,600);
        // GraphicsContext is used to draw shapes, text, and images on the canvas
        gc = canvas.getGraphicsContext2D();
        BorderPane root = new BorderPane();
        
        // create a new road object
        road = new Road(canvas.getWidth()/2, 3, canvas.getWidth() * 0.5);

        // create the traffic and population
        population = generatePopulation(populationCount);
        bestCar = population[0];
        // populate the saved brain to all cars in population
        if(isSavedBrainAvailable()){
            NeuralNetwork loadedBrain = loadBrain();
            for(int i = 0; i < population.length; i++){
                population[i].brain = null;
                population[i].brain = loadedBrain;
                if(i != 0){
                    population[i].brain.mutate(0.2);
                }
            }
        }
        // traffic
        traffic = new Car[]{
            new Car(road.getLaneCenter(1), -200, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(2), -400, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(0), -600, 50, 100, 2, "DUMMY"),
        };

        // initialize UI
        root.getChildren().add(canvas);
        resetButton.setOnAction(e -> {
            // reset the population
            for(Car car: population){
                car.reset();
            }
            for(Car car: traffic){
                car.reset();
            }
        });
        saveButton.setOnAction(e -> {
            saveBrain(null);
        });

        remainingCarsLabel.setFont(new Font(16));
        generationLabel.setFont(new Font(20));
        infoBox.getChildren().addAll(generationLabel, remainingCarsLabel, speedLabel, xLabel, yLabel, angleLabel, predictedActionLabel, controlBox);
        controlBox.getChildren().addAll(resetButton, saveButton);
        infoBox.setPadding(new Insets(10, 10, 10, 10));
        controlBox.setPadding(new Insets(10, 10, 10, 10));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        mainBox.getChildren().addAll(infoBox, spacer, controlBox);
        root.setCenter(mainBox);
        root.setStyle("-fx-font-family: 'Lucida Console'");
        Scene mainScene = new Scene(root, 800, 600);
        
        mainStage.setScene(mainScene);
        mainStage.setTitle("Self Driving Car");
        mainStage.show();

        setUpEventhandling(mainScene);

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000/60), e -> {
            // clear the canvas
            gc.clearRect(0, 0, 800, 600);
            gc.save();

            trainingLoop(canvas);

            setUpDisplayInfo(bestCar);

            gc.restore();

        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

    }

    private void trainingLoop(Canvas canvas){
        // update the traffic
        for(Car car: traffic){
            car.update(0, 1, road.borders, null);
        }
        // update the population
        for(Car car: population){
            car.update(0, 0, road.borders, traffic);
        }
        // find the best car using min y value
        bestCar = Arrays.stream(population)
            .min(Comparator.comparingDouble(car -> car.y))
            .orElse(population[0]);
        // if(bestCar.y < prevBestFitness){
        //     System.out.println("New best fitness: " + bestCar.y);
        //     prevBestFitness = bestCar.y;
        // }

        // move the camera with best car
        gc.translate(0, -bestCar.y + canvas.getHeight() * 0.7); // Move the canvas up based on the car's speed

        // draw on the canvas
        road.draw(gc);
        for(Car car: traffic){
            car.draw(gc, road.borders, null, false, false);
        }
        // car.draw(gc, road.borders, traffic);
        for(Car car: population){
            car.draw(gc, road.borders, null, false, true);
        }
        bestCar.draw(gc, road.borders, traffic, true, false);

        // check if all cars in pupulation are dead
        allDead = true;
        remainingCars = 0;
        for(Car car: population){
            if(car.isAlive()){
                allDead = false;
                remainingCars++;
            }
        }
        
        // increment frames in generation
        framesInGeneration++;

        // if all dead or times up then evolve
        if(allDead || (framesInGeneration >= MAX_FRAMES_IN_GENERATION)){
            evolve();
        }
    }

    private void evolve(){
        // print generation completed
        System.out.println("====== Generation " + currentGeneration + " completed ======");

        // find the best performing car
        Car champion = bestCar;

        // if not saved, then save the new champion
        if(!isSavedBrainAvailable() || (champion.y < prevBestFitness)){
            System.out.println("New champion found with fitness: " + champion.brain.bestFitness);
            champion.brain.bestFitness = champion.y;
            saveBrain(champion.brain);
        }

        // clear and generate new population by mutatuing the brain and reset the position
        NeuralNetwork championBrain = loadBrain();
        population[0].brain = championBrain;
        for(int i = 0; i < populationCount; i++){
            population[i].brain = championBrain;
            if(i != 0){
                population[i].brain.mutate(0.4);;
            }

            population[i].reset();
        }

        //reset traffic
        for(Car car: traffic){
            car.reset();
        }

        // increase generation
        currentGeneration++;

        // reset frames in generation
        framesInGeneration = 0;
    }

    private void setUpEventhandling(Scene scene){
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
    }

    private void setUpDisplayInfo(Car car){
        generationLabel.setText(String.format("Generation: %d", currentGeneration));
        remainingCarsLabel.setText(String.format("Cars: %d/20", remainingCars));
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
            cars[i] = new Car(road.getLaneCenter(1), 100.0, 50, 100, 3, "CAR");
        }
        return cars;
    }

    private void saveBrain(NeuralNetwork brain){
        try {
            FileOutputStream fileOut = new FileOutputStream("best_brain.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(brain == null ? bestCar.brain: brain);
            out.close();
            fileOut.close();
            System.out.println("Saved best brain successfully");
        } catch (Exception e) {
            System.out.println("Error occured while saving best brain: " + e);
        }
    }

    private NeuralNetwork loadBrain(){
        String fileName = "best_brain.ser";
        NeuralNetwork brain = null;
        try {
            if(isSavedBrainAvailable()){
                FileInputStream fileIn = new FileInputStream(fileName);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                brain = (NeuralNetwork) in.readObject();
                prevBestFitness = brain.bestFitness;
                System.out.println("Loaded best brain successfully with fitness: " + prevBestFitness);  
                in.close();
                fileIn.close();
            } 
        } catch (Exception e) {
            System.out.println("Error occured while loadaing brain: " + e);
        }
        if(brain != null){
            return brain;
        }
        return null;
    }

    private boolean isSavedBrainAvailable(){
        String fileName = "best_brain.ser";
        File file = new File(fileName);
        return (file.exists());
    }
}
