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

  public MergeScope(Collection<Scope> scopes) {
    myScopes = scopes;
  }

  public MergeScope(Scope... scopes) {
    myScopes = Arrays.asList(scopes);
  }

  @NotNull
  @Override
  public List<Referable> getElements() {
    List<Referable> result = new ArrayList<>();
    for (Scope scope : myScopes) {
      result.addAll(scope.getElements());
    }
    return result;
  }

  @Override
  public Referable resolveName(String name) {
    for (Scope scope : myScopes) {
      Referable ref = scope.resolveName(name);
      if (ref != null) {
        return ref;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    for (Scope scope : myScopes) {
      Scope result = scope.resolveNamespace(name, onlyInternal);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (Scope scope : myScopes) {
      Referable ref = scope.find(pred);
      if (ref != null) {
        return ref;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    List<Scope> scopes = new ArrayList<>(myScopes.size());
    for (Scope scope : myScopes) {
      scopes.add(scope.getGlobalSubscopeWithoutOpens());
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
