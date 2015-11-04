package org.blueskiron.goldilocks.membership;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.Raft;

/**
 * @author jzachar
 */
public class LocalMember extends Member {

    private final Map<String, Raft> resources = new ConcurrentHashMap<>();

    public LocalMember(final String binding) {
        super(binding);
    }

    protected Collection<Raft> getAllResources(){
        return resources.values();
    }
    
    protected Optional<Raft> getResource(String id) {
        return resources.containsKey(id) ? Optional.of(resources.get(id)) : Optional.empty();
    }

    void addResource(Rafter raft) {
        resources.put(raft.getStateMachineId(), raft);
    }

    void removeResource(String stateMachineId) {
        resources.remove(stateMachineId);
    }

    void startResource(String stateMachineId) {
        getResource(stateMachineId).ifPresent(raft -> raft.start());
    }

    void stopResource(String stateMachineId) {
        getResource(stateMachineId).ifPresent(raft -> raft.stop());
    }

    void startAllResources() {
        resources.values().stream().forEach(r -> r.start());
    }

    void stopAllResources() {
        resources.values().stream().forEach(r -> r.stop());
    }
}
