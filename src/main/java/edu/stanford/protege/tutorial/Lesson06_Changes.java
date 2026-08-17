package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lesson 6 -- changes, edits, and undo.
 *
 * <p>This is the lesson that matters most for Protege plugin work. Protege never
 * mutates an ontology directly; it builds a list of {@link OWLOntologyChange}
 * objects and applies them. That indirection is what gives Protege undo/redo,
 * dirty-tracking, and change listeners.
 *
 * <p>{@code manager.addAxiom(o, ax)} is just a convenience wrapper around
 * {@code manager.applyChange(new AddAxiom(o, ax))}. Once you need undo or
 * batching, work with the change objects directly.
 */
public class Lesson06_Changes {

    public static void main(String[] args) throws OWLOntologyCreationException {
        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory f = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        OWLClass pizza = f.getOWLClass(Tutorial.iri("Pizza"));
        OWLClass margherita = f.getOWLClass(Tutorial.iri("Margherita"));
        OWLClass calzone = f.getOWLClass(Tutorial.iri("Calzone"));

        Tutorial.banner("Listening for changes");

        // A change listener sees every applied change. Protege uses exactly this
        // hook to refresh its views when the ontology is edited.
        manager.addOntologyChangeListener(changes -> {
            for (OWLOntologyChange change : changes) {
                System.out.println("  [listener] "
                        + (change.isAddAxiom() ? "ADD    " : "REMOVE ")
                        + ManchesterPrinter.render(change.getAxiom()));
            }
        });

        Tutorial.banner("Applying an explicit change list (one batch)");

        // Batching matters: applying a list once fires the listener once with
        // all the changes, instead of once per axiom. For large edits this is
        // the difference between a responsive UI and a frozen one.
        List<OWLOntologyChange> changes = new ArrayList<>();
        changes.add(new AddAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza)));
        changes.add(new AddAxiom(ontology, f.getOWLSubClassOfAxiom(calzone, pizza)));
        changes.add(new AddAxiom(ontology, f.getOWLDeclarationAxiom(pizza)));

        manager.applyChanges(changes);
        System.out.println("Axioms now: " + ontology.getAxiomCount());

        Tutorial.banner("Removing an axiom");

        OWLAxiom calzoneIsPizza = f.getOWLSubClassOfAxiom(calzone, pizza);
        manager.applyChange(new RemoveAxiom(ontology, calzoneIsPizza));
        System.out.println("Axioms now: " + ontology.getAxiomCount());

        Tutorial.banner("Hand-rolled undo");

        // There is no built-in undo in the OWL API itself -- Protege implements
        // its own on top of the change list. The principle is simple: every
        // change knows how to describe itself, so you can invert it.
        List<OWLOntologyChange> applied = Collections.singletonList(
                new AddAxiom(ontology, f.getOWLSubClassOfAxiom(
                        f.getOWLClass(Tutorial.iri("Marinara")), pizza)));

        manager.applyChanges(applied);
        System.out.println("After edit:  " + ontology.getAxiomCount() + " axioms");

        manager.applyChanges(invert(applied));
        System.out.println("After undo:  " + ontology.getAxiomCount() + " axioms");

        Tutorial.banner("Renaming an entity (refactoring)");

        // Renaming is NOT a rename: an entity is its IRI, so "renaming" means
        // rewriting every axiom that mentions the old IRI. OWLEntityRenamer
        // produces the change list to do that.
        manager.addAxiom(ontology, f.getOWLAnnotationAssertionAxiom(
                f.getRDFSLabel(), margherita.getIRI(), f.getOWLLiteral("Margherita", "en")));

        OWLEntityRenamer renamer =
                new OWLEntityRenamer(manager, Collections.singleton(ontology));

        IRI newIri = Tutorial.iri("MargheritaPizza");
        System.out.println("Renaming Margherita -> MargheritaPizza");
        manager.applyChanges(renamer.changeIRI(margherita.getIRI(), newIri));

        System.out.println("Old IRI still present? "
                + ontology.containsClassInSignature(margherita.getIRI()));
        System.out.println("New IRI present?       "
                + ontology.containsClassInSignature(newIri));

        Tutorial.banner("Deleting an entity everywhere");

        // Likewise, "delete a class" means "remove every axiom referencing it".
        // OWLEntityRemover collects those removals for you.
        OWLEntityRemover remover =
                new OWLEntityRemover(Collections.singleton(ontology));

        // The visitor pattern: the remover is an OWLEntityVisitor, so you accept
        // it on each entity you want gone.
        OWLEntity doomed = f.getOWLClass(newIri);
        doomed.accept(remover);
        manager.applyChanges(remover.getChanges());

        System.out.println("MargheritaPizza gone? "
                + !ontology.containsClassInSignature(newIri));

        Tutorial.banner("Final state");
        ontology.getAxioms(Imports.EXCLUDED).stream()
                .map(ManchesterPrinter::render)
                .sorted()
                .forEach(a -> System.out.println("  " + a));
    }

    /**
     * Inverts a change list to undo it: adds become removes and vice versa, in
     * reverse order. This is the core of how an undo stack works.
     */
    private static List<OWLOntologyChange> invert(List<OWLOntologyChange> changes) {
        List<OWLOntologyChange> undo = new ArrayList<>();
        for (int i = changes.size() - 1; i >= 0; i--) {
            OWLOntologyChange change = changes.get(i);
            if (change.isAddAxiom()) {
                undo.add(new RemoveAxiom(change.getOntology(), change.getAxiom()));
            } else if (change.isRemoveAxiom()) {
                undo.add(new AddAxiom(change.getOntology(), change.getAxiom()));
            }
        }
        return undo;
    }
}
