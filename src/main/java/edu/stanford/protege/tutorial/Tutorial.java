package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Shared constants and small helpers used by every lesson.
 *
 * <p>Nothing here is OWL API machinery you need to learn -- it just keeps the
 * lessons free of repeated boilerplate so the OWL API calls stand out.
 */
public final class Tutorial {

    /**
     * The ontology IRI of the little "pizza-ish" ontology the lessons build up.
     *
     * <p>By convention an ontology IRI looks like a web address. It does not
     * have to resolve to anything -- it is an identifier, not a URL you fetch.
     * (You can make it resolvable, and for published ontologies you should,
     * but the OWL API never requires it.)
     */
    public static final String ONTOLOGY_IRI = "http://example.org/tutorial/food";

    /**
     * The <b>prefix</b> that entity IRIs in this tutorial are formed from.
     *
     * <p>Terminology, which is worth getting right because the OWL API and
     * Protege both use it precisely:
     *
     * <ul>
     *   <li>a <b>prefix</b> is the IRI you abbreviate, here
     *       {@code http://example.org/tutorial/food/};
     *   <li>a <b>prefix name</b> is the short label standing in for it, and
     *       always ends with a colon -- {@code food:} below. Even the empty
     *       prefix name is written {@code :};
     *   <li>a <b>prefixed name</b> is the abbreviated IRI itself, e.g.
     *       {@code food:Pizza}, which expands to
     *       {@code http://example.org/tutorial/food/Pizza}.
     * </ul>
     *
     * <p>A prefix should end in a slash ({@code /}) or a hash ({@code #}). The
     * modern convention is a slash, which is what this tutorial uses. Protege
     * still defaults to a hash for a newly created ontology, so you will see
     * both in the wild -- and a prefix that ends mid-word cannot be used to
     * abbreviate anything (Lesson 10 demonstrates that failure).
     *
     * <h2>A real-world example: OBO Foundry</h2>
     *
     * <p>The OBO Foundry ontologies (GO, CL, ChEBI, Uberon, ...) are a useful
     * example to study, because they use one shared, slash-terminated prefix
     * across many ontologies, and because their term IRIs are systematically
     * generated rather than hand-chosen. Every OBO term IRI is built from:
     *
     * <pre>
     *   prefix        http://purl.obolibrary.org/obo/
     *   prefix name   obo:
     *   prefixed name obo:GO_0006915
     *   full IRI      http://purl.obolibrary.org/obo/GO_0006915
     * </pre>
     *
     * <p>That IRI is the Gene Ontology term "apoptotic process", and unlike our
     * {@code example.org} IRIs it really does resolve: the PURL 303-redirects to
     * a term page, which is the OBO Foundry's whole reason for using purl.org.
     *
     * <p>Note the slash-terminated prefix, and that the local name encodes the
     * source ontology and a numeric id joined by an <em>underscore</em>
     * ({@code GO_0006915}), not a colon. That underscore matters: {@code GO:0006915}
     * is the OBO <b>CURIE</b> that appears in OBO-format files and in publications,
     * but it is <em>not</em> the IRI. Converting between the two --
     * {@code GO:0006915} to {@code http://purl.obolibrary.org/obo/GO_0006915} --
     * is a step people frequently get wrong when loading OBO data.
     *
     * <p>In practice tools also register per-ontology prefix names, so that the
     * same term can be written {@code GO:0006915} against the prefix
     * {@code http://purl.obolibrary.org/obo/GO_}:
     *
     * <pre>
     *   obo:  ->  http://purl.obolibrary.org/obo/        (shared, slash-terminated)
     *   GO:   ->  http://purl.obolibrary.org/obo/GO_     (per-ontology, underscore)
     *   CL:   ->  http://purl.obolibrary.org/obo/CL_
     *   RO:   ->  http://purl.obolibrary.org/obo/RO_
     * </pre>
     *
     * <p>The {@code GO:} form is the one exception to "prefixes end in / or #":
     * it deliberately ends in {@code _} so the numeric id alone forms the local
     * name. It works because the underscore is a legal name character, and it is
     * the convention OBO tooling expects -- so treat it as a documented special
     * case rather than a licence to end prefixes anywhere you like.
     */
    public static final String PREFIX = ONTOLOGY_IRI + "/";

    /** The prefix name for {@link #PREFIX}. Note the trailing colon. */
    public static final String PREFIX_NAME = "food:";

    /**
     * Builds the ontology IRI of a related ontology, e.g. {@code "core"} gives
     * {@code http://example.org/tutorial/food-core}.
     *
     * <p>Deliberately a sibling ({@code food-core}) rather than a child
     * ({@code food/core}): the latter would fall <em>inside</em> {@link #PREFIX},
     * so {@code core} would be indistinguishable from an entity local name.
     * Keeping ontology IRIs out of your entity prefix avoids that ambiguity.
     */
    public static String relatedOntologyIri(String suffix) {
        return ONTOLOGY_IRI + "-" + suffix;
    }

    /**
     * Expands a local name into a full IRI using the tutorial's {@link #PREFIX}.
     *
     * <p>{@code iri("Pizza")} is the prefixed name {@code food:Pizza} written
     * out in full.
     */
    public static IRI iri(String localName) {
        return IRI.create(PREFIX, localName);
    }

    /** Prints a titled banner, so multi-step console output stays readable. */
    public static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /**
     * Prints a one-line summary of an ontology: its IRI plus how many axioms
     * and classes it holds.
     */
    public static void describe(OWLOntology ontology) {
        System.out.println("Ontology:   " + ontology.getOntologyID());
        System.out.println("Axioms:     " + ontology.getAxiomCount());
        System.out.println("Classes:    " + ontology.getClassesInSignature().size());
        System.out.println("Individuals:" + ontology.getIndividualsInSignature().size());
    }

    /**
     * Quietens the SLF4J simple-logger banner that the OWL API emits on
     * startup, so lesson output is easier to read. Purely cosmetic.
     */
    public static void quietLogging() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private Tutorial() {
        // Not instantiable: this is a holder for static helpers.
    }

    /**
     * Convenience for lessons that just need a manager. Kept as a method rather
     * than a field because a manager is stateful -- see docs/02-manager.md for
     * why you generally want one manager per unit of work.
     */
    public static OWLOntologyManager newManager() {
        return OWLManager.createOWLOntologyManager();
    }
}
