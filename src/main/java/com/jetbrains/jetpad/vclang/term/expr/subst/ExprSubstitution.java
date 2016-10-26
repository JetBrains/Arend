package com.jetbrains.jetpad.vclang.term.expr.subst;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class ExprSubstitution {
  private Map<Referable, Expression> mySubstExprs;

  public ExprSubstitution() {
    mySubstExprs = Collections.emptyMap();
  }

  public ExprSubstitution(Referable from, Expression to) {
    mySubstExprs = new HashMap<>();
    add(from, to);
  }

  public Set<Referable> getDomain() {
    return mySubstExprs.keySet();
  }

  public Expression get(Referable binding)  {
    return mySubstExprs.get(binding);
  }

  public void clear() {
    mySubstExprs.clear();
  }

  public void add(Referable binding, Expression expression) {
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
    for (Map.Entry<Referable, Expression> entry : mySubstExprs.entrySet()) {
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
    for (Referable binding : subst.getDomain()) {
      result.add(binding, subst.get(binding).subst(this));
    }
    return result;
  }

  public List<TypedBinding> extendBy(List<Binding> context) {
    List<TypedBinding> result = new ArrayList<>();
    for (Binding binding : context) {
      result.add(new TypedBinding(binding.getName(), binding.getType().subst(this, new LevelSubstitution())));
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