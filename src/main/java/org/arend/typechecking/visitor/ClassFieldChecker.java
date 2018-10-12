package org.arend.typechecking.visitor;

import org.arend.naming.error.NamingError;
import org.arend.naming.reference.*;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

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
      Concrete.ClassDefinition def = myConcreteProvider.getConcreteClass(child);
      if (def == null) {
        return false;
      }
      child = def.enclosingClass;
    }

    return false;
  }

  private Concrete.Expression getParentCall(TCClassReferable parent, TCClassReferable child, Concrete.Expression expr) {
    while (child != null) {
      if (child.equals(parent)) {
        return expr;
      }
      Concrete.ClassDefinition def = myConcreteProvider.getConcreteClass(child);
      if (def == null) {
        return expr;
      }
      child = def.enclosingClass;
      expr = Concrete.AppExpression.make(expr.getData(), new Concrete.ReferenceExpression(expr.getData(), def.getFields().get(0).getData()), expr, false);
    }

    return expr;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    ref = ref instanceof TCReferable ? ((TCReferable) ref).getUnderlyingTypecheckable() : null;

    if (ref != null) {
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
