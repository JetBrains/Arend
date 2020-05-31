package org.arend.typechecking.instance.provider;

import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SimpleInstanceProvider implements InstanceProvider {
  private final List<Concrete.FunctionDefinition> myInstances;

  public SimpleInstanceProvider() {
    myInstances = new ArrayList<>();
  }

  public SimpleInstanceProvider(SimpleInstanceProvider another) {
    myInstances = new ArrayList<>(another.myInstances);
  }

  public void put(Concrete.FunctionDefinition instance) {
    myInstances.add(instance);
  }

  @Override
  public Concrete.FunctionDefinition findInstance(Predicate<Concrete.FunctionDefinition> pred) {
    for (Concrete.FunctionDefinition instance : myInstances) {
      if (pred.test(instance)) {
        return instance;
      }
    }
    return null;
  }
}
