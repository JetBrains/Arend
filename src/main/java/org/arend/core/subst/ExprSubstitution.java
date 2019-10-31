package org.arend.core.subst;

import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;

import java.util.*;

public class ExprSubstitution {
  private Map<Variable, Expression> mySubstExprs;

  public ExprSubstitution() {
    mySubstExprs = Collections.emptyMap();
  }

  public ExprSubstitution(ExprSubstitution substitution) {
    mySubstExprs = substitution.mySubstExprs.isEmpty() ? Collections.emptyMap() : new HashMap<>(substitution.mySubstExprs);
  }

  public ExprSubstitution(Variable from, Expression to) {
    mySubstExprs = new HashMap<>();
    add(from, to);
  }

  public Set<Variable> getKeys() {
    return mySubstExprs.keySet();
  }

  public Set<Map.Entry<Variable, Expression>> getEntries() {
    return mySubstExprs.entrySet();
  }

  public boolean isEmpty() {
    return mySubstExprs.isEmpty();
  }

  public Expression get(Variable binding)  {
    return mySubstExprs.get(binding);
  }

  public void clear() {
    if (!mySubstExprs.isEmpty()) {
      mySubstExprs.clear();
    }
  }

  public void remove(Variable variable) {
    if (!mySubstExprs.isEmpty()) {
      mySubstExprs.remove(variable);
    }
  }

  public void add(Variable binding, Expression expression) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.put(binding, expression);
  }

  public void addSubst(Variable binding, Expression expression) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    } else {
      for (Map.Entry<Variable, Expression> entry : mySubstExprs.entrySet()) {
        entry.setValue(entry.getValue().subst(binding, expression));
      }
    }
    mySubstExprs.put(binding, expression);
  }

  public void addAll(ExprSubstitution substitution) {
    if (!substitution.mySubstExprs.isEmpty()) {
      if (mySubstExprs.isEmpty()) {
        mySubstExprs = new HashMap<>();
      }
      mySubstExprs.putAll(substitution.mySubstExprs);
    }
  }

  public ExprSubstitution add(DependentLink link, List<? extends Expression> args) {
    if (!args.isEmpty() && mySubstExprs.isEmpty() && link.hasNext()) {
      mySubstExprs = new HashMap<>();
    }
    for (Expression arg : args) {
      if (!link.hasNext()) {
        break;
      }
      mySubstExprs.put(link, arg);
      link = link.getNext();
    }
    return this;
  }

  public void subst(ExprSubstitution subst) {
    for (Map.Entry<Variable, Expression> entry : mySubstExprs.entrySet()) {
      entry.setValue(entry.getValue().subst(subst));
    }
  }

  public String toString() {
    return mySubstExprs.toString();
  }
}