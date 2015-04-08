package com.jetbrains.jetpad.vclang.parser;

import com.google.common.collect.Lists;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private final List<ParserError> myErrors = new ArrayList<>();

  public List<ParserError> getErrors() {
    return myErrors;
  }

  private List<String> getVars(Expression expr) {
    List<String> vars = new ArrayList<>();
    while (expr instanceof AppExpression) {
      Expression arg = ((AppExpression) expr).getArgument();
      if (arg instanceof VarExpression) {
        vars.add(((VarExpression) arg).getName());
      } else {
        return null;
      }
      expr = ((AppExpression) expr).getFunction();
    }
    if (expr instanceof VarExpression) {
      vars.add(((VarExpression) expr).getName());
    } else {
      return null;
    }
    return Lists.reverse(vars);
  }

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
  public Expression visitTuple(TupleContext ctx) {
    if (ctx.expr().size() == 1) {
      return visitExpr(ctx.expr(0));
    } else {
      List<Expression> fields = new ArrayList<>(ctx.expr().size());
      for (ExprContext exprCtx : ctx.expr()) {
        fields.add(visitExpr(exprCtx));
      }
      return Tuple(fields);
    }
  }

  @Override
  public NelimExpression visitNelim(NelimContext ctx) {
    return Nelim();
  }

  @Override
  public Expression visitLam(LamContext ctx) {
    Expression expr = visitExpr(ctx.expr());
    List<Argument> arguments = new ArrayList<>(ctx.lamArg().size());
    for (LamArgContext arg : ctx.lamArg()) {
      if (arg instanceof LamArgIdContext) {
        arguments.add(Name(((LamArgIdContext) arg).ID().getText()));
      } else {
        TeleContext tele = ((LamArgTeleContext) arg).tele();
        boolean explicit = tele instanceof ExplicitContext;
        TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
        Expression varsExpr;
        Expression typeExpr;
        if (typedExpr instanceof TypedContext) {
          varsExpr = visitExpr(((TypedContext) typedExpr).expr1(0));
          typeExpr = visitExpr(((TypedContext) typedExpr).expr1(1));
        } else {
          varsExpr = visitExpr(((NotTypedContext) typedExpr).expr1());
          typeExpr = null;
        }
        List<String> vars = getVars(varsExpr);
        if (vars == null) {
          Token token = ctx.getToken(LAMBDA, 0).getSymbol();
          myErrors.add(new ParserError(token.getLine(), token.getCharPositionInLine(), null));
          return null;
        }
        if (typeExpr == null) {
          for (String var : vars) {
            arguments.add(Name(explicit, var));
          }
        } else {
          arguments.add(Tele(explicit, vars, typeExpr));
        }
      }
    }
    return Lam(arguments, expr);
  }

  @Override
  public Expression visitId(IdContext ctx) {
    return Var(ctx.ID().getText());
  }

  @Override
  public UniverseExpression visitUniverse(UniverseContext ctx) {
    return Universe(Integer.valueOf(ctx.UNIVERSE().getText().substring("\\Type".length())));
  }

  private List<TypeArgument> visitTeles(List<TeleContext> teles) {
    List<TypeArgument> arguments = new ArrayList<>(teles.size());
    for (TeleContext tele : teles) {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      if (typedExpr instanceof TypedContext) {
        List<String> vars = getVars(visitExpr(((TypedContext) typedExpr).expr1(0)));
        arguments.add(Tele(explicit, vars, visitExpr(((TypedContext) typedExpr).expr1(1))));
      } else {
        arguments.add(TypeArg(explicit, visitExpr(((NotTypedContext) typedExpr).expr1())));
      }
    }
    return arguments;
  }

  @Override
  public Object visitSigma(SigmaContext ctx) {
    return Sigma(visitTeles(ctx.tele()));
  }

  @Override
  public Expression visitPi(PiContext ctx) {
    return Pi(visitTeles(ctx.tele()), visitExpr(ctx.expr1()));
  }
}
