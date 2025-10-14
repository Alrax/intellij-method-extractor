# IntelliJ Method Extractor

<!-- Plugin description -->
Extracts names and bodies of Java methods in the open project and writes them into a single JSON file (methods.json). Runs automatically after project opens and indexing completes, and also provides a Tools menu action to dump on demand.
<!-- Plugin description end -->

## Overview
This IntelliJ IDEA plugin scans Java source files using PSI and file-based indexes and produces a JSON snapshot of all declared methods:
- On project open: waits until the IDE finishes configuring and indices are ready, then writes methods.json to the project root.
- On demand: Tools > Dump Methods to JSON action to re-generate the snapshot after changes.

The JSON contains a list of objects with two keys: "name" and "body". Methods without a body (e.g., abstract/interface methods) will have a null body.

## How it fulfills the task instructions
- File-based indexes: Uses FileTypeIndex over GlobalSearchScope.projectScope(project) to enumerate Java files without manual project traversal.
- PSI parsing: Walks PsiJavaFile classes and PsiMethod declarations via JavaRecursiveElementVisitor to extract method name and body text.
- ProjectActivity on open: Registers a ProjectActivity that triggers once a project opens.
- Wait for indexing: Calls Observation.awaitConfiguration(project) and project.waitForSmartMode() before scanning.
- JSON library: Uses Gson (com.google.code.gson:gson:2.11.0) to serialize to JSON with pretty-printing.
- Output format: Produces a single methods.json in the project root with [{"name": string, "body": string|null}, ...].
- Bonus action: Adds a Tools menu action "Dump Methods to JSON" to re-run extraction; updating a method and invoking the action updates the corresponding entry in JSON.

## Architecture and key files
- services/MethodExtractor.kt
  - Provides MethodExtractor.collectAll(project): List<MethodInfo>.
  - Inside a ReadAction, enumerates Java VirtualFile entries via FileTypeIndex, resolves to PsiJavaFile, visits classes and collects PsiMethod data.
  - MethodInfo fields: name (simple method name) and body (trimmed method body text or null).

- services/JsonExporter.kt
  - Serializes List<MethodInfo> with Gson (pretty, HTML escaping disabled) and writes to <projectRoot>/methods.json.
  - Overwrites existing file each time.

- startup/DumpMethodsOnOpen.kt
  - ProjectActivity that runs on project open.
  - Suspends until Observation.awaitConfiguration(project) and project.waitForSmartMode() complete, then invokes MethodExtractor and JsonExporter.

- actions/DumpMethodsAction.kt
  - UI action registered in Tools menu.
  - Uses DumbService.runWhenSmart to ensure indices are ready, then runs Task.Backgroundable to extract and dump without blocking the UI thread.

- resources/META-INF/plugin.xml
  - Declares plugin id, name, dependencies (platform, java), the postStartupActivity (DumpMethodsOnOpen), and the action in the Tools menu.

## JSON schema
Each item in methods.json follows:
- name: string — the simple method name (no class or package qualification)
- body: string|null — the method body including braces, e.g., "{ return x + y; }"; null for methods without bodies (abstract/interface)

Example (abbreviated):
[
  { "name": "add", "body": "{ return x + y; }" },
  { "name": "toString", "body": "{ return \"...\"; }" },
  { "name": "save", "body": null }
]

## Build and run (local)
Prerequisites:
- JDK 21
- Gradle wrapper (included)

Windows (cmd.exe):
- Build the plugin
  - gradlew.bat build
- Run the IDE with the plugin
  - gradlew.bat runIde

This launches a sandbox IDE with the plugin enabled. Open or create a Java project in this IDE to test behavior.

## Usage
- Automatic on open
  - Open a Java project in the runIde sandbox. The plugin waits for project configuration and indexing to finish, then writes methods.json to the project root.
- Manual dump via action
  - Tools > Dump Methods to JSON
  - Re-runs extraction and overwrites methods.json with the latest results (useful after editing methods).
- Output location
  - <projectRoot>/methods.json

## Generating JSON for spring-petclinic (example)
1) Launch the sandbox IDE with the plugin:
- gradlew.bat runIde
2) In the sandbox IDE, clone and open https://github.com/spring-projects/spring-petclinic or open an existing checkout.
3) Wait until indexing completes (progress bar finishes). The plugin will auto-generate methods.json at the project root.
4) Optionally, make code changes and run Tools > Dump Methods to JSON to refresh the file.

## Implementation details
Below is a concise explanation of the core classes and their responsibilities, reflecting the inline code comments:

- DumpMethodsAction
  - Purpose: On-demand trigger to extract methods and write JSON.
  - Key points: Uses DumbService.runWhenSmart, executes as Task.Backgroundable, delegates to MethodExtractor and JsonExporter.

- MethodExtractor
  - Purpose: ReadAction-safe scanning of Java files to extract PsiMethod data.
  - Index usage: FileTypeIndex.getFiles(StdFileTypes.JAVA, GlobalSearchScope.projectScope(project)).
  - PSI traversal: PsiJavaFile -> JavaRecursiveElementVisitor -> PsiClass.methods.
  - Output: List<MethodInfo(name, body)).

- JsonExporter
  - Purpose: Serialize and persist method data.
  - Behavior: Pretty-printed JSON; overwrites <projectRoot>/methods.json; base path from Project.basePath or presentableUrl.

- DumpMethodsOnOpen
  - Purpose: Project startup hook to auto-generate methods.json after indexing.
  - Awaiting readiness: Observation.awaitConfiguration(project) and project.waitForSmartMode().

- plugin.xml
  - Registers the ProjectActivity and the Tools menu action; depends on platform and Java modules.

## Limitations and notes
- Scope: Only Java source files (*.java) are scanned; Kotlin and other languages are out of scope.
- Method identity: Uses only simple method names in JSON; overloaded methods will appear as multiple items with the same name.
- Bodies: Method body text is captured verbatim (including braces). Abstract/interface methods produce null bodies.
- Updates: The file updates automatically on project open and when the manual action is used; there’s no continuous watching.
- Performance: The plugin is slow on large projects due to full scans; optimizations like caching or incremental updates are not implemented.