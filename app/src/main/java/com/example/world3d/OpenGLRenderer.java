package com.example.world3d;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLRenderer implements GLSurfaceView.Renderer {

    // Use this to show variable in bottom textView:
    // MainActivity.getInstance().setText(String.valueOf(variable));


    /** Used for debug logs. */
    private static final String TAG = "Renderer";

    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space. */
    public float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye. */
    private float[] mViewMatrix = new float[16];
    /** Store the inverse of view matrix. This is used for pointer ray calculations. */
    public float[] mInverseViewMatrix = new float[16];
    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] mProjectionMatrix = new float[16];
    /** Store the inverse of projection matrix. This is used for pointer ray calculations. */
    public float[] mInverseProjectionMatrix = new float[16];
    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];

    /** Store our model data in a float buffer. */
    public final FloatBuffer mObjectPositions;
    public final FloatBuffer mObjectColors;
    public final FloatBuffer mObjectTextures;

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;
    /** This will be used to pass in the model view matrix. */
    private int mMVMatrixHandle;
    /** This will be used to pass in model position information. */
    private int mPositionHandle;
    /** This will be used to pass in model color information. */
    private int mColorHandle;

    /** These will be used to hold object textures */
    public Canvas Overlay;
    public Canvas Result;
    public Paint paint;
    public static Bitmap bitmap;
    public static Bitmap overlay;
    public static Bitmap bitmapSum;

    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;
    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;
    /** This is a handle to our texture data. */
    private static int mTextureDataHandle;

    /** Size of the texture coordinate data in elements. */
    private final int mTextureDataSize = 2;
    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;
    /** Size of the color data in elements. */
    private final int mColorDataSize = 4;
    /** This is a handle to our per-vertex cube shading program. */
    private int mPerVertexProgramHandle;

    public float xAngle = -70; // X -70 and Y -16 centers initial rotation above Mediterranean
    public float yAngle = -16;
    public float radius = 2f;
    public int sphereStep = 16;
    public float xMovement = 0;
    public float yMovement = 0;
    public int viewportHeight;
    public int viewportWidth;
    public int pWidth = 1920;
    public int pHeight = 960;

    // Position the eye in front of the origin.
    public final float[] eye = {0.0f, 0.0f, 5.0f};
    // We are looking toward the distance
    private final float[] look = {0.0f, 0.0f, -5.0f};
    // Set our up vector. This is where our head would be pointing were we holding the camera.
    private final float[] up = {0.0f, 1.0f, 0.0f};

    // Define the 3D object
    Sphere Object = new Sphere(radius, sphereStep);

    OpenGLView mActivityContext;

    /** Initialize the model data. */
    public OpenGLRenderer( OpenGLView surfaceView) {
        // Initialize the buffers.
        mActivityContext = surfaceView;
        mObjectPositions = Object.objectVertex;
        mObjectColors = Object.objectColor;
        mObjectTextures = Object.objectTexture;
    }

    protected String getVertexShader() {
        final String vertexShader =
                "uniform mat4 u_MVPMatrix;        \n"	// A constant representing the combined model/view/projection matrix.
                        + "uniform mat4 u_MVMatrix;       \n"	// A constant representing the combined model/view matrix.
                        + "attribute vec4 a_Position;     \n"	// Per-vertex position information we will pass in.
                        + "attribute vec4 a_Color;        \n"	// Per-vertex color information we will pass in.
                        + "attribute vec2 a_TexCoordinate;\n"
                        + "varying vec2 v_TexCoordinate;  \n"
                        + "varying vec4 v_Color;          \n"	// This will be passed into the fragment shader.
                        + "void main()                    \n" 	// The entry point for our vertex shader.
                        + "{                              \n"   // Transform the vertex into eye space.
                        + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);\n"    // Multiply the color by the illumination level. It will be interpolated across the triangle.
                        + "   v_Color = a_Color;\n"             // Pass the color through to the fragment shader.
                        +"v_TexCoordinate = a_TexCoordinate;\n"
                        + "   gl_Position = u_MVPMatrix   \n" 	// gl_Position is a special variable used to store the final position.
                        + "               * a_Position;   \n"   // Multiply the vertex by the matrix to get the final point in
                        + "}\n";                                // normalized screen coordinates.
        return vertexShader;
    }

    protected String getFragmentShader() {
        final String fragmentShader =
                "precision mediump float;         \n"		// Set the default precision to medium. We don't need as high of a precision in the fragment shader.
                        + "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the triangle per fragment.
                        + "uniform sampler2D u_Texture;   \n"
                        + "varying vec2 v_TexCoordinate;  \n"
                        + "void main()                    \n"		// The entry point for our fragment shader.
                        + "{                              \n"
                        + "   gl_FragColor = (v_Color * texture2D(u_Texture, v_TexCoordinate)); \n"	// Pass the color directly through the pipeline.
                        + "}                              \n";
        return fragmentShader;
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background clear color to gray.
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

//        // Set the view matrix. This matrix can be said to represent the camera position.
//        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
//        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
//        Matrix.setLookAtM(mViewMatrix, 0, eye[0], eye[1], eye[2], look[0], look[1], look[2], up[0], up[1], up[2]);
//
//        // Invert mViewMatrix for ray calculations
//        Matrix.invertM(mInverseViewMatrix, 0, mViewMatrix, 0);

        // Previous section replaced by this function
        calculateViewMatrix();

        // Invert mViewMatrix for ray calculations
        Matrix.invertM(mInverseViewMatrix, 0, mViewMatrix, 0);

        final String vertexShader = getVertexShader();
        final String fragmentShader = getFragmentShader();

        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position",  "a_Color", "a_TexCoordinate"});
    }


    public void updateEyeAngle(float yAngle) {
        // eye = {0.0f, 0.0f, 5.0f}
        // look = {0.0f, 0.0f, -5.0f}
        // up = {0.0f, 1.0f, 0.0f}
        eye[1] = (float) Math.sin(yAngle * Math.PI / 180) * 5f;
        eye[2] = (float) Math.cos(yAngle * Math.PI / 180) * 5f;
        look[1] = (float) Math.sin(yAngle * Math.PI / 180) * -5.0f;
        look[2] = (float) Math.cos(yAngle * Math.PI / 180) * -5.0f;
        up[1] = (float) Math.cos(yAngle * Math.PI / 180);
        up[2] = (float) Math.sin(yAngle * Math.PI / 180) * -1.0f;
        calculateViewMatrix();
    }


    private void calculateViewMatrix() {
        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eye[0], eye[1], eye[2], look[0], look[1], look[2], up[0], up[1], up[2]);

//        // Invert mViewMatrix for ray calculations
//        Matrix.invertM(mInverseViewMatrix, 0, mViewMatrix, 0);
    }


    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        viewportWidth = width;
        viewportHeight = height;

        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);
        // Calculate initial projection matrix
        calculateProjection(width, height, 1);
    }


    public void calculateProjection(int width, int height, float scale) {
        // Create a new perspective projection matrix.
        // The height will stay the same while the width will vary as per aspect ratio.
        // Added scale factor for zoom functionality.
        final float ratio = (float) width / height;
        final float left = -ratio * scale;
        final float right = ratio * scale;
        final float bottom = -1.0f * scale;
        final float top = 1.0f * scale;
        final float near = 1.0f;
        final float far = 10.0f;

        // Calculate projection matrix
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        // Invert projection matrix for ray calculations
        Matrix.invertM(mInverseProjectionMatrix, 0, mProjectionMatrix, 0);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mPerVertexProgramHandle);

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_TexCoordinate");

        // Draw the object
        float slowCoefficient = 0.93f;
        float verticalMovementRatio = 0.7f;
        int maxAngle = 45;

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, 0.0f);
        // Slowing down yMovement and xMovement
        if (Math.abs(xMovement) < 0.08) {
            xMovement = 0;
        } else {
            xMovement = Math.round(xMovement * slowCoefficient * 100) / 100f;
        }
        if (Math.abs(yMovement) < 0.08) {
            yMovement = 0;
        } else {
            yMovement = Math.round(yMovement * slowCoefficient * 100) / 100f;
        }
        xAngle += xMovement;
        yAngle += yMovement * verticalMovementRatio;

        // Restrict yAngle to +/- maxAngle degrees
        yAngle = Math.min(maxAngle, Math.max(-maxAngle, yAngle));
        // Restrict xAngle to 0 - 360 degrees
        xAngle = (xAngle + 360) % 360;

        // Rotate Eye angle by yAngle
        updateEyeAngle(-yAngle);

        Matrix.rotateM(mModelMatrix, 0, 0, 1.0f, 0.0f, 0.0f); // pitch  // -yAngle
        Matrix.rotateM(mModelMatrix, 0, -xAngle, 0.0f, 1.0f, 0.0f); // roll

        // Put overlay Bitmap on map Bitmap
        Rect rectangle = new Rect(0, 0, pWidth, pHeight);
        Result.drawBitmap(bitmap, new Rect(0, 0, pWidth, pHeight), rectangle, null);
        Result.drawBitmap(overlay, 0, 0, null);

        // Set the active texture unit
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Update existing texture|
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D,0,0,0, bitmapSum);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        drawObject();
    }

    /**
     * Draws the object. */
    private void drawObject() {
        // Pass in the position information
        mObjectPositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mObjectPositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        mObjectColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                0, mObjectColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Pass in the texture coordinate information.
        mObjectTextures.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureDataSize, GLES20.GL_FLOAT, false,
                0, mObjectTextures);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the object.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, Object.mTriangles * 3);
    }



    /**
     * Helper function to compile a shader.
     * @param shaderType The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader. */
    private int compileShader(final int shaderType, final String shaderSource) {
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shaderHandle;
    }

    /**
     * Helper function to compile and link a program.
     * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex shader.
     * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
     * @param attributes Attributes that need to be bound to the program.
     * @return An OpenGL handle to the program. */
    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++)
                {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);
            mTextureDataHandle = loadTexture(mActivityContext, R.drawable.map_world);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }


    public int loadTexture(GLSurfaceView mActivityContext2, final int resourceId)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resources and make them mutable (except original map)
            bitmap = BitmapFactory.decodeResource(mActivityContext2.getResources(), resourceId, options);
            overlay = Bitmap.createBitmap(pWidth, pHeight, Bitmap.Config.ARGB_8888);
            overlay = overlay.copy(Bitmap.Config.ARGB_8888, true);
            bitmapSum = Bitmap.createBitmap(pWidth, pHeight, Bitmap.Config.ARGB_8888);
            bitmapSum = bitmapSum.copy(Bitmap.Config.ARGB_8888, true);

            GLES20.glGenTextures(1, textureHandle, 0);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmapSum into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmapSum, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            Overlay = new Canvas(overlay);
            Result = new Canvas(bitmapSum);
            paint = new Paint();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }


    public void drawPointOnBitmap(float x, float y) {
        overlay.eraseColor(Color.TRANSPARENT);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        Overlay.drawCircle(x, y, 7, paint);
    }
}
