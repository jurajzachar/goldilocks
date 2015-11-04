package test.org.blueskiron.goldilocks.leader.election;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.Membership;
import org.blueskiron.goldilocks.api.Raft;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;
import org.blueskiron.goldilocks.api.states.Candidate;
import org.blueskiron.goldilocks.api.states.Follower;
import org.blueskiron.goldilocks.api.states.Leader;
import org.blueskiron.goldilocks.api.states.State;
import org.blueskiron.goldilocks.leader.election.Election;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class AbstractTest {
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);
  static final String SM_BINDING = "mock";
  static final String BINDING = "mockMember:";
  static final String FQID_BINDING = SM_BINDING + "@" + BINDING;
  private final long votingTimeout = 10L;
  private final int numOfRemotes = 2;
  private final int term = 1;
  private final Raft raft = Mockito.mock(Raft.class);
  private final Follower follower = Mockito.mock(Follower.class);
  private final Candidate candidate = Mockito.mock(Candidate.class);
  private final Leader leader = Mockito.mock(Leader.class);
  private final VoteRequest voteRequest = Mockito.mock(VoteRequest.class);
  private final AppendEntriesRequest leaseRequest = new MockAppendEntriesRequest(SM_BINDING, BINDING + 0, term);
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
  protected Membership membership;
  protected Election election;

  @SuppressWarnings({ "unchecked" })
  @Before
  public void setup() {
    // adjust Leader's term
    Mockito.when(getLeader().term()).thenReturn(getTerm());
    // adjust Candidates's term
    Mockito.when(getCandidate().term()).thenReturn(getTerm());
    // wire AppendEntriesRequest to leader
    Mockito.doReturn(leaseRequest).when(leader).currentLeaseRequest();

    // wire VoteRequest to candidate allow him to vote
    Mockito.doReturn(voteRequest).when(candidate).voteRequest();
    // follower returns the correct term
    Mockito.doReturn(term - 1).when(follower).term();

    // setup up some membership stuff
    membership = new Membership() {
      @Override
      public Set<Member> remoteMembers() {
        Set<Member> mems = new HashSet<>();
        int counter = numOfRemotes;
        while (counter > 0) {
          mems.add(new Member(BINDING + counter) {
          });
          counter--;
        }
        return mems;
      }

      @Override
      public Set<Member> members() {
        return Sets.union(remoteMembers(), Sets.newHashSet(localMember()));
      }

      @Override
      public Optional<Member> getMember(String binding) {
        Iterator<Member> single = Sets.filter(members(), m -> m.getId().equals(binding)).iterator();
        if (single.hasNext()) {
          return Optional.of(single.next());
        } else {
          return Optional.empty();
        }
      }

      @Override
      public Member localMember() {
        Member local = Mockito.mock(Member.class);
        Mockito.when(local.getId()).thenReturn(BINDING + 0);
        return local;
      }

      @Override
      public Optional<Member> leader() {
        return Optional.empty();
      }

      @Override
      public State localState() {
        return null;
      }

    };
    Mockito.doReturn(membership).when(getRaft()).membership();
    Mockito.when(raft.stateMachine()).thenReturn(Mockito.mock(StateMachine.class));
    Mockito.when(raft.stateMachine().getId()).thenReturn(SM_BINDING);
  }

  public VoteRequest getVoteRequest() {
    return voteRequest;
  }

  /**
   * @return the term
   */
  public int getTerm() {
    return term;
  }

  /**
   * @return the candidate
   */
  public Candidate getCandidate() {
    return candidate;
  }

  /**
   * @return the raft
   */
  public Raft getRaft() {
    return raft;
  }

  /**
   * @return the votingTimeout
   */
  public long getVotingTimeout() {
    return votingTimeout;
  }

  /**
   * @return the executor
   */
  public ScheduledExecutorService getExecutor() {
    return executor;
  }

  /**
   * @return the leader
   */
  public Leader getLeader() {
    return leader;
  }

  /**
   * @return the follower
   */
  public Follower getFollower() {
    return follower;
  }

  void sleep(long howLong) {
    LOG.debug("Waiting {} millis...", howLong);
    try {
      Thread.sleep(howLong);
    } catch (InterruptedException e) {
      LOG.error(e.getMessage());
    }
  }

  /**
   * @return the leaseRequest
   */
  public AppendEntriesRequest getLeaseRequest() {
    return leaseRequest;
  }
}
