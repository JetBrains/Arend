package org.arend.core.expr.visitor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.StdLevelSubstitution;
import org.arend.error.IncorrectExpressionException;
import org.arend.prelude.Prelude;

import java.util.ArrayList;
import java.util.List;

import static org.arend.core.expr.ExpressionFactory.Nat;

public class GetTypeVisitor implements ExpressionVisitor<Void, Expression> {
  public final static GetTypeVisitor INSTANCE = new GetTypeVisitor(true);
  public final static GetTypeVisitor NN_INSTANCE = new GetTypeVisitor(false);

  private final boolean myNormalizing;

  private GetTypeVisitor(boolean normalizing) {
    myNormalizing = normalizing;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    Expression result = expr.getFunction().accept(this, null).applyExpression(expr.getArgument(), myNormalizing);
    if (result == null && myNormalizing) {
      throw new IncorrectExpressionException("Expression " + expr.getFunction() + " does not have a pi type, but is applied to " + expr.getArgument());
    }
    return result;
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Void params) {
    List<DependentLink> defParams = new ArrayList<>();
    Expression type = expr.getDefinition().getTypeWithParams(defParams, expr.getSortArgument());
    assert expr.getDefCallArguments().size() == defParams.size();
    return type.subst(DependentLink.Helper.toSubstitution(defParams, expr.getDefCallArguments()));
  }

  @Override
  public UniverseExpression visitDataCall(DataCallExpression expr, Void params) {
    return new UniverseExpression(expr.getDefinition().getSort().subst(new StdLevelSubstitution(expr.getSortArgument())));
  }

  private Expression normalizeFieldCall(FieldCallExpression expr) {
    Expression arg = expr.getArgument();
    Expression type;
    if (arg instanceof FieldCallExpression) {
      arg = normalizeFieldCall((FieldCallExpression) arg);
      return arg instanceof NewExpression ? ((NewExpression) arg).getImplementation(expr.getDefinition()) : null;
    } else {
      type = arg.accept(this, null);
      if (type == null) {
        return null;
      }
      ClassCallExpression classCall = type.cast(ClassCallExpression.class);
      return classCall == null ? null : classCall.getImplementation(expr.getDefinition(), arg);
    }
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    if (expr.getArgument() instanceof FieldCallExpression) {
      Expression norm = normalizeFieldCall(expr);
      if (norm != null) {
        return norm.accept(this, null);
      }
    }

    Expression type = expr.getArgument().accept(this, null);
    if (type != null) {
      ClassCallExpression classCall = type.cast(ClassCallExpression.class);
      if (classCall != null) {
        if (!(expr.getArgument() instanceof FieldCallExpression)) {
          Expression impl = classCall.getImplementation(expr.getDefinition(), expr.getArgument());
          if (impl != null) {
            return impl.accept(this, null);
          }
        }
        PiExpression fieldType = classCall.getDefinition().getOverriddenType(expr.getDefinition(), expr.getSortArgument());
        if (fieldType != null) {
          return fieldType.applyExpression(expr.getArgument());
        }
      }
    }
    return expr.getDefinition().getType(expr.getSortArgument()).applyExpression(expr.getArgument());
  }

  @Override
  public DataCallExpression visitConCall(ConCallExpression expr, Void params) {
    return expr.getDefinition().getDataTypeExpression(expr.getSortArgument(), expr.getDataTypeArguments());
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Void params) {
    return new UniverseExpression(expr.getSort().subst(expr.getSortArgument().toLevelSubstitution()));
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getTypeExpr().copy();
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : expr.getVariable().getType();
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    Expression type = expr.getExpression().accept(this, null);
    return type == null ? null : SubstExpression.make(type, expr.getSubstitution());
  }

  @Override
  public Expression visitLam(LamExpression expr, Void ignored) {
    return new PiExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, null));
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    return new UniverseExpression(expr.getResultSort());
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    return new UniverseExpression(expr.getSort());
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return new UniverseExpression(expr.getSort().succ());
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpression() == null ? expr : new ErrorExpression(expr.getExpression().accept(this, null), expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    return expr.getSigmaType();
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void ignored) {
    Expression type = expr.getExpression().accept(this, null);
    if (myNormalizing) {
      type = type.normalize(NormalizeVisitor.Mode.WHNF);
    }
    type = type.getUnderlyingExpression();
    if (type instanceof ErrorExpression) {
      return type;
    }

    if (!(type instanceof SigmaExpression)) {
      if (myNormalizing) {
        throw new IncorrectExpressionException("Expression " + expr + " should have a sigma type");
      } else {
        return null;
      }
    }
    DependentLink params = ((SigmaExpression) type).getParameters();
    if (expr.getField() == 0) {
      return params.getTypeExpr();
    }

    ExprSubstitution subst = new ExprSubstitution();
    for (int i = 0; i < expr.getField(); i++) {
      subst.add(params, ProjExpression.make(expr.getExpression(), i));
      params = params.getNext();
    }
    return params.getTypeExpr().subst(subst);
  }

  @Override
  public ClassCallExpression visitNew(NewExpression expr, Void params) {
    return expr.getType();
  }

  @Override
  public Expression visitLet(LetExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    return expr.getResultType().subst(DependentLink.Helper.toSubstitution(expr.getParameters(), expr.getArguments()));
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getTypeOf();
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Void params) {
    return Nat();
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    Expression normExpr = expr.eval();
    if (normExpr == null) {
      return null;
    }

    Expression type = expr.getExpression().accept(this, null);
    Sort sortArg = type == null ? null : type.getSortOfType();
    if (sortArg == null) {
      return null;
    }

    List<Expression> args = new ArrayList<>(3);
    args.add(type);
    args.add(expr.getExpression());
    args.add(normExpr);
    return new FunCallExpression(Prelude.PATH_INFIX, sortArg, args);
  }
}
