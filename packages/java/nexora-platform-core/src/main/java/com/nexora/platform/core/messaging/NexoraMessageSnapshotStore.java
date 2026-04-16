package com.nexora.platform.core.messaging;

import com.nexora.platform.core.api.MessageSnapshot;
import java.util.Optional;

public interface NexoraMessageSnapshotStore {

  Optional<MessageSnapshot> current();

  void store(MessageSnapshot snapshot);
}
