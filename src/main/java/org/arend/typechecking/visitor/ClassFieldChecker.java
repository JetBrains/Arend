package org.arend.typechecking.visitor;

import org.arend.error.ErrorReporter;
import org.arend.naming.error.NamingError;
import org.arend.naming.reference.*;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.provider.ConcreteProvider;

import java.util.Set;

public class ClassFieldChecker extends BaseConcreteExpressionVisitor<Void> {
  private Referable myThisParameter;
  private final TCClassReferable myClassReferable;
  private final ConcreteProvider myConcreteProvider;
  private final Set<? extends LocatedReferable> myFields;
  private final Set<TCReferable> myFutureFields;
  private final ErrorReporter myErrorReporter;
  private int myClassCallNumber;

  ClassFieldChecker(Referable thisParameter, TCClassReferable classReferable, ConcreteProvider concreteProvider, Set<? extends LocatedReferable> fields, Set<TCReferable> futureFields, ErrorReporter errorReporter) {
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

  private Concrete.Expression makeErrorExpression(Concrete.ReferenceExpression expr) {
    LocalError error = new NamingError("Fields may refer only to previous fields", expr);
    myErrorReporter.report(error);
    return new Concrete.ErrorHoleExpression(expr.getData(), error);
  }

  private boolean isParent(TCClassReferable parent, TCClassReferable child) {
    if (parent == null) {
      return false;
    }

    while (child != null) {
      if (child.isSubClassOf(parent)) {
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
      if (child.isSubClassOf(parent)) {
        return expr;
      }
      Concrete.ClassDefinition def = myConcreteProvider.getConcreteClass(child);
      if (def == null) {
        return expr;
      }
      child = def.enclosingClass;
      expr = Concrete.AppExpression.make(expr.getData(), new Concrete.ReferenceExpression(expr.getData(), ((Concrete.ClassField) def.getElements().get(0)).getData()), expr, false);
    }

    return expr;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    if (ref instanceof TCReferable) {
      if (myFields.contains(ref)) {
        if (myFutureFields != null && myFutureFields.contains(ref)) {
          return makeErrorExpression(expr);
        } else {
          return Concrete.AppExpression.make(expr.getData(), expr, new Concrete.ThisExpression(expr.getData(), myThisParameter), false);
        }
      } else {
        Concrete.ReferableDefinition def = myConcreteProvider.getConcrete((TCReferable) ref);
        if (def != null && !(def instanceof Concrete.ClassField)) {
          TCClassReferable defEnclosingClass = def.getRelatedDefinition().enclosingClass;
          if (myFutureFields != null && myClassReferable.equals(defEnclosingClass)) {
            return makeErrorExpression(expr);
          }
          if (isParent(defEnclosingClass, myClassReferable)) {
            return Concrete.AppExpression.make(expr.getData(), expr, getParentCall(defEnclosingClass, myClassReferable, new Concrete.ThisExpression(expr.getData(), myThisParameter)), false);
          }
        }
      }
    }
    return expr;
  }

  @Override
  public Concrete.ThisExpression visitThis(Concrete.ThisExpression expr, Void params) {
    if (myClassCallNumber == 0) {
      expr.setReferent(myThisParameter);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitTyped(Concrete.TypedExpression expr, Void params) {
    if (expr.expression instanceof Concrete.ThisExpression && expr.type instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) expr.type).getReferent().equals(myClassReferable)) {
      ((Concrete.ThisExpression) expr.expression).setReferent(myThisParameter);
      return expr.expression;
    } else {
      return super.visitTyped(expr, null);
    }
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    if (expr.getFunction() instanceof Concrete.ReferenceExpression && !expr.getArguments().get(0).isExplicit()) {
      if (expr.getArguments().get(0).expression instanceof Concrete.HoleExpression) {
        Referable ref = ((Concrete.ReferenceExpression) expr.getFunction()).getReferent();
        if (ref instanceof GlobalReferable) {
          Concrete.ReferableDefinition def = myConcreteProvider.getConcrete((GlobalReferable) ref);
          if (def != null && def.getRelatedDefinition().enclosingClass == myClassReferable) {
            if (expr.getArguments().size() == 1) {
              return expr.getFunction().accept(this, null);
            } else {
              expr.getArguments().remove(0);
              return super.visitApp(expr, null);
            }
          }
        }
      }
      for (Concrete.Argument argument : expr.getArguments()) {
        argument.expression = argument.expression.accept(this, params);
      }
      return expr;
    } else {
      return super.visitApp(expr, params);
    }
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    expr.setBaseClassExpression(expr.getBaseClassExpression().accept(this, params));
    myClassCallNumber++;
    visitClassFieldImpls(expr.getStatements(), params);
    myClassCallNumber--;
    return expr;
  }
}
