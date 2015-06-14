package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private final List<VcError> myErrors;

  public BuildVisitor(List<VcError> errors) {
    myErrors = errors;
  }

  private class ParserException extends RuntimeException {}

  private List<Concrete.NameArgument> getVars(Concrete.Expression expr, Concrete.Position position) {
    List<Concrete.NameArgument> vars = new ArrayList<>();
    while (expr instanceof Concrete.AppExpression) {
      Concrete.Expression arg = ((Concrete.AppExpression) expr).getArgument().getExpression();
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
      myErrors.add(new ParserError(position, "Expected a list of variables"));
      throw new ParserException();
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
      try {
        Concrete.Definition concDef = visitDef(def);
        defs.add(concDef);
      } catch (ParserException ignored) {}
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
    if (ctx instanceof DefClassContext) {
      return visitDefClass((DefClassContext) ctx);
    }
    throw new IllegalStateException();
  }

  @Override
  public Concrete.FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    String name;
    Concrete.Position position;
    if (isPrefix) {
      name = ((NameIdContext) ctx.name()).ID().getText();
      position = tokenPosition(((NameIdContext) ctx.name()).ID().getSymbol());
    } else {
      name = ((NameBinOpContext) ctx.name()).BIN_OP().getText();
      position = tokenPosition(((NameBinOpContext) ctx.name()).BIN_OP().getSymbol());
    }
    List<Concrete.TelescopeArgument> arguments = new ArrayList<>();
    for (TeleContext tele : ctx.tele()) {
      List<Concrete.Argument> args = visitLamTele(tele);
      if (args.get(0) instanceof Concrete.TelescopeArgument) {
        arguments.add((Concrete.TelescopeArgument) args.get(0));
      } else {
        myErrors.add(new ParserError(tokenPosition(tele.getStart()), "Expected a typed variable"));
        throw new ParserException();
      }
    }
    Concrete.Expression type = visitTypeOpt(ctx.typeOpt());
    Definition.Arrow arrow = ctx.termOpt() instanceof NoTermContext ? null : ((WithTermContext) ctx.termOpt()).arrow() instanceof ArrowRightContext ? Definition.Arrow.RIGHT : Definition.Arrow.LEFT;
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(position, name, visitPrecedence(ctx.precedence()), isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, arguments, type, arrow, null);
    if (ctx.termOpt() instanceof WithTermContext) {
      def.setTerm(visitExpr(((WithTermContext) ctx.termOpt()).expr()));
    }
    return def;
  }

  public Abstract.Definition.Precedence visitPrecedence(PrecedenceContext ctx) {
    return (Abstract.Definition.Precedence) visit(ctx);
  }

  @Override
  public Abstract.Definition.Precedence visitNoPrecedence(NoPrecedenceContext ctx) {
    return Abstract.Definition.DEFAULT_PRECEDENCE;
  }

  @Override
  public Abstract.Definition.Precedence visitWithPrecedence(WithPrecedenceContext ctx) {
    int priority = Integer.valueOf(ctx.NUMBER().getText());
    if (priority < 1 || priority > 9) {
      myErrors.add(new ParserError(tokenPosition(ctx.NUMBER().getSymbol()), "Precedence out of range: " + priority));

      if (priority < 1) {
        priority = 1;
      } else {
        priority = 9;
      }
    }

    return new Abstract.Definition.Precedence((Abstract.Definition.Associativity) visit(ctx.associativity()), (byte) priority);
  }

  @Override
  public Abstract.Definition.Associativity visitNonAssoc(NonAssocContext ctx) {
    return Abstract.Definition.Associativity.NON_ASSOC;
  }

  @Override
  public Abstract.Definition.Associativity visitLeftAssoc(LeftAssocContext ctx) {
    return Abstract.Definition.Associativity.LEFT_ASSOC;
  }

  @Override
  public Abstract.Definition.Associativity visitRightAssoc(RightAssocContext ctx) {
    return Abstract.Definition.Associativity.RIGHT_ASSOC;
  }

  @Override
  public Concrete.DataDefinition visitDefData(DefDataContext ctx) {
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    String name;
    Concrete.Position position;
    if (isPrefix) {
      name = ((NameIdContext) ctx.name()).ID().getText();
      position = tokenPosition(((NameIdContext) ctx.name()).ID().getSymbol());
    } else {
      name = ((NameBinOpContext) ctx.name()).BIN_OP().getText();
      position = tokenPosition(((NameBinOpContext) ctx.name()).BIN_OP().getSymbol());
    }
    List<Concrete.TypeArgument> parameters = visitTeles(ctx.tele());
    Concrete.Expression type = visitTypeOpt(ctx.typeOpt());
    if (type != null && !(type instanceof Concrete.UniverseExpression)) {
      myErrors.add(new ParserError(tokenPosition(ctx.typeOpt().getStart()), "Expected a universe"));
      throw new ParserException();
    }

    List<Concrete.Constructor> constructors = new ArrayList<>();
    Definition.Fixity fixity = isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX;
    Universe universe = type == null ? null : ((Concrete.UniverseExpression) type).getUniverse();
    Concrete.DataDefinition def = new Concrete.DataDefinition(position, name, visitPrecedence(ctx.precedence()), fixity, universe, parameters, constructors);
    for (ConstructorContext constructor : ctx.constructor()) {
      isPrefix = constructor.name() instanceof NameIdContext;
      if (isPrefix) {
        name = ((NameIdContext) constructor.name()).ID().getText();
        position = tokenPosition(((NameIdContext) constructor.name()).ID().getSymbol());
      } else {
        name = ((NameBinOpContext) constructor.name()).BIN_OP().getText();
        position = tokenPosition(((NameBinOpContext) constructor.name()).BIN_OP().getSymbol());
      }
      try {
        constructors.add(new Concrete.Constructor(position, name, visitPrecedence(constructor.precedence()), isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, new Universe.Type(), visitTeles(constructor.tele()), def));
      } catch (ParserException ignored) {}
    }
    return def;
  }

  @Override
  public Concrete.ClassDefinition visitDefClass(DefClassContext ctx) {
    List<Concrete.Definition> fields = new ArrayList<>(ctx.defs().def().size());
    for (DefContext def : ctx.defs().def()) {
      Concrete.Definition newDef = visitDef(def);
      if (newDef != null) {
        fields.add(newDef);
      }
    }
    return new Concrete.ClassDefinition(tokenPosition(ctx.getStart()), ctx.ID().getText(), new Universe.Type(), fields);
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
      if (literalContext instanceof IdContext && ((IdContext) literalContext).name() instanceof NameIdContext) {
        arguments.add(new Concrete.NameArgument(tokenPosition(((NameIdContext) ((IdContext) literalContext).name()).ID().getSymbol()), true, ((NameIdContext) ((IdContext) literalContext).name()).ID().getText()));
      } else
      if (literalContext instanceof UnknownContext) {
        arguments.add(new Concrete.NameArgument(tokenPosition(literalContext.getStart()), true, null));
      } else {
        myErrors.add(new ParserError(tokenPosition(literalContext.getStart()), "Unexpected token. Expected an identifier."));
        throw new ParserException();
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
      List<Concrete.NameArgument> vars = getVars(varsExpr, tokenPosition(typedExpr.getStart()));
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
      arguments.addAll(visitLamTele(arg));
    }
    return arguments;
  }

  @Override
  public Concrete.Expression visitLam(LamContext ctx) {
    return new Concrete.LamExpression(tokenPosition(ctx.getStart()), visitLamTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.Expression visitId(IdContext ctx) {
    String name = ctx.name() instanceof NameIdContext ? ((NameIdContext) ctx.name()).ID().getText() : "(" + ((NameBinOpContext) ctx.name()).BIN_OP().getText() + ")";
    return new Concrete.VarExpression(tokenPosition(ctx.name().getStart()), name);
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
    return new Concrete.UniverseExpression(tokenPosition(ctx.PROP().getSymbol()), new Universe.Type(0, Universe.Type.PROP));
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
        List<Concrete.NameArgument> args = getVars(visitExpr(((TypedContext) typedExpr).expr(0)), tokenPosition(typedExpr.getStart()));
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
  public Concrete.Expression visitAtomNumber(AtomNumberContext ctx) {
    int number = Integer.valueOf(ctx.NUMBER().getText());
    Concrete.Position pos = tokenPosition(ctx.NUMBER().getSymbol());
    Concrete.Expression result = new Concrete.VarExpression(pos, Prelude.ZERO.getName());
    for (int i = 0; i < number; ++i) {
      result = new Concrete.AppExpression(pos, new Concrete.VarExpression(pos, Prelude.SUC.getName()), new Concrete.ArgumentExpression(result, true, false));
    }
    return result;
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    for (Concrete.TypeArgument arg : args) {
      if (!arg.getExplicit()) {
        myErrors.add(new ParserError(arg.getPosition(), "Fields in sigma types must be explicit"));
      }
    }
    return new Concrete.SigmaExpression(tokenPosition(ctx.getStart()), args);
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    return new Concrete.PiExpression(tokenPosition(ctx.getStart()), visitTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  private Concrete.Expression visitFieldsAcc(Concrete.Expression expr, List<FieldAccContext> fieldAccs) {
    for (FieldAccContext field : fieldAccs) {
      if (field instanceof ClassFieldContext) {
        String name;
        Abstract.Definition.Fixity fixity;
        NameContext nameCtx = ((ClassFieldContext) field).name();
        if (nameCtx instanceof NameIdContext) {
          name = ((NameIdContext) nameCtx).ID().getText();
          fixity = Abstract.Definition.Fixity.PREFIX;
        } else {
          name = ((NameBinOpContext) nameCtx).BIN_OP().getText();
          fixity = Abstract.Definition.Fixity.INFIX;
        }
        expr = new Concrete.FieldAccExpression(expr.getPosition(), expr, name, fixity);
      } else {
        expr = new Concrete.ProjExpression(expr.getPosition(), expr, Integer.valueOf(((SigmaFieldContext) field).NUMBER().getText()) - 1);
      }
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    return visitFieldsAcc(visitExpr(ctx.atom()), ctx.fieldAcc());
  }

  private Concrete.Expression visitAtoms(Concrete.Expression expr, List<ArgumentContext> arguments) {
    for (ArgumentContext argument : arguments) {
      boolean explicit = argument instanceof ArgumentExplicitContext;
      Concrete.Expression expr1;
      if (explicit) {
        expr1 = visitAtomFieldsAcc(((ArgumentExplicitContext) argument).atomFieldsAcc());
      } else {
        expr1 = visitExpr(((ArgumentImplicitContext) argument).expr());
      }
      expr = new Concrete.AppExpression(expr.getPosition(), expr, new Concrete.ArgumentExpression(expr1, explicit, false));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOp(BinOpContext ctx) {
    if (ctx.binOpLeft().size() == 0) {
      return visitAtoms(visitAtomFieldsAcc(ctx.atomFieldsAcc()), ctx.argument());
    }

    List<Concrete.Expression> arguments = new ArrayList<>(ctx.binOpLeft().size() + 1);
    List<Concrete.Expression> operators = new ArrayList<>(ctx.binOpLeft().size());

    for (BinOpLeftContext leftContext : ctx.binOpLeft()) {
      if (leftContext.infix() instanceof InfixBinOpContext) {
        operators.add(new Concrete.VarExpression(tokenPosition(((InfixBinOpContext) leftContext.infix()).BIN_OP().getSymbol()), ((InfixBinOpContext) leftContext.infix()).BIN_OP().getText()));
      } else {
        InfixIdContext infixId = (InfixIdContext) leftContext.infix();
        operators.add(visitFieldsAcc(new Concrete.VarExpression(tokenPosition(infixId.name().getStart()), ((NameIdContext) infixId.name()).ID().getText()), infixId.fieldAcc()));
      }
      arguments.add(visitAtoms(visitAtomFieldsAcc(leftContext.atomFieldsAcc()), leftContext.argument()));
    }

    arguments.add(visitAtoms(visitAtomFieldsAcc(ctx.atomFieldsAcc()), ctx.argument()));
    return new Concrete.BinOpExpression(arguments, operators);
  }

  @Override
  public Concrete.Expression visitExprElim(ExprElimContext ctx) {
    List<Concrete.Clause> clauses = new ArrayList<>(ctx.clause().size());
    Concrete.Clause otherwise = null;
    for (ClauseContext clauseCtx : ctx.clause()) {
      Concrete.Clause clause = visitClause(clauseCtx);
      if (clause.getName() == null) {
        if (otherwise != null) {
          myErrors.add(new ParserError(tokenPosition(clauseCtx.clauseName().getStart()), "Overlapping pattern matching"));
        }
        otherwise = clause;
      } else {
        clauses.add(clause);
      }
    }

    Abstract.ElimExpression.ElimType elimType = ctx.elimCase() instanceof ElimContext ? Abstract.ElimExpression.ElimType.ELIM : Abstract.ElimExpression.ElimType.CASE;
    Concrete.ElimExpression result = new Concrete.ElimExpression(tokenPosition(ctx.getStart()), elimType, visitExpr(ctx.expr()), clauses, otherwise);
    for (Concrete.Clause clause : clauses) {
      clause.setElimExpression(result);
    }
    if (otherwise != null) {
      otherwise.setElimExpression(result);
    }
    return result;
  }

  @Override
  public String visitClauseNoName(ClauseNoNameContext ctx) {
    return null;
  }

  @Override
  public String visitClauseNameArgs(ClauseNameArgsContext ctx) {
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    return isPrefix ? ((NameIdContext) ctx.name()).ID().getText() : ((NameBinOpContext) ctx.name()).BIN_OP().getText();
  }

  @Override
  public Concrete.Clause visitClause(ClauseContext ctx) {
    String name = (String) visit(ctx.clauseName());
    Definition.Arrow arrow = ctx.arrow() instanceof ArrowRightContext ? Definition.Arrow.RIGHT : Definition.Arrow.LEFT;
    List<Concrete.Argument> arguments = ctx.clauseName() instanceof ClauseNameArgsContext ? visitLamTeles(((ClauseNameArgsContext) ctx.clauseName()).tele()) : null;
    return new Concrete.Clause(tokenPosition(ctx.getStart()), name, arguments, arrow, visitExpr(ctx.expr()), null);
  }

  private static Concrete.Position tokenPosition(Token token) {
    return new Concrete.Position(token.getLine(), token.getCharPositionInLine());
  }

  @Override
  public Concrete.ErrorExpression visitHole(HoleContext ctx) {
    return new Concrete.ErrorExpression(tokenPosition(ctx.getStart()));
  }
}
