package org.arend.naming.renamer;

import org.arend.core.context.binding.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Renamer {
  private String myUnnamed = "unnamed";
  private int myBase = 1;

  public void setUnnamed(String unnamed) {
    myUnnamed = unnamed;
  }

  public void setBase(int base) {
    myBase = base;
  }

  public String getNewName(Variable variable) {
    return variable.getName();
  }

  public void generateFreshNames(Collection<? extends Variable> variables) {
    for (Variable variable : variables) {
      generateFreshName(variable, variables);
    }
  }

  public String generateFreshName(Variable var, Collection<? extends Variable> variables) {
    String name = var.getName();
    if (name == null) {
      if (myUnnamed == null) {
        return null;
      }
      name = myUnnamed;
    }

    String prefix = null;
    Set<Integer> indices = Collections.emptySet();
    for (Variable variable : variables) {
      if (variable != var) {
        String otherName = getNewName(variable);
        if (otherName != null) {
          if (prefix == null) {
            prefix = getPrefix(name);
          }
          if (prefix.equals(getPrefix(otherName))) {
            if (indices.isEmpty()) {
              indices = new HashSet<>();
            }
            indices.add(getSuffix(otherName));
          }
        }
      }
    }

    if (!indices.isEmpty()) {
      int suffix = getSuffix(name);
      if (indices.contains(suffix)) {
        suffix = myBase;
        while (indices.contains(suffix)) {
          suffix++;
        }
        name = prefix + suffix;
      }
    }

    return name;
  }

  private static String getPrefix(String name) {
    int i = name.length() - 1;
    while (Character.isDigit(name.charAt(i))) {
      i--;
    }
    return name.substring(0, i + 1);
  }

  private static int getSuffix(String name) {
    int i = name.length() - 1;
    while (Character.isDigit(name.charAt(i))) {
      i--;
    }
    return i + 1 == name.length() ? -1 : Integer.parseInt(name.substring(i + 1));
  }
}
