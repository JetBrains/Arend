package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class MergeScope implements Scope {
  private final Collection<Scope> myScopes;
  private final boolean myMergeNamespaces;

  public MergeScope(Collection<Scope> scopes) {
    myScopes = scopes;
    myMergeNamespaces = false;
  }

  public MergeScope(Scope... scopes) {
    myScopes = Arrays.asList(scopes);
    myMergeNamespaces = false;
  }

  public MergeScope(boolean mergeNamespaces, Collection<Scope> scopes) {
    myScopes = scopes;
    myMergeNamespaces = mergeNamespaces;
  }

  public MergeScope(boolean mergeNamespaces, Scope... scopes) {
    myScopes = Arrays.asList(scopes);
    myMergeNamespaces = mergeNamespaces;
  }

  @NotNull
  @Override
  public List<Referable> getElements(@Nullable ScopeContext context) {
    List<Referable> result = new ArrayList<>();
    for (Scope scope : myScopes) {
      result.addAll(scope.getElements(context));
    }
    return result;
  }

  @Override
  public Referable resolveName(@NotNull String name, @Nullable ScopeContext context) {
    for (Scope scope : myScopes) {
      Referable ref = scope.resolveName(name, context);
      if (ref != null) {
        return ref;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name) {
    if (myMergeNamespaces) {
      List<Scope> scopes = new ArrayList<>(myScopes.size());
      for (Scope scope : myScopes) {
        Scope result = scope.resolveNamespace(name);
        if (result != null) {
          scopes.add(result);
        }
      }
      return scopes.isEmpty() ? null : scopes.size() == 1 ? scopes.get(0) : new MergeScope(true, scopes);
    } else {
      for (Scope scope : myScopes) {
        Scope result = scope.resolveNamespace(name);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred, @Nullable ScopeContext context) {
    for (Scope scope : myScopes) {
      Referable ref = scope.find(pred, context);
      if (ref != null) {
        return ref;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    List<Scope> scopes = new ArrayList<>(myScopes.size());
    for (Scope scope : myScopes) {
      scopes.add(scope.getGlobalSubscopeWithoutOpens(withImports));
    }
    return new MergeScope(scopes);
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    for (Scope scope : myScopes) {
      ImportedScope importedScope = scope.getImportedSubscope();
      if (importedScope != null) {
        return importedScope;
      }
    }
    return null;
  }
}
