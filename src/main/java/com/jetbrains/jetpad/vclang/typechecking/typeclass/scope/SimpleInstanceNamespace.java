package com.jetbrains.jetpad.vclang.typechecking.typeclass.scope;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateInstanceError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

public class SimpleInstanceNamespace<T> implements Scope {
  private final ErrorReporter<T> myErrorReporter;
  private Map<Pair<GlobalReferable, GlobalReferable>, Concrete.ClassViewInstance<T>> myInstances = Collections.emptyMap();

  public SimpleInstanceNamespace(ErrorReporter<T> errorReporter) {
    myErrorReporter = errorReporter;
  }

  public void addInstance(Concrete.ClassViewInstance<T> instance) {
    if (myInstances.isEmpty()) {
      myInstances = new HashMap<>();
    }
    Concrete.ClassView classView = (Concrete.ClassView) instance.getClassView().getReferent();
    Pair<GlobalReferable, GlobalReferable> pair = new Pair<>(instance.isDefault() ? (GlobalReferable) classView.getUnderlyingClass().getReferent() : classView, instance.getClassifyingDefinition());
    Concrete.ClassViewInstance oldInstance = myInstances.putIfAbsent(pair, instance);
    if (oldInstance != null) {
      myErrorReporter.report(new DuplicateInstanceError<>(Error.Level.ERROR, oldInstance, instance));
    }
  }

  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public Concrete.Definition resolveName(String name) {
    return null;
  }

  @Override
  public Collection<? extends Concrete.ClassViewInstance> getInstances() {
    return myInstances.values();
  }
}
