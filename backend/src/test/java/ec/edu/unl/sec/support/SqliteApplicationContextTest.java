package ec.edu.unl.sec.support;

import static org.assertj.core.api.Assertions.assertThat;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.audit.application.AuditQueryPort;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("sqlite-test")
class SqliteApplicationContextTest {
    private static final String DATABASE = "sqlite-context-" + UUID.randomUUID();
    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path fileRoot;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:file:" + DATABASE + "?mode=memory&cache=shared");
        registry.add("sec.files.root", () -> fileRoot.toString());
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired SessionService sessions;
    @Autowired AuthorizationPort authorization;
    @Autowired AuditQueryPort audit;
    @Autowired ec.edu.unl.sec.evidence.application.EvidenceDraftService evidenceDrafts;
    @Autowired ec.edu.unl.sec.evidence.application.EvidenceSourceService evidenceSources;
    @Autowired ec.edu.unl.sec.evidence.application.EvidenceFileService evidenceFiles;
    @Autowired ec.edu.unl.sec.evidence.application.SubmitEvidenceService submission;
    @Autowired ec.edu.unl.sec.evidence.application.EvidenceDownloadService downloads;
    @Autowired ec.edu.unl.sec.evidence.api.EvidenceDraftController evidenceController;
    @Autowired ec.edu.unl.sec.reporting.application.DocumentaryProgressQueryService documentaryProgress;
    @Autowired ec.edu.unl.sec.reporting.application.DocumentarySnapshotService documentarySnapshots;
    @Autowired ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort documentaryProcesses;
    @Autowired org.springframework.context.ApplicationContext application;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired ec.edu.unl.sec.reporting.application.DocumentaryClosureService documentaryClosures;
    @Autowired ec.edu.unl.sec.documentreview.application.DocumentReviewService documentReviews;
    @Autowired ec.edu.unl.sec.evidence.application.EvidenceSourceCoverageService sourceCoverages;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void closesAcceptedSourcesWithoutAcademicsAndFreezesDocumentaryWrites() {
        var versions=acceptedDocumentarySources();
        var processId=ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID;
        asRole("coordinador");
        try {
            var before=documentaryProcesses.context(processId).orElseThrow();
            var closure=documentaryClosures.close(processId,before.revision(),"DOCUMENTARY_STRICT_V1",
                    "Cierre documental sintético sin módulos académicos.",UUID.randomUUID());
            assertThat(documentaryProcesses.context(processId).orElseThrow().state()).isEqualTo("DOCUMENTARY_CLOSED");
            assertThat(closure.content().processRevision()).isEqualTo(before.revision()+2);
            assertThat(documentaryClosures.get(closure.content().id())).isEqualTo(closure);
            var cut=documentarySnapshots.get(closure.content().snapshotId());
            assertThat(cut.content().progress().acceptedSourceTotal()).isEqualTo(2);
            assertThat(cut.content().processState()).isEqualTo("ACTIVE");
            asRole("responsable");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> evidenceDrafts.create(documentaryDraftCommand(),UUID.randomUUID()))
                    .isInstanceOf(ec.edu.unl.sec.evidence.application.EvidenceProblemException.class)
                    .extracting("code").isEqualTo("PROCESS_NOT_ACTIVE");
            asRole("revisor");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> documentReviews.decide(versions.getFirst(),
                    new ec.edu.unl.sec.documentreview.application.DocumentReviewService.ReviewInput(1,false,null,
                            "Revisión tardía que debe bloquear el cierre.",List.of(new ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome(
                                    UUID.fromString("80000000-0000-0000-0000-000000000001"),
                                    ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome.Outcome.ACCEPTED,null)))))
                    .isInstanceOf(ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException.class)
                    .extracting("code").isEqualTo("PROCESS_NOT_ACTIVE");
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    @Test
    void failedClosureAuditRollsBackStateCutHistoryAndClosure() {
        acceptedDocumentarySources();
        var processId=ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID;
        asRole("coordinador");
        try {
            var before=documentaryProcesses.context(processId).orElseThrow();
            var cutCount=jdbc.queryForObject("select count(*) from documentary_snapshot",Long.class);
            var transitionCount=jdbc.queryForObject("select count(*) from process_transition",Long.class);
            var closureCount=jdbc.queryForObject("select count(*) from documentary_closure",Long.class);
            var failingAudit=org.mockito.Mockito.mock(ec.edu.unl.sec.audit.application.AuditPort.class);
            org.mockito.Mockito.when(failingAudit.record(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("synthetic closure audit failure"));
            var failing=new ec.edu.unl.sec.reporting.application.DocumentaryClosureService(
                    application.getBean(ec.edu.unl.sec.identityaccess.application.CurrentActorPort.class),authorization,documentaryProcesses,
                    documentarySnapshots,application.getBean(ec.edu.unl.sec.reporting.application.DocumentaryClosureRepository.class),
                    application.getBean(ec.edu.unl.sec.reporting.application.DocumentarySnapshotCodec.class),
                    application.getBean(ec.edu.unl.sec.evidence.application.EvidenceIntegrityPort.class),failingAudit);
            var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> tx.execute(status -> failing.close(processId,before.revision(),
                    "DOCUMENTARY_STRICT_V1","Cierre que debe revertirse por fallo de auditoría.",UUID.randomUUID())))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(documentaryProcesses.context(processId).orElseThrow()).isEqualTo(before);
            assertThat(jdbc.queryForObject("select count(*) from documentary_snapshot",Long.class)).isEqualTo(cutCount);
            assertThat(jdbc.queryForObject("select count(*) from process_transition",Long.class)).isEqualTo(transitionCount);
            assertThat(jdbc.queryForObject("select count(*) from documentary_closure",Long.class)).isEqualTo(closureCount);
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    private List<UUID> acceptedDocumentarySources() {
        var versions=new java.util.ArrayList<UUID>();
        for (int number=1;number<=2;number++) {
            asRole("responsable");
            var indicator=UUID.fromString("50000000-0000-0000-0000-00000000000"+number);
            var source=UUID.fromString("80000000-0000-0000-0000-00000000000"+number);
            var command=documentaryDraftCommand();
            var version=evidenceDrafts.create(new ec.edu.unl.sec.evidence.application.EvidenceDraftService.CreateCommand(
                    command.processId(),command.title(),command.description(),command.provenance(),command.coveredFrom(),command.coveredTo(),List.of(indicator)),UUID.randomUUID()).latestVersion();
            sourceCoverages.declare(version.id(),"\"0\"",source,"Cobertura sintética para cierre documental.");
            var reference=evidenceSources.addReference(version.id(),"\"0\"","https://synthetic.invalid/closure/"+number,"Referencia sintética documental.");
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) { entityManager.flush();entityManager.clear(); }
            submission.submit(version.id(),"\""+reference.revision()+"\"");
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) { entityManager.flush();entityManager.clear(); }
            asRole("revisor");
            documentReviews.decide(version.id(),new ec.edu.unl.sec.documentreview.application.DocumentReviewService.ReviewInput(
                    0,false,null,"Aceptación documental sintética para cierre independiente.",List.of(
                            new ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome(source,
                                    ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome.Outcome.ACCEPTED,null))));
            versions.add(version.id());
        }
        return versions;
    }
    private ec.edu.unl.sec.evidence.application.EvidenceDraftService.CreateCommand documentaryDraftCommand() {
        return new ec.edu.unl.sec.evidence.application.EvidenceDraftService.CreateCommand(
                ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID,"Evidencia sintética para cierre", "Descripción documental sintética.",
                "Fixture sintético de cierre",java.time.LocalDate.of(2026,1,1),java.time.LocalDate.of(2026,6,30),
                List.of(ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.INDICATOR_A_ID));
    }
    private void asRole(String login) {
        var id=UUID.nameUUIDFromBytes(("sec-unl:"+login).getBytes(StandardCharsets.UTF_8));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(id,null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_HUMAN"))));
    }

    @Test
    void snapshotReservesRevisionWithoutClosingAndRollsBackIfAuditFails() {
        var processId=ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID;
        var coordinator=UUID.nameUUIDFromBytes("sec-unl:coordinador".getBytes(StandardCharsets.UTF_8));
        var security=org.springframework.security.core.context.SecurityContextHolder.getContext();
        security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                coordinator,null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_HUMAN"))));
        try {
            var before=documentaryProcesses.context(processId).orElseThrow();
            var snapshot=documentarySnapshots.create(processId,before.revision(),"Corte documental guardado sin cerrar el proceso.",UUID.randomUUID());
            var after=documentaryProcesses.context(processId).orElseThrow();
            assertThat(after.state()).isEqualTo(before.state());
            assertThat(after.revision()).isEqualTo(before.revision()+1);
            assertThat(documentarySnapshots.get(snapshot.content().id())).isEqualTo(snapshot);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> documentarySnapshots.create(processId,before.revision(),
                    "Corte con revisión obsoleta rechazado.",UUID.randomUUID()))
                    .isInstanceOf(ec.edu.unl.sec.reporting.application.ReportingProblemException.class)
                    .extracting("code").isEqualTo("STALE_PROCESS");
            var failingAudit=org.mockito.Mockito.mock(ec.edu.unl.sec.audit.application.AuditPort.class);
            org.mockito.Mockito.when(failingAudit.record(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("synthetic audit failure"));
            var failingService=new ec.edu.unl.sec.reporting.application.DocumentarySnapshotService(
                    application.getBean(ec.edu.unl.sec.identityaccess.application.CurrentActorPort.class),authorization,documentaryProcesses,
                    documentaryProgress,application.getBean(ec.edu.unl.sec.evidence.application.EvidenceProgressPort.class),
                    application.getBean(ec.edu.unl.sec.reporting.application.DocumentarySnapshotRepository.class),
                    application.getBean(ec.edu.unl.sec.reporting.application.DocumentarySnapshotCodec.class),failingAudit);
            var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE);
            var count=jdbc.queryForObject("select count(*) from documentary_snapshot",Long.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> tx.execute(status -> failingService.create(processId,after.revision(),
                    "Corte que debe revertirse por fallo de auditoría.",UUID.randomUUID()))).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForObject("select count(*) from documentary_snapshot",Long.class)).isEqualTo(count);
            assertThat(documentaryProcesses.context(processId).orElseThrow().revision()).isEqualTo(after.revision());
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    @Test
    void coordinatorReadsDocumentaryProgressThroughPublicPortsWithRealAuthorization() {
        var actor = UUID.nameUUIDFromBytes("sec-unl:coordinador".getBytes(StandardCharsets.UTF_8));
        var security = org.springframework.security.core.context.SecurityContextHolder.getContext();
        security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                actor, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_HUMAN"))));
        try {
            var view = documentaryProgress.query(ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID,
                    null, null, 0, 20);
            assertThat(view.indicatorIds()).hasSize(2);
            assertThat(view.progress().formulaVersion()).isEqualTo("SOURCE_COVERAGE_V1");
            var responsible = UUID.nameUUIDFromBytes("sec-unl:responsable".getBytes(StandardCharsets.UTF_8));
            security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    responsible, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_HUMAN"))));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> documentaryProgress.query(
                    ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID, null, null, 0, 20))
                    .isInstanceOf(ec.edu.unl.sec.reporting.application.ReportingProblemException.class)
                    .extracting("code").isEqualTo("FORBIDDEN");
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    @Test
    void startsWithSyntheticPilotData() {
        assertThat(jdbc.queryForObject("select count(*) from access_identity", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from self_assessment_process", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from process_assignment", Integer.class)).isEqualTo(2);
    }

    @Test
    void authenticatesAndUsesTheSessionAgainstSqlite() {
        var login = sessions.login("admin", "Synthetic-Admin-2026", "127.0.0.1", UUID.randomUUID());

        assertThat(sessions.authenticateForRequest(login.token()))
                .isPresent()
                .get()
                .extracting(SessionService.AuthenticatedSession::displayName)
                .isEqualTo("Administrador Sintetico");
        assertThat(audit.search(List.of(), true, null, null, 0, 20)).isNotEmpty();
        sessions.logout(login.token(), login.identityId(), UUID.randomUUID());
        assertThat(sessions.authenticateForRequest(login.token())).isEmpty();
    }

    @Test
    void responsibleCanCollectWithoutAFalseFunctionConflict() {
        UUID responsible = UUID.nameUUIDFromBytes("sec-unl:responsable".getBytes(StandardCharsets.UTF_8));
        assertThat(authorization.isAllowed(
                responsible,
                OperationCode.EVIDENCE_COLLECT,
                new AuthorizationScope(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        UUID.fromString("40000000-0000-0000-0000-000000000001"),
                        UUID.fromString("50000000-0000-0000-0000-000000000001"))))
                .isTrue();
    }

    @Test
    void authorEditsReferencesWithSharedJpaJdbcTransactionsAndVersionReadsIncludeThem() {
        UUID responsible = UUID.nameUUIDFromBytes("sec-unl:responsable".getBytes(StandardCharsets.UTF_8));
        var security = org.springframework.security.core.context.SecurityContextHolder.getContext();
        security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                responsible, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_HUMAN"))));
        try {
            var version = evidenceDrafts.create(new ec.edu.unl.sec.evidence.application.EvidenceDraftService.CreateCommand(
                    UUID.fromString("40000000-0000-0000-0000-000000000001"), "Referencia sintetica", "Descripcion sintetica",
                    "Fixture de integracion", java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 6, 30),
                    List.of(UUID.fromString("50000000-0000-0000-0000-000000000001"))), UUID.randomUUID()).latestVersion();
            var added = evidenceSources.addReference(version.id(), "\"0\"", "https://synthetic.invalid/document", "Declaracion");
            assertThat(added.revision()).isEqualTo(1);
            assertThat(evidenceDrafts.getVersion(version.id()).lockRevision()).isEqualTo(1);
            var response = evidenceController.getVersion(version.id());
            assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
            assertThat(response.getBody().externalReferences()).hasSize(1);
            assertThat(response.getBody().externalReferences().getFirst().uri()).isEqualTo("https://synthetic.invalid/document");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> evidenceSources.removeReference(version.id(), added.value().id(), "\"0\""))
                    .isInstanceOf(ec.edu.unl.sec.evidence.application.EvidenceProblemException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> evidenceSources.addReference(version.id(), "\"1\"", "relative/path", "Invalida"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(evidenceDrafts.getVersion(version.id()).lockRevision()).isEqualTo(1);
            assertThat(evidenceSources.snapshot(version.id()).externalReferences()).hasSize(1);
            evidenceSources.updateReference(version.id(), added.value().id(), "\"1\"", "urn:synthetic:updated", "Actualizada");
            assertThat(evidenceSources.removeReference(version.id(), added.value().id(), "\"2\"")).isEqualTo(3);
            assertThat(evidenceSources.snapshot(version.id()).externalReferences()).isEmpty();
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    @Test
    void uploadsValidatesPromotesAndAttachesASyntheticFileToTheDraft() throws Exception {
        UUID responsible = UUID.nameUUIDFromBytes("sec-unl:responsable".getBytes(StandardCharsets.UTF_8));
        var security = org.springframework.security.core.context.SecurityContextHolder.getContext();
        security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                responsible, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_HUMAN"))));
        try {
            var version = evidenceDrafts.create(new ec.edu.unl.sec.evidence.application.EvidenceDraftService.CreateCommand(
                    UUID.fromString("40000000-0000-0000-0000-000000000001"), "Archivo sintetico", "Descripcion sintetica",
                    "Fixture de integracion", java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 6, 30),
                    List.of(UUID.fromString("50000000-0000-0000-0000-000000000001"))), UUID.randomUUID()).latestVersion();
            var uploaded = evidenceFiles.upload(version.id(), "\"0\"", "synthetic.txt", "text/plain",
                    new java.io.ByteArrayInputStream("synthetic evidence".getBytes(StandardCharsets.UTF_8)), UUID.randomUUID());
            assertThat(uploaded.revision()).isEqualTo(1);
            assertThat(uploaded.value().status()).isEqualTo(ec.edu.unl.sec.filestorage.domain.FileAsset.Status.AVAILABLE);
            assertThat(uploaded.value().snapshot().originalName()).isEqualTo("synthetic.txt");
            assertThat(uploaded.value().snapshot().sizeBytes()).isEqualTo(18);
            var sources = evidenceSources.snapshot(version.id());
            assertThat(sources.files()).hasSize(1);
            assertThat(sources.files().getFirst().snapshot().fileId())
                    .isEqualTo(uploaded.value().snapshot().fileId());

            assertThat(evidenceDrafts.getVersion(version.id()).lockRevision()).isEqualTo(1L);
            var submitted = submission.submit(version.id(), "\"1\"");
            assertThat(submitted.status()).isEqualTo(ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.SUBMITTED);
            assertThat(submitted.lockRevision()).isEqualTo(2);

            var downloaded = downloads.prepare(version.id(), uploaded.value().snapshot().fileId());
            assertThat(downloaded.filename()).isEqualTo("synthetic.txt");
            assertThat(downloaded.sha256()).isEqualTo(uploaded.value().snapshot().sha256());
            assertThat(new String(downloaded.content(), StandardCharsets.UTF_8)).isEqualTo("synthetic evidence");
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
}
