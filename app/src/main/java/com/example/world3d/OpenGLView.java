package com.example.world3d;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class OpenGLView extends GLSurfaceView {

    // Use this to show variable in bottom textView:
    // MainActivity.getInstance().setText(String.valueOf(variable));

    // Variables for touch interaction
    private float touchX = 0;
    private float touchY = 0;
    private float lastTouchDistance;
    private float touchDir;
    private float sizeCoef = 1;
    private boolean ignoreOnce = false; // Ignore movement measurement once after releasing second finger
    private boolean movementDetected = false; // Don't calculate touch coordinates if movement detected before

    OpenGLRenderer renderer;

    public OpenGLView(Context context) {
        super(context);
        init();
    }

    public OpenGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        setRenderer(renderer = new OpenGLRenderer( this));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int points = event.getPointerCount();
        final int action = event.getAction();
        float touchDistance;
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: { // One finger down
                touchX = event.getX();
                touchY = event.getY();
            }
            break;
            case MotionEvent.ACTION_POINTER_DOWN: { // Other finger down
                movementDetected = true;
                touchDistance = getTouchedDistance(event);
                lastTouchDistance = touchDistance;
            }
            break;
            case MotionEvent.ACTION_MOVE: { // Finger(s) move
                movementDetected = true;
                if (points == 1) {
                    // Calculate movement
                    if (ignoreOnce) {
                        ignoreOnce = false;
                    } else {
                        renderer.xMovement = (touchX - event.getX()) / 5f * sizeCoef;
                        renderer.yMovement = (touchY - event.getY()) / 5f * sizeCoef;
                    }
                    // Get new reading
                    touchX = event.getX(0);
                    touchY = event.getY(0);
                } else if (points == 2) {
                    touchDistance = getTouchedDistance(event);
                    if (touchDistance < lastTouchDistance) {
                        touchDir = 1;
                    } else if (touchDistance >= lastTouchDistance) {
                        touchDir = -1;
                    }
                    sizeCoef = Math.max(0.25f, Math.min(1f, sizeCoef + 0.03f * touchDir));
                    renderer.calculateProjection(renderer.viewportWidth, renderer.viewportHeight, sizeCoef);
                    lastTouchDistance = touchDistance;
                    // update firstTouch coordinates
                    touchX = event.getX(0);
                    touchY = event.getY(0);
                }
            }
            break;
            case MotionEvent.ACTION_POINTER_UP: { // Other finger up
                ignoreOnce = true;
            }
            break;
            case MotionEvent.ACTION_UP: {
                if (!movementDetected) { // Last finger up
                    try {
                        float angleX = renderer.xAngle - 90f; // compensation for texture shift
                        float angleY = -renderer.yAngle;

                        float angleXrad = (float) (angleX * Math.PI / 180);
                        float angleYrad = (float) (angleY * Math.PI / 180);

                        float[] intersect = intersectionPoint(castRay(touchX, touchY), renderer.eye, renderer.radius);
                        int touched = Float.compare(intersect[0], Float.NaN);

                        float[] intersectRotated = rotatePoint(intersect, angleXrad, angleYrad);

                        float[] polar = getPolar(intersectRotated[0], intersectRotated[1], intersectRotated[2]);

                        polar[0] += angleXrad;
                        // Restrict X to 0 - 2*PI radians
                        polar[0] = (float) ((polar[0] + Math.PI * 2) % (Math.PI * 2));
                        // Restrict Y to 0 - PI radians
                        polar[1] = (float) Math.min(Math.PI / 2, Math.max(-Math.PI / 2, polar[1]));

                        // Only draw point if not on poles
                        if (polar[1] > -Math.PI / 2 + 0.3 && polar[1] < Math.PI / 2 - 0.4) {
                            float[] mapLoc = new float[2];
                            mapLoc[0] = (float) (polar[0] / (Math.PI * 2));
                            mapLoc[1] = (float) ((polar[1] + Math.PI / 2) / Math.PI);

                            renderer.drawPointOnBitmap(mapLoc[0] * renderer.pWidth, mapLoc[1] * renderer.pHeight);
                        }

                        // Show coordinates if clicked on sphere
                        if (touched != 0) {
                            showCoordinates(polar);
                        } else {
                            MainActivity.getInstance().setText("");
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                movementDetected = false;
            }
            break;
            default:
                throw new IllegalStateException("Unexpected value: " + action); // (action & MotionEvent.ACTION_MASK));
        }
        return true;
    }

    private void showCoordinates(float[] polar) {
        float[] coordinates = new float[2];
        coordinates[0] = Math.round((polar[0] - Math.PI) / Math.PI * 180 * 100f) / 100f;
        coordinates[1] = Math.round(polar[1] / (Math.PI / 2) * 90 * 100f) / 100f;
        String longitude = (coordinates[0] < 0) ? "°W " : "°E ";
        String latitude = (coordinates[1] < 0) ? "°N " : "°S ";

        MainActivity.getInstance().setText(String.valueOf(Math.abs(coordinates[0])) + longitude + "; "
                + String.valueOf(Math.abs(coordinates[1])) + latitude);
    }


    private float[] intersectionPoint(float[] vector, float[] eye, float radius) {
        float[] origin = {0f, 0f, 5f};
        float[] intersection = new float[4];
        float a, b, c, t;

        a = vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2];
        b = origin[0] * vector[0] * 2 + origin[1] * vector[1] * 2 + origin[2] * vector[2] * 2;
        c = origin[0]*origin[0] + origin[1]*origin[1] + origin[2]*origin[2] - radius*radius;

        t = minRoot(a, b, c);
        intersection[0] = origin[0] + vector[0] * t;
        intersection[1] = origin[1] + vector[1] * t;
        intersection[2] = origin[2] + vector[2] * t;
        intersection[3] = 1.0f;
        return intersection;
    }


    private float minRoot(float a, float b, float c) {
        float root1, root2, sqrtD;

        sqrtD = (float) Math.sqrt((b*b) - (4*a*c));
        root1 = (-b + sqrtD) / (2*a);
        root2 = (-b - sqrtD) / (2*a);

        return Math.min(root1, root2);
    }


    private float[] getPolar(float x, float y, float z) {
        float[] polarCoords = new float[2];
        float h = (float) Math.sqrt(x*x + z*z);
        float r = renderer.radius;
        polarCoords[0] = (float) Math.asin(x/h);
        polarCoords[1] = (float) Math.asin(y/r);
        return polarCoords;
    }


    private float[] castRay(float posX, float posY) throws InterruptedException {
        float[] pointPosition = new float[4];
        pointPosition[0] = (2.0f * posX) / renderer.viewportWidth - 1.0f;
        pointPosition[1] = (2.0f * posY) / renderer.viewportHeight - 1.0f;
        pointPosition[2] = -1.0f;
        pointPosition[3] = 1.0f;

        // get touch ray line matrix
        float[] touchRay = multiplyMat4ByVec4(renderer.mInverseProjectionMatrix, pointPosition);
        touchRay[2] = -1.0f;
        touchRay[3] = 0.0f;

        return multiplyMat4ByVec4(renderer.mInverseViewMatrix, touchRay);
    }


    private float[] rotatePoint(float[] pointV4, float xAngle, float yAngle) {
//        float cosX = (float) Math.cos(xAngle);
//        float sinX = (float) Math.sin(xAngle);
        float cosY = (float) Math.cos(yAngle);
        float sinY = (float) Math.sin(yAngle);

        float[] rotationMatrixY = {1, 0, 0, 0,
                0, cosY, -sinY, 0,
                0, sinY, cosY, 0,
                0, 0, 0, 1};
//        float[] rotationMatrix = {cos, -sin, 0, 0,
//                                  sin, cos, 0, 0,
//                                  0, 0, 1, 0,
//                                  0, 0, 0, 1};
//        float[] rotationMatrixX = {cosX, 0, sinX, 0,
//                                   0, 1, 0, 0,
//                                   -sinX, 0, cosX, 0,
//                                   0, 0, 0, 1};
//        Log.d("CALC", String.valueOf(pointV4[0]) + " " + String.valueOf(pointV4[1]) + " " + String.valueOf(pointV4[2]));
//        float[] rotatedPointY = multiplyMat4ByVec4(rotationMatrixY, pointV4);
//        Log.d("CALC", String.valueOf(rotatedPointY[0]) + " " + String.valueOf(rotatedPointY[1]) + " " + String.valueOf(rotatedPointY[2]));
//        float[] rotatedPointXY = multiplyMat4ByVec4(rotationMatrixX, rotatedPointY);
//        Log.d("CALC", String.valueOf(rotatedPointXY[0]) + " " + String.valueOf(rotatedPointXY[1]) + " " + String.valueOf(rotatedPointXY[2]));

        return multiplyMat4ByVec4(rotationMatrixY, pointV4);
    }


    private float[] multiplyMat4ByVec4(float[] matrix4, float[] vector4) {
        float[] returnMatrix = new float[4];
        returnMatrix[0] = (matrix4[0] * vector4[0]) + (matrix4[1] * vector4[1]) + (matrix4[2] * vector4[2]) + (matrix4[3] * vector4[3]);
        returnMatrix[1] = (matrix4[4] * vector4[0]) + (matrix4[5] * vector4[1]) + (matrix4[6] * vector4[2]) + (matrix4[7] * vector4[3]);
        returnMatrix[2] = (matrix4[8] * vector4[0]) + (matrix4[9] * vector4[1]) + (matrix4[10] * vector4[2]) + (matrix4[11] * vector4[3]);
        returnMatrix[3] = (matrix4[12] * vector4[0]) + (matrix4[13] * vector4[1]) + (matrix4[14] * vector4[2]) + (matrix4[15] * vector4[3]);
        return returnMatrix;
    }


    private float getTouchedDistance(MotionEvent event) {
        float x1 = event.getX(0);
        float y1 = event.getY(0);
        float x2 = event.getX(1);
        float y2 = event.getY(1);
        return (float) Math.sqrt(Math.pow(Math.abs(x2 - x1), 2) + Math.pow(Math.abs(y2 - y1), 2));
    }
}
