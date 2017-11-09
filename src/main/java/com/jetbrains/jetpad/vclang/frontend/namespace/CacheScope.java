package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;

public class CacheScope {
  public final @Nonnull CachedSubScope root = new CachedSubScope();

  public GlobalReferable ensureReferable(List<String> path, Precedence precedence, boolean addToGrandparentScope) {
    if (path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be empty");
    }

    CachedSubScope scope = ensureScope(path);
    if (scope.here != null) {
      scope.here = new CachedDefinitionStub(precedence, path.get(path.size() - 1));
    }

    if (addToGrandparentScope) {
      String name = path.get(path.size() - 1);
      CachedSubScope grandparentScope = ensureScope(path.subList(0, path.size() - 2));
      ensureSubScope(grandparentScope, singletonList(name)).here = scope.here;
    }

    return scope.here;
  }

  public GlobalReferable ensureReferable(List<String> path, Precedence precedence) {
    return ensureReferable(path, precedence, false);
  }

  private CachedSubScope ensureSubScope(CachedSubScope parent, List<String> ns) {
    for (String n : ns) {
      parent = parent.sub.computeIfAbsent(n, k -> new CachedSubScope());
    }
    return parent;
  }

  private CachedSubScope ensureScope(List<String> ns) {
    return ensureSubScope(root, ns);
  }

  private final class CachedSubScope implements Scope {
    public final Map<String, CachedSubScope> sub = new HashMap<>();
    public GlobalReferable here = null;

    @Nullable
    @Override
    public Referable find(Predicate<Referable> pred) {
      return sub.values().stream().map(ns -> ns.here).filter(r -> r != null && pred.test(r)).findFirst().orElse(null);
    }

    @Nullable
    @Override
    public Scope resolveNamespace(String name, boolean resolveModules) {
      return sub.get(name);
    }
  }

  private class CachedDefinitionStub implements GlobalReferable {
    private final Precedence myPrecedence;
    private final String myName;

    private CachedDefinitionStub(Precedence precedence, String name) {
      myPrecedence = precedence;
      myName = name;
    }

    @Nonnull
    @Override
    public Precedence getPrecedence() {
      return myPrecedence;
    }

    @Nonnull
    @Override
    public String textRepresentation() {
      return myName;
    }
  }
}
