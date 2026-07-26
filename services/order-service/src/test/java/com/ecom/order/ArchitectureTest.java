package com.ecom.order;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Garante a regra de dependencia da Clean/Hexagonal sem precisar de modulos Maven
 * separados. O isolamento vem do ArchUnit.
 *
 * <p>Regras escritas como {@code @Test} do JUnit (com {@code .check()} explicito) em
 * vez de {@code @ArchTest}: o engine do archunit-junit5 nao e executado de forma
 * confiavel pelo surefire neste ambiente (rodava 0 regras sem falhar).
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.ecom.order");
    }

    @Test
    void dominio_nao_depende_de_camadas_externas_nem_frameworks() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..application..", "..infrastructure..", "..api..",
                        "org.springframework..", "jakarta.persistence..", "lombok..")
                .check(classes);
    }

    @Test
    void aplicacao_nao_depende_de_infra_nem_api() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..api..")
                .check(classes);
    }

    @Test
    void dominio_nao_usa_jpa() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("jakarta.persistence..")
                .check(classes);
    }

    @Test
    void api_nao_depende_de_infraestrutura() {
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void infraestrutura_nao_depende_da_api() {
        noClasses().that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAPackage("..api..")
                .check(classes);
    }

    @Test
    void camadas_nao_possuem_ciclos() {
        slices().matching("com.ecom.order.(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }
}
