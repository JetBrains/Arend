package org.arend.core.expr;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.typechecking.implicitargs.equations.Equations;

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
          if (!field.isProperty()) {
            Expression impl = classCall.getImplementation(field, this);
            if (impl != null) {
              equations.addEquation(FieldCallExpression.make(field, classCall.getSortArgument(), this), impl.normalize(NormalizeVisitor.Mode.WHNF), Equations.CMP.EQ, binding.getSourceNode(), binding, impl.getStuckInferenceVariable());
            }
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
