package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Function;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.normalization.Normalizer;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;

public class NormalizeVisitor extends BaseExpressionVisitor<NormalizeVisitor.Mode, Expression>  {
  private final Normalizer myNormalizer;

  public NormalizeVisitor(Normalizer normalizer) {
    myNormalizer = normalizer;
  }

  public enum Mode { WHNF, NF, HUMAN_NF, TOP }

  @Override
  public Expression visitApp(AppExpression expr, Mode mode) {
    Expression fun = expr.getFunction();
    LamExpression lamFun = fun.toLam();
    if (lamFun != null) {
      return myNormalizer.normalize(lamFun, expr.getArguments(), mode);
    }

    if (fun.toDefCall() != null) {
      return visitDefCallExpr(expr, mode);
    } else {
      ReferenceExpression ref = fun.toReference();
      if (ref != null) {
        Binding binding = ref.getBinding();
        if (binding instanceof Function) {
          return visitFunctionCall((Function) binding, LevelSubstitution.EMPTY, expr, mode);
        }
      }
    }

    if (mode == Mode.TOP) return null;
    Expression newExpr = fun.accept(this, Mode.TOP);
    if (newExpr != null) {
      newExpr = newExpr.addArguments(expr.getArguments());
      return newExpr.accept(this, mode);
    }

    return applyDefCall(expr, mode);
  }

  private Expression applyDefCall(Expression expr, Mode mode) {
    if (mode == Mode.TOP) return null;
    if ((expr.toApp() != null || expr.toDefCall() != null && !expr.toDefCall().getDefCallArguments().isEmpty()) && (mode == Mode.NF || mode == Mode.HUMAN_NF)) {
      List<Expression> newArgs = expr.getArguments().isEmpty() ? Collections.<Expression>emptyList() : new ArrayList<Expression>(expr.getArguments().size());
      for (Expression argument : expr.getArguments()) {
        newArgs.add(argument.accept(this, mode));
      }

      Expression fun = expr.getFunction();
      if (fun.toFieldCall() != null) {
        fun = ExpressionFactory.FieldCall(fun.toFieldCall().getDefinition(), fun.toFieldCall().getExpression().accept(this, mode));
      }
      if (fun.toFunCall() != null && !fun.toFunCall().getDefCallArguments().isEmpty()) {
        List<Expression> args = new ArrayList<>(fun.toFunCall().getDefCallArguments().size());
        for (Expression arg : fun.toFunCall().getDefCallArguments()) {
          args.add(arg.accept(this, mode));
        }
        fun = new FunCallExpression(fun.toFunCall().getDefinition(), fun.toFunCall().getLevelArguments(), args);
      }
      if (fun.toDataCall() != null && !fun.toDataCall().getDefCallArguments().isEmpty()) {
        List<Expression> args = new ArrayList<>(fun.toDataCall().getDefCallArguments().size());
        for (Expression arg : fun.toDataCall().getDefCallArguments()) {
          args.add(arg.accept(this, mode));
        }
        fun = new DataCallExpression(fun.toDataCall().getDefinition(), fun.toDataCall().getLevelArguments(), args);
      }
      if (fun.toConCall() != null && !fun.toConCall().getDefCallArguments().isEmpty()) {
        List<Expression> args = new ArrayList<>(fun.toConCall().getDefCallArguments().size());
        for (Expression arg : fun.toConCall().getDefCallArguments()) {
          args.add(arg.accept(this, mode));
        }
        fun = new ConCallExpression(fun.toConCall().getDefinition(), fun.toConCall().getLevelArguments(), new ArrayList<>(fun.toConCall().getDataTypeArguments()), args);
      }
      return newArgs.isEmpty() ? fun : new AppExpression(fun, newArgs);
    } else {
      return expr;
    }
  }

  private Expression visitDefCallExpr(Expression expr, Mode mode) {
    DefCallExpression defCallExpr = expr.getFunction().toDefCall();
    if (!defCallExpr.getDefinition().status().bodyIsOK()) {
      return mode == Mode.TOP ? null : applyDefCall(expr, mode);
    }

    if (defCallExpr instanceof FieldCallExpression) {
      Expression thisExpr = ((FieldCallExpression) defCallExpr).getExpression().normalize(Mode.WHNF);
      TypeMax type = thisExpr.getType();
      if (type instanceof Expression) {
        ClassCallExpression classCall = ((Expression) type).normalize(Mode.WHNF).toClassCall();
        if (classCall != null) {
          FieldSet.Implementation impl = classCall.getFieldSet().getImplementation((ClassField) defCallExpr.getDefinition());
          if (impl != null) {
            Expression result = Apps(impl.substThisParam(thisExpr), expr.getArguments());
            return mode == Mode.TOP ? result : result.accept(this, mode);
          }
        }
      }
    }

    if (defCallExpr.toConCall() != null) {
      return visitConstructorCall(defCallExpr.toConCall(), mode);
    }
    if (defCallExpr.getDefinition() instanceof Function) {
      return visitFunctionCall((Function) defCallExpr.getDefinition(), defCallExpr.getLevelArguments().toLevelSubstitution(), expr, mode); //.subst(defCallExpr.getPolyParamsSubst());
    }

    return mode == Mode.TOP ? null : applyDefCall(expr, mode);
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
      expr = ConCall(expr.getDefinition(), expr.getLevelArguments(), parameters, args.subList(take, args.size()));
    }

    return visitFunctionCall(expr.getDefinition(), expr.getLevelArguments().toLevelSubstitution(), expr, mode);
  }

  private Expression visitFunctionCall(Function func, LevelSubstitution polySubst, Expression expr, Mode mode) {
    List<Expression> args = expr.getFunction().toDefCall() != null ? new ArrayList<>(expr.getFunction().toDefCall().getDefCallArguments()) : new ArrayList<Expression>(expr.getArguments().size());
    args.addAll(expr.getArguments());
    List<Expression> requiredArgs;
    DependentLink excessiveParams;
    int numberOfRequiredArgs = func.getNumberOfRequiredArguments();
    if (numberOfRequiredArgs > args.size()) {
      excessiveParams = DependentLink.Helper.subst(DependentLink.Helper.get(func.getParameters(), args.size()), new ExprSubstitution());
      if (!args.isEmpty()) {
        ExprSubstitution substitution = new ExprSubstitution();
        int i = 0;
        for (DependentLink link = func.getParameters(); i < args.size(); link = link.getNext(), i++) {
          substitution.add(link, args.get(i));
        }
        for (DependentLink link = excessiveParams; link.hasNext(); link = link.getNext()) {
          link = link.getNextTyped(null);
          link.setType(link.getType().subst(substitution, LevelSubstitution.EMPTY));
        }
      }
      requiredArgs = args;
      for (DependentLink link = excessiveParams; link.hasNext(); link = link.getNext()) {
        requiredArgs.add(ExpressionFactory.Reference(link));
      }
      args = Collections.emptyList();
    } else {
      excessiveParams = EmptyDependentLink.getInstance();
      requiredArgs = args.subList(0, func.getNumberOfRequiredArguments());
      args = args.subList(numberOfRequiredArgs, args.size());
    }

    DependentLink params = EmptyDependentLink.getInstance();
    List<? extends Expression> paramArgs = Collections.<Expression>emptyList();
    ConCallExpression conCall = expr.toConCall();
    if (conCall != null) {
      params = conCall.getDefinition().getDataTypeParameters();
      paramArgs = conCall.getDataTypeArguments();
    }
    Expression result = myNormalizer.normalize(func, polySubst, params, paramArgs, requiredArgs, args, mode);

    if (Thread.interrupted()) {
      throw new ComputationInterruptedException();
    }

    if (result == null) {
      return applyDefCall(expr, mode);
    }

    return excessiveParams.hasNext() ? ExpressionFactory.Lam(excessiveParams, result) : result;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Mode mode) {
    return visitDefCallExpr(expr, mode);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Mode mode) {
    if (mode == Mode.TOP) return null;
    if (mode == Mode.WHNF) return expr;
    return expr.applyVisitorToImplementedHere(this, mode);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Mode mode) {
    if (mode == Mode.TOP) {
      return null;
    }
    Binding binding = expr.getBinding();
    if (binding instanceof Function) {
      return visitFunctionCall((Function) binding, LevelSubstitution.EMPTY, expr, mode);
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Mode mode) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, mode) : mode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitLam(LamExpression expr, Mode mode) {
    if (mode == Mode.TOP) {
      return null;
    }
    if (mode == Mode.HUMAN_NF) {
      ExprSubstitution substitution = new ExprSubstitution();
      return ExpressionFactory.Lam(DependentLink.Helper.accept(expr.getParameters(), substitution, this, mode), expr.getBody().subst(substitution).accept(this, mode));
    }
    if (mode == Mode.NF) {
      return ExpressionFactory.Lam(expr.getParameters(), expr.getBody().accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitPi(PiExpression expr, Mode mode) {
    if (mode == Mode.TOP) {
      return null;
    }
    if (mode == Mode.HUMAN_NF || mode == Mode.NF) {
      ExprSubstitution substitution = new ExprSubstitution();
      return ExpressionFactory.Pi(DependentLink.Helper.accept(expr.getParameters(), substitution, this, mode), expr.getCodomain().subst(substitution).accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Mode mode) {
    if (mode == Mode.TOP) return null;
    /*if ((mode == Mode.NF || mode == Mode.HUMAN_NF)) {
      return ((TypeUniverse) expr.getSort()).getLevel() != null ? Universe(((TypeUniverse) expr.getSort()).getLevel().getValue().accept(this, mode)) : expr;
    } /**/
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : mode != Mode.NF && mode != Mode.HUMAN_NF || expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, mode), expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Mode mode) {
    if (mode == Mode.TOP) return null;
    if (mode != Mode.NF && mode != Mode.HUMAN_NF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, mode));
    }
    return Tuple(fields, expr.getType());
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : mode == Mode.NF || mode == Mode.HUMAN_NF ? ExpressionFactory.Sigma(DependentLink.Helper.accept(expr.getParameters(), this, mode)) : expr;
  }

  @Override
  public Expression visitProj(ProjExpression expr, Mode mode) {
    TupleExpression exprNorm = expr.getExpression().normalize(Mode.WHNF).toTuple();
    if (exprNorm != null) {
      Expression result = exprNorm.getFields().get(expr.getField());
      return mode == Mode.TOP ? result : result.accept(this, mode);
    } else {
      return mode == Mode.TOP ? null : mode == Mode.NF || mode == Mode.HUMAN_NF ? ExpressionFactory.Proj(expr.getExpression().accept(this, mode), expr.getField()) : expr;
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : mode == Mode.WHNF ? expr : ExpressionFactory.New(visitClassCall(expr.getExpression(), mode));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Mode mode) {
    return myNormalizer.normalize(letExpression);
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Mode mode) {
    return mode == Mode.NF ? new OfTypeExpression(expr.getExpression().accept(this, mode), expr.getType()) : expr.getExpression().accept(this, mode);
  }

}
