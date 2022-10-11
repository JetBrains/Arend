package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.PersistentEvaluatingBinding;
import org.arend.core.context.binding.inference.MetaInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.let.TypedHaveClause;
import org.arend.core.expr.let.TypedLetClause;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Sort;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.ListErrorReporter;
import org.arend.ext.error.LocalError;
import org.arend.ext.util.Pair;
import org.arend.prelude.Prelude;

import java.util.*;

public class StripVisitor implements ExpressionVisitor<Void, Expression> {
  private final Set<EvaluatingBinding> myBoundEvaluatingBindings;
  private ErrorReporter myErrorReporter;
  private boolean myEvaluateBindings;

  public StripVisitor() {
    this(null);
  }

  public StripVisitor(ErrorReporter errorReporter) {
    this(errorReporter, true);
  }

  public StripVisitor(ErrorReporter errorReporter, boolean evaluateBindings) {
    this(new HashSet<>(), errorReporter, evaluateBindings);
  }

  private StripVisitor(Set<EvaluatingBinding> boundEvaluatingBindings, ErrorReporter errorReporter, boolean evaluateBindings) {
    myBoundEvaluatingBindings = boundEvaluatingBindings;
    myErrorReporter = errorReporter;
    myEvaluateBindings = evaluateBindings;
  }

  public void setErrorReporter(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  public void setEvaluateBindings(boolean evaluateBindings) {
    myEvaluateBindings = evaluateBindings;
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
    return FunCallExpression.make(expr.getDefinition(), expr.getLevels(), args);
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
    args.replaceAll(expression -> expression.accept(this, null));
    return expr;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    if (!expr.getDefinition().isProperty()) {
      NewExpression newExpr = expr.getArgument().cast(NewExpression.class);
      if (newExpr != null) {
        return newExpr.getImplementation(expr.getDefinition()).accept(this, null);
      }
    }
    return FieldCallExpression.make(expr.getDefinition(), expr.getArgument().accept(this, null), false);
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
    if (binding instanceof EvaluatingBinding && myEvaluateBindings && !myBoundEvaluatingBindings.contains(binding)) {
      if (binding instanceof PersistentEvaluatingBinding) {
        PersistentEvaluatingBinding evaluating = (PersistentEvaluatingBinding) binding;
        evaluating.setExpression(evaluating.getExpression().accept(this, null));
        myBoundEvaluatingBindings.add(evaluating);
        return expr;
      }
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

  public void visitParameters(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      link1.setType(link1.getType().strip(this));
    }
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    expr.setResultSort(visitSort(expr.getResultSort()));
    visitParameters(expr.getParameters());
    return new LamExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, null));
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    expr.setResultSort(visitSort(expr.getResultSort()));
    visitParameters(expr.getParameters());
    return new PiExpression(expr.getResultSort(), expr.getParameters(), expr.getCodomain().accept(this, null));
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return expr;
  }

  private Sort visitSort(Sort sort) {
    return sort.getHLevel().isProp() ? Sort.PROP : sort;
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr, Void params) {
    return new UniverseExpression(visitSort(expr.getSort()));
  }

  @Override
  public ErrorExpression visitError(ErrorExpression expr, Void params) {
    if (expr.getExpression() == null) {
      return expr;
    }
    if (expr instanceof GoalErrorExpression) {
      return expr.replaceExpression(expr.getExpression().accept(new StripVisitor(myBoundEvaluatingBindings, new ListErrorReporter(((GoalErrorExpression) expr).goalError.errors), myEvaluateBindings), null));
    } else {
      return new ErrorExpression(null, expr.getGoalName(), expr.useExpression());
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
      if (clause instanceof TypedLetClause) {
        ((TypedLetClause) clause).type = null;
      } else if (clause instanceof TypedHaveClause) {
        ((TypedHaveClause) clause).type = null;
      }
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
    expr.getArguments().replaceAll(expression -> expression.accept(this, null));
    visitParameters(expr.getParameters());
    visitElimBody(expr.getElimBody());
    return new CaseExpression(expr.isSCase(), expr.getParameters(), expr.getResultType().accept(this, null), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(this, null), expr.getElimBody(), expr.getArguments());
  }

  private void visitElimBody(ElimBody body) {
    for (ElimClause<Pattern> clause : body.getClauses()) {
      visitParameters(clause.getParameters());
      if (clause.getExpression() != null) {
        clause.setExpression(clause.getExpression().accept(this, null));
      }
    }
  }

  public Body visitBody(Body body) {
    if (body instanceof IntervalElim) {
      IntervalElim intervalElim = (IntervalElim) body;
      List<IntervalElim.CasePair> cases = intervalElim.getCases();
      for (int i = 0; i < cases.size(); i++) {
        Pair<Expression, Expression> pair = cases.get(i);
        cases.set(i, new IntervalElim.CasePair(pair.proj1 == null ? null : pair.proj1.accept(this, null), pair.proj2 == null ? null : pair.proj2.accept(this, null)));
      }
      if (intervalElim.getOtherwise() != null) {
        visitElimBody(intervalElim.getOtherwise());
      }
    } else if (body instanceof Expression) {
      return ((Expression) body).accept(this, null);
    } else if (body instanceof ElimBody) {
      visitElimBody((ElimBody) body);
    } else {
      assert body == null;
    }

    return body;
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
  public Expression visitTypeConstructor(TypeConstructorExpression expr, Void params) {
    List<Expression> args = expr.getClauseArguments();
    args.replaceAll(expression -> expression.accept(this, null));
    expr.setArgument(expr.getArgument().accept(this, null));
    return expr;
  }

  @Override
  public Expression visitTypeDestructor(TypeDestructorExpression expr, Void params) {
    return new TypeDestructorExpression(expr.getDefinition(), expr.getArgument().accept(this, null));
  }

  @Override
  public Expression visitArray(ArrayExpression expr, Void params) {
    List<Expression> elements = expr.getElements();
    elements.replaceAll(expression -> expression.accept(this, null));
    return ArrayExpression.make(expr.getLevels(), expr.getElementsType().accept(this, null), elements, expr.getTail() == null ? null : expr.getTail().accept(this, null));
  }

  @Override
  public Expression visitPath(PathExpression expr, Void params) {
    Expression arg = expr.getArgument().accept(this, null);
    return new PathExpression(expr.getLevels(), expr.getArgumentType().accept(this, null), arg);
  }

  @Override
  public Expression visitAt(AtExpression expr, Void params) {
    return AtExpression.make(expr.getPathArgument().accept(this, null), expr.getIntervalArgument().accept(this, null), true);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    return new PEvalExpression(expr.getExpression().accept(this, null));
  }

  @Override
  public Expression visitBox(BoxExpression expr, Void params) {
    return new BoxExpression(expr.getExpression().accept(this, null));
  }
}
