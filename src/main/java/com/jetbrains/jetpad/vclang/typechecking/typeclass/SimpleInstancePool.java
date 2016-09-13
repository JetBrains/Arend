package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;

public class SimpleInstancePool implements ClassViewInstancePool {
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
  public Expression getLocalInstance(Expression classifyingExpression) {
    Expression expr = classifyingExpression.normalize(NormalizeVisitor.Mode.NF);
    for (Pair pair : myPool) {
      if (pair.key.equals(expr)) {
        return pair.value;
      }
    }
    return null;
  }

  private boolean addLocalInstance(Expression classifyingExpression, Expression instance) {
    if (getLocalInstance(classifyingExpression) != null) {
      return false;
    } else {
      myPool.add(new Pair(classifyingExpression, instance));
      return true;
    }
  }

  public boolean addLocalInstance(Binding binding, Expression type) {
    ClassCallExpression classCall = type.toClassCall();
    if (classCall != null && classCall.getClassView().getClassifyingField() != null) {
      ReferenceExpression reference = new ReferenceExpression(binding);
      return addLocalInstance(Apps(new FieldCallExpression(classCall.getClassView().getClassifyingField()), reference).normalize(NormalizeVisitor.Mode.NF), reference);
    } else {
      return true;
    }
  }
}
