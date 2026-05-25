package com.ontology.platform.ontology.service;

import com.ontology.platform.ontology.entity.CapabilityIndicator;
import com.ontology.platform.ontology.entity.CapabilityRelationship;
import lombok.extern.slf4j.Slf4j;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OwlParserService {

    /**
     * Parse an OWL file and extract capability indicators and their relationships.
     */
    public OwlParseResult parseOwlFile(String owlFilePath) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(
                new java.io.File(owlFilePath));

        List<CapabilityIndicator> indicators = new ArrayList<>();
        List<CapabilityRelationship> relationships = new ArrayList<>();

        // Extract classes as indicators
        for (OWLClass owlClass : ontology.getClassesInSignature()) {
            if (owlClass.isOWLNothing() || owlClass.isOWLThing()) continue;

            CapabilityIndicator indicator = new CapabilityIndicator();
            indicator.setId(UUID.randomUUID().toString());
            indicator.setIri(owlClass.getIRI().toString());
            indicator.setName(extractLabel(owlClass, ontology));

            // Extract annotations
            List<OWLAnnotation> annotations =
                    EntitySearcher.getAnnotations(owlClass, ontology).toList();
            for (OWLAnnotation annotation : annotations) {
                String value = annotation.getValue().toString();
                String propName = annotation.getProperty().getIRI().getShortForm();
                switch (propName) {
                    case "domain" -> indicator.setDomain(value);
                    case "category" -> indicator.setCategory(value);
                    case "description" -> indicator.setDescription(value);
                    case "unit" -> indicator.setUnit(value);
                }
            }

            indicators.add(indicator);
        }

        // Extract object properties as relationships
        for (OWLObjectProperty property : ontology.getObjectPropertiesInSignature()) {
            String propName = property.getIRI().getShortForm();

            for (OWLObjectPropertyAssertionAxiom axiom :
                    ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                if (!axiom.getProperty().equals(property)) continue;

                CapabilityRelationship rel = new CapabilityRelationship();
                rel.setId(UUID.randomUUID().toString());
                rel.setSourceIndicatorId(axiom.getSubject().toString());
                rel.setTargetIndicatorId(axiom.getObject().toString());
                rel.setRelationshipType(propName);
                rel.setEnabled(true);
                relationships.add(rel);
            }
        }

        log.info("Parsed {} indicators and {} relationships from OWL file",
                indicators.size(), relationships.size());

        return new OwlParseResult(indicators, relationships);
    }

    private String extractLabel(OWLClass owlClass, OWLOntology ontology) {
        String iri = owlClass.getIRI().toString();
        String fragment = owlClass.getIRI().getShortForm();

        // Try rdfs:label annotation
        List<OWLAnnotation> labelAnnotations =
                EntitySearcher.getAnnotations(owlClass, ontology).toList();
        for (OWLAnnotation annotation : labelAnnotations) {
            if (annotation.getProperty().isLabel()) {
                return annotation.getValue().toString();
            }
        }

        return fragment != null ? fragment : iri;
    }

    public record OwlParseResult(List<CapabilityIndicator> indicators,
                                  List<CapabilityRelationship> relationships) {}
}
