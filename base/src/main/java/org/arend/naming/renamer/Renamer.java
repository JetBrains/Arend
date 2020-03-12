package org.arend.naming.renamer;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.Variable;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.AppExpression;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.sort.Sort;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Renamer {
  public final static String UNNAMED = "_x";
  private String myUnnamed = UNNAMED;
  private int myBase = 1;
  private boolean myForceTypeSCName = false;

  public static String getValidName(String name, String unnamed) {
    return name == null || name.isEmpty() || name.equals("_") ? unnamed : name;
  }

  public String getValidName(Variable var) {
    String name = var.getName();
    Expression typeExpr = null;
    if (var instanceof Binding)
      typeExpr = ((Binding) var).getTypeExpr();
    else if (var instanceof ClassField)
      typeExpr = ((ClassField) var).getType(Sort.STD).getCodomain();

    if (name != null && !name.isEmpty() && !name.equals("_") && (typeExpr == null || !myForceTypeSCName)) {
      return name;
    }

    if (typeExpr != null) {
      Character c = getTypeStartingCharacter(typeExpr);
      if (c != null) {
        return c.toString();
      }
    }

    return myUnnamed;
  }

  public void setUnnamed(String unnamed) {
    myUnnamed = unnamed;
  }

  public static String getNameFromType(Expression type, String def) {
    Character c = getTypeStartingCharacter(type);
    return c != null ? c.toString() : getValidName(def, UNNAMED);
  }

  public static Character getTypeStartingCharacter(Expression type) {
    if (type == null) {
      return null;
    }

    String name;
    type = type.getUnderlyingExpression();
    while (type instanceof AppExpression) {
      type = type.getFunction().getUnderlyingExpression();
    }
    if (type instanceof DefCallExpression) {
      name = ((DefCallExpression) type).getDefinition().getName();
    } else if (type instanceof ReferenceExpression) {
      name = ((ReferenceExpression) type).getBinding().getName();
    } else {
      return null;
    }

    return name.isEmpty() ? null : Character.toLowerCase(name.charAt(0));
  }

  public void setBase(int base) {
    myBase = base;
  }

  public String getNewName(Variable variable) {
    return variable.getName();
  }

  public void generateFreshNames(Collection<? extends Variable> variables) {
    for (Variable variable : variables) {
      if (variable instanceof Binding) {
        generateFreshName(variable, variables);
      }
    }
  }

  public String generateFreshName(Variable var, Collection<? extends Variable> variables) {
    String name = getValidName(var);
    if (name == null) {
      return null;
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

  public void setForceTypeSCName(boolean value) { myForceTypeSCName = value; }

  public boolean getForceTypeSCName() { return myForceTypeSCName; }

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
