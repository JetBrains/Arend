package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class ExprSubstitution {
  private Map<Binding, Expression> mySubstExprs;

  public ExprSubstitution() {
    mySubstExprs = Collections.emptyMap();
  }

  public ExprSubstitution(Binding from, Expression to) {
    mySubstExprs = new HashMap<>();
    add(from, to);
  }

  public Set<Binding> getDomain() {
    return mySubstExprs.keySet();
  }

  public Expression get(Binding binding)  {
    return mySubstExprs.get(binding);
  }

  public void add(Binding binding, Expression expression) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.put(binding, expression);
  }

  public void add(ExprSubstitution substitution) {
    if (!substitution.mySubstExprs.isEmpty()) {
      if (mySubstExprs.isEmpty()) {
        mySubstExprs = new HashMap<>();
      }
      mySubstExprs.putAll(substitution.mySubstExprs);
    }
  }

  public void subst(Substitution subst) {
    for (Map.Entry<Binding, Expression> entry : mySubstExprs.entrySet()) {
      entry.setValue(entry.getValue().subst(subst));
    }
  }

  public List<Expression> substExprs(List<Expression> expressions) {
    List<Expression> result = new ArrayList<>();
    for (Expression expr : expressions) {
      result.add(expr.subst(this));
    }
    return result;
  }

  public ExprSubstitution compose(ExprSubstitution subst) {
    ExprSubstitution result = new ExprSubstitution();
    for (Binding binding : subst.getDomain()) {
      result.add(binding, subst.get(binding).subst(this));
    }
    return result;
  }

  public List<Binding> extendBy(List<Binding> context) {
    List<Binding> result = new ArrayList<>();
    for (Binding binding : context) {
      result.add(new TypedBinding(binding.getName(), binding.getType().subst(this)));
      add(binding, Reference(result.get(result.size() - 1)));
    }
    return result;
  }

  public static ExprSubstitution getIdentity(List<Binding> bindings) {
    ExprSubstitution result = new ExprSubstitution();
    for (Binding binding : bindings) {
      result.add(binding, new ReferenceExpression(binding));
    }
    return result;
  }

  public String toString() {
    return mySubstExprs.toString();
  }
}