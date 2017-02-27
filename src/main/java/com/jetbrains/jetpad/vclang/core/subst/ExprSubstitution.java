package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;

import java.util.*;

public class ExprSubstitution {
  private Map<Variable, Expression> mySubstExprs;

  public ExprSubstitution() {
    mySubstExprs = Collections.emptyMap();
  }

  public ExprSubstitution(Variable from, Expression to) {
    mySubstExprs = new HashMap<>();
    add(from, to);
  }

  public Set<Variable> getDomain() {
    return mySubstExprs.keySet();
  }

  public Expression get(Variable binding)  {
    return mySubstExprs.get(binding);
  }

  public void clear() {
    mySubstExprs.clear();
  }

  public void add(Variable binding, Expression expression) {
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

  public void subst(ExprSubstitution subst) {
    for (Map.Entry<Variable, Expression> entry : mySubstExprs.entrySet()) {
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
    for (Variable binding : subst.getDomain()) {
      result.add(binding, subst.get(binding).subst(this));
    }
    return result;
  }

  public List<TypedBinding> extendBy(List<Binding> context) {
    List<TypedBinding> result = new ArrayList<>();
    for (Binding binding : context) {
      result.add(new TypedBinding(binding.getName(), binding.getType().subst(this, new LevelSubstitution())));
      add(binding, ExpressionFactory.Reference(result.get(result.size() - 1)));
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