package com.jetbrains.jetpad.vclang.typechecking.typeclass.scope;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

// TODO[abstract]: Maybe delete this, replace with ordinary scopes
public class SimpleInstanceNamespace<T> implements Scope {
  private final ErrorReporter<T> myErrorReporter;
  private Map<Pair<GlobalReferable, GlobalReferable>, Concrete.Instance<T>> myInstances = Collections.emptyMap();

  public SimpleInstanceNamespace(ErrorReporter<T> errorReporter) {
    myErrorReporter = errorReporter;
  }

  public void addInstance(Concrete.Instance<T> instance) {
    /*
    if (myInstances.isEmpty()) {
      myInstances = new HashMap<>();
    }
    Referable classView = instance.getClassView().getReferent();
    Pair<GlobalReferable, GlobalReferable> pair = new Pair<>(instance.isDefault() ? (GlobalReferable) classView.getUnderlyingClass().getReferent() : classView, instance.getClassifyingDefinition());
    Concrete.Instance oldInstance = myInstances.putIfAbsent(pair, instance);
    if (oldInstance != null) {
      myErrorReporter.report(new DuplicateInstanceError<>(Error.Level.ERROR, oldInstance, instance));
    }
    */
  }

  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public GlobalReferable resolveName(String name) {
    return null;
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances() {
    return myInstances.values();
  }
}
