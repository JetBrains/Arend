package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.TCDefReferable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SimpleInstanceProvider implements InstanceProvider {
  private final List<TCDefReferable> myInstances;

  public SimpleInstanceProvider() {
    myInstances = new ArrayList<>();
  }

  public SimpleInstanceProvider(List<TCDefReferable> instances) {
    myInstances = instances;
  }

  public SimpleInstanceProvider(SimpleInstanceProvider another) {
    myInstances = new ArrayList<>(another.myInstances);
  }

  public void add(int index, TCDefReferable instance) {
    if (index < 0) {
      myInstances.add(instance);
    } else {
      myInstances.add(index, instance);
    }
  }

  public boolean isEmpty() {
    return myInstances.isEmpty();
  }

  public boolean remove(TCDefReferable instance) {
    return myInstances.remove(instance);
  }

  public List<TCDefReferable> getInstances() {
    return myInstances;
  }

  public void reverseFrom(int n) {
    if (myInstances.size() > n + 1) {
      Collections.reverse(myInstances.subList(n, myInstances.size()));
    }
  }

  @Override
  public TCDefReferable findInstance(Predicate<TCDefReferable> pred) {
    for (int i = myInstances.size() - 1; i >= 0; i--) {
      if (pred.test(myInstances.get(i))) {
        return myInstances.get(i);
      }
    }
    return null;
  }
}
