# CRDT Merge Algorithm Specification (LWW-Element-Register with HLC)

## 1. Problem Statement

In a multi-user collaborative spreadsheet where concurrent edits arrive out-of-order over network partitions and distributed pods, using standard wall-clock timestamps (`System.currentTimeMillis()`) leads to:
1. **Clock Skew Anomaly**: A machine whose clock is 50ms ahead will indefinitely overwrite updates from accurate nodes.
2. **Non-deterministic Concurrency**: Simultaneous edits at identical millisecond timestamps produce non-deterministic results across replicas.

## 2. Hybrid Logical Clock (HLC)

We utilize an HLC timestamp tuple:
$$HLC = (l, c, k)$$
- $l$: Physical time in milliseconds ($l = \text{max}(\text{local\_now}, \text{last\_hlc.l})$)
- $c$: Logical counter for causal ordering at the exact same physical millisecond
- $k$: Node/Client identifier for deterministic tie-breaking

### Total Order Comparison Function
For two timestamps $HLC_1 = (l_1, c_1, k_1)$ and $HLC_2 = (l_2, c_2, k_2)$:

$$
HLC_1 > HLC_2 \iff
\begin{cases}
l_1 > l_2 \\
\text{or } (l_1 = l_2 \land c_1 > c_2) \\
\text{or } (l_1 = l_2 \land c_1 = c_2 \land k_1 > k_2)
\end{cases}
$$

## 3. LWW Merge Strategy

Given cell state $S_{current}$ and incoming update $S_{incoming}$:

$$
\text{merge}(S_{current}, S_{incoming}) = 
\begin{cases}
S_{incoming}, & \text{if } S_{current} = \emptyset \\
S_{incoming}, & \text{if } S_{incoming}.timestamp > S_{current}.timestamp \\
S_{current}, & \text{otherwise (discard older edit)}
\end{cases}
$$

## 4. Convergence & Invariants

- **Idempotence**: $\text{merge}(S, S) = S$
- **Commutativity**: $\text{merge}(A, B) = \text{merge}(B, A)$
- **Associativity**: $\text{merge}(A, \text{merge}(B, C)) = \text{merge}(\text{merge}(A, B), C)$

All distributed replicas (browser clients, Redis cluster instances, PostgreSQL replicas) converge to the exact same cell state without centralized locking.
