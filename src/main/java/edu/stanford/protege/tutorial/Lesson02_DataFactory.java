package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Lesson 2 -- the data factory, entities, and axioms.
 *
 * <p>{@link OWLDataFactory} is the <em>only</em> sanctioned way to build OWL
 * objects. You never call a constructor on an OWL API model class; you ask the
 * factory. That indirection is what lets the implementation intern/share
 * objects and keep them immutable.
 *
 * <p>The key mental model in this lesson:
 * <b>declaring an entity and asserting something about it are different acts.</b>
 */
public class Lesson02_DataFactory {

    public static void main(String[] args) throws OWLOntologyCreationException {

        // SubClassOf(A B)
        // EquivalentClass(C D)
        // DisjointClasses(X Y Z)

        // OWLClass, OWLObjectProperty  -> OWLEntity
        // OWLObjectSomeValuesFrom OWLObjectAllValuesFroms

        // OWLSubClassOfAxiom
        // OWLDisjointClassesAxiom

        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        Tutorial.banner("Entities are just names");

        // Getting an entity from the factory creates NOTHING in any ontology.
        // An OWLClass is a name (an IRI) with a type. It is inert on its own.
        OWLClass pizza = factory.getOWLClass(Tutorial.iri("Pizza"));
        OWLClass margherita = factory.getOWLClass(Tutorial.iri("Margherita"));
        OWLClass topping = factory.getOWLClass(Tutorial.iri("Topping"));

        // Declaration --- OWLDeclarationAxiom
        // SubClassOf --- OWLSubClassOfAxiom

        // margherita is a class  --- declaration
        // pizza is a class --- declaration
        // all margherita are pizza --- sub class of

        System.out.println("Built 3 class objects.");
        System.out.println("Ontology axiom count is still: " + ontology.getAxiomCount());

        // Entities are value objects: same IRI means equal, and interned.
        OWLClass pizzaAgain = factory.getOWLClass(Tutorial.iri("Pizza"));
        System.out.println("pizza.equals(pizzaAgain): " + pizza.equals(pizzaAgain));

        Tutorial.banner("Declarations put an entity in the signature");

        // A declaration axiom says "this name exists and is a class". Protege
        // shows undeclared-but-used entities too, but declaring is good hygiene
        // and is what makes an entity appear in a fresh ontology's signature.
        manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(pizza));
        manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(topping));
        System.out.println("After 2 declarations, axioms = " + ontology.getAxiomCount());

        Tutorial.banner("Axioms are the real content");

        // SubClassOf(Margherita, Pizza). Note we never declared Margherita:
        // using it in an axiom is enough to bring it into the signature.
        OWLSubClassOfAxiom margheritaIsAPizza = factory.getOWLSubClassOfAxiom(margherita, pizza);
        manager.addAxiom(ontology, margheritaIsAPizza);

        System.out.println("Added: " + margheritaIsAPizza);
        System.out.println("Axioms = " + ontology.getAxiomCount());
        System.out.println("Margherita in signature? "
                + ontology.getClassesInSignature().contains(margherita));

        Tutorial.banner("Properties and individuals");

        OWLObjectProperty hasTopping = factory.getOWLObjectProperty(Tutorial.iri("hasTopping"));
        OWLDataProperty hasCalories = factory.getOWLDataProperty(Tutorial.iri("hasCalories"));
        OWLNamedIndividual myLunch = factory.getOWLNamedIndividual(Tutorial.iri("myLunch"));

        // ClassAssertion: myLunch is a Margherita.
        OWLClassAssertionAxiom clsAssert = factory.getOWLClassAssertionAxiom(margherita, myLunch);
        manager.addAxiom(ontology, clsAssert);

        // A data property assertion with a typed literal. getOWLLiteral is
        // overloaded for the common Java types and picks the right XSD datatype.
        manager.addAxiom(ontology,
                factory.getOWLDataPropertyAssertionAxiom(hasCalories, myLunch, 850));

        // Domain/range are axioms too, not field metadata.
        manager.addAxiom(ontology, factory.getOWLObjectPropertyDomainAxiom(hasTopping, pizza));
        manager.addAxiom(ontology, factory.getOWLObjectPropertyRangeAxiom(hasTopping, topping));

        Tutorial.banner("What we built");
        Tutorial.describe(ontology);

        Tutorial.banner("Every axiom, listed");
        // Sorted so the output is stable across runs -- axiom sets are unordered.
        ontology.getAxioms().stream()
                .map(Object::toString)
                .sorted()
                .forEach(a -> System.out.println("  " + a));

        System.out.println();
        System.out.println("Note how literal 850 rendered as \"850\"^^xsd:integer -- the");
        System.out.println("factory inferred the datatype from the Java int.");


        ontology.getImports();
        ontology.getAnnotations();
        ontology.getAxioms();

        ontology.getAxioms(AxiomType.DISJOINT_DATA_PROPERTIES);

        List<OWLOntologyChange> changes = new ArrayList<>();
        changes.add(new AddAxiom(ontology, margheritaIsAPizza));
        changes.add(new AddAxiom(ontology, clsAssert));
        manager.applyChanges(changes);
    }
}
