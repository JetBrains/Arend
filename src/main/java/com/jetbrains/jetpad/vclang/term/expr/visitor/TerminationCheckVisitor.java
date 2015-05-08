package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;

public class TerminationCheckVisitor implements ExpressionVisitor<Boolean> {
  private static class Pair {
    int index;
    boolean reduced;

    Pair(int index, boolean reduced) {
      this.index = index;
      this.reduced = reduced;
    }
  }

  private final FunctionDefinition myDef;
  private final Pair[] myIndices;

  private TerminationCheckVisitor(FunctionDefinition def, Pair[] indices) {
    myDef = def;
    myIndices = indices;
  }

  public TerminationCheckVisitor(FunctionDefinition def) {
    myDef = def;
    myIndices = new Pair[numberOfVariables(def.getArguments()) + 1];
    for (int i = 0; i < myIndices.length; ++i) {
      myIndices[i] = new Pair(myIndices.length - 1 - i, false);
    }
  }

  private boolean checkTermination(List<Expression> args) {
    if (args.size() + 1 > myIndices.length) return false;

    for (int i = 0; i < args.size(); ++i) {
      Expression arg = args.get(args.size() - 1 - i);
      while (arg instanceof AppExpression) {
        arg = ((AppExpression) arg).getFunction();
      }
      if (arg instanceof IndexExpression) {
        int index = ((IndexExpression) arg).getIndex();
        if (index >= myIndices[i + 1].index && index < myIndices[i].index) {
          if (myIndices[i].reduced) return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    }

    return false;
  }

  @Override
  public Boolean visitApp(AppExpression expr) {
    List<Expression> args = new ArrayList<>();
    Expression fun = expr.getFunction(args);
    if (fun instanceof DefCallExpression && ((DefCallExpression) fun).getDefinition().equals(myDef)) {
      if (!checkTermination(args)) return false;
    }

    if (!fun.accept(this)) return false;
    for (Expression arg : args) {
      if (!arg.accept(this)) return false;
    }
    return true;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr) {
    return !expr.getDefinition().equals(myDef);
  }

  @Override
  public Boolean visitIndex(IndexExpression expr) {
    return true;
  }

  @Override
  public Boolean visitLam(LamExpression expr) {
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof TypeArgument) {
        if (!((TypeArgument) argument).getType().accept(this)) return false;
      }
    }
    return expr.getBody().accept(this);
  }

  @Override
  public Boolean visitPi(PiExpression expr) {
    for (TypeArgument argument : expr.getArguments()) {
      if (!argument.getType().accept(this)) return false;
    }
    return expr.getCodomain().accept(this);
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr) {
    return true;
  }

  @Override
  public Boolean visitVar(VarExpression expr) {
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
    for (TypeArgument argument : expr.getArguments()) {
      if (!argument.getType().accept(this)) return false;
    }
    return true;
  }

  @Override
  public Boolean visitBinOp(BinOpExpression expr) {
    if (expr.getBinOp().equals(myDef)) {
      List<Expression> args = new ArrayList<>(2);
      args.add(expr.getRight());
      args.add(expr.getLeft());
      if (!checkTermination(args)) return false;
    }
    return expr.getLeft().accept(this) && expr.getRight().accept(this);
  }

  @Override
  public Boolean visitElim(ElimExpression expr) {
    if (!expr.getExpression().accept(this)) return false;
    for (Clause clause : expr.getClauses()) {
      for (Argument argument : clause.getArguments()) {
        if (argument instanceof TypeArgument) {
          if (!((TypeArgument) argument).getType().accept(this)) return false;
        }
      }
    }

    if (expr.getElimType() == Abstract.ElimExpression.ElimType.ELIM && expr.getExpression() instanceof IndexExpression) {
      int var = ((IndexExpression) expr.getExpression()).getIndex();
      for (Clause clause : expr.getClauses()) {
        Pair[] indices = Arrays.copyOf(myIndices, myIndices.length);
        int shift = numberOfVariables(clause.getArguments()) - 1;
        for (int i = 0; i < indices.length; ++i) {
          if (indices[i].index <= var) {
            indices[i] = new Pair(indices[i].index, true);
            break;
          } else {
            indices[i] = new Pair(indices[i].index + shift, indices[i].reduced);
          }
        }

        if (!clause.getExpression().accept(new TerminationCheckVisitor(myDef, indices))) return false;
      }
    } else {
      for (Clause clause : expr.getClauses()) {
        if (!clause.getExpression().accept(this)) return false;
      }
    }

    return true;
  }
}
