package com.fynixdev.Game.road;

import java.util.ArrayList;
import java.util.List;

import com.fynixdev.utils.Utils;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Road {
    double x;
    int laneCount;
    double width, left, right, top, bottom;
    int infinity = 1000000;
    public List<List<Utils.Point>> borders = new ArrayList<>();

    public Road(double x, int laneCount, double width) {
        this.x = x;
        this.laneCount = laneCount;
        this.width = width;

        this.left = x - width / 2;
        this.right = x + width / 2;

        this.top = -infinity;
        this.bottom = infinity;

        borders.add(List.of(new Utils.Point(this.left, this.bottom), new Utils.Point(this.left, this.top)));   
        borders.add(List.of(new Utils.Point(this.right, this.bottom), new Utils.Point(this.right, this.top)));
    }

    public double getLaneCenter(int laneIndex){
        double laneWidth = (double) this.width / this.laneCount;
        return this.left + (laneWidth/2) + (Math.min(laneIndex, this.laneCount - 1) * laneWidth);
    }

    public void draw(GraphicsContext gc){
        // road background
        gc.setFill(Color.GRAY);
        double padding = 50;
        // fillRect(x, y, w, h)
        gc.fillRect(this.left - padding/2, this.top, this.width + padding, this.infinity);

        // middle lines
        for (int i = 0; i < laneCount; i++) {
            double offset = Utils.lerp(this.left, this.right, (double) i / this.laneCount);
            gc.setStroke(Color.WHITE);
            gc.setLineDashes(20, 20);
            gc.setLineWidth(5);
            gc.strokeLine(offset, this.top, offset, this.bottom);
        }

        gc.setStroke(Color.WHITE);
        gc.setLineDashes(null);
        gc.setLineWidth(10);
        borders.forEach((border) -> {
            gc.strokeLine(border.get(0).x(), border.get(0).y(), border.get(1).x(), border.get(1).y());
        });

    } 
}
