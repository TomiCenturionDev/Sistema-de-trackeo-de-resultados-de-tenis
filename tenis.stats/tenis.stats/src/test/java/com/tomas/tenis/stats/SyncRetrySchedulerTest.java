package com.tomas.tenis.stats;

import com.tomas.tenis.stats.model.Partido;
import com.tomas.tenis.stats.model.SyncStatus;
import com.tomas.tenis.stats.repository.PartidoRepository;
import com.tomas.tenis.stats.salesforce.service.SalesforceDataService;
import com.tomas.tenis.stats.scheduler.SyncRetryScheduler;
import com.tomas.tenis.stats.util.ExternalIdGenerator;
import com.tomas.tenis.stats.util.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SyncRetrySchedulerTest {

    @Mock
    private PartidoRepository partidoRepository;

    @Mock
    private SalesforceDataService salesforceService;

    @Mock
    private RetryPolicy retryPolicy;

    @Mock
    private ExternalIdGenerator externalIdGenerator;

    private SyncRetryScheduler scheduler;

    @BeforeEach
    public void setUp() {
        scheduler = new SyncRetryScheduler(
                partidoRepository,
                salesforceService,
                retryPolicy,
                externalIdGenerator
        );
    }

    @Test
    public void deberiaPasarAFailedFinalCuandoSuperaMaxRetries() {

        Partido partido = new Partido();
        partido.setRetryCount(2); // suponiendo MAX_RETRIES = 3
        partido.setSyncStatus(SyncStatus.FAILED);

        when(partidoRepository.findBySyncStatusAndRetryCountLessThan(eq(SyncStatus.FAILED), anyInt()))
                .thenReturn(List.of(partido));

        RuntimeException error = new RuntimeException();

        when(salesforceService.enviarPartidoASalesforce(any(Partido.class), any()))
                .thenThrow(error);

        when(retryPolicy.esErrorReintentable(error))
                .thenReturn(true);

        scheduler.retryFailedSyncs();

        assertEquals(3, partido.getRetryCount());
        assertEquals(SyncStatus.FAILED_FINAL, partido.getSyncStatus());

        // 🔥 ahora hay 2 saves (externalId + resultado final)
        verify(partidoRepository, times(2)).save(partido);
    }

    @Test
    public void deberiaPasarASuccessCuandoRetryEsExitoso() {

        Partido partido = new Partido();
        partido.setRetryCount(1);
        partido.setSyncStatus(SyncStatus.FAILED);

        // 🔥 CLAVE
        partido.setExternalId("TEST_ID");

        when(partidoRepository.findBySyncStatusAndRetryCountLessThan(eq(SyncStatus.FAILED), anyInt()))
                .thenReturn(List.of(partido));

        when(partidoRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doReturn("OK").when(salesforceService)
                .enviarPartidoASalesforce(any(Partido.class), any());

        scheduler.retryFailedSyncs();

        assertEquals(SyncStatus.SUCCESS, partido.getSyncStatus());
        assertEquals(1, partido.getRetryCount());

        verify(salesforceService).enviarPartidoASalesforce(any(), notNull());
        verify(partidoRepository, times(1)).save(partido); // 🔥 ahora es 1 porque no entra al if
    }

    @Test
    public void deberiaIrAFailedFinalSiElErrorNoEsReintentable() {

        Partido partido = new Partido();
        partido.setRetryCount(0);
        partido.setSyncStatus(SyncStatus.FAILED);

        when(partidoRepository.findBySyncStatusAndRetryCountLessThan(eq(SyncStatus.FAILED), anyInt()))
                .thenReturn(List.of(partido));

        RuntimeException error = new RuntimeException("Error no reintentable");

        when(salesforceService.enviarPartidoASalesforce(any(Partido.class), any()))
                .thenThrow(error);

        when(retryPolicy.esErrorReintentable(error))
                .thenReturn(false);

        scheduler.retryFailedSyncs();

        assertEquals(SyncStatus.FAILED_FINAL, partido.getSyncStatus());
        assertEquals(1, partido.getRetryCount());

        verify(partidoRepository, times(2)).save(partido);
    }
}