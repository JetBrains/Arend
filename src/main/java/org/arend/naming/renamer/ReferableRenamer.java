package org.arend.naming.renamer;

import org.arend.core.context.binding.Variable;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.arend.term.concrete.ConcreteExpressionFactory.ref;

public class ReferableRenamer extends Renamer {
  private final Map<Variable, LocalReferable> myMap = new HashMap<>();

  public void addNewName(Variable variable, LocalReferable referable) {
    myMap.put(variable, referable);
  }

  @Override
  public String getNewName(Variable variable) {
    Referable ref = myMap.get(variable);
    return ref == null ? variable.getName() : ref.textRepresentation();
  }

  @Override
  public String generateFreshName(Variable var, Collection<? extends Variable> variables) {
    String newName = super.generateFreshName(var, variables);
    addNewName(var, ref(newName));
    return newName;
  }

  public LocalReferable getNewReferable(Variable variable) {
    return myMap.get(variable);
  }

  public LocalReferable generateFreshReferable(Variable var, Collection<? extends Variable> variables) {
    String newName = super.generateFreshName(var, variables);
    LocalReferable referable = ref(newName);
    addNewName(var, referable);
    return referable;
  }
}
