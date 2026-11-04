package ec.edu.unl.sec.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "ec.edu.unl.sec", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleArchitectureTest {
    @ArchTest
    static final ArchRule GOVERNANCE_USES_PUBLIC_CROSS_MODULE_PORTS = noClasses()
            .that().resideInAPackage("..governance..")
            .should().dependOnClassesThat().resideInAnyPackage("..academicevaluation.infrastructure..",
                    "..documentreview.infrastructure..", "..selfassessment.infrastructure..", "..audit.infrastructure..");

    @ArchTest
    static final ArchRule IMPROVEMENT_USES_PUBLIC_CROSS_MODULE_PORTS = noClasses()
            .that().resideInAPackage("..improvement..")
            .should().dependOnClassesThat().resideInAnyPackage("..governance.infrastructure..",
                    "..selfassessment.infrastructure..", "..audit.infrastructure..");

    @ArchTest
    static final ArchRule ARCHIVE_USES_PUBLIC_CROSS_MODULE_PORTS = noClasses()
            .that().resideInAPackage("..archive..")
            .should().dependOnClassesThat().resideInAnyPackage("..reporting.infrastructure..",
                    "..governance.infrastructure..", "..improvement.infrastructure..",
                    "..selfassessment.infrastructure..", "..audit.infrastructure..");

    @ArchTest
    static final ArchRule REPORTING_USES_PUBLIC_SOURCE_PORTS = noClasses()
            .that().resideInAPackage("..reporting..")
            .should().dependOnClassesThat().resideInAnyPackage("..evidence.infrastructure..",
                    "..documentreview.infrastructure..", "..identityaccess.infrastructure..",
                    "..evaluationmodel.infrastructure..", "..selfassessment.infrastructure..");

    @ArchTest
    static final ArchRule DOCUMENTARY_REPORTING_IS_INDEPENDENT_OF_LATER_WORKFLOWS = noClasses()
            .that().resideInAPackage("..reporting..")
            .should().dependOnClassesThat().resideInAnyPackage("..academicevaluation..",
                    "..governance..", "..improvement..", "..archive..");

    @ArchTest
    static final ArchRule DOMAIN_IS_INDEPENDENT = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..");

    @ArchTest
    static final ArchRule APPLICATION_IGNORES_API_AND_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..");

    @ArchTest
    static final ArchRule MVP_DOES_NOT_DEPEND_ON_INTEGRATION = noClasses()
            .that().resideOutsideOfPackage("..integration..")
            .should().dependOnClassesThat().resideInAPackage("..integration..");

    @ArchTest
    static final ArchRule OTHER_MODULES_DO_NOT_IMPORT_IDENTITY_INFRASTRUCTURE = noClasses()
            .that().resideOutsideOfPackage("..identityaccess..")
            .should().dependOnClassesThat().resideInAPackage("..identityaccess.infrastructure..");

    @ArchTest
    static final ArchRule EVALUATION_MODEL_USES_ONLY_PUBLIC_CROSS_MODULE_PORTS = noClasses()
            .that().resideInAPackage("..evaluationmodel..")
            .should().dependOnClassesThat().resideInAnyPackage("..identityaccess.infrastructure..", "..audit.infrastructure..");

    @ArchTest
    static final ArchRule SELF_ASSESSMENT_USES_ONLY_PUBLIC_CROSS_MODULE_PORTS = noClasses()
            .that().resideInAPackage("..selfassessment..")
            .should().dependOnClassesThat().resideInAnyPackage("..identityaccess.infrastructure..", "..evaluationmodel.infrastructure..", "..audit.infrastructure..");

    @ArchTest
    static final ArchRule EVIDENCE_AND_STORAGE_DOMAINS_IGNORE_ADAPTERS = noClasses()
            .that().resideInAnyPackage("..evidence.domain..", "..filestorage.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..");

    @ArchTest
    static final ArchRule EVIDENCE_DOES_NOT_DEPEND_ON_LATER_MVP_MODULES = noClasses()
            .that().resideInAPackage("..evidence..")
            .should().dependOnClassesThat().resideInAnyPackage("..documentreview..", "..academicevaluation..", "..reporting..", "..integration..");
}
