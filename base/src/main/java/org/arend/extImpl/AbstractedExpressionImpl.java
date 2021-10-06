package org.arend.extImpl;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.AbstractedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AbstractedExpressionImpl implements AbstractedExpression {
  private final List<? extends Binding> myParameters;
  private final AbstractedExpression myExpression;

  private AbstractedExpressionImpl(List<? extends Binding> parameters, AbstractedExpression expression) {
    myParameters = parameters;
    myExpression = expression;
  }

  public static AbstractedExpression make(List<? extends Binding> parameters, AbstractedExpression expression) {
    return parameters.isEmpty() ? expression : new AbstractedExpressionImpl(parameters, expression);
  }

  public static AbstractedExpression make(DependentLink parameters, AbstractedExpression expression) {
    return parameters.hasNext() ? new AbstractedExpressionImpl(DependentLink.Helper.toList(parameters), expression) : expression;
  }

  public List<? extends Binding> getParameters() {
    return myParameters;
  }

  public AbstractedExpression getExpression() {
    return myExpression;
  }

  public static AbstractedExpression subst(AbstractedExpression expression, SubstVisitor visitor) {
    if (visitor.isEmpty()) {
      return expression;
    }
    if (expression instanceof Expression) {
      return ((Expression) expression).accept(visitor, null);
    }
    if (expression instanceof AbstractedDependentLinkType) {
      AbstractedDependentLinkType abs = (AbstractedDependentLinkType) expression;
      return AbstractedExpressionImpl.make(abs.getParameters().subst(visitor, abs.getSize(), false), DependentLink.Helper.get(abs.getParameters(), abs.getSize()).getTypeExpr().accept(visitor, null));
    }
    if (!(expression instanceof AbstractedExpressionImpl)) {
      throw new IllegalArgumentException();
    }
    AbstractedExpressionImpl abs = (AbstractedExpressionImpl) expression;
    List<Binding> newBindings = new ArrayList<>(abs.myParameters.size());
    for (Binding binding : abs.myParameters) {
      Binding newBinding = binding.subst(visitor);
      newBindings.add(newBinding);
      visitor.getExprSubstitution().add(binding, new ReferenceExpression(newBinding));
    }
    return new AbstractedExpressionImpl(newBindings, subst(abs.myExpression, visitor));
  }

  public static Expression getExpression(AbstractedExpression abstracted, List<Binding> bindings) {
    if (abstracted instanceof Expression) {
      return (Expression) abstracted;
    }
    if (abstracted instanceof AbstractedDependentLinkType) {
      AbstractedDependentLinkType abs = (AbstractedDependentLinkType) abstracted;
      DependentLink link = abs.getParameters();
      for (int i = 0; i < abs.getSize(); i++) {
        bindings.add(link);
        link = link.getNext();
      }
      return link.getTypeExpr();
    }
    if (!(abstracted instanceof AbstractedExpressionImpl)) {
      throw new IllegalArgumentException();
    }
    AbstractedExpressionImpl abs = (AbstractedExpressionImpl) abstracted;
    bindings.addAll(abs.myParameters);
    return getExpression(abs.myExpression, bindings);
  }

  @Override
  public int getNumberOfAbstractedBindings() {
    return myParameters.size();
  }

  @Override
  public @Nullable CoreBinding findFreeBinding(@NotNull Set<? extends CoreBinding> bindings) {
    if (bindings.isEmpty()) return null;
    for (Binding param : myParameters) {
      CoreBinding free = param.getTypeExpr().findFreeBinding(bindings);
      if (free != null) return free;
    }
    return myExpression.findFreeBinding(bindings);
  }
}
