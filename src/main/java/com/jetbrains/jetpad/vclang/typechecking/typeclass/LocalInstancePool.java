package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ClassViewCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements ClassViewInstancePool {
  static private class Pair {
    Expression key;
    Expression value;

    public Pair(Expression key, Expression value) {
      this.key = key;
      this.value = value;
    }
  }

  private final List<Pair> myPool = new ArrayList<>();

  @Override
  public Expression getInstance(Expression classifyingExpression) {
    Expression expr = classifyingExpression.normalize(NormalizeVisitor.Mode.NF);
    for (Pair pair : myPool) {
      if (pair.key.equals(expr)) {
        return pair.value;
      }
    }
    return null;
  }

  private boolean addInstance(Expression classifyingExpression, Expression instance) {
    if (getInstance(classifyingExpression) != null) {
      return false;
    } else {
      myPool.add(new Pair(classifyingExpression, instance));
      return true;
    }
  }

  public boolean addInstance(Binding binding, Expression type) {
    if (type instanceof ClassViewCallExpression && ((ClassViewCallExpression) type).getClassView().getClassifyingField() != null) {
      ReferenceExpression reference = new ReferenceExpression(binding);
      return addInstance(new FieldCallExpression(((ClassViewCallExpression) type).getClassView().getClassifyingField(), reference).normalize(NormalizeVisitor.Mode.NF), reference);
    } else {
      return true;
    }
  }
}
