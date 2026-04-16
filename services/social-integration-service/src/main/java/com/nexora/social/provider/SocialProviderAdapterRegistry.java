package com.nexora.social.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SocialProviderAdapterRegistry {

  private final Map<SocialProvider, SocialProviderAdapter> adapters;

  public SocialProviderAdapterRegistry(List<SocialProviderAdapter> adapters) {
    this.adapters = adapters.stream()
        .collect(Collectors.toUnmodifiableMap(SocialProviderAdapter::provider, Function.identity()));
  }

  public SocialProviderAdapter require(SocialProvider provider) {
    SocialProviderAdapter adapter = adapters.get(provider);
    if (adapter == null) {
      throw new IllegalArgumentException("No adapter registered for provider: " + provider.code());
    }
    if (!adapter.descriptor().enabled()) {
      throw new IllegalStateException("Provider is currently disabled: " + provider.code());
    }
    return adapter;
  }

  public List<SocialProviderAdapter.ProviderDescriptor> descriptors() {
    return adapters.values().stream()
        .map(SocialProviderAdapter::descriptor)
        .sorted(java.util.Comparator.comparing(descriptor -> descriptor.provider().code()))
        .toList();
  }
}
