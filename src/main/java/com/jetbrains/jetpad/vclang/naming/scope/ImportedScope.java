package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.group.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class ImportedScope implements Scope {
  private final Tree myExpectedNamesTree;
  private final ModuleScopeProvider myProvider;
  private final Scope myElemntsScope;

  public ImportedScope(@Nonnull Group group, ModuleScopeProvider provider, @Nullable Scope elementsScope) {
    myExpectedNamesTree = new Tree();
    myProvider = provider;
    myElemntsScope = elementsScope;

    for (NamespaceCommand command : group.getNamespaceCommands()) {
      if (command.getKind() == NamespaceCommand.Kind.IMPORT) {
        myExpectedNamesTree.addPath(command.getImportedPath());
      }
    }
  }

  private ImportedScope(Tree tree, ModuleScopeProvider provider, Scope elementsScope) {
    myExpectedNamesTree = tree;
    myProvider = provider;
    myElemntsScope = elementsScope;
  }

  @Nonnull
  @Override
  public Collection<? extends Referable> getElements() {
    if (myElemntsScope != null) {
      return myElemntsScope.getElements();
    }

    List<Referable> result = new ArrayList<>();
    for (Triple triple : myExpectedNamesTree.map.values()) {
      result.add(triple.referable);
    }
    return result;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    for (Triple triple : myExpectedNamesTree.map.values()) {
      if (pred.test(triple.referable)) {
        return triple.referable;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    Triple triple = myExpectedNamesTree.map.get(name);
    return triple == null ? null : triple.referable;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean resolveModuleNames) {
    if (!resolveModuleNames) {
      return null;
    }

    Triple triple = myExpectedNamesTree.map.get(name);
    if (triple == null) {
      return null;
    }

    Scope scope2 = new ImportedScope(triple.tree, myProvider, myElemntsScope == null ? null : myElemntsScope.resolveNamespace(name, true));
    if (triple.modulePath == null) {
      return scope2;
    }

    Scope scope1 = myProvider.forModule(triple.modulePath);
    return scope1 == null ? scope2 : new MergeScope(scope1, scope2);
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return this;
  }

  private static class Triple {
    Referable referable;
    ModulePath modulePath;
    Tree tree;

    Triple(Referable referable, ModulePath modulePath, Tree tree) {
      this.referable = referable;
      this.modulePath = modulePath;
      this.tree = tree;
    }
  }

  private static class Tree {
    final Map<String, Triple> map = new LinkedHashMap<>();

    void addPath(Collection<? extends ModuleReferable> path) {
      if (path.isEmpty()) {
        return;
      }

      Tree tree = this;
      for (Iterator<? extends ModuleReferable> it = path.iterator(); it.hasNext(); ) {
        ModuleReferable key = it.next();
        tree = tree.map.computeIfAbsent(key.path.getLastName(), k -> new Triple(key, it.hasNext() ? null : key.path, new Tree())).tree;
      }
    }
  }
}
