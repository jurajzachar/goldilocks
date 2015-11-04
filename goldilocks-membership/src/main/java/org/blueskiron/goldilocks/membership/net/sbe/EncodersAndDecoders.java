package org.blueskiron.goldilocks.membership.net.sbe;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.api.statemachine.LogEntry;
import org.blueskiron.goldilocks.membership.messages.AppendEntriesRequestImpl;
import org.blueskiron.goldilocks.membership.messages.AppendEntriesResponseImpl;
import org.blueskiron.goldilocks.membership.messages.Join;
import org.blueskiron.goldilocks.membership.messages.Leave;
import org.blueskiron.goldilocks.membership.messages.Rejoin;
import org.blueskiron.goldilocks.membership.messages.VoteRequestImpl;
import org.blueskiron.goldilocks.membership.messages.VoteResponseImpl;
import org.blueskiron.goldilocks.membership.messages.sbe.AppendEntriesRequestDecoder;
import org.blueskiron.goldilocks.membership.messages.sbe.AppendEntriesRequestEncoder;
import org.blueskiron.goldilocks.membership.messages.sbe.AppendEntriesResponseDecoder;
import org.blueskiron.goldilocks.membership.messages.sbe.AppendEntriesResponseEncoder;
import org.blueskiron.goldilocks.membership.messages.sbe.BooleanType;
import org.blueskiron.goldilocks.membership.messages.sbe.JoinDecoder;
import org.blueskiron.goldilocks.membership.messages.sbe.JoinEncoder;
import org.blueskiron.goldilocks.membership.messages.sbe.LeaveDecoder;
import org.blueskiron.goldilocks.membership.messages.sbe.LeaveEncoder;
import org.blueskiron.goldilocks.membership.messages.sbe.RejoinDecoder;
import org.blueskiron.goldilocks.membership.messages.sbe.RejoinEncoder;
import org.blueskiron.goldilocks.membership.messages.sbe.VoteRequestDecoder;
import org.blueskiron.goldilocks.membership.messages.sbe.VoteRequestEncoder;
import org.blueskiron.goldilocks.membership.messages.sbe.VoteResponseDecoder;
import org.blueskiron.goldilocks.membership.messages.sbe.VoteResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.advantageous.boon.json.JsonFactory;
import io.advantageous.boon.json.ObjectMapper;
import uk.co.real_logic.agrona.MutableDirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.sbe.ir.generated.MessageHeaderDecoder;
import uk.co.real_logic.sbe.ir.generated.MessageHeaderEncoder;

/**
 * This class contains all boiler plate code that is needed for Simple Binary
 * Encoding to work.
 * 
 * @author jzachar
 */
public class EncodersAndDecoders {

  private static final Logger LOG = LoggerFactory.getLogger(EncodersAndDecoders.class);

  private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
  private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();

  // VOTES
  private final VoteRequestDecoder vrqDecoder = new VoteRequestDecoder();
  private final VoteRequestEncoder vrqEncoder = new VoteRequestEncoder();
  private final VoteResponseDecoder vrpDecoder = new VoteResponseDecoder();
  private final VoteResponseEncoder vrpEncoder = new VoteResponseEncoder();

  // APPEND ENTRIES
  private final AppendEntriesRequestDecoder aerqDecoder = new AppendEntriesRequestDecoder();
  private final AppendEntriesRequestEncoder aerqEncoder = new AppendEntriesRequestEncoder();
  private final AppendEntriesResponseDecoder aerpDecoder = new AppendEntriesResponseDecoder();
  private final AppendEntriesResponseEncoder aerpEncoder = new AppendEntriesResponseEncoder();

  // Aux Membership
  private final JoinDecoder joinDecoder = new JoinDecoder();
  private final JoinEncoder joinEncoder = new JoinEncoder();
  private final RejoinDecoder rejoinDecoder = new RejoinDecoder();
  private final RejoinEncoder rejoinEncoder = new RejoinEncoder();
  private final LeaveDecoder leaveDecoder = new LeaveDecoder();
  private final LeaveEncoder leaveEncoder = new LeaveEncoder();

  // JSON serializer for LogEntries (not ideal, but SBE supports only fixed
  // arrays of primitives at
  // the
  // moment)
  public static final ObjectMapper mapper = JsonFactory.create();

  public int encode(Object obj, MutableDirectBuffer directBuffer) {
    int len = 0;
    if (obj instanceof VoteRequest) {
      len = encode((VoteRequest) obj, directBuffer);
    } else if (obj instanceof VoteResponse) {
      len = encode((VoteResponse) obj, directBuffer);
    } else if (obj instanceof AppendEntriesRequest) {
      len = encode((AppendEntriesRequest) obj, directBuffer);
    } else if (obj instanceof AppendEntriesResponse) {
      len = encode((AppendEntriesResponse) obj, directBuffer);
    } else if (obj instanceof Join) {
      len = encode((Join) obj, directBuffer);
    } else if (obj instanceof Rejoin) {
      len = encode((Rejoin) obj, directBuffer);
    } else if (obj instanceof Leave) {
      len = encode((Leave) obj, directBuffer);
    } else {
      LOG.error("Failed to find appropriate encoder for {}", obj);
    }
    return len;
  }

  public int encode(Object obj, ByteBuffer buffer) {
    final UnsafeBuffer directBuffer = new UnsafeBuffer(buffer);
    return encode(obj, directBuffer);
  }

  public Object decode(ByteBuffer buffer) {
    UnsafeBuffer directBuffer = new UnsafeBuffer(buffer);
    return decode(directBuffer, 0);
  }

  public Object decode(MutableDirectBuffer directBuffer, int bufferOffset) {
    messageHeaderDecoder.wrap(directBuffer, bufferOffset);

    final int templateId = messageHeaderDecoder.templateId();
    Object obj = null;
    switch (templateId) {
    // AppendEntries
    case AppendEntriesResponseEncoder.TEMPLATE_ID:
      obj = decodeAppendEntriesResponse(directBuffer, bufferOffset, messageHeaderDecoder);
      break;
    case AppendEntriesRequestEncoder.TEMPLATE_ID:
      obj = decodeAppendEntriesRequest(directBuffer, bufferOffset, messageHeaderDecoder);
      break;
    // Votes
    case VoteResponseEncoder.TEMPLATE_ID:
      obj = decodeVoteResponse(directBuffer, bufferOffset, messageHeaderDecoder);
      break;
    case VoteRequestEncoder.TEMPLATE_ID:
      obj = decodeVoteRequest(directBuffer, bufferOffset, messageHeaderDecoder);
      break;
    // Membership
    case JoinEncoder.TEMPLATE_ID:
      obj = decodeJoin(directBuffer, bufferOffset, messageHeaderDecoder);
      break;
    case RejoinEncoder.TEMPLATE_ID:
      obj = decodeRejoin(directBuffer, bufferOffset, messageHeaderDecoder);
      break;
    case LeaveEncoder.TEMPLATE_ID:
      obj = decodeLeave(directBuffer, bufferOffset, messageHeaderDecoder);
      break;
    default:
      LOG.error("Decoding failed. No suitable encoder template found for id {}", templateId);
    }
    return obj;
  }

  /*
   * VRQ
   */
  private int encode(VoteRequest voteRequest, MutableDirectBuffer directBuffer) {
    int offset = 0;
    messageHeaderEncoder.wrap(directBuffer, offset).blockLength(vrqEncoder.sbeBlockLength())
        .templateId(vrqEncoder.sbeTemplateId()).schemaId(vrqEncoder.sbeSchemaId())
        .version(vrqEncoder.sbeSchemaVersion());
    offset += messageHeaderEncoder.encodedLength();
    vrqEncoder.wrap(directBuffer, offset).term(voteRequest.getTerm()).commitIndex(voteRequest.getCommittedLogIndex())
        .compositeId(voteRequest.getFullyQualifiedId());
    return vrqEncoder.encodedLength();
  }

  /*
   * VRQ
   */
  private VoteRequest decodeVoteRequest(MutableDirectBuffer directBuffer, int bufferOffset,
      MessageHeaderDecoder decoder) {
    bufferOffset += decoder.encodedLength();
    final int actingBlockLength = decoder.blockLength();
    final int schemaId = decoder.schemaId();
    vrqDecoder.wrap(directBuffer, bufferOffset, actingBlockLength, schemaId);
    int term = vrqDecoder.term();
    long commitIndex = vrqDecoder.commitIndex();
    String compositeId = vrqDecoder.compositeId();
    if (compositeId != null && compositeId.length() > 3) {
      return new VoteRequestImpl(compositeId, term, commitIndex);
    } else {
      return null;
    }
  }

  /*
   * VRP
   */
  private int encode(VoteResponse voteResponse, MutableDirectBuffer directBuffer) {
    int offset = 0;
    messageHeaderEncoder.wrap(directBuffer, offset).blockLength(vrpEncoder.sbeBlockLength())
        .templateId(vrpEncoder.sbeTemplateId()).schemaId(vrpEncoder.sbeSchemaId())
        .version(vrpEncoder.sbeSchemaVersion());
    offset += messageHeaderEncoder.encodedLength();
    vrpEncoder.wrap(directBuffer, offset).term(voteResponse.getTerm()).commitIndex(voteResponse.getCommittedLogIndex())
        .isGranted(parseBooleanType(voteResponse.isGranted())).compositeId(voteResponse.getFullyQualifiedId())
        .originatorId(voteResponse.getFullyQualifiedOriginatorId());

    return vrpEncoder.encodedLength();
  }

  /*
   * VRP
   */
  private VoteResponse decodeVoteResponse(MutableDirectBuffer directBuffer, int bufferOffset,
      MessageHeaderDecoder decoder) {
    bufferOffset += messageHeaderDecoder.encodedLength();
    final int actingBlockLength = messageHeaderDecoder.blockLength();
    final int schemaId = messageHeaderDecoder.schemaId();
    vrpDecoder.wrap(directBuffer, bufferOffset, actingBlockLength, schemaId);
    int term = vrpDecoder.term();
    long commitIndex = vrpDecoder.commitIndex();
    boolean isGranted = parseBoolean(vrpDecoder.isGranted());
    String compositeId = vrpDecoder.compositeId();
    String originatorId = vrpDecoder.originatorId();
    if (compositeId != null && compositeId.length() > 3) {
      return new VoteResponseImpl(compositeId, originatorId, term, commitIndex, isGranted);
    } else {
      return null;
    }
  }

  /*
   * AERQ
   */
  private int encode(AppendEntriesRequest aeRequest, MutableDirectBuffer directBuffer) {
    int offset = 0;
    messageHeaderEncoder.wrap(directBuffer, offset).blockLength(aerqEncoder.sbeBlockLength())
        .templateId(aerqEncoder.sbeTemplateId()).schemaId(aerqEncoder.sbeSchemaId())
        .version(aerqEncoder.sbeSchemaVersion());
    offset += messageHeaderEncoder.encodedLength();
    aerqEncoder.wrap(directBuffer, offset).term(aeRequest.getTerm()).commitIndex(aeRequest.getCommittedLogIndex())
        .isLeaseRequest(parseBooleanType(aeRequest.isLeaseRequest())).compositeId(aeRequest.getFullyQualifiedId())
        .optionalEntries(serializeEntries(aeRequest.entries()));

    return aerqEncoder.encodedLength();
  }

  /*
   * AERQ
   */
  private AppendEntriesRequest decodeAppendEntriesRequest(MutableDirectBuffer directBuffer, int bufferOffset,
      MessageHeaderDecoder decoder) {
    bufferOffset += messageHeaderDecoder.encodedLength();
    final int actingBlockLength = messageHeaderDecoder.blockLength();
    final int schemaId = messageHeaderDecoder.schemaId();

    aerqDecoder.wrap(directBuffer, bufferOffset, actingBlockLength, schemaId);
    int term = aerqDecoder.term();
    long commitIndex = aerqDecoder.commitIndex();
    boolean isLeaseRequest = parseBoolean(aerqDecoder.isLeaseRequest());
    String compositeId = aerqDecoder.compositeId();
    String serializedEntries = aerqDecoder.optionalEntries();
    AppendEntriesRequest aerq = null;
    if (compositeId != null && compositeId.length() > 3) {
      if (serializedEntries.isEmpty() || serializedEntries == null)
        aerq = new AppendEntriesRequestImpl(compositeId, term, commitIndex, Optional.empty(), isLeaseRequest);
      else {
        List<LogEntry> entries = deserializeEntries(serializedEntries);
        aerq = new AppendEntriesRequestImpl(compositeId, term, commitIndex,
            entries == null ? Optional.empty() : Optional.of(entries), isLeaseRequest);
      }
    }
    return aerq;
  }

  /*
   * AERP
   */
  private int encode(AppendEntriesResponse aeResponse, MutableDirectBuffer directBuffer) {
    int offset = 0;
    messageHeaderEncoder.wrap(directBuffer, offset).blockLength(vrpEncoder.sbeBlockLength())
        .templateId(aerpEncoder.sbeTemplateId()).schemaId(aerpEncoder.sbeSchemaId())
        .version(aerpEncoder.sbeSchemaVersion());
    offset += messageHeaderEncoder.encodedLength();
    aerpEncoder.wrap(directBuffer, offset).term(aeResponse.getTerm()).commitIndex(aeResponse.getCommittedLogIndex())
        .isLeaseResponse(parseBooleanType(aeResponse.isLeaseResponse())).compositeId(aeResponse.getFullyQualifiedId())
        .originatorId(aeResponse.getFullyQualifiedOriginatorId());

    return aerpEncoder.encodedLength();
  }

  /*
   * AERP
   */
  private AppendEntriesResponse decodeAppendEntriesResponse(MutableDirectBuffer directBuffer, int bufferOffset,
      MessageHeaderDecoder decoder) {
    bufferOffset += messageHeaderDecoder.encodedLength();
    final int actingBlockLength = messageHeaderDecoder.blockLength();
    final int schemaId = messageHeaderDecoder.schemaId();
    aerpDecoder.wrap(directBuffer, bufferOffset, actingBlockLength, schemaId);
    int term = aerpDecoder.term();
    long commitIndex = aerpDecoder.commitIndex();
    boolean isLeaseResponse = parseBoolean(aerpDecoder.isLeaseResponse());
    String compositeId = aerpDecoder.compositeId();
    String originatorId = aerpDecoder.originatorId();
    if (compositeId != null && compositeId.length() > 3) {
      return new AppendEntriesResponseImpl(compositeId, originatorId, term, commitIndex, isLeaseResponse);
    } else {
      return null;
    }
  }

  /*
   * Auxiliary to Membership
   */
  // JOIN
  private int encode(Join join, MutableDirectBuffer directBuffer) {
    int offset = 0;
    messageHeaderEncoder.wrap(directBuffer, offset).blockLength(joinEncoder.sbeBlockLength())
        .templateId(joinEncoder.sbeTemplateId()).schemaId(joinEncoder.sbeSchemaId())
        .version(joinEncoder.sbeSchemaVersion());
    offset += messageHeaderEncoder.encodedLength();
    joinEncoder.wrap(directBuffer, offset).stateMachineId(join.getStateMachineId())
        .stateMachineClassName(join.getStateMachineClassName()).serializedStateMachine(join.getSerializedStateMachine())
        .memberIds(set2String(join.getMemberIds())).acknowledgements(set2String(join.getAcknowledgements()));

    return joinEncoder.encodedLength();
  }

  private Join decodeJoin(MutableDirectBuffer directBuffer, int bufferOffset, MessageHeaderDecoder decoder) {
    bufferOffset += messageHeaderDecoder.encodedLength();
    final int actingBlockLength = messageHeaderDecoder.blockLength();
    final int schemaId = messageHeaderDecoder.schemaId();
    joinDecoder.wrap(directBuffer, bufferOffset, actingBlockLength, schemaId);
    String stateMachineId = joinDecoder.stateMachineId();
    String stateMachineClassName = joinDecoder.stateMachineClassName();
    String serializedSm = joinDecoder.serializedStateMachine();
    Set<String> memberIds = string2Set(joinDecoder.memberIds());
    Set<String> acks = string2Set(joinDecoder.acknowledgements());
    return new Join(stateMachineId, stateMachineClassName, serializedSm, memberIds, acks);
  }

  // REJOIN
  private int encode(Rejoin rejoin, MutableDirectBuffer directBuffer) {
    int offset = 0;
    messageHeaderEncoder.wrap(directBuffer, offset).blockLength(rejoinEncoder.sbeBlockLength())
        .templateId(rejoinEncoder.sbeTemplateId()).schemaId(rejoinEncoder.sbeSchemaId())
        .version(rejoinEncoder.sbeSchemaVersion());
    offset += messageHeaderEncoder.encodedLength();
    rejoinEncoder.wrap(directBuffer, offset).memberId(rejoin.getMemberId()).stateMachineId(rejoin.getStateMachineId())
        .acknowledgements(set2String(rejoin.getAcknowledgements()));

    return rejoinEncoder.encodedLength();
  }

  private Rejoin decodeRejoin(MutableDirectBuffer directBuffer, int bufferOffset, MessageHeaderDecoder decoder) {
    bufferOffset += messageHeaderDecoder.encodedLength();
    final int actingBlockLength = messageHeaderDecoder.blockLength();
    final int schemaId = messageHeaderDecoder.schemaId();
    rejoinDecoder.wrap(directBuffer, bufferOffset, actingBlockLength, schemaId);
    String memberId = rejoinDecoder.memberId();
    String stateMachineId = rejoinDecoder.stateMachineId();
    Set<String> acks = string2Set(rejoinDecoder.acknowledgements());
    return new Rejoin(memberId, stateMachineId, acks);
  }

  // LEAVE
  private int encode(Leave leave, MutableDirectBuffer directBuffer) {
    int offset = 0;
    messageHeaderEncoder.wrap(directBuffer, offset).blockLength(leaveEncoder.sbeBlockLength())
        .templateId(leaveEncoder.sbeTemplateId()).schemaId(leaveEncoder.sbeSchemaId())
        .version(leaveEncoder.sbeSchemaVersion());
    offset += messageHeaderEncoder.encodedLength();
    leaveEncoder.wrap(directBuffer, offset).stateMachineId(leave.getStateMachineId())
        .originatorId(leave.getOriginatorId()).acknowledgements(set2String(leave.getAcknowledgements()));

    return leaveEncoder.encodedLength();
  }

  private Leave decodeLeave(MutableDirectBuffer directBuffer, int bufferOffset, MessageHeaderDecoder decoder) {
    bufferOffset += messageHeaderDecoder.encodedLength();
    final int actingBlockLength = messageHeaderDecoder.blockLength();
    final int schemaId = messageHeaderDecoder.schemaId();
    leaveDecoder.wrap(directBuffer, bufferOffset, actingBlockLength, schemaId);
    String stateMachineId = leaveDecoder.stateMachineId();
    String memberId = leaveDecoder.originatorId();
    Set<String> acks = string2Set(leaveDecoder.acknowledgements());
    return new Leave(stateMachineId, memberId, acks);
  }

  private static Set<String> string2Set(String data) {
    String[] tokens = data.split(",");
    Set<String> val = new HashSet<String>();
    for (int i = 0; i < tokens.length; i++) {
      val.add(tokens[i]);
    }
    return val;
  }

  private static String set2String(Set<String> data) {
    StringBuilder sb = new StringBuilder();
    Iterator<String> iter = data.iterator();
    while (iter.hasNext()) {
      sb.append(iter.next());
      if (iter.hasNext()) {
        sb.append(",");
      }
    }
    return sb.toString();
  }

  private static boolean parseBoolean(BooleanType b) {
    switch (b.value()) {
    case 0:
      return false;
    case 1:
      return true;
    default:
      return false;
    }
  }

  private static BooleanType parseBooleanType(boolean b) {
    if (b) {
      return BooleanType.TRUE;
    } else {
      return BooleanType.FALSE;
    }
  }

  private static String serializeEntries(Optional<List<LogEntry>> entries) {
    String serialized = "";
    if (entries != null && entries.isPresent()) {
      serialized = mapper.writeValueAsString(entries);
    }
    return serialized;
  }

  @SuppressWarnings("unchecked")
  private static List<LogEntry> deserializeEntries(String serializedEntries) {
    List<LogEntry> entries = null;
    if (serializedEntries != null && !serializedEntries.isEmpty()) {
      try {
        entries = mapper.readValue(serializedEntries, List.class, LogEntry.class);
      } catch (Exception e) {
        LOG.error("Failed to deserialize LogEntries...");
      }
    }
    return entries;
  }
}
