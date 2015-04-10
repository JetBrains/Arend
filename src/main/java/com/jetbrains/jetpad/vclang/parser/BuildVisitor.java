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
import java.util.Map;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private final List<ParserError> myErrors = new ArrayList<>();
  private final Map<String, Definition.Precedence> myOperators;

  public BuildVisitor(Map<String, Definition.Precedence> operators) {
    myOperators = operators;
  }

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

  public Expression visitExpr(Expr2Context expr) {
    return (Expression) visit(expr);
  }

  public Expression visitExpr(AtomContext expr) {
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
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    String name = isPrefix ? ((NameIdContext) ctx.name()).ID().getText() : ((NameBinOpContext) ctx.name()).BIN_OP().getText();
    Expression type = visitExpr(ctx.expr1());
    Expression term = visitExpr(ctx.expr());
    return new FunctionDefinition(name, new Signature(new TypeArgument[0], type), isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, term);
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
  public SigmaExpression visitSigma(SigmaContext ctx) {
    return Sigma(visitTeles(ctx.tele()));
  }

  @Override
  public PiExpression visitPi(PiContext ctx) {
    return Pi(visitTeles(ctx.tele()), visitExpr(ctx.expr1()));
  }

  private Expression visitAtoms(List<AtomContext> atoms) {
    Expression result = visitExpr(atoms.get(0));
    for (int i = 1; i < atoms.size(); ++i) {
      result = Apps(result, visitExpr(atoms.get(i)));
    }
    return result;
  }

  @Override
  public Expression visitExpr2Atom(Expr2AtomContext ctx) {
    return visitAtoms(ctx.atom());
  }

  private Expression insertBinOp(Expression expr1, Definition.Precedence prec, String binOp, Token token, Expression expr2) {
    if (expr2 instanceof BinOpExpression) {
      BinOpExpression binOp2 = (BinOpExpression) expr2;
      Definition.Precedence prec2 = binOp2.getBinOp().getPrecedence();
      if (prec.priority < prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.RIGHT_ASSOC && prec2.associativity == Definition.Associativity.RIGHT_ASSOC)) {
        return BinOp(expr1, new FunctionDefinition(binOp, null, prec, Definition.Fixity.INFIX, Var(binOp)), expr2);
      }
      if (prec.priority > prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.LEFT_ASSOC && prec2.associativity == Definition.Associativity.LEFT_ASSOC)) {
        return BinOp(insertBinOp(expr1, prec, binOp, token, binOp2.getLeft()), binOp2.getBinOp(), binOp2.getRight());
      }
      String msg = "Precedence parsing error: cannot mix (" + binOp + ") [" + prec + "] and (" + binOp2.getBinOp().getName() + ") [" + prec2 + "] in the same infix expression";
      myErrors.add(new ParserError(token.getLine(), token.getCharPositionInLine(), msg));
      return BinOp(expr1, new FunctionDefinition(binOp, null, prec, Definition.Fixity.INFIX, Var(binOp)), expr2);
    } else {
      return BinOp(expr1, new FunctionDefinition(binOp, null, prec, Definition.Fixity.INFIX, Var(binOp)), expr2);
    }
  }

  private Expression visitExpr2Infix(List<AtomContext> atoms, String binOp, Token binOpToken, Definition.Fixity fixity, Expr2Context expr2Ctx) {
    Expression expr1 = visitAtoms(atoms);
    Expression expr2 = visitExpr(expr2Ctx);
    Definition.Precedence prec = myOperators.get(binOp);
    if (prec == null) {
      return BinOp(expr1, new FunctionDefinition(binOp, null, null, fixity, Var(binOp)), expr2);
    }
    return insertBinOp(expr1, prec, binOp, binOpToken, expr2);
  }

  @Override
  public Expression visitExpr2BinOp(Expr2BinOpContext ctx) {
    return visitExpr2Infix(ctx.atom(), ctx.BIN_OP().getText(), ctx.getToken(BIN_OP, 0).getSymbol(), Definition.Fixity.INFIX, ctx.expr2());
  }

  @Override
  public Object visitExpr2Id(Expr2IdContext ctx) {
    return visitExpr2Infix(ctx.atom(), ctx.ID().getText(), ctx.getToken(ID, 0).getSymbol(), Definition.Fixity.PREFIX, ctx.expr2());
  }
}
