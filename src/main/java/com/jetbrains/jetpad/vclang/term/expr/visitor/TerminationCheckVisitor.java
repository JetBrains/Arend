package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.OverriddenDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;

public class TerminationCheckVisitor implements ExpressionVisitor<Boolean> {
  private final FunctionDefinition myDef;
  private final List<Expression> myPatterns;

  public TerminationCheckVisitor(FunctionDefinition def) {
    myDef = def;

    int vars = numberOfVariables(def.getArguments());
    myPatterns = new ArrayList<>(vars);
    for (int i = 0; i < vars; ++i) {
      myPatterns.add(Index(i));
    }
  }

  private TerminationCheckVisitor(FunctionDefinition def, List<Expression> patterns) {
    myDef = def;
    myPatterns = patterns;
  }

  private enum Ord { LESS, EQUALS, NOT_LESS }

  private Ord isLess(Expression expr1, Expression expr2) {
    List<Expression> args1 = new ArrayList<>();
    expr1 = expr1.getFunction(args1);
    List<Expression> args2 = new ArrayList<>();
    expr2 = expr2.getFunction(args2);
    if (expr1.equals(expr2)) {
      Ord ord = isLess(args1, args2);
      if (ord != Ord.NOT_LESS) return ord;
    }
    for (Expression arg : args2) {
      if (isLess(expr1, arg) != Ord.NOT_LESS) return Ord.LESS;
    }
    return Ord.NOT_LESS;
  }

  private Ord isLess(List<Expression> exprs1, List<Expression> exprs2) {
    for (int i = 0; i < Math.min(exprs1.size(), exprs2.size()); ++i) {
      Ord ord = isLess(exprs1.get(exprs1.size() - 1 - i), exprs2.get(exprs2.size() - 1 - i));
      if (ord != Ord.EQUALS) return ord;
    }
    return exprs1.size() >= exprs2.size() ? Ord.EQUALS : Ord.NOT_LESS;
  }

  @Override
  public Boolean visitApp(AppExpression expr) {
    List<Expression> args = new ArrayList<>();
    Expression fun = expr.getFunction(args);
    if (fun instanceof DefCallExpression) {
      if (((DefCallExpression) fun).getDefinition() == myDef && isLess(args, myPatterns) != Ord.LESS) return false;
    } else {
      if (!fun.accept(this)) return false;
    }

    for (Expression arg : args) {
      if (!arg.accept(this)) return false;
    }
    return true;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr) {
    return expr.getDefinition() != myDef && (expr.getExpression() == null || expr.getExpression().accept(this));
  }

  @Override
  public Boolean visitIndex(IndexExpression expr) {
    return true;
  }

  private void liftPatterns(int on) {
    if (on == 0) return;
    for (int i = 0; i < myPatterns.size(); ++i) {
      myPatterns.set(i, myPatterns.get(i).liftIndex(0, on));
    }
  }

  @Override
  public Boolean visitLam(LamExpression expr) {
    return visitLamArguments(expr.getArguments(), expr.getBody(), null);
  }

  private boolean visitLamArguments(List<Argument> arguments, Expression body1, Expression body2) {
    int on = 0, total = 0;
    for (Argument argument : arguments) {
      if (argument instanceof NameArgument) {
        ++on;
        ++total;
      } else
      if (argument instanceof TelescopeArgument) {
        liftPatterns(on);
        on = ((TelescopeArgument) argument).getNames().size();
        total += on;
        if (!((TypeArgument) argument).getType().accept(this)) return false;
      } else {
        throw new IllegalStateException();
      }
    }

    liftPatterns(on);
    boolean result = (body1 == null ? true : body1.accept(this)) && (body2 == null ? true : body2.accept(this));
    liftPatterns(-total);
    return result;
  }

  private Boolean visitArguments(List<TypeArgument> arguments, Expression codomain) {
    int total = 0;
    for (TypeArgument argument : arguments) {
      if (!argument.getType().accept(this)) return false;
      if (argument instanceof TelescopeArgument) {
        int on = ((TelescopeArgument) argument).getNames().size();
        total += on;
        liftPatterns(on);
      } else {
        liftPatterns(1);
        ++total;
      }
    }

    boolean result = codomain != null ? codomain.accept(this) : true;
    liftPatterns(-total);
    return result;
  }

  @Override
  public Boolean visitPi(PiExpression expr) {
    return visitArguments(expr.getArguments(), expr.getCodomain());
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr) {
    return true;
  }

  @Override
  public Boolean visitInferHole(InferHoleExpression expr) {
    return true;
  }

  @Override
  public Boolean visitError(ErrorExpression expr) {
    return true;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr) {
    for (Expression field : expr.getFields()) {
      if (!field.accept(this)) return false;
    }
    return true;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr) {
    return visitArguments(expr.getArguments(), null);
  }

  @Override
  public Boolean visitElim(ElimExpression expr) {
    if (!expr.getExpression().accept(this)) return false;
    for (Clause clause : expr.getClauses()) {
      if (clause == null) continue;
      for (Argument argument : clause.getArguments()) {
        if (argument instanceof TypeArgument) {
          if (!((TypeArgument) argument).getType().accept(this)) return false;
        }
      }
    }

    if (expr.getElimType() == Abstract.ElimExpression.ElimType.ELIM && expr.getExpression() instanceof IndexExpression) {
      int var = expr.getExpression().getIndex();
      for (Clause clause : expr.getClauses()) {
        if (clause == null) continue;

        int vars = numberOfVariables(clause.getArguments());
        Expression newExpr = DefCall(clause.getConstructor());
        for (int i = var + vars - 1; i >= var; --i) {
          newExpr = Apps(newExpr, Index(i));
        }

        List<Expression> patterns = new ArrayList<>(myPatterns.size());
        for (Expression pattern : myPatterns) {
          patterns.add(pattern.liftIndex(var + 1, vars).subst(newExpr, var));
        }

        if (!clause.getExpression().accept(new TerminationCheckVisitor(myDef, patterns))) return false;
      }

      if (expr.getOtherwise() != null && !expr.getExpression().accept(this)) return false;
    } else {
      for (Clause clause : expr.getClauses()) {
        if (clause != null && !clause.getExpression().accept(this)) return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitProj(ProjExpression expr) {
    return expr.getExpression().accept(this);
  }

  @Override
  public Boolean visitClassExt(ClassExtExpression expr) {
    for (OverriddenDefinition definition : expr.getDefinitionsMap().values()) {
      if (!definition.hasErrors() && !definition.typeHasErrors() && !visitLamArguments(definition.getArguments(), definition.getResultType(), definition.getTerm())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitNew(NewExpression expr) {
    return expr.getExpression().accept(this);
  }
}
