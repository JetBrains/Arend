package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.params;

public class GetTypeVisitor extends BaseExpressionVisitor<Void, TypeMax> {
  @Override
  public TypeMax visitApp(AppExpression expr, Void params) {
    TypeMax functionType = expr.getFunction().accept(this, null);
    if (functionType == null) {
      return null;
    }
    functionType = functionType.normalize(NormalizeVisitor.Mode.WHNF);
    return functionType instanceof ErrorExpression ? functionType : functionType.applyExpressions(expr.getArguments());
  }

  @Override
  public TypeMax visitDefCall(DefCallExpression expr, Void params) {
    List<DependentLink> defParams = new ArrayList<>();
    TypeMax type = expr.getDefinition().getTypeWithParams(defParams, expr.getPolyArguments());
    assert expr.getDefCallArguments().size() == defParams.size();
    return type.subst(DependentLink.Helper.toSubstitution(defParams, expr.getDefCallArguments()), new LevelSubstitution(expr.getDefinition().getPolyParams(), expr.getPolyArguments().getLevels()));
  }

  @Override
  public TypeMax visitConCall(ConCallExpression expr, Void params) {
    List<DependentLink> defParams = new ArrayList<>();
    TypeMax type = expr.getDefinition().getTypeWithParams(defParams, expr.getPolyArguments());
    assert expr.getDataTypeArguments().size() + expr.getDefCallArguments().size() == defParams.size();
    ExprSubstitution subst = DependentLink.Helper.toSubstitution(defParams, expr.getDataTypeArguments());
    defParams = defParams.subList(expr.getDataTypeArguments().size(), defParams.size());
    subst.add(DependentLink.Helper.toSubstitution(defParams, expr.getDefCallArguments()));
    return type.subst(subst, new LevelSubstitution());
  }

  @Override
  public TypeMax visitClassCall(ClassCallExpression expr, Void params) {
    return expr.getSorts().toType().subst(new ExprSubstitution(), expr.getPolyArguments().toLevelSubstitution(expr.getDefinition()));
  }

  @Override
  public TypeMax visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getType().subst(new ExprSubstitution(), new LevelSubstitution());
  }

  @Override
  public TypeMax visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : expr.getVariable().getType();
  }

  @Override
  public TypeMax visitLam(LamExpression expr, Void ignored) {
    TypeMax bodyType = expr.getBody().accept(this, null);
    if (bodyType instanceof Expression) {
      return new PiExpression(expr.getParameters(), (Expression) bodyType);
    } else
    if (bodyType instanceof PiUniverseType) {
      return new PiUniverseType(params(DependentLink.Helper.clone(expr.getParameters()), bodyType.getPiParameters()), ((PiUniverseType) bodyType).getSorts());
    } else {
      return null;
    }
  }

  private SortMax getSorts(TypeMax type) {
    if (type instanceof Expression) {
      UniverseExpression universeType = ((Expression) type).normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
      if (universeType != null) {
        return new SortMax(universeType.getSort());
      }
    } else
    if (type instanceof PiUniverseType) {
      if (!type.getPiParameters().hasNext()) {
        return ((PiUniverseType) type).getSorts();
      }
    }
    return null;
  }

  private TypeMax visitDependentType(DependentTypeExpression expr) {
    SortMax codomain = null;
    if (expr instanceof PiExpression) {
      Expression codomainExpr = ((PiExpression) expr).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
      if (codomainExpr instanceof ErrorExpression) {
        return codomainExpr;
      }
      codomain = getSorts(codomainExpr.accept(this, null));
      if (codomain == null) {
        return null;
      }

      if (codomain.getHLevel().isMinimum()) {
        return new UniverseExpression(Sort.PROP);
      }
    }

    SortMax sorts = new SortMax();
    for (DependentLink link = expr.getParameters(); link.hasNext(); link = link.getNext()) {
      if (!(link instanceof UntypedDependentLink)) {
        Expression typeExpr = link.getType().toExpression();
        if (typeExpr == null) {
          return null;
        }
        typeExpr = typeExpr.normalize(NormalizeVisitor.Mode.WHNF);
        if (typeExpr instanceof ErrorExpression) {
          return typeExpr;
        }
        SortMax sorts1 = getSorts(typeExpr.accept(this, null));
        if (sorts1 == null) {
          return null;
        }
        sorts.add(sorts1);
      }
    }

    if (codomain != null) {
      sorts = new SortMax(sorts.getPLevel().max(codomain.getPLevel()), codomain.getHLevel());
    }

    return new PiUniverseType(EmptyDependentLink.getInstance(), sorts);
  }

  @Override
  public TypeMax visitPi(PiExpression expr, Void params) {
    return visitDependentType(expr);
  }

  @Override
  public TypeMax visitSigma(SigmaExpression expr, Void params) {
    return visitDependentType(expr);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return new UniverseExpression(expr.getSort().succ());
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    Expression expr1 = null;
    if (expr.getExpr() != null) {
      TypeMax type = expr.getExpr().accept(this, null);
      if (type instanceof Expression) {
        expr1 = (Expression) type;
      }
    }
    return new ErrorExpression(expr1, expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    return expr.getType();
  }

  @Override
  public TypeMax visitProj(ProjExpression expr, Void ignored) {
    TypeMax type = expr.getExpression().accept(this, null);
    if (!(type instanceof Expression)) {
      return null;
    }

    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    if (type instanceof ErrorExpression) {
      return type;
    }

    SigmaExpression sigma = ((Expression) type).toSigma();
    if (sigma == null) return null;
    DependentLink params = sigma.getParameters();
    if (expr.getField() == 0) {
      return params.getType();
    }

    ExprSubstitution subst = new ExprSubstitution();
    for (int i = 0; i < expr.getField(); i++) {
      if (!params.hasNext()) {
        return null;
      }
      subst.add(params, new ProjExpression(expr.getExpression(), i));
      params = params.getNext();
    }
    return params.getType().subst(subst, new LevelSubstitution());
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return expr.getExpression();
  }

  @Override
  public TypeMax visitLet(LetExpression expr, Void params) {
    TypeMax type = expr.getExpression().accept(this, null);
    if (type == null) {
      return null;
    }
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    if (type instanceof ErrorExpression) {
      return type;
    }
    return expr.getType(type);
  }

  @Override
  public TypeMax visitOfType(OfTypeExpression expr, Void params) {
    return expr.getType();
  }
}
