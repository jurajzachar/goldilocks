package org.blueskiron.goldilocks.api;

import java.util.Optional;
import java.util.Set;

import org.blueskiron.goldilocks.api.states.State;

/**
 * @author jurajzachar
 */
public interface Membership {

  /**
   * @param members
   * @return
   */
  public default int quorum() {
    return (members().size() / 2) + 1;
  }

  /**
   * @param members
   * @return
   */
  public default boolean hasMajority(Set<Member> members) {
    return members.size() >= quorum();
  }

  /**
   * @return
   */
  public Set<Member> members();

  /**
   * @param binding
   * @return
   */
  public Optional<Member> getMember(String binding);

  /**
   * @return
   */
  public State localState();

  /**
   * @return
   */
  public Member localMember();

  /**
   * @return
   */
  public Set<Member> remoteMembers();

  /**
   * @return
   */
  public Optional<Member> leader();
}
