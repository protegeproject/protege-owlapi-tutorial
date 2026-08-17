package edu.stanford.protege.tutorial;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Lesson 9 -- materialising inferences into a real ontology.
 *
 * <p>A reasoner answers questions but does not change your ontology. Sometimes
 * you want the entailments written down as actual axioms -- to publish a
 * "classified" version, or to hand data to a tool that cannot reason. That is
 * what Protege's <i>Refactor &gt; Export inferred axioms</i> does, and it uses
 * exactly the {@link InferredOntologyGenerator} shown here.
 *
 * <p>Caveat worth stating up front: materialising is lossy in the sense that the
 * result no longer distinguishes what you asserted from what was derived.
 */
public class Lesson09_InferredAxioms {

    public static void main(String[] args)
            throws OWLOntologyCreationException, OWLOntologyStorageException {
        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory f = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        OWLClass pizza = f.getOWLClass(Tutorial.iri("Pizza"));
        OWLClass vegetarianPizza = f.getOWLClass(Tutorial.iri("VegetarianPizza"));
        OWLClass margherita = f.getOWLClass(Tutorial.iri("Margherita"));
        OWLClass cheeseTopping = f.getOWLClass(Tutorial.iri("CheeseTopping"));
        OWLClass meatTopping = f.getOWLClass(Tutorial.iri("MeatTopping"));
        OWLObjectProperty hasTopping = f.getOWLObjectProperty(Tutorial.iri("hasTopping"));

        manager.addAxiom(ontology, f.getOWLDisjointClassesAxiom(cheeseTopping, meatTopping));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita,
                f.getOWLObjectAllValuesFrom(hasTopping, cheeseTopping)));
        manager.addAxiom(ontology, f.getOWLEquivalentClassesAxiom(vegetarianPizza,
                f.getOWLObjectIntersectionOf(pizza,
                        f.getOWLObjectAllValuesFrom(hasTopping,
                                f.getOWLObjectComplementOf(meatTopping)))));
        manager.addAxiom(ontology, f.getOWLClassAssertionAxiom(margherita,
                f.getOWLNamedIndividual(Tutorial.iri("myLunch"))));

        System.out.println("Asserted axiom count: " + ontology.getAxiomCount());

        Tutorial.banner("Generating inferred axioms into a NEW ontology");

        OWLReasoner reasoner = new ReasonerFactory().createReasoner(ontology);

        // You choose which KINDS of inference to materialise. Generating
        // everything is rarely what you want -- the output explodes with trivia
        // like "X SubClassOf owl:Thing".
        List<InferredAxiomGenerator<? extends OWLAxiom>> generators = new ArrayList<>();
        generators.add(new InferredSubClassAxiomGenerator());
        generators.add(new InferredEquivalentClassAxiomGenerator());
        generators.add(new InferredClassAssertionAxiomGenerator());

        InferredOntologyGenerator inferredGenerator =
                new InferredOntologyGenerator(reasoner, generators);

        // Target a separate ontology so asserted and inferred stay distinguishable.
        OWLOntology inferred = manager.createOntology(IRI.create(Tutorial.relatedOntologyIri("inferred")));
        inferredGenerator.fillOntology(f, inferred);

        System.out.println("Inferred axiom count: " + inferred.getAxiomCount());
        System.out.println("Inferred axioms:");
        inferred.getAxioms().stream()
                .map(ManchesterPrinter::render)
                .sorted()
                .forEach(a -> System.out.println("  " + a));

        Tutorial.banner("Which of those were NOT already asserted?");

        // The genuinely new knowledge: set difference against the original.
        inferred.getLogicalAxioms().stream()
                .filter(a -> !ontology.containsAxiom(a))
                .map(ManchesterPrinter::render)
                .sorted()
                .forEach(a -> System.out.println("  NEW: " + a));

        Tutorial.banner("The materialised ontology, serialised");

        StringDocumentTarget target = new StringDocumentTarget();
        manager.saveOntology(inferred, new FunctionalSyntaxDocumentFormat(), target);
        System.out.println(target.toString().trim());

        reasoner.dispose();

        Tutorial.banner("Caveats");
        System.out.println("1. owl:Thing/owl:Nothing axioms are usually noise -- filter them.");
        System.out.println("2. Materialised axioms lose their provenance: you can no");
        System.out.println("   longer tell asserted from derived. Keep them in a");
        System.out.println("   separate ontology (as here) if that matters.");
        System.out.println("3. Re-running a reasoner over materialised output can");
        System.out.println("   produce yet more axioms; it is not a fixpoint.");
    }
}
