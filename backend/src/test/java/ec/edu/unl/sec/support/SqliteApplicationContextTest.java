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

    @Test
    @org.springframework.test.annotation.DirtiesContext(methodMode=org.springframework.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD)
    void manualJudgementUsesRealAcceptedSupportAndRollsBackWhenAuditFails() throws Exception {
        new D7ManualEvaluationData(jdbc,application.getBean(com.fasterxml.jackson.databind.ObjectMapper.class)).run(null);
        var processId=D7ManualEvaluationData.PROCESS;
        var indicator=ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.INDICATOR_A_ID;
        var source=UUID.fromString("d7000000-0000-0000-0000-000000000101");
        try {
            asRole("responsable");
            var input=documentaryDraftCommand();
            var version=evidenceDrafts.create(new ec.edu.unl.sec.evidence.application.EvidenceDraftService.CreateCommand(
                    processId,input.title(),input.description(),input.provenance(),input.coveredFrom(),input.coveredTo(),List.of(indicator)),UUID.randomUUID()).latestVersion();
            sourceCoverages.declare(version.id(),"\"0\"",source,"Cobertura sintética para valoración manual.");
            var reference=evidenceSources.addReference(version.id(),"\"0\"","https://synthetic.invalid/manual","Referencia sintética académica.");
            submission.submit(version.id(),"\""+reference.revision()+"\"");
            asRole("revisor");
            documentReviews.decide(version.id(),new ec.edu.unl.sec.documentreview.application.DocumentReviewService.ReviewInput(
                    0,false,null,"Aceptación documental sintética para valoración.",List.of(
                    new ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome(source,
                            ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome.Outcome.ACCEPTED,null))));
            asRole("evaluador");
            var service=application.getBean(ec.edu.unl.sec.academicevaluation.application.AcademicEvaluationService.class);
            var repository=application.getBean(ec.edu.unl.sec.academicevaluation.application.ManualEvaluationRepository.class);
            var actor=application.getBean(ec.edu.unl.sec.identityaccess.application.CurrentActorPort.class);
            var command=new ec.edu.unl.sec.academicevaluation.application.AcademicEvaluationService.Command(
                    UUID.fromString("d7000000-0000-0000-0000-000000000011"),List.of(version.id()),
                    "Justificación académica sintética.","Limitaciones del piloto sintético.",null);
            var before=documentaryProcesses.context(processId).orElseThrow();
            var failingAudit=org.mockito.Mockito.mock(ec.edu.unl.sec.audit.application.AuditPort.class);
            org.mockito.Mockito.when(failingAudit.record(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("synthetic audit failure"));
            var failing=new ec.edu.unl.sec.academicevaluation.application.AcademicEvaluationService(actor,authorization,documentaryProcesses,
                    application.getBean(ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort.class),
                    application.getBean(ec.edu.unl.sec.evidence.application.RequiredSourceCatalogPort.class),
                    application.getBean(ec.edu.unl.sec.evidence.application.EvidenceProgressPort.class),
                    application.getBean(ec.edu.unl.sec.documentreview.application.DocumentReviewProgressPort.class),repository,failingAudit);
            var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            org.assertj.core.api.Assertions.assertThatThrownBy(()->tx.execute(status -> failing.evaluate(processId,indicator,0,command,UUID.randomUUID())))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(repository.history(processId,indicator,0,20)).isEmpty();
            assertThat(documentaryProcesses.context(processId)).contains(before);
            var saved=service.evaluate(processId,indicator,0,command,UUID.randomUUID());
            assertThat(service.view(processId,indicator,0,20).items()).containsExactly(saved);
            assertThat(saved.supports().getFirst().title()).isEqualTo(input.title());
            assertThat(saved.supports().getFirst().evidenceVersionId()).isEqualTo(version.id());
            assertThat(jdbc.queryForObject("select count(*) from audit_event where action='ACADEMIC_MANUAL_EVALUATION_RECORDED'",Long.class)).isEqualTo(1L);
            var request=new org.springframework.mock.web.MockHttpServletRequest();
            request.setAttribute(ec.edu.unl.sec.shared.infrastructure.CorrelationFilter.CORRELATION_ATTRIBUTE,UUID.randomUUID().toString());
            var controller=application.getBean(ec.edu.unl.sec.academicevaluation.api.AcademicEvaluationController.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.evaluate(processId,indicator,"\"0\"",
                    new ec.edu.unl.sec.academicevaluation.api.AcademicEvaluationController.EvaluateRequest(command.ratingOptionId(),
                            command.evidenceVersionIds(),command.justification(),command.limitations(),null),request))
                    .isInstanceOf(ec.edu.unl.sec.academicevaluation.application.AcademicProblemException.class);
            assertThat(jdbc.queryForObject("select count(*) from audit_event where action='ACADEMIC_MANUAL_EVALUATION_REJECTED'",Long.class)).isEqualTo(1L);
            assertThat(repository.history(processId,indicator,0,20)).containsExactly(saved);
            exerciseQuantitativeCalculation(processId);
            exerciseReconciliation(processId,version.id(),saved);
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    private void exerciseReconciliation(UUID processId,UUID versionId,ec.edu.unl.sec.academicevaluation.domain.ManualAcademicEvaluation first)throws Exception {
        new D7ReconciliationData(jdbc,application.getBean(com.fasterxml.jackson.databind.ObjectMapper.class),
                application.getBean(org.springframework.security.crypto.password.PasswordEncoder.class)).run(null);
        var indicator=first.indicatorId();asRole("evaluador2");
        var academic=application.getBean(ec.edu.unl.sec.academicevaluation.application.AcademicEvaluationService.class);
        var second=academic.evaluate(processId,indicator,0,new ec.edu.unl.sec.academicevaluation.application.AcademicEvaluationService.Command(
                UUID.fromString("d7000000-0000-0000-0000-000000000012"),List.of(versionId),"Fundamento del segundo evaluador sintético.","Limitaciones del segundo juicio sintético.",null),UUID.randomUUID());
        var service=application.getBean(ec.edu.unl.sec.academicevaluation.application.AcademicReconciliationService.class);
        var repository=application.getBean(ec.edu.unl.sec.academicevaluation.application.ReconciliationRepository.class);
        var disagreement=service.open(processId,indicator,0,"Discrepancia sintética entre dos juicios fundamentados.",UUID.randomUUID());
        assertThat(service.view(processId,indicator,0,0,20).status()).isEqualTo("PENDING_RECONCILIATION");
        asRole("conciliador");
        var policy=service.configure(processId,indicator,0,new ec.edu.unl.sec.academicevaluation.application.AcademicReconciliationService.PolicyCommand(
                "Procedimiento sintético 1",ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.METHOD,"Competencia sintética de comité registrada.",
                "fixture://committee-policy",D7ReconciliationData.ASSIGNMENT),UUID.randomUUID());
        var actor=UUID.nameUUIDFromBytes("sec-unl:conciliador".getBytes(StandardCharsets.UTF_8));
        var command=new ec.edu.unl.sec.academicevaluation.application.AcademicReconciliationService.ResolutionCommand(policy.id(),first.id(),
                List.of(first.evaluatorId(),second.evaluatorId(),actor),"Selección sintética fundamentada del primer juicio.",D7ReconciliationData.ASSIGNMENT);
        var failingAudit=org.mockito.Mockito.mock(ec.edu.unl.sec.audit.application.AuditPort.class);
        org.mockito.Mockito.when(failingAudit.record(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("synthetic reconciliation audit failure"));
        var failing=new ec.edu.unl.sec.academicevaluation.application.AcademicReconciliationService(academic,
                application.getBean(ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort.class),
                application.getBean(ec.edu.unl.sec.academicevaluation.application.ManualEvaluationRepository.class),
                application.getBean(ec.edu.unl.sec.academicevaluation.application.QuantitativeRepository.class),repository,authorization,
                application.getBean(ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort.class),failingAudit);
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->tx.execute(status->failing.resolve(processId,indicator,disagreement.id(),1,command,UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.resolution(disagreement.id())).isEmpty();
        var resolution=service.resolve(processId,indicator,disagreement.id(),1,command,UUID.randomUUID());
        assertThat(service.view(processId,indicator,0,0,20).result().id()).isEqualTo(first.id());
        asRole("revisor");
        documentReviews.decide(versionId,new ec.edu.unl.sec.documentreview.application.DocumentReviewService.ReviewInput(
                1,false,null,"Rectificación sintética que exige revisar los sustentos.",List.of(new ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome(
                        UUID.fromString("d7000000-0000-0000-0000-000000000101"),ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome.Outcome.OBSERVED,"Fuente pendiente tras rectificación sintética."))));
        asRole("conciliador");
        var changed=service.view(processId,indicator,0,0,20);assertThat(changed.status()).isEqualTo("REQUIRES_REVIEW");assertThat(changed.result()).isNull();
        assertThat(repository.resolution(disagreement.id())).contains(resolution);
        assertThat(academic.view(processId,indicator,0,20).items()).contains(first,second);
        var request=new org.springframework.mock.web.MockHttpServletRequest();request.setAttribute(ec.edu.unl.sec.shared.infrastructure.CorrelationFilter.CORRELATION_ATTRIBUTE,UUID.randomUUID().toString());
        var controller=application.getBean(ec.edu.unl.sec.academicevaluation.api.AcademicReconciliationController.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->controller.resolve(processId,indicator,disagreement.id(),"\"0\"",
                new ec.edu.unl.sec.academicevaluation.api.AcademicReconciliationController.ResolutionCommand(command.policyId(),command.selectedJudgementId(),command.participants(),command.reason(),command.authorityAssignmentId()),request))
                .isInstanceOf(ec.edu.unl.sec.academicevaluation.application.AcademicProblemException.class);
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action='ACADEMIC_RECONCILIATION_RESOLVED'",Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action='ACADEMIC_RECONCILIATION_REJECTED'",Long.class)).isEqualTo(1L);
    }

    private void exerciseQuantitativeCalculation(UUID processId) {
        var indicator=ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.INDICATOR_B_ID;
        var source=UUID.fromString("d7000000-0000-0000-0000-000000000103");
        asRole("responsable");
        var draft=documentaryDraftCommand();
        var version=evidenceDrafts.create(new ec.edu.unl.sec.evidence.application.EvidenceDraftService.CreateCommand(processId,
                draft.title(),draft.description(),draft.provenance(),draft.coveredFrom(),draft.coveredTo(),List.of(indicator)),UUID.randomUUID()).latestVersion();
        sourceCoverages.declare(version.id(),"\"0\"",source,"Cobertura sintética cuantitativa.");
        var reference=evidenceSources.addReference(version.id(),"\"0\"","https://synthetic.invalid/quantitative","Referencia sintética cuantitativa.");
        submission.submit(version.id(),"\""+reference.revision()+"\"");
        asRole("revisor");
        documentReviews.decide(version.id(),new ec.edu.unl.sec.documentreview.application.DocumentReviewService.ReviewInput(
                0,false,null,"Aceptación documental sintética de cifras.",List.of(new ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome(
                        source,ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome.Outcome.ACCEPTED,null))));
        asRole("evaluador");
        var variables=application.getBean(ec.edu.unl.sec.academicevaluation.application.VariableValueService.class);
        var calculations=application.getBean(ec.edu.unl.sec.academicevaluation.application.AcademicCalculationService.class);
        var repository=application.getBean(ec.edu.unl.sec.academicevaluation.application.QuantitativeRepository.class);
        var inputs=variables.capture(processId,indicator,0,new ec.edu.unl.sec.academicevaluation.application.VariableValueService.Command(
                draft.coveredFrom(),draft.coveredTo(),java.util.Map.of(
                "A",new ec.edu.unl.sec.academicevaluation.application.VariableValueService.ValueInput("3","personas sintéticas",List.of(version.id())),
                "B",new ec.edu.unl.sec.academicevaluation.application.VariableValueService.ValueInput("4","personas sintéticas",List.of(version.id()))),null),UUID.randomUUID());
        org.assertj.core.api.Assertions.assertThatThrownBy(()->calculations.calculate(processId,indicator,inputs.id(),1,
                "Cálculo sintético pendiente de verificar.","Limitaciones sintéticas declaradas.",UUID.randomUUID()))
                .isInstanceOf(ec.edu.unl.sec.academicevaluation.application.AcademicProblemException.class).extracting("code").isEqualTo("VARIABLES_UNVERIFIED");
        variables.verify(processId,indicator,inputs.id(),1,"Cifras, unidades, período y sustentos verificados.",UUID.randomUUID());
        var failingAudit=org.mockito.Mockito.mock(ec.edu.unl.sec.audit.application.AuditPort.class);
        org.mockito.Mockito.when(failingAudit.record(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("synthetic calculation audit failure"));
        var failing=new ec.edu.unl.sec.academicevaluation.application.AcademicCalculationService(
                application.getBean(ec.edu.unl.sec.academicevaluation.application.QuantitativeContext.class),variables,repository,failingAudit);
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->tx.execute(status->failing.calculate(processId,indicator,inputs.id(),1,
                "Justificación cuantitativa sintética.","Limitaciones cuantitativas sintéticas.",UUID.randomUUID()))).isInstanceOf(IllegalStateException.class);
        assertThat(repository.run(inputs.id())).isEmpty();
        var run=calculations.calculate(processId,indicator,inputs.id(),1,"Justificación cuantitativa sintética.","Limitaciones cuantitativas sintéticas.",UUID.randomUUID());
        assertThat(run.result()).isEqualByComparingTo("75");
        assertThat(repository.run(inputs.id())).contains(run);
        assertThat(run.inputs().variables().getFirst().supports().getFirst().evidenceVersionId()).isEqualTo(version.id());
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action='ACADEMIC_CALCULATION_RECORDED'",Long.class)).isEqualTo(1L);
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
            var uploaded=evidenceFiles.upload(version.id(),"\""+reference.revision()+"\"","synthetic.txt","text/plain",
                    new java.io.ByteArrayInputStream("synthetic closure support".getBytes(StandardCharsets.UTF_8)),UUID.randomUUID());
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) { entityManager.flush();entityManager.clear(); }
            submission.submit(version.id(),"\""+uploaded.revision()+"\"");
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
            sourceCoverages.declare(version.id(), "\"1\"", UUID.fromString("80000000-0000-0000-0000-000000000001"),
                    "Cobertura explicita del archivo sintetico de integracion.");
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
