package com.fynixdev.Game.Car;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fynixdev.Game.neuralnetwork.Layer;
import com.fynixdev.Game.neuralnetwork.NeuralNetwork;
import com.fynixdev.Game.neuralnetwork.activationfunction.Linear;
import com.fynixdev.Game.neuralnetwork.activationfunction.ReLU;
import com.fynixdev.Game.sensor.Sensor;
import com.fynixdev.utils.Utils;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Car {
    public double speed;
    public double x, y, angle;
    public double intiX, intiY;
    private double acceleration, friction, rotationAngle = 0.5, rotationSpeed = 2.0;
    public String type;
    public int width, height, maxSpeed;
    private boolean isDamaged = false;
    private Sensor sensor = null;
    public List<List<Utils.Point>> polygonPoints = new ArrayList<>();
    private double[] xPoints;
    private double[] yPoints;
    // private Polygon polygon;
    private boolean printable = true;
    public NeuralNetwork brain;
    public double[] predictedOutputs = new double[]{0, 0, 0, 0};
    public int predictedActionIndex = 0;
    public boolean hasDetectedCar = false;
    public Car detectedCar = null;
    public boolean laneChangeStarted = false    ;
    public int waitingStartTime = -1;
    public double distanceTravelled = 0.0;
    private final Set<Car> rewardedTraffic = new HashSet<>();
    public Car trackedTrafficCar = null;
    public boolean trafficEncounterActive = false;
    public boolean overtakeRewarded = false;
    public boolean crashPenaltyApplied = false;
    public double previousY;
    public int overtakingStartTime = -1;
    public boolean waitingTooLongPenaaltyApplied = false;

    public Car(double x, double y, int width, int height, int maxSpeed, String type) {
        this.x = x;
        this.y = y;
        this.intiX = x;
        this.intiY = y;
        this.width = width;
        this.height = height;
        this.type = type;

        this.speed = 0;
        this.acceleration = 0.2;
        this.friction = 0.05;
        this.maxSpeed = maxSpeed;
        this.isDamaged = false;
        this.previousY = this.y;

        if("CAR".equals(type)){
            sensor = new Sensor(this, 5, 300);
            
            // create the model structure
            Layer inputLayer = new Layer(this.sensor.rayCount * 2, 10, new Linear());
            Layer hiddenLayer1 = new Layer(12, this.sensor.rayCount * 2, new ReLU());
            Layer hiddenLayer2 = new Layer(8, 12, new ReLU());
            Layer outputLayer = new Layer(4, 8, new Linear());
            brain = new NeuralNetwork(0.0);
            brain.addLayer(inputLayer);
            brain.addLayer(hiddenLayer1);
            brain.addLayer(hiddenLayer2);
            brain.addLayer(outputLayer);
        }
    }

    public void draw(GraphicsContext gc, List<List<Utils.Point>> borders, List<Car> traffic, boolean drawSensor, boolean transparent, int index) {
        // save
        gc.save();
        
        //draw
        gc.setGlobalAlpha(transparent ? 0.2 : 1.0);
        gc.setFill(isDamaged ? Color.BLACK : index == 0 ? Color.GREEN : "CAR".equals(this.type) ? Color.RED : Color.BLUE);
        gc.setStroke(Color.BLACK);
        gc.fillPolygon(this.xPoints, this.yPoints, 4);

        if("CAR".equals(this.type) && drawSensor){
            this.sensor.draw(gc);
        }
        // restore
        gc.restore();
    }

    private void move(int up, int down, int left, int right){
        if(up == 1){
            this.speed += this.acceleration;
        }
        if (down == 1) {
            if (speed > 0) {
                speed = Math.max(0, speed - acceleration);
            } else if (speed < 0) {
                speed = Math.min(0, speed + acceleration);
            }
        }

        if (this.speed != 0) {
            int flip = this.speed > 0 ? 1 : -1;

            if (left == 1) {
                this.angle -= this.rotationAngle * flip;
            }
            
            if (right == 1) {
                this.angle += this.rotationAngle * flip;
            }
        }
        // Keep angle in the range [-180, 180].
        this.angle = ((this.angle + 180) % 360 + 360) % 360 - 180;

        // check few conditions to follow the physics of the car
        if(this.speed > this.maxSpeed){
            this.speed = this.maxSpeed;
        }
        if(this.speed < -this.maxSpeed/2){
            this.speed = -this.maxSpeed/2;
        }
        if(this.speed > 0){
            this.speed = Math.max(0, this.speed - this.friction);
        } else if(this.speed < 0){
            this.speed = Math.min(0, this.speed + this.friction);
        }



        double angleRadians = Math.toRadians(this.angle);
        this.x -= Math.sin(angleRadians) * this.speed;
        this.y -= Math.cos(angleRadians) * this.speed;
    }

    public void update(int x, int y, List<List<Utils.Point>> borders, List<Car> traffic, int frames){
        if("CAR".equals(this.type)){
            sensor.update(borders, traffic);

            
            double[] readings = new double[sensor.rayCount * 2];
            if(frames % 3 == 0){
                for (int i = 0; i < sensor.rayCount; i++) {
                    readings[i] = sensor.trafficInputs.get(i);
                    readings[i + sensor.rayCount] = sensor.borderInputs.get(i);
                }
                predictedOutputs = brain.feedForward(readings);
            }
            int bestActionIndex = Utils.softMax(predictedOutputs);
            // System.out.println("Best action index: " + bestActionIndex + "Predicted output: " + Arrays.toString(predictedOutputs));
            predictedActionIndex = bestActionIndex;
            if(!isDamaged){
                move(
                    bestActionIndex == 0 ? 1 : 0,
                    bestActionIndex == 1 ? 1 : 0,
                    bestActionIndex == 2 ? 1 : 0,
                    bestActionIndex == 3 ? 1 : 0
                );
            }
            if(this.speed > 0){
                distanceTravelled += 0.1;
            }
        } else if("DUMMY".equals(this.type)){
            move(1, 0, 0, 0);
        } else if(!isDamaged){
            move(x, y, 0, 0);
        }

        createPolygon();
        isDamaged = assessDamage(borders, traffic);
        if("CAR".equals(this.type)){
            // Keep the displayed sensor rays aligned with the car after movement.
            sensor.update(borders, traffic);
        }
    }

    private boolean assessDamage(List<List<Utils.Point>> borders, List<Car> traffic){
        if(isDamaged){
            return true;
        }
        // detect collision with border
        for(int i = 0; i < borders.size(); i++){
            for(int j = 0; j < polygonPoints.size(); j++){
                double[] touched = Utils.getIntersection(
                    polygonPoints.get(j % polygonPoints.size()).get(0),
                    polygonPoints.get((j + 1) % polygonPoints.size()).get(0),
                    borders.get(i).get(0),
                    borders.get(i).get(1)
                );
                if(touched != null){
                    return true;
                }
            }
        }

        // detect collision with traffic
        if(traffic != null){
            // loop for car count
            for(int i = 0; i < traffic.size(); i++){
                // loop for each point in a traffic car's polygon
                for(int j = 0; j < traffic.get(i).polygonPoints.size(); j++){
                    // loop for this polygon points
                    for(int k = 0; k < polygonPoints.size(); k++){
                        double[] touch = Utils.getIntersection(
                            traffic.get(i).polygonPoints.get(j % polygonPoints.size()).get(0),
                            traffic.get(i).polygonPoints.get((j + 1) % polygonPoints.size()).get(0), 
                            polygonPoints.get(k % polygonPoints.size()).get(0), 
                            polygonPoints.get((k + 1) % polygonPoints.size()).get(0)
                        );
                        if(touch != null){
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    } 

    private void createPolygon(){
        double radius = Math.hypot((double) this.width, (double) this.height)/2;
        double alpha = Math.atan2(this.width, this.height); 
        polygonPoints.clear();
        polygonPoints.add(List.of(new Utils.Point(
            this.x - Math.sin(Math.toRadians(this.angle) - alpha) * radius,
            this.y - Math.cos(Math.toRadians(this.angle) - alpha) * radius
        )));
        polygonPoints.add(List.of(new Utils.Point(
            this.x - Math.sin(Math.toRadians(this.angle) + alpha) * radius,
            this.y - Math.cos(Math.toRadians(this.angle) + alpha) * radius
        )));
        polygonPoints.add(List.of(new Utils.Point(
            this.x - Math.sin(Math.PI + Math.toRadians(this.angle) - alpha) * radius,
            this.y - Math.cos(Math.PI + Math.toRadians(this.angle) - alpha) * radius
        )));
        polygonPoints.add(List.of(new Utils.Point(
            this.x - Math.sin(Math.PI + Math.toRadians(this.angle) + alpha) * radius,
            this.y - Math.cos(Math.PI + Math.toRadians(this.angle) + alpha) * radius
        )));

        xPoints = polygonPoints.stream().mapToDouble(p -> p.get(0).x()).toArray();
        yPoints = polygonPoints.stream().mapToDouble(p -> p.get(0).y()).toArray();
        if(printable){
            printable = false;
        }
    }

    public void reset(Double x, Double y){
        this.speed = 0;
        this.x = x == null ? intiX : x;
        this.y = y == null ? intiY : y;
        this.previousY = this.y;
        this.angle = 0;
        this.isDamaged = false;
        this.hasDetectedCar = false;
        this.detectedCar = null;
        this.crashPenaltyApplied = false;
        this.laneChangeStarted = false;
        this.waitingStartTime = -1;
        this.distanceTravelled = 0.0;
        this.rewardedTraffic.clear();
        if("CAR".equals(this.type)){
            sensor.reset();
        }
    }

    public boolean markTrafficAsRewarded(Car trafficCar){
        return rewardedTraffic.add(trafficCar);
    }

    public boolean isAlive(){
        return !isDamaged;
    }
}
