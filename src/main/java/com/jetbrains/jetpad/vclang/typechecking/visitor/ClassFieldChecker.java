package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.naming.error.NamingError;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ClassFieldChecker implements ConcreteExpressionVisitor<Void, Concrete.Expression>, ConcreteDefinitionVisitor<Void, Void> {
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

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    expr.function = expr.function.accept(this, null);
    for (Concrete.Argument argument : expr.getArguments()) {
      argument.expression = argument.expression.accept(this, null);
    }
    return expr;
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

  private void visitParameters(List<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) parameter).type = ((Concrete.TypeParameter) parameter).type.accept(this, null);
      }
    }
  }

  @Override
  public Concrete.Expression visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    visitParameters(expr.getParameters());
    expr.body = expr.body.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    visitParameters(expr.getParameters());
    expr.codomain = expr.codomain.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Void params) {
    if (expr.expression != null) {
      expr.expression = expr.expression.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void params) {
    for (int i = 0; i < expr.getFields().size(); i++) {
      expr.getFields().set(i, expr.getFields().get(i).accept(this, null));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      expr.getExpressions().set(i, expr.getExpressions().get(i).accept(this, null));
    }
    for (Concrete.FunctionClause clause : expr.getClauses()) {
      clause.expression = clause.expression.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void params) {
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    expr.baseClassExpression = expr.baseClassExpression.accept(this, null);
    for (Concrete.ClassFieldImpl classFieldImpl : expr.getStatements()) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void params) {
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    for (Concrete.LetClause clause : expr.getClauses()) {
      if (clause.resultType != null) {
        clause.resultType = clause.resultType.accept(this, null);
      }
      clause.term = clause.term.accept(this, null);
    }
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return expr;
  }

  private void visitClauses(Collection<? extends Concrete.FunctionClause> clauses) {
    for (Concrete.FunctionClause clause : clauses) {
      if (clause.expression != null) {
        clause.expression = clause.expression.accept(this, null);
      }
    }
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
    visitParameters(def.getParameters());

    if (def.getResultType() != null) {
      def.setResultType(def.getResultType().accept(this, null));
    }

    Concrete.FunctionBody body = def.getBody();
    if (body instanceof Concrete.TermFunctionBody) {
      ((Concrete.TermFunctionBody) body).setTerm(((Concrete.TermFunctionBody) body).getTerm().accept(this, null));
    }
    if (body instanceof Concrete.ElimFunctionBody) {
      for (Concrete.FunctionClause clause : ((Concrete.ElimFunctionBody) body).getClauses()) {
        if (clause.expression != null) {
          clause.expression = clause.expression.accept(this, null);
        }
      }
    }

    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    visitParameters(def.getParameters());
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        visitParameters(constructor.getParameters());
        visitClauses(constructor.getClauses());
      }
    }
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    for (Concrete.ClassField field : def.getFields()) {
      field.setResultType(field.getResultType().accept(this, null));
    }
    for (Concrete.ClassFieldImpl classFieldImpl : def.getImplementations()) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitClassSynonym(Concrete.ClassSynonym def, Void params) {
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Void params) {
    visitParameters(def.getParameters());
    for (Concrete.ClassFieldImpl classFieldImpl : def.getClassFieldImpls()) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
    }
    return null;
  }
}
