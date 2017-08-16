package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class InferenceReferenceExpression extends Expression {
  private final InferenceVariable myVar;
  private Expression mySubstExpression;

  public InferenceReferenceExpression(InferenceVariable binding, Equations equations) {
    myVar = binding;
    myVar.setReference(this);

    Expression type = myVar.getType().normalize(NormalizeVisitor.Mode.WHNF);
    if (type.isInstance(ClassCallExpression.class)) {
      ClassCallExpression classCall = type.cast(ClassCallExpression.class);
      if (!classCall.getDefinition().getFields().isEmpty()) {
        for (ClassField field : classCall.getDefinition().getFields()) {
          Expression impl = classCall.getImplementation(field, this);
          if (impl != null) {
            equations.add(new FieldCallExpression(field, this), impl, Equations.CMP.EQ, myVar.getSourceNode(), myVar);
          }
        }
        type = new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument());
      }
    }
    myVar.setType(type);
  }

  public InferenceReferenceExpression(InferenceVariable binding, Expression substExpression) {
    myVar = binding;
    mySubstExpression = substExpression;
  }

  public InferenceVariable getVariable() {
    return mySubstExpression == null ? myVar : null;
  }

  public Expression getSubstExpression() {
    return mySubstExpression;
  }

  public void setSubstExpression(Expression substExpression) {
    mySubstExpression = substExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInferenceReference(this, params);
  }

  @Override
  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : mySubstExpression.cast(clazz);
  }

  @Override
  public <T extends Expression> boolean isInstance(Class<T> clazz) {
    return mySubstExpression != null && mySubstExpression.isInstance(clazz) || clazz.isInstance(this);
  }

  @Override
  public boolean isWHNF() {
    return mySubstExpression == null;
  }

  @Override
  public Expression getStuckExpression() {
    return mySubstExpression != null ? mySubstExpression.getStuckExpression() : this;
  }
}
