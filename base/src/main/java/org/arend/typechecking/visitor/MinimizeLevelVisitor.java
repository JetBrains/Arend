package org.arend.typechecking.visitor;

import org.arend.core.context.LinkList;
import org.arend.core.context.param.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.BaseExpressionVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;

public class MinimizeLevelVisitor extends BaseExpressionVisitor<Void, Type> {
  @Override
  public Type visitApp(AppExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitDefCall(DefCallExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitReference(ReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() == null ? null : expr.getSubstExpression().accept(this, null);
  }

  @Override
  public Type visitSubst(SubstExpression expr, Void params) {
    return expr.getSubstExpression().accept(this, null);
  }

  @Override
  public Type visitLam(LamExpression expr, Void params) {
    return null;
  }

  private Type visit(Expression expr) {
    Type type = expr.accept(this, null);
    if (type != null) return type;
    Sort sort = expr.getSortOfType();
    return sort == null ? null : new TypeExpression(expr, sort);
  }

  @Override
  public Type visitPi(PiExpression expr, Void params) {
    if (expr.getResultSort().isProp()) return expr;
    Type dom = visit(expr.getParameters().getTypeExpr());
    if (dom == null) return null;
    Type cod = visit(expr.getCodomain());
    if (cod == null) return null;
    Sort sort = new Sort(dom.getSortOfType().getPLevel().max(cod.getSortOfType().getPLevel()), cod.getSortOfType().getHLevel());
    if (sort.equals(expr.getResultSort()) || !sort.isLessOrEquals(expr.getResultSort())) {
      return expr;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    LinkList list = new LinkList();
    for (SingleDependentLink param = expr.getParameters(); param.hasNext(); param = param.getNext()) {
      SingleDependentLink newParam = param instanceof TypedSingleDependentLink ? new TypedSingleDependentLink(param.isExplicit(), param.getName(), dom, param.isHidden()) : new UntypedSingleDependentLink(param.getName());
      list.append(newParam);
      substitution.add(param, new ReferenceExpression(newParam));
    }
    return new PiExpression(sort, (SingleDependentLink) list.getFirst(), cod.getExpr().subst(substitution));
  }

  @Override
  public Type visitSigma(SigmaExpression expr, Void params) {
    if (expr.getSort().isProp()) return expr;
    Sort sort = Sort.PROP;
    LinkList list = new LinkList();
    ExprSubstitution substitution = new ExprSubstitution();
    for (DependentLink param = expr.getParameters(); param.hasNext(); param = param.getNext()) {
      DependentLink newParam;
      if (param instanceof TypedDependentLink) {
        Type type = visit(param.getTypeExpr());
        if (type == null) return null;
        sort = sort.max(type.getSortOfType());
        if (sort.equals(expr.getSort()) || !sort.isLessOrEquals(expr.getSort())) {
          return expr;
        }
        newParam = new TypedDependentLink(param.isExplicit(), param.getName(), type.subst(new SubstVisitor(substitution, LevelSubstitution.EMPTY)), param.isHidden(), EmptyDependentLink.getInstance());
      } else {
        newParam = new UntypedDependentLink(param.getName());
      }
      list.append(newParam);
      substitution.add(param, new ReferenceExpression(newParam));
    }
    return new SigmaExpression(sort, list.getFirst());
  }

  @Override
  public Type visitUniverse(UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public Type visitError(ErrorExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitTuple(TupleExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitProj(ProjExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitNew(NewExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitPEval(PEvalExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitLet(LetExpression expr, Void params) {
    return expr.getResult().accept(this, null);
  }

  @Override
  public Type visitCase(CaseExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Type visitInteger(IntegerExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitTypeCoerce(TypeCoerceExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitArray(ArrayExpression expr, Void params) {
    return null;
  }

  @Override
  public Type visitPath(PathExpression expr, Void params) {
    return null;
  }
}
