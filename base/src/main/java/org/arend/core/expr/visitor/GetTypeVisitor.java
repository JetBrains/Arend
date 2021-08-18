package org.arend.core.expr.visitor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.error.IncorrectExpressionException;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.util.SingletonList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.arend.core.expr.ExpressionFactory.*;

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
    FunctionDefinition definition = expr.getDefinition();
    List<? extends Expression> arguments = expr.getDefCallArguments();
    if (definition == Prelude.DIV_MOD || definition == Prelude.MOD) {
      Expression arg2 = arguments.get(1);
      IntegerExpression integer = arg2.cast(IntegerExpression.class);
      ConCallExpression conCall = arg2.cast(ConCallExpression.class);
      if (integer != null && !integer.isZero() || conCall != null && conCall.getDefinition() == Prelude.SUC) {
        return definition == Prelude.MOD ? Fin(arg2) : finDivModType(arg2);
      } else {
        return definition == Prelude.MOD ? Nat() : Prelude.DIV_MOD_TYPE;
      }
    }

    List<DependentLink> defParams = new ArrayList<>();
    Expression type = definition.getTypeWithParams(defParams, expr.getLevels());
    assert arguments.size() == defParams.size();
    return type.subst(DependentLink.Helper.toSubstitution(defParams, arguments));
  }

  @Override
  public UniverseExpression visitDataCall(DataCallExpression expr, Void params) {
    return new UniverseExpression(expr.getDefinition().getSort().subst(expr.getLevelSubstitution()));
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Expression type = expr.getArgument().accept(this, null);
    if (type != null) {
      if (myNormalizing) {
        type = type.normalize(NormalizationMode.WHNF);
      }
      if (type instanceof ClassCallExpression) {
        ClassCallExpression classCall = (ClassCallExpression) type;
        if (!expr.getDefinition().isProperty()) {
          Expression impl = classCall.getImplementation(expr.getDefinition(), expr.getArgument());
          if (impl != null) {
            return impl.getType();
          }
        }

        PiExpression fieldType = classCall.getDefinition().getOverriddenType(expr.getDefinition(), classCall.getLevels());
        if (fieldType != null) {
          return fieldType.applyExpression(expr.getArgument());
        }
        return expr.getDefinition().getType(classCall.getLevels(expr.getDefinition().getParentClass())).applyExpression(expr.getArgument());
      }
    }
    if (myNormalizing) {
      throw new IncorrectExpressionException("Expression " + expr.getArgument() + " does not have a class type");
    }
    return null;
  }

  @Override
  public DataCallExpression visitConCall(ConCallExpression expr, Void params) {
    if (expr.getDefinition() == Prelude.SUC) {
      int sucs = 1;
      Expression expression = expr.getDefCallArguments().get(0);
      while (expression instanceof ConCallExpression && ((ConCallExpression) expression).getDefinition() == Prelude.SUC) {
        sucs++;
        expression = ((ConCallExpression) expression).getDefCallArguments().get(0);
      }
      Expression argType = expression.accept(this, null);
      if (argType != null) {
        DataCallExpression dataCall = argType.cast(DataCallExpression.class);
        if (dataCall != null && dataCall.getDefinition() == Prelude.FIN) {
          Expression arg = dataCall.getDefCallArguments().get(0);
          for (int i = 0; i < sucs; i++) {
            arg = Suc(arg);
          }
          return new DataCallExpression(dataCall.getDefinition(), dataCall.getLevels(), new SingletonList<>(arg));
        }
      }
      return Nat();
    }
    return expr.getDefinition().getDataTypeExpression(expr.getLevels(), expr.getDataTypeArguments());
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Void params) {
    return new UniverseExpression(expr.getSort());
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getTypeExpr();
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : expr.getVariable().getType();
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    Expression type = expr.getExpression().accept(this, null);
    return type == null ? null : type.subst(expr.getSubstitution(), expr.getLevelSubstitution());
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
    return expr.getExpression() == null ? expr : expr.replaceExpression(expr.getExpression().accept(this, null));
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    return expr.getSigmaType();
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void ignored) {
    Expression type = expr.getExpression().accept(this, null);
    if (myNormalizing) {
      type = type.normalize(NormalizationMode.WHNF);
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
    List<HaveClause> clauses = new ArrayList<>(expr.getClauses().size());
    for (HaveClause clause : expr.getClauses()) {
      if (!(clause instanceof LetClause)) {
        clauses.add(clause);
      }
    }
    Expression result = expr.getExpression().accept(this, null);
    return clauses.isEmpty() ? result : new LetExpression(expr.isStrict(), clauses, result);
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
    return Fin(expr.suc());
  }

  @Override
  public Expression visitTypeCoerce(TypeCoerceExpression expr, Void params) {
    return expr.getType();
  }

  @Override
  public Expression visitArray(ArrayExpression expr, Void params) {
    Map<ClassField, Expression> implementations = new HashMap<>();
    implementations.put(Prelude.ARRAY_ELEMENTS_TYPE, expr.getElementsType());
    if (expr.getTail() == null) {
      implementations.put(Prelude.ARRAY_LENGTH, new SmallIntegerExpression(expr.getElements().size()));
    } else {
      Expression tailType = expr.getTail().accept(this, null).getUnderlyingExpression();
      Expression length = null;
      if (tailType instanceof ClassCallExpression && ((ClassCallExpression) tailType).getDefinition() == Prelude.DEP_ARRAY) {
        length = ((ClassCallExpression) tailType).getImplementationHere(Prelude.ARRAY_LENGTH, expr.getTail());
      }
      if (length == null) {
        length = FieldCallExpression.make(Prelude.ARRAY_LENGTH, expr.getTail());
      }
      length = length.getUnderlyingExpression();
      if (length instanceof IntegerExpression) {
        length = ((IntegerExpression) length).plus(expr.getElements().size());
      } else {
        for (Expression ignored : expr.getElements()) {
          length = Suc(length);
        }
      }
      implementations.put(Prelude.ARRAY_LENGTH, length);
    }
    return new ClassCallExpression(Prelude.DEP_ARRAY, expr.getLevels(), implementations, new Sort(expr.getPLevel(), expr.getHLevel().max(new Level(0))), UniverseKind.NO_UNIVERSES);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    Expression normExpr = expr.eval();
    if (normExpr == null) {
      return null;
    }

    Expression type = expr.getExpression().accept(this, null);
    Sort sort = type == null ? null : type.getSortOfType();
    if (sort == null) {
      return null;
    }

    List<Expression> args = new ArrayList<>(3);
    args.add(type);
    args.add(expr.getExpression());
    args.add(normExpr);
    return FunCallExpression.make(Prelude.PATH_INFIX, new LevelPair(sort.getPLevel(), sort.getHLevel()), args);
  }
}
