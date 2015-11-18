package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.Utils;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.*;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.patternMultipleMatch;

public class NormalizeVisitor extends BaseExpressionVisitor<Expression> {
  public enum Mode { WHNF, NF, TOP }

  private final Mode myMode;
  private final List<Binding> myContext;

  public NormalizeVisitor(Mode mode, List<Binding> context) {
    myContext = context;
    myMode = mode;
  }

  private Expression visitApps(Expression expr, List<ArgumentExpression> exprs) {
    List<Argument> args = new ArrayList<>();
    expr = expr.lamSplitAt(exprs.size(), args);
    int numberOfLambdas = args.size();

    if (numberOfLambdas > 0) {
      if (numberOfLambdas < exprs.size()) {
        ArgumentExpression[] exprs1 = new ArgumentExpression[exprs.size() - numberOfLambdas];
        List<Expression> exprs2 = new ArrayList<>(numberOfLambdas);
        for (int i = 0; i < exprs.size() - numberOfLambdas; ++i) {
          exprs1[i] = exprs.get(exprs.size() - numberOfLambdas - 1 - i);
        }
        for (int i = exprs.size() - numberOfLambdas; i < exprs.size(); ++i) {
          exprs2.add(exprs.get(i).getExpression());
        }
        expr = Apps(expr.subst(exprs2, 0), exprs1);
        return myMode == Mode.TOP ? expr : expr.accept(this);
      } else {
        List<Expression> jexprs = new ArrayList<>(exprs.size());
        for (ArgumentExpression expr1 : exprs) {
          jexprs.add(expr1.getExpression());
        }
        expr = expr.subst(jexprs, 0);
        return myMode == Mode.TOP ? expr : expr.accept(this);
      }
    }

    if (expr instanceof DefCallExpression) {
      return visitDefCall((DefCallExpression) expr, exprs);
    } else if (expr instanceof IndexExpression && ((IndexExpression) expr).getIndex() < myContext.size()) {
      Binding binding = getBinding(myContext, ((IndexExpression) expr).getIndex());
      if (binding != null && binding instanceof Function) {
        return visitFunctionCall((Function) binding, expr, exprs);
      }
    }

    if (myMode == Mode.TOP) return null;
    if (myMode == Mode.NF) {
      expr = expr.accept(this);
    }
    for (int i = exprs.size() - 1; i >= 0; --i) {
      if (myMode == Mode.NF) {
        expr = Apps(expr, new ArgumentExpression(exprs.get(i).getExpression().accept(this), exprs.get(i).isExplicit(), exprs.get(i).isHidden()));
      } else {
        expr = Apps(expr, exprs.get(i));
      }
    }
    return expr;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    List<ArgumentExpression> exprs = new ArrayList<>();
    return visitApps(expr.getFunctionArgs(exprs), exprs);
  }

  private Expression addLambdas(List<? extends TypeArgument> args1, int drop, Expression expr) {
    List<Argument> arguments = new ArrayList<>();
    int j = 0, i = 0;
    if (i < drop) {
      for (; j < args1.size(); ++j) {
        if (args1.get(j) instanceof TelescopeArgument) {
          List<String> names = ((TelescopeArgument) args1.get(j)).getNames();
          i += names.size();
          if (i > drop) {
            arguments.add(Tele(names.subList(i - drop, names.size()), args1.get(j).getType()));
            ++j;
            break;
          }
          if (i == drop) {
            ++j;
            break;
          }
        } else {
          if (++i == drop) {
            ++j;
            break;
          }
        }
      }
    }
    for (; j < args1.size(); ++j) {
      if (args1.get(j) instanceof TelescopeArgument) {
        arguments.add(args1.get(j));
      } else {
        arguments.add(Tele(args1.get(j).getExplicit(), vars("x"), args1.get(j).getType()));
      }
    }

    return arguments.isEmpty() ? expr : Lam(arguments, expr);
  }

  public Expression applyDefCall(Expression defCallExpr, List<ArgumentExpression> args) {
    if (myMode == Mode.TOP) return null;

    Expression expr = defCallExpr;
    for (int i = args.size() - 1; i >= 0; --i) {
      if (myMode == Mode.NF) {
        expr = Apps(expr, new ArgumentExpression(args.get(i).getExpression().accept(this), args.get(i).isExplicit(), args.get(i).isHidden()));
      } else {
        expr = Apps(expr, args.get(i));
      }
    }
    return expr;
  }

  public Expression visitDefCall(DefCallExpression defCallExpr, List<ArgumentExpression> args) {
    if (defCallExpr.getDefinition().hasErrors()) {
      return myMode == Mode.TOP ? null : applyDefCall(defCallExpr, args);
    }

    if (defCallExpr.getDefinition() instanceof ClassField) {
      if (args.isEmpty()) {
        if (myMode == Mode.TOP) {
          return null;
        }
        return FieldCall((ClassField) defCallExpr.getDefinition());
      }

      ArgumentExpression thisArg = args.get(args.size() - 1);
      Expression thisType = thisArg.getExpression().getType(myContext);
      if (thisType == null) {
        assert false;
      } else {
        thisType = thisType.normalize(Mode.WHNF, myContext);
        if (thisType instanceof ClassCallExpression) {
          ClassCallExpression.ImplementStatement elem = ((ClassCallExpression) thisType).getImplementStatements().get(defCallExpr.getDefinition());
          if (elem != null && elem.term != null) {
            if (myMode == Mode.TOP) {
              Collections.reverse(args);
              return Apps(elem.term, args.subList(1, args.size()).toArray(new ArgumentExpression[args.size() - 1]));
            } else {
              return visitApps(elem.term, args.subList(0, args.size() - 1));
            }
          }
        }
      }
    }

    if (defCallExpr.getDefinition() instanceof Function) {
      return visitFunctionCall((Function) defCallExpr.getDefinition(), defCallExpr, args);
    }

    if (myMode == Mode.TOP) return null;

    if (defCallExpr instanceof ClassCallExpression || defCallExpr instanceof FieldCallExpression) {
      return applyDefCall(defCallExpr, args);
    }

    List<TypeArgument> arguments;
    if (defCallExpr instanceof DataCallExpression) {
      DataDefinition dataDefinition = ((DataCallExpression) defCallExpr).getDefinition();
      if (dataDefinition.getThisClass() == null) {
        arguments = dataDefinition.getParameters();
      } else {
        arguments = new ArrayList<>(dataDefinition.getParameters().size() + 1);
        arguments.add(TypeArg(ClassCall(dataDefinition.getThisClass())));
        arguments.addAll(dataDefinition.getParameters());
      }
    } else
    if (defCallExpr instanceof ConCallExpression) {
      List<TypeArgument> arguments1 = ((Constructor) defCallExpr.getDefinition()).getArguments();
      List<Expression> parameters = ((ConCallExpression) defCallExpr).getParameters();
      if (!parameters.isEmpty()) {
        List<Expression> substExprs = new ArrayList<>(parameters);
        Collections.reverse(substExprs);
        arguments = new ArrayList<>(arguments1.size());
        for (int j = 0; j < arguments1.size(); ++j) {
          int num;
          if (arguments1.get(j) instanceof TelescopeArgument) {
            arguments.add(Tele(arguments1.get(j).getExplicit(), ((TelescopeArgument) arguments1.get(j)).getNames(), arguments1.get(j).getType().subst(substExprs, 0)));
            num = ((TelescopeArgument) arguments1.get(j)).getNames().size();
          } else {
            arguments.add(TypeArg(arguments1.get(j).getExplicit(), arguments1.get(j).getType().subst(substExprs, j)));
            num = 1;
          }

          for (int i = 0; i < substExprs.size(); ++i) {
            substExprs.set(i, substExprs.get(i).liftIndex(0, num));
          }
        }
      } else {
        arguments = arguments1;
      }
    } else {
      throw new IllegalStateException();
    }

    List<TypeArgument> splitArguments = new ArrayList<>();
    splitArguments(arguments, splitArguments);
    if (myMode == Mode.WHNF && splitArguments.size() >= args.size()) {
      return applyDefCall(defCallExpr, args);
    }

    Expression result = defCallExpr;
    for (int i = args.size() - 1; i >= 0; --i) {
      Expression arg = args.get(i).getExpression();
      if (myMode == Mode.NF) {
        arg = arg.accept(this);
      }
      if (splitArguments.size() > args.size()) {
        arg = arg.liftIndex(0, splitArguments.size() - args.size());
      }
      result = Apps(result, arg != args.get(i).getExpression() ? new ArgumentExpression(arg, args.get(i).isExplicit(), args.get(i).isHidden()) : args.get(i));
    }
    for (int i = splitArguments.size() - args.size() - 1; i >= 0; --i) {
      result = Apps(result, new ArgumentExpression(Index(i), splitArguments.get(splitArguments.size() - 1 - i).getExplicit(), !splitArguments.get(splitArguments.size() - 1 - i).getExplicit()));
    }
    return addLambdas(arguments, args.size(), result);
  }

  private Expression visitFunctionCall(Function func, Expression defCallExpr, List<ArgumentExpression> args) {
    if (func instanceof FunctionDefinition && func.equals(Prelude.COERCE) && args.size() == 3
      && Apps(args.get(2).getExpression().liftIndex(0, 1), Index(0)).accept(new NormalizeVisitor(Mode.NF, myContext)).liftIndex(0, -1) != null) {
      return myMode == Mode.TOP ? args.get(1).getExpression() : args.get(1).getExpression().accept(this);
    }

    Expression result = func.getTerm();
    List<TypeArgument> args1 = new ArrayList<>();
    ClassDefinition thisClass = func.getThisClass();
    if (thisClass != null) {
      args1.add(TypeArg(ClassCall(thisClass)));
    }
    splitArguments(getFunctionType(func), args1, myContext);
    int numberOfArgs = numberOfVariables(args1);
    if (myMode == Mode.WHNF && numberOfArgs > args.size() || result == null) {
      return applyDefCall(defCallExpr, args);
    }

    List<Expression> args2 = new ArrayList<>(numberOfArgs);
    int numberOfSubstArgs = numberOfVariables(func.getArguments()) + (thisClass != null ? 1 : 0);
    for (int i = numberOfArgs - numberOfSubstArgs; i < numberOfArgs - args.size(); ++i) {
      args2.add(Index(i));
    }
    for (int i = args.size() - Math.min(numberOfSubstArgs, args.size()); i < args.size(); ++i) {
      args2.add(args.get(i).getExpression().liftIndex(0, numberOfArgs > args.size() ? numberOfArgs - args.size() : 0));
    }

    Abstract.Definition.Arrow arrow = func.getArrow();
    while (result instanceof ElimExpression) {
      List<Expression> exprs = new ArrayList<>();
      List<List<Pattern>> patterns = new ArrayList<>();
      for (Expression expr : ((ElimExpression) result).getExpressions()) {
        exprs.add(expr.subst(args2, 0));
        patterns.add(new ArrayList<Pattern>());
      }
      for (Clause clause : ((ElimExpression) result).getClauses()) {
        for (int i = 0; i < clause.getPatterns().size(); i++)
          patterns.get(i).add(clause.getPatterns().get(i));
      }
      List<Integer> validClauses = patternMultipleMatch(patterns, exprs, myContext, ((ElimExpression) result).getClauses().size());
      if (validClauses.isEmpty() && func == Prelude.AT && ((ElimExpression) result).getClauses().size() == 3) {
        validClauses = Collections.singletonList(2);
      }
      if (!validClauses.isEmpty()) {
        Clause clauseOK = ((ElimExpression) result).getClauses().get(validClauses.get(0));
        for (int i = 0; i < ((ElimExpression) result).getExpressions().size(); i++) {
          Utils.PatternMatchOKResult matchOKResult = (Utils.PatternMatchOKResult) clauseOK.getPatterns().get(i).match(exprs.get(i), myContext);

          int var = ((ElimExpression) result).getExpressions().get(i).getIndex();
          args2.remove(var);
          Collections.reverse(matchOKResult.expressions);
          args2.addAll(var, matchOKResult.expressions);
        }
        result = clauseOK.getExpression();
        arrow = clauseOK.getArrow();
        continue;
      }
      return applyDefCall(defCallExpr, args);
    }

    result = result.liftIndex(0, numberOfArgs > args.size() ? numberOfArgs - args.size() : 0).subst(args2, 0);
    if (arrow == Abstract.Definition.Arrow.LEFT) {
      try (ContextSaver ignore = new ContextSaver(myContext)) {
        for (TypeArgument arg : args1) {
          pushArgument(myContext, arg);
        }
        result = result.normalize(Mode.TOP, myContext);
      }
      if (result == null) {
        return applyDefCall(defCallExpr, args);
      }
    }

    for (int i = args.size() - Math.min(numberOfSubstArgs, args.size()) - 1; i >= 0; --i) {
      result = Apps(result, args.get(i));
    }

    for (int i = numberOfArgs - Math.max(numberOfSubstArgs, args.size()) - 1; i >= 0; --i) {
      result = Apps(result, Index(i));
    }
    if (args.size() < numberOfArgs) {
      result = addLambdas(args1, args.size(), result);
    }

    return myMode == Mode.TOP ? result : result.accept(this);
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return visitDefCall(expr, Collections.<ArgumentExpression>emptyList());
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr) {
    if (myMode == Mode.TOP) return null;
    if (myMode == Mode.WHNF) return expr;

    Map<ClassField, ClassCallExpression.ImplementStatement> statements = new HashMap<>();
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      statements.put(elem.getKey(), new ClassCallExpression.ImplementStatement(elem.getValue().type == null ? null : elem.getValue().type.accept(this), elem.getValue().term == null ? null : elem.getValue().term.accept(this)));
    }

    return ClassCall(expr.getDefinition(), statements);
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    if (myMode == Mode.TOP)
      return null;
    Binding binding = getBinding(myContext, expr.getIndex());
    if (binding != null && binding instanceof Function) {
      return visitFunctionCall((Function) binding, expr, new ArrayList<ArgumentExpression>());
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    try (ContextSaver saver = new ContextSaver(myContext)) {
      return myMode == Mode.TOP ? null : myMode == Mode.NF ? Lam(visitArguments(expr.getArguments()), expr.getBody().accept(this)) : expr;
    }
  }

  private List<Argument> visitArguments(List<Argument> arguments) {
    List<Argument> result = new ArrayList<>(arguments.size());
    for (Argument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        result.add(new TelescopeArgument(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), ((TelescopeArgument) argument).getType().accept(this)));
      } else {
        if (argument instanceof TypeArgument) {
          result.add(new TypeArgument(argument.getExplicit(), ((TypeArgument) argument).getType().accept(this)));
        } else {
          result.add(argument);
        }
      }
      pushArgument(myContext, argument);
    }
    return result;
  }

  private List<TypeArgument> visitTypeArguments(List<TypeArgument> arguments) {
    List<TypeArgument> result = new ArrayList<>(arguments.size());
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        result.add(new TelescopeArgument(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), argument.getType().accept(this)));
      } else {
        result.add(new TypeArgument(argument.getExplicit(), argument.getType().accept(this)));
      }
      pushArgument(myContext, argument);
    }
    return result;
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    try (ContextSaver saver = new ContextSaver(myContext)) {
      return myMode == Mode.TOP ? null : myMode == Mode.NF ? Pi(visitTypeArguments(expr.getArguments()), expr.getCodomain().accept(this)) : expr;
    }
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr) {
    return myMode == Mode.TOP ? null : myMode != Mode.NF || expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this), expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr) {
    if (myMode == Mode.TOP) return null;
    if (myMode != Mode.NF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this));
    }
    return Tuple(fields, expr.getType());
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    try (ContextSaver saver = new ContextSaver(myContext)) {
      return myMode == Mode.TOP ? null : myMode == Mode.NF ? Sigma(visitTypeArguments(expr.getArguments())) : expr;
    }
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    throw new IllegalStateException();
  }

  @Override
  public Expression visitProj(ProjExpression expr) {
    Expression exprNorm = expr.getExpression().normalize(Mode.WHNF, myContext);
    if (exprNorm instanceof TupleExpression) {
      Expression result = ((TupleExpression) exprNorm).getFields().get(expr.getField());
      return myMode == Mode.TOP ? result : result.accept(this);
    } else {
      return myMode == Mode.TOP ? null : myMode == Mode.NF ? Proj(expr.getExpression().accept(this), expr.getField()) : expr;
    }
  }

  @Override
  public Expression visitNew(NewExpression expr) {
    return myMode == Mode.TOP ? null : myMode == Mode.WHNF ? expr : New(expr.getExpression().accept(this));
  }

  @Override
  public Expression visitLet(LetExpression letExpression) {
    try (ContextSaver ignore = new ContextSaver(myContext)) {
      for (LetClause clause : letExpression.getClauses()) {
        myContext.add(clause);
      }

      Expression term = letExpression.getExpression().accept(this);
      if (term.liftIndex(0, -letExpression.getClauses().size()) != null)
        return term.liftIndex(0, -letExpression.getClauses().size());
      else
        return Let(letExpression.getClauses(), term);
    }
  }
}
