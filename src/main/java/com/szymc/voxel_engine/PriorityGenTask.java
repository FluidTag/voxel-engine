package com.szymc.voxel_engine;
public class PriorityGenTask implements Runnable, Prioritizable {
	private final Runnable internalTask;
	private int priority;
	
	public PriorityGenTask(int priority, Runnable task) {
		this.internalTask = task;
		this.priority = priority;
	}
	
	@Override
	public void run() {
		this.internalTask.run();
	}
	
	@Override
	public int getPriorityValue() {
		return this.priority;
	}
}