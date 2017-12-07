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
    binding.setReference(this);

    Expression type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF);
    if (type.isInstance(ClassCallExpression.class)) {
      ClassCallExpression classCall = type.cast(ClassCallExpression.class);
      if (!classCall.getDefinition().getFields().isEmpty()) {
        for (ClassField field : classCall.getDefinition().getFields()) {
          Expression impl = classCall.getImplementation(field, this);
          if (impl != null) {
            equations.add(FieldCallExpression.make(field, this), impl, Equations.CMP.EQ, binding.getSourceNode(), binding);
          }
        }
        type = new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument());
      }
    }
    binding.setType(type);
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
  public <E extends Expression> E cast(Class<E> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : mySubstExpression.cast(clazz);
  }

  @Override
  public boolean isInstance(Class clazz) {
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
