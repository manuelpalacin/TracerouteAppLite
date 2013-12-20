package edu.upf.nets.app;

public class CancellationToken {
	private volatile boolean isCanceled = false;
	
	public void reset() {
		this.isCanceled = false;
	}
	
	public void cancel() {
		this.isCanceled = true;
	}
	
	public boolean isCanceled() {
		return this.isCanceled;
	}
}
