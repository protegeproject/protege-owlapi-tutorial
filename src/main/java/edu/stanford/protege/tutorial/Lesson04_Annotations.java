package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Lesson 4 -- annotations: labels, comments, and metadata.
 *
 * <p>Annotations carry no logical meaning: a reasoner ignores them entirely.
 * But they are what humans actually read, and in Protege they drive the entity
 * rendering (the "Render by label" preference), so any plugin touching the UI
 * needs them.
 *
 * <p>Key distinction in this lesson: an annotation on an <b>entity</b>
 * (rdfs:label on Pizza) versus an annotation on an <b>axiom</b>
 * (a provenance note about why a SubClassOf holds).
 */
public class Lesson04_Annotations {

    public static void main(String[] args) throws OWLOntologyCreationException {
        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory f = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        OWLClass pizza = f.getOWLClass(Tutorial.iri("Pizza"));
        OWLClass margherita = f.getOWLClass(Tutorial.iri("Margherita"));

        Tutorial.banner("rdfs:label with a language tag");

        // The built-in annotation properties have factory shortcuts.
        OWLAnnotationProperty label = f.getRDFSLabel();
        OWLAnnotationProperty comment = f.getRDFSComment();

        // getOWLLiteral(lexicalValue, languageTag) makes a plain literal with a
        // lang tag -- NOT a typed literal. This is what rdfs:label normally uses.
        addAnnotation(manager, f, ontology, pizza, label, f.getOWLLiteral("Pizza", "en"));
        addAnnotation(manager, f, ontology, pizza, label, f.getOWLLiteral("Pizza", "it"));
        addAnnotation(manager, f, ontology, pizza, label, f.getOWLLiteral("Flammkuchen", "de"));

        addAnnotation(manager, f, ontology, pizza, comment,
                f.getOWLLiteral("A flat bread base with toppings.", "en"));

        // Reading them back. EntitySearcher is the convenience API for the
        // "find things about this entity" queries you would otherwise write as
        // manual axiom-set filters.
        Collection<OWLAnnotation> annotations =
                EntitySearcher.getAnnotationObjects(pizza, ontology);
        System.out.println("All annotations on Pizza:");
        for (OWLAnnotation a : annotations) {
            System.out.println("  " + ManchesterPrinter.render(a));
        }

        Tutorial.banner("Picking the label for one language");

        System.out.println("English label: " + labelIn(ontology, f, pizza, "en").orElse("(none)"));
        System.out.println("German label:  " + labelIn(ontology, f, pizza, "de").orElse("(none)"));
        System.out.println("French label:  " + labelIn(ontology, f, pizza, "fr").orElse("(none)"));

        Tutorial.banner("A custom annotation property");

        // Nothing special about custom properties -- just an IRI you choose.
        OWLAnnotationProperty status =
                f.getOWLAnnotationProperty(Tutorial.iri("editorialStatus"));
        manager.addAxiom(ontology, f.getOWLDeclarationAxiom(status));
        addAnnotation(manager, f, ontology, margherita, status, f.getOWLLiteral("needs-review"));

        // Well-known vocabularies are available as enums, so you do not have to
        // remember/retype their IRIs.
        System.out.println("skos:prefLabel IRI  = " + SKOSVocabulary.PREFLABEL.getIRI());
        System.out.println("rdfs:isDefinedBy IRI= "
                + OWLRDFVocabulary.RDFS_IS_DEFINED_BY.getIRI());

        Tutorial.banner("Annotating an AXIOM, not an entity");

        // Why: "Margherita is a Pizza" might need provenance -- who said so and
        // when. That metadata belongs on the axiom.
        //
        // Important consequence: an annotated axiom is NOT equal to the same
        // axiom without annotations, so removing requires matching annotations
        // (or using the axiom's getAxiomWithoutAnnotations()).
        OWLAnnotation provenance = f.getOWLAnnotation(comment,
                f.getOWLLiteral("Asserted by the tutorial, 2026.", "en"));

        OWLAxiom annotatedAxiom =
                f.getOWLSubClassOfAxiom(margherita, pizza, Collections.singleton(provenance));
        manager.addAxiom(ontology, annotatedAxiom);

        OWLAxiom plainAxiom = f.getOWLSubClassOfAxiom(margherita, pizza);

        System.out.println("Annotated: " + ManchesterPrinter.render(annotatedAxiom));
        System.out.println("Axiom is annotated?          " + annotatedAxiom.isAnnotated());
        System.out.println("annotated.equals(plain)?     " + annotatedAxiom.equals(plainAxiom));
        System.out.println("Ontology contains plain?     " + ontology.containsAxiom(plainAxiom));
        System.out.println("Same once annotations dropped? "
                + annotatedAxiom.getAxiomWithoutAnnotations().equals(plainAxiom));

        // Hence this "ignore annotations" variant, which is usually what you
        // want when checking whether a logical axiom is present.
        System.out.println("Contains, ignoring annotations? "
                + ontology.containsAxiom(plainAxiom,
                        Imports.INCLUDED,
                        AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS));

        Tutorial.banner("Annotations on the ontology itself");

        // Ontology-level metadata (version notes, creator, license) is an
        // ontology annotation, applied via a change object rather than an axiom.
        manager.applyChange(new AddOntologyAnnotation(
                ontology,
                f.getOWLAnnotation(f.getRDFSComment(),
                        f.getOWLLiteral("Tutorial ontology for the OWL API 4.x.", "en"))));

        System.out.println("Ontology annotations: " + ontology.getAnnotations().size());
        ontology.getAnnotations()
                .forEach(a -> System.out.println("  " + ManchesterPrinter.render(a)));
    }

    /** Adds an annotation assertion attaching {@code value} to {@code subject}. */
    private static void addAnnotation(OWLOntologyManager manager,
                                      OWLDataFactory f,
                                      OWLOntology ontology,
                                      OWLClass subject,
                                      OWLAnnotationProperty property,
                                      OWLLiteral value) {
        // Note the subject is the entity's IRI, not the entity: annotation
        // subjects are IRIs (or anonymous individuals), which is why you can
        // annotate something that is not even declared.
        manager.addAxiom(ontology,
                f.getOWLAnnotationAssertionAxiom(property, subject.getIRI(), value));
    }

    /** Finds an rdfs:label with the given language tag, if there is one. */
    private static Optional<String> labelIn(OWLOntology ontology,
                                            OWLDataFactory f,
                                            OWLClass entity,
                                            String languageTag) {
        // Passing the annotation property narrows the search to rdfs:label only.
        return EntitySearcher.getAnnotationObjects(entity, ontology, f.getRDFSLabel())
                .stream()
                .map(OWLAnnotation::getValue)
                .filter(v -> v instanceof OWLLiteral)
                .map(v -> (OWLLiteral) v)
                .filter(l -> l.hasLang(languageTag))
                .map(OWLLiteral::getLiteral)
                .findFirst();
    }
}
