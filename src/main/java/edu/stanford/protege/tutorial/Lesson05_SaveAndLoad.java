package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lesson 5 -- serialising and parsing: formats, files, and round-trips.
 *
 * <p>An important separation of concerns: the <b>axioms</b> are the ontology;
 * the <b>format</b> is only how you write them down. The same ontology can be
 * saved as RDF/XML, Turtle, OWL/XML, Functional or Manchester syntax without
 * changing its logical content.
 *
 * <p>This is exactly what Protege's "Save as..." does.
 */
public class Lesson05_SaveAndLoad {

    public static void main(String[] args)
            throws OWLOntologyCreationException, OWLOntologyStorageException, java.io.IOException {
        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory f = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        // A tiny ontology worth looking at in several syntaxes.
        OWLClass pizza = f.getOWLClass(Tutorial.iri("Pizza"));
        OWLClass margherita = f.getOWLClass(Tutorial.iri("Margherita"));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza));
        manager.addAxiom(ontology, f.getOWLAnnotationAssertionAxiom(
                f.getRDFSLabel(), pizza.getIRI(), f.getOWLLiteral("Pizza", "en")));

        Tutorial.banner("The same ontology in five formats");

        printAs(manager, ontology, new FunctionalSyntaxDocumentFormat(), "Functional syntax");
        printAs(manager, ontology, new ManchesterSyntaxDocumentFormat(), "Manchester syntax");
        printAs(manager, ontology, new TurtleDocumentFormat(), "Turtle");
        printAs(manager, ontology, new RDFXMLDocumentFormat(), "RDF/XML");
        printAs(manager, ontology, new OWLXMLDocumentFormat(), "OWL/XML");

        Tutorial.banner("Saving to a file");

        Path outputDir = Path.of("target", "tutorial-output");
        Files.createDirectories(outputDir);
        File file = outputDir.resolve("food.ttl").toFile();

        // saveOntology(ontology, format, IRI) -- the IRI here is the DOCUMENT
        // IRI (where to write), not the ontology IRI (its name).
        manager.saveOntology(ontology, new TurtleDocumentFormat(), IRI.create(file));
        System.out.println("Wrote " + file.getPath() + " (" + file.length() + " bytes)");

        Tutorial.banner("Loading it back");

        // A FRESH manager. A single manager cannot hold two ontologies with the
        // same ontology IRI -- reloading into `manager` would throw
        // OWLOntologyAlreadyExistsException. This is a very common stumble.
        OWLOntologyManager loader = OWLManager.createOWLOntologyManager();
        OWLOntology reloaded = loader.loadOntologyFromOntologyDocument(file);

        System.out.println("Reloaded axioms: " + reloaded.getAxiomCount());
        System.out.println("Logically same as original? "
                + reloaded.getLogicalAxioms().equals(ontology.getLogicalAxioms()));

        // The format the ontology was actually parsed from is available, which
        // is how Protege remembers to save a file back in its original syntax.
        OWLDocumentFormat detected = loader.getOntologyFormat(reloaded);
        System.out.println("Detected format: " + detected);

        Tutorial.banner("Parsing from a string");

        // Useful for tests and for pasted snippets. Note Manchester/Functional
        // syntax needs the prefix declarations to resolve names.
        String turtle = ""
                + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix : <" + Tutorial.PREFIX + "> .\n"
                + "<" + Tutorial.ONTOLOGY_IRI + "> a owl:Ontology .\n"
                + ":Calzone a owl:Class ; rdfs:subClassOf :Pizza .\n";

        OWLOntology fromString = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new StringDocumentSource(turtle));

        System.out.println("Parsed from string, axioms = " + fromString.getAxiomCount());
        fromString.getLogicalAxioms()
                .forEach(a -> System.out.println("  " + ManchesterPrinter.render(a)));

        Tutorial.banner("Note: rdf:type prefix typo tolerance");
        System.out.println("Parsers are strict about syntax but lenient about");
        System.out.println("unknown vocabulary -- unrecognised triples usually become");
        System.out.println("annotations rather than errors. Check your axiom count!");
    }

    /**
     * Serialises to an in-memory string rather than a file, using a
     * {@link StringDocumentTarget}.
     */
    private static void printAs(OWLOntologyManager manager,
                                OWLOntology ontology,
                                OWLDocumentFormat format,
                                String title) throws OWLOntologyStorageException {
        StringDocumentTarget target = new StringDocumentTarget();
        manager.saveOntology(ontology, format, target);

        System.out.println();
        System.out.println("--- " + title + " ---");
        System.out.println(target.toString().trim());
    }
}
