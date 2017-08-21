package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckableProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CollectDefCallsVisitor<T> implements ConcreteExpressionVisitor<T, Void, Void> {
  private final InstanceProvider myInstanceProvider;
  private final TypecheckableProvider myTypecheckableProvider;
  private final Set<Abstract.GlobalReferableSourceNode> myDependencies;

  public CollectDefCallsVisitor(InstanceProvider instanceProvider, TypecheckableProvider typecheckableProvider, Set<Abstract.GlobalReferableSourceNode> dependencies) {
    myInstanceProvider = instanceProvider;
    myTypecheckableProvider = typecheckableProvider;
    myDependencies = dependencies;
  }

  @Override
  public Void visitApp(Concrete.AppExpression<T> expr, Void ignore) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  private void addDependencies(Abstract.ReferableSourceNode referable) {
    if (!(referable instanceof Abstract.GlobalReferableSourceNode)) {
      return;
    }

    Concrete.Definition<?> definition = myTypecheckableProvider.forReferable((Abstract.GlobalReferableSourceNode) referable);
    if (myInstanceProvider != null) {
      if (definition instanceof Concrete.ClassViewField) {
        myDependencies.addAll(myInstanceProvider.getInstances(((Concrete.ClassViewField) definition).getOwnView()));
      } else {
        Collection<? extends Concrete.Parameter<?>> parameters = Concrete.getParameters(definition);
        if (parameters != null) {
          if (definition instanceof Concrete.Constructor) {
            List<? extends Concrete.TypeParameter<?>> dataTypeParameters = ((Concrete.Constructor<?>) definition).getDataType().getParameters();
            List<Concrete.Parameter<?>> totalParameters = new ArrayList<>(dataTypeParameters.size() + parameters.size());
            totalParameters.addAll(dataTypeParameters);
            totalParameters.addAll(parameters);
            parameters = totalParameters;
          }
          for (Concrete.Parameter<?> parameter : parameters) {
            Abstract.ClassView classView = Concrete.getUnderlyingClassView(((Concrete.TypeParameter<?>) parameter).getType());
            if (classView != null) {
              myDependencies.addAll(myInstanceProvider.getInstances(classView));
            }
          }
        }
      }
    }
    myDependencies.add(definition);
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression<T> expr, Void ignore) {
    addDependencies(expr.getReferent());
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitModuleCall(Concrete.ModuleCallExpression expr, Void params) {
    if (expr.getModule() != null)
      myDependencies.add(expr.getModule());
    return null;
  }

  @Override
  public Void visitLam(Concrete.LamExpression<T> expr, Void ignore) {
    visitParameters(expr.getParameters());
    expr.getBody().accept(this, null);
    return null;
  }

  private void visitParameters(List<? extends Concrete.Parameter<T>> params) {
    for (Concrete.Parameter<T> param : params) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter<T>) param).getType().accept(this, null);
      }
    }
  }

  @Override
  public Void visitPi(Concrete.PiExpression<T> expr, Void ignore) {
    visitParameters(expr.getParameters());
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitUniverse(Concrete.UniverseExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitInferHole(Concrete.InferHoleExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression<T> expr, Void ignore) {
    for (Concrete.Expression<T> comp : expr.getFields()) {
      comp.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression<T> expr, Void ignore) {
    visitParameters(expr.getParameters());
    return null;
  }

  @Override
  public Void visitBinOp(Concrete.BinOpExpression<T> expr, Void ignore) {
    addDependencies(expr.getReferent());
    expr.getLeft().accept(this, null);
    if (expr.getRight() != null) {
      expr.getRight().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitBinOpSequence(Concrete.BinOpSequenceExpression<T> expr, Void ignore) {
    expr.getLeft().accept(this, null);
    for (Concrete.BinOpSequenceElem<T> elem : expr.getSequence()) {
      visitReference(elem.binOp, null);
      if (elem.argument != null) {
        elem.argument.accept(this, null);
      }
    }
    return null;
  }

  @Override
  public Void visitCase(Concrete.CaseExpression<T> expr, Void ignore) {
    for (Concrete.Expression<T> caseExpr : expr.getExpressions()) {
      caseExpr.accept(this, null);
    }
    for (Concrete.FunctionClause<T> clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitProj(Concrete.ProjExpression<T> expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression<T> expr, Void ignore) {
    expr.getBaseClassExpression().accept(this, null);
    for (Concrete.ClassFieldImpl<T> statement : expr.getStatements()) {
      statement.getImplementation().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitNew(Concrete.NewExpression<T> expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Concrete.LetExpression<T> letExpression, Void ignore) {
    for (Concrete.LetClause<T> clause : letExpression.getClauses()) {
      visitParameters(clause.getParameters());
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, null);
      }
      clause.getTerm().accept(this, null);
    }
    letExpression.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, Void ignore) {
    return null;
  }
}
