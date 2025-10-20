package com.mcmiddleearth.mapInteraction.map;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class TransformationTest {

    Transformation transformation = new Transformation(-1, 0, 1, 1,
    0, 0, 2, 3, Transformation.Rotation.RIGHT_90);

    @org.junit.jupiter.api.Test
    void getMap() {
        double xWorld = 2;
        double zWorld = 0;
        Logger.getGlobal().info("("+xWorld+"|"+zWorld+") -> ("+transformation.getMapX(xWorld,zWorld)+"|"
                +transformation.getMapZ(xWorld, zWorld)+")");
    }

    @org.junit.jupiter.api.Test
    void getMapX() {
    }

    @org.junit.jupiter.api.Test
    void getMapZ() {
    }

    @org.junit.jupiter.api.Test
    void getWorldX() {
    }

    @org.junit.jupiter.api.Test
    void getWorldZ() {
    }

}