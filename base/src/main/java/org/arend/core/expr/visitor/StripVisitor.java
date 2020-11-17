package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.inference.MetaInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.pattern.Pattern;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.ListErrorReporter;
import org.arend.ext.error.LocalError;
import org.arend.prelude.Prelude;

import java.util.*;

public class StripVisitor implements ExpressionVisitor<Void, Expression> {
  private final Set<EvaluatingBinding> myBoundEvaluatingBindings;
  private ErrorReporter myErrorReporter;

  public StripVisitor() {
    myBoundEvaluatingBindings = new HashSet<>();
    myErrorReporter = null;
  }

  public StripVisitor(ErrorReporter errorReporter) {
    myBoundEvaluatingBindings = new HashSet<>();
    myErrorReporter = errorReporter;
  }

  private StripVisitor(Set<EvaluatingBinding> boundEvaluatingBindings, ErrorReporter errorReporter) {
    myBoundEvaluatingBindings = boundEvaluatingBindings;
    myErrorReporter = errorReporter;
  }

  public void setErrorReporter(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    return AppExpression.make(expr.getFunction().accept(this, null), expr.getArgument().accept(this, null), expr.isExplicit());
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }
    return FunCallExpression.make(expr.getDefinition(), expr.getSortArgument(), args);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Void params) {
    Expression it = expr;
    if (expr.getDefinition() == Prelude.SUC) {
      int n = 0;
      do {
        n++;
        List<Expression> args = ((ConCallExpression) it).getDefCallArguments();
        it = args.get(0).accept(this, null);
        args.set(0, it);
      } while (it instanceof ConCallExpression && ((ConCallExpression) it).getDefinition() == Prelude.SUC);

      return it instanceof IntegerExpression ? ((IntegerExpression) it).plus(n) : expr;
    }

    List<Expression> args;
    int recursiveParam;
    do {
      ConCallExpression conCall = (ConCallExpression) it;
      args = conCall.getDataTypeArguments();
      for (int i = 0; i < args.size(); i++) {
        args.set(i, args.get(i).accept(this, null));
      }

      args = conCall.getDefCallArguments();
      recursiveParam = conCall.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        for (int i = 0; i < args.size(); i++) {
          args.set(i, args.get(i).accept(this, null));
        }
        return expr;
      }

      for (int i = 0; i < args.size(); i++) {
        if (i != recursiveParam) {
          args.set(i, args.get(i).accept(this, null));
        }
      }

      it = args.get(recursiveParam);
    } while (it instanceof ConCallExpression);

    args.set(recursiveParam, it.accept(this, null));
    return expr;
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Void params) {
    List<Expression> args = expr.getDefCallArguments();
    for (int i = 0; i < args.size(); i++) {
      args.set(i, args.get(i).accept(this, null));
    }
    return expr;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    NewExpression newExpr = expr.getArgument().cast(NewExpression.class);
    if (newExpr != null) {
      return newExpr.getImplementation(expr.getDefinition()).accept(this, null);
    } else {
      return FieldCallExpression.make(expr.getDefinition(), expr.getSortArgument(), expr.getArgument().accept(this, null));
    }
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      entry.setValue(entry.getValue().accept(this, null));
    }
    return expr;
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    Binding binding = expr.getBinding();
    if (binding instanceof EvaluatingBinding && !myBoundEvaluatingBindings.contains(binding)) {
      return ((EvaluatingBinding) binding).getExpression().accept(this, null);
    }
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() == null) {
      if (myErrorReporter == null || expr.getVariable() instanceof MetaInferenceVariable) {
        return expr;
      } else {
        LocalError error = expr.getVariable().getErrorInfer();
        myErrorReporter.report(error);
        Expression result = new ErrorExpression(error);
        expr.setSubstExpression(result);
        return result;
      }
    } else {
      return expr.getSubstExpression().accept(this, null);
    }
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    if (expr.isMetaInferenceVariable()) {
      for (Map.Entry<Binding, Expression> entry : expr.getSubstitution().getEntries()) {
        entry.setValue(entry.getValue().accept(this, null));
      }
      return expr;
    } else {
      return expr.getSubstExpression().accept(this, null);
    }
  }

  private void visitParameters(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      link1.setType(link1.getType().strip(this));
    }
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return new LamExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, null));
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return new PiExpression(expr.getResultSort(), expr.getParameters(), expr.getCodomain().accept(this, null));
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return expr;
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public ErrorExpression visitError(ErrorExpression expr, Void params) {
    if (expr.getExpression() == null) {
      return expr;
    }
    if (expr instanceof GoalErrorExpression) {
      return expr.replaceExpression(expr.getExpression().accept(new StripVisitor(myBoundEvaluatingBindings, new ListErrorReporter(((GoalErrorExpression) expr).goalError.errors)), null));
    } else {
      return new ErrorExpression(null, expr.isGoal(), expr.useExpression());
    }
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return new TupleExpression(fields, visitSigma(expr.getSigmaType(), null));
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return ProjExpression.make(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public NewExpression visitNew(NewExpression expr, Void params) {
    return new NewExpression(expr.getRenewExpression() == null ? null : expr.getRenewExpression().accept(this, null), visitClassCall(expr.getClassCall(), null));
  }

  @Override
  public LetExpression visitLet(LetExpression expr, Void params) {
    for (HaveClause clause : expr.getClauses()) {
      clause.setExpression(clause.getExpression().accept(this, null));
      if (clause instanceof LetClause) {
        myBoundEvaluatingBindings.add((LetClause) clause);
      }
    }

    LetExpression result = new LetExpression(expr.isStrict(), expr.getClauses(), expr.getExpression().accept(this, null));
    for (HaveClause clause : expr.getClauses()) {
      if (clause instanceof LetClause) {
        myBoundEvaluatingBindings.remove(clause);
      }
    }
    return result;
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    for (int i = 0; i < expr.getArguments().size(); i++) {
      expr.getArguments().set(i, expr.getArguments().get(i).accept(this, null));
    }
    visitParameters(expr.getParameters());
    for (ElimClause<Pattern> clause : expr.getElimBody().getClauses()) {
      visitParameters(clause.getParameters());
      if (clause.getExpression() != null) {
        clause.setExpression(clause.getExpression().accept(this, null));
      }
    }
    return new CaseExpression(expr.isSCase(), expr.getParameters(), expr.getResultType().accept(this, null), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(this, null), expr.getElimBody(), expr.getArguments());
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public IntegerExpression visitInteger(IntegerExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    return new PEvalExpression(expr.getExpression().accept(this, null));
  }
}
