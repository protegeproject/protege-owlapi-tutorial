# From OWL API to Protégé plugins

Everything in the lessons applies inside Protégé, with three changes: you do not
create the manager, you must route changes so undo works, and your jar is an OSGi
bundle.

All versions and signatures below were verified against Maven Central and the
`protegeproject` repositories (sources at the end).

## Why this tutorial pins OWL API 4.5.29

A plugin must link against the same OWL API the host application exports. From
`protege-parent`'s dependency management:

| Protégé | OWL API (`owlapi-osgidistribution`) | Java |
|---|---|---|
| 5.5.0 | 4.5.9 | 8 |
| 5.6.6 | 4.5.29 | 11 |

Same 4.5.x line, different point releases. This tutorial targets 4.5.29 / Java
11, matching Protégé 5.6.x. If you are building against 5.5.0, drop the OWL API
version to 4.5.9 and the compiler release to 8.

Note the artifact Protégé bundles is `owlapi-osgidistribution` — the OSGi-ready
distribution — not the plain `owlapi-distribution` this tutorial uses for
standalone work.

## Plugin dependencies

Both artifacts are on Maven Central; **no custom repository is required**.
(The `raw.github.com/protegeproject/mvn-repo` URL you may find online is for
building Protégé itself, not for consuming its releases.)

```xml
<dependency>
  <groupId>edu.stanford.protege</groupId>
  <artifactId>protege-editor-owl</artifactId>
  <version>5.6.6</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>edu.stanford.protege</groupId>
  <artifactId>protege-editor-core</artifactId>
  <version>5.6.6</version>
  <scope>provided</scope>
</dependency>
```

### Why `provided` is mandatory, not stylistic

Protégé's distribution installs the OWL API into its `bundles/` directory as its
own OSGi bundle, next to `protege-editor-core` and `protege-editor-owl`:

```xml
<!-- protege-desktop/src/main/assembly/dependency-sets.xml -->
<dependencySet>
  <outputDirectory>bundles</outputDirectory>
  <includes>
    <include>edu.stanford.protege:protege-editor-core:jar</include>
    <include>net.sourceforge.owlapi:owlapi-osgidistribution:jar</include>
    <include>edu.stanford.protege:protege-editor-owl:jar</include>
    <include>com.google.guava:guava:jar</include>
    <include>org.slf4j:slf4j-api:jar</include>
```

So OWL API classes are resolved from the running framework. Embed your own copy
and you get two `OWLOntology` classes from different classloaders, producing
`ClassCastException` or `LinkageError` at runtime — with confusing messages like
"OWLOntology cannot be cast to OWLOntology". Mark the OWL API, Guava and
`slf4j-api` `provided` for the same reason.

## OSGi packaging

```xml
<packaging>bundle</packaging>
...
<plugin>
  <groupId>org.apache.felix</groupId>
  <artifactId>maven-bundle-plugin</artifactId>
  <version>3.0.0</version>
  <extensions>true</extensions>
  <configuration>
    <instructions>
      <Bundle-Activator>org.protege.editor.owl.ProtegeOWL</Bundle-Activator>
      <Bundle-ClassPath>.</Bundle-ClassPath>
      <Bundle-SymbolicName>${project.artifactId};singleton:=true</Bundle-SymbolicName>
      <Bundle-Vendor>Your Name</Bundle-Vendor>
      <Import-Package>
        org.protege.editor.core.*;version="5.0.0",
        org.protege.editor.owl.*;version="5.0.0",
        org.semanticweb.owlapi.*;version="[4.1.2,5.0.0)",
        *
      </Import-Package>
      <Include-Resource>plugin.xml, {maven-resources}</Include-Resource>
    </instructions>
  </configuration>
</plugin>
```

Points that matter:

- **`;singleton:=true` is required.** Most Protégé plugins only work when
  instantiated exactly once.
- **Reuse Protégé's activator** (`org.protege.editor.owl.ProtegeOWL`). You do not
  write your own.
- **Version-range the OWL API import** as `[4.1.2,5.0.0)`. This is what OWLViz
  does, and it stops OSGi from wiring your 4.x plugin to a 5.x OWL API.
- **`Update-Url` is optional** — only for auto-update. It points at a properties
  file with keys `id` (matching the `Bundle-SymbolicName` id), `version`, and
  `download`.
- Enable resource filtering on `src/main/resources` if your `plugin.xml`
  interpolates `${project.artifactId}`.

## Extension points

Declared in `src/main/resources/plugin.xml`.

### A view

Extend `AbstractOWLViewComponent`:

```java
public class MyViewComponent extends AbstractOWLViewComponent {

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        add(buildUi(getOWLModelManager()), BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        // Detach listeners here -- views are created and destroyed repeatedly.
    }
}
```

```xml
<extension id="MyViewComponent"
           point="org.protege.editor.core.application.ViewComponent">
    <label value="My view"/>
    <class value="com.example.MyViewComponent"/>
    <headerColor value="@org.protege.ontologycolor"/>
    <category value="@org.protege.ontologycategory"/>
</extension>
```

### A tab

To host views in a tab, reuse Protégé's `OWLWorkspaceViewsTab`:

```xml
<extension id="MyTab" point="org.protege.editor.core.application.WorkspaceTab">
    <label value="My Tab"/>
    <class value="org.protege.editor.owl.ui.OWLWorkspaceViewsTab"/>
    <editorKitId value="OWLEditorKit"/>
    <defaultViewConfigFileName value="viewconfig-mytab.xml"/>
</extension>
```

### A menu action

Extend `ProtegeOWLAction`:

```java
public class MyToolsAction extends ProtegeOWLAction {

    @Override public void initialise() throws Exception { }
    @Override public void dispose() throws Exception { }

    @Override
    public void actionPerformed(ActionEvent event) {
        OWLClass selected = getOWLWorkspace()
                .getOWLSelectionModel().getLastSelectedClass();
        if (selected == null) {
            return;
        }
        // ... act on the selection
    }
}
```

```xml
<extension id="mytoolsaction"
           point="org.protege.editor.core.application.EditorKitMenuAction">
    <name value="Do the thing"/>
    <path value="org.protege.editor.owl.menu.tools/SlotG-A"/>
    <toolTip value="Appears in the Tools menu"/>
    <class value="com.example.MyToolsAction"/>
    <editorKitId value="any"/>
</extension>
```

`path` is `<parentMenuId>/Slot<Group><Index>`; the Tools menu id is
`org.protege.editor.owl.menu.tools`. Changing the group letter (`SlotJ-A` vs
`SlotG-A`) puts the item in a different separator-delimited group. A top-level
menu is an extension with `path="/SlotG-A"` and **no** `<class>`.

## Getting at the ontology

`AbstractOWLViewComponent` and `ProtegeOWLAction` both give you
`getOWLModelManager()`, `getOWLEditorKit()` and `getOWLWorkspace()`.

```java
OWLModelManager mm = getOWLModelManager();

OWLOntology active     = mm.getActiveOntology();      // where edits go
Set<OWLOntology> all   = mm.getActiveOntologies();    // its imports closure
OWLDataFactory factory = mm.getOWLDataFactory();
OWLReasoner reasoner   = mm.getReasoner();
```

`getActiveOntology()` is the "active ontology" selector in the UI — the one thing
with no equivalent in the standalone lessons. Do not assume there is only one
ontology loaded.

## Applying changes so undo works

**This is the single most important difference.** Route every edit through the
model manager:

```java
// RIGHT -- Protege records this in its HistoryManager, so undo/redo work
mm.applyChange(new AddAxiom(mm.getActiveOntology(), axiom));
mm.applyChanges(listOfChanges);   // List<? extends OWLOntologyChange>

// WRONG in a plugin -- bypasses the history manager; the user cannot undo it
mm.getOWLOntologyManager().addAxiom(ontology, axiom);
```

Exact signatures from `OWLModelManager`:

```java
void applyChange(OWLOntologyChange change);
void applyChanges(List<? extends OWLOntologyChange> changes);
HistoryManager getHistoryManager();   // "tracks the changes ... supports undo and redo"
```

Note `applyChanges` takes a **`List`**, not a `Set` — order is preserved because
inverting it is how undo is implemented (Lesson 6 shows the mechanism).

So the batching habit from Lesson 6 pays off twice here: one `applyChanges` call
is one undo step for the user, and one UI refresh instead of N.

## Rendering entities

Do not use `toString()` or your own short-form provider in UI code. Protégé has a
user preference for rendering by IRI fragment or by `rdfs:label`; honour it:

```java
String text = getOWLModelManager().getRendering(entity);
```

`ManchesterPrinter` in this tutorial is a standalone stand-in for that mechanism.

## Sources

- Coordinates/versions: `repo1.maven.org/maven2/edu/stanford/protege/protege-parent/{5.5.0,5.6.6}/…pom`
- Packaging and `plugin.xml`: [protege-plugin-examples](https://github.com/protegeproject/protege-plugin-examples),
  [owlviz](https://github.com/protegeproject/owlviz)
- `OWLModelManager`: [protege/…/model/OWLModelManager.java](https://github.com/protegeproject/protege/blob/master/protege-editor-owl/src/main/java/org/protege/editor/owl/model/OWLModelManager.java)
- Bundle layout: [protege-desktop/…/dependency-sets.xml](https://github.com/protegeproject/protege/blob/master/protege-desktop/src/main/assembly/dependency-sets.xml)
- Wiki: [PluginAnatomy](https://protegewiki.stanford.edu/wiki/PluginAnatomy),
  [EnablePluginAutoUpdate](https://protegewiki.stanford.edu/wiki/EnablePluginAutoUpdate)

Where the wiki and the examples repo disagree, prefer the repo — the wiki has
older snippets.

## Caveats

- `protege-plugin-examples` pins `protege-editor-owl` 5.0.0 and Java 8. It is the
  right structural reference, but modernise the versions.
- Compile against the Protégé version your users actually run. Building against a
  newer one can cause OSGi resolution failures at load time.
- The `viewconfig-*.xml` format referenced by `defaultViewConfigFileName` is not
  documented here — copy a working example from the examples repo.
