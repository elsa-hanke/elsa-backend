package fi.elsapalvelu.elsa

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchTest {

    @Test
    fun servicesAndRepositoriesShouldNotDependOnWebLayer() {

        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("fi.elsapalvelu.elsa")

        noClasses()
            .that()
            .resideInAnyPackage("fi.elsapalvelu.elsa.service..")
            .or()
            .resideInAnyPackage("fi.elsapalvelu.elsa.repository..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..fi.elsapalvelu.elsa.web..")
            .because("Services and repositories should not depend on web layer")
            .check(importedClasses)
    }
}
