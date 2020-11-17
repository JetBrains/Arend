package org.arend.core.subst;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;

import java.util.*;

public class ExprSubstitution {
  private Map<Binding, Expression> mySubstExprs;

  public ExprSubstitution() {
    mySubstExprs = Collections.emptyMap();
  }

  public ExprSubstitution(ExprSubstitution substitution) {
    mySubstExprs = substitution.mySubstExprs.isEmpty() ? Collections.emptyMap() : new HashMap<>(substitution.mySubstExprs);
  }

  public ExprSubstitution(Binding from, Expression to) {
    mySubstExprs = new HashMap<>();
    add(from, to);
  }

  public Set<Binding> getKeys() {
    return mySubstExprs.keySet();
  }

  public Set<Map.Entry<Binding, Expression>> getEntries() {
    return mySubstExprs.entrySet();
  }

  public boolean isEmpty() {
    return mySubstExprs.isEmpty();
  }

  public int size() {
    return mySubstExprs.size();
  }

  public Expression get(Binding binding)  {
    return mySubstExprs.get(binding);
  }

  public void clear() {
    if (!mySubstExprs.isEmpty()) {
      mySubstExprs.clear();
    }
  }

  public void remove(Binding variable) {
    if (!mySubstExprs.isEmpty()) {
      mySubstExprs.remove(variable);
    }
  }

  public void add(Binding binding, Expression expression) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.put(binding, expression);
  }

  public void addIfAbsent(Binding binding, Expression expression) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.putIfAbsent(binding, expression);
  }

  public void addSubst(Binding binding, Expression expression) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    } else {
      for (Map.Entry<Binding, Expression> entry : mySubstExprs.entrySet()) {
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

  public void addSubst(ExprSubstitution subst) {
    if (subst.isEmpty()) {
      return;
    }
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    subst(subst);
    addAll(subst);
  }

  public void subst(ExprSubstitution subst) {
    if (subst.isEmpty()) {
      return;
    }
    for (Map.Entry<Binding, Expression> entry : mySubstExprs.entrySet()) {
      entry.setValue(entry.getValue().subst(subst));
    }
  }

  public void subst(LevelSubstitution subst) {
    if (subst.isEmpty()) {
      return;
    }
    for (Map.Entry<Binding, Expression> entry : mySubstExprs.entrySet()) {
      entry.setValue(entry.getValue().subst(subst));
    }
  }

  public String toString() {
    return mySubstExprs.toString();
  }
}