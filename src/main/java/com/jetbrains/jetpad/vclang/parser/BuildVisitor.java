package com.jetbrains.jetpad.vclang.parser;

import com.google.common.collect.Lists;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private final List<ParserError> myErrors = new ArrayList<>();
  private final Map<String, Definition> myGlobalContext;

  public BuildVisitor(Map<String, Definition> globalContext) {
    myGlobalContext = globalContext;
  }

  public List<ParserError> getErrors() {
    return myErrors;
  }

  private List<String> getVars(Expression expr, Token token) {
    List<String> vars = new ArrayList<>();
    while (expr instanceof AppExpression) {
      Expression arg = ((AppExpression) expr).getArgument();
      if (arg instanceof VarExpression) {
        vars.add(((VarExpression) arg).getName());
      } else {
        break;
      }
      expr = ((AppExpression) expr).getFunction();
    }
    if (expr instanceof VarExpression) {
      vars.add(((VarExpression) expr).getName());
    } else
    if (expr instanceof InferHoleExpression) {
      vars.add("_");
    } else {
      myErrors.add(new ParserError(token.getLine(), token.getCharPositionInLine(), "Expected a list of variables"));
      return null;
    }
    return Lists.reverse(vars);
  }

  public Expression visitExpr(ExprContext expr) {
    return (Expression) visit(expr);
  }

  public Expression visitExpr(AtomContext expr) {
    return (Expression) visit(expr);
  }

  public Expression visitExpr(LiteralContext expr) {
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

  public Definition visitDef(DefContext ctx) {
    if (ctx instanceof DefFunctionContext) {
      return visitDefFunction((DefFunctionContext) ctx);
    }
    if (ctx instanceof DefDataContext) {
      return visitDefData((DefDataContext) ctx);
    }
    throw new IllegalStateException();
  }

  @Override
  public FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    String name = isPrefix ? ((NameIdContext) ctx.name()).ID().getText() : ((NameBinOpContext) ctx.name()).BIN_OP().getText();
    List<TelescopeArgument> arguments = new ArrayList<>();
    for (TeleContext tele : ctx.tele()) {
      List<Argument> args = visitLamTele(tele);
      if (args == null) return null;
      if (args.get(0) instanceof TelescopeArgument) {
        arguments.add((TelescopeArgument) args.get(0));
      } else {
        myErrors.add(new ParserError(tele.getStart().getLine(), tele.getStart().getCharPositionInLine(), "Expected a typed variable"));
        return null;
      }
    }
    Expression type = visitTypeOpt(ctx.typeOpt());
    Definition.Arrow arrow = ctx.arrow() instanceof ArrowRightContext ? Definition.Arrow.RIGHT : Definition.Arrow.LEFT;
    Expression term = visitExpr(ctx.expr());
    FunctionDefinition def = new FunctionDefinition(name, isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, arguments, type, arrow, term);
    myGlobalContext.put(name, def);
    return def;
  }

  @Override
  public DataDefinition visitDefData(DefDataContext ctx) {
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    String name = isPrefix ? ((NameIdContext) ctx.name()).ID().getText() : ((NameBinOpContext) ctx.name()).BIN_OP().getText();
    List<TypeArgument> parameters = visitTeles(ctx.tele());
    Expression type = visitTypeOpt(ctx.typeOpt());
    if (type != null && !(type instanceof UniverseExpression)) {
      myErrors.add(new ParserError(ctx.typeOpt().getStart().getLine(), ctx.typeOpt().getStart().getCharPositionInLine(), "Expected a universe"));
      return null;
    }

    List<Constructor> constructors = new ArrayList<>();
    Definition.Fixity fixity = isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX;
    Universe universe = type == null ? new Universe.Type() : ((UniverseExpression) type).getUniverse();
    DataDefinition def = new DataDefinition(name, fixity, universe, parameters, constructors);
    for (ConstructorContext constructor : ctx.constructor()) {
      isPrefix = constructor.name() instanceof NameIdContext;
      name = isPrefix ? ((NameIdContext) constructor.name()).ID().getText() : ((NameBinOpContext) constructor.name()).BIN_OP().getText();
      constructors.add(new Constructor(name, isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, new Universe.Type(), visitTeles(constructor.tele()), def));
    }
    myGlobalContext.put(name, def);
    return def;
  }

  private Expression visitTypeOpt(TypeOptContext ctx) {
    if (ctx instanceof NoTypeContext) {
      return null;
    }
    if (ctx instanceof WithTypeContext) {
      return visitExpr(((WithTypeContext) ctx).expr());
    }
    throw new IllegalStateException();
  }

  @Override
  public InferHoleExpression visitUnknown(UnknownContext ctx) {
    return new InferHoleExpression();
  }

  @Override
  public ZeroExpression visitZero(ZeroContext ctx) {
    return Zero();
  }

  @Override
  public NatExpression visitNat(NatContext ctx) {
    return Nat();
  }

  @Override
  public SucExpression visitSuc(SucContext ctx) {
    return Suc();
  }

  @Override
  public PiExpression visitArr(ArrContext ctx) {
    Expression left = visitExpr(ctx.expr(0));
    Expression right = visitExpr(ctx.expr(1));
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

  private List<Argument> visitLamTele(TeleContext tele) {
    List<Argument> arguments = new ArrayList<>(3);
    if (tele instanceof TeleLiteralContext) {
      LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
      if (literalContext instanceof IdContext) {
        arguments.add(Name(((IdContext) literalContext).ID().getText()));
      } else
      if (literalContext instanceof UnknownContext) {
        arguments.add(Name("_"));
      } else {
        myErrors.add(new ParserError(literalContext.getStart().getLine(), literalContext.getStart().getCharPositionInLine(), "Unexpected token. Expected an identifier."));
        return null;
      }
    } else {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      Expression varsExpr;
      Expression typeExpr;
      if (typedExpr instanceof TypedContext) {
        varsExpr = visitExpr(((TypedContext) typedExpr).expr(0));
        typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
      } else {
        varsExpr = visitExpr(((NotTypedContext) typedExpr).expr());
        typeExpr = null;
      }
      List<String> vars = getVars(varsExpr, typedExpr.getStart());
      if (vars == null) return null;
      if (typeExpr == null) {
        for (String var : vars) {
          arguments.add(Name(explicit, var));
        }
      } else {
        arguments.add(Tele(explicit, vars, typeExpr));
      }
    }
    return arguments;
  }

  @Override
  public Expression visitLam(LamContext ctx) {
    List<Argument> arguments = new ArrayList<>(ctx.tele().size());
    for (TeleContext arg : ctx.tele()) {
      List<Argument> args = visitLamTele(arg);
      if (args == null) return null;
      arguments.addAll(args);
    }
    return Lam(arguments, visitExpr(ctx.expr()));
  }

  @Override
  public Expression visitId(IdContext ctx) {
    return Var(ctx.ID().getText());
  }

  @Override
  public UniverseExpression visitUniverse(UniverseContext ctx) {
    return Universe(Integer.valueOf(ctx.UNIVERSE().getText().substring("\\Type".length())));
  }

  @Override
  public UniverseExpression visitTruncatedUniverse(TruncatedUniverseContext ctx) {
    String text = ctx.TRUNCATED_UNIVERSE().getText();
    int indexOfMinusSign = text.indexOf('-');
    return Universe(Integer.valueOf(text.substring(1, indexOfMinusSign)), Integer.valueOf(text.substring(indexOfMinusSign + "-Type".length())));
  }

  @Override
  public UniverseExpression visitProp(PropContext ctx) {
    return Universe(Universe.NO_LEVEL, Universe.Type.PROP);
  }

  @Override
  public UniverseExpression visitSet(SetContext ctx) {
    return Universe(Integer.valueOf(ctx.SET().getText().substring("\\Set".length())), 0);
  }

  private List<TypeArgument> visitTeles(List<TeleContext> teles) {
    List<TypeArgument> arguments = new ArrayList<>(teles.size());
    for (TeleContext tele : teles) {
      boolean explicit = !(tele instanceof ImplicitContext);
      TypedExprContext typedExpr;
      if (explicit) {
        if (tele instanceof ExplicitContext) {
          typedExpr = ((ExplicitContext) tele).typedExpr();
        } else {
          arguments.add(TypeArg(visitExpr(((TeleLiteralContext) tele).literal())));
          continue;
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        List<String> vars = getVars(visitExpr(((TypedContext) typedExpr).expr(0)), typedExpr.getStart());
        if (vars == null) return null;
        arguments.add(Tele(explicit, vars, visitExpr(((TypedContext) typedExpr).expr(1))));
      } else {
        arguments.add(TypeArg(explicit, visitExpr(((NotTypedContext) typedExpr).expr())));
      }
    }
    return arguments;
  }

  @Override
  public Expression visitAtomLiteral(AtomLiteralContext ctx) {
    return visitExpr(ctx.literal());
  }

  @Override
  public SigmaExpression visitSigma(SigmaContext ctx) {
    return Sigma(visitTeles(ctx.tele()));
  }

  @Override
  public PiExpression visitPi(PiContext ctx) {
    return Pi(visitTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  private Expression visitAtoms(List<AtomContext> atoms) {
    Expression result = visitExpr(atoms.get(0));
    for (int i = 1; i < atoms.size(); ++i) {
      result = Apps(result, visitExpr(atoms.get(i)));
    }
    return result;
  }

  private class Pair {
    Expression expression;
    Definition binOp;

    Pair(Expression expression, Definition binOp) {
      this.expression = expression;
      this.binOp = binOp;
    }
  }

  private void pushOnStack(List<Pair> stack, Expression left, Definition binOp, Token token) {
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
    pushOnStack(stack, BinOp(pair.expression, pair.binOp, left), binOp, token);
  }

  @Override
  public Expression visitBinOp(BinOpContext ctx) {
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
      Definition def = myGlobalContext.get(name);
      if (def == null) {
        myErrors.add(new ParserError(token.getLine(), token.getCharPositionInLine(), new NotInScopeError(Var(name)).toString()));
        return null;
      }
      pushOnStack(stack, visitAtoms(leftContext.atom()), def, token);
    }

    Expression result = visitAtoms(ctx.atom());
    for (int i = stack.size() - 1; i >= 0; --i) {
      result = BinOp(stack.get(i).expression, stack.get(i).binOp, result);
    }
    return result;
  }

  @Override
  public Expression visitExprElim(ExprElimContext ctx) {
    // TODO: Write this.
    return null;
  }
}
