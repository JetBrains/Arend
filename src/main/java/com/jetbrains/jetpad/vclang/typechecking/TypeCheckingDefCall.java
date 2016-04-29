package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.HasErrors;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static com.jetbrains.jetpad.vclang.naming.NamespaceMember.toNamespaceMember;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class TypeCheckingDefCall {
  private final CheckTypeVisitor myVisitor;
  private ClassDefinition myThisClass;
  private Expression myThisExpr;

  public TypeCheckingDefCall(CheckTypeVisitor visitor) {
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
      NamespaceMember member = toNamespaceMember(resolvedDefinition);
      if (member == null || member.definition == null) {
        assert false;
        TypeCheckingError error = new TypeCheckingError("Internal error: definition '" + resolvedDefinition + "' is not available yet", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
      Expression thisExpr = null;
      if (member.definition.getThisClass() != null) {
        if (myThisClass != null) {
          thisExpr = findParent(myThisClass, member.definition, myThisExpr, expr);
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
      return applyThis(new CheckTypeVisitor.Result(member.definition.getDefCall(), member.definition.getTypeWithThis()), thisExpr, expr);
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
    ClassCallExpression classCall = type.toClassCall();
    if (classCall != null) {
      ClassDefinition classDefinition = classCall.getDefinition();
      String name = expr.getName();
      Definition definition = classDefinition.getResolvedName().toNamespace().getDefinition(name);
      if (definition == null) {
        TypeCheckingError error = new TypeCheckingError("Cannot find definition '" + name + "' in class '" + classDefinition.getName() + "'", expr);
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

      Expression thisExpr = result.expression;
      result.expression = definition.getDefCall();
      result.type = definition.getTypeWithThis();
      return applyThis(result, thisExpr, expr);
    }

    List<? extends Expression> arguments = result.expression.getArguments();
    Expression fun = result.expression.getFunction();
    DataCallExpression dataCall = fun.toDataCall();
    if (dataCall != null) {
      DataDefinition dataDefinition = dataCall.getDefinition();
      String name = expr.getName();
      Constructor constructor = dataDefinition.getConstructor(name);
      if (constructor != null) {
        result.expression = ConCall(constructor, new ArrayList<>(arguments));
        result.type = constructor.getType().applyExpressions(arguments);
        return result;
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
    ClassCallExpression classCall1 = result.expression.toClassCall();
    if (classCall1 != null) {
      definition = classCall1.getDefinition();
      ClassField parentField = classCall1.getDefinition().getParentField();
      if (parentField != null) {
        ClassCallExpression.ImplementStatement statement = classCall1.getImplementStatements().get(parentField);
        if (statement != null) {
          thisExpr = statement.term;
        }
      }
    } else
    if (result.expression.toDefCall() != null) {
      thisExpr = null;
      definition = result.expression.toDefCall().getDefinition();
    } else
    if (result.expression.getFunction().toDefCall() != null && result.expression.getArguments().size() == 1) {
      thisExpr = result.expression.getArguments().get(0);
      definition = result.expression.getFunction().toDefCall().getDefinition();
    } else {
      TypeCheckingError error = new TypeCheckingError("Expected a definition", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    String name = expr.getName();
    ResolvedName resolvedName = definition.getResolvedName();
    NamespaceMember member = resolvedName.toNamespace().getMember(name);
    if (member == null) {
      TypeCheckingError error = new TypeCheckingError("Cannot find definition '" + name + "' in '" + resolvedName + "'", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }
    if (member.definition == null) {
      assert false;
      TypeCheckingError error = new TypeCheckingError("Internal error: definition '" + member.namespace + "' is not available yet", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    result.expression = member.definition.getDefCall();
    result.type = member.definition.getTypeWithThis();
    return applyThis(result, thisExpr, expr);
  }

  private Expression findParent(ClassDefinition classDefinition, Definition definition, Expression result, Abstract.Expression expr) {
    if (classDefinition == definition.getThisClass()) {
      return result;
    }
    ClassField parentField = classDefinition.getParentField();
    if (parentField == null || parentField.getBaseType().toClassCall() == null) {
      TypeCheckingError error = new TypeCheckingError("Definition '" + definition.getName() + "' is not available in this context", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }
    return findParent(parentField.getBaseType().toClassCall().getDefinition(), definition, Apps(FieldCall(parentField), result), expr);
  }

  private CheckTypeVisitor.Result applyThis(CheckTypeVisitor.Result result, Expression thisExpr, Abstract.Expression expr) {
    DefCallExpression defCall = result.expression.toDefCall();
    if (result.type == null) {
      TypeCheckingError error = new HasErrors(defCall.getDefinition().getName(), expr);
      expr.setWellTyped(myVisitor.getContext(), Error(result.expression, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }
    if (thisExpr != null) {
      result.expression = defCall.applyThis(thisExpr);
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
