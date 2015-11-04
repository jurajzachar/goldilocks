package org.blueskiron.goldilocks.membership;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.Membership;
import org.blueskiron.goldilocks.api.states.State;

import com.google.common.collect.Sets;

/**
 * @author jzachar
 */
public final class GroupMembership implements Membership {

  private final Optional<Member> leader;
  private final Optional<Integer> leaderTerm;
  private final Member localMember;
  private final State localState;
  private final Set<Member> remoteMembers = new HashSet<>();

  private GroupMembership(LocalMember localMember, State localState, Set<Member> remoteMembers,
      Optional<Member> leader, Optional<Integer> leaderTerm) {
    this.leader = leader;
    this.leaderTerm = leaderTerm;
    this.localMember = localMember;
    this.localState = localState;
    this.remoteMembers.addAll(remoteMembers);
  }

  private GroupMembership(Membership oldMembership, State localState, Member leader, int leaderTerm) {
    localMember = oldMembership.localMember();
    remoteMembers.addAll(oldMembership.remoteMembers());
    this.leader = Optional.of(leader);
    this.localState = localState;
    this.leaderTerm = Optional.of(leaderTerm);
  }

  public static GroupMembership createLeaderless(LocalMember localMember, State localState,
      Set<Member> remoteMembers) {
    return new GroupMembership(localMember, localState, remoteMembers, Optional.empty(),
        Optional.empty());
  }

  public static GroupMembership createWithConsensus(Membership oldMembership, Member leader,
      int leaderTerm, State localState) {
    return new GroupMembership(oldMembership, localState, leader, leaderTerm);
  }

  @Override
  public Member localMember() {
    return localMember;
  }

  @Override
  public Set<Member> remoteMembers() {
    return remoteMembers;
  }

  /* LeaderlessMembership is a membership without a consensus */
  @Override
  public Optional<Member> leader() {
    return leader;
  }

  @Override
  public State localState() {
    return localState;
  }
  
  @Override
  public Set<Member> members() {
    return Sets.union(remoteMembers, Sets.newHashSet(localMember));
  }

  @Override
  public Optional<Member> getMember(String binding) {
    Iterator<Member> single = Sets.filter(members(), m -> m.getId().equals(binding)).iterator();
    if(single.hasNext()){
      return Optional.of(single.next());
    } else {
      return Optional.empty();
    }
  }

  @Override
  public String toString() {
    final String outer = "GroupMembership={local: %s %s}";
    String inner = "";
    if (leader().isPresent() && leaderTerm.isPresent()) {
      inner = String.format("with leader=(%s[%d])", leader.get(), leaderTerm.get());
    } else {
      inner = "(leader-less)";
    }
    return String.format(outer, localState, inner);
  }

}
