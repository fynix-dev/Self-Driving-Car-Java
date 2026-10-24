package com.fynixdev.Game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import com.fynixdev.Game.Car.Car;
import com.fynixdev.Game.neuralnetwork.NeuralNetwork;
import com.fynixdev.Game.road.Road;
import com.fynixdev.utils.Utils;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
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
    private int populationCount = 100;
    private double[] populationFitness = new double[populationCount];
    Car bestCar;
    private int framesInGeneration = 0;
    private static int MAX_FRAMES_IN_GENERATION = 3000;
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
    private Label carRewardPointLabel = new Label("Reward Points: 0");
    private Label carIndexLabel = new Label("Car Index: 0");
    private Label timeLabel = new Label("Time: 0");
    
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
                population[i].brain.copy(loadedBrain);
                if(i != 0){
                    population[i].brain.mutate(0.4);
                }
            }
        }
        // traffic
        traffic = new Car[]{
            new Car(road.getLaneCenter(1), -200, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(0), -200, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(2), -500, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(0), -800, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(2), -1200, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(1), -1500, 50, 100, 2, "DUMMY"),
            new Car(road.getLaneCenter(0), -1800, 50, 100, 2, "DUMMY"),
        };

        // initialize UI
        root.getChildren().add(canvas);
        resetButton.setOnAction(e -> {
            // reset the population
            evolve();
        });
        saveButton.setOnAction(e -> {
            saveBrain(null);
        });

        remainingCarsLabel.setFont(new Font(16));
        generationLabel.setFont(new Font(20));
        infoBox.getChildren().addAll(generationLabel, remainingCarsLabel, carIndexLabel, speedLabel, xLabel, yLabel, angleLabel, predictedActionLabel, carRewardPointLabel, timeLabel);
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
        // update the reward function
        for(int i = 0; i < populationCount; i++){
            rewardFunction(population[i]);
        }

        // find the best car using the reward points, which one has highest points
        bestCar = population[Utils.softMax(populationFitness)];
        
        // move the camera with best car
        gc.translate(0, -bestCar.y + canvas.getHeight() * 0.7); // Move the canvas up based on the car's speed
        
        // draw on the canvas
        road.draw(gc);
        for(Car car: traffic){
            car.draw(gc, road.borders, null, false, false);
        }
  
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
        // set UI info
        setUpDisplayInfo(bestCar);
        
        // if all dead or times up then evolve
        if(allDead || (framesInGeneration >= MAX_FRAMES_IN_GENERATION)){
            evolve();
        }
    }

    private void evolve(){
        // print generation completed
        System.out.println("====== Generation " + currentGeneration + " completed ======");

        // find the best performing car
        int bestCarIndex = Arrays.asList(population).indexOf(bestCar);

        // if not saved, then save the new champion
        if(!isSavedBrainAvailable() || (populationFitness[bestCarIndex] > prevBestFitness)){
            System.out.println("New champion found with fitness: " + populationFitness[bestCarIndex]);
            population[bestCarIndex].brain.bestFitness = populationFitness[bestCarIndex];
            saveBrain(population[bestCarIndex].brain);
        }

        // v1 clear and generate new population by mutatuing the brain and reset the position
        // v2 clear old, generate some random and some mutated
        population = generatePopulation(populationCount);
        NeuralNetwork championBrain = loadBrain();
        for(int i = 10; i < populationCount; i++){         
            population[i].brain.copy(championBrain);;
            if(i != 0){
                population[i].brain.mutate(0.8);;
            }

            population[i].reset(null ,null);
        }

        //reset traffic
        for(Car car: traffic){
            car.reset(null, null);
        }

        // reset population fitness
        for(int i = 0; i < populationCount; i++){
            populationFitness[i] = 0.0;
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
        remainingCarsLabel.setText(String.format("Cars: %d/%d", remainingCars, populationCount));
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
        carRewardPointLabel.setText(String.format("Reward Points: %.2f", populationFitness[Arrays.asList(population).indexOf(car)]));
        carIndexLabel.setText(String.format("Car index: %d", Arrays.asList(population).indexOf(car)));
        timeLabel.setText(String.format("Time: %d", framesInGeneration/60));
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


private void rewardFunction(Car car) {
    int index = Arrays.asList(population).indexOf(car);

    if (index < 0) return;

    // CHANGED: Apply crash penalty once, then skip dead cars.
    if (!car.isAlive()) {
        if (!car.crashPenaltyApplied) {
            populationFitness[index] -= 50.0;
            car.crashPenaltyApplied = true;
        }
        return;
    }

    // Reward for moving forward
    if (car.speed > 0.1) {
        populationFitness[index] += 0.01;
    }

    // Detect traffic and start tracking a lane change
    boolean changingLane = Math.abs(car.angle) > 20;

    if (car.hasDetectedCar && changingLane && car.speed > 0.1) {
        car.laneChangeStarted = true;
    }

    boolean movingInLane = Math.abs(car.angle) < 2.0;

    // CHANGED: Award the lane-change bonus only once when the
    // car returns straight after starting a lane change.
    if (movingInLane && car.laneChangeStarted && car.speed > 0.1) {
        car.laneChangeStarted = false;
        car.waitingStartTime = -1;
        populationFitness[index] += 10.0; // CHANGED: Reduced from +50
    }

    // Track how long the car waits behind traffic
    if (car.hasDetectedCar && car.speed > 0.1 && !car.laneChangeStarted) {
        if (car.waitingStartTime == -1) {
            car.waitingStartTime = framesInGeneration / 60;
        }
    }

    // CHANGED: Penalize waiting only once per waiting period.
    if (car.waitingStartTime != -1
            && (framesInGeneration / 60 - car.waitingStartTime) > 5
            && !car.laneChangeStarted) {
        populationFitness[index] -= 5.0; // CHANGED: Reduced from -50
        car.waitingStartTime = -1;
    }
}

}
