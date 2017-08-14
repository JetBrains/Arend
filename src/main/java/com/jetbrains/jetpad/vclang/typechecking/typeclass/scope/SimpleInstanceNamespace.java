package com.jetbrains.jetpad.vclang.typechecking.typeclass.scope;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateInstanceError;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

public class SimpleInstanceNamespace implements Scope {
  private final ErrorReporter myErrorReporter;
  private Map<Pair<Abstract.Definition, Abstract.Definition>, Abstract.ClassViewInstance> myInstances = Collections.emptyMap();

  public SimpleInstanceNamespace(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  public void addInstance(Abstract.ClassViewInstance instance) {
    if (myInstances.isEmpty()) {
      myInstances = new HashMap<>();
    }
    Abstract.ClassView classView = (Abstract.ClassView) instance.getClassView().getReferent();
    Pair<Abstract.Definition, Abstract.Definition> pair = new Pair<>(instance.isDefault() ? (Abstract.Definition) classView.getUnderlyingClassReference().getReferent() : classView, instance.getClassifyingDefinition());
    Abstract.ClassViewInstance oldInstance = myInstances.get(pair);
    if (oldInstance != null) {
      myErrorReporter.report(new DuplicateInstanceError(Error.Level.ERROR, oldInstance, instance));
    } else {
      myInstances.put(pair, instance);
    }
  }

  @Override
  public Set<String> getNames() {
    return Collections.emptySet();
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    return null;
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    return myInstances.values();
  }
}
