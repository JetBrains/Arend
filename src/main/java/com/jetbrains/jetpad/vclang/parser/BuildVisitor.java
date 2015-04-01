package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
  public Expression visitExpr(ExprContext expr) {
    return (Expression) visit(expr);
  }

  public Expression visitExpr(Expr1Context expr) {
    return (Expression) visit(expr);
  }

  @Override
  public List<Definition> visitDefs(DefsContext ctx) {
    List<Definition> defs = new ArrayList<>();
    for (DefContext def : ctx.def()) {
      defs.add(visitDef(def));
    }
    return defs;
  }

  @Override
  public Definition visitDef(DefContext ctx) {
    String name = ctx.ID().getText();
    Expression type = visitExpr(ctx.expr1());
    Expression term = visitExpr(ctx.expr());
    return new FunctionDefinition(name, new Signature(new TypeArgument[0], type), term);
  }

  @Override
  public NatExpression visitNat(NatContext ctx) {
    return Nat();
  }

  @Override
  public ZeroExpression visitZero(ZeroContext ctx) {
    return Zero();
  }

  @Override
  public SucExpression visitSuc(SucContext ctx) {
    return Suc();
  }

  @Override
  public PiExpression visitArr(ArrContext ctx) {
    Expression left = visitExpr(ctx.expr1(0));
    Expression right = visitExpr(ctx.expr1(1));
    return Pi(left, right);
  }

  @Override
  public Expression visitApp(AppContext ctx) {
    Expression left = visitExpr(ctx.expr1(0));
    Expression right = visitExpr(ctx.expr1(1));
    return Apps(left, right);
  }

  @Override
  public Expression visitParens(ParensContext ctx) {
    return visitExpr(ctx.expr());
  }

  @Override
  public NelimExpression visitNelim(NelimContext ctx) {
    return Nelim();
  }

  @Override
  public Expression visitLam(LamContext ctx) {
    Expression expr = visitExpr(ctx.expr());
    ListIterator<TerminalNode> it = ctx.ID().listIterator(ctx.ID().size());
    while (it.hasPrevious()) {
      List<Argument> arguments = new ArrayList<>(1);
      arguments.add(new NameArgument(true, it.previous().getText()));
      expr = Lam(arguments, expr);
    }
    return expr;
  }

  @Override
  public Expression visitId(IdContext ctx) {
    return Var(ctx.ID().getText());
  }

  @Override
  public UniverseExpression visitUniverse(UniverseContext ctx) {
    return Universe(Integer.valueOf(ctx.UNIVERSE().getText().substring("Type".length())));
  }

  @Override
  public Expression visitPi(PiContext ctx) {
    int telescopeSize = ctx.tele().size();
    Expression[] lefts = new Expression[telescopeSize];
    for (int i = 0; i < telescopeSize; ++i) {
      boolean explicit = ctx.tele(i) instanceof ExplicitContext;
      Expr1Context expr1 = explicit ? ((ExplicitContext) ctx.tele(i)).expr1() : ((ImplicitContext) ctx.tele(i)).expr1();
      lefts[i] = visitExpr(expr1);
    }
    Expression expr = visitExpr(ctx.expr1());
    List<TypeArgument> arguments = new ArrayList<>(telescopeSize);
    for (int i = 0; i < telescopeSize; ++i) {
      boolean explicit = ctx.tele(i) instanceof ExplicitContext;
      List<TerminalNode> ids = explicit ? ((ExplicitContext) ctx.tele(i)).ID() : ((ImplicitContext) ctx.tele(i)).ID();
      List<String> names = new ArrayList<>(ids.size());
      for (TerminalNode id : ids) {
        names.add(id.getText());
      }
      arguments.add(new TelescopeArgument(explicit, names, lefts[i]));
    }
    return Pi(arguments, expr);
  }
}
