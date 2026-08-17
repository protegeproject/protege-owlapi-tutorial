# Recipes

Copy-paste answers to the things you will actually need. All verified against
OWL API 4.5.29.

Assumed setup in each snippet:

```java
OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
OWLDataFactory f = manager.getOWLDataFactory();
```

## Load an ontology

```java
// From a file
OWLOntology o = manager.loadOntologyFromOntologyDocument(new File("pizza.owl"));

// From a URL / IRI
OWLOntology o = manager.loadOntology(IRI.create("http://example.org/pizza"));

// From a string (great for tests)
OWLOntology o = manager.loadOntologyFromOntologyDocument(
        new StringDocumentSource(turtleText));

// From the classpath
try (InputStream in = getClass().getResourceAsStream("/pizza.owl")) {
    OWLOntology o = manager.loadOntologyFromOntologyDocument(in);
}
```

**Reloading something already in memory throws
`OWLOntologyAlreadyExistsException`.** Use a fresh manager, or
`manager.removeOntology(old)` first.

## Save an ontology

```java
// To a file, in a chosen format
manager.saveOntology(o, new TurtleDocumentFormat(), IRI.create(new File("out.ttl")));

// To a string
StringDocumentTarget target = new StringDocumentTarget();
manager.saveOntology(o, new FunctionalSyntaxDocumentFormat(), target);
String text = target.toString();

// Keep the format it was loaded with
manager.saveOntology(o, manager.getOntologyFormat(o), IRI.create(new File("out.owl")));
```

Formats: `RDFXMLDocumentFormat`, `TurtleDocumentFormat`,
`OWLXMLDocumentFormat`, `FunctionalSyntaxDocumentFormat`,
`ManchesterSyntaxDocumentFormat`, `OBODocumentFormat`.

## Load an ontology whose imports are local files

Redirect an IRI to a local document so imports resolve offline:

```java
manager.addIRIMapper(new SimpleIRIMapper(
        IRI.create("http://example.org/core"),
        IRI.create(new File("local/core.owl"))));

// Or map a whole directory by ontology IRI:
manager.addIRIMapper(new AutoIRIMapper(new File("ontologies"), true));
```

(In OWL API 5.x this is `manager.getIRIMappers().add(...)` — another 4-vs-5
difference to watch for in online snippets.)

To tolerate missing imports instead:

```java
OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration()
        .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
OWLOntology o = manager.loadOntologyFromOntologyDocument(
        new FileDocumentSource(new File("x.owl")), config);
```

## Common axioms

```java
// Taxonomy
f.getOWLSubClassOfAxiom(margherita, pizza);
f.getOWLEquivalentClassesAxiom(vegetarianPizza, definition);
f.getOWLDisjointClassesAxiom(cheeseTopping, meatTopping);

// Individuals
f.getOWLClassAssertionAxiom(pizza, myLunch);
f.getOWLObjectPropertyAssertionAxiom(hasTopping, myLunch, someCheese);
f.getOWLDataPropertyAssertionAxiom(hasCalories, myLunch, 850);
f.getOWLDifferentIndividualsAxiom(a, b);   // defeat non-unique names
f.getOWLSameIndividualAxiom(a, b);

// Property characteristics
f.getOWLObjectPropertyDomainAxiom(hasTopping, pizza);
f.getOWLObjectPropertyRangeAxiom(hasTopping, topping);
f.getOWLTransitiveObjectPropertyAxiom(hasPart);
f.getOWLFunctionalObjectPropertyAxiom(hasBase);
f.getOWLInverseObjectPropertiesAxiom(hasTopping, isToppingOf);
f.getOWLSubObjectPropertyOfAxiom(hasCheeseTopping, hasTopping);

// Metadata
f.getOWLDeclarationAxiom(pizza);
f.getOWLAnnotationAssertionAxiom(f.getRDFSLabel(), pizza.getIRI(),
        f.getOWLLiteral("Pizza", "en"));
```

## Class expressions

| Manchester | Java |
|---|---|
| `hasTopping some Cheese` | `f.getOWLObjectSomeValuesFrom(hasTopping, cheese)` |
| `hasTopping only Cheese` | `f.getOWLObjectAllValuesFrom(hasTopping, cheese)` |
| `hasTopping min 2 Cheese` | `f.getOWLObjectMinCardinality(2, hasTopping, cheese)` |
| `hasTopping exactly 1` | `f.getOWLObjectExactCardinality(1, hasTopping)` |
| `hasTopping value theBase` | `f.getOWLObjectHasValue(hasTopping, theBase)` |
| `A and B` | `f.getOWLObjectIntersectionOf(a, b)` |
| `A or B` | `f.getOWLObjectUnionOf(a, b)` |
| `not A` | `f.getOWLObjectComplementOf(a)` |
| `{a, b, c}` | `f.getOWLObjectOneOf(a, b, c)` |
| `hasCalories some int[<= 500]` | `f.getOWLDataSomeValuesFrom(p, f.getOWLDatatypeMaxInclusiveRestriction(500))` |

## Literals

```java
f.getOWLLiteral("Pizza", "en");          // plain, with language tag
f.getOWLLiteral(850);                    // xsd:integer
f.getOWLLiteral(12.5);                   // xsd:double
f.getOWLLiteral(true);                   // xsd:boolean
f.getOWLLiteral("42", OWL2Datatype.XSD_INT);   // explicit datatype
```

Reading them back:

```java
if (literal.hasLang("en")) { ... }
literal.getLiteral();        // the lexical string
literal.parseInteger();      // throws if not an integer type
literal.getDatatype();
```

## Query what is asserted

```java
// By axiom type -- returns the right Java type, no casting
Set<OWLSubClassOfAxiom> subs = o.getAxioms(AxiomType.SUBCLASS_OF, Imports.INCLUDED);

// About one entity
o.getSubClassAxiomsForSubClass(pizza);       // Pizza SubClassOf ?
o.getSubClassAxiomsForSuperClass(pizza);     // ? SubClassOf Pizza
o.getClassAssertionAxioms(pizza);
o.getAnnotationAssertionAxioms(pizza.getIRI());
o.getReferencingAxioms(pizza);               // every axiom mentioning it

// Convenience helpers (org.semanticweb.owlapi.search.EntitySearcher)
EntitySearcher.getSuperClasses(pizza, o);
EntitySearcher.getTypes(myLunch, o);
EntitySearcher.getAnnotationObjects(pizza, o, f.getRDFSLabel());

// Signature
o.getClassesInSignature(Imports.INCLUDED);
o.containsClassInSignature(iri, Imports.INCLUDED);
```

## Find restrictions nested inside expressions

Axiom-type queries only see top level. To find every use of a property
anywhere, walk the structure:

```java
OWLOntologyWalker walker = new OWLOntologyWalker(o.getImportsClosure());
walker.walkStructure(new OWLOntologyWalkerVisitor(walker) {
    @Override public void visit(OWLObjectSomeValuesFrom r) {
        System.out.println(r + " inside " + getCurrentAxiom());
    }
});
```

## Reason

```java
OWLReasoner reasoner = new ReasonerFactory().createReasoner(o);   // HermiT

reasoner.isConsistent();                              // check FIRST
reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();

reasoner.getSuperClasses(pizza, true).getFlattened(); // true = direct only
reasoner.getSubClasses(pizza, false).getFlattened();
reasoner.getEquivalentClasses(pizza).getEntities();
reasoner.getInstances(pizza, false).getFlattened();
reasoner.getTypes(myLunch, true).getFlattened();
reasoner.getObjectPropertyValues(myLunch, hasTopping).getFlattened();

reasoner.isEntailed(f.getOWLSubClassOfAxiom(a, b));

reasoner.flush();     // AFTER any ontology edit
reasoner.dispose();   // when done
```

`NodeSet`/`Node`: each `Node` groups **equivalent** entities. Use
`getFlattened()` when you do not care about that grouping.

**Gotchas:** always check `isConsistent()` first — an inconsistent ontology
entails everything. And a buffering reasoner (HermiT's default) will not see
your edits until `flush()`.

Also, the metadata methods are unreliable: `getReasonerName()` returns `null` on
HermiT 1.3.8, and `getReasonerVersion()` **throws** `NumberFormatException`
because HermiT's build id (`"20151128-2235"`) is not parseable as a version
number. Wrap it if you display it.

## Materialise inferences

```java
List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<>();
gens.add(new InferredSubClassAxiomGenerator());
gens.add(new InferredClassAssertionAxiomGenerator());

OWLOntology target = manager.createOntology(IRI.create("http://example.org/inferred"));
new InferredOntologyGenerator(reasoner, gens).fillOntology(f, target);
```

Filter `owl:Thing` noise afterwards; and keep inferred axioms in a *separate*
ontology so you can still tell them from asserted ones.

## Edit safely

```java
// Batch: fires listeners once, not once per axiom
List<OWLOntologyChange> changes = new ArrayList<>();
changes.add(new AddAxiom(o, ax1));
changes.add(new RemoveAxiom(o, ax2));
manager.applyChanges(changes);

// Rename an entity everywhere
OWLEntityRenamer renamer = new OWLEntityRenamer(manager, Collections.singleton(o));
manager.applyChanges(renamer.changeIRI(oldIri, newIri));

// Delete an entity everywhere
OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(o));
pizza.accept(remover);
manager.applyChanges(remover.getChanges());

// Listen
manager.addOntologyChangeListener(changes -> { /* refresh UI */ });
```

## Render for humans

`toString()` gives full IRIs and is unreadable in logs. Use Manchester syntax:

```java
ManchesterOWLSyntaxOWLObjectRendererImpl r =
        new ManchesterOWLSyntaxOWLObjectRendererImpl();
r.setShortFormProvider(new SimpleShortFormProvider());
String pretty = r.render(axiom);      // "Margherita SubClassOf Pizza"
```

To render by `rdfs:label` instead of IRI fragment, use
`AnnotationValueShortFormProvider`.

## Copy axioms between ontologies

```java
manager.addAxioms(target, source.getAxioms());

// Or extract a module (owlapi has SyntacticLocalityModuleExtractor)
SyntacticLocalityModuleExtractor extractor =
        new SyntacticLocalityModuleExtractor(manager, source, ModuleType.STAR);
Set<OWLAxiom> module = extractor.extract(signature);
```
