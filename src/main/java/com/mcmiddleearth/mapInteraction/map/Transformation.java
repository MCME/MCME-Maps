package com.mcmiddleearth.mapInteraction.map;

public class Transformation {

    private final double xShift;
    private final double zShift;
    private final double xScale;
    private final double zScale;
    private final Rotation rotation;

    public Transformation(double xMapMin, double zMapMin, double xMapMax, double zMapMax,
                          double xWorldMin, double zWorldMin, double xWorldMax, double zWorldMax, Rotation rotation) {
        this.rotation = rotation;
        double xWorldMinR = xWorldMin;
        double xWorldMaxR = xWorldMax;
        double zWorldMinR = zWorldMin;
        double zWorldMaxR = zWorldMax;
        switch(rotation) {
            case RIGHT_90 -> {
                xWorldMinR = zWorldMin;
                xWorldMaxR = zWorldMax;
                zWorldMinR = -xWorldMax;
                zWorldMaxR = -xWorldMin;
            }
            case LEFT_90 -> {
                xWorldMinR = -zWorldMax;
                xWorldMaxR = -zWorldMin;
                zWorldMinR = xWorldMin;
                zWorldMaxR = xWorldMax;
            }
            case TURN_180 -> {
                xWorldMinR = -xWorldMax;
                xWorldMaxR = -xWorldMin;
                zWorldMinR = -zWorldMax;
                zWorldMaxR = -zWorldMin;
            }
        }
        xScale = (xMapMax-xMapMin)/(xWorldMaxR-xWorldMinR);
        zScale = (zMapMax-zMapMin)/(zWorldMaxR-zWorldMinR);
        xShift = xMapMin - xWorldMinR * xScale;
        zShift = zMapMin - zWorldMinR * zScale;
    }

    public double getMapX(double xWorld, double zWorld) {
        return switch(rotation) {
            case NONE -> xWorld * xScale + xShift;
            case RIGHT_90 -> zWorld * xScale + xShift;
            case LEFT_90 -> -zWorld * xScale + xShift;
            case TURN_180 -> -xWorld * xScale + xShift;
        };
    }

    public double getMapZ(double xWorld, double zWorld) {
        return switch(rotation) {
            case NONE -> zWorld * zScale + zShift;
            case RIGHT_90 -> -xWorld * zScale + zShift;
            case LEFT_90 -> xWorld * zScale + zShift;
            case TURN_180 -> - zWorld * zScale + zShift;
        };
        //return zWorld * zScale + zShift;
    }

    public double getWorldX(double xMap, double zMap) {
        return switch(rotation) {
            case NONE -> (xMap - xShift) / xScale;
            case RIGHT_90 -> -(zMap - zShift) / zScale;
            case LEFT_90 -> (zMap - zShift) / zScale;
            case TURN_180 -> -(xMap - xShift) / xScale;
        };
    }

    public double getWorldZ(double xMap, double zMap) {
        return switch(rotation) {
            case NONE -> (zMap - zShift) / zScale;
            case RIGHT_90 -> (xMap - xShift) / xScale;
            case LEFT_90 -> -(xMap - xShift) / xScale;
            case TURN_180 -> -(zMap - zShift) / zScale;
        };
    }

    public enum Rotation {
        NONE, RIGHT_90, LEFT_90, TURN_180
    }
}
