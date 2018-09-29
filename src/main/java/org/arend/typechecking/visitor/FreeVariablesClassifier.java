package org.arend.typechecking.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.prelude.Prelude;

import java.util.*;

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
    return visitList(expr.getDefCallArguments(), good && expr.getDefinition() == Prelude.PATH_INFIX);
  }

  @Override
  public Result visitConCall(ConCallExpression expr, Boolean good) {
    boolean goodArg = good && expr.getDefinition().getBody() == null;
    Result result = visitList(expr.getDataTypeArguments(), goodArg);
    return result == Result.BOTH || result != Result.NONE && !goodArg ? result : result.add(visitList(expr.getDefCallArguments(), goodArg));
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
    return visitClassCall(expr.getExpression(), good);
  }

  @Override
  public Result visitLet(LetExpression expr, Boolean good) {
    Result result = expr.getExpression().accept(this, good);
    if (result == Result.BOTH || result == Result.BAD) {
      return result;
    }

    for (LetClause clause : expr.getClauses()) {
      result = result.add(clause.getExpression().accept(this, false));
      if (result == Result.BOTH || result == Result.BAD) {
        return result;
      }
    }
    return result;
  }

  private Result visitElimTree(ElimTree elimTree) {
    Result result = visitParameters(elimTree.getParameters());
    if (result != Result.NONE) {
      return result;
    }
    if (elimTree instanceof LeafElimTree) {
      result = ((LeafElimTree) elimTree).getExpression().accept(this, false);
      if (result != Result.NONE) {
        return result;
      }
    }
    if (elimTree instanceof BranchElimTree) {
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        result = visitElimTree(entry.getValue());
        if (result != Result.NONE) {
          return result;
        }
      }
    }
    return Result.NONE;
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
    result = visitParameters(expr.getParameters());
    if (result != Result.NONE) {
      return result;
    }
    return visitElimTree(expr.getElimTree());
  }

  @Override
  public Result visitOfType(OfTypeExpression expr, Boolean good) {
    return expr.getExpression().accept(this, good);
  }

  @Override
  public Result visitInteger(IntegerExpression expr, Boolean good) {
    return Result.NONE;
  }
}
