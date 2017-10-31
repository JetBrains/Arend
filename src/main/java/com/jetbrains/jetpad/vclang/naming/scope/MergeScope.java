package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.NamespaceDuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class MergeScope implements Scope {
  private final Collection<Scope> myScopes;

  public MergeScope(Collection<Scope> scopes) {
    myScopes = scopes;
  }

  public MergeScope(Scope... scopes) {
    myScopes = Arrays.asList(scopes);
  }

  public void addScope(Scope scope, ErrorReporter errorReporter, GlobalReferable groupRef) {
    for (Referable element : scope.getElements()) {
      String name = element.textRepresentation();
      Referable ref1 = resolveName(name);
      if (ref1 != null) {
        Referable ref2 = scope.resolveName(name);
        if (ref2 != null && ref1 != ref2) {
          errorReporter.report(new ProxyError(groupRef, new NamespaceDuplicateNameError(Error.Level.WARNING, ref2, ref1)));
        }
      }
    }
    myScopes.add(scope);
  }

  @Nonnull
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
  public Scope resolveNamespace(String name, boolean resolveModuleNames) {
    for (Scope scope : myScopes) {
      Scope result = scope.resolveNamespace(name, resolveModuleNames);
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
