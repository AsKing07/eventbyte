package fr.soft_labs.eventbyte;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garde-fous de l'architecture hexagonale (US-1, US-19).
 * Tournent sans Docker ni base : à lancer avant chaque PR.
 */
@AnalyzeClasses(packages = "fr.soft_labs.eventbyte", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] TECHNIQUE = {
            "org.springframework..", "jakarta.persistence..", "org.hibernate..",
            "com.meilisearch..", "org.springframework.amqp..", "com.stripe.."
    };

    @ArchTest
    static final ArchRule le_domaine_ne_depend_d_aucun_framework = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(TECHNIQUE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule les_cas_d_usage_ne_dependent_d_aucun_framework = noClasses()
            .that().resideInAPackage("..usecases..")
            .should().dependOnClassesThat().resideInAnyPackage(TECHNIQUE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule le_domaine_ne_connait_pas_les_couches_externes = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..usecases..", "..ports..", "..adapters..", "..config..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule les_cas_d_usage_ne_connaissent_pas_les_adapters = noClasses()
            .that().resideInAPackage("..usecases..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapters..", "..config..")
            .allowEmptyShould(true);
}
