package org.arend.naming.scope;

import org.arend.ext.module.ModulePath;
import org.arend.module.ModuleLocation;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class ImportedScope implements Scope {
  private final Tree myExpectedNamesTree;
  private final ModuleScopeProvider myProvider;
  private final Scope myElementsScope;

  public ImportedScope(@NotNull Group group, ModuleScopeProvider provider) {
    myExpectedNamesTree = new Tree();
    myProvider = provider;
    myElementsScope = null;

    ModuleLocation location = group.getReferable().getLocation();
    if (location != null) {
      myExpectedNamesTree.addPath(location.getModulePath().toList());
    }

    for (Statement statement : group.getStatements()) {
      NamespaceCommand cmd = statement.getNamespaceCommand();
      if (cmd != null && cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        myExpectedNamesTree.addPath(cmd.getPath());
      }
    }
  }

  public ImportedScope(ImportedScope importedScope, @NotNull Scope elementsScope) {
    myExpectedNamesTree = importedScope.myExpectedNamesTree;
    myProvider = importedScope.myProvider;
    myElementsScope = elementsScope;
  }

  private ImportedScope(Tree tree, ModuleScopeProvider provider, Scope elementsScope) {
    myExpectedNamesTree = tree;
    myProvider = provider;
    myElementsScope = elementsScope;
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getElements(Referable.RefKind kind) {
    if (myElementsScope != null) {
      return myElementsScope.getElements(kind);
    }

    List<Referable> result = new ArrayList<>();
    for (Triple triple : myExpectedNamesTree.map.values()) {
      if (triple.scope != null) {
        result.add(triple.referable);
      }
    }
    return result;
  }

  @Override
  public @NotNull Collection<? extends Referable> getAllElements() {
    return myElementsScope != null ? myElementsScope.getAllElements() : Scope.super.getAllElements();
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    for (Triple triple : myExpectedNamesTree.map.values()) {
      if (triple.scope != null && pred.test(triple.referable)) {
        return triple.referable;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, @Nullable Referable.RefKind kind) {
    Triple triple = myExpectedNamesTree.map.get(name);
    return triple == null || triple.scope == null ? null : triple.referable;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    Triple triple = myExpectedNamesTree.map.get(name);
    if (triple == null) {
      return null;
    }

    Scope scope2 = new ImportedScope(triple.tree, myProvider, myElementsScope == null ? null : myElementsScope.resolveNamespace(name, true));
    return triple.modulePath == null || triple.scope == null ? scope2 : new MergeScope(triple.scope, scope2);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(List<? extends String> path) {
    if (path.isEmpty()) {
      return EmptyScope.INSTANCE;
    }

    ImportedScope scope = this;
    for (int i = 0; i < path.size() - 1; i++) {
      Triple triple = scope.myExpectedNamesTree.map.get(path.get(i));
      if (triple == null) {
        return null;
      }
      scope = new ImportedScope(triple.tree, myProvider, null);
    }

    Triple triple = scope.myExpectedNamesTree.map.get(path.get(path.size() - 1));
    return triple != null && triple.modulePath != null ? triple.scope : null;
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return this;
  }

  private static class Triple {
    ModuleReferable referable;
    ModulePath modulePath;
    Tree tree;
    Scope scope;

    Triple(ModuleReferable referable, ModulePath modulePath, Tree tree, Scope scope) {
      this.referable = referable;
      this.modulePath = modulePath;
      this.tree = tree;
      this.scope = scope;
    }
  }

  private class Tree {
    final Map<String, Triple> map = new LinkedHashMap<>();

    void addPath(List<String> path) {
      if (path.isEmpty()) {
        return;
      }

      Tree tree = this;
      for (int i = 0; i < path.size(); i++) {
        final int finalI = i + 1;
        Triple triple = tree.map.compute(path.get(i), (k,oldTriple) -> {
          ModulePath modulePath = new ModulePath(path.subList(0, finalI));
          if (oldTriple == null) {
            return new Triple(new ModuleReferable(modulePath), finalI == path.size() ? modulePath : null, new Tree(), finalI == path.size() ? myProvider.forModule(modulePath) : EmptyScope.INSTANCE);
          }
          if (oldTriple.modulePath != null || finalI != path.size()) {
            return oldTriple;
          }
          return new Triple(oldTriple.referable, modulePath, oldTriple.tree, myProvider.forModule(modulePath));
        });
        if (triple.modulePath == null && finalI == path.size()) {
          triple.modulePath = triple.referable.path;
        }
        tree = triple.tree;
      }
    }
  }
}
