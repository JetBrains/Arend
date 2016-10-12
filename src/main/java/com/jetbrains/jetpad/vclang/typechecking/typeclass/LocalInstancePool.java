package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ClassViewCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;

public class LocalInstancePool implements ClassViewInstancePool {
  static private class Pair {
    Expression key;
    ClassView classView;
    Expression value;

    public Pair(Expression key, ClassView classView, Expression value) {
      this.key = key;
      this.classView = classView;
      this.value = value;
    }
  }

  private final List<Pair> myPool = new ArrayList<>();

  @Override
  public Expression getInstance(Expression classifyingExpression, ClassView classView) {
    Expression expr = classifyingExpression.normalize(NormalizeVisitor.Mode.NF);
    Expression result = null;
    for (Pair pair : myPool) {
      if (pair.key.equals(expr) && (classView == null || pair.classView == classView)) {
        if (classView != null) {
          return pair.value;
        } else {
          if (result != null) {
            return null;
          } else {
            result = pair.value;
          }
        }
      }
    }
    return result;
  }

  private boolean addInstance(Expression classifyingExpression, ClassView classView, Expression instance) {
    if (getInstance(classifyingExpression, classView) != null) {
      return false;
    } else {
      myPool.add(new Pair(classifyingExpression, classView, instance));
      return true;
    }
  }

  public boolean addInstance(Binding binding, Expression type) {
    if (type instanceof ClassViewCallExpression) {
      ClassView classView = ((ClassViewCallExpression) type).getClassView();
      if (classView.getClassifyingField() != null) {
        ReferenceExpression reference = new ReferenceExpression(binding);
        return addInstance(FieldCall(((ClassViewCallExpression) type).getClassView().getClassifyingField(), reference).normalize(NormalizeVisitor.Mode.NF), classView, reference);
      }
    }
    return true;
  }
}
