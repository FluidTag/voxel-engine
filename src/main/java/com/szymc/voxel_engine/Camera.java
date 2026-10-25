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
	private Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(74.0f), (float) App.WINDOW_WIDTH /App.WINDOW_HEIGHT, 0.1f, 1500.0f);
	public final FrustumIntersection frustumInt = new FrustumIntersection();

	@Override
	public String toString() {
		return "[CamData: " + cameraPos.toString() + " yaw: " + yaw + ", pitch: " + pitch + "]";
	}

	public void setOrientation(float yaw, float pitch) {
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public BLOCK_FACE getHorizontalAxisDirection() {
		double x = Math.cos(yaw * Math.PI/180) * Math.cos(pitch * Math.PI/180);
		double z = Math.sin(yaw * Math.PI/180) * Math.cos(pitch * Math.PI/180);

		double absX = Math.abs(x);
		double absZ = Math.abs(z);

		if (absX > absZ) {
			return x > 0 ? BLOCK_FACE.EAST : BLOCK_FACE.WEST;
		} else {
			return z > 0 ? BLOCK_FACE.NORTH : BLOCK_FACE.SOUTH;
		}
	}

	public BLOCK_FACE getAxisDirection() {
		double x = Math.cos(yaw * Math.PI/180) * Math.cos(pitch * Math.PI/180);
		double y = Math.sin(pitch * Math.PI/180);
		double z = Math.sin(yaw * Math.PI/180) * Math.cos(pitch * Math.PI/180);

		double absX = Math.abs(x);
		double absY = Math.abs(y);
		double absZ = Math.abs(z);

		if (absY > absX && absY > absZ) {
			return y > 0 ? BLOCK_FACE.TOP : BLOCK_FACE.BOTTOM;
		} else if (absX > absY && absX > absZ) {
			return x > 0 ? BLOCK_FACE.EAST : BLOCK_FACE.WEST;
		} else if (absZ > absX && absZ > absY) {
			return z > 0 ? BLOCK_FACE.NORTH : BLOCK_FACE.SOUTH;
		}

		return null;
	}

	public float getYaw() {
		return this.yaw;
	}

	public float getPitch() {
		return this.pitch;
	}

	public Camera() {}

	public Vector3f getLookUnitNormal() {
		return new Vector3f(cameraFront).normalize();
	}
	public Vector3f getCameraUp() {return new Vector3f(cameraUp).normalize();}

	public int getWorldX() {
		return (int) Math.floor(cameraPos.x);
	}

	public int getWorldY() {
		return (int) Math.floor(cameraPos.y);
	}

	public int getWorldZ() {
		return (int) Math.floor(cameraPos.z);
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

		if (yaw > 360) yaw = 0;
		if (yaw < -360) yaw = 0;

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