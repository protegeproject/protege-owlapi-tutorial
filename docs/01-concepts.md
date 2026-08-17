# The mental model

Almost every OWL API misunderstanding traces back to one of five ideas. Get
these straight and the API stops feeling arbitrary.

## 1. An ontology is a set of axioms

Not a graph you navigate, not an object with setters. `OWLOntology` is,
conceptually, a `Set<OWLAxiom>` with an identity.

Consequences that surprise people:

- There is no `pizza.setSuperClass(food)`. You add a `SubClassOf(Pizza, Food)`
  **axiom**.
- Axiom sets are unordered. If you need stable output, sort it yourself.
- Two ontologies with the same axioms are logically identical regardless of
  which file format they came from.

## 2. Entities are names, not objects with content

An `OWLClass` is an IRI plus a Java type. That is all. It holds no list of
superclasses, no properties, no annotations.

```java
OWLClass pizza = factory.getOWLClass(IRI.create(NS + "Pizza"));
// Nothing has been added to any ontology. This is just a name.
```

So "what are Pizza's superclasses?" is not a question you ask the class. You ask
an **ontology** (what is asserted) or a **reasoner** (what follows).

This is also why `OWLEntityRenamer` exists: since an entity *is* its IRI,
renaming means rewriting every axiom that mentions the old IRI.

## 3. The data factory builds everything

You never `new` an OWL API model object. `OWLDataFactory` is the single
constructor for entities, class expressions, axioms and literals. That
indirection lets the implementation intern objects and keep them immutable —
which is why entities are safe to use as `HashMap` keys and to share freely
across threads.

## 4. The manager owns lifecycle and change

`OWLOntologyManager` creates, loads and saves ontologies, resolves imports, and
applies changes. Two rules follow:

- **One manager cannot hold two ontologies with the same ontology IRI.** Trying
  throws `OWLOntologyAlreadyExistsException`. Use a fresh manager to reload
  something you already have in memory.
- **Changes go through the manager**, as `OWLOntologyChange` objects. This is
  what makes undo, batching and change listeners possible.

`manager.addAxiom(o, ax)` is sugar for
`manager.applyChange(new AddAxiom(o, ax))`.

## 5. Asserted is not entailed

The single most important distinction in OWL.

| Question | Ask |
|---|---|
| What did someone write down? | the `OWLOntology` |
| What logically follows? | an `OWLReasoner` |

```java
ontology.containsAxiom(f.getOWLSubClassOfAxiom(margherita, vegetarianPizza));
// false -- nobody asserted this

reasoner.isEntailed(f.getOWLSubClassOfAxiom(margherita, vegetarianPizza));
// true  -- but it follows from the definitions
```

In Protégé this is the white (asserted) versus yellow (inferred) distinction in
the class hierarchy.

Two corollaries that bite:

**Open world.** Absence of information is not negation. If nothing says a pizza
has a meat topping, OWL does *not* conclude it has none. It concludes nothing.
That is why `hasTopping only CheeseTopping` is satisfied by a pizza with no
toppings at all.

**No unique names.** Two differently-named individuals may be the same thing
unless you say otherwise (`DifferentIndividuals`, or `HasKey`).

## Where the pieces live

```
OWLOntologyManager  ── creates/loads/saves ──►  OWLOntology
        │                                          (Set<OWLAxiom>)
        │ getOWLDataFactory()                          ▲
        ▼                                              │ axioms built by
OWLDataFactory ──── builds entities, expressions, axioms
        
OWLReasonerFactory ── createReasoner(ontology) ──►  OWLReasoner
                                                    (answers entailment
                                                     questions; needs flush()
                                                     after edits)
```

## The `Imports` flag

Nearly every query method has an overload taking
`org.semanticweb.owlapi.model.parameters.Imports`:

- `Imports.EXCLUDED` — only this ontology's own axioms. **This is the no-arg default.**
- `Imports.INCLUDED` — this ontology plus its whole import closure.

If a query mysteriously cannot find an axiom you know exists, this flag is the
first thing to check. And remember: an axiom belongs to exactly one ontology.
Adding to an importing ontology never modifies the imported one — in Protégé,
that is what the "active ontology" selector controls.

## OWL API 4.x vs 5.x

This tutorial targets **4.x** because that is what Protégé 5.x ships. The two
differences you will actually trip over:

| | 4.x (here) | 5.x |
|---|---|---|
| Optional type | `com.google.common.base.Optional` (Guava) | `java.util.Optional` |
| Collections | `Set<...>` returns | streams (`...Stream()` methods) |

So in 4.x you write `ontology.getOntologyID().getOntologyIRI().orNull()`, where
5.x would use `.orElse(null)`. Code samples found online frequently mix these
up; if a snippet does not compile, the version is the first suspect.
