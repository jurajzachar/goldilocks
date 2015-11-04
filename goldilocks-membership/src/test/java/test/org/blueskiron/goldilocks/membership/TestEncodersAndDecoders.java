package test.org.blueskiron.goldilocks.membership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.advantageous.boon.json.JsonFactory;
import io.advantageous.boon.json.ObjectMapper;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.api.statemachine.Command;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;
import org.blueskiron.goldilocks.membership.messages.AppendEntriesRequestImpl;
import org.blueskiron.goldilocks.membership.messages.AppendEntriesResponseImpl;
import org.blueskiron.goldilocks.membership.messages.Join;
import org.blueskiron.goldilocks.membership.messages.Leave;
import org.blueskiron.goldilocks.membership.messages.Rejoin;
import org.blueskiron.goldilocks.membership.messages.VoteRequestImpl;
import org.blueskiron.goldilocks.membership.messages.VoteResponseImpl;
import org.blueskiron.goldilocks.membership.net.sbe.EncodersAndDecoders;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class TestEncodersAndDecoders {

  private static final ObjectMapper mapper = JsonFactory.create();
  private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
  private final MutableDirectBuffer directBuffer = new UnsafeBuffer(byteBuffer);
  private final EncodersAndDecoders util = new EncodersAndDecoders();
  private final int term = 1;
  private final long commitIndex = 0;
  private final String smId = TestKVStore.STATE_MACHINE_NAME;
  private final String compositeId = smId + "@localhost:9000";
  private final StateMachine<KVStore, Command<KVStore>> kvStore = new KVStore(smId);
  final VoteRequest voteRequest = new VoteRequestImpl(compositeId, term, commitIndex);
  final VoteResponse voteResponse = new VoteResponseImpl(compositeId, compositeId, term,
      commitIndex, true);
  final AppendEntriesRequest appendRequest = new AppendEntriesRequestImpl(compositeId, term,
      commitIndex, Optional.empty(), false);
  final AppendEntriesResponse appendResponse = new AppendEntriesResponseImpl(compositeId,
      compositeId, term, commitIndex, true);
  Set<String> memberIds = Sets.newSet(smId + "@localhost:9001", smId + "@localhost:9002", smId +
      "m3@localhost:9003");
  Set<String> acks = Sets.newSet(smId + "@localhost:9001", smId + "@localhost:9002");
  final Join join = new Join(smId, KVStore.class.getName(), mapper.writeValueAsString(kvStore),
      memberIds, acks);
  final Leave leave = new Leave(smId, "localhost:9003", acks);
  final Rejoin rejoin = new Rejoin("localhost:9003", smId, acks);

  @Test
  public void test() {
    int feed = 143;
    int count = feed;
    long start = System.currentTimeMillis();
    while (feed > 0) {
      testEncodingAndDecodingOfAppendEntriesRequest();
      testEncodingAndDecodingOfAppendEntriesResponse();
      testEncodingAndDecodingOfVoteResponse();
      testEncodingAndDecodingOfVoteRequest();
      testEncodingAndDecodingOfRejoin();
      testEncodingAndDecodingOfJoin();
      testEncodingAndDecodingOfLeave();
      feed--;
    }
    long end = System.currentTimeMillis() - start;
    System.out.println("Object encoded and decoded: " + (count * 7));
    System.out.println("Test complete in " + end + " millis");
  }

  // @Test
  public void testEncodingAndDecodingOfVoteRequest() {
    util.encode(voteRequest, directBuffer);
    Object obj = util.decode(directBuffer, 0);
    assertTrue("Decoded object is null!", obj != null);
    assertTrue("Decoded object is of incorrect type!", obj instanceof VoteRequest);
    assertTrue("Decoded object failed structural equality check", obj.equals(voteRequest));
  }

  // @Test
  public void testEncodingAndDecodingOfVoteResponse() {
    util.encode(voteResponse, directBuffer);
    Object obj = util.decode(directBuffer, 0);
    assertTrue("Decoded object is null!", obj != null);
    assertTrue("Decoded object is of incorrect type!", obj instanceof VoteResponse);
    assertTrue("Decoded object failed structural equality check", obj.equals(voteResponse));
  }

  // @Test
  public void testEncodingAndDecodingOfAppendEntriesRequest() {
    util.encode(appendRequest, directBuffer);
    Object obj = util.decode(directBuffer, 0);
    assertTrue("Decoded object is null!", obj != null);
    assertTrue("Decoded object is of incorrect type!", obj instanceof AppendEntriesRequest);
    assertTrue("Decoded object failed structural equality check", obj.equals(appendRequest));
  }

  // @Test
  public void testEncodingAndDecodingOfAppendEntriesResponse() {
    util.encode(appendResponse, directBuffer);
    Object obj = util.decode(directBuffer, 0);
    assertTrue("Decoded object is null!", obj != null);
    assertTrue("Decoded object is of incorrect type!", obj instanceof AppendEntriesResponse);
    assertTrue("Decoded object failed structural equality check", obj.equals(appendResponse));
  }

  // @Test
  public void testEncodingAndDecodingOfJoin() {
    util.encode(join, directBuffer);
    Object obj = util.decode(directBuffer, 0);
    assertTrue("Decoded object is null!", obj != null);
    assertTrue("Decoded object is of incorrect type!", obj instanceof Join);
    assertTrue("Decoded object failed structural equality check", obj.equals(join));
    // additional checks for the serialized statemachine that join carries...
    Join join = (Join) obj;
    try {
      Class<?> klazz = Class.forName(join.getStateMachineClassName());
      StateMachine<?, ?> sm =
          (StateMachine<?, ?>) EncodersAndDecoders.mapper.readValue(
              join.getSerializedStateMachine(), klazz);
      assertEquals(kvStore, sm);
    } catch (ClassNotFoundException e) {
      System.out.println("Eeek! " + e);
      assertTrue(false);
    }

  }

  // @Test
  public void testEncodingAndDecodingOfRejoin() {
    util.encode(rejoin, directBuffer);
    Object obj = util.decode(directBuffer, 0);
    assertTrue("Decoded object is null!", obj != null);
    assertTrue("Decoded object is of incorrect type!", obj instanceof Rejoin);
    assertTrue("Decoded object failed structural equality check", obj.equals(rejoin));
  }

  // @Test
  public void testEncodingAndDecodingOfLeave() {
    util.encode(leave, directBuffer);
    Object obj = util.decode(directBuffer, 0);
    assertTrue("Decoded object is null!", obj != null);
    assertTrue("Decoded object is of incorrect type!", obj instanceof Leave);
    assertTrue("Decoded object failed structural equality check", obj.equals(leave));
  }
}
