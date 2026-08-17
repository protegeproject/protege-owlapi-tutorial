package edu.stanford.protege.tutorial;

import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

/**
 * Renders OWL objects in Manchester syntax -- the notation Protege's editors
 * and class-hierarchy tooltips use.
 *
 * <p>This is worth knowing because {@code toString()} on an OWL API object gives
 * you functional/OWL-XML-ish output with full IRIs, which is unreadable in logs.
 * Protege itself renders through a {@link ShortFormProvider}, so this class is a
 * miniature version of what the Protege UI does.
 */
public final class ManchesterPrinter {

    private static final ManchesterOWLSyntaxOWLObjectRendererImpl RENDERER =
            new ManchesterOWLSyntaxOWLObjectRendererImpl();

    static {
        // A short-form provider decides how an entity's IRI is abbreviated for
        // display. SimpleShortFormProvider uses the IRI fragment / last path
        // segment, so http://example.org/tutorial/food#Pizza renders as "Pizza".
        //
        // In a Protege plugin you would instead reuse Protege's own renderer,
        // so your plugin honours the user's chosen rendering (labels vs. IRIs).
        RENDERER.setShortFormProvider(new SimpleShortFormProvider());
    }

    /**
     * Renders any OWL object -- a class expression, an axiom, an entity -- as a
     * single-line Manchester syntax string.
     */
    public static String render(OWLObject object) {
        // The renderer inserts newlines for long axioms; collapse them so each
        // rendered object stays on one console line.
        return RENDERER.render(object).replaceAll("\\s+", " ").trim();
    }

    private ManchesterPrinter() {
    }
}
