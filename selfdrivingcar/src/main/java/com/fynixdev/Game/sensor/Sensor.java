package com.fynixdev.Game.sensor;

import java.util.ArrayList;
import java.util.List;

import com.fynixdev.Game.Car.Car;
import com.fynixdev.utils.Utils;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Sensor {
    public int rayCount;
    double rayLength, raySpread = Math.PI * 0.8;
    Car car;
    List<List<Utils.Point>> rays = new ArrayList<>();
    public List<double[]> readings = new ArrayList<>();
    public List<Double> trafficInputs = new ArrayList<>();
    public List<Double> borderInputs = new ArrayList<>();
    boolean printable = true;

    public Sensor(Car car, int rayCount, int rayLength){
        this.car = car;
        this.rayCount = rayCount;
        this.rayLength = rayLength;
    }

    private void castRays(){
        rays.clear();
        for (int i = 0; i < rayCount; i++) {
            double rayAngle = Utils.lerp(
                this.raySpread/2,
                -this.raySpread/2,
                this.rayCount == 1 ? 0.5 : (double) i/(this.rayCount - 1)
            ) + Math.toRadians(this.car.angle);
            rays.add(List.of(
                // from point
                new Utils.Point(this.car.x, this.car.y),
                // to point
                new Utils.Point(
                    this.car.x - (Math.sin(rayAngle) * this.rayLength),
                    this.car.y - (Math.cos(rayAngle) * this.rayLength)
                )
            ));
        }
    }

    // private void getReadings(List<List<Utils.Point>> borders, Car[] traffic){
    //     readings.clear();
    //     car.hasDetectedCar = false;
    //     car.detectedCar = null;

    //     // loop for each ray
    //     for (int i = 0; i < rays.size(); i++) {
    //         // ray start and end
    //         Utils.Point rayStart = rays.get(i).get(0);
    //         Utils.Point rayEnd = rays.get(i).get(1);

    //         double[] reading = null;

    //         // loop for each border
    //         for (List<Utils.Point> border : borders) {
    //             double[] intersection = Utils.getIntersection(
    //                 rayStart,
    //                 rayEnd,
    //                 border.get(0),
    //                 border.get(1)
    //             );
    //             if (intersection != null && (reading == null || intersection[2] < reading[2])) {
    //                 reading = intersection;
    //             }
    //         }
            
    //         if(traffic != null){
    //             // loop for traffic car
    //             for(Car trafficCar: traffic){
    //                 if(trafficCar.polygonPoints.size() < 2){
    //                     continue;
    //                 }
    //                 // loop for each point in polypoints of traffic
    //                 for(int j = 0; j < trafficCar.polygonPoints.size(); j++){
    //                     double[] intersection = Utils.getIntersection(
    //                         rayStart,
    //                         rayEnd,
    //                         trafficCar.polygonPoints.get(j).get(0),
    //                         trafficCar.polygonPoints.get((j + 1) % trafficCar.polygonPoints.size()).get(0)
    //                     );
    //                     if (intersection != null && (reading == null || intersection[2] < reading[2])) {
    //                         reading = intersection;
    //                         this.car.hasDetectedCar = true;
    //                         this.car.detectedCar = trafficCar;
    //                     }
    //                 }
    //             }
    //         }

    //         // reading already contains the nearest hit, or null if the ray hit nothing.
    //         readings.add(reading);
    //     }

    // }

    private void getReadings(List<List<Utils.Point>> borders, List<Car> traffic) {

        readings.clear();
        trafficInputs.clear();
        borderInputs.clear();

        car.hasDetectedCar = false;
        car.detectedCar = null;

        double closestTrafficDistance = Double.MAX_VALUE;

        for (int i = 0; i < rays.size(); i++) {

            Utils.Point rayStart = rays.get(i).get(0);
            Utils.Point rayEnd = rays.get(i).get(1);

            double[] nearestBorder = null;
            double[] nearestTraffic = null;

            // Find nearest road boundary.
            for (List<Utils.Point> border : borders) {

                double[] intersection = Utils.getIntersection(
                    rayStart, rayEnd,
                    border.get(0), border.get(1)
                );

                if (intersection != null &&
                    (nearestBorder == null ||
                    intersection[2] < nearestBorder[2])) {

                    nearestBorder = intersection;
                }
            }

            // Find nearest traffic car independently.
            if (traffic != null) {

                for (Car trafficCar : traffic) {

                    if (trafficCar == car ||
                        !trafficCar.isAlive() ||
                        trafficCar.polygonPoints.size() < 2) {
                        continue;
                    }

                    for (int j = 0; j < trafficCar.polygonPoints.size(); j++) {

                        double[] intersection = Utils.getIntersection(
                            rayStart, rayEnd,
                            trafficCar.polygonPoints.get(j).get(0),
                            trafficCar.polygonPoints.get(
                                (j + 1) % trafficCar.polygonPoints.size()
                            ).get(0)
                        );

                        if (intersection != null &&
                            (nearestTraffic == null ||
                            intersection[2] < nearestTraffic[2])) {

                            nearestTraffic = intersection;
                        }
                    }

                    if (nearestTraffic != null &&
                        nearestTraffic[2] < closestTrafficDistance) {

                        closestTrafficDistance = nearestTraffic[2];
                        car.hasDetectedCar = true;
                        car.detectedCar = trafficCar;
                    }
                }
            }

            // Convert distances into normalized proximity values.
            double trafficValue = nearestTraffic == null
                ? 0.0 : 1.0 - nearestTraffic[2];

            double borderValue = nearestBorder == null
                ? 0.0 : 1.0 - nearestBorder[2];

            trafficInputs.add(trafficValue);
            borderInputs.add(borderValue);

            // Keep existing visual sensor readings.
            double[] nearest = nearestBorder;

            if (nearestTraffic != null &&
                (nearest == null ||
                nearestTraffic[2] < nearest[2])) {

                nearest = nearestTraffic;
            }

            readings.add(nearest);
        }
    }

    public void update(List<List<Utils.Point>> borders, List<Car> traffic){
        castRays();
        getReadings(borders, traffic);
    }

    public void draw(GraphicsContext gc){
        if(!"CAR".equals(this.car.type)){
            return;
        }
        for (int i = 0; i < rayCount; i++) {
            gc.setStroke(Color.YELLOW);
            gc.setLineDashes(null);
            gc.setLineWidth(3);
            
            if(readings.get(i) != null){
                gc.strokeLine(
                    rays.get(i).get(0).x(),
                    rays.get(i).get(0).y(),
                    readings.get(i)[0],
                    readings.get(i)[1]
                );
                gc.setStroke(Color.BLACK);
                gc.setLineDashes(null);
                gc.setLineWidth(3);
                gc.strokeLine(
                    readings.get(i)[0],
                    readings.get(i)[1],
                    rays.get(i).get(1).x(),
                    rays.get(i).get(1).y()
                );
            } else {
                gc.strokeLine(
                    rays.get(i).get(0).x(),
                    rays.get(i).get(0).y(),
                    rays.get(i).get(1).x(),
                    rays.get(i).get(1).y()
                );
            }
        }
    }

    public void reset(){
        rays.clear();
        readings.clear();
    }
}
