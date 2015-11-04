package org.blueskiron.goldilocks.membership.net;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.blueskiron.goldilocks.api.messages.MembershipMessage;
import org.blueskiron.goldilocks.api.messages.RaftMessage;
import org.blueskiron.goldilocks.membership.GroupMembershipAware;

import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

/**
 * A simple Reactive Event Bus based on BehaviourSubject.
 * @author jzachar
 */
public final class RxBus {

  // all-around subject used for both requests and responses
  private final BehaviorSubject<Object> messages = BehaviorSubject.create();
  private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

  /**
   * publish event to the subject
   * @param event
   */
  public void publish(Object event) {
    try {
      messages.onNext(event);
    } catch (Exception e) {
      messages.onError(e);
    }
  }

  public void unsubscribeFromAll() {
    subscriptions.keySet().forEach(smId -> unsubscribe(smId));
  }

  /**
   * Unsubscribe from specifig StateMachine events.
   * @param stateMachineId
   */
  public void unsubscribe(String stateMachineId) {
    if (subscriptions.containsKey(stateMachineId)) {
      subscriptions.get(stateMachineId).unsubscribe();
    }
  }

  /**
   * Subscribe to all membership events. This is reserved for cluster only.
   * @param action
   * @return
   */
  public void subscribeToMembership(String clusterId, GroupMembershipAware cluster) {
    subscriptions.put(clusterId,
        messages.filter(filterByUnknownStateMachines()).subscribe(new Action1<Object>() {
          @Override
          public void call(Object event) {
            if (event instanceof RaftMessage) {
              cluster.onRaftMessage((RaftMessage) event);
            } else if (event instanceof MembershipMessage) {
              cluster.onMembershipMessage((MembershipMessage) event);
            }
          }
        }));
  }

  /**
   * Subscribe to events filtered by a StateMachineId.
   * @param action
   * @return
   */
  public void subscribeToRaft(String stateMachineId, Action1<Object> action) {
    Subscription sub = messages.filter(filterByStateMachine(stateMachineId)).subscribe(action);
    subscriptions.put(stateMachineId, sub);
  }

  /**
   * Creates filter predicate for subscription call based on StateMachine id parameter.
   * @param stateMachineId
   * @return
   */
  private Func1<Object, Boolean> filterByStateMachine(String stateMachineId) {
    return new Func1<Object, Boolean>() {
      @Override
      public Boolean call(Object event) {
        boolean accept = false;
        if (event instanceof RaftMessage) {
          RaftMessage rmsg = (RaftMessage) event;
          accept = stateMachineId.equals(rmsg.getStateMachineId());
        }
        return accept;
      }
    };
  }

  /**
   * Creates filter predicate for subscription call based on whether the StateMachine id is known or
   * not.
   * @return
   */
  private Func1<Object, Boolean> filterByUnknownStateMachines() {
    return new Func1<Object, Boolean>() {
      @Override
      public Boolean call(Object event) {
        boolean accept = false;
        // unknown state machine id means we've fallen out of the membership and we must rejoin
        if (event instanceof RaftMessage) {
          RaftMessage rmsg = (RaftMessage) event;
          accept = !subscriptions.containsKey(rmsg.getStateMachineId());
          // all membership life cycle messages are expected here
        } else if (event instanceof MembershipMessage) {
          accept = true;
        }
        return accept;
      }
    };
  }
}
