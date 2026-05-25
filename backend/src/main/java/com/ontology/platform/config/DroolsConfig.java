package com.ontology.platform.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

@Configuration
public class DroolsConfig {

    @Value("${drools.rules-path:classpath:rules/}")
    private String rulesPath;

    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(rulesPath + "*.drl");

        for (Resource resource : resources) {
            kieFileSystem.write(ResourceFactory.newClassPathResource(
                    "rules/" + resource.getFilename(), "UTF-8"));
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(
                org.kie.api.builder.Message.Level.ERROR)) {
            throw new IllegalStateException("Drools rules compilation failed: "
                    + kieBuilder.getResults().getMessages());
        }

        return kieServices.newKieContainer(kieServices.getRepository()
                .getDefaultReleaseId());
    }

    @Bean
    public KieServices kieServices() {
        return KieServices.Factory.get();
    }
}
