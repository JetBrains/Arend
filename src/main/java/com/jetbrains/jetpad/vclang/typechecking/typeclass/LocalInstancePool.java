package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ClassViewCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;

public class LocalInstancePool implements ClassViewInstancePool {
  static private class Pair {
    Expression key;
    Abstract.ClassView classView;
    Expression value;

    public Pair(Expression key, Abstract.ClassView classView, Expression value) {
      this.key = key;
      this.classView = classView;
      this.value = value;
    }
  }

  private final List<Pair> myPool = new ArrayList<>();

  @Override
  public Expression getInstance(Expression classifyingExpression, Abstract.ClassView classView) {
    Expression expr = classifyingExpression.normalize(NormalizeVisitor.Mode.NF);
    for (Pair pair : myPool) {
      if (pair.key.equals(expr) && (classView == null || pair.classView == classView)) {
        return pair.value;
      }
    }
    return null;
  }

  public boolean addInstance(Expression classifyingExpression, Abstract.ClassView classView, Expression instance) {
    if (getInstance(classifyingExpression, classView) != null) {
      return false;
    } else {
      myPool.add(new Pair(classifyingExpression, classView, instance));
      return true;
    }
  }
}
