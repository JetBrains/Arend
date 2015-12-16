package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.*;

public class NormalizeVisitor extends BaseExpressionVisitor<NormalizeVisitor.Mode, Expression> implements ElimTreeNodeVisitor<List<Expression>, LeafElimTreeNode> {
  @Override
  public LeafElimTreeNode visitBranch(BranchElimTreeNode branchNode, List<Expression> subst) {
    List<Expression> arguments = new ArrayList<>();
    Expression func = subst.get(branchNode.getIndex()).normalize(Mode.WHNF, myContext).getFunction(arguments);
    if (func instanceof ConCallExpression) {
      ElimTreeNode child = branchNode.getChild(((ConCallExpression) func).getDefinition());
      if (child == null)
        return null;
      subst.remove(branchNode.getIndex());
      subst.addAll(branchNode.getIndex(), arguments);
      return child.accept(this, subst);
    }
    // @
    ElimTreeNode child = branchNode.getChild(null);
    if (child != null)
      return child.accept(this, subst);
    return null;
  }

  @Override
  public LeafElimTreeNode visitLeaf(LeafElimTreeNode leafNode, List<Expression> subst) {
    return leafNode;
  }

  @Override
  public LeafElimTreeNode visitEmpty(EmptyElimTreeNode emptyNode, List<Expression> params) {
    return null;
  }

  public enum Mode { WHNF, NF, TOP }

  private final List<Binding> myContext;

  public NormalizeVisitor(List<Binding> context) {
    myContext = context;
  }

  private Expression visitApps(Expression expr, List<ArgumentExpression> exprs, Mode mode) {
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
        return mode == Mode.TOP ? expr : expr.accept(this, mode);
      } else {
        List<Expression> jexprs = new ArrayList<>(exprs.size());
        for (ArgumentExpression expr1 : exprs) {
          jexprs.add(expr1.getExpression());
        }
        expr = expr.subst(jexprs, 0);
        return mode == Mode.TOP ? expr : expr.accept(this, mode);
      }
    }

    if (expr instanceof DefCallExpression) {
      return visitDefCall((DefCallExpression) expr, exprs, mode);
    } else if (expr instanceof IndexExpression && ((IndexExpression) expr).getIndex() < myContext.size()) {
      Binding binding = getBinding(myContext, ((IndexExpression) expr).getIndex());
      if (binding != null && binding instanceof Function) {
        return visitFunctionCall((Function) binding, expr, exprs, mode);
      }
    }

    if (mode == Mode.TOP) return null;
    if (mode == Mode.NF) {
      expr = expr.accept(this, mode);
    }
    for (int i = exprs.size() - 1; i >= 0; --i) {
      if (mode == Mode.NF) {
        expr = Apps(expr, new ArgumentExpression(exprs.get(i).getExpression().accept(this, mode), exprs.get(i).isExplicit(), exprs.get(i).isHidden()));
      } else {
        expr = Apps(expr, exprs.get(i));
      }
    }
    return expr;
  }

  @Override
  public Expression visitApp(AppExpression expr, Mode mode) {
    List<ArgumentExpression> exprs = new ArrayList<>();
    return visitApps(expr.getFunctionArgs(exprs), exprs, mode);
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

  public Expression applyDefCall(Expression defCallExpr, List<ArgumentExpression> args, Mode mode) {
    if (mode == Mode.TOP) return null;

    Expression expr = defCallExpr;
    for (int i = args.size() - 1; i >= 0; --i) {
      if (mode == Mode.NF) {
        expr = Apps(expr, new ArgumentExpression(args.get(i).getExpression().accept(this, mode), args.get(i).isExplicit(), args.get(i).isHidden()));
      } else {
        expr = Apps(expr, args.get(i));
      }
    }
    return expr;
  }

  public Expression visitDefCall(DefCallExpression defCallExpr, List<ArgumentExpression> args, Mode mode) {
    if (defCallExpr.getDefinition().hasErrors()) {
      return mode == Mode.TOP ? null : applyDefCall(defCallExpr, args, mode);
    }

    if (defCallExpr.getDefinition() instanceof ClassField) {
      if (args.isEmpty()) {
        if (mode == Mode.TOP) {
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
            if (mode == Mode.TOP) {
              Collections.reverse(args);
              return Apps(elem.term, args.subList(1, args.size()).toArray(new ArgumentExpression[args.size() - 1]));
            } else {
              return visitApps(elem.term, args.subList(0, args.size() - 1), mode);
            }
          }
        }
      }
    }

    if (defCallExpr.getDefinition() instanceof Function) {
      return visitFunctionCall((Function) defCallExpr.getDefinition(), defCallExpr, args, mode);
    } else if (defCallExpr instanceof ConCallExpression) {
      return visitConstructorCall((ConCallExpression) defCallExpr, args, mode);
    }

    if (mode == Mode.TOP) return null;

    if (defCallExpr instanceof ClassCallExpression || defCallExpr instanceof FieldCallExpression) {
      return applyDefCall(defCallExpr, args, mode);
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
    } else {
      throw new IllegalStateException();
    }

    List<TypeArgument> splitArguments = splitArguments(arguments);
    if (mode == Mode.WHNF && splitArguments.size() >= args.size()) {
      return applyDefCall(defCallExpr, args, mode);
    }

    Expression result = defCallExpr;
    for (int i = args.size() - 1; i >= 0; --i) {
      Expression arg = args.get(i).getExpression();
      if (mode == Mode.NF) {
        arg = arg.accept(this, mode);
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

  private Expression visitConstructorCall(ConCallExpression conCallExpression, List<ArgumentExpression> args, Mode mode) {
    int take = conCallExpression.getDefinition().getNumberOfAllParameters() - conCallExpression.getParameters().size();
    if (take > 0) {
      int to = args.size() - take;
      if (to < 0) {
        to = 0;
      }
      List<Expression> parameters = new ArrayList<>(conCallExpression.getParameters().size() + args.size() - to);
      parameters.addAll(conCallExpression.getParameters());
      for (int i = args.size() - 1; i >= to; --i) {
        parameters.add(args.get(i).getExpression());
      }
      args = args.subList(0, to);
      conCallExpression = ConCall(conCallExpression.getDefinition(), parameters);
    }
    // TODO: what if the list of parameters is still incomplete?

    int numberOfSubstArgs = numberOfVariables(conCallExpression.getDefinition().getArguments());
    List<TypeArgument> args1 = new ArrayList<>();
    List<ArgumentExpression> argsToSubst = new ArrayList<>(args);
    Collections.reverse(argsToSubst);
    splitArguments((numberOfSubstArgs == 0 ? conCallExpression : Apps(conCallExpression, argsToSubst.toArray(new ArgumentExpression[argsToSubst.size()]))).getType(myContext), args1, myContext);
    args1.subList(Math.max(0, numberOfSubstArgs - args.size()), args1.size()).clear();
    if (mode == Mode.WHNF && !args1.isEmpty()) {
      return applyDefCall(conCallExpression, args, mode);
    }

    List<Expression> args2 = completeArgs(args, numberOfSubstArgs);
    args2.addAll(conCallExpression.getParameters());
    Collections.reverse(args2.subList(numberOfSubstArgs, args2.size()));

    if (conCallExpression.getDefinition().getDataType().getCondition(conCallExpression.getDefinition()) == null) {
      return applyDefCall(conCallExpression, args, mode);
    }

    LeafElimTreeNode leaf = conCallExpression.getDefinition().getDataType().getCondition(conCallExpression.getDefinition()).getElimTree().accept(this, args2);
    if (leaf == null)
      return applyDefCall(conCallExpression, args, mode);
    Expression result = leaf.getExpression().subst(args2, 0);

    result = bindExcessiveArgs(args, result, args1, numberOfSubstArgs);

    return mode == Mode.TOP ? result : result.accept(this, mode);
  }

  private Expression visitFunctionCall(Function func, Expression defCallExpr, List<ArgumentExpression> args, Mode mode) {
    if (func instanceof FunctionDefinition && func.equals(Prelude.COERCE) && args.size() == 3
      && Apps(args.get(2).getExpression().liftIndex(0, 1), Index(0)).accept(this, Mode.NF).liftIndex(0, -1) != null) {
      return mode == Mode.TOP ? args.get(1).getExpression() : args.get(1).getExpression().accept(this, mode);
    }

    int numberOfSubstArgs = numberOfVariables(func.getArguments()) + (func.getThisClass() != null ? 1 : 0);
    List<TypeArgument> args1 = new ArrayList<>();
    List<ArgumentExpression> argsToSubst = new ArrayList<>(args);
    Collections.reverse(argsToSubst);
    splitArguments((numberOfSubstArgs == 0 ? defCallExpr : Apps(defCallExpr, argsToSubst.toArray(new ArgumentExpression[argsToSubst.size()]))).getType(myContext), args1, myContext);
    args1.subList(Math.max(numberOfSubstArgs - args.size(), 0), args1.size()).clear();

    if (mode == Mode.WHNF && !args1.isEmpty() || func.getElimTree() == null) {
      return applyDefCall(defCallExpr, args, mode);
    }

    List<Expression> args2 = completeArgs(args, numberOfSubstArgs);

    LeafElimTreeNode leaf = func.getElimTree().accept(this, args2);
    if (leaf == null)
      return applyDefCall(defCallExpr, args, mode);
    Expression result = leaf.getExpression().liftIndex(args2.size(), args1.size()).subst(args2, 0);
    if (leaf.getArrow() == Abstract.Definition.Arrow.LEFT) {
      try (ContextSaver ignore = new ContextSaver(myContext)) {
        for (TypeArgument arg : args1) {
          pushArgument(myContext, arg);
        }
        result = result.normalize(Mode.TOP, myContext);
      }
      if (result == null) {
        return applyDefCall(defCallExpr, args, mode);
      }
    }

    result = bindExcessiveArgs(args, result, args1, numberOfSubstArgs);

    return mode == Mode.TOP ? result : result.accept(this, mode);
  }

  private Expression bindExcessiveArgs(List<ArgumentExpression> args, Expression result, List<TypeArgument> argTypes, int numberOfSubstArgs) {
    for (int i = args.size() - numberOfSubstArgs - 1; i >= 0; --i) {
      result = Apps(result, args.get(i));
    }

    if (!argTypes.isEmpty()) {
      return Lam(new ArrayList<Argument>(argTypes), result);
    }

    return result;
  }

  private List<Expression> completeArgs(List<ArgumentExpression> args, int numberOfSubstArgs) {
    List<Expression> args2 = new ArrayList<>(numberOfSubstArgs);
    for (int i = 0; i < numberOfSubstArgs - args.size(); ++i) {
      args2.add(Index(i));
    }
    for (int i = args.size() - Math.min(numberOfSubstArgs, args.size()); i < args.size(); ++i) {
      args2.add(args.get(i).getExpression().liftIndex(0, numberOfSubstArgs > args.size() ? numberOfSubstArgs - args.size() : 0));
    }
    return args2;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Mode mode) {
    return visitDefCall(expr, Collections.<ArgumentExpression>emptyList(), mode);
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
  public Expression visitIndex(IndexExpression expr, Mode mode) {
    if (mode == Mode.TOP)
      return null;
    Binding binding = getBinding(myContext, expr.getIndex());
    if (binding != null && binding instanceof Function) {
      return visitFunctionCall((Function) binding, expr, new ArrayList<ArgumentExpression>(), mode);
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitLam(LamExpression expr, Mode mode) {
    try (ContextSaver ignore = new ContextSaver(myContext)) {
      return mode == Mode.TOP ? null : mode == Mode.NF ? Lam(visitArguments(expr.getArguments(), mode), expr.getBody().accept(this, mode)) : expr;
    }
  }

  private List<Argument> visitArguments(List<Argument> arguments, Mode mode) {
    List<Argument> result = new ArrayList<>(arguments.size());
    for (Argument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        result.add(new TelescopeArgument(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), ((TelescopeArgument) argument).getType().accept(this, mode)));
      } else {
        if (argument instanceof TypeArgument) {
          result.add(new TypeArgument(argument.getExplicit(), ((TypeArgument) argument).getType().accept(this, mode)));
        } else {
          result.add(argument);
        }
      }
      pushArgument(myContext, argument);
    }
    return result;
  }

  private List<TypeArgument> visitTypeArguments(List<TypeArgument> arguments, Mode mode) {
    List<TypeArgument> result = new ArrayList<>(arguments.size());
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        result.add(new TelescopeArgument(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), argument.getType().accept(this, mode)));
      } else {
        result.add(new TypeArgument(argument.getExplicit(), argument.getType().accept(this, mode)));
      }
      pushArgument(myContext, argument);
    }
    return result;
  }

  @Override
  public Expression visitPi(PiExpression expr, Mode mode) {
    try (ContextSaver ignore = new ContextSaver(myContext)) {
      return mode == Mode.TOP ? null : mode == Mode.NF ? Pi(visitTypeArguments(expr.getArguments(), mode), expr.getCodomain().accept(this, mode)) : expr;
    }
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : mode != Mode.NF || expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, mode), expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Mode mode) {
    if (mode == Mode.TOP) return null;
    if (mode != Mode.NF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, mode));
    }
    return Tuple(fields, expr.getType());
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Mode mode) {
    try (ContextSaver ignore = new ContextSaver(myContext)) {
      return mode == Mode.TOP ? null : mode == Mode.NF ? Sigma(visitTypeArguments(expr.getArguments(), mode)) : expr;
    }
  }

  @Override
  public Expression visitProj(ProjExpression expr, Mode mode) {
    Expression exprNorm = expr.getExpression().normalize(Mode.WHNF, myContext);
    if (exprNorm instanceof TupleExpression) {
      Expression result = ((TupleExpression) exprNorm).getFields().get(expr.getField());
      return mode == Mode.TOP ? result : result.accept(this, mode);
    } else {
      return mode == Mode.TOP ? null : mode == Mode.NF ? Proj(expr.getExpression().accept(this, mode), expr.getField()) : expr;
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : mode == Mode.WHNF ? expr : New(expr.getExpression().accept(this, mode));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Mode mode) {
    try (ContextSaver ignore = new ContextSaver(myContext)) {
      for (LetClause clause : letExpression.getClauses()) {
        myContext.add(clause);
      }

      Expression term = letExpression.getExpression().accept(this, mode);
      if (term.liftIndex(0, -letExpression.getClauses().size()) != null)
        return term.liftIndex(0, -letExpression.getClauses().size());
      else
        return Let(letExpression.getClauses(), term);
    }
  }
}
