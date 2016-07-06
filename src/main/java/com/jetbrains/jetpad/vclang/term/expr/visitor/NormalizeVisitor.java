package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.typechecking.normalization.Normalizer;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

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
      return myNormalizer.normalize(lamFun, expr.getArguments(), expr.getFlags(), mode);
    }

    if (fun.toDefCall() != null) {
      return visitDefCallExpr(expr, mode);
    } else {
      ReferenceExpression ref = fun.toReference();
      if (ref != null) {
        Binding binding = ref.getBinding();
        if (binding instanceof Function) {
          return visitFunctionCall((Function) binding, expr, mode);
        }
      }
    }

    if (mode == Mode.TOP) return null;
    Expression newExpr = fun.accept(this, Mode.TOP);
    if (newExpr != null) {
      newExpr = newExpr.addArguments(expr.getArguments(), expr.getFlags());
      return newExpr.accept(this, mode);
    }

    return applyDefCall(expr, mode);
  }

  public Expression applyDefCall(Expression expr, Mode mode) {
    if (mode == Mode.TOP) return null;
    AppExpression appExpr = expr.toApp();
    if (appExpr != null && (mode == Mode.NF || mode == Mode.HUMAN_NF)) {
      List<Expression> newArgs = new ArrayList<>(expr.getArguments().size());
      for (Expression argument : expr.getArguments()) {
        newArgs.add(argument.accept(this, mode));
      }
      return new AppExpression(expr.getFunction(), newArgs, appExpr.getFlags());
    } else {
      return expr;
    }
  }

  public Expression visitDefCallExpr(Expression expr, Mode mode) {
    DefCallExpression defCallExpr = expr.getFunction().toDefCall();
    if (defCallExpr.getDefinition().hasErrors()) {
      return mode == Mode.TOP ? null : applyDefCall(expr, mode);
    }

    if (defCallExpr.getDefinition() instanceof ClassField) {
      if (expr.getArguments().isEmpty()) {
        assert false;
        if (mode == Mode.TOP) {
          return null;
        }
        return FieldCall((ClassField) defCallExpr.getDefinition());
      }

      Expression thisExpr = expr.getArguments().get(0).normalize(Mode.WHNF);
      ClassCallExpression classCall = thisExpr.getType().normalize(Mode.WHNF).toClassCall();
      if (classCall != null) {
        ClassCallExpression.ImplementStatement elem = classCall.getImplementStatements().get(defCallExpr.getDefinition());
        Expression impl = null;
        if (elem != null) {
          impl = elem.term;
        }
        if (impl == null) {
          ClassDefinition.FieldImplementation fieldImpl = classCall.getDefinition().getFieldImpl((ClassField) defCallExpr.getDefinition());
          if (fieldImpl.isImplemented()) {
            impl = fieldImpl.implementation.subst(fieldImpl.thisParameter, thisExpr);
          }
        }
        if (impl != null) {
          List<? extends EnumSet<AppExpression.Flag>> flags = expr.toApp().getFlags();
          Expression result = Apps(impl, expr.getArguments().subList(1, expr.getArguments().size()), flags.subList(1, flags.size()));
          return mode == Mode.TOP ? result : result.accept(this, mode);
        }
      }
    }

    if (defCallExpr.toConCall() != null) {
      return visitConstructorCall(expr, mode);
    }
    if (defCallExpr.getDefinition() instanceof Function) {
      return visitFunctionCall((Function) defCallExpr.getDefinition(), expr, mode);
    }

    return mode == Mode.TOP ? null : applyDefCall(expr, mode);
  }

  private Expression visitConstructorCall(Expression expr, Mode mode) {
    ConCallExpression conCallExpression = expr.getFunction().toConCall();
    List<? extends Expression> args = expr.getArguments();
    int take = DependentLink.Helper.size(conCallExpression.getDefinition().getDataTypeParameters()) - conCallExpression.getDataTypeArguments().size();
    if (take > 0) {
      if (take >= args.size()) {
        take = args.size();
      }
      List<Expression> parameters = new ArrayList<>(conCallExpression.getDataTypeArguments().size() + take);
      parameters.addAll(conCallExpression.getDataTypeArguments());
      for (int i = 0; i < take; i++) {
        parameters.add(args.get(i));
      }
      conCallExpression = ConCall(conCallExpression.getDefinition(), parameters);
      int size = args.size();
      args = args.subList(take, size);
      if (!args.isEmpty()) {
        expr = Apps(conCallExpression, args, expr.toApp().getFlags().subList(take, size));
      } else {
        expr = conCallExpression;
      }
    }

    return visitFunctionCall(conCallExpression.getDefinition(), expr, mode);
  }

  private Expression visitFunctionCall(Function func, Expression expr, Mode mode) {
    List<? extends Expression> args = expr.getArguments();
    List<? extends EnumSet<AppExpression.Flag>> flags = Collections.emptyList();
    List<? extends Expression> requiredArgs;
    DependentLink excessiveParams;
    int numberOfRequiredArgs = func.getNumberOfRequiredArguments();
    if (numberOfRequiredArgs > args.size()) {
      excessiveParams = DependentLink.Helper.subst(DependentLink.Helper.get(func.getParameters(), args.size()), new Substitution());
      List<Expression> requiredArgs1 = new ArrayList<>();
      for (DependentLink link = excessiveParams; link.hasNext(); link = link.getNext()) {
        requiredArgs1.add(Reference(link));
      }
      List<Expression> requiredArgs2 = new ArrayList<>(args.size() + requiredArgs1.size());
      requiredArgs2.addAll(args);
      requiredArgs2.addAll(requiredArgs1);
      requiredArgs = requiredArgs2;
      args = Collections.emptyList();
    } else {
      excessiveParams = EmptyDependentLink.getInstance();
      requiredArgs = args.subList(0, func.getNumberOfRequiredArguments());
      AppExpression app = expr.toApp();
      if (app != null) {
        flags = app.getFlags().subList(numberOfRequiredArgs, args.size());
      }
      args = args.subList(numberOfRequiredArgs, args.size());
    }

    DependentLink params = EmptyDependentLink.getInstance();
    List<? extends Expression> paramArgs = Collections.<Expression>emptyList();
    ConCallExpression conCall = expr.getFunction().toConCall();
    if (conCall != null) {
      params = conCall.getDefinition().getDataTypeParameters();
      paramArgs = conCall.getDataTypeArguments();
    }
    Expression result = myNormalizer.normalize(func, params, paramArgs, requiredArgs, args, flags, mode);
    if (result == null) {
      return applyDefCall(expr, mode);
    }

    return excessiveParams.hasNext() ? Lam(excessiveParams, result) : result;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Mode mode) {
    return visitDefCallExpr(expr, mode);
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Mode mode) {
    if (mode == Mode.TOP) return null;
    if (mode == Mode.WHNF) return expr;

    Map<ClassField, ClassCallExpression.ImplementStatement> statements = new HashMap<>();
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      statements.put(elem.getKey(), new ClassCallExpression.ImplementStatement(elem.getValue().type == null ? null : elem.getValue().type.accept(this, mode), elem.getValue().term == null ? null : elem.getValue().term.accept(this, mode)));
    }

    return ClassCall(expr.getDefinition(), statements);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Mode mode) {
    if (mode == Mode.TOP) {
      return null;
    }
    Binding binding = expr.getBinding();
    if (binding instanceof Function) {
      return visitFunctionCall((Function) binding, expr, mode);
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitLam(LamExpression expr, Mode mode) {
    if (mode == Mode.TOP) {
      return null;
    }
    if (mode == Mode.HUMAN_NF) {
      Substitution substitution = new Substitution();
      return Lam(DependentLink.Helper.accept(expr.getParameters(), substitution, this, mode), expr.getBody().subst(substitution).accept(this, mode));
    }
    if (mode == Mode.NF) {
      return Lam(expr.getParameters(), expr.getBody().accept(this, mode));
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
      Substitution substitution = new Substitution();
      return Pi(DependentLink.Helper.accept(expr.getParameters(), substitution, this, mode), expr.getCodomain().subst(substitution).accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Mode mode) {
    if (mode == Mode.TOP) return null;
    /*if ((mode == Mode.NF || mode == Mode.HUMAN_NF)) {
      return ((TypeUniverse) expr.getUniverse()).getLevel() != null ? Universe(((TypeUniverse) expr.getUniverse()).getLevel().getValue().accept(this, mode)) : expr;
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
    return mode == Mode.TOP ? null : mode == Mode.NF || mode == Mode.HUMAN_NF ? Sigma(DependentLink.Helper.accept(expr.getParameters(), this, mode)) : expr;
  }

  @Override
  public Expression visitProj(ProjExpression expr, Mode mode) {
    TupleExpression exprNorm = expr.getExpression().normalize(Mode.WHNF).toTuple();
    if (exprNorm != null) {
      Expression result = exprNorm.getFields().get(expr.getField());
      return mode == Mode.TOP ? result : result.accept(this, mode);
    } else {
      return mode == Mode.TOP ? null : mode == Mode.NF || mode == Mode.HUMAN_NF ? Proj(expr.getExpression().accept(this, mode), expr.getField()) : expr;
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : mode == Mode.WHNF ? expr : New(expr.getExpression().accept(this, mode));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Mode mode) {
    return myNormalizer.normalize(letExpression);
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Mode mode) {
    return mode == Mode.NF ? new OfTypeExpression(expr.getExpression().accept(this, mode), expr.getType()) : expr.getExpression().accept(this, mode);
  }

  @Override
  public Expression visitLevel(LevelExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : expr;
  }
}
