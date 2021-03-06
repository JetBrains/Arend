package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.TCDefReferable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SimpleInstanceProvider implements InstanceProvider {
  private final List<TCDefReferable> myInstances;

  public SimpleInstanceProvider() {
    myInstances = new ArrayList<>();
  }

  public SimpleInstanceProvider(SimpleInstanceProvider another) {
    myInstances = new ArrayList<>(another.myInstances);
  }

  public void put(TCDefReferable instance) {
    myInstances.add(instance);
  }

  public boolean isEmpty() {
    return myInstances.isEmpty();
  }

  @Override
  public TCDefReferable findInstance(Predicate<TCDefReferable> pred) {
    for (TCDefReferable instance : myInstances) {
      if (pred.test(instance)) {
        return instance;
      }
    }
    return null;
  }
}
