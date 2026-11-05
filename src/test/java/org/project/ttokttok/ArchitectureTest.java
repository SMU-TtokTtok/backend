package org.project.ttokttok;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * rules/reference/ddd-architecture.md 의 계층 방향을 자동으로 검사한다.
 */
class ArchitectureTest {

    private static final String ROOT = "org.project.ttokttok";
    private static final String CONTROLLER = ROOT + ".domain..controller..";
    private static final String SERVICE = ROOT + ".domain..service..";

    private static final String[] DOMAIN_LAYER = {
            ROOT + ".domain.*.domain..",
            ROOT + ".domain.*.*.domain.."
    };
    private static final String REPOSITORY = ROOT + ".domain..repository..";
    private static final String GLOBAL = ROOT + ".global..";
    private static final String INFRASTRUCTURE = ROOT + ".infrastructure..";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT);
    }

    @Test
    @DisplayName("컨트롤러 계층에는 컨트롤러 밖에서 의존하지 않는다")
    void onlyControllersDependOnControllerLayer() {
        noClasses()
                .that().resideInAnyPackage(SERVICE, REPOSITORY, GLOBAL, INFRASTRUCTURE,
                        DOMAIN_LAYER[0], DOMAIN_LAYER[1])
                .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
                .check(classes);
    }

    @Test
    @DisplayName("도메인, 리포지토리, 공통 모듈은 서비스 계층에 의존하지 않는다")
    void lowerLayersDoNotDependOnServiceLayer() {
        noClasses()
                .that().resideInAnyPackage(REPOSITORY, GLOBAL, INFRASTRUCTURE,
                        DOMAIN_LAYER[0], DOMAIN_LAYER[1])
                .should().dependOnClassesThat().resideInAPackage(SERVICE)
                .check(classes);
    }
}
