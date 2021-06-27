package com.example.world3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

public class Sphere {

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;

//    public final FloatBuffer objectVertex;
//    public final FloatBuffer objectColor;
//    public final FloatBuffer objectTexture;

    private final float[] white = {1.0f, 1.0f, 1.0f, 1.0f};

    //    public float[] Points;
//    private float[] Colors;
    public float[] Textures;
    public final FloatBuffer objectVertex; // listPoints;
    public final FloatBuffer objectColor; // listColors;
    public final FloatBuffer objectTexture; // listTextures;

    private final double mRaduis;
    private final int mStep;
    public int mTriangles;

    /**
     * The value of step will define the size of each facet as well as the number of facets
     * @param radius
     * @param step */
    public Sphere( float radius, int step) {
        this.mRaduis = radius;
        this.mStep = step;

        mTriangles = 4 * mStep * (mStep - 1);
        int mPoints = 3 * mTriangles;

//        listPoints = FloatBuffer.allocate(mPoints * 3); // 3 floats per point
//        listColors = FloatBuffer.allocate(mPoints * 4); // 4 floats per point
//        listTextures = FloatBuffer.allocate(mPoints * 2); // 2 floats per point

        objectVertex = ByteBuffer.allocateDirect(mPoints * 3 * mBytesPerFloat) // mPoints instead
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
//        objectVertex.put(Points).position(0);

        objectColor = ByteBuffer.allocateDirect(mPoints * 4 * mBytesPerFloat) // mColors instead
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
//        objectColor.put(Colors).position(0);

        objectTexture = ByteBuffer.allocateDirect(mPoints * 2 * mBytesPerFloat) // mColors instead
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
//        objectColor.put(Textures).position(0);

        build();
    }

    private void build() {
        /**
         * x = p * sin(phi) * cos(theta)
         * z = p * cos(phi)
         * y = p * sin(phi) * sin(theta)
         *  z and y switched because z axis is horizontal and y is vertical (rotation axis for sphere) */
        double dTheta = (double) Math.PI / mStep;

        // Generate triangle coordinates for the sphere
        // for each horizontal line
        for (int i=0; i < mStep; i++) {
            double phiU = i * dTheta;
            double phiL = (i+1) * dTheta;
            // for each meridian
//            for (double theta = 0.0; theta <= (Math.PI * 2); theta+=dTheta) {
            for (int j=0; j < mStep * 2; j++) {
                double thetaL = j * dTheta;
                double thetaR = ((j+1) * dTheta) % (2 * mStep * dTheta);
                if (i == 0) {
                    // Create triangles at top pole
                    objectVertex.put(0f); objectVertex.put(calcZ(phiU)); objectVertex.put(0f);
                    objectVertex.put(calcX(phiL, thetaR)); objectVertex.put(calcZ(phiL)); objectVertex.put(calcY(phiL, thetaR));
                    objectVertex.put(calcX(phiL, thetaL)); objectVertex.put(calcZ(phiL)); objectVertex.put(calcY(phiL, thetaL));
                    paintTriangle(white);
                    objectTexture.put(1-j*0.5f/mStep); objectTexture.put(0f);
                    objectTexture.put(1-(j+1)*0.5f/mStep); objectTexture.put((i+1)*1f/mStep);
                    objectTexture.put(1-j*0.5f/mStep); objectTexture.put((i+1)*1f/mStep);
                } else if (i == mStep-1) {
                    // Create triangles at bottom pole
                    objectVertex.put(calcX(phiU, thetaL)); objectVertex.put(calcZ(phiU)); objectVertex.put(calcY(phiU, thetaL));
                    objectVertex.put(calcX(phiU, thetaR)); objectVertex.put(calcZ(phiU)); objectVertex.put(calcY(phiU, thetaR));
                    objectVertex.put(0f); objectVertex.put(calcZ(phiL)); objectVertex.put(0f);
                    paintTriangle(white);
                    objectTexture.put(1-j*0.5f/mStep); objectTexture.put(i*1f/mStep);
                    objectTexture.put(1-(j+1)*0.5f/mStep); objectTexture.put(i*1f/mStep);
                    objectTexture.put(1-(j+1)*0.5f/mStep); objectTexture.put(1f);
                } else {
                    // Create two triangles for each trapezoid
                    objectVertex.put(calcX(phiU, thetaL)); objectVertex.put(calcZ(phiU)); objectVertex.put(calcY(phiU, thetaL));
                    objectVertex.put(calcX(phiL, thetaR)); objectVertex.put(calcZ(phiL)); objectVertex.put(calcY(phiL, thetaR));
                    objectVertex.put(calcX(phiL, thetaL)); objectVertex.put(calcZ(phiL)); objectVertex.put(calcY(phiL, thetaL));
                    paintTriangle(white);
                    objectTexture.put(1-j*0.5f/mStep); objectTexture.put(i*1f/mStep);
                    objectTexture.put(1-(j+1)*0.5f/mStep); objectTexture.put((i+1)*1f/mStep);
                    objectTexture.put(1-j*0.5f/mStep); objectTexture.put((i+1)*1f/mStep);

                    objectVertex.put(calcX(phiU, thetaL)); objectVertex.put(calcZ(phiU)); objectVertex.put(calcY(phiU, thetaL));
                    objectVertex.put(calcX(phiU, thetaR)); objectVertex.put(calcZ(phiU)); objectVertex.put(calcY(phiU, thetaR));
                    objectVertex.put(calcX(phiL, thetaR)); objectVertex.put(calcZ(phiL)); objectVertex.put(calcY(phiL, thetaR));
                    paintTriangle(white);
                    objectTexture.put(1-j*0.5f/mStep); objectTexture.put(i*1f/mStep);
                    objectTexture.put(1-(j+1)*0.5f/mStep); objectTexture.put(i*1f/mStep);
                    objectTexture.put(1-(j+1)*0.5f/mStep); objectTexture.put((i+1)*1f/mStep);
                }
            }
        }
        objectVertex.position(0);
        objectColor.position(0);
        objectTexture.position(0);
//        Points = objectVertex.array();
//        Colors = objectColor.array();
//        Textures = objectTexture.array();
    }

    private float calcX(double phi, double theta) {
        return(float) (mRaduis * Math.sin(phi) * Math.cos(theta));
    }

    private float calcY(double phi, double theta) {
        return (float) (mRaduis * Math.sin(phi) * Math.sin(theta));
    }

    private float calcZ(double phi) {
        return (float) (mRaduis * Math.cos(phi));
    }

    private void paintTriangle() {
        Random r = new Random();
        float[] color = {r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0f};
        objectColor.put(color); objectColor.put(color); objectColor.put(color);
    }

    private void paintTriangle(float[] color) {
        objectColor.put(color); objectColor.put(color); objectColor.put(color);
    }
}