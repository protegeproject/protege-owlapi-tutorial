package edu.stanford.protege.tutorial;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;

import java.util.stream.Collectors;

/**
 * Lesson 7 -- reasoning: asking what follows.
 *
 * <p>Up to now everything has been <em>asserted</em>. A reasoner computes what is
 * <em>entailed</em>. The distinction is the heart of OWL, and it is what the
 * yellow-highlighted inferred hierarchy in Protege shows you.
 *
 * <p>Two rules to internalise:
 * <ul>
 *   <li>Ask the reasoner, not the ontology, for inferred facts.
 *   <li>OWL is open-world: "not stated" never means "false".
 * </ul>
 */
public class Lesson07_Reasoning {

    public static void main(String[] args) throws OWLOntologyCreationException {
        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory f = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        OWLClass pizza = f.getOWLClass(Tutorial.iri("Pizza"));
        OWLClass vegetarianPizza = f.getOWLClass(Tutorial.iri("VegetarianPizza"));
        OWLClass margherita = f.getOWLClass(Tutorial.iri("Margherita"));
        OWLClass topping = f.getOWLClass(Tutorial.iri("Topping"));
        OWLClass cheeseTopping = f.getOWLClass(Tutorial.iri("CheeseTopping"));
        OWLClass meatTopping = f.getOWLClass(Tutorial.iri("MeatTopping"));
        OWLObjectProperty hasTopping = f.getOWLObjectProperty(Tutorial.iri("hasTopping"));

        // --- Build a small ontology that actually entails something. ---

        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(cheeseTopping, topping));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(meatTopping, topping));

        // Cheese and meat toppings are disjoint -- nothing is both.
        manager.addAxiom(ontology, f.getOWLDisjointClassesAxiom(cheeseTopping, meatTopping));

        // Margherita: a Pizza whose toppings are only CheeseTopping.
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza));
        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita,
                f.getOWLObjectAllValuesFrom(hasTopping, cheeseTopping)));

        // VegetarianPizza DEFINED as: Pizza and (hasTopping only (not MeatTopping)).
        // Equivalence (not subclass) is what lets the reasoner conclude
        // membership rather than merely check it.
        OWLClassExpression vegDefinition = f.getOWLObjectIntersectionOf(
                pizza,
                f.getOWLObjectAllValuesFrom(hasTopping, f.getOWLObjectComplementOf(meatTopping)));
        manager.addAxiom(ontology, f.getOWLEquivalentClassesAxiom(vegetarianPizza, vegDefinition));

        Tutorial.banner("Creating a reasoner");

        // The OWLReasonerFactory abstraction means you can swap HermiT for ELK,
        // Pellet, JFact, ... without changing the querying code below.
        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        // Precomputing is optional but makes the first query's cost explicit
        // rather than hidden inside it.
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        // Two real-world potholes in the reasoner-metadata methods, worth knowing
        // because they are pure incidental breakage:
        //
        //  * getReasonerName() may return null -- HermiT 1.3.8 does.
        //  * getReasonerVersion() can THROW. HermiT's build id is "20151128-2235",
        //    which it tries to parse as an int and fails with
        //    NumberFormatException. Never call it unguarded in production code.
        System.out.println("Reasoner:   " + reasoner.getClass().getSimpleName()
                + " (getReasonerName() -> " + reasoner.getReasonerName() + ")");
        System.out.println("Version:    " + describeVersion(reasoner));
        System.out.println("Consistent? " + reasoner.isConsistent());

        Tutorial.banner("The inference: Margherita is a VegetarianPizza");

        // We never asserted this. It follows because a Margherita's toppings are
        // only CheeseTopping, cheese is disjoint from meat, so it cannot have a
        // meat topping -- satisfying the VegetarianPizza definition.
        System.out.println("Asserted in ontology? "
                + ontology.containsAxiom(f.getOWLSubClassOfAxiom(margherita, vegetarianPizza)));
        System.out.println("Entailed by reasoner? "
                + reasoner.isEntailed(f.getOWLSubClassOfAxiom(margherita, vegetarianPizza)));

        Tutorial.banner("Asserted vs inferred superclasses");

        // The ontology only knows what you told it...
        System.out.println("Asserted superclasses of Margherita:");
        EntitySearcher.getSuperClasses(margherita, ontology)
                .forEach(c -> System.out.println("  " + ManchesterPrinter.render(c)));

        // ...the reasoner knows the consequences. `true` = direct only, i.e. skip
        // ancestors reachable through another named superclass. This is exactly
        // the flag that decides whether you get a tree or a tangle.
        System.out.println("Inferred DIRECT superclasses of Margherita:");
        reasoner.getSuperClasses(margherita, true).getFlattened()
                .forEach(c -> System.out.println("  " + ManchesterPrinter.render(c)));

        System.out.println("Inferred ALL superclasses of Margherita:");
        reasoner.getSuperClasses(margherita, false).getFlattened()
                .forEach(c -> System.out.println("  " + ManchesterPrinter.render(c)));

        Tutorial.banner("NodeSet and Node: why the return types look odd");

        // Reasoners return NodeSet<...>, where each Node is a set of EQUIVALENT
        // classes. Two classes proven equivalent are indistinguishable to a
        // reasoner, so they share one node. getFlattened() discards that
        // grouping when you do not care about it.
        System.out.println("Subclasses of Pizza, grouped into equivalence nodes:");
        reasoner.getSubClasses(pizza, false).forEach(node ->
                System.out.println("  node " + node.getEntities().stream()
                        .map(ManchesterPrinter::render)
                        .sorted()
                        .collect(Collectors.joining(" = "))));

        Tutorial.banner("Classifying an individual");

        OWLNamedIndividual myLunch = f.getOWLNamedIndividual(Tutorial.iri("myLunch"));
        manager.addAxiom(ontology, f.getOWLClassAssertionAxiom(margherita, myLunch));

        // The ontology changed, so the reasoner must be told. HermiT is a
        // buffering reasoner by default: it works from a snapshot until you
        // flush. Forgetting to flush is a classic "why is my inference stale?"
        reasoner.flush();

        System.out.println("Types of myLunch (direct):");
        reasoner.getTypes(myLunch, true).getFlattened()
                .forEach(c -> System.out.println("  " + ManchesterPrinter.render(c)));

        System.out.println("Is myLunch a VegetarianPizza? "
                + reasoner.isEntailed(f.getOWLClassAssertionAxiom(vegetarianPizza, myLunch)));

        Tutorial.banner("Inconsistency and unsatisfiability");

        // An UNSATISFIABLE class cannot have instances; the reasoner reports it
        // as equivalent to owl:Nothing. Protege shows these in red.
        OWLClass impossible = f.getOWLClass(Tutorial.iri("ImpossibleTopping"));
        manager.addAxiom(ontology,
                f.getOWLSubClassOfAxiom(impossible, cheeseTopping));
        manager.addAxiom(ontology,
                f.getOWLSubClassOfAxiom(impossible, meatTopping));
        reasoner.flush();

        System.out.println("Still consistent? " + reasoner.isConsistent()
                + "  (an unsatisfiable CLASS does not make the ONTOLOGY inconsistent)");
        System.out.println("Unsatisfiable classes:");
        reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom()
                .forEach(c -> System.out.println("  " + ManchesterPrinter.render(c)));

        // Now make the ontology itself inconsistent, by asserting that something
        // actually IS an instance of the impossible class.
        manager.addAxiom(ontology, f.getOWLClassAssertionAxiom(
                impossible, f.getOWLNamedIndividual(Tutorial.iri("weirdTopping"))));
        reasoner.flush();

        System.out.println("After asserting an instance of it, consistent? "
                + reasoner.isConsistent());
        System.out.println("(An inconsistent ontology entails EVERYTHING, so most");
        System.out.println(" reasoner queries stop being meaningful -- always check");
        System.out.println(" isConsistent() before trusting other results.)");

        // Reasoners can hold significant memory and native resources.
        reasoner.dispose();
    }

    /**
     * Reads a reasoner's version defensively.
     *
     * <p>{@code getReasonerVersion()} is specified to return a
     * {@code Version}, but HermiT 1.3.8 builds one from the string
     * {@code "20151128-2235"} and blows up with {@link NumberFormatException}.
     * Any code that surfaces reasoner metadata to a user should tolerate that
     * rather than propagate it.
     */
    private static String describeVersion(OWLReasoner reasoner) {
        try {
            return String.valueOf(reasoner.getReasonerVersion());
        } catch (RuntimeException e) {
            return "(unavailable: " + e.getClass().getSimpleName() + ")";
        }
    }
}
