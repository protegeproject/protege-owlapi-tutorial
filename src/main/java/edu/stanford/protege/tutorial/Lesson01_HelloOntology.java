package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Lesson 1 -- the three objects you cannot avoid.
 *
 * <p>Every OWL API program starts by getting hold of three things:
 *
 * <ol>
 *   <li>an {@link OWLOntologyManager} -- creates, loads, saves and tracks ontologies;
 *   <li>an {@link OWLOntology} -- a container of axioms;
 *   <li>an {@code OWLDataFactory} -- builds entities and axioms (Lesson 2).
 * </ol>
 *
 * <p>The single most common beginner surprise: an {@code OWLOntology} is
 * <em>not</em> a graph object you mutate with setters. It is a set of axioms,
 * and you change it by asking the <em>manager</em> to apply changes to it.
 */
public class Lesson01_HelloOntology {

    public static void main(String[] args) throws OWLOntologyCreationException {
        Tutorial.quietLogging();

        // OWLManager is the entry point from the owlapi-apibinding module. It
        // wires up a manager with all the standard parsers and renderers
        // registered. **In a Protege plugin you do NOT do this** -- Protege hands
        // you its own manager. See docs/09-protege-plugins.md.
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        Tutorial.banner("An empty ontology");

        // An ontology with an IRI (its name). The manager now "holds" it.
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        // Brand new, so zero axioms. An ontology is legal while empty.
        System.out.println("Axiom count: " + ontology.getAxiomCount());
        System.out.println("Ontology ID: " + ontology.getOntologyID());

        // The manager knows about every ontology it created or loaded. This
        // matters because imports resolution and change-application all go
        // through the manager, not through individual ontologies.
        System.out.println("Manager holds " + manager.getOntologies().size() + " ontology/ies");

        Tutorial.banner("Ontology IRI vs. document IRI");

        // Two different notions that beginners routinely conflate:
        //
        //   ontology IRI  -- the logical NAME of the ontology
        //   document IRI  -- WHERE the bytes actually live (file, URL, ...)
        //
        // A freshly created ontology has no real document yet, so the manager
        // invents a placeholder document IRI for it.
        System.out.println("Ontology IRI: "
                + ontology.getOntologyID().getOntologyIRI().orNull());
        System.out.println("Document IRI: " + manager.getOntologyDocumentIRI(ontology));

        // Note `.orNull()` above: OWL API 4.x uses Guava's Optional, not
        // java.util.Optional. This is one of the most visible 4.x-vs-5.x
        // differences and a frequent source of confusion, because the two types
        // have similar but not identical APIs.
        //   4.x: com.google.common.base.Optional  -> .orNull(), .isPresent(), .get()
        //   5.x: java.util.Optional               -> .orElse(null), ...
        // An ontology IRI is optional because anonymous ontologies are legal.

        Tutorial.banner("Anonymous ontologies");

        OWLOntology anonymous = manager.createOntology();
        System.out.println("Anonymous ontology ID: " + anonymous.getOntologyID());
        System.out.println("Is anonymous? " + anonymous.isAnonymous());

        System.out.println();
        System.out.println("Next: Lesson02_DataFactory -- building entities and axioms.");
    }
}
