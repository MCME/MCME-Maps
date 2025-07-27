package com.mcmiddleearth.mapInteraction.map;

public class Transformation {

    private final double xShift;
    private final double zShift;
    private final double xScale;
    private final double zScale;

    public Transformation(double xMapMin, double zMapMin, double xMapMax, double zMapMax,
                          double xWorldMin, double zWorldMin, double xWorldMax, double zWorldMax) {
        xScale = (xMapMax-xMapMin)/(xWorldMax-xWorldMin);
        zScale = (zMapMax-zMapMin)/(zWorldMax-zWorldMin);
        xShift = xMapMin - xWorldMin * xScale;
        zShift = zMapMin - zWorldMin * zScale;
    }

    public double getMapX(double xWorld) {
        return xWorld * xScale + xShift;
    }

    public double getMapZ(double zWorld) {
        return zWorld * zScale + zShift;
    }

    public double getWorldX(double xMap) {
        return (xMap - xShift) / xScale;
    }

    public double getWorldZ(double zMap) {
        return (zMap - zShift) / zScale;
    }

}
