package com.gameengine.client.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 3D camera with view and projection matrices
 */
public class Camera {

    private final Vector3f position;
    private final Vector3f rotation;
    private final Matrix4f viewMatrix;
    private final Matrix4f projectionMatrix;

    public Camera() {
        position = new Vector3f(0, 2, 10);
        rotation = new Vector3f(0, 0, 0);
        viewMatrix = new Matrix4f();
        projectionMatrix = new Matrix4f();
    }

    public Matrix4f getViewMatrix() {
        viewMatrix.identity()
                .rotateX((float) Math.toRadians(rotation.x))
                .rotateY((float) Math.toRadians(rotation.y))
                .rotateZ((float) Math.toRadians(rotation.z))
                .translate(-position.x, -position.y, -position.z);
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix(float fov, float aspect, float near, float far) {
        projectionMatrix.identity();
        projectionMatrix.perspective(fov, aspect, near, far);
        return projectionMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(float x, float y, float z) {
        rotation.set(x, y, z);
    }

    public void move(float offsetX, float offsetY, float offsetZ) {
        if (offsetZ != 0) {
            position.x += (float) Math.sin(Math.toRadians(rotation.y)) * -1.0f * offsetZ;
            position.z += (float) Math.cos(Math.toRadians(rotation.y)) * offsetZ;
        }
        if (offsetX != 0) {
            position.x += (float) Math.sin(Math.toRadians(rotation.y - 90)) * -1.0f * offsetX;
            position.z += (float) Math.cos(Math.toRadians(rotation.y - 90)) * offsetX;
        }
        position.y += offsetY;
    }

    public void rotate(float offsetX, float offsetY, float offsetZ) {
        rotation.add(offsetX, offsetY, offsetZ);
    }
}