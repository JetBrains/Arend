package org.arend.frontend.repl.action;

import org.arend.ext.module.ModulePath;
import org.arend.frontend.repl.CommonCliRepl;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public final class ListLoadedModulesAction implements CliReplCommand {
  public static final @NotNull ListLoadedModulesAction INSTANCE = new ListLoadedModulesAction();

  private ListLoadedModulesAction() {
  }

  @Override
  public void invoke(@NotNull String line, @NotNull CommonCliRepl api, @NotNull Supplier<@NotNull String> scanner) {
    Node root = new Node("", null);
    api.getReplLibrary().getLoadedModules().forEach(mP -> insert(mP.toArray(), root));

    if (root.children.isEmpty()) api.println("[INFO] No modules loaded."); else print(api, root, "", true, root);
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "List all loaded modules";
  }

  private static void insert(String[] longName, Node root) {
    Node currentNode = root;
    for (String name : longName) {
      currentNode.children.putIfAbsent(name, new Node(name, currentNode));
      currentNode = currentNode.children.get(name);
    }
    currentNode.fileExists = true;
  }

  private static void print(@NotNull CommonCliRepl api, Node node, String prefix, boolean isTail, Node root) {
    String folderName = node.fileExists ? "" : (node == root ? "(root)" : "(not a module)");
    StringBuilder moduleErrors = new StringBuilder();

    if (node.fileExists) {
      Scope scope = api.getAvailableModuleScopeProvider().forModule(new ModulePath(node.getPath(null)));
      int successful = 0;
      List<Referable> failedDefs = new ArrayList<>();

      if (scope != null) {
        for (Referable referable : scope.getElements()) {
          if (referable instanceof TCDefReferable referable1) {
            if (referable1.getTypechecked() == null && referable1.getKind().isTypecheckable())
              failedDefs.add(referable);
            else successful++;
          }
        }
      }

      if (successful == 0) moduleErrors.append(" [Not Loaded]"); else {
        if (!failedDefs.isEmpty()) moduleErrors.append(" Failing defs: ");
        for (Referable failedDef : failedDefs) {
          moduleErrors.append(failedDef.getRefName()).append(" ");
        }
      }
    }

    api.print(prefix + (isTail ? "└── " : "├── ") + node.value + " " + folderName);
    if (!moduleErrors.isEmpty()) api.eprintln(moduleErrors.toString().trim()); else api.println();

    List<Node> children = new ArrayList<>(node.children.values());
    children.sort(Comparator.comparing(o -> o.value));
    for (int i = 0; i < children.size() - 1; i++) {
      print(api, children.get(i), prefix + (isTail ? "    " : "│   "), false, root);
    }

    if (!children.isEmpty()) {
      print(api, children.get(children.size() - 1), prefix + (isTail ? "    " : "│   "), true, root);
    }
  }

  private static class Node {
    String value;
    Boolean fileExists;
    Map<String, Node> children;

    Node myParent;

    Node(String value, Node parent) {
      this.value = value;
      this.children = new LinkedHashMap<>();
      this.fileExists = false;
      this.myParent = parent;
    }

    List<String> getPath(Node root) {
      if (this.myParent == root || this.myParent == null) {
        return new ArrayList<>();
      }
      List<String> result = this.myParent.getPath(root);
      result.add(value);
      return result;
    }
  }

}
