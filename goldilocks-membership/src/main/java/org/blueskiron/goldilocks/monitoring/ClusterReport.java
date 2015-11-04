package org.blueskiron.goldilocks.monitoring;

public interface ClusterReport {

	public ClusterStatus clusterStatus();
	
	public LogStatus logStatus();
}
