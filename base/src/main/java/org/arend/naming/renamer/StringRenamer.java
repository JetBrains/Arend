package org.arend.naming.renamer;

import org.arend.ext.variable.Variable;
import org.jetbrains.annotations.NotNull;

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
  public @NotNull String generateFreshName(@NotNull Variable var, @NotNull Collection<? extends Variable> variables) {
    String newName = super.generateFreshName(var, variables);
    addNewName(var, newName);
    return newName;
  }
}
