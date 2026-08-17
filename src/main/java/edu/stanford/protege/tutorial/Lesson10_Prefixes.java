package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

/**
 * Lesson 10 -- prefixes, prefix names, and prefixed names.
 *
 * <p>Terminology first, because these three are routinely muddled and the OWL API
 * uses them precisely:
 *
 * <ul>
 *   <li>a <b>prefix</b> is the IRI being abbreviated, e.g.
 *       {@code https://example.org/ontology/}. A prefix should end in a slash
 *       or a hash; the modern convention is a slash;
 *   <li>a <b>prefix name</b> is the short label standing in for it, and
 *       <b>always ends in a colon</b>: {@code ex:}. The empty prefix name is
 *       written {@code :};
 *   <li>a <b>prefixed name</b> is the abbreviation itself: {@code ex:Pizza},
 *       which expands to {@code https://example.org/ontology/Pizza}.
 * </ul>
 *
 * <p>Prefixes are purely a <em>serialisation and display</em> concern. They never
 * change the logical content: {@code ex:Pizza} and the full IRI are the same
 * entity. But they decide how readable your saved file is, and in Protege they
 * are what the "Active ontology > Prefixes" tab edits.
 */
public class Lesson10_Prefixes {

    public static void main(String[] args)
            throws OWLOntologyCreationException, OWLOntologyStorageException {
        Tutorial.quietLogging();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory f = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology(IRI.create(Tutorial.ONTOLOGY_IRI));

        Tutorial.banner("Expanding a prefixed name by hand");

        // A PrefixManager maps prefix names to prefixes, and can expand a
        // prefixed name into a full IRI.
        DefaultPrefixManager prefixManager = new DefaultPrefixManager();

        // setPrefix takes the prefix NAME (with its colon) and the prefix.
        prefixManager.setPrefix("food:", Tutorial.PREFIX);

        // The empty prefix name is ":" -- used for the ontology's "own" terms,
        // and what Protege writes by default.
        prefixManager.setPrefix(":", Tutorial.PREFIX);

        // Note this tutorial's prefix ends in '/'. A prefix should end in a
        // slash or a hash; the modern convention is the slash. Protege still
        // defaults to a hash for newly created ontologies, so both are common.

        System.out.println("prefix name  : food:");
        System.out.println("prefix       : " + Tutorial.PREFIX);
        System.out.println("prefixed name: food:Pizza");
        System.out.println("expands to   : " + prefixManager.getIRI("food:Pizza"));
        System.out.println();
        System.out.println("With the empty prefix name, :Pizza expands to "
                + prefixManager.getIRI(":Pizza"));

        Tutorial.banner("Using a PrefixManager with the data factory");

        // getOWLClass(String, PrefixManager) takes a PREFIXED NAME directly, so
        // you never concatenate strings to build IRIs.
        OWLClass pizza = f.getOWLClass("food:Pizza", prefixManager);
        OWLClass margherita = f.getOWLClass("food:Margherita", prefixManager);

        System.out.println("f.getOWLClass(\"food:Pizza\", pm) -> " + pizza);
        System.out.println("Same as the long form? "
                + pizza.equals(f.getOWLClass(IRI.create(Tutorial.PREFIX, "Pizza"))));

        manager.addAxiom(ontology, f.getOWLSubClassOfAxiom(margherita, pizza));

        Tutorial.banner("Declaring prefixes in the OUTPUT document");

        // To control how a saved file abbreviates IRIs, set the prefixes on the
        // document FORMAT, not on the ontology. A format that supports prefixes
        // implements PrefixDocumentFormat.
        TurtleDocumentFormat turtle = new TurtleDocumentFormat();
        turtle.setPrefix("food:", Tutorial.PREFIX);

        StringDocumentTarget withPrefix = new StringDocumentTarget();
        manager.saveOntology(ontology, turtle, withPrefix);

        System.out.println("--- Turtle WITH a 'food:' prefix declared ---");
        System.out.println(withPrefix.toString().trim());

        // Without it, the serialiser falls back to a generated prefix name or
        // full IRIs. Logically identical; far less pleasant to read.
        StringDocumentTarget withoutPrefix = new StringDocumentTarget();
        manager.saveOntology(ontology, new TurtleDocumentFormat(), withoutPrefix);

        System.out.println();
        System.out.println("--- Turtle with NO prefix declared ---");
        System.out.println(withoutPrefix.toString().trim());

        Tutorial.banner("Prefixes are cosmetic: the axioms are unchanged");

        System.out.println("Both documents above assert exactly:");
        ontology.getLogicalAxioms()
                .forEach(a -> System.out.println("  " + ManchesterPrinter.render(a)));

        Tutorial.banner("Reading the prefixes a document was parsed with");

        // When loading, the format object records the prefixes it saw. This is
        // how Protege can preserve your prefix declarations on save.
        OWLDocumentFormat format = manager.getOntologyFormat(ontology);
        System.out.println("This ontology's format: " + format);

        if (format instanceof PrefixDocumentFormat) {
            PrefixDocumentFormat prefixFormat = (PrefixDocumentFormat) format;
            System.out.println("Prefix name -> prefix map:");
            prefixFormat.getPrefixName2PrefixMap()
                    .forEach((name, prefix) -> System.out.println("  " + name + "  ->  " + prefix));
        }

        Tutorial.banner("A trap: prefix must end at a sensible boundary");

        // A prefix must end at a name boundary -- a slash or a hash. Here the
        // prefix stops mid-word ("...food" instead of "...food/"), so nothing
        // can be abbreviated with it. The writer silently falls back to another
        // prefix or the full IRI rather than reporting an error.
        RDFXMLDocumentFormat rdfxml = new RDFXMLDocumentFormat();
        rdfxml.setPrefix("bad:", Tutorial.ONTOLOGY_IRI);   // no trailing '/'!

        StringDocumentTarget badTarget = new StringDocumentTarget();
        manager.saveOntology(ontology, rdfxml, badTarget);

        boolean abbreviated = badTarget.toString().contains("bad:");
        System.out.println("Did the 'bad:' prefix get used? " + abbreviated);
        System.out.println("(Prefixes should end at '#' or '/'.)");

        Tutorial.banner("Rendering with prefixed names");

        // A ShortFormProvider backed by a PrefixManager renders entities as
        // prefixed names -- closer to what Protege shows than a bare fragment.
        PrefixManager pm = prefixManager;
        System.out.println("Pizza as a prefixed name: "
                + pm.getPrefixIRI(pizza.getIRI()));
        System.out.println("(getPrefixIRI is the inverse of getIRI: full IRI -> prefixed name.)");
    }
}
