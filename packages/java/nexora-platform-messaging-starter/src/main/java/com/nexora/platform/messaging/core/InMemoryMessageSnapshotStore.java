package com.nexora.platform.messaging.core;

import com.nexora.platform.core.api.MessageSnapshot;
import com.nexora.platform.core.messaging.NexoraMessageSnapshotStore;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryMessageSnapshotStore implements NexoraMessageSnapshotStore {

  private final AtomicReference<MessageSnapshot> current = new AtomicReference<>();

  @Override
  public Optional<MessageSnapshot> current() {
    return Optional.ofNullable(current.get());
  }

  @Override
  public void store(MessageSnapshot snapshot) {
    current.set(snapshot);
  }
}
