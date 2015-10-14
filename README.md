# Goldilocks
State Machine Replicator for Java based on Raft consensus protocol.

The Goldilocks Replicator is a concrete Java implementation of the Raft consensus protocol. For more information on Raft specification please see https://raftconsensus.github.io. Raft aims to solve some pressing challenges in distributed computing, such as, safety of replicated state machines, strongly consistent state logs, remote executing and failure recovery.

This implementation of Raft also makes a conceptual distinction between a physical cluster membership and logical resource replication. By doing so, the protocol allows for independent coexistence of the state machines and their replicas within one single cluster membership.
