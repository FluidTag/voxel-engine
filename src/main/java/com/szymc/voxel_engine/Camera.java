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
	public Vector3f cameraPos = new Vector3f(0.0f, 150.0f, 3.0f);
	private Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
	private Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
	private float sensitivity = 0.1f;
	private Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(74.0f), 1600.0f/900.0f, 0.1f, 1500.0f);
	public final FrustumIntersection frustumInt = new FrustumIntersection();

	public Vector3f getLookUnitNormal() {
		return new Vector3f(cameraFront).normalize();
	}

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

	private final Vector3f temp = new Vector3f();
	private final Vector3f tempResult = new Vector3f();
	private final Vector3f tempLookVector = new Vector3f();

	public Vector3f pollSurvivalCameraMovements(long window, float newCamSpeed) {
		tempLookVector.x = cameraFront.x;
		tempLookVector.z = cameraFront.z;

		if (tempLookVector.lengthSquared() > 0) tempLookVector.normalize();
		tempResult.zero();

		if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
			tempResult.add(tempLookVector.mul(newCamSpeed, temp));
		if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
			tempResult.sub(tempLookVector.mul(newCamSpeed, temp));
		if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
			tempResult.sub(tempLookVector.cross(cameraUp, temp).normalize().mul(newCamSpeed));
		if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
			tempResult.add(tempLookVector.cross(cameraUp, temp).normalize().mul(newCamSpeed));

		return tempResult;
	}

	public Vector3f pollCreativeCameraMovements(long window, float newCamSpeed) {
		tempResult.zero();

		if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
			tempResult.add(cameraFront.mul(newCamSpeed, temp));
		if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
			tempResult.sub(cameraFront.mul(newCamSpeed, temp));
		if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
			tempResult.sub(cameraFront.cross(cameraUp, temp).normalize().mul(newCamSpeed));
		if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
			tempResult.add(cameraFront.cross(cameraUp, temp).normalize().mul(newCamSpeed));

		return tempResult;
	}
}