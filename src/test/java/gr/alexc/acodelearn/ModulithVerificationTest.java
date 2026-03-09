package gr.alexc.acodelearn;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithVerificationTest {

    ApplicationModules modules = ApplicationModules.of(AcodeLearnBackendApplication.class);

    @Test
    void verifyModulith() {
        modules.verify();
    }

    @Test
    void writeModuleDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
