package org.arend.typechecking.instance.provider;

import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.List;

public class SimpleInstanceProvider implements InstanceProvider {
  private List<Concrete.FunctionDefinition> myInstances;

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
  public List<? extends Concrete.FunctionDefinition> getInstances() {
    return myInstances;
  }
}
