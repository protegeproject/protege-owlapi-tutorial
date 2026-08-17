package edu.stanford.protege.tutorial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that double as executable documentation: each one pins down a claim the
 * lessons make. If the OWL API version changes and a behaviour shifts, these
 * fail rather than the tutorial quietly becoming wrong.
 *
 * <p>They are also a template for how to test ontology code -- build in memory,
 * assert on axioms, and use a string round-trip instead of temp files.
 */
class OwlApiBasicsTest {

    private OWLOntologyManager manager;
    private OWLDataFactory f;
    private OWLOntology ontology;

    private OWLClass pizza;
    private OWLClass margherita;
    private OWLClass cheeseTopping;
    private OWLClass meatTopping;
    private OWLObjectProperty hasTopping;

    @BeforeEach
    void setUp() throws OWLOntologyCreationException {
        manager = OWLManager.createOWLOntologyManager();
        f = manager.getOWLDataFactory();
        ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        pizza = f.getOWLClass(Tutorial.iri("Pizza"));
        margherita = f.getOWLClass(Tutorial.iri("Margherita"));
        cheeseTopping = f.getOWLClass(Tutorial.iri("CheeseTopping"));
        meatTopping = f.getOWLClass(Tutorial.iri("MeatTopping"));
        hasTopping = f.getOWLObjectProperty(Tutorial.iri("hasTopping"));
    }

    @Test
    @DisplayName("Building an entity does not add anything to an ontology")
    void entitiesAreInert() {
        f.getOWLClass(Tutorial.iri("Anything"));
        assertEquals(0, ontology.getAxiomCount());
    }

    @Test
    @DisplayName("Entities with the same IRI are equal")
    void entitiesAreValueObjects() {
        assertEquals(f.getOWLClass(Tutorial.iri("Pizza")), pizza);
        assertNotEquals(f.getOWLClass(Tutorial.iri("Calzone")), pizza);
    }

    @Test
    @DisplayName("Using an entity in an axiom puts it in the signature without a declaration")
    void axiomsIntroduceEntities() {
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza));

        assertTrue(ontology.getClassesInSignature().contains(margherita));
        // ...but there is still no declaration axiom for it.
        assertFalse(ontology.containsAxiom(f.getOWLDeclarationAxiom(margherita)));
    }

    @Test
    @DisplayName("An annotated axiom is not equal to the same axiom without annotations")
    void axiomAnnotationsAffectEquality() {
        OWLSubClassOfAxiom plain = f.getOWLSubClassOfAxiom(margherita, pizza);
        OWLSubClassOfAxiom annotated = f.getOWLSubClassOfAxiom(margherita, pizza,
                Collections.singleton(
                        f.getOWLAnnotation(f.getRDFSComment(), f.getOWLLiteral("note"))));

        assertNotEquals(plain, annotated);
        assertEquals(plain, annotated.getAxiomWithoutAnnotations());

        manager.addAxiom(ontology, annotated);
        assertFalse(ontology.containsAxiom(plain));
        assertTrue(ontology.containsAxiom(plain, Imports.EXCLUDED,
                AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));
    }

    @Test
    @DisplayName("An ontology survives a Turtle round-trip unchanged")
    void roundTripPreservesLogicalAxioms()
            throws OWLOntologyStorageException, OWLOntologyCreationException {
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(pizza,
                f.getOWLObjectSomeValuesFrom(hasTopping, cheeseTopping)));

        StringDocumentTarget target = new StringDocumentTarget();
        manager.saveOntology(ontology, new TurtleDocumentFormat(), target);

        // A fresh manager: one manager cannot hold two ontologies with the same IRI.
        OWLOntology reloaded = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(
                        new StringDocumentSource(target.toString()));

        assertEquals(ontology.getLogicalAxioms(), reloaded.getLogicalAxioms());
    }

    @Test
    @DisplayName("Imports.INCLUDED sees imported axioms; EXCLUDED does not")
    void importsFlagChangesVisibility() throws OWLOntologyCreationException {
        IRI coreIri = IRI.create(Tutorial.relatedOntologyIri("core"));
        OWLOntology core = manager.createOntology(coreIri);
        manager.addAxiom(core, f.getOWLSubClassOfAxiom(cheeseTopping,
                f.getOWLClass(Tutorial.iri("Topping"))));

        manager.applyChange(new AddImport(
                ontology, f.getOWLImportsDeclaration(coreIri)));

        assertFalse(ontology.containsClassInSignature(
                cheeseTopping.getIRI(), Imports.EXCLUDED));
        assertTrue(ontology.containsClassInSignature(
                cheeseTopping.getIRI(), Imports.INCLUDED));
    }

    @Test
    @DisplayName("The reasoner entails Margherita is a VegetarianPizza; the ontology does not assert it")
    void reasonerFindsEntailmentThatWasNeverAsserted() {
        OWLClass vegetarianPizza = f.getOWLClass(Tutorial.iri("VegetarianPizza"));

        manager.addAxiom(ontology, f.getOWLDisjointClassesAxiom(cheeseTopping, meatTopping));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita,
                f.getOWLObjectAllValuesFrom(hasTopping, cheeseTopping)));

        OWLClassExpression vegDefinition = f.getOWLObjectIntersectionOf(
                pizza,
                f.getOWLObjectAllValuesFrom(hasTopping,
                        f.getOWLObjectComplementOf(meatTopping)));
        manager.addAxiom(ontology,
                f.getOWLEquivalentClassesAxiom(vegetarianPizza, vegDefinition));

        OWLSubClassOfAxiom conclusion =
                f.getOWLSubClassOfAxiom(margherita, vegetarianPizza);

        // Asserted? No. Entailed? Yes. That gap is the whole point of OWL.
        assertFalse(ontology.containsAxiom(conclusion));

        OWLReasoner reasoner = new ReasonerFactory().createReasoner(ontology);
        try {
            assertTrue(reasoner.isConsistent());
            assertTrue(reasoner.isEntailed(conclusion));
            assertTrue(reasoner.getSuperClasses(margherita, true)
                    .getFlattened().contains(vegetarianPizza));
        } finally {
            reasoner.dispose();
        }
    }

    @Test
    @DisplayName("A reasoner must be flushed after the ontology changes")
    void reasonerNeedsFlushAfterEdits() {
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza));

        OWLReasoner reasoner = new ReasonerFactory().createReasoner(ontology);
        try {
            OWLNamedIndividual lunch = f.getOWLNamedIndividual(Tutorial.iri("lunch"));
            manager.addAxiom(ontology, f.getOWLClassAssertionAxiom(margherita, lunch));

            // HermiT buffers by default, so the new individual is not visible yet.
            assertFalse(reasoner.getInstances(pizza, false).getFlattened().contains(lunch));

            reasoner.flush();
            assertTrue(reasoner.getInstances(pizza, false).getFlattened().contains(lunch));
        } finally {
            reasoner.dispose();
        }
    }

    @Test
    @DisplayName("An unsatisfiable class does not make the ontology inconsistent")
    void unsatisfiableIsNotInconsistent() {
        OWLClass impossible = f.getOWLClass(Tutorial.iri("Impossible"));
        manager.addAxiom(ontology, f.getOWLDisjointClassesAxiom(cheeseTopping, meatTopping));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(impossible, cheeseTopping));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(impossible, meatTopping));

        OWLReasoner reasoner = new ReasonerFactory().createReasoner(ontology);
        try {
            assertTrue(reasoner.isConsistent());
            assertTrue(reasoner.getUnsatisfiableClasses()
                    .getEntitiesMinusBottom().contains(impossible));

            // Give it an instance and NOW the ontology itself breaks.
            manager.addAxiom(ontology, f.getOWLClassAssertionAxiom(
                    impossible, f.getOWLNamedIndividual(Tutorial.iri("boom"))));
            reasoner.flush();

            assertFalse(reasoner.isConsistent());
        } finally {
            reasoner.dispose();
        }
    }
}
