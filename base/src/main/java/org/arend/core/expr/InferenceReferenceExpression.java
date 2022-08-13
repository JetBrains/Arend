package org.arend.core.expr;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreInferenceReferenceExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InferenceReferenceExpression extends Expression implements CoreInferenceReferenceExpression {
  private final InferenceVariable myVar;
  private Set<ClassField> myImplementedFields;
  private Expression mySubstExpression;

  public static Expression makeUnique(Expression type) {
    if (type instanceof SigmaExpression && !((SigmaExpression) type).getParameters().hasNext()) {
      return new TupleExpression(Collections.emptyList(), (SigmaExpression) type);
    } else if (type instanceof ClassCallExpression && ((ClassCallExpression) type).getNumberOfNotImplementedFields() == 0) {
      return new NewExpression(null, (ClassCallExpression) type);
    }
    return null;
  }

  public static Expression make(InferenceVariable binding, Equations equations) {
    Expression type = binding.getType().normalize(NormalizationMode.WHNF);
    Expression uniqueResult = makeUnique(type);
    if (uniqueResult != null) return uniqueResult;

    InferenceReferenceExpression result = new InferenceReferenceExpression(binding);
    if (!equations.supportsExpressions()) {
      return result;
    }

    if (!binding.resetClassCall()) {
      return result;
    }

    ClassCallExpression classCall = type.cast(ClassCallExpression.class);
    if (classCall != null && !classCall.getDefinition().getFields().isEmpty()) {
      type = new ClassCallExpression(classCall.getDefinition(), classCall.getLevels());
      binding.setType(type);
      result.myImplementedFields = new HashSet<>();
      for (Map.Entry<ClassField, Expression> entry : classCall.getImplementedHere().entrySet()) {
        ClassField field = entry.getKey();
        if (field.isProperty()) continue;
        equations.addEquation(FieldCallExpression.make(field, result), entry.getValue().normalize(NormalizationMode.WHNF), classCall.getDefinition().getFieldType(field, classCall.getLevels(field.getParentClass()), result), CMP.EQ, binding.getSourceNode(), binding, entry.getValue().getStuckInferenceVariable(), false);
        if (result.getSubstExpression() != null) {
          Expression solution = result.getSubstExpression();
          binding.setType(classCall);
          binding.unsolve();
          return equations.solve(binding, solution) ? solution : result;
        }
        result.myImplementedFields.add(field);
      }
    }
    return result;
  }

  public InferenceReferenceExpression(InferenceVariable binding, Expression substExpression) {
    myVar = binding;
    mySubstExpression = substExpression;
  }

  public InferenceReferenceExpression(InferenceVariable binding) {
    myVar = binding;
    binding.setReference(this);
  }

  @Override
  public InferenceVariable getVariable() {
    return mySubstExpression == null ? myVar : null;
  }

  public InferenceVariable getOriginalVariable() {
    return myVar;
  }

  public boolean isFieldImplemented(ClassField field) {
    return myImplementedFields != null && myImplementedFields.contains(field);
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
  public boolean isBoxed() {
    return mySubstExpression != null && mySubstExpression.isBoxed();
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
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInferenceReference(this, params);
  }

  @NotNull
  @Override
  public Expression getUnderlyingExpression() {
    return mySubstExpression == null ? this : mySubstExpression.getUnderlyingExpression();
  }

  @Override
  public <T extends Expression> boolean isInstance(Class<T> clazz) {
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
