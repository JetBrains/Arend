package org.arend.typechecking.visitor;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.NameResolverError;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.naming.reference.*;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.ext.error.LocalError;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ClassFieldChecker extends BaseConcreteExpressionVisitor<Void> {
  private Referable myThisParameter;
  private final Set<TCDefReferable> myRecursiveDefs;
  private final TCDefReferable myClassReferable;
  private final Collection<CoreClassDefinition> mySuperClasses;
  private final Set<? extends LocatedReferable> myFields;
  private final Set<TCDefReferable> myFutureFields;
  private final ErrorReporter myErrorReporter;
  private int myClassCallNumber;

  ClassFieldChecker(Referable thisParameter, Set<TCDefReferable> recursiveDefs, TCDefReferable classReferable, Collection<CoreClassDefinition> superClasses, Set<? extends LocatedReferable> fields, Set<TCDefReferable> futureFields, ErrorReporter errorReporter) {
    myThisParameter = thisParameter;
    myRecursiveDefs = recursiveDefs;
    myClassReferable = classReferable;
    mySuperClasses = superClasses;
    myFields = fields;
    myFutureFields = futureFields;
    myErrorReporter = errorReporter;
  }

  void setThisParameter(Referable thisParameter) {
    myThisParameter = thisParameter;
  }

  private Concrete.Expression makeErrorExpression(Concrete.ReferenceExpression expr) {
    LocalError error = new NameResolverError("Fields may refer only to previous fields", expr);
    myErrorReporter.report(error);
    return new Concrete.ErrorHoleExpression(expr.getData(), error);
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    if (ref instanceof TCDefReferable) {
      if (myFields.contains(ref)) {
        if (myFutureFields != null && myFutureFields.contains(ref)) {
          return makeErrorExpression(expr);
        } else {
          return Concrete.AppExpression.make(expr.getData(), expr, new Concrete.ThisExpression(expr.getData(), myThisParameter), false);
        }
      } else {
        ClassDefinition enclosingClass = null;
        if (myRecursiveDefs.contains(ref)) {
          Definition def = myClassReferable.getTypechecked();
          if (def instanceof ClassDefinition) {
            enclosingClass = (ClassDefinition) def;
          }
        } else {
          Definition def = ((TCDefReferable) ref).getTypechecked();
          if (def != null && !(def instanceof ClassField)) {
            enclosingClass = def.getEnclosingClass();
          }
        }
        if (enclosingClass != null) {
          if (myFutureFields != null && myClassReferable.equals(enclosingClass.getReferable())) {
            return makeErrorExpression(expr);
          }
          if (ClassDefinition.isSubClassOf(new ArrayDeque<>(mySuperClasses), enclosingClass)) {
            return Concrete.AppExpression.make(expr.getData(), expr, new Concrete.ThisExpression(expr.getData(), myThisParameter), false);
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
    if (expr.getFunction() instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) expr.getFunction()).getReferent() instanceof MetaReferable) {
      MetaDefinition meta = ((MetaReferable) ((Concrete.ReferenceExpression) expr.getFunction()).getReferent()).getDefinition();
      if (meta != null) {
        int[] indices = meta.desugarArguments(expr.getArguments());
        if (indices != null) {
          List<Concrete.Argument> arguments = expr.getArguments();
          for (int index : indices) {
            Concrete.Argument arg = arguments.get(index);
            arg.expression = arg.expression.accept(this, params);
          }
          return expr;
        }
      }
    } else if (expr.getFunction() instanceof Concrete.ReferenceExpression && !expr.getArguments().get(0).isExplicit()) {
      if (expr.getArguments().get(0).expression instanceof Concrete.HoleExpression) {
        Referable ref = ((Concrete.ReferenceExpression) expr.getFunction()).getReferent();
        if (ref instanceof TCDefReferable) {
          Definition def = ((TCDefReferable) ref).getTypechecked();
          if (def != null && def.getEnclosingClass() != null && def.getEnclosingClass().getReferable() == myClassReferable) {
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
    }

    return super.visitApp(expr, params);
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    expr.setBaseClassExpression(expr.getBaseClassExpression().accept(this, params));
    myClassCallNumber++;
    for (Concrete.ClassElement element : expr.getStatements()) {
      if (myClassCallNumber == 1 && element instanceof Concrete.ClassFieldImpl && ((Concrete.ClassFieldImpl) element).implementation instanceof Concrete.ThisExpression && ((Concrete.ClassFieldImpl) element).getImplementedRef() instanceof TCDefReferable && ((TCDefReferable) ((Concrete.ClassFieldImpl) element).getImplementedRef()).getTypechecked() instanceof ClassDefinition) {
        ((Concrete.ThisExpression) ((Concrete.ClassFieldImpl) element).implementation).setReferent(myThisParameter);
      }
      visitClassElement(element, params);
    }
    myClassCallNumber--;
    return expr;
  }
}
