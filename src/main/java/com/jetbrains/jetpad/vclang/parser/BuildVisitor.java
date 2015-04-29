package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Var;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private final List<ParserError> myErrors = new ArrayList<>();
  private final Map<String, Definition> myGlobalContext;
  private final Map<String, Concrete.Definition> myLocalContext;

  public BuildVisitor(Map<String, Definition> globalContext) {
    myGlobalContext = globalContext;
    myLocalContext = new HashMap<>();
  }

  public List<ParserError> getErrors() {
    return myErrors;
  }

  private List<Concrete.NameArgument> getVars(Concrete.Expression expr, Token token) {
    List<Concrete.NameArgument> vars = new ArrayList<>();
    while (expr instanceof Concrete.AppExpression) {
      Concrete.Expression arg = ((Concrete.AppExpression) expr).getArgument();
      if (arg instanceof Concrete.VarExpression) {
        vars.add(new Concrete.NameArgument(arg.getPosition(), true, ((Concrete.VarExpression) arg).getName()));
      } else {
        break;
      }
      expr = ((Concrete.AppExpression) expr).getFunction();
    }
    if (expr instanceof Concrete.VarExpression) {
      vars.add(new Concrete.NameArgument(expr.getPosition(), true, ((Concrete.VarExpression) expr).getName()));
    } else
    if (expr instanceof Concrete.InferHoleExpression) {
      vars.add(new Concrete.NameArgument(expr.getPosition(), true, "_"));
    } else {
      myErrors.add(new ParserError(token.getLine(), token.getCharPositionInLine(), "Expected a list of variables"));
      return null;
    }

    List<Concrete.NameArgument> result = new ArrayList<>(vars.size());
    for (int i = vars.size() - 1; i >= 0; --i) {
        result.add(vars.get(i));
    }
    return result;
  }

  public Concrete.Expression visitExpr(ExprContext expr) {
    return (Concrete.Expression) visit(expr);
  }

  public Concrete.Expression visitExpr(AtomContext expr) {
    return (Concrete.Expression) visit(expr);
  }

  public Concrete.Expression visitExpr(LiteralContext expr) {
    return (Concrete.Expression) visit(expr);
  }

  @Override
  public List<Concrete.Definition> visitDefs(DefsContext ctx) {
    List<Concrete.Definition> defs = new ArrayList<>();
    for (DefContext def : ctx.def()) {
      defs.add(visitDef(def));
    }
    return defs;
  }

  public Concrete.Definition visitDef(DefContext ctx) {
    if (ctx instanceof DefFunctionContext) {
      return visitDefFunction((DefFunctionContext) ctx);
    }
    if (ctx instanceof DefDataContext) {
      return visitDefData((DefDataContext) ctx);
    }
    throw new IllegalStateException();
  }

  @Override
  public Concrete.FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    String name;
    Token token;
    if (isPrefix) {
      name = ((NameIdContext) ctx.name()).ID().getText();
      token = ((NameIdContext) ctx.name()).ID().getSymbol();
    } else {
      name = ((NameBinOpContext) ctx.name()).BIN_OP().getText();
      token = ((NameBinOpContext) ctx.name()).BIN_OP().getSymbol();
    }
    List<Concrete.TelescopeArgument> arguments = new ArrayList<>();
    for (TeleContext tele : ctx.tele()) {
      List<Concrete.Argument> args = visitLamTele(tele);
      if (args == null) return null;
      if (args.get(0) instanceof Concrete.TelescopeArgument) {
        arguments.add((Concrete.TelescopeArgument) args.get(0));
      } else {
        myErrors.add(new ParserError(tele.getStart().getLine(), tele.getStart().getCharPositionInLine(), "Expected a typed variable"));
        return null;
      }
    }
    Concrete.Expression type = visitTypeOpt(ctx.typeOpt());
    Definition.Arrow arrow = ctx.arrow() instanceof ArrowRightContext ? Definition.Arrow.RIGHT : Definition.Arrow.LEFT;
    Concrete.Expression term = visitExpr(ctx.expr());
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(tokenPosition(token), name, Abstract.Definition.DEFAULT_PRECEDENCE, isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, arguments, type, arrow, term);
    myLocalContext.put(name, def);
    return def;
  }

  @Override
  public Concrete.DataDefinition visitDefData(DefDataContext ctx) {
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    String name;
    Token token;
    if (isPrefix) {
      name = ((NameIdContext) ctx.name()).ID().getText();
      token = ((NameIdContext) ctx.name()).ID().getSymbol();
    } else {
      name = ((NameBinOpContext) ctx.name()).BIN_OP().getText();
      token = ((NameBinOpContext) ctx.name()).BIN_OP().getSymbol();
    }
    List<Concrete.TypeArgument> parameters = visitTeles(ctx.tele());
    Concrete.Expression type = visitTypeOpt(ctx.typeOpt());
    if (type != null && !(type instanceof Concrete.UniverseExpression)) {
      myErrors.add(new ParserError(ctx.typeOpt().getStart().getLine(), ctx.typeOpt().getStart().getCharPositionInLine(), "Expected a universe"));
      return null;
    }

    List<Concrete.Constructor> constructors = new ArrayList<>();
    Definition.Fixity fixity = isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX;
    Universe universe = type == null ? new Universe.Type() : ((Concrete.UniverseExpression) type).getUniverse();
    Concrete.DataDefinition def = new Concrete.DataDefinition(tokenPosition(token), name, Abstract.Definition.DEFAULT_PRECEDENCE, fixity, universe, parameters, constructors);
    for (ConstructorContext constructor : ctx.constructor()) {
      isPrefix = constructor.name() instanceof NameIdContext;
      if (isPrefix) {
        name = ((NameIdContext) constructor.name()).ID().getText();
        token = ((NameIdContext) constructor.name()).ID().getSymbol();
      } else {
        name = ((NameBinOpContext) constructor.name()).BIN_OP().getText();
        token = ((NameBinOpContext) constructor.name()).BIN_OP().getSymbol();
      }
      constructors.add(new Concrete.Constructor(tokenPosition(token), name, Abstract.Definition.DEFAULT_PRECEDENCE, isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, new Universe.Type(), visitTeles(constructor.tele()), def));
    }
    myLocalContext.put(name, def);
    return def;
  }

  private Concrete.Expression visitTypeOpt(TypeOptContext ctx) {
    if (ctx instanceof NoTypeContext) {
      return null;
    }
    if (ctx instanceof WithTypeContext) {
      return visitExpr(((WithTypeContext) ctx).expr());
    }
    throw new IllegalStateException();
  }

  @Override
  public Concrete.InferHoleExpression visitUnknown(UnknownContext ctx) {
    return new Concrete.InferHoleExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.ZeroExpression visitZero(ZeroContext ctx) {
    return new Concrete.ZeroExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.NatExpression visitNat(NatContext ctx) {
    return new Concrete.NatExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.SucExpression visitSuc(SucContext ctx) {
    return new Concrete.SucExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.NelimExpression visitNelim(NelimContext ctx) {
    return new Concrete.NelimExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    Concrete.Expression domain = visitExpr(ctx.expr(0));
    List<Concrete.TypeArgument> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeArgument(domain.getPosition(), true, domain));
    Concrete.Expression codomain = visitExpr(ctx.expr(1));
    return new Concrete.PiExpression(tokenPosition(ctx.getToken(ARROW, 0).getSymbol()), arguments, codomain);
  }

  @Override
  public Concrete.Expression visitTuple(TupleContext ctx) {
    if (ctx.expr().size() == 1) {
      return visitExpr(ctx.expr(0));
    } else {
      List<Concrete.Expression> fields = new ArrayList<>(ctx.expr().size());
      for (ExprContext exprCtx : ctx.expr()) {
        fields.add(visitExpr(exprCtx));
      }
      return new Concrete.TupleExpression(tokenPosition(ctx.getStart()), fields);
    }
  }

  private List<Concrete.Argument> visitLamTele(TeleContext tele) {
    List<Concrete.Argument> arguments = new ArrayList<>(3);
    if (tele instanceof TeleLiteralContext) {
      LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
      if (literalContext instanceof IdContext) {
        arguments.add(new Concrete.NameArgument(tokenPosition(((IdContext) literalContext).ID().getSymbol()), true, ((IdContext) literalContext).ID().getText()));
      } else
      if (literalContext instanceof UnknownContext) {
        arguments.add(new Concrete.NameArgument(tokenPosition(literalContext.getStart()), true, "_"));
      } else {
        myErrors.add(new ParserError(literalContext.getStart().getLine(), literalContext.getStart().getCharPositionInLine(), "Unexpected token. Expected an identifier."));
        return null;
      }
    } else {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      Concrete.Expression varsExpr;
      Concrete.Expression typeExpr;
      if (typedExpr instanceof TypedContext) {
        varsExpr = visitExpr(((TypedContext) typedExpr).expr(0));
        typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
      } else {
        varsExpr = visitExpr(((NotTypedContext) typedExpr).expr());
        typeExpr = null;
      }
      List<Concrete.NameArgument> vars = getVars(varsExpr, typedExpr.getStart());
      if (vars == null) return null;
      if (typeExpr == null) {
        arguments.addAll(vars);
      } else {
        List<String> args = new ArrayList<>(vars.size());
        for (Concrete.NameArgument var : vars) {
          args.add(var.getName());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, args, typeExpr));
      }
    }
    return arguments;
  }

  private List<Concrete.Argument> visitLamTeles(List<TeleContext> tele) {
    List<Concrete.Argument> arguments = new ArrayList<>(tele.size());
    for (TeleContext arg : tele) {
      List<Concrete.Argument> args = visitLamTele(arg);
      if (args == null) return null;
      arguments.addAll(args);
    }
    return arguments;
  }

  @Override
  public Concrete.Expression visitLam(LamContext ctx) {
    return new Concrete.LamExpression(tokenPosition(ctx.getStart()), visitLamTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.Expression visitId(IdContext ctx) {
    return new Concrete.VarExpression(tokenPosition(ctx.ID().getSymbol()), ctx.ID().getText());
  }

  @Override
  public Concrete.UniverseExpression visitUniverse(UniverseContext ctx) {
    return new Concrete.UniverseExpression(tokenPosition(ctx.UNIVERSE().getSymbol()), new Universe.Type(Integer.valueOf(ctx.UNIVERSE().getText().substring("\\Type".length()))));
  }

  @Override
  public Concrete.UniverseExpression visitTruncatedUniverse(TruncatedUniverseContext ctx) {
    String text = ctx.TRUNCATED_UNIVERSE().getText();
    int indexOfMinusSign = text.indexOf('-');
    return new Concrete.UniverseExpression(tokenPosition(ctx.TRUNCATED_UNIVERSE().getSymbol()), new Universe.Type(Integer.valueOf(text.substring(1, indexOfMinusSign)), Integer.valueOf(text.substring(indexOfMinusSign + "-Type".length()))));
  }

  @Override
  public Concrete.UniverseExpression visitProp(PropContext ctx) {
    return new Concrete.UniverseExpression(tokenPosition(ctx.PROP().getSymbol()), new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP));
  }

  @Override
  public Concrete.UniverseExpression visitSet(SetContext ctx) {
    return new Concrete.UniverseExpression(tokenPosition(ctx.SET().getSymbol()), new Universe.Type(Integer.valueOf(ctx.SET().getText().substring("\\Set".length())), Universe.Type.SET));
  }

  private List<Concrete.TypeArgument> visitTeles(List<TeleContext> teles) {
    List<Concrete.TypeArgument> arguments = new ArrayList<>(teles.size());
    for (TeleContext tele : teles) {
      boolean explicit = !(tele instanceof ImplicitContext);
      TypedExprContext typedExpr;
      if (explicit) {
        if (tele instanceof ExplicitContext) {
          typedExpr = ((ExplicitContext) tele).typedExpr();
        } else {
          arguments.add(new Concrete.TypeArgument(true, visitExpr(((TeleLiteralContext) tele).literal())));
          continue;
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        List<Concrete.NameArgument> args = getVars(visitExpr(((TypedContext) typedExpr).expr(0)), typedExpr.getStart());
        if (args == null) return null;
        List<String> vars = new ArrayList<>(args.size());
        for (Concrete.NameArgument arg : args) {
          vars.add(arg.getName());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, vars, visitExpr(((TypedContext) typedExpr).expr(1))));
      } else {
        arguments.add(new Concrete.TypeArgument(explicit, visitExpr(((NotTypedContext) typedExpr).expr())));
      }
    }
    return arguments;
  }

  @Override
  public Concrete.Expression visitAtomLiteral(AtomLiteralContext ctx) {
    return visitExpr(ctx.literal());
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    return new Concrete.SigmaExpression(tokenPosition(ctx.getStart()), visitTeles(ctx.tele()));
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    return new Concrete.PiExpression(tokenPosition(ctx.getStart()), visitTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  private Concrete.Expression visitAtoms(List<AtomContext> atoms) {
    Concrete.Expression result = visitExpr(atoms.get(0));
    for (int i = 1; i < atoms.size(); ++i) {
      result = new Concrete.AppExpression(result.getPosition(), result, visitExpr(atoms.get(i)), true);
    }
    return result;
  }

  private class Pair {
    Concrete.Expression expression;
    Abstract.Definition binOp;

    Pair(Concrete.Expression expression, Abstract.Definition binOp) {
      this.expression = expression;
      this.binOp = binOp;
    }
  }

  private void pushOnStack(List<Pair> stack, Concrete.Expression left, Abstract.Definition binOp, Token token) {
    if (stack.isEmpty()) {
      stack.add(new Pair(left, binOp));
      return;
    }

    Pair pair = stack.get(stack.size() - 1);
    Definition.Precedence prec = pair.binOp.getPrecedence();
    Definition.Precedence prec2 = binOp.getPrecedence();
    if (prec.priority < prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.RIGHT_ASSOC && prec2.associativity == Definition.Associativity.RIGHT_ASSOC)) {
      stack.add(new Pair(left, binOp));
      return;
    }
    if (!(prec.priority > prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.LEFT_ASSOC && prec2.associativity == Definition.Associativity.LEFT_ASSOC))) {
      String msg = "Precedence parsing error: cannot mix (" + pair.binOp.getName() + ") [" + prec + "] and (" + binOp.getName() + ") [" + prec2 + "] in the same infix expression";
      myErrors.add(new ParserError(token.getLine(), token.getCharPositionInLine(), msg));
    }
    stack.remove(stack.size() - 1);
    pushOnStack(stack, new Concrete.BinOpExpression(tokenPosition(token), pair.expression, pair.binOp, left), binOp, token);
  }

  @Override
  public Concrete.Expression visitBinOp(BinOpContext ctx) {
    List<Pair> stack = new ArrayList<>(ctx.binOpLeft().size());
    for (BinOpLeftContext leftContext : ctx.binOpLeft()) {
      String name;
      Token token;
      if (leftContext.infix() instanceof InfixBinOpContext) {
        name = ((InfixBinOpContext) leftContext.infix()).BIN_OP().getText();
        token = ((InfixBinOpContext) leftContext.infix()).BIN_OP().getSymbol();
      } else {
        name = ((InfixIdContext) leftContext.infix()).ID().getText();
        token = ((InfixIdContext) leftContext.infix()).ID().getSymbol();
      }
      Abstract.Definition def = myLocalContext.get(name);
      if (def == null) {
        def = myGlobalContext.get(name);
        if (def == null) {
          myErrors.add(new ParserError(token.getLine(), token.getCharPositionInLine(), new NotInScopeError(Var(name)).toString()));
          return null;
        }
      }
      pushOnStack(stack, visitAtoms(leftContext.atom()), def, token);
    }

    Concrete.Expression result = visitAtoms(ctx.atom());
    for (int i = stack.size() - 1; i >= 0; --i) {
      result = new Concrete.BinOpExpression(stack.get(i).expression.getPosition(), stack.get(i).expression, stack.get(i).binOp, result);
    }
    return result;
  }

  @Override
  public Concrete.Expression visitExprElim(ExprElimContext ctx) {
    List<Concrete.Clause> clauses = new ArrayList<>(ctx.clause().size());
    for (ClauseContext clause : ctx.clause()) {
      clauses.add(visitClause(clause));
    }
    Abstract.ElimExpression.ElimType elimType = ctx.elimCase() instanceof ElimContext ? Abstract.ElimExpression.ElimType.ELIM : Abstract.ElimExpression.ElimType.CASE;
    return new Concrete.ElimExpression(tokenPosition(ctx.getStart()), elimType, visitExpr(ctx.expr()), clauses);
  }

  @Override
  public Concrete.Clause visitClause(ClauseContext ctx) {
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    String name = isPrefix ? ((NameIdContext) ctx.name()).ID().getText() : ((NameBinOpContext) ctx.name()).BIN_OP().getText();
    Definition.Arrow arrow = ctx.arrow() instanceof ArrowRightContext ? Definition.Arrow.RIGHT : Definition.Arrow.LEFT;
    List<Concrete.Argument> arguments = visitLamTeles(ctx.tele());
    return new Concrete.Clause(tokenPosition(ctx.getStart()), name, arguments, arrow, visitExpr(ctx.expr()));
  }

  private static Concrete.Position tokenPosition(Token token) {
    return new Concrete.Position(token.getLine(), token.getCharPositionInLine());
  }
}
