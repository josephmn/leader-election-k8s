package com.example.leaderelectionk8s.config;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderElectionConfig {

    private final KubernetesClient kubernetesClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String NAMESPACE = "default";
    private static final String LEASE_NAME = "my-leader-election";
    private static final int LEASE_DURATION_SECONDS = 10; // Duración del liderazgo en segundos
//    private static final String INSTANCE_ID = "instance-" + System.getenv("HOSTNAME");
//    private static final String INSTANCE_ID = "instance-" + System.getenv("COMPUTERNAME") + "-" + java.util.UUID.randomUUID();

    public void startLeaderElection() {
        scheduler.scheduleAtFixedRate(this::attemptLeadership, 0, 5, TimeUnit.SECONDS);
    }

    private String getInstanceId() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null || hostname.isEmpty()) {
            try {
                hostname = InetAddress.getLocalHost().getHostName(); // Obtener hostname de la red
            } catch (UnknownHostException e) {
                hostname = "unknown";
            }
        }
        return "instance-" + hostname;
    }

    private void attemptLeadership() {
        try {
            Lease lease = kubernetesClient.leases().inNamespace(NAMESPACE).withName(LEASE_NAME).get();

            if (lease == null) {
                log.info("No existe un Lease. Creando uno nuevo y asumiendo liderazgo...");
                createLease();
                return;
            }

            String currentLeader = lease.getSpec().getHolderIdentity();

            if (currentLeader == null || currentLeader.isEmpty() || isLeaseExpired(lease)) {
                log.info("El líder ha expirado o no existe. Este pod asumirá el liderazgo...");
                updateLease(lease);
                return;
            }

            if (getInstanceId().equals(currentLeader)) {
                renewLease(lease);
            } else {
                log.info("Líder actual: {}. Este pod solo esperará.", currentLeader);
            }
        } catch (KubernetesClientException e) {
            log.error("Error accediendo a Kubernetes API", e);
        }
    }

    private void renewLease(Lease lease) {
        Lease updatedLease = new LeaseBuilder(lease)
                .editSpec()
                .withRenewTime(ZonedDateTime.now(ZoneOffset.UTC)) // Solo renovamos sin cambiar el líder
                .endSpec()
                .build();

        kubernetesClient.leases().inNamespace(NAMESPACE)
                .withName(LEASE_NAME)
                .patch(PatchContext.of(PatchType.STRATEGIC_MERGE), updatedLease);

        log.info("🔄 {} ha renovado el liderazgo.", getInstanceId());
    }

    private void createLease() {
        Lease lease = new LeaseBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(LEASE_NAME)
                        .withNamespace(NAMESPACE)
                        .build())
                .withSpec(new LeaseSpecBuilder()
                        .withHolderIdentity(getInstanceId())
                        .withLeaseDurationSeconds(LEASE_DURATION_SECONDS)
                        .withRenewTime(ZonedDateTime.now(ZoneOffset.UTC))
                        .build())
                .build();

//        kubernetesClient.leases().inNamespace(NAMESPACE).createOrReplace(lease);
        kubernetesClient.leases().inNamespace(NAMESPACE).createOrReplace(lease);
        log.info("🎉 Se ha creado un nuevo Lease y {} es el líder.", getInstanceId());
    }

    private void updateLease(Lease lease) {
        Lease updatedLease = new LeaseBuilder(lease)
                .editSpec()
                .withHolderIdentity(getInstanceId())  // Asigna el liderazgo a este pod
                .withRenewTime(ZonedDateTime.now(ZoneOffset.UTC))
                .endSpec()
                .build();

        kubernetesClient.leases().inNamespace(NAMESPACE)
                .withName(LEASE_NAME)
                .patch(PatchContext.of(PatchType.STRATEGIC_MERGE), updatedLease);

        log.info("✅ {} ha asumido el liderazgo.", getInstanceId());
        executeLeaderTask();
    }

    private boolean isLeaseExpired(Lease lease) {
        if (lease.getSpec() == null || lease.getSpec().getRenewTime() == null) {
            return true; // Si no hay tiempo de renovación, el líder ha expirado
        }

        ZonedDateTime lastRenewTime = lease.getSpec().getRenewTime();
        ZonedDateTime expirationTime = lastRenewTime.plusSeconds(LEASE_DURATION_SECONDS);

        return ZonedDateTime.now(ZoneOffset.UTC).isAfter(expirationTime);
    }
    private void executeLeaderTask() {
        log.info("Ejecutando tarea exclusiva del líder...");
        // TODO: Agregar aquí la lógica que solo debe ejecutar el líder
    }
}
