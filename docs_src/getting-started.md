<h1 id="getting-started">Getting Started<a class="headerlink" href="#getting-started" title="Permanent link">&para;</a></h1>

You need to have [JRE 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html) installed on your computer to use Arend.
Arend is available either as an [IntelliJ IDEA](https://www.jetbrains.com/idea) plugin (see [this section](#intellij-idea-plugin) for the installation instructions) or as a console applications (see [this section](#console-application) for the installation instructions).

# IntelliJ IDEA Plugin

To install the IntelliJ IDEA plugin, follow the instructions below.

* Download (either community or ultimate version of) [IntelliJ IDEA](https://www.jetbrains.com/idea).
* Download the [Arend plugin](http://valis.github.io/intellij-vclang.zip). You can also get the latest version of the plugin by following instructions on [this page](https://github.com/JetBrains/intellij-vclang/blob/dev/README.md).
* Run Intellij IDEA, choose either **Configure | Plugins** if you are on a _Welcome screen_ or **File | Settings** from the main menu if a project is open, go to **Plugins** tab, click **Install plugin from disk**, choose downloaded `intellij-vclang.zip` file, restart Intellij IDEA.

Let's create our first Arend project.
Run Intellij IDEA and choose either **Create New Project** if you are on a _Welcome screen_ or **File | New | Project** from the main menu if a project is open.
Choose **Vclang** in the list on the left, click **Next**, click **Finish**.
You should get a new project which contains (among other files) a file `<project_name>.vcl` and an empty directory `src`.
The `vcl` file contains a description of the project.
<!-- You can read more about vcl files in ... TODO -->
Create a new file `example.vc` in `src` directory.
Add the following line to this file:
```arend
\func f => 0
```
Right click `example.vc` file and choose `Run 'Typecheck example'` in the popup menu (you can also use shortcut `<Alt+Shift+F10>`).
You should see the message _All Tests Passed_, which indicates that the typechecking was successful.
Modify the file as follows:
```arend
\func f : Nat -> Nat => 0
```
Run the typechecking again (you can use shortcut `<Shift+F10>` for this).
You should see the following error message:
```bash
[ERROR] example.vc:1:25: Type mismatch
  Expected type: Nat -> Nat
    Actual type: Nat
  In: 0
  While processing: f
```

You can read more about IntelliJ IDEA [here](https://www.jetbrains.com/help/idea/discover-intellij-idea.html).
To learn more about Arend, see the [tutorial](tutorial) and the [language reference](language-reference).

# Console Application

To install the console application, follow the instructions below.

* Download the vclang [jar file](http://valis.github.io/vclang.jar). You can also get the latest version of the plugin by following instructions on [this page](https://github.com/JetBrains/vclang/blob/master/README.md).
* Run `java -jar vclang.jar` to check that everything is alright. You should see the following output:
<pre><code class="bash">$ java -jar vclang.jar
[INFO] Loading library prelude
[INFO] Loaded library prelude
Nothing to load
</code></pre>
To see command line options, run `java -jar vclang.jar --help`.

Let's create our first Arend project.
Create a directory for your project:
```bash
$ mkdir testProject
$ cd testProject
```
Create file `myProject.vcl` inside this directory.
This file contains the description of your project.
Currently, we just need to specify the location of source files of your project.
<!-- You can read more about vcl files in ... TODO -->
Add the following line to `myProject.vcl`:
```bash
sourcesDir: src
```
Create directory `src` which will contain source files for this project.
Create a file `example.vc` inside `src` with the following content:
```arend
\func f => 0
```
Run `java -jar $vclang $myProject`, where `$vclang` is the path to `vclang.jar` and `$myProject` is the path to `myProject.vcl`.
You should see the following output:
```bash
[INFO] Loading library prelude
[INFO] Loaded library prelude
[INFO] Loading library myProject
[INFO] Loaded library myProject
--- Typechecking myProject ---
[ ] example
--- Done ---
```
This means that module `example` was successfully typechecked.
Modify file `example.vc` as follows:
```arend
\func f : Nat -> Nat => 0
```
If you run `java -jar $vclang $myProject` again, it should produce the following error message:
```bash
[INFO] Loading library prelude
[INFO] Loaded library prelude
[INFO] Loading library myProject
[INFO] Loaded library myProject
--- Typechecking myProject ---
[ERROR] example:1:25: Type mismatch
  Expected type: Nat -> Nat
    Actual type: Nat
  In: 0
  While processing: f
[✗] example
Number of modules with errors: 1
--- Done ---
```
