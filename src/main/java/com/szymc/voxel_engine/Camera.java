package com.szymc.voxel_engine;


import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;


import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;


public class Camera {
	private float yaw = -90.0f;
	private float pitch = 0.0f;
	public Vector3f cameraPos = new Vector3f(0.0f, 220.0f, 3.0f);
	private Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
	private Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
	private float sensitivity = 0.1f;
	private Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(74.0f), 1600.0f/900.0f, 0.1f, 1500.0f);
	public final FrustumIntersection frustumInt = new FrustumIntersection();
	
	public int getWorldX() {
		return (int)cameraPos.x;
	}

	public int getWorldY() {
		return (int)cameraPos.y;
	}

	public int getWorldZ() {
		return (int)cameraPos.z;
	}

	public Matrix4f getProjectionMatrix() {
		return this.projection;
	}

	private void updateCameraVectors() {
		Vector3f direction = new Vector3f();
		direction.x = (float)(Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
		direction.y = (float)(Math.sin(Math.toRadians(pitch)));
		direction.z = (float)(Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));

		cameraFront = direction.normalize();
	}

	public void recieveMouseOffset(float xOffset, float yOffset) {
		xOffset *= sensitivity;
		yOffset *= sensitivity;

		yaw += xOffset;
		pitch += yOffset;

		if (pitch > 89.0f) pitch = 89.0f;
		if (pitch < -89.0f) pitch = -89.0f;

		updateCameraVectors();
	}

	private final Matrix4f viewMatrix = new Matrix4f();
	private final Matrix4f pvMatrix = new Matrix4f();
	private final Vector3f lookAtTarget = new Vector3f();

	public Matrix4f getViewMatrix() {
		cameraPos.add(cameraFront, lookAtTarget);

		return viewMatrix.identity().lookAt(
				cameraPos,
				lookAtTarget,
				cameraUp
				);
	}

	public void updateFrustum(Matrix4f viewMatrix) {
		projection.mul(viewMatrix, pvMatrix);
		frustumInt.set(pvMatrix);
	}

	private Vector3f temp = new Vector3f();
	public Vector3f pollCameraMovements(long window, float newCamSpeed) {
		Vector3f lookVector = new Vector3f(cameraFront.x, 0, cameraFront.z);
		if (lookVector.lengthSquared() > 0) lookVector.normalize();
		Vector3f result = new Vector3f();

		if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
			result.add(lookVector.mul(newCamSpeed, temp));
		if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
			result.sub(lookVector.mul(newCamSpeed, temp));
		if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
			result.sub(lookVector.cross(cameraUp, temp).normalize().mul(newCamSpeed));
		if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
			result.add(lookVector.cross(cameraUp, temp).normalize().mul(newCamSpeed));

		return result;
	}
}