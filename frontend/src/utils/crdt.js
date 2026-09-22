/**
 * Pure JavaScript implementation of Hybrid Logical Clock (HLC) & CRDT LWW merge logic
 * Mirrors the backend Java implementation for 100% deterministic convergence.
 */

export class HybridLogicalClock {
  constructor(physicalTime, logicalCounter, clientId) {
    this.physicalTime = physicalTime;
    this.logicalCounter = logicalCounter;
    this.clientId = clientId;
  }

  static now(clientId) {
    return new HybridLogicalClock(Date.now(), 0, clientId);
  }

  update(remoteHlc) {
    const now = Date.now();
    const remotePhysical = remoteHlc.physicalTime;
    const maxPhysical = Math.max(now, Math.max(this.physicalTime, remotePhysical));

    let newCounter;
    if (maxPhysical === this.physicalTime && maxPhysical === remotePhysical) {
      newCounter = Math.max(this.logicalCounter, remoteHlc.logicalCounter) + 1;
    } else if (maxPhysical === this.physicalTime) {
      newCounter = this.logicalCounter + 1;
    } else if (maxPhysical === remotePhysical) {
      newCounter = remoteHlc.logicalCounter + 1;
    } else {
      newCounter = 0;
    }

    this.physicalTime = maxPhysical;
    this.logicalCounter = newCounter;
    return this;
  }

  compareTo(other) {
    if (!other) return 1;
    if (this.physicalTime !== other.physicalTime) {
      return this.physicalTime - other.physicalTime;
    }
    if (this.logicalCounter !== other.logicalCounter) {
      return this.logicalCounter - other.logicalCounter;
    }
    return this.clientId.localeCompare(other.clientId);
  }

  toCompactString() {
    return `${this.physicalTime}:${this.logicalCounter}:${this.clientId}`;
  }

  static fromString(str) {
    if (!str) return null;
    const parts = str.split(':');
    if (parts.length < 3) return null;
    return new HybridLogicalClock(
      parseInt(parts[0], 10),
      parseInt(parts[1], 10),
      parts[2]
    );
  }
}

/**
 * Last-Write-Wins CRDT merge for CellState
 */
export function mergeCellState(currentState, incomingState) {
  if (!currentState) return incomingState;
  if (!incomingState) return currentState;

  const currentHlc = currentState.timestamp instanceof HybridLogicalClock
    ? currentState.timestamp
    : (typeof currentState.timestamp === 'string'
        ? HybridLogicalClock.fromString(currentState.timestamp)
        : new HybridLogicalClock(currentState.timestamp.physicalTime, currentState.timestamp.logicalCounter, currentState.timestamp.clientId));

  const incomingHlc = incomingState.timestamp instanceof HybridLogicalClock
    ? incomingState.timestamp
    : (typeof incomingState.timestamp === 'string'
        ? HybridLogicalClock.fromString(incomingState.timestamp)
        : new HybridLogicalClock(incomingState.timestamp.physicalTime, incomingState.timestamp.logicalCounter, incomingState.timestamp.clientId));

  if (incomingHlc.compareTo(currentHlc) > 0) {
    return incomingState;
  }
  return currentState;
}
