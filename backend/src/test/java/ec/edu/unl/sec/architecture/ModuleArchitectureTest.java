package ec.edu.unl.sec.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "ec.edu.unl.sec")
class ModuleArchitectureTest {
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
}
