package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.OverriddenDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class ReplaceDefCallVisitor implements ExpressionVisitor<Expression> {
  private final Definition myParent;
  private final Expression myExpression;

  public ReplaceDefCallVisitor(Definition parent, Expression expression) {
    myParent = parent;
    myExpression = expression;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    return Apps(expr.getFunction().accept(this), new ArgumentExpression(expr.getArgument().getExpression().accept(this), expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return expr.getDefinition().getParent() == myParent ? FieldAcc(myExpression, expr.getDefinition()) : expr;
  }

  @Override
  public IndexExpression visitIndex(IndexExpression expr) {
    return expr;
  }

  private <T extends Argument> List<T> visitArguments(List<T> args) {
    List<T> arguments = new ArrayList<>(args.size());
    for (T arg : args) {
      if (arg instanceof TelescopeArgument) {
        arguments.add((T) Tele(arg.getExplicit(), ((TelescopeArgument) arg).getNames(), ((TelescopeArgument) arg).getType().accept(this)));
      } else
      if (arg instanceof TypeArgument) {
        arguments.add((T) TypeArg(arg.getExplicit(), ((TypeArgument) arg).getType().accept(this)));
      } else {
        arguments.add(arg);
      }
    }
    return arguments;
  }

  @Override
  public LamExpression visitLam(LamExpression expr) {
    return Lam(visitArguments(expr.getArguments()), expr.getBody().accept(this));
  }

  @Override
  public PiExpression visitPi(PiExpression expr) {
    return Pi(visitArguments(expr.getArguments()), expr.getCodomain().accept(this));
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr) {
    return expr;
  }

  @Override
  public VarExpression visitVar(VarExpression expr) {
    return expr;
  }

  @Override
  public InferHoleExpression visitInferHole(InferHoleExpression expr) {
    return expr;
  }

  @Override
  public ErrorExpression visitError(ErrorExpression expr) {
    return expr.getExpr() == null ? expr : Error(expr.getExpr().accept(this), expr.getError());
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this));
    }
    return Tuple(fields);
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr) {
    return Sigma(visitArguments(expr.getArguments()));
  }

  private Clause visitClause(Clause clause, ElimExpression elimExpression) {
    return new Clause(clause.getConstructor(), visitArguments(clause.getArguments()), clause.getArrow(), clause.getExpression().accept(this), elimExpression);
  }

  @Override
  public ElimExpression visitElim(ElimExpression expr) {
    List<Clause> clauses = new ArrayList<>(expr.getClauses().size());
    Clause otherwise = expr.getOtherwise() == null ? null : visitClause(expr.getOtherwise(), null);
    ElimExpression elimExpression = Elim(expr.getElimType(), expr.getExpression(), clauses, otherwise);
    if (otherwise != null) {
      otherwise.setElimExpression(elimExpression);
    }
    for (Clause clause : expr.getClauses()) {
      clauses.add(visitClause(clause, elimExpression));
    }
    return elimExpression;
  }

  @Override
  public Expression visitFieldAcc(FieldAccExpression expr) {
    return FieldAcc(expr.getExpression().accept(this), expr.getField());
  }

  @Override
  public Expression visitProj(ProjExpression expr) {
    return Proj(expr.getExpression().accept(this), expr.getField());
  }

  @Override
  public Expression visitClassExt(ClassExtExpression expr) {
    Map<FunctionDefinition, OverriddenDefinition> definitions = new HashMap<>();
    for (Map.Entry<FunctionDefinition, OverriddenDefinition> entry : expr.getDefinitionsMap().entrySet()) {
      List<Argument> arguments = null;
      if (entry.getValue().getArguments() != null) {
        arguments = new ArrayList<>(entry.getValue().getArguments().size());
        for (Argument argument : entry.getValue().getArguments()) {
          if (argument instanceof TypeArgument) {
            Expression type = ((TypeArgument) argument).getType().accept(this);
            if (argument instanceof TelescopeArgument) {
              arguments.add(Tele(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), type));
            } else {
              arguments.add(TypeArg(argument.getExplicit(), type));
            }
          } else {
            arguments.add(argument);
          }
        }
      }

      Expression resultType = entry.getValue().getResultType() == null ? null : entry.getValue().getResultType().accept(this);
      Expression term = entry.getValue().getTerm() == null ? null : entry.getValue().getTerm().accept(this);
      OverriddenDefinition definition = new OverriddenDefinition(entry.getValue().getName(), entry.getValue().getParent(), entry.getValue().getPrecedence(), entry.getValue().getFixity(), arguments, resultType, entry.getValue().getArrow(), term, entry.getValue().getOverriddenFunction());
      definitions.put(entry.getKey(), definition);
    }
    return ClassExt(expr.getBaseClass(), definitions);
  }

  @Override
  public Expression visitNew(NewExpression expr) {
    return New(expr.getExpression().accept(this));
  }

  @Override
  public Expression visitLet(LetExpression letExpression) {
    List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      clauses.add(new LetClause(clause.getName(), visitArguments(clause.getArguments()),
              clause.getResultType() == null ? null : clause.getResultType().accept(this), clause.getArrow(), clause.getTerm().accept(this)));
    }
    return Let(clauses, letExpression.getExpression().accept(this));
  }
}
