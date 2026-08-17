package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lesson 8 -- querying an ontology, and how imports change the answers.
 *
 * <p>Two themes:
 * <ol>
 *   <li>The many ways to ask "what does this ontology say?" -- by axiom type, by
 *       referencing entity, or by walking the whole structure.
 *   <li>The {@link Imports} flag, which decides whether a query sees only this
 *       ontology or its whole import closure. Getting this wrong is the source of
 *       a lot of "why can't I find my axiom?" confusion.
 * </ol>
 */
public class Lesson08_QueryingAndImports {

    public static void main(String[] args) throws OWLOntologyCreationException {
        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory f = manager.getOWLDataFactory();

        // --- Two ontologies: a "core" of toppings, imported by the pizza one. ---

        IRI coreIri = IRI.create(Tutorial.relatedOntologyIri("core"));
        OWLOntology core = manager.createOntology(coreIri);

        OWLClass topping = f.getOWLClass(Tutorial.iri("Topping"));
        OWLClass cheeseTopping = f.getOWLClass(Tutorial.iri("CheeseTopping"));
        manager.addAxiom(core, f.getOWLSubClassOfAxiom(cheeseTopping, topping));

        OWLOntology pizzas = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        // An import is declared by an axiom-like change on the ontology.
        manager.applyChange(new AddImport(
                pizzas, f.getOWLImportsDeclaration(coreIri)));

        OWLClass pizza = f.getOWLClass(Tutorial.iri("Pizza"));
        OWLClass margherita = f.getOWLClass(Tutorial.iri("Margherita"));
        OWLObjectProperty hasTopping = f.getOWLObjectProperty(Tutorial.iri("hasTopping"));

        manager.addAxiom(pizzas, f.getOWLSubClassOfAxiom(margherita, pizza));
        manager.addAxiom(pizzas, f.getOWLSubClassOfAxiom(pizza,
                f.getOWLObjectSomeValuesFrom(hasTopping, topping)));
        manager.addAxiom(pizzas, f.getOWLAnnotationAssertionAxiom(
                f.getRDFSLabel(), pizza.getIRI(), f.getOWLLiteral("Pizza", "en")));

        Tutorial.banner("Imports: EXCLUDED vs INCLUDED");

        // THE flag to remember. EXCLUDED = just this ontology's own axioms.
        // INCLUDED = this ontology plus everything it imports, transitively.
        System.out.println("Axioms, EXCLUDED: " + pizzas.getAxiomCount(Imports.EXCLUDED));
        System.out.println("Axioms, INCLUDED: " + pizzas.getAxiomCount(Imports.INCLUDED));

        System.out.println("Classes in signature, EXCLUDED: "
                + names(pizzas.getClassesInSignature(Imports.EXCLUDED)));
        System.out.println("Classes in signature, INCLUDED: "
                + names(pizzas.getClassesInSignature(Imports.INCLUDED)));

        // No-arg getAxioms() means EXCLUDED. Worth knowing, because the default
        // is not always the one you want.
        System.out.println("Import closure size: " + pizzas.getImportsClosure().size()
                + " (includes the ontology itself)");
        System.out.println("Direct imports: " + pizzas.getDirectImportsDocuments());

        Tutorial.banner("Querying by axiom type");

        // AxiomType is a type-safe key: the returned set is already the right
        // Java type, so no casting.
        Set<OWLSubClassOfAxiom> subClassAxioms =
                pizzas.getAxioms(AxiomType.SUBCLASS_OF, Imports.INCLUDED);
        System.out.println("SubClassOf axioms (with imports): " + subClassAxioms.size());
        subClassAxioms.stream()
                .map(ManchesterPrinter::render).sorted()
                .forEach(a -> System.out.println("  " + a));

        Set<OWLAnnotationAssertionAxiom> annotationAxioms =
                pizzas.getAxioms(AxiomType.ANNOTATION_ASSERTION, Imports.EXCLUDED);
        System.out.println("Annotation assertions: " + annotationAxioms.size());

        Tutorial.banner("Querying by entity");

        // "What does this ontology say about Pizza?" -- axioms where Pizza is
        // the subject/subclass side.
        System.out.println("Axioms about Pizza (as subclass):");
        pizzas.getSubClassAxiomsForSubClass(pizza)
                .forEach(a -> System.out.println("  " + ManchesterPrinter.render(a)));

        // Or every axiom that mentions it anywhere -- the usual choice when
        // building a UI panel for an entity.
        System.out.println("Every axiom referencing Topping (with imports):");
        for (OWLOntology o : pizzas.getImportsClosure()) {
            o.getReferencingAxioms(topping)
                    .forEach(a -> System.out.println("  " + ManchesterPrinter.render(a)));
        }

        Tutorial.banner("Walking nested structure with a visitor");

        // Querying by axiom type finds top-level axioms, but restrictions hide
        // INSIDE class expressions. OWLOntologyWalker descends into them, which
        // is how you answer "where is hasTopping actually used?"
        OWLOntologyWalker walker = new OWLOntologyWalker(pizzas.getImportsClosure());

        OWLOntologyWalkerVisitor visitor = new OWLOntologyWalkerVisitor(walker) {
            @Override
            public void visit(OWLObjectSomeValuesFrom restriction) {
                System.out.println("  found existential: "
                        + ManchesterPrinter.render(restriction));
                // getCurrentAxiom() tells you which axiom you are inside -- the
                // context you would otherwise have to thread through manually.
                System.out.println("    inside axiom: "
                        + ManchesterPrinter.render(getCurrentAxiom()));
            }
        };
        walker.walkStructure(visitor);

        Tutorial.banner("Signature and entity lookup by IRI");

        System.out.println("Contains Pizza (EXCLUDED)?        "
                + pizzas.containsClassInSignature(pizza.getIRI(), Imports.EXCLUDED));
        System.out.println("Contains CheeseTopping (EXCLUDED)? "
                + pizzas.containsClassInSignature(cheeseTopping.getIRI(), Imports.EXCLUDED));
        System.out.println("Contains CheeseTopping (INCLUDED)? "
                + pizzas.containsClassInSignature(cheeseTopping.getIRI(), Imports.INCLUDED));

        // Reverse lookup: given an IRI, what entities use it? An IRI can name a
        // class AND a property simultaneously (OWL 2 "punning"), so this returns
        // a set rather than one entity.
        System.out.println("Entities with Pizza's IRI: "
                + pizzas.getEntitiesInSignature(pizza.getIRI(), Imports.INCLUDED));

        Tutorial.banner("A gotcha: axioms live in ONE ontology");

        OWLAxiom cheeseAxiom = f.getOWLSubClassOfAxiom(cheeseTopping, topping);
        System.out.println("pizzas.containsAxiom(cheese..), EXCLUDED: "
                + pizzas.containsAxiom(cheeseAxiom, Imports.EXCLUDED,
                        AxiomAnnotations.CONSIDER_AXIOM_ANNOTATIONS));
        System.out.println("pizzas.containsAxiom(cheese..), INCLUDED: "
                + pizzas.containsAxiom(cheeseAxiom, Imports.INCLUDED,
                        AxiomAnnotations.CONSIDER_AXIOM_ANNOTATIONS));
        System.out.println("core.containsAxiom(cheese..):            "
                + core.containsAxiom(cheeseAxiom));
        System.out.println();
        System.out.println("Adding an axiom to `pizzas` never modifies `core`.");
        System.out.println("To edit an imported axiom you must target the ontology");
        System.out.println("that actually holds it -- in Protege, that is the");
        System.out.println("\"active ontology\" selector.");
    }

    /** Renders a set of classes as short names, sorted, for readable output. */
    private static String names(Set<OWLClass> classes) {
        return classes.stream()
                .map(ManchesterPrinter::render)
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
