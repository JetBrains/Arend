package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;

public class GetTypeVisitor extends BaseExpressionVisitor<Void,Expression> {
  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    Expression functionType = expr.getFunction().accept(this, null);
    if (functionType != null) {
      return functionType.applyExpressions(expr.getArguments());
    } else {
      return null;
    }
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    return expr.getDefinition().getType().subst(expr.getPolyParamsSubst());
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Void params) {
    return visitDefCall(expr, null).applyExpressions(expr.getDataTypeArguments());
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Void params) {
    return new UniverseExpression(expr.getUniverse());
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getType().accept(new SubstVisitor(new ExprSubstitution()), null);
  }

  @Override
  public Expression visitLam(LamExpression expr, Void params) {
    Expression bodyType = expr.getBody().accept(this, null);
    return bodyType != null ? new PiExpression(expr.getParameters(), bodyType) : null;
  }

  private TypeUniverse getDependentTypeUniverse(DependentTypeExpression expr) {
    DependentLink link = expr.getParameters();
    TypeUniverse universe = null;

    while (link.hasNext()) {
      if (!(link instanceof UntypedDependentLink)) {
        UniverseExpression type = link.getType().accept(this, null).toUniverse();
        if (type == null) return null;
        if (universe == null) {
          universe = type.getUniverse();
        } else {
          //Universe.CompareResult cmp = universe.compare(type.getUniverse());
          //if (cmp == null) return null;
          universe = universe.max(type.getUniverse());
        }
      }
      link = link.getNext();
    }

    if (expr instanceof PiExpression) {
      Expression type = ((PiExpression) expr).getCodomain().accept(this, null);
      if (type == null || universe == null) {
        return null;
      }
      type = type.normalize(NormalizeVisitor.Mode.WHNF);
      TypeUniverse codomainUniverse = type.toUniverse().getUniverse();
      if (codomainUniverse == null) return null;
      universe = codomainUniverse.equals(TypeUniverse.PROP) ? TypeUniverse.PROP : new TypeUniverse(universe.getPLevel().max(codomainUniverse.getPLevel()), codomainUniverse.getHLevel());
    }

    return universe;
  }

  private UniverseExpression visitDependentType(DependentTypeExpression expr) {
    TypeUniverse universe = getDependentTypeUniverse(expr);
    return universe == null ? null : new UniverseExpression(universe);
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    return visitDependentType(expr);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    return visitDependentType(expr);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return new UniverseExpression(expr.getUniverse().succ());
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return new ErrorExpression(expr.getExpr() != null ? expr.getExpr().accept(this, null) : null, expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    return expr.getType();
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void ignored) {
    Expression type = expr.getExpression().accept(this, null);
    if (type == null) {
      return null;
    }

    SigmaExpression sigma = type.normalize(NormalizeVisitor.Mode.WHNF).toSigma();
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
    return params.getType().subst(subst);
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return expr.getExpression();
  }

  @Override
  public Expression visitLet(LetExpression expr, Void params) {
    Expression type = expr.getExpression().accept(this, null);
    return type != null ? new LetExpression(expr.getClauses(), type) : null;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getType();
  }
}
