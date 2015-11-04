package test.org.blueskiron.goldilocks.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.blueskiron.goldilocks.api.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConfiguration {

	private static final Logger LOG = LoggerFactory
			.getLogger(TestConfiguration.class);
	private static Configuration config;

	@BeforeClass
	public static void setup() {
		config = new Configuration();
	}

	@AfterClass
	public static void teardown() {
		config = null;
	}

	@Test
	public void testFullConfig() {
	    long appendEntriesPeriod = config.withAppendEntriesPeriod();
	    assertNotNull(appendEntriesPeriod);
	    LOG.debug("Append Entries Period: {}", appendEntriesPeriod);
	    
		long maxLeaderTimeout = config.withMaxLeaderTimeout();
		assertNotNull(maxLeaderTimeout);
		LOG.debug("Maximum Leader Timeout: {}", maxLeaderTimeout);
		
		double lowerLeaderTimeoutThreshold = config.withLowerLeaderTimeoutThreshold();
        assertNotNull(lowerLeaderTimeoutThreshold);
        LOG.debug("Maximum Leader Timeout: {}", lowerLeaderTimeoutThreshold);
        
        double upperLeaderTimeoutThreshold = config.withUpperLeaderTimeoutThreshold();
        assertNotNull(upperLeaderTimeoutThreshold);
        LOG.debug("Maximum Leader Timeout: {}", upperLeaderTimeoutThreshold);
        
		long votingTimeout = config.withVotingTimeout();
		assertNotNull(votingTimeout);
		LOG.debug("Voting Timeout: {}", votingTimeout);
		
		int clusterWorkers = config.withClusterWorkers();
		assertNotNull(clusterWorkers);
		LOG.debug("Cluster Workers: {}", clusterWorkers);
		
		long writeTimeout = config.withWriteTimeout();
		assertNotNull(writeTimeout);
		LOG.debug("Write Timeout: {}", writeTimeout);
		
		int appendEntriesWorkers = config.withAppendEntriesWorkers();
		assertNotNull(appendEntriesWorkers);
		LOG.debug("Append Entries Workers: {}", appendEntriesWorkers);
		
		String localBinding = config.withLocalBinding();
		assertNotNull(localBinding);
		LOG.debug("Local Binding: {}", localBinding);
		
		List<String> members = config.withRemoteMembers();
		assertNotNull(members);
		assertTrue(members.size() > 1);
		LOG.debug("Members: {}", members);
	}
}
