package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.*;
import org.arend.ext.core.ops.NormalizationMode;

import java.util.*;

public class FieldsCollector extends VoidExpressionVisitor<Void> {
  private final Binding myThisBinding;
  private final Set<? extends ClassField> myFields;
  private final Set<ClassField> myResult;
  private final Map<FunctionDefinition, ClassField> myFunctions;

  private FieldsCollector(Binding thisParameter, Set<? extends ClassField> fields, Map<FunctionDefinition, ClassField> functions, Set<ClassField> result) {
    myThisBinding = thisParameter;
    myFields = fields;
    myFunctions = functions;
    myResult = result;
  }

  public Set<ClassField> getResult() {
    return myResult;
  }

  public static void getFields(Expression expr, Binding thisBinding, Set<? extends ClassField> fields, Set<ClassField> result) {
    if (expr != null && !fields.isEmpty()) {
      expr.accept(new FieldsCollector(thisBinding, fields, Collections.emptyMap(), result), null);
    }
  }

  public static Set<ClassField> getFields(Body body, Binding thisBinding, Set<? extends ClassField> fields) {
    return getFields(body, thisBinding, fields, Collections.emptyMap());
  }

  public static Set<ClassField> getFields(Body body, Binding thisBinding, Set<? extends ClassField> fields, Map<FunctionDefinition, ClassField> functions) {
    Set<ClassField> result = new HashSet<>();
    if (!fields.isEmpty()) {
      new FieldsCollector(thisBinding, fields, functions, result).visitBody(body, null);
    }
    return result;
  }

  private void checkArgument(Expression argument, Expression type) {
    argument.accept(this, null);
    if (!(argument instanceof ReferenceExpression && ((ReferenceExpression) argument).getBinding() == myThisBinding)) {
      return;
    }

    ClassCallExpression classCall = type.cast(ClassCallExpression.class);
    if (classCall == null) {
      classCall = type.normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
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
    Expression it = expr;
    Expression type = null;
    do {
      expr = (ConCallExpression) it;

      checkArguments(expr.getDefinition().getDataTypeParameters(), expr.getDataTypeArguments());

      int recursiveParam = expr.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        checkArguments(expr.getDefinition().getParameters(), expr.getDefCallArguments());
        return null;
      }

      DependentLink link = expr.getDefinition().getParameters();
      for (int i = 0; i < expr.getDefCallArguments().size(); i++) {
        if (i != recursiveParam) {
          checkArgument(expr.getDefCallArguments().get(i), link.getTypeExpr());
        } else {
          type = link.getTypeExpr();
        }
        link = link.getNext();
      }

      it = expr.getDefCallArguments().get(recursiveParam);
    } while (it instanceof ConCallExpression);

    checkArgument(it, type);
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      checkArgument(entry.getValue(), entry.getKey().getType().getCodomain());
    }
    return super.visitClassCall(expr, params);
  }

  @Override
  public Void visitFunCall(FunCallExpression expr, Void params) {
    ClassField field = myFunctions.get(expr.getDefinition());
    if (field == null) {
      return super.visitFunCall(expr, params);
    }

    List<? extends Expression> args = expr.getDefCallArguments();
    if (args.isEmpty()) {
      return null;
    }

    ReferenceExpression refExpr = args.get(0).cast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding() == myThisBinding && (myFields == null || myFields.contains(field))) {
      myResult.add(field);
    }
    args.get(0).accept(this, null);

    DependentLink link = expr.getDefinition().getParameters().getNext();
    for (int i = 1; i < args.size(); i++) {
      checkArgument(args.get(i), link.getTypeExpr());
      link = link.getNext();
    }

    return null;
  }

  @Override
  public Void visitFieldCall(FieldCallExpression expr, Void params) {
    ReferenceExpression refExpr = expr.getArgument().cast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding() == myThisBinding && (myFields == null || myFields.contains(expr.getDefinition()))) {
      myResult.add(expr.getDefinition());
    }
    expr.getArgument().accept(this, null);
    return null;
  }
}
