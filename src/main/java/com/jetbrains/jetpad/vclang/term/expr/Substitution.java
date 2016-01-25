package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class Substitution {
  private final Map<Binding, Expression> mySubstExprs;

  public Substitution() {
    mySubstExprs = new HashMap<>();
  }

  public Substitution(Binding from, Expression to) {
    this();
    addMapping(from, to);
  }

  public Set<Binding> getDomain() {
    return mySubstExprs.keySet();
  }

  public Expression get(Binding binding)  {
    return mySubstExprs.get(binding);
  }

  public void addMapping(Binding binding, Expression expression) {
    mySubstExprs.put(binding, expression);
  }

  public List<Expression> substExprs(List<Expression> expressions) {
    List<Expression> result = new ArrayList<>();
    for (Expression expr : expressions) {
      result.add(expr.subst(this));
    }
    return result;
  }

  public Substitution compose(Substitution subst) {
    Substitution result = new Substitution();
    for (Binding binding : subst.getDomain()) {
      result.addMapping(binding, subst.get(binding).subst(this));
    }
    return result;
  }

  public List<Binding> extendBy(List<Binding> context) {
    List<Binding> result = new ArrayList<>();
    for (Binding binding : context) {
      result.add(new TypedBinding(binding.getName(), binding.getType().subst(this)));
      addMapping(binding, Reference(result.get(result.size() - 1)));
    }
    return result;
  }

  public static Substitution getIdentity(List<Binding> bindings) {
    Substitution result = new Substitution();
    for (Binding binding : bindings) {
      result.addMapping(binding, new ReferenceExpression(binding));
    }
    return result;
  }
}