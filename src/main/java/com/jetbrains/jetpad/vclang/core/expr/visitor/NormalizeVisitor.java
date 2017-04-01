package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.normalization.Normalizer;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NormalizeVisitor extends BaseExpressionVisitor<NormalizeVisitor.Mode, Expression>  {
  private final Normalizer myNormalizer;

  public NormalizeVisitor(Normalizer normalizer) {
    myNormalizer = normalizer;
  }

  public enum Mode { WHNF, NF, HUMAN_NF }

  @Override
  public Expression visitApp(AppExpression expr, Mode mode) {
    Expression funNorm = expr.getFunction().accept(this, Mode.WHNF);
    LamExpression lamFun = funNorm.toLam();
    if (lamFun != null) {
      return myNormalizer.normalize(lamFun, expr.getArguments(), mode);
    }

    if (mode != Mode.NF) {
      return funNorm.addArguments(expr.getArguments());
    }

    funNorm = funNorm.accept(this, Mode.NF);
    for (Expression arg : expr.getArguments()) {
      funNorm = funNorm.addArgument(arg.accept(this, mode));
    }

    return funNorm;
  }

  private Expression applyDefCall(CallableCallExpression expr, Mode mode) {
    if (expr.getDefCallArguments().isEmpty() || (mode != Mode.NF && mode != Mode.HUMAN_NF)) {
      return (Expression) expr;
    }

    if (expr instanceof FieldCallExpression) {
      return ExpressionFactory.FieldCall((ClassField) expr.getDefinition(), ((FieldCallExpression) expr).getExpression().accept(this, mode));
    }

    if (expr instanceof FunCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new FunCallExpression((FunctionDefinition) expr.getDefinition(), ((FunCallExpression) expr).getSortArgument(), args);
    }

    if (expr instanceof DataCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new DataCallExpression((DataDefinition) expr.getDefinition(), ((DataCallExpression) expr).getSortArgument(), args);
    }

    if (expr instanceof ConCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new ConCallExpression((Constructor) expr.getDefinition(), ((ConCallExpression) expr).getSortArgument(), new ArrayList<>(((ConCallExpression) expr).getDataTypeArguments()), args);
    }

    if (expr instanceof LetClauseCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new LetClauseCallExpression(((LetClauseCallExpression) expr).getLetClause(), args);
    }

    throw new IllegalStateException();
  }

  private Expression visitConstructorCall(ConCallExpression expr, Mode mode) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments());
    int take = DependentLink.Helper.size(expr.getDefinition().getDataTypeParameters()) - expr.getDataTypeArguments().size();
    if (take > 0) {
      if (take >= args.size()) {
        take = args.size();
      }
      List<Expression> parameters = new ArrayList<>(expr.getDataTypeArguments().size() + take);
      parameters.addAll(expr.getDataTypeArguments());
      for (int i = 0; i < take; i++) {
        parameters.add(args.get(i));
      }
      expr = new ConCallExpression(expr.getDefinition(), expr.getSortArgument(), parameters, args.subList(take, args.size()));
    }

    return visitCallableCall(expr, expr.getSortArgument().toLevelSubstitution(), mode);
  }

  private Expression visitCallableCall(CallableCallExpression expr, LevelSubstitution polySubst, Mode mode) {
    DependentLink params = EmptyDependentLink.getInstance();
    List<? extends Expression> paramArgs = Collections.<Expression>emptyList();
    if (expr instanceof ConCallExpression) {
      params = ((ConCallExpression) expr).getDefinition().getDataTypeParameters();
      paramArgs = ((ConCallExpression) expr).getDataTypeArguments();
    }

    Expression result = myNormalizer.normalize((Function) expr.getDefinition(), polySubst, params, paramArgs, expr.getDefCallArguments(), mode);

    if (Thread.interrupted()) {
      throw new ComputationInterruptedException();
    }

    if (result == null) {
      return applyDefCall(expr, mode);
    }

    return result;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Mode mode) {
    if (!expr.getDefinition().status().bodyIsOK()) {
      return applyDefCall(expr, mode);
    }

    if (expr instanceof FieldCallExpression) {
      Expression thisExpr = ((FieldCallExpression) expr).getExpression().normalize(Mode.WHNF);
      if (thisExpr.toInferenceReference() == null || !(thisExpr.toInferenceReference().getVariable() instanceof TypeClassInferenceVariable)) {
        Expression type = thisExpr.getType();
        if (type != null) {
          ClassCallExpression classCall = type.normalize(Mode.WHNF).toClassCall();
          if (classCall != null) {
            FieldSet.Implementation impl = classCall.getFieldSet().getImplementation((ClassField) expr.getDefinition());
            if (impl != null) {
              return impl.substThisParam(thisExpr).accept(this, mode);
            }
          }
        }
      }
    }

    if (expr.toConCall() != null) {
      return visitConstructorCall(expr.toConCall(), mode);
    }
    if (expr.getDefinition() instanceof Function) {
      return visitCallableCall(expr, expr.getSortArgument().toLevelSubstitution(), mode);
    }

    return applyDefCall(expr, mode);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Mode mode) {
    if (mode == Mode.WHNF) return expr;

    FieldSet fieldSet = FieldSet.applyVisitorToImplemented(expr.getFieldSet(), expr.getDefinition().getFieldSet(), this, mode);
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), fieldSet);
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Mode mode) {
    return (DataCallExpression) applyDefCall(expr, mode);
  }

  @Override
  public Expression visitLetClauseCall(LetClauseCallExpression expr, Mode mode) {
    return visitCallableCall(expr, LevelSubstitution.EMPTY, mode);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Mode mode) {
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Mode mode) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, mode) : expr;
  }

  @Override
  public Expression visitLam(LamExpression expr, Mode mode) {
    if (mode == Mode.HUMAN_NF) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = DependentLink.Helper.subst(expr.getParameters(), substitution);
      for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
        link1 = link1.getNextTyped(null);
        link1.setType(link1.getType().normalize(mode));
      }
      return new LamExpression(expr.getResultSort(), link, expr.getBody().subst(substitution).accept(this, mode));
    }
    if (mode == Mode.NF) {
      return new LamExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Mode mode) {
    if (mode == Mode.HUMAN_NF || mode == Mode.NF) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = DependentLink.Helper.subst(expr.getParameters(), substitution);
      for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
        link1 = link1.getNextTyped(null);
        link1.setType(link1.getType().normalize(mode));
      }
      return new PiExpression(expr.getResultSort(), link, expr.getCodomain().subst(substitution).accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Mode mode) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Mode mode) {
    return mode != Mode.NF && mode != Mode.HUMAN_NF || expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, mode), expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Mode mode) {
    if (mode != Mode.NF && mode != Mode.HUMAN_NF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, mode));
    }
    return new TupleExpression(fields, expr.getSigmaType());
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Mode mode) {
    if (mode != Mode.NF && mode != Mode.HUMAN_NF) {
      return expr;
    }

    DependentLink link = DependentLink.Helper.subst(expr.getParameters(), new ExprSubstitution());
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      link1 = link1.getNextTyped(null);
      link1.setType(link1.getType().normalize(mode));
    }
    return new SigmaExpression(expr.getSort(), link);
  }

  @Override
  public Expression visitProj(ProjExpression expr, Mode mode) {
    TupleExpression exprNorm = expr.getExpression().normalize(Mode.WHNF).toTuple();
    if (exprNorm != null) {
      return exprNorm.getFields().get(expr.getField()).accept(this, mode);
    } else {
      return mode == Mode.NF || mode == Mode.HUMAN_NF ? new ProjExpression(expr.getExpression().accept(this, mode), expr.getField()) : expr;
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, Mode mode) {
    return mode == Mode.WHNF ? expr : new NewExpression(visitClassCall(expr.getExpression(), mode));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Mode mode) {
    return myNormalizer.normalize(letExpression);
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Mode mode) {
    return mode == Mode.NF ? new OfTypeExpression(expr.getExpression().accept(this, mode), expr.getTypeOf()) : expr.getExpression().accept(this, mode);
  }

}
