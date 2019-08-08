package org.arend.naming.renamer;

import org.arend.core.context.binding.Variable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StringRenamer extends Renamer {
  private final Map<Variable, String> myMap = new HashMap<>();

  public void addNewName(Variable variable, String newName) {
    myMap.put(variable, newName);
  }

  @Override
  public String getNewName(Variable variable) {
    String name = myMap.get(variable);
    return name == null ? variable.getName() : name;
  }

  @Override
  public String generateFreshName(Variable var, Collection<? extends Variable> variables) {
    String newName = super.generateFreshName(var, variables);
    addNewName(var, newName);
    return newName;
  }
}
