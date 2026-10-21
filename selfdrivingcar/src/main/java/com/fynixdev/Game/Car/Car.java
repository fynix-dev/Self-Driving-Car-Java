package com.fynixdev.Game.Car;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fynixdev.Game.neuralnetwork.Layer;
import com.fynixdev.Game.neuralnetwork.NeuralNetwork;
import com.fynixdev.Game.neuralnetwork.activationfunction.Linear;
import com.fynixdev.Game.neuralnetwork.activationfunction.ReLU;
import com.fynixdev.Game.sensor.Sensor;
import com.fynixdev.utils.Utils;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class Car {
    public double speed;
    public double x, y, angle;
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
    private NeuralNetwork brain;
    public double[] predictedOutputs = new double[]{0, 0, 0, 0};
    public int predictedActionIndex = 0;

    public Car(double x, double y, int width, int height, int maxSpeed, String type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;

        this.speed = 0;
        this.acceleration = 0.2;
        this.friction = 0.05;
        this.maxSpeed = maxSpeed;
        this.isDamaged = false;

        if("CAR".equals(type)){
            sensor = new Sensor(this, 6, 250);
            
            // create the model structure
            Layer inputLayer = new Layer(this.sensor.rayCount, 6, new Linear());
            Layer hiddenLayer = new Layer(6, 6, new ReLU());
            Layer outputLayer = new Layer(4, 6, new ReLU());
            brain = new NeuralNetwork();
            brain.addLayer(inputLayer);
            brain.addLayer(hiddenLayer);
            brain.addLayer(outputLayer);
        }
    }

    public void draw(GraphicsContext gc, List<List<Utils.Point>> borders, Car[] traffic) {
        // save
        gc.save();
        
        //draw
        gc.setFill(isDamaged ? Color.BLACK : "CAR".equals(this.type) ? Color.RED : Color.BLUE);
        gc.setStroke(Color.BLACK);
        gc.fillPolygon(this.xPoints, this.yPoints, 4);

        if("CAR".equals(this.type)){
            this.sensor.draw(gc);
        }
        // restore
        gc.restore();
    }

    private void move(int up, int down, int left, int right){
        if(up == 1){
            this.speed += this.acceleration;
        }
        if(down == 1){
            this.speed -= this.acceleration;
        }
        //if(this.speed != 0){
            int flip = speed > 0 ? 1 : -1;
            if(left == 1){
                this.angle -= this.rotationAngle * flip;
            }
            if(right == 1){
                this.angle += this.rotationAngle * flip;
            }
        //}

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

    

    public void update(int x, int y, List<List<Utils.Point>> borders, Car[] traffic){
        if("CAR".equals(this.type)){
            sensor.update(borders, traffic);
            
            double[] readings = sensor.readings.stream()
                .mapToDouble(reading -> reading == null ? 0 : 1 - reading[2])
                .toArray();
            predictedOutputs = brain.feedForward(readings);
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

    private boolean assessDamage(List<List<Utils.Point>> borders, Car[] traffic){
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
            for(int i = 0; i < traffic.length; i++){
                // loop for each point in a traffic car's polygon
                for(int j = 0; j < traffic[i].polygonPoints.size(); j++){
                    // loop for this polygon points
                    for(int k = 0; k < polygonPoints.size(); k++){
                        double[] touch = Utils.getIntersection(
                            traffic[i].polygonPoints.get(j % polygonPoints.size()).get(0),
                            traffic[i].polygonPoints.get((j + 1) % polygonPoints.size()).get(0), 
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
            System.out.println("PolygonPoints: " + polygonPoints);
            printable = false;
        }
    }
}
