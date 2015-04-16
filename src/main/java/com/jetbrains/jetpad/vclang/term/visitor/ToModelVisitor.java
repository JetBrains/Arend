package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.model.StringWrapper;
import com.jetbrains.jetpad.vclang.model.expr.Model;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

import java.util.List;

public class ToModelVisitor implements ExpressionVisitor<Model.Expression> {
  private final List<String> myLocalContext;
  private final WrapperContext myContext;

  public ToModelVisitor(WrapperContext ctx, List<String> localContext) {
    myLocalContext = localContext;
    myContext = ctx;
  }

  @Override
  public Model.AppExpression visitApp(AppExpression expr) {
    Model.AppExpression result = new Model.AppExpression(myContext);
    result.function().set(expr.getFunction().accept(this));
    result.argument().set(expr.getArgument().accept(this));
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.VarExpression visitDefCall(DefCallExpression expr) {
    Model.VarExpression result = new Model.VarExpression(myContext);
    result.name().set(expr.getDefinition().getName());
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.VarExpression visitIndex(IndexExpression expr) {
    assert expr.getIndex() < myLocalContext.size();
    Model.VarExpression result = new Model.VarExpression(myContext);
    result.name().set(myLocalContext.get(expr.getIndex()));
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.LamExpression visitLam(LamExpression expr) {
    Model.LamExpression result = new Model.LamExpression(myContext);
    int numberOfVars = 0;
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof NameArgument) {
        Model.NameArgument modelArgument = new Model.NameArgument(myContext);
        result.getArguments().add(modelArgument);
        modelArgument.isExplicit().set(argument.getExplicit());
        modelArgument.name().set(((NameArgument) argument).getName());

        ++numberOfVars;
        myLocalContext.add(((NameArgument) argument).getName());
      } else
      if (argument instanceof TelescopeArgument) {
        Model.TelescopeArgument modelArgument = new Model.TelescopeArgument(myContext);
        result.getArguments().add(modelArgument);
        modelArgument.isExplicit().set(argument.getExplicit());
        modelArgument.getNames().addAll(((TelescopeArgument) argument).getNames());
        modelArgument.type().set(((TelescopeArgument) argument).getType().accept(this));

        numberOfVars += ((TelescopeArgument) argument).getNames().size();
        myLocalContext.addAll(((TelescopeArgument) argument).getNames());
      } else {
        throw new IllegalStateException();
      }
    }

    result.body().set(expr.getBody().accept(this));
    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.NatExpression visitNat(NatExpression expr) {
    Model.NatExpression result = new Model.NatExpression(myContext);
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.NelimExpression visitNelim(NelimExpression expr) {
    Model.NelimExpression result = new Model.NelimExpression(myContext);
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.PiExpression visitPi(PiExpression expr) {
    Model.PiExpression result = new Model.PiExpression(myContext);
    int numberOfVars = 0;
    for (TypeArgument argument : expr.getArguments()) {
      if (argument instanceof TelescopeArgument) {
        Model.TelescopeArgument arg = new Model.TelescopeArgument(myContext);
        result.getArguments().add(arg);
        arg.isExplicit().set(argument.getExplicit());
        arg.type().set(argument.getType().accept(this));
        arg.names().addAll(StringWrapper.wrap(myContext, ((TelescopeArgument) argument).getNames()));

        numberOfVars += ((TelescopeArgument) argument).getNames().size();
        myLocalContext.addAll(((TelescopeArgument) argument).getNames());
      } else {
        Model.TypeArgument arg = new Model.TypeArgument(myContext);
        result.getArguments().add(arg);
        arg.isExplicit().set(argument.getExplicit());
        arg.type().set(argument.getType().accept(this));
      }
    }

    result.codomain().set(expr.getCodomain().accept(this));
    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.SucExpression visitSuc(SucExpression expr) {
    Model.SucExpression result = new Model.SucExpression(myContext);
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.UniverseExpression visitUniverse(UniverseExpression expr) {
    Model.UniverseExpression result = new Model.UniverseExpression(myContext);
    result.universe = expr.getUniverse();
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.VarExpression visitVar(VarExpression expr) {
    Model.VarExpression result = new Model.VarExpression(myContext);
    result.name().set(expr.getName());
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.ZeroExpression visitZero(ZeroExpression expr) {
    Model.ZeroExpression result = new Model.ZeroExpression(myContext);
    result.setWellTyped(expr);
    return result;
  }

  @Override
  public Model.Expression visitHole(HoleExpression expr) {
    /*
    // TODO: Check if expr.expression() == null.
    Model.Expression result = expr.expression().accept(this);
    result.setWellTyped(expr);
    return result;
    */
    return null;
  }

  @Override
  public Model.Expression visitTuple(TupleExpression expr) {
    // TODO: Write this.
    return null;
  }

  @Override
  public Model.Expression visitSigma(SigmaExpression expr) {
    // TODO: Write this.
    return null;
  }

  @Override
  public Model.Expression visitBinOp(BinOpExpression expr) {
    // TODO: Write this.
    return null;
  }
}
