package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.naming.error.NamingError;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.concrete.BaseConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.Set;

public class ClassFieldChecker extends BaseConcreteExpressionVisitor<Void> implements ConcreteDefinitionVisitor<Void, Void> {
  private Referable myThisParameter;
  private final TCClassReferable myClassReferable;
  private final ConcreteProvider myConcreteProvider;
  private final Set<? extends LocatedReferable> myFields;
  private final Set<TCReferable> myFutureFields;
  private final LocalErrorReporter myErrorReporter;

  ClassFieldChecker(Referable thisParameter, TCClassReferable classReferable, ConcreteProvider concreteProvider, Set<? extends LocatedReferable> fields, Set<TCReferable> futureFields, LocalErrorReporter errorReporter) {
    myThisParameter = thisParameter;
    myClassReferable = classReferable;
    myConcreteProvider = concreteProvider;
    myFields = fields;
    myFutureFields = futureFields;
    myErrorReporter = errorReporter;
  }

  void setThisParameter(Referable thisParameter) {
    myThisParameter = thisParameter;
  }

  private Concrete.Expression makeErrorExpression(Object data) {
    LocalError error = new NamingError("Fields may refer only to previous fields", data);
    myErrorReporter.report(error);
    return new Concrete.ErrorHoleExpression(data, error);
  }

  private boolean isParent(TCClassReferable parent, TCClassReferable child) {
    if (parent == null) {
      return false;
    }

    while (child != null) {
      if (child.equals(parent)) {
        return true;
      }
      Concrete.ReferableDefinition def = myConcreteProvider.getConcrete(child);
      if (!(def instanceof Concrete.ClassDefinition)) {
        return false;
      }
      child = ((Concrete.ClassDefinition) def).enclosingClass;
    }

    return false;
  }

  private Concrete.Expression getParentCall(TCClassReferable parent, TCClassReferable child, Concrete.Expression expr) {
    while (child != null) {
      if (child.equals(parent)) {
        return expr;
      }
      Concrete.ReferableDefinition def = myConcreteProvider.getConcrete(child);
      if (!(def instanceof Concrete.ClassDefinition)) {
        return expr;
      }
      child = ((Concrete.ClassDefinition) def).enclosingClass;
      expr = Concrete.AppExpression.make(expr.getData(), new Concrete.ReferenceExpression(expr.getData(), ((Concrete.ClassDefinition) def).getFields().get(0).getData()), expr, false);
    }

    return expr;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    if (ref instanceof TCReferable) {
      if (myFields.contains(ref)) {
        if (myFutureFields != null && myFutureFields.contains(ref)) {
          return makeErrorExpression(expr.getData());
        } else {
          return Concrete.AppExpression.make(expr.getData(), expr, new Concrete.ReferenceExpression(expr.getData(), myThisParameter), false);
        }
      } else {
        Concrete.ReferableDefinition def = myConcreteProvider.getConcrete((GlobalReferable) ref);
        if (def != null) {
          TCClassReferable defEnclosingClass = def instanceof Concrete.ClassField ? ((Concrete.ClassField) def).getRelatedDefinition().getData() : def.getRelatedDefinition().enclosingClass;
          if (myFutureFields != null && myClassReferable.equals(defEnclosingClass)) {
            return makeErrorExpression(expr.getData());
          }
          if (isParent(defEnclosingClass, myClassReferable)) {
            return Concrete.AppExpression.make(expr.getData(), expr, getParentCall(defEnclosingClass, myClassReferable, new Concrete.ReferenceExpression(expr.getData(), myThisParameter)), false);
          }
        }
      }
    }
    return expr;
  }
}
