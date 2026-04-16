package com.nexora.gateway.api;

import com.nexora.gateway.config.GatewayDownstreamServicesProperties;
import com.nexora.platform.core.api.ServiceInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/system/services")
public class GatewayServiceCatalogController {

  private final WebClient.Builder webClientBuilder;
  private final GatewayDownstreamServicesProperties properties;

  public GatewayServiceCatalogController(
      WebClient.Builder webClientBuilder,
      GatewayDownstreamServicesProperties properties) {
    this.webClientBuilder = webClientBuilder;
    this.properties = properties;
  }

  @GetMapping("/catalog")
  public Flux<DownstreamServiceStatusResponse> catalog() {
    return Flux.fromIterable(properties.getDownstreamServices())
        .flatMap(this::inspectService);
  }

  private Mono<DownstreamServiceStatusResponse> inspectService(
      GatewayDownstreamServicesProperties.DownstreamService service) {
    WebClient client = webClientBuilder.baseUrl(service.getBaseUrl()).build();

    Mono<ServiceInfoResponse> infoMono =
        client.get()
            .uri(service.getInfoPath())
            .retrieve()
            .bodyToMono(ServiceInfoResponse.class)
            .timeout(properties.getProbeTimeout());

    Mono<Boolean> docsMono =
        client.get()
            .uri(service.getDocsPath())
            .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
            .timeout(properties.getProbeTimeout());

    return Mono.zip(infoMono, docsMono)
        .map(tuple ->
            new DownstreamServiceStatusResponse(
                service.getService(),
                service.getBaseUrl(),
                "UP",
                tuple.getT2(),
                tuple.getT1(),
                null))
        .onErrorResume(
            exception ->
                Mono.just(
                    new DownstreamServiceStatusResponse(
                        service.getService(),
                        service.getBaseUrl(),
                        "DOWN",
                        false,
                        null,
                        exception.getMessage())));
  }
}
