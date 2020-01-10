package org.arend.core.expr;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreInferenceReferenceExpression;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Decision;

import javax.annotation.Nonnull;

public class InferenceReferenceExpression extends Expression implements CoreInferenceReferenceExpression {
  private final InferenceVariable myVar;
  private Expression mySubstExpression;

  public InferenceReferenceExpression(InferenceVariable binding, Equations equations) {
    myVar = binding;
    if (equations.isDummy()) {
      return;
    }

    binding.setReference(this);
    Expression type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCall = type.cast(ClassCallExpression.class);
    if (classCall != null && !classCall.getDefinition().getFields().isEmpty()) {
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (!field.isProperty()) {
          Expression impl = classCall.getImplementation(field, this);
          if (impl != null) {
            equations.addEquation(FieldCallExpression.make(field, classCall.getSortArgument(), this), impl.normalize(NormalizeVisitor.Mode.WHNF), field.getType(classCall.getSortArgument()).applyExpression(this), Equations.CMP.EQ, binding.getSourceNode(), binding, impl.getStuckInferenceVariable());
          }
        }
      }
      type = new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument());
    }
    binding.setType(type);
  }

  public InferenceReferenceExpression(InferenceVariable binding, Expression substExpression) {
    myVar = binding;
    mySubstExpression = substExpression;
  }

  @Override
  public InferenceVariable getVariable() {
    return mySubstExpression == null ? myVar : null;
  }

  public InferenceVariable getOriginalVariable() {
    return myVar;
  }

  @Override
  public Expression getSubstExpression() {
    return mySubstExpression;
  }

  public void setSubstExpression(Expression substExpression) {
    mySubstExpression = substExpression;
  }

  @Override
  public boolean canBeConstructor() {
    return mySubstExpression == null || mySubstExpression.canBeConstructor();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInferenceReference(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitInferenceReference(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@Nonnull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInferenceReference(this, params);
  }

  @Nonnull
  @Override
  public Expression getUnderlyingExpression() {
    return mySubstExpression == null ? this : mySubstExpression.getUnderlyingExpression();
  }

  @Override
  public boolean isInstance(Class clazz) {
    return mySubstExpression != null && mySubstExpression.isInstance(clazz) || clazz.isInstance(this);
  }

  @Override
  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : mySubstExpression != null ? mySubstExpression.cast(clazz) : null;
  }

  @Override
  public Decision isWHNF() {
    return mySubstExpression == null ? Decision.MAYBE : mySubstExpression.isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return mySubstExpression != null ? mySubstExpression.getStuckExpression() : this;
  }
}
