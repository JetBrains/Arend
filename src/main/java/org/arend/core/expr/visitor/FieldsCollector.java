package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FieldsCollector extends VoidExpressionVisitor<Void> {
  private final Binding myThisBinding;
  private final Set<? extends ClassField> myFields;
  private final Set<ClassField> myResult;

  private FieldsCollector(Binding thisParameter, Set<? extends ClassField> fields, Set<ClassField> result) {
    myThisBinding = thisParameter;
    myFields = fields;
    myResult = result;
  }

  public Set<ClassField> getResult() {
    return myResult;
  }

  public static void getFields(Expression expr, Binding thisBinding, Set<? extends ClassField> fields, Set<ClassField> result) {
    if (!fields.isEmpty()) {
      expr.accept(new FieldsCollector(thisBinding, fields, result), null);
    }
  }

  public static Set<ClassField> getFields(Expression expr, DependentLink thisParameter, Set<? extends ClassField> fields) {
    Set<ClassField> result = new HashSet<>();
    getFields(expr, thisParameter, fields, result);
    return result;
  }

  private void checkArgument(Expression argument, Expression type) {
    argument.accept(this, null);
    if (!(argument instanceof ReferenceExpression && ((ReferenceExpression) argument).getBinding() == myThisBinding)) {
      return;
    }

    ClassCallExpression classCall = type.cast(ClassCallExpression.class);
    if (classCall == null) {
      classCall = type.normalize(NormalizeVisitor.Mode.WHNF).cast(ClassCallExpression.class);
    }
    if (classCall != null) {
      myResult.addAll(classCall.getDefinition().getFields());
    }
  }

  private void checkArguments(DependentLink link, List<? extends Expression> arguments) {
    for (Expression argument : arguments) {
      checkArgument(argument, link.getTypeExpr());
      link = link.getNext();
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    checkArguments(expr.getDefinition().getParameters(), expr.getDefCallArguments());
    return null;
  }

  @Override
  public Void visitConCall(ConCallExpression expr, Void params) {
    checkArguments(expr.getDefinition().getDataTypeParameters(), expr.getDataTypeArguments());
    checkArguments(expr.getDefinition().getParameters(), expr.getDefCallArguments());
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      checkArgument(entry.getValue(), entry.getKey().getType(Sort.STD).getCodomain());
    }
    return super.visitClassCall(expr, params);
  }

  @Override
  public Void visitFieldCall(FieldCallExpression expr, Void params) {
    if (myFields == null || myFields.contains(expr.getDefinition())) {
      myResult.add(expr.getDefinition());
    }
    expr.getArgument().accept(this, null);
    return null;
  }
}
