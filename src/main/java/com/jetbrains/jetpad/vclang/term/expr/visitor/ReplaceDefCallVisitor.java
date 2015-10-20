package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
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
  private final Namespace myNamespace;
  private Expression myExpression;

  public ReplaceDefCallVisitor(Namespace namespace, Expression expression) {
    myNamespace = namespace;
    myExpression = expression;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    return Apps(expr.getFunction().accept(this), new ArgumentExpression(expr.getArgument().getExpression().accept(this), expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public DefCallExpression visitDefCall(DefCallExpression expr) {
    Expression expr1;
    if (expr.getExpression() != null) {
      expr1 = expr.getExpression().accept(this);
    } else {
      expr1 = expr.getResolvedName().parent == myNamespace && expr.getDefinition() instanceof ClassField ? myExpression : null;
    }

    List<Expression> parameters = expr.getParameters() == null ? null : new ArrayList<Expression>(expr.getParameters().size());
    if (expr.getParameters() != null) {
      for (Expression parameter : expr.getParameters()) {
        parameters.add(parameter.accept(this));
      }
    }

    return DefCall(expr1, expr.getDefinition(), parameters);
  }

  @Override
  public IndexExpression visitIndex(IndexExpression expr) {
    return expr;
  }

  @Override
  public LamExpression visitLam(LamExpression expr) {
    Expression oldExpression = myExpression;
    List<Argument> arguments = visitArguments(expr.getArguments());
    LamExpression result = Lam(arguments, expr.getBody().accept(this));
    myExpression = oldExpression;
    return result;
  }

  private List<Argument> visitArguments(List<Argument> arguments) {
    List<Argument> result = new ArrayList<>(arguments.size());
    for (Argument arg : arguments) {
      if (arg instanceof TelescopeArgument) {
        result.add(Tele(arg.getExplicit(), ((TelescopeArgument) arg).getNames(), ((TelescopeArgument) arg).getType().accept(this)));
        myExpression = myExpression.liftIndex(0, ((TelescopeArgument) arg).getNames().size());
      } else {
        if (arg instanceof TypeArgument) {
          result.add(TypeArg(arg.getExplicit(), ((TypeArgument) arg).getType().accept(this)));
        } else {
          result.add(arg);
        }
        myExpression = myExpression.liftIndex(0, 1);
      }
    }
    return result;
  }

  private Expression visitTypeArguments(List<TypeArgument> args, Expression codomain) {
    Expression oldExpression = myExpression;
    List<TypeArgument> arguments = new ArrayList<>(args.size());
    for (TypeArgument arg : args) {
      if (arg instanceof TelescopeArgument) {
        arguments.add(Tele(arg.getExplicit(), ((TelescopeArgument) arg).getNames(), arg.getType().accept(this)));
        myExpression = myExpression.liftIndex(0, ((TelescopeArgument) arg).getNames().size());
      } else {
        arguments.add(TypeArg(arg.getExplicit(), arg.getType().accept(this)));
        myExpression = myExpression.liftIndex(0, 1);
      }
    }
    Expression result = codomain == null ? Sigma(arguments) : Pi(arguments, codomain.accept(this));
    myExpression = oldExpression;
    return result;
  }

  @Override
  public PiExpression visitPi(PiExpression expr) {
    return (PiExpression) visitTypeArguments(expr.getArguments(), expr.getCodomain());
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr) {
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
    return Tuple(fields, visitSigma(expr.getType()));
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr) {
    return (SigmaExpression) visitTypeArguments(expr.getArguments(), null);
  }

  private Clause visitClause(Clause clause, ElimExpression elimExpression) {
    return new Clause(clause.getPatterns(), clause.getArrow(), clause.getExpression().accept(this), elimExpression);
  }

  @Override
  public ElimExpression visitElim(ElimExpression expr) {
    List<Clause> clauses = new ArrayList<>(expr.getClauses().size());
    ElimExpression elimExpression = Elim(expr.getExpressions(), clauses);
    for (Clause clause : expr.getClauses()) {
      clauses.add(visitClause(clause, elimExpression));
    }
    return elimExpression;
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
      OverriddenDefinition function = entry.getValue();
      if (function.getArguments() != null) {
        arguments = new ArrayList<>(function.getArguments().size());
        for (Argument argument : function.getArguments()) {
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

      Expression resultType = function.getResultType() == null ? null : function.getResultType().accept(this);
      Expression term = function.getTerm() == null ? null : function.getTerm().accept(this);
      OverriddenDefinition definition = new OverriddenDefinition(function.getParentNamespace(), function.getName(), function.getPrecedence(), arguments, resultType, function.getArrow(), term, function.getOverriddenFunction());
      definitions.put(entry.getKey(), definition);
    }
    return ClassExt(visitDefCall(expr.getBaseClassExpression()), definitions, expr.getUniverse());
  }

  @Override
  public Expression visitNew(NewExpression expr) {
    return New(expr.getExpression().accept(this));
  }

  private LetClause visitLetClause(LetClause clause) {
    Expression oldExpresson = myExpression;
    LetClause result = new LetClause(clause.getName(), visitArguments(clause.getArguments()), clause.getResultType() == null ? null : clause.getResultType().accept(this),
            clause.getArrow(), clause.getTerm().accept(this));
    myExpression = oldExpresson;
    return result;
  }

  @Override
  public Expression visitLet(LetExpression letExpression) {
    Expression oldExpression = myExpression;
    List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      clauses.add(visitLetClause(clause));
      myExpression.liftIndex(0, 1);
    }

    Expression result = Let(clauses, letExpression.getExpression().accept(this));
    myExpression = oldExpression;
    return result;
  }
}
