package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SimpleInstanceProvider implements InstanceProvider {
  private final List<TCReferable> myInstances;

  public SimpleInstanceProvider() {
    myInstances = new ArrayList<>();
  }

  public SimpleInstanceProvider(SimpleInstanceProvider another) {
    myInstances = new ArrayList<>(another.myInstances);
  }

  public void put(TCReferable instance) {
    myInstances.add(instance);
  }

  @Override
  public TCReferable findInstance(Predicate<TCReferable> pred) {
    for (TCReferable instance : myInstances) {
      if (pred.test(instance)) {
        return instance;
      }
    }
    return null;
  }
}
