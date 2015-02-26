package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.definition.Argument;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;

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
    return new FunctionDefinition(name, new Signature(new Argument[0], type), term);
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
      expr = Lam(it.previous().getText(), expr);
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
    for (int i = telescopeSize - 1; i >= 0; --i) {
      boolean explicit = ctx.tele(i) instanceof ExplicitContext;
      List<TerminalNode> ids = explicit ? ((ExplicitContext) ctx.tele(i)).ID() : ((ImplicitContext) ctx.tele(i)).ID();
      ListIterator<TerminalNode> it = ids.listIterator(ids.size());
      while (it.hasPrevious()) {
        expr = Pi(explicit, it.previous().getText(), lefts[i], expr);
      }
    }
    return expr;
  }
}
