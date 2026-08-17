# OWL API 4.x Tutorial (with Protégé in mind)

A runnable, heavily-commented tutorial for the [OWL API](https://github.com/owlcs/owlapi)
4.x, aimed at people who will end up writing Protégé plugins or tools that read
and write OWL ontologies.

- **Java 11** (compiled with `--release 11`, so it runs on 11+)
- **Maven** build
- **OWL API 4.5.29** — the 4.x line, which is what Protégé 5.x ships
- **HermiT 1.3.8.413** as the reasoner

Every lesson is a `main()` you can run and read side by side. Every claim the
comments make is covered by a test.

## Quick start

```bash
mvn compile

# Run any lesson
mvn -q compile exec:java -Dexec.mainClass=edu.stanford.protege.tutorial.Lesson01_HelloOntology

# Run the tests (they double as executable documentation)
mvn test
```

In IntelliJ: open the `pom.xml` as a project, then run any `LessonNN_*` class
directly with the green gutter arrow.

## The lessons

Read them in order; each builds on the last.

| # | Class | What it teaches |
|---|---|---|
| 1 | `Lesson01_HelloOntology` | Manager, ontology, data factory. Ontology IRI vs. document IRI. |
| 2 | `Lesson02_DataFactory` | Entities are inert names; axioms are the content. Declarations vs. assertions. |
| 3 | `Lesson03_ClassExpressions` | `some`/`only`/cardinality/intersection/union/complement, and nesting. |
| 4 | `Lesson04_Annotations` | Labels with language tags; annotating an **axiom** vs. an entity. |
| 5 | `Lesson05_SaveAndLoad` | Five serialisations of one ontology; round-tripping; parsing from a string. |
| 6 | `Lesson06_Changes` | `OWLOntologyChange`, batching, listeners, undo, rename, delete. **Most relevant to Protégé.** |
| 7 | `Lesson07_Reasoning` | Asserted vs. entailed; `NodeSet`; `flush()`; consistency vs. satisfiability. |
| 8 | `Lesson08_QueryingAndImports` | Querying by type/entity; the `Imports` flag; walking nested expressions. |
| 9 | `Lesson09_InferredAxioms` | Materialising inferences (what Protégé's "export inferred axioms" does). |
| 10 | `Lesson10_Prefixes` | Prefix, prefix name, prefixed name; controlling serialised output. |

Supporting classes:

- `Tutorial` — shared IRI/prefix constants and console helpers. Its Javadoc is
  the reference for the prefix terminology used throughout, including a
  worked OBO Foundry example.
- `ManchesterPrinter` — renders any OWL object in Manchester syntax, the way
  Protégé displays it. Use this instead of `toString()` in logs.

## The written guides

- **[docs/01-concepts.md](docs/01-concepts.md)** — the mental model. Read this
  first if you are new to the OWL API; it covers the five ideas that explain why
  the API is shaped the way it is, plus the 4.x-vs-5.x differences.
- **[docs/02-recipes.md](docs/02-recipes.md)** — copy-pasteable answers for
  loading, saving, building axioms, querying, reasoning, and editing.
- **[docs/03-protege-plugins.md](docs/03-protege-plugins.md)** — how this
  translates to Protégé plugin development.

## Things that trip everyone up

Collected here because they account for most lost time:

1. **An ontology is a set of axioms.** There is no `pizza.setSuperClass(...)`.
   You add a `SubClassOf` axiom.
2. **Entities are just names.** `factory.getOWLClass(iri)` adds nothing to any
   ontology.
3. **One manager cannot hold two ontologies with the same IRI.** Reloading
   something already in memory throws `OWLOntologyAlreadyExistsException`; use a
   fresh manager.
4. **`Imports.EXCLUDED` is the no-arg default.** If a query cannot find an axiom
   you know exists, check this first.
5. **Annotated axioms are not equal to unannotated ones.** Use
   `getAxiomWithoutAnnotations()` or `AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS`.
6. **Reasoners buffer.** Call `reasoner.flush()` after editing, or you will read
   stale inferences.
7. **Check `isConsistent()` first.** An inconsistent ontology entails everything,
   which makes every other answer meaningless.
8. **`only` does not imply `some`.** `hasTopping only Cheese` is satisfied by a
   pizza with no toppings at all — OWL is open-world.
9. **OWL API 4.x uses Guava's `Optional`**, not `java.util.Optional`. Snippets
   found online often assume 5.x.

## Why OWL API 4.x and not 5.x/6.x?

Because Protégé 5.x embeds the 4.x line, and a plugin must link against the same
OWL API the host application exports. If you are writing a standalone tool with
no Protégé involvement, prefer the latest OWL API instead — but expect the
`Optional` and stream-vs-set differences noted in
[docs/01-concepts.md](docs/01-concepts.md).
