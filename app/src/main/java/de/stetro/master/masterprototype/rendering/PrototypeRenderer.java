package de.stetro.master.masterprototype.rendering;

import android.content.Context;
import android.opengl.GLU;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.util.ArrayUtils;

import de.stetro.master.masterprototype.PointCloudManager;

public class PrototypeRenderer extends TangoRajawaliRenderer {
    private static final float CAMERA_NEAR = 0.01f;
    private static final float CAMERA_FAR = 200f;
    private static final int MAX_NUMBER_OF_POINTS = 60000;
    private static final String tag = PrototypeRenderer.class.getSimpleName();
    private Points points;
    private PointCloudManager pointCloudManager;
    private boolean pointCloudFreeze = false;
    private boolean pointCloudVisible = true;
    private Cubes cubes;

    public PrototypeRenderer(Context context, PointCloudManager pointCloudManager) {
        super(context);
        this.pointCloudManager = pointCloudManager;
    }

    @Override
    protected void initScene() {
        super.initScene();

        DirectionalLight light = new DirectionalLight(1, 0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(0.8f);
        light.setPosition(3, 2, 4);
        getCurrentScene().addLight(light);

        cubes = new Cubes();
        getCurrentScene().addChild(cubes);

        points = new Points(MAX_NUMBER_OF_POINTS);
        getCurrentScene().addChild(points);
        getCurrentCamera().setNearPlane(CAMERA_NEAR);
        getCurrentCamera().setFarPlane(CAMERA_FAR);
    }

    @Override
    protected synchronized void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        if (!pointCloudFreeze) {
            PointCloudManager.PointCloudData renderPointCloudData = pointCloudManager.updateAndGetLatestPointCloudRenderBuffer();
            points.updatePoints(renderPointCloudData.floatBuffer, renderPointCloudData.pointCount);
            Quaternion cameraOrientation = getCurrentCamera().getOrientation().clone();
            Vector3 pointCloudCameraHorizontalDirection = new Vector3(1, 0, 0).multiply(cameraOrientation.toRotationMatrix());
            Matrix4 modelMatrix = generateCurrentPointCloudModelMatrix(pointCloudCameraHorizontalDirection);
            points.calculateModelMatrix(modelMatrix);
        }
    }

    @NonNull
    private Matrix4 generateCurrentPointCloudModelMatrix(Vector3 pointCloudCameraHorizontalDirection) {
        return Matrix4
                .createTranslationMatrix(getCurrentCamera().getPosition())
                .rotate(pointCloudCameraHorizontalDirection, 180)
                .rotate(getCurrentCamera().getOrientation());
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public synchronized void onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        Vector3 pointNear = unProject(x, y, -1);
        Vector3 pointFar = unProject(x, y, 1);

        Log.d(tag, "touched at " + x + " x " + y + " Ray intersection between " + pointNear.toString() + " " + pointFar.toString());

        Quaternion cameraOrientation = getCurrentCamera().getOrientation().clone();
        Vector3 pointCloudCameraHorizontalDirection = new Vector3(1, 0, 0).multiply(cameraOrientation.toRotationMatrix());
        Matrix4 modelMatrix = generateCurrentPointCloudModelMatrix(pointCloudCameraHorizontalDirection);

        if (points.intersect(pointNear, pointFar, modelMatrix)) {
            Log.d(tag, "intersects ..." + points.intersection);
            cubes.addChildCubeAt(points.intersection);
        } else {
            Log.d(tag, "intersects not...");
        }
    }

    public void togglePointCloudFreeze() {
        pointCloudFreeze = !pointCloudFreeze;
    }

    public boolean isPointCloudFreeze() {
        return pointCloudFreeze;
    }

    public void togglePointCloudVisibility() {
        pointCloudVisible = !pointCloudVisible;
        points.setVisible(pointCloudVisible);
    }

    public boolean isPointCloudVisible() {
        return pointCloudVisible;
    }


    @Override
    public Vector3 unProject(double dX, double dY, double dZ) {
        float[] np4 = new float[4];
        int[] mViewport = new int[]{0, 0, getViewportWidth(), getViewportHeight()};
        float x = (float) dX;
        float y = (float) dY;
        float z = (float) dZ;

        float[] mmatrix = ArrayUtils.convertDoublesToFloats(getCurrentCamera().getViewMatrix().getDoubleValues());
        float[] pmatrix = ArrayUtils.convertDoublesToFloats(getCurrentCamera().getProjectionMatrix().getDoubleValues());

        GLU.gluUnProject(
                x, getViewportHeight() - y, z,
                mmatrix, 0,
                pmatrix, 0,
                mViewport, 0,
                np4, 0
        );

        return new Vector3((double) (np4[0] / np4[3]), (double) (np4[1] / np4[3]), (double) (np4[2] / np4[3]));
    }

    public void deleteCubes() {
        this.cubes.clear();
    }
}