package com.tomas.tenis.stats.scheduler;

import com.tomas.tenis.stats.model.Partido;
import com.tomas.tenis.stats.model.SyncStatus;
import com.tomas.tenis.stats.repository.PartidoRepository;
import com.tomas.tenis.stats.salesforce.service.SalesforceDataService;
import com.tomas.tenis.stats.util.RetryPolicy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.tomas.tenis.stats.util.ExternalIdGenerator;

import java.util.List;

@Component
public class SyncRetryScheduler {

    private final PartidoRepository partidoRepository;
    private final SalesforceDataService salesforceService;
    private final RetryPolicy retryPolicy;
    private final ExternalIdGenerator externalIdGenerator;

    private static final int MAX_RETRIES = 3;

    public SyncRetryScheduler(PartidoRepository partidoRepository,
                              SalesforceDataService salesforceService,
                              RetryPolicy retryPolicy,
                              ExternalIdGenerator externalIdGenerator) {
        this.partidoRepository = partidoRepository;
        this.salesforceService = salesforceService;
        this.retryPolicy = retryPolicy;
        this.externalIdGenerator = externalIdGenerator;
    }

    @Scheduled(fixedDelay = 60000)
    public void retryFailedSyncs() {

        List<Partido> failed = partidoRepository
                .findBySyncStatusAndRetryCountLessThan(SyncStatus.FAILED, MAX_RETRIES);

        for (Partido partido : failed) {
            if (partido.getExternalId() == null) {
                partido.setExternalId(externalIdGenerator.generarExternalId(partido));
                partidoRepository.save(partido);
            }
            try {
                salesforceService.enviarPartidoASalesforce(partido, partido.getExternalId());
                partido.setSyncStatus(SyncStatus.SUCCESS);
            } catch (Exception e) {

                partido.setRetryCount(partido.getRetryCount() + 1);

                if (!retryPolicy.esErrorReintentable(e) ||
                        partido.getRetryCount() >= MAX_RETRIES) {

                    partido.setSyncStatus(SyncStatus.FAILED_FINAL);

                } else {
                    partido.setSyncStatus(SyncStatus.FAILED);
                }
            }

            partidoRepository.save(partido);
        }
    }
}