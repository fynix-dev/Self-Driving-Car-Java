package com.fynixdev.utils;

public class Utils {
    
    public static double lerp(double A, double B, double t){
        return A + (B - A) * t;
    }
    
    public record Point(double x, double y) {}
    
    public static double[] getIntersection(Point ray0, Point ray1, Point border0, Point border1){
        double denom = ((ray0.x() - ray1.x()) * (border0.y() - border1.y())
                - (ray0.y() - ray1.y()) * (border0.x() - border1.x()));
        
        if (Math.abs(denom) < 1e-10) {
            return null;
        }

        double t = ((ray0.x() - border0.x()) * (border0.y() - border1.y())
                - (ray0.y() - border0.y()) * (border0.x() - border1.x())) / denom;
        double u = -((ray0.x() - ray1.x()) * (ray0.y() - border0.y())
                - (ray0.y() - ray1.y()) * (ray0.x() - border0.x())) / denom;
        
        if (t < 0 || t > 1 || u < 0 || u > 1) {
            return null;
        }

        double ix = ray0.x() + t * (ray1.x() - ray0.x());
        double iy = ray0.y() + t * (ray1.y() - ray0.y());
        return new double[]{ix, iy, t};
    }

    public static int softMax(double[] values) {
       double max = -Double.MAX_VALUE;
       int index = 0;
       for(int i = 0; i < values.length; i++){
            if(values[i] > max){
                index = i;
                max = values[i];
            }
       }
       return index;
    }
}
