package org.arend.typechecking.instance.provider;

import org.arend.term.concrete.Concrete;

import java.util.*;

public class SimpleInstanceProvider implements InstanceProvider {
  private List<Concrete.Instance> myInstances;

  public SimpleInstanceProvider() {
    myInstances = new ArrayList<>();
  }

  public SimpleInstanceProvider(SimpleInstanceProvider another) {
    myInstances = new ArrayList<>(another.myInstances);
  }

  public void put(Concrete.Instance instance) {
    myInstances.add(instance);
  }

  @Override
  public List<? extends Concrete.Instance> getInstances() {
    return myInstances;
  }
}
