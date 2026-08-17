package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Lesson 3 -- class expressions (the "anonymous classes" in Protege).
 *
 * <p>A named class is one kind of {@link OWLClassExpression}. The interesting
 * ones are built with operators: intersection, union, complement, and the
 * restrictions {@code some}, {@code only}, {@code min/max/exactly}, {@code value}.
 *
 * <p>These correspond exactly to what you type into Protege's class-expression
 * editor in Manchester syntax, so this lesson prints both forms side by side.
 */
public class Lesson03_ClassExpressions {

    public static void main(String[] args) throws OWLOntologyCreationException {
        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory f = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        OWLClass pizza = f.getOWLClass(Tutorial.iri("Pizza"));
        OWLClass vegetarianPizza = f.getOWLClass(Tutorial.iri("VegetarianPizza"));
        OWLClass meatTopping = f.getOWLClass(Tutorial.iri("MeatTopping"));
        OWLClass cheeseTopping = f.getOWLClass(Tutorial.iri("CheeseTopping"));
        OWLClass vegetableTopping = f.getOWLClass(Tutorial.iri("VegetableTopping"));
        OWLObjectProperty hasTopping = f.getOWLObjectProperty(Tutorial.iri("hasTopping"));
        OWLDataProperty hasCalories = f.getOWLDataProperty(Tutorial.iri("hasCalories"));

        Tutorial.banner("Existential restriction:  hasTopping some CheeseTopping");

        // "has at least one topping that is a CheeseTopping"
        // ObjectSomeValuesFrom(hasTopping CheeseTopping)
        OWLClassExpression hasSomeCheese =
                f.getOWLObjectSomeValuesFrom(hasTopping, cheeseTopping);
        show(hasSomeCheese);

        Tutorial.banner("Universal restriction:  hasTopping only VegetableTopping");

        // CAREFUL: "only" does NOT imply "at least one". A pizza with no
        // toppings at all satisfies `hasTopping only VegetableTopping`.
        // This trips up nearly everyone at first.
        OWLClassExpression onlyVeg =
                f.getOWLObjectAllValuesFrom(hasTopping, vegetableTopping);
        show(onlyVeg);

        Tutorial.banner("Intersection and complement");

        // Pizza and (hasTopping only (not MeatTopping))
        OWLClassExpression noMeat =
                f.getOWLObjectAllValuesFrom(hasTopping, f.getOWLObjectComplementOf(meatTopping));
        OWLClassExpression vegDefinition = f.getOWLObjectIntersectionOf(pizza, noMeat);
        show(vegDefinition);

        // A *defined* class: equivalence, not subsumption. This is what makes a
        // reasoner able to CLASSIFY pizzas as vegetarian rather than requiring
        // you to assert it. Subclass-of would only constrain, never conclude.
        manager.addAxiom(ontology, f.getOWLEquivalentClassesAxiom(vegetarianPizza, vegDefinition));

        Tutorial.banner("Union");

        OWLClassExpression anyTopping =
                f.getOWLObjectUnionOf(meatTopping, cheeseTopping, vegetableTopping);
        show(anyTopping);

        Tutorial.banner("Cardinality restrictions");

        // hasTopping min 3
        show(f.getOWLObjectMinCardinality(3, hasTopping));
        // hasTopping exactly 1 CheeseTopping
        show(f.getOWLObjectExactCardinality(1, hasTopping, cheeseTopping));
        // hasTopping max 5 Topping
        show(f.getOWLObjectMaxCardinality(5, hasTopping));

        Tutorial.banner("Data restrictions and datatype facets");

        // hasCalories some int[<= 500] -- a restricted datatype.
        OWLClassExpression lowCalorie = f.getOWLDataSomeValuesFrom(
                hasCalories,
                f.getOWLDatatypeMaxInclusiveRestriction(500));
        show(lowCalorie);

        Tutorial.banner("Nesting: the recursive part");

        // Pizza and (hasTopping some (CheeseTopping and (not MeatTopping)))
        // Class expressions compose arbitrarily deep -- that is the whole point.
        OWLClassExpression nested = f.getOWLObjectIntersectionOf(
                pizza,
                f.getOWLObjectSomeValuesFrom(
                        hasTopping,
                        f.getOWLObjectIntersectionOf(
                                cheeseTopping,
                                f.getOWLObjectComplementOf(meatTopping))));
        show(nested);

        Tutorial.banner("Inspecting an expression");

        System.out.println("Type:            " + nested.getClassExpressionType());
        System.out.println("Is anonymous?    " + nested.isAnonymous());
        System.out.println("Classes used:    " + nested.getClassesInSignature());
        System.out.println("Properties used: " + nested.getObjectPropertiesInSignature());
    }

    /** Prints an expression in Manchester syntax (what Protege shows) and raw form. */
    private static void show(OWLClassExpression expression) {
        System.out.println("  Manchester: " + ManchesterPrinter.render(expression));
        System.out.println("  Functional: " + expression);
    }
}
