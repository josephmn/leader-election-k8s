package com.example.leaderelectionk8s.config;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

    private final KubernetesClient kubernetesClient = new DefaultKubernetesClient();

    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }

    @PreDestroy
    public void shutdown() {
        kubernetesClient.close();
    }
}
