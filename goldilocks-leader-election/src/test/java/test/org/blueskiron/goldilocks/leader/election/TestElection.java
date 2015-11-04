package test.org.blueskiron.goldilocks.leader.election;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.leader.election.Election;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestElection extends AbstractTest {

  @Before
  public void setup() {
    super.setup();
    VoteRequest vrq = getVoteRequest();
    Set<VoteResponse> votes = new HashSet<>();
    votes.add(new MockVoteResponse(vrq, BINDING + 1, true));
    votes.add(new MockVoteResponse(vrq, BINDING + 2, true));
    Mockito.doReturn(new MockVoteResponse(vrq, BINDING + 0, true)).when(getCandidate())
        .onVoteRequest(vrq);
    Mockito.doReturn(CompletableFuture.completedFuture(votes)).when(getRaft())
        .collectVotes(getTerm(), getVoteRequest());
  }

  @Test
  public void testReachingMajority() throws InterruptedException {
    LOG.info("Got {}", getVoteRequest());
    LOG.info("Testing Candidate's election: Reaching Majority and Leader Promotion");
    election = new Election(getCandidate(), getRaft(), getVotingTimeout(), getExecutor());
    election.start();
    Thread.sleep(100);
    Mockito.verify(getCandidate(), Mockito.times(1)).becomeLeader(1);
  }

  @Test
  public void testNotReachingMajority() throws InterruptedException {
    LOG.info("Got {}", getVoteRequest());
    LOG.info("Testing Candidate's election: Not Reaching Majority");
    Set<VoteResponse> votes = new HashSet<>();
    Mockito.doReturn(CompletableFuture.completedFuture(votes)).when(getRaft())
        .collectVotes(getTerm(), getVoteRequest());
    election = new Election(getCandidate(), getRaft(), getVotingTimeout(), getExecutor());
    election.start();
    Thread.sleep(100);
    // here we expect a new election for term 2 to take place...
    Mockito.verify(getCandidate(), Mockito.times(1)).repeatElection(getTerm()+1);
  }
}
