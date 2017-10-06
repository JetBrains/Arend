package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.NamespaceDuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;

import java.util.*;

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

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    Set<Concrete.Instance> result = new LinkedHashSet<>();
    for (Scope scope : myScopes) {
      result.addAll(scope.getInstances());
    }
    return result;
  }
}
