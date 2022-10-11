package org.arend.typechecking.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.prelude.Prelude;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FreeVariablesClassifier implements ExpressionVisitor<Boolean, FreeVariablesClassifier.Result> {
  public enum Result {
    GOOD, BAD, BOTH, NONE;

    public Result add(Result other) {
      return this == other || other == NONE ? this : this == NONE ? other : BOTH;
    }
  }

  private final Set<Binding> myGoodBindings = new HashSet<>();
  private final Set<Binding> myBadBindings = new HashSet<>();

  public FreeVariablesClassifier(Binding binding) {
    myGoodBindings.add(binding);
  }

  public Result checkBinding(Binding binding) {
    Result result = binding.getTypeExpr().accept(this, true);
    if (result == Result.GOOD) {
      myGoodBindings.add(binding);
    }
    if (result == Result.BAD || result == Result.BOTH) {
      myBadBindings.add(binding);
    }
    return result;
  }

  @Override
  public Result visitApp(AppExpression expr, Boolean good) {
    Result result = expr.getFunction().accept(this, false);
    return result != Result.NONE ? result : expr.getArgument().accept(this, false);
  }

  private Result visitList(Collection<? extends Expression> arguments, boolean good) {
    Result result = Result.NONE;
    for (Expression argument : arguments) {
      result = result.add(argument.accept(this, good));
      if (result == Result.BOTH || result != Result.NONE && !good) {
        return result;
      }
    }
    return result;
  }

  @Override
  public Result visitFunCall(FunCallExpression expr, Boolean good) {
    return visitList(expr.getDefCallArguments(), good && (expr.getDefinition() == Prelude.PATH_INFIX || expr.getDefinition() == Prelude.ARRAY));
  }

  @Override
  public Result visitConCall(ConCallExpression expr, Boolean good) {
    Result result;
    Expression it = expr;
    boolean goodArg;

    do {
      expr = (ConCallExpression) it;
      goodArg = good && expr.getDefinition().getBody() == null;
      result = visitList(expr.getDataTypeArguments(), goodArg);
      if (result == Result.BOTH || result != Result.NONE && !goodArg) {
        return result;
      }

      int recursiveParam = expr.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        return result.add(visitList(expr.getDefCallArguments(), goodArg));
      }

      List<Expression> args = ((ConCallExpression) it).getDefCallArguments();
      for (int i = 0; i < args.size(); i++) {
        if (i != recursiveParam) {
          result = result.add(args.get(i).accept(this, goodArg));
          if (result == Result.BOTH || result != Result.NONE && !goodArg) {
            return result;
          }
        }
      }

      it = args.get(recursiveParam);
    } while (it instanceof ConCallExpression);

    return result.add(it.accept(this, goodArg));
  }

  @Override
  public Result visitDataCall(DataCallExpression expr, Boolean good) {
    return visitList(expr.getDefCallArguments(), good);
  }

  @Override
  public Result visitFieldCall(FieldCallExpression expr, Boolean good) {
    return expr.getArgument().accept(this, false);
  }

  @Override
  public Result visitClassCall(ClassCallExpression expr, Boolean good) {
    return visitList(expr.getImplementedHere().values(), good);
  }

  @Override
  public Result visitReference(ReferenceExpression expr, Boolean good) {
    if (myBadBindings.contains(expr.getBinding())) {
      return good ? Result.NONE : Result.BAD;
    }
    if (myGoodBindings.contains(expr.getBinding())) {
      return good ? Result.GOOD : Result.BAD;
    }
    return Result.NONE;
  }

  @Override
  public Result visitInferenceReference(InferenceReferenceExpression expr, Boolean good) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, good) : Result.NONE;
  }

  @Override
  public Result visitSubst(SubstExpression expr, Boolean good) {
    return expr.getSubstExpression().accept(this, good);
  }

  private Result visitParameters(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      Result result = link.getTypeExpr().accept(this, false);
      if (result != Result.NONE) {
        return result;
      }
    }
    return Result.NONE;
  }

  @Override
  public Result visitLam(LamExpression expr, Boolean good) {
    Result result = expr.getBody().accept(this, good);
    return result == Result.BOTH || result == Result.BAD ? result : result.add(visitParameters(expr.getParameters()));
  }

  @Override
  public Result visitPi(PiExpression expr, Boolean good) {
    Result result = expr.getCodomain().accept(this, false);
    return result != Result.NONE ? result : result.add(visitParameters(expr.getParameters()));
  }

  @Override
  public Result visitSigma(SigmaExpression expr, Boolean good) {
    return visitParameters(expr.getParameters());
  }

  @Override
  public Result visitUniverse(UniverseExpression expr, Boolean good) {
    return Result.NONE;
  }

  @Override
  public Result visitError(ErrorExpression expr, Boolean good) {
    return Result.NONE;
  }

  @Override
  public Result visitTuple(TupleExpression expr, Boolean good) {
    Result result = visitList(expr.getFields(), good);
    return result == Result.BOTH || result == Result.BAD ? result : visitParameters(expr.getSigmaType().getParameters());
  }

  @Override
  public Result visitProj(ProjExpression expr, Boolean good) {
    return expr.getExpression().accept(this, false);
  }

  @Override
  public Result visitNew(NewExpression expr, Boolean good) {
    Result result = visitClassCall(expr.getClassCall(), good);
    if (expr.getRenewExpression() == null || result == Result.BOTH || result == Result.BAD) {
      return result;
    }
    return result.add(expr.getRenewExpression().accept(this, false));
  }

  @Override
  public Result visitLet(LetExpression expr, Boolean good) {
    Result result = expr.getExpression().accept(this, good);
    if (result == Result.BOTH || result == Result.BAD) {
      return result;
    }

    for (HaveClause clause : expr.getClauses()) {
      result = result.add(clause.getExpression().accept(this, false));
      if (result == Result.BOTH || result == Result.BAD) {
        return result;
      }
    }
    return result;
  }

  @Override
  public Result visitCase(CaseExpression expr, Boolean good) {
    Result result = visitList(expr.getArguments(), false);
    if (result != Result.NONE) {
      return result;
    }
    result = expr.getResultType().accept(this, false);
    if (result != Result.NONE) {
      return result;
    }
    if (expr.getResultTypeLevel() != null) {
      result = expr.getResultTypeLevel().accept(this, false);
      if (result != Result.NONE) {
        return result;
      }
    }
    result = visitParameters(expr.getParameters());
    if (result != Result.NONE) {
      return result;
    }
    for (var clause : expr.getElimBody().getClauses()) {
      result = visitParameters(clause.getParameters());
      if (result != Result.NONE) {
        return result;
      }
      if (clause.getExpression() != null) {
        result = clause.getExpression().accept(this, false);
        if (result != Result.NONE) {
          return result;
        }
      }
    }
    return Result.NONE;
  }

  @Override
  public Result visitOfType(OfTypeExpression expr, Boolean good) {
    return expr.getExpression().accept(this, good);
  }

  @Override
  public Result visitInteger(IntegerExpression expr, Boolean good) {
    return Result.NONE;
  }

  @Override
  public Result visitTypeConstructor(TypeConstructorExpression expr, Boolean good) {
    Result result = visitList(expr.getClauseArguments(), good);
    return result != Result.NONE ? result : expr.getArgument().accept(this, good);
  }

  @Override
  public Result visitTypeDestructor(TypeDestructorExpression expr, Boolean good) {
    return expr.getArgument().accept(this, good);
  }

  @Override
  public Result visitArray(ArrayExpression expr, Boolean good) {
    Result result = visitList(expr.getElements(), good);
    if (result == Result.BOTH || result != Result.NONE && !good) {
      return result;
    }
    if (expr.getTail() != null) {
      result = expr.getTail().accept(this, good);
      if (result == Result.BOTH || result != Result.NONE && !good) {
        return result;
      }
    }
    return expr.getElementsType().accept(this, good);
  }

  @Override
  public Result visitPath(PathExpression expr, Boolean good) {
    return expr.getArgument().accept(this, good);
  }

  @Override
  public Result visitAt(AtExpression expr, Boolean params) {
    Result result = expr.getPathArgument().accept(this, false);
    return result != Result.NONE ? result : expr.getIntervalArgument().accept(this, false);
  }

  @Override
  public Result visitPEval(PEvalExpression expr, Boolean good) {
    return expr.getExpression().accept(this, good);
  }

  @Override
  public Result visitBox(BoxExpression expr, Boolean good) {
    return expr.getExpression().accept(this, good);
  }
}
