package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.HasErrors;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class TypeCheckingDefCall {
  private final TypecheckerState myState;
  private final CheckTypeVisitor myVisitor;
  private ClassDefinition myThisClass;
  private Expression myThisExpr;

  public TypeCheckingDefCall(TypecheckerState state, CheckTypeVisitor visitor) {
    myState = state;
    myVisitor = visitor;
  }

  public void setThisClass(ClassDefinition thisClass, Expression thisExpr) {
    myThisClass = thisClass;
    myThisExpr = thisExpr;
  }

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public Expression getThisExpression() {
    return myThisExpr;
  }

  public CheckTypeVisitor.Result typeCheckDefCall(Abstract.DefCallExpression expr) {
    Referable resolvedDefinition = expr.getReferent();
    if (resolvedDefinition != null) {
      final Definition typecheckedDefinition = myState.getTypechecked(resolvedDefinition);
      if (typecheckedDefinition == null) {
        assert false;
        TypeCheckingError error = new TypeCheckingError("Internal error: definition '" + resolvedDefinition + "' is not available yet", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
      Expression thisExpr = null;
      if (typecheckedDefinition.getThisClass() != null) {
        if (myThisClass != null) {
          thisExpr = findParent(myThisClass, typecheckedDefinition, myThisExpr, expr);
          if (thisExpr == null) {
            return null;
          }
        } else {
          TypeCheckingError error = new TypeCheckingError("Non-static definitions are not allowed in a static context", expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      }
      return applyThis(new CheckTypeVisitor.Result(typecheckedDefinition.getDefCall(), typecheckedDefinition.getTypeWithThis()), thisExpr, expr);
    }

    Abstract.Expression left = expr.getExpression();
    CheckTypeVisitor.Result result = null;
    if (left != null) {
      result = left.accept(myVisitor, null);
      if (result == null) {
        return null;
      }
    }

    if (result == null) {
      return getLocalVar(expr);
    }

    Expression type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (type instanceof ClassCallExpression) {
      ClassDefinition classDefinition = ((ClassCallExpression) type).getDefinition();
      String name = expr.getName();
      Definition definition = myState.getDynamicTypecheckedMember(classDefinition, name);
      if (definition == null) {
        TypeCheckingError error = new TypeCheckingError("Cannot find dynamic definition '" + name + "' in class '" + classDefinition.getResolvedName() + "'", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
      if (definition.getThisClass() == null) {
        TypeCheckingError error = new TypeCheckingError("Static definitions are not allowed in a non-static context", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
      if (definition.getThisClass() != classDefinition) {
        TypeCheckingError error = new TypeMismatchError(definition.getThisClass().getDefCall(), type, left);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
      return applyThis(new CheckTypeVisitor.Result(definition.getDefCall(), definition.getTypeWithThis()), result.expression, expr);
    }

    List<? extends Expression> arguments = result.expression.getArguments();
    Expression fun = result.expression.getFunction();
    if (fun instanceof DataCallExpression) {
      DataDefinition dataDefinition = ((DataCallExpression) fun).getDefinition();
      String name = expr.getName();
      Constructor constructor = dataDefinition.getConstructor(name);
      if (constructor != null) {
        return new CheckTypeVisitor.Result(ConCall(constructor, new ArrayList<>(arguments)), constructor.getType().applyExpressions(arguments));
      }

      if (!arguments.isEmpty()) {
        TypeCheckingError error = new TypeCheckingError("Cannot find constructor '" + name + "' of data type '" + dataDefinition.getName() + "'", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    Definition definition;
    Expression thisExpr = null;
    if (result.expression instanceof ClassCallExpression) {
      ClassCallExpression classCall = (ClassCallExpression) result.expression;
      definition = classCall.getDefinition();
      ClassField parentField = classCall.getDefinition().getParentField();
      if (parentField != null) {
        ClassCallExpression.ImplementStatement statement = classCall.getImplementStatements().get(parentField);
        if (statement != null) {
          thisExpr = statement.term;
        }
      }
    } else
    if (result.expression instanceof DefCallExpression) {
      thisExpr = null;
      definition = ((DefCallExpression) result.expression).getDefinition();
    } else
    if (result.expression instanceof AppExpression && result.expression.getFunction() instanceof DefCallExpression && result.expression.getArguments().size() == 1) {
      thisExpr = ((AppExpression) result.expression).getArguments().get(0);
      definition = ((DefCallExpression) result.expression.getFunction()).getDefinition();
    } else {
      TypeCheckingError error = new TypeCheckingError("Expected a definition", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    String name = expr.getName();
    Definition member = myState.getTypecheckedMember(definition, name);
    if (member == null) {
      TypeCheckingError error = new TypeCheckingError("Cannot find definition '" + name + "' in '" + definition.getResolvedName() + "'", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    return applyThis(new CheckTypeVisitor.Result(member.getDefCall(), member.getTypeWithThis()), thisExpr, expr);
  }

  private Expression findParent(ClassDefinition classDefinition, Definition definition, Expression result, Abstract.Expression expr) {
    if (classDefinition == definition.getThisClass()) {
      return result;
    }
    ClassField parentField = classDefinition.getParentField();
    if (parentField == null || !(parentField.getBaseType() instanceof ClassCallExpression)) {
      TypeCheckingError error = new TypeCheckingError("Definition '" + definition.getName() + "' is not available in this context", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }
    return findParent(((ClassCallExpression) parentField.getBaseType()).getDefinition(), definition, Apps(FieldCall(parentField), result), expr);
  }

  private CheckTypeVisitor.Result applyThis(CheckTypeVisitor.Result result, Expression thisExpr, Abstract.Expression expr) {
    if (result.type == null) {
      TypeCheckingError error = new HasErrors(((DefCallExpression) result.expression).getDefinition().getName(), expr);
      expr.setWellTyped(myVisitor.getContext(), Error(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }
    if (thisExpr != null) {
      result.expression = ((DefCallExpression) result.expression).applyThis(thisExpr);
      result.type = result.type.applyExpressions(Collections.singletonList(thisExpr));
    }
    return result;
  }

  public CheckTypeVisitor.Result getLocalVar(Abstract.DefCallExpression expr) {
    String name = expr.getName();
    ListIterator<Binding> it = myVisitor.getContext().listIterator(myVisitor.getContext().size());
    while (it.hasPrevious()) {
      Binding def = it.previous();
      if (name.equals(def.getName())) {
        return new CheckTypeVisitor.Result(Reference(def), def.getType());
      }
    }

    TypeCheckingError error = new NotInScopeError(expr, name);
    expr.setWellTyped(myVisitor.getContext(), Error(null, error));
    myVisitor.getErrorReporter().report(error);
    return null;
  }
}
