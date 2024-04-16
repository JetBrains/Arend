package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.*;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalExpressionPrettifier implements ExpressionPrettifier {
  private final Map<Binding, Accessor> myBindings;

  public static class Accessor {
    private final Referable referable;
    private final List<Accessor> projAccessors;
    private final Map<ClassField, Accessor> fieldAccessors;

    public Accessor(Referable referable, List<Accessor> projAccessors, Map<ClassField, Accessor> fieldAccessors) {
      this.referable = referable;
      this.projAccessors = projAccessors;
      this.fieldAccessors = fieldAccessors;
    }

    public void addProjAccessor(Accessor accessor) {
      projAccessors.add(accessor);
    }

    public void addFieldAccessor(ClassField field, Accessor accessor) {
      fieldAccessors.put(field, accessor);
    }
  }

  public LocalExpressionPrettifier() {
    myBindings = new HashMap<>();
  }

  public LocalExpressionPrettifier(LocalExpressionPrettifier prettifier) {
    myBindings = new HashMap<>(prettifier.myBindings);
  }

  public void removeBinding(Binding binding) {
    myBindings.remove(binding);
  }

  public void clear() {
    myBindings.clear();
  }

  public void addBinding(Binding binding, Accessor accessor) {
    myBindings.putIfAbsent(binding, accessor);
  }

  @Override
  public @Nullable ConcreteExpression prettify(@NotNull CoreExpression expression, @NotNull ExpressionPrettifier defaultPrettifier) {
    if (!(expression instanceof ProjExpression || expression instanceof FieldCallExpression)) {
      return null;
    }
    List<Object> indices = new ArrayList<>();
    while (true) {
      if (expression instanceof ProjExpression projExpr) {
        indices.add(projExpr.getField());
        expression = projExpr.getExpression().getUnderlyingExpression();
      } else if (expression instanceof FieldCallExpression fieldCall) {
        indices.add(fieldCall.getDefinition());
        expression = fieldCall.getArgument().getUnderlyingExpression();
      } else {
        break;
      }
    }
    if (!(expression instanceof ReferenceExpression refExpr)) {
      return null;
    }
    Accessor accessor = myBindings.get(refExpr.getBinding());
    if (accessor == null) {
      return null;
    }

    int i = indices.size() - 1;
    for (; i >= 0; i--) {
      Object index = indices.get(i);
      if (index instanceof Integer intIndex) {
        if (accessor.projAccessors == null) break;
        if (intIndex >= accessor.projAccessors.size()) return null;
        accessor = accessor.projAccessors.get(intIndex);
      } else {
        ClassField field = (ClassField) index;
        if (accessor.fieldAccessors == null) break;
        accessor = accessor.fieldAccessors.get(field);
      }
      if (accessor == null) return null;
    }

    if (accessor.referable == null) return null;
    Concrete.Expression result = new Concrete.ReferenceExpression(null, accessor.referable);
    for (; i >= 0; i--) {
      Object index = indices.get(i);
      if (index instanceof Integer) {
        result = new Concrete.ProjExpression(null, result, (int) index);
      } else {
        result = Concrete.AppExpression.make(null, new Concrete.ReferenceExpression(null, ((ClassField) index).getReferable()), result, false);
      }
    }
    return result;
  }
}
