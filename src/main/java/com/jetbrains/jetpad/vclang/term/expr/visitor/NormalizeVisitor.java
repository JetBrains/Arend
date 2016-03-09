package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class NormalizeVisitor extends BaseExpressionVisitor<NormalizeVisitor.Mode, Expression>  {
  public enum Mode { WHNF, NF, HUMAN_NF, TOP }

  @Override
  public Expression visitApp(AppExpression expr, Mode mode) {
    Expression fun = expr.getFunction();
    if (fun instanceof LamExpression) {
      int i = 0;
      DependentLink link = ((LamExpression) fun).getParameters();
      Substitution subst = new Substitution();
      while (link.hasNext() && i < expr.getArguments().size()) {
        subst.add(link, expr.getArguments().get(i++));
        link = link.getNext();
      }
      fun = ((LamExpression) fun).getBody();
      if (link.hasNext()) {
        fun = Lam(link, fun);
      }
      fun = fun.subst(subst);
      fun = fun.addArguments(expr.getArguments().subList(i, expr.getArguments().size()), expr.getFlags().subList(i, expr.getFlags().size()));
      return mode == Mode.TOP ? fun : fun.accept(this, mode);
    }

    if (fun instanceof DefCallExpression) {
      return visitDefCallExpr(expr, mode);
    } else
    if (fun instanceof ReferenceExpression) {
      Binding binding = ((ReferenceExpression) fun).getBinding();
      if (binding instanceof Function) {
        return visitFunctionCall((Function) binding, expr, mode);
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
    if (expr instanceof AppExpression && (mode == Mode.NF || mode == Mode.HUMAN_NF)) {
      List<Expression> newArgs = new ArrayList<>(expr.getArguments().size());
      for (Expression argument : expr.getArguments()) {
        newArgs.add(argument.accept(this, mode));
      }
      return new AppExpression(expr.getFunction(), newArgs, ((AppExpression) expr).getFlags());
    } else {
      return expr;
    }
  }

  public Expression visitDefCallExpr(Expression expr, Mode mode) {
    DefCallExpression defCallExpr = (DefCallExpression) expr.getFunction();
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

      Expression thisArg = expr.getArguments().get(0);
      Expression thisType = thisArg.getType();
      if (thisType == null) {
        assert false;
      } else {
        thisType = thisType.normalize(Mode.WHNF);
        if (thisType instanceof ClassCallExpression) {
          ClassCallExpression.ImplementStatement elem = ((ClassCallExpression) thisType).getImplementStatements().get(defCallExpr.getDefinition());
          if (elem != null && elem.term != null) {
            Expression result = Apps(elem.term, expr.getArguments().subList(1, expr.getArguments().size()), ((AppExpression) expr).getFlags().subList(1, ((AppExpression) expr).getFlags().size()));
            return mode == Mode.TOP ? result : result.accept(this, mode);
          }
        }
      }
    }

    if (defCallExpr.getDefinition() instanceof Function) {
      return visitFunctionCall((Function) defCallExpr.getDefinition(), expr, mode);
    } else if (defCallExpr instanceof ConCallExpression) {
      return visitConstructorCall(expr, mode);
    }

    if (mode == Mode.TOP) return null;

    if (defCallExpr instanceof ClassCallExpression || defCallExpr instanceof FieldCallExpression) {
      return applyDefCall(expr, mode);
    }

    if (!(defCallExpr instanceof DataCallExpression)) {
      throw new IllegalStateException();
    }

    DataDefinition dataDefinition = ((DataCallExpression) defCallExpr).getDefinition();
    DependentLink parameters = dataDefinition.getParameters();

    Expression result = applyDefCall(expr, mode);
    for (Expression ignored : expr.getArguments()) {
      parameters = parameters.getNext();
    }

    if (parameters.hasNext()) {
      parameters = DependentLink.Helper.subst(parameters, new Substitution());
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        result = result.addArgument(Reference(link), AppExpression.DEFAULT);
      }
      return Lam(parameters, result);
    } else {
      return result;
    }
  }

  private Expression visitConstructorCall(Expression expr, Mode mode) {
    ConCallExpression conCallExpression = (ConCallExpression) expr.getFunction();
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
        expr = Apps(conCallExpression, args, ((AppExpression) expr).getFlags().subList(take, size));
      } else {
        expr = conCallExpression;
      }
    }

    DependentLink excessiveParams = conCallExpression.getDefinition().getParameters();
    int i = 0;
    for (; i < args.size(); i++) {
      if (!excessiveParams.hasNext()) {
        break;
      }
      excessiveParams = excessiveParams.getNext();
    }

    if (mode == Mode.WHNF && excessiveParams.hasNext()) {
      return applyDefCall(expr, mode);
    }

    excessiveParams = DependentLink.Helper.subst(excessiveParams, new Substitution());
    List<Expression> args2 = completeArgs(args, conCallExpression.getDefinition().getParameters(), excessiveParams);


    Condition condition = conCallExpression.getDefinition().getDataType().getCondition(conCallExpression.getDefinition());
    if (condition == null) {
      return applyDefCall(expr, mode);
    }

    LeafElimTreeNode leaf = condition.getElimTree().match(args2);
    if (leaf == null) {
      return applyDefCall(expr, mode);
    }

    Substitution subst = leaf.matchedToSubst(args2);

    DependentLink link = conCallExpression.getDefinition().getDataTypeParameters();
    for (Expression argument : conCallExpression.getDataTypeArguments()) {
      subst.add(link, argument);
      link = link.getNext();
    }

    Expression result = leaf.getExpression().subst(subst);

    result = excessiveParams.hasNext() ? Lam(excessiveParams, result) : result;
    return mode == Mode.TOP ? result : result.accept(this, mode);
  }

  private Expression visitFunctionCall(Function func, Expression expr, Mode mode) {
    List<? extends Expression> args = expr.getArguments();
    if (func instanceof FunctionDefinition && Prelude.isCoe((FunctionDefinition) func) && args.size() >= 3) {
      Expression result = null;

      Binding binding = new TypedBinding("i", DataCall(Prelude.INTERVAL));
      Expression normExpr = Apps(args.get(0), Reference(binding)).accept(this, NormalizeVisitor.Mode.NF);
      if (!normExpr.findBinding(binding)) {
        result = args.get(1);
      } else {
        Expression mbIso = normExpr.getFunction();
        List<? extends Expression> mbIsoArgs = normExpr.getArguments();
        if (mbIso instanceof FunCallExpression && Prelude.isIso(((FunCallExpression) mbIso).getDefinition()) && mbIsoArgs.size() == 7) {
          boolean noFreeVar = true;
          for (int i = 0; i < mbIsoArgs.size() - 1; i++) {
            if (mbIsoArgs.get(i).findBinding(binding)) {
              noFreeVar = false;
              break;
            }
          }
          if (noFreeVar) {
            Expression normedPt = args.get(2).accept(this, Mode.NF);
            if (normedPt instanceof ConCallExpression && ((ConCallExpression) normedPt).getDefinition() == Prelude.RIGHT) {
              result = Apps(mbIsoArgs.get(2), args.get(1));
            }
          }
        }
      }

      if (result != null) {
        List<? extends EnumSet<AppExpression.Flag>> flags = ((AppExpression) expr).getFlags();
        result = Apps(result, args.subList(3, args.size()), flags.subList(3, flags.size()));
        return mode == Mode.TOP ? result : result.accept(this, mode);
      }
    }

    DependentLink excessiveParams = func.getParameters();
    int i = 0;
    for (; i < args.size(); i++) {
      if (!excessiveParams.hasNext()) {
        break;
      }
      excessiveParams = excessiveParams.getNext();
    }

    if (mode == Mode.WHNF && excessiveParams.hasNext() || func.getElimTree() == null) {
      return applyDefCall(expr, mode);
    }

    excessiveParams = DependentLink.Helper.subst(excessiveParams, new Substitution());
    List<Expression> args2 = completeArgs(args, func.getParameters(), excessiveParams);

    LeafElimTreeNode leaf = func.getElimTree().match(args2);
    if (leaf == null) {
      return applyDefCall(expr, mode);
    }

    Expression result = leaf.getExpression().subst(leaf.matchedToSubst(args2));
    if ((mode == Mode.HUMAN_NF || mode == Mode.TOP) && leaf.getArrow() == Abstract.Definition.Arrow.LEFT) {
      result = result.accept(this, Mode.TOP);
      if (result == null) {
        return applyDefCall(expr, mode);
      }
    }

    for (; i < args.size(); i++) {
      result = result.addArgument(args.get(i), ((AppExpression) expr).getFlags().get(i));
    }
    result = excessiveParams.hasNext() ? Lam(excessiveParams, result) : result;
    return mode == Mode.TOP ? result : result.accept(this, mode);
  }

  private List<Expression> completeArgs(List<? extends Expression> args, DependentLink params, DependentLink excessiveParams) {
    List<Expression> result = new ArrayList<>();
    for (Expression arg : args) {
      if (!params.hasNext()) {
        break;
      }
      result.add(arg);
      params = params.getNext();
    }
    for (; excessiveParams.hasNext(); excessiveParams = excessiveParams.getNext(), params = params.getNext()) {
      result.add(Reference(excessiveParams));
    }
    return result;
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
      return Lam(visitParameters(expr.getParameters(), substitution, mode), expr.getBody().subst(substitution).accept(this, mode));
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
      return Pi(visitParameters(expr.getParameters(), substitution, mode), expr.getCodomain().subst(substitution).accept(this, mode));
    } else {
      return expr;
    }
  }

  private DependentLink visitParameters(DependentLink link, Substitution substitution, Mode mode) {
    link = DependentLink.Helper.subst(link, substitution);
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      link1 = link1.getNextTyped(null);
      link1.setType(link1.getType().accept(this, mode));
    }
    return link;
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Mode mode) {
    return mode == Mode.TOP ? null : expr;
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
    return mode == Mode.TOP ? null : mode == Mode.NF || mode == Mode.HUMAN_NF ? Sigma(visitParameters(expr.getParameters(), new Substitution(), mode)) : expr;
  }

  @Override
  public Expression visitProj(ProjExpression expr, Mode mode) {
    Expression exprNorm = expr.getExpression().normalize(Mode.WHNF);
    if (exprNorm instanceof TupleExpression) {
      Expression result = ((TupleExpression) exprNorm).getFields().get(expr.getField());
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
    Expression term = letExpression.getExpression().accept(this, mode);
    Set<Binding> bindings = new HashSet<>();
    for (LetClause clause : letExpression.getClauses()) {
      bindings.add(clause);
    }
    return term.findBinding(bindings) ? Let(letExpression.getClauses(), term) : term;
  }
}
