package fi.oulu.elsa

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchTest {

    @Test
    fun servicesAndRepositoriesShouldNotDependOnWebLayer() {

        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("fi.oulu.elsa")

        noClasses()
            .that()
            .resideInAnyPackage("fi.oulu.elsa.service..")
            .or()
            .resideInAnyPackage("fi.oulu.elsa.repository..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..fi.oulu.elsa.web..")
            .because("Services and repositories should not depend on web layer")
            .check(importedClasses)
    }
}
