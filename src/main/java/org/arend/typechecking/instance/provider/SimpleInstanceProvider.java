package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
  public Concrete.FunctionDefinition findInstance(ClassReferable classRef, Predicate<Concrete.FunctionDefinition> pred) {
    for (Concrete.FunctionDefinition instance : myInstances) {
      Referable ref = instance.getReferenceInType();
      if (ref instanceof ClassReferable && ((ClassReferable) ref).isSubClassOf(classRef) && pred.test(instance)) {
        return instance;
      }
    }
    return null;
  }
}
