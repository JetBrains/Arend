package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private ClassDefinition myParent;
  private List<String> myContext = new ArrayList<>();
  private final Module myModule;
  private final ModuleLoader myModuleLoader;

  public BuildVisitor(ClassDefinition parent, ModuleLoader moduleLoader) {
    myParent = parent;
    myModule = new Module((ClassDefinition) parent.getParent(), parent.getName());
    myModuleLoader = moduleLoader;
  }

  private Concrete.NameArgument getVar(AtomFieldsAccContext ctx) {
    if (!ctx.fieldAcc().isEmpty() || !(ctx.atom() instanceof AtomLiteralContext)) {
      return null;
    }
    LiteralContext literal = ((AtomLiteralContext) ctx.atom()).literal();
    if (literal instanceof UnknownContext) {
      return new Concrete.NameArgument(tokenPosition(literal.getStart()), true, "_");
    }
    if (literal instanceof IdContext && ((IdContext) literal).name() instanceof NameIdContext) {
      return new Concrete.NameArgument(tokenPosition(literal.getStart()), true, ((NameIdContext) ((IdContext) literal).name()).ID().getText());
    }
    return null;
  }

  private List<Concrete.NameArgument> getVarsNull(ExprContext expr) {
    if (!(expr instanceof BinOpContext && ((BinOpContext) expr).binOpLeft().isEmpty())) {
      return null;
    }
    Concrete.NameArgument firstArg = getVar(((BinOpContext) expr).atomFieldsAcc());
    if (firstArg == null) {
      return null;
    }

    List<Concrete.NameArgument> result = new ArrayList<>();
    result.add(firstArg);
    for (ArgumentContext argument : ((BinOpContext) expr).argument()) {
      if (argument instanceof ArgumentExplicitContext) {
        Concrete.NameArgument arg = getVar(((ArgumentExplicitContext) argument).atomFieldsAcc());
        if (arg == null) {
          return null;
        }
        result.add(arg);
      } else {
        List<Concrete.NameArgument> arguments = getVarsNull(((ArgumentImplicitContext) argument).expr());
        if (arguments == null) {
          return null;
        }
        for (Concrete.NameArgument arg : arguments) {
          arg.setExplicit(false);
          result.add(arg);
        }
      }
    }
    return result;
  }

  private List<Concrete.NameArgument> getVars(ExprContext expr) {
    List<Concrete.NameArgument> result = getVarsNull(expr);
    if (result == null) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(expr.getStart()), "Expected a list of variables"));
      return null;
    } else {
      return result;
    }
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
  public Void visitDefs(DefsContext ctx) {
    if (ctx == null) return null;
    for (DefContext def : ctx.def()) {
      visitDef(def);
    }
    return null;
  }

  public List<Concrete.FunctionDefinition> visitDefsRaw(DefsContext ctx) {
    if (ctx == null) return null;
    List<Concrete.FunctionDefinition> definitions = new ArrayList<>(ctx.def().size());
    for (DefContext def : ctx.def()) {
      if (def instanceof DefOverrideContext) {
        DefOverrideContext defOverride = (DefOverrideContext) def;
        Concrete.FunctionDefinition definition = visitFunctionRawBegin(true, null, defOverride.name(), defOverride.tele(), defOverride.expr().size() == 1 ? null : defOverride.expr(0), defOverride.arrow());
        if (definition != null) {
          ExprContext termCtx = defOverride.expr(defOverride.expr().size() == 1 ? 0 : 1);
          if (termCtx != null) {
            definition.setTerm(visitExpr(termCtx));
          }
          visitFunctionRawEnd(definition);
          definitions.add(definition);
        }
      } else {
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.getStart()), "Expected an overridden function"));
      }
    }
    return definitions;
  }

  public Definition visitDef(DefContext ctx) {
    if (ctx instanceof DefFunctionContext) {
      return visitDefFunction((DefFunctionContext) ctx);
    } else
    if (ctx instanceof DefDataContext) {
      return visitDefData((DefDataContext) ctx);
    } else
    if (ctx instanceof DefClassContext) {
      ClassDefinition classDef = myParent.getClass(((DefClassContext) ctx).ID().getText(), myModuleLoader.getErrors());
      if (classDef == null) return null;
      classDef.hasErrors(false);
      ClassDefinition oldParent = myParent;
      myParent = classDef;
      visitDefClass((DefClassContext) ctx);
      myParent = oldParent;
      myParent.addStaticField(classDef, myModuleLoader.getErrors());
      return classDef;
    } else
    if (ctx instanceof DefCmdContext) {
      visitDefCmd((DefCmdContext) ctx);
      return null;
    } else
    if (ctx instanceof DefOverrideContext) {
      return visitDefOverride((DefOverrideContext) ctx);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Void visitDefCmd(DefCmdContext ctx) {
    // TODO
    /*
    if (ctx == null) return null;
    Definition module = visitModule(ctx.name(0), ctx.fieldAcc());
    if (module == null) return null;
    boolean remove = ctx.nsCmd() instanceof CloseCmdContext;
    boolean export = ctx.nsCmd() instanceof ExportCmdContext;
    if (ctx.name().size() > 1) {
      for (int i = 1; i < ctx.name().size(); ++i) {
        String name = ctx.name(i) instanceof NameBinOpContext ? ((NameBinOpContext) ctx.name(i)).BIN_OP().getText() : ((NameIdContext) ctx.name(i)).ID().getText();
        Definition definition = module.findChild(name);
        if (definition == null) {
          myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.name(i).getStart()), name + " is not exported from " + module.getFullName()));
          continue;
        }

        if (remove) {
          myParent.remove(definition);
        } else {
          if (!definition.isAbstract() && definition.getDependencies().isEmpty()) {
            myParent.add(definition, export, myModuleLoader.getErrors());
          }
        }
      }
    } else {
      if (module.getChildren() == null) return null;
      for (Definition definition : module.getChildren()) {
        if (remove) {
          myParent.remove(definition);
        } else {
          if (!definition.isAbstract() && (definition.getDependencies() == null || definition.getDependencies().isEmpty())) {
            myParent.add(definition, export, myModuleLoader.getErrors());
          }
        }
      }
    }
    */
    return null;
  }

  @Override
  public FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    if (ctx == null) return null;
    ExprContext typeCtx = ctx.typeTermOpt() instanceof WithTypeContext ? ((WithTypeContext) ctx.typeTermOpt()).expr() : ctx.typeTermOpt() instanceof WithTypeAndTermContext ? ((WithTypeAndTermContext) ctx.typeTermOpt()).expr(0) : null;
    ArrowContext arrowCtx = ctx.typeTermOpt() instanceof WithTermContext ? ((WithTermContext) ctx.typeTermOpt()).arrow() : ctx.typeTermOpt() instanceof WithTypeAndTermContext ? ((WithTypeAndTermContext) ctx.typeTermOpt()).arrow() : null;
    ExprContext termCtx = ctx.typeTermOpt() instanceof WithTermContext ? ((WithTermContext) ctx.typeTermOpt()).expr() : ctx.typeTermOpt() instanceof WithTypeAndTermContext ? ((WithTypeAndTermContext) ctx.typeTermOpt()).expr(1) : null;
    return visitDefFunction(false, ctx.precedence(), ctx.name(), ctx.tele(), typeCtx, arrowCtx, termCtx);
  }

  @Override
  public FunctionDefinition visitDefOverride(DefOverrideContext ctx) {
    if (ctx == null) return null;
    return visitDefFunction(true, null, ctx.name(), ctx.tele(), ctx.expr().size() == 1 ? null : ctx.expr(0), ctx.arrow(), ctx.expr(ctx.expr().size() == 1 ? 0 : 1));
  }

  private FunctionDefinition visitDefFunction(boolean overridden, PrecedenceContext precCtx, NameContext nameCtx, List<TeleContext> teleCtx, ExprContext typeCtx, ArrowContext arrowCtx, ExprContext termCtx) {
    Concrete.FunctionDefinition def = visitFunctionRawBegin(overridden, precCtx, nameCtx, teleCtx, typeCtx, arrowCtx);
    if (def == null) {
      return null;
    }

    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(myModuleLoader, myParent, def, localContext, null);
    if (typedDef != null && termCtx != null) {
      TypeChecking.typeCheckFunctionEnd(myModuleLoader, myParent, visitExpr(termCtx), typedDef, localContext, null);
    }
    visitFunctionRawEnd(def);
    return typedDef;
  }

  private Concrete.FunctionDefinition visitFunctionRawBegin(boolean overridden, PrecedenceContext precCtx, NameContext nameCtx, List<TeleContext> teleCtx, ExprContext typeCtx, ArrowContext arrowCtx) {
    boolean isPrefix = nameCtx instanceof NameIdContext;
    String name;
    Concrete.Position position;
    if (isPrefix) {
      name = ((NameIdContext) nameCtx).ID().getText();
      position = tokenPosition(((NameIdContext) nameCtx).ID().getSymbol());
    } else {
      name = ((NameBinOpContext) nameCtx).BIN_OP().getText();
      position = tokenPosition(((NameBinOpContext) nameCtx).BIN_OP().getSymbol());
    }

    int size = myContext.size();
    List<Concrete.Argument> arguments = new ArrayList<>();
    for (TeleContext tele : teleCtx) {
      List<Concrete.Argument> args = visitLamTele(tele);
      if (args == null) {
        trimToSize(myContext, size);
        return null;
      }

      if (overridden || args.get(0) instanceof Concrete.TelescopeArgument) {
        arguments.add(args.get(0));
      } else {
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(tele.getStart()), "Expected a typed variable"));
        trimToSize(myContext, size);
        return null;
      }
    }

    Concrete.Expression type = typeCtx == null ? null : visitExpr(typeCtx);
    Definition.Arrow arrow = arrowCtx instanceof ArrowLeftContext ? Abstract.Definition.Arrow.LEFT : arrowCtx instanceof ArrowRightContext ? Abstract.Definition.Arrow.RIGHT : null;
    Abstract.Definition.Fixity fixity = isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX;
    return new Concrete.FunctionDefinition(position, name, precCtx == null ? null : visitPrecedence(precCtx), fixity, arguments, type, arrow, null, overridden);
  }

  private void visitFunctionRawEnd(Concrete.FunctionDefinition definition) {
    for (Concrete.Argument argument : definition.getArguments()) {
      if (argument instanceof Concrete.TelescopeArgument) {
        for (String ignored : ((Concrete.TelescopeArgument) argument).getNames()) {
          myContext.remove(myContext.size() - 1);
        }
      } else {
        myContext.remove(myContext.size() - 1);
      }
    }
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
    if (ctx == null) return null;
    int priority = Integer.valueOf(ctx.NUMBER().getText());
    if (priority < 1 || priority > 9) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.NUMBER().getSymbol()), "Precedence out of range: " + priority));

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
  public DataDefinition visitDefData(DefDataContext ctx) {
    if (ctx == null) return null;
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
    int origSize = myContext.size();
    List<Concrete.TypeArgument> parameters = visitTeles(ctx.tele());
    if (parameters == null) {
      trimToSize(myContext, origSize);
      return null;
    }

    Concrete.Expression type = ctx.expr() == null ? null : visitExpr(ctx.expr());
    if (type != null && !(type instanceof Concrete.UniverseExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.expr().getStart()), "Expected a universe"));
      trimToSize(myContext, origSize);
      return null;
    }

    Definition.Fixity fixity = isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX;
    Universe universe = type == null ? null : ((Concrete.UniverseExpression) type).getUniverse();
    Concrete.DataDefinition def = new Concrete.DataDefinition(position, name, visitPrecedence(ctx.precedence()), fixity, universe, parameters, null);
    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(myModuleLoader, myParent, def, localContext);

    int size = myContext.size();
    int index = 0;
    for (int i = 0; i < ctx.constructor().size(); ++i) {
      isPrefix = ctx.constructor(i).name() instanceof NameIdContext;
      if (isPrefix) {
        name = ((NameIdContext) ctx.constructor(i).name()).ID().getText();
        position = tokenPosition(((NameIdContext) ctx.constructor(i).name()).ID().getSymbol());
      } else {
        name = ((NameBinOpContext) ctx.constructor(i).name()).BIN_OP().getText();
        position = tokenPosition(((NameBinOpContext) ctx.constructor(i).name()).BIN_OP().getSymbol());
      }

      List<Concrete.TypeArgument> arguments = visitTeles(ctx.constructor(i).tele());
      trimToSize(myContext, size);
      if (arguments == null) continue;
      Concrete.Constructor con = new Concrete.Constructor(position, name, visitPrecedence(ctx.constructor(i).precedence()), isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, new Universe.Type(), arguments, def);
      if (TypeChecking.typeCheckConstructor(myModuleLoader, typedDef, con, localContext, index) != null) {
        ++index;
      }
    }

    trimToSize(myContext, origSize);
    TypeChecking.typeCheckDataEnd(myModuleLoader, myParent, def, typedDef, null);
    return typedDef;
  }

  @Override
  public Void visitDefClass(DefClassContext ctx) {
    if (ctx == null) return null;
    visitDefs(ctx.classFields().defs());
    return null;
  }

  @Override
  public Concrete.InferHoleExpression visitUnknown(UnknownContext ctx) {
    if (ctx == null) return null;
    return new Concrete.InferHoleExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    if (ctx == null) return null;
    Concrete.Expression domain = visitExpr(ctx.expr(0));
    if (domain == null) return null;
    List<Concrete.TypeArgument> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeArgument(domain.getPosition(), true, domain));
    myContext.add(null);
    Concrete.Expression codomain = visitExpr(ctx.expr(1));
    myContext.remove(myContext.size() - 1);
    if (codomain == null) return null;
    return new Concrete.PiExpression(tokenPosition(ctx.getToken(ARROW, 0).getSymbol()), arguments, codomain);
  }

  @Override
  public Concrete.Expression visitTuple(TupleContext ctx) {
    if (ctx == null) return null;
    if (ctx.expr().size() == 1) {
      return visitExpr(ctx.expr(0));
    } else {
      List<Concrete.Expression> fields = new ArrayList<>(ctx.expr().size());
      for (ExprContext exprCtx : ctx.expr()) {
        Concrete.Expression expr = visitExpr(exprCtx);
        if (expr == null) return null;
        fields.add(expr);
      }
      return new Concrete.TupleExpression(tokenPosition(ctx.getStart()), fields);
    }
  }

  private List<Concrete.Argument> visitLamTele(TeleContext tele) {
    List<Concrete.Argument> arguments = new ArrayList<>(3);
    if (tele instanceof TeleLiteralContext) {
      LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
      if (literalContext instanceof IdContext && ((IdContext) literalContext).name() instanceof NameIdContext) {
        String name = ((NameIdContext) ((IdContext) literalContext).name()).ID().getText();
        arguments.add(new Concrete.NameArgument(tokenPosition(((NameIdContext) ((IdContext) literalContext).name()).ID().getSymbol()), true, name));
        myContext.add(name);
      } else
      if (literalContext instanceof UnknownContext) {
        arguments.add(new Concrete.NameArgument(tokenPosition(literalContext.getStart()), true, "_"));
        myContext.add(null);
      } else {
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(literalContext.getStart()), "Unexpected token. Expected an identifier."));
        return null;
      }
    } else {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      ExprContext varsExpr;
      Concrete.Expression typeExpr;
      if (typedExpr instanceof TypedContext) {
        varsExpr = ((TypedContext) typedExpr).expr(0);
        typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
        if (typeExpr == null) return null;
      } else {
        varsExpr = ((NotTypedContext) typedExpr).expr();
        typeExpr = null;
      }
      List<Concrete.NameArgument> vars = getVars(varsExpr);
      if (vars == null) return null;

      if (typeExpr == null) {
        arguments.addAll(vars);
        for (Concrete.NameArgument var : vars) {
          myContext.add(var.getName());
        }
      } else {
        List<String> args = new ArrayList<>(vars.size());
        for (Concrete.NameArgument var : vars) {
          args.add(var.getName());
          myContext.add(var.getName());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, args, typeExpr));
      }
    }
    return arguments;
  }

  private List<Concrete.Argument> visitLamTeles(List<TeleContext> tele) {
    List<Concrete.Argument> arguments = new ArrayList<>();
    for (TeleContext arg : tele) {
      List<Concrete.Argument> arguments1 = visitLamTele(arg);
      if (arguments1 == null) return null;
      arguments.addAll(arguments1);
    }
    return arguments;
  }

  @Override
  public Concrete.Expression visitLam(LamContext ctx) {
    if (ctx == null) return null;
    int size = myContext.size();
    List<Concrete.Argument> args = visitLamTeles(ctx.tele());
    if (args == null) {
      trimToSize(myContext, size);
      return null;
    }
    Concrete.Expression body = visitExpr(ctx.expr());
    trimToSize(myContext, size);
    if (body == null) return null;
    return new Concrete.LamExpression(tokenPosition(ctx.getStart()), args, body);
  }

  @Override
  public Concrete.Expression visitId(IdContext ctx) {
    if (ctx == null) return null;
    boolean binOp = ctx.name() instanceof NameBinOpContext;
    String name = binOp ? ((NameBinOpContext) ctx.name()).BIN_OP().getText() : ((NameIdContext) ctx.name()).ID().getText();
    return findId(name, binOp, tokenPosition(ctx.name().getStart()));
  }

  private Concrete.Expression findId(String name, boolean binOp, Concrete.Position position) {
    Definition child = Prelude.PRELUDE.findChild(name);
    if (child != null) {
      return new Concrete.DefCallExpression(position, child);
    }

    if (!binOp) {
      for (int i = myContext.size() - 1; i >= 0; --i) {
        if (name.equals(myContext.get(i))) {
          return new Concrete.IndexExpression(position, myContext.size() - 1 - i);
        }
      }
    }

    for (Definition definition = myParent; definition != null; definition = definition.getParent()) {
      if (!(definition instanceof ClassDefinition)) {
        continue;
      }

      Definition field = ((ClassDefinition) definition).getPrivateField(name);
      if (field != null) {
        return new Concrete.DefCallExpression(position, field);
      }
    }

    if (!binOp) {
      Definition definition = myModuleLoader.loadModule(new Module(myModuleLoader.rootModule(), name), true);
      if (definition != null) {
        return new Concrete.DefCallExpression(position, definition);
      }
    }

    myModuleLoader.getErrors().add(new ParserError(myModule, position, "Not in scope: " + name));
    return null;
  }

  @Override
  public Concrete.UniverseExpression visitUniverse(UniverseContext ctx) {
    if (ctx == null) return null;
    return new Concrete.UniverseExpression(tokenPosition(ctx.UNIVERSE().getSymbol()), new Universe.Type(Integer.valueOf(ctx.UNIVERSE().getText().substring("\\Type".length()))));
  }

  @Override
  public Concrete.UniverseExpression visitTruncatedUniverse(TruncatedUniverseContext ctx) {
    if (ctx == null) return null;
    String text = ctx.TRUNCATED_UNIVERSE().getText();
    int indexOfMinusSign = text.indexOf('-');
    return new Concrete.UniverseExpression(tokenPosition(ctx.TRUNCATED_UNIVERSE().getSymbol()), new Universe.Type(Integer.valueOf(text.substring(1, indexOfMinusSign)), Integer.valueOf(text.substring(indexOfMinusSign + "-Type".length()))));
  }

  @Override
  public Concrete.UniverseExpression visitProp(PropContext ctx) {
    if (ctx == null) return null;
    return new Concrete.UniverseExpression(tokenPosition(ctx.PROP().getSymbol()), new Universe.Type(0, Universe.Type.PROP));
  }

  @Override
  public Concrete.UniverseExpression visitSet(SetContext ctx) {
    if (ctx == null) return null;
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
          Concrete.Expression expr = visitExpr(((TeleLiteralContext) tele).literal());
          if (expr == null) return null;
          arguments.add(new Concrete.TypeArgument(true, expr));
          myContext.add(null);
          continue;
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        Concrete.Expression type = visitExpr(((TypedContext) typedExpr).expr(1));
        if (type == null) return null;
        List<Concrete.NameArgument> args = getVars(((TypedContext) typedExpr).expr(0));
        if (args == null) return null;

        List<String> vars = new ArrayList<>(args.size());
        for (Concrete.NameArgument arg : args) {
          vars.add(arg.getName());
          myContext.add(arg.getName());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, vars, type));
      } else {
        Concrete.Expression expr = visitExpr(((NotTypedContext) typedExpr).expr());
        if (expr == null) return null;
        arguments.add(new Concrete.TypeArgument(explicit, expr));
        myContext.add(null);
      }
    }
    return arguments;
  }

  @Override
  public Concrete.Expression visitAtomLiteral(AtomLiteralContext ctx) {
    if (ctx == null) return null;
    return visitExpr(ctx.literal());
  }

  @Override
  public Concrete.Expression visitAtomNumber(AtomNumberContext ctx) {
    if (ctx == null) return null;
    int number = Integer.valueOf(ctx.NUMBER().getText());
    Concrete.Position pos = tokenPosition(ctx.NUMBER().getSymbol());
    Concrete.Expression result = new Concrete.DefCallExpression(pos, Prelude.ZERO);
    for (int i = 0; i < number; ++i) {
      result = new Concrete.AppExpression(pos, new Concrete.DefCallExpression(pos, Prelude.SUC), new Concrete.ArgumentExpression(result, true, false));
    }
    return result;
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    if (ctx == null) return null;
    int size = myContext.size();
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    trimToSize(myContext, size);
    if (args == null) return null;

    for (Concrete.TypeArgument arg : args) {
      if (!arg.getExplicit()) {
        myModuleLoader.getErrors().add(new ParserError(myModule, arg.getPosition(), "Fields in sigma types must be explicit"));
      }
    }
    return new Concrete.SigmaExpression(tokenPosition(ctx.getStart()), args);
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    if (ctx == null) return null;
    int size = myContext.size();
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    if (args == null) {
      trimToSize(myContext, size);
      return null;
    }
    Concrete.Expression codomain = visitExpr(ctx.expr());
    trimToSize(myContext, size);
    if (codomain == null) return null;
    return new Concrete.PiExpression(tokenPosition(ctx.getStart()), args, codomain);
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
        if (expr instanceof Concrete.DefCallExpression) {
          Definition definition = ((Concrete.DefCallExpression) expr).getDefinition();
          Definition classField = definition.findChild(name);
          if (classField == null && definition instanceof ClassDefinition) {
            classField = myModuleLoader.loadModule(new Module((ClassDefinition) definition, name), true);
          }
          if (classField == null && definition instanceof FunctionDefinition) {
            FunctionDefinition functionDefinition = (FunctionDefinition) definition;
            if (!functionDefinition.typeHasErrors() && functionDefinition.getArguments().isEmpty() && functionDefinition.getResultType() instanceof DefCallExpression && ((DefCallExpression) functionDefinition.getResultType()).getDefinition() instanceof ClassDefinition) {
              expr = new Concrete.FieldAccExpression(expr.getPosition(), expr, name, fixity);
              continue;
            }
          }
          if (classField == null) {
            myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(nameCtx.getStart()), name + " is not a static field of " + definition.getFullName()));
            return null;
          }
          expr = new Concrete.DefCallExpression(expr.getPosition(), classField);
          continue;
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
    if (ctx == null) return null;
    Concrete.Expression expr = visitExpr(ctx.atom());
    if (expr == null) return null;

    expr = visitFieldsAcc(expr, ctx.fieldAcc());
    if (expr == null) return null;

    if (ctx.classFields() != null) {
      if (!(expr instanceof Concrete.DefCallExpression && ((Concrete.DefCallExpression) expr).getDefinition() instanceof ClassDefinition)) {
        myModuleLoader.getErrors().add(new ParserError(myModule, expr.getPosition(), "Expected a class name"));
        return null;
      }

      ClassDefinition newParent = (ClassDefinition) ((Concrete.DefCallExpression) expr).getDefinition();
      ClassDefinition oldParent = myParent;
      myParent = newParent;
      List<Concrete.FunctionDefinition> definitions = visitDefsRaw(ctx.classFields().defs());
      myParent = oldParent;

      expr = new Concrete.ClassExtExpression(tokenPosition(ctx.getStart()), newParent, definitions);
    }
    return expr;
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
      if (expr1 == null) return null;
      expr = new Concrete.AppExpression(expr.getPosition(), expr, new Concrete.ArgumentExpression(expr1, explicit, false));
    }
    return expr;
  }

  private class StackElem {
    public Concrete.Expression argument;
    public Definition definition;

    public StackElem(Concrete.Expression argument, Definition definition) {
      this.argument = argument;
      this.definition = definition;
    }
  }

  private void pushOnStack(List<StackElem> stack, StackElem elem, Concrete.Position position) {
    if (stack.isEmpty()) {
      stack.add(elem);
      return;
    }

    StackElem topElem = stack.get(stack.size() - 1);
    Definition.Precedence prec = topElem.definition.getPrecedence();
    Definition.Precedence prec2 = elem.definition.getPrecedence();

    if (prec.priority < prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.RIGHT_ASSOC && prec2.associativity == Definition.Associativity.RIGHT_ASSOC)) {
      stack.add(elem);
      return;
    }

    if (!(prec.priority > prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.LEFT_ASSOC && prec2.associativity == Definition.Associativity.LEFT_ASSOC))) {
      String msg = "Precedence parsing error: cannot mix (" + topElem.definition.getName() + ") [" + prec + "] and (" + elem.definition.getName() + ") [" + prec2 + "] in the same infix expression";
      myModuleLoader.getErrors().add(new ParserError(myModule, position, msg));
    }
    stack.remove(stack.size() - 1);
    pushOnStack(stack, new StackElem(new Concrete.BinOpExpression(position, new Concrete.ArgumentExpression(topElem.argument, true, false), topElem.definition, new Concrete.ArgumentExpression(elem.argument, true, false)), elem.definition), position);
  }

  private Concrete.Expression rollUpStack(List<StackElem> stack, Concrete.Expression expr) {
    for (int i = stack.size() - 1; i >= 0; --i) {
      expr = new Concrete.BinOpExpression(stack.get(i).argument.getPosition(), new Concrete.ArgumentExpression(stack.get(i).argument, true, false), stack.get(i).definition, new Concrete.ArgumentExpression(expr, true, false));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOp(BinOpContext ctx) {
    if (ctx == null) return null;
    List<StackElem> stack = new ArrayList<>(ctx.binOpLeft().size());
    for (BinOpLeftContext leftContext : ctx.binOpLeft()) {
      Concrete.Position position = tokenPosition(leftContext.infix().getStart());
      Definition def;
      if (leftContext.infix() instanceof InfixBinOpContext) {
        def = visitInfixBinOp((InfixBinOpContext) leftContext.infix());
      } else {
        def = visitInfixId((InfixIdContext) leftContext.infix());
      }

      Concrete.Expression expr = visitAtomFieldsAcc(leftContext.atomFieldsAcc());
      if (expr == null) continue;
      expr = visitAtoms(expr, leftContext.argument());
      if (expr == null) continue;
      if (leftContext.maybeNew() instanceof WithNewContext) {
        expr = new Concrete.NewExpression(tokenPosition(leftContext.getStart()), expr);
      }

      if (def == null) {
        stack = new ArrayList<>(ctx.binOpLeft().size() - stack.size());
        continue;
      }

      pushOnStack(stack, new StackElem(expr, def), position);
    }

    Concrete.Expression expr = visitAtomFieldsAcc(ctx.atomFieldsAcc());
    if (expr == null) return null;
    expr = visitAtoms(expr, ctx.argument());
    if (expr == null) return null;
    if (ctx.maybeNew() instanceof WithNewContext) {
      expr = new Concrete.NewExpression(tokenPosition(ctx.getStart()), expr);
    }
    return rollUpStack(stack, expr);
  }

  @Override
  public Definition visitInfixBinOp(InfixBinOpContext ctx) {
    if (ctx == null) return null;
    String name = ctx.BIN_OP().getText();
    Concrete.Position position = tokenPosition(ctx.BIN_OP().getSymbol());
    Concrete.Expression expr = findId(name, true, position);
    if (expr == null) return null;
    if (!(expr instanceof Concrete.DefCallExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, position, "Infix notation cannot be used with local variables"));
      return null;
    }

    return ((Concrete.DefCallExpression) expr).getDefinition();
  }

  @Override
  public Definition visitInfixId(InfixIdContext ctx) {
    if (ctx == null) return null;
    return visitModule(ctx.name(), ctx.fieldAcc());
  }

  public Definition visitModule(NameContext nameCtx, List<FieldAccContext> fieldAccCtx) {
    boolean binOp = nameCtx instanceof NameBinOpContext;
    String name = binOp ? ((NameBinOpContext) nameCtx).BIN_OP().getText() : ((NameIdContext) nameCtx).ID().getText();
    Concrete.Expression expr = findId(name, binOp, tokenPosition(nameCtx.getStart()));
    if (expr == null) return null;
    if (!(expr instanceof Concrete.DefCallExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(nameCtx.getStart()), "Expected a global definition"));
      return null;
    }

    expr = visitFieldsAcc(expr, fieldAccCtx);
    if (expr == null) return null;
    if (!(expr instanceof Concrete.DefCallExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(nameCtx.getStart()), "Expected a global definition"));
      return null;
    }

    return ((Concrete.DefCallExpression) expr).getDefinition();
  }

  @Override
  public Concrete.Expression visitExprElim(ExprElimContext ctx) {
    if (ctx == null) return null;
    List<Concrete.Clause> clauses = new ArrayList<>(ctx.clause().size());
    Concrete.Clause otherwise = null;

    Concrete.Expression elimExpr = visitExpr(ctx.expr());
    if (elimExpr == null) return null;
    if (!(elimExpr instanceof Concrete.IndexExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, elimExpr.getPosition(), "\\elim can be applied only to a local variable"));
      return null;
    }
    int elimIndex = ((Concrete.IndexExpression) elimExpr).getIndex();

    List<String> oldContext = new ArrayList<>(myContext);
    for (ClauseContext clauseCtx : ctx.clause()) {
      String name = (String) visit(clauseCtx.clauseName());
      Definition.Arrow arrow = clauseCtx.arrow() instanceof ArrowRightContext ? Definition.Arrow.RIGHT : Definition.Arrow.LEFT;
      List<Concrete.NameArgument> nameArguments = null;
      if (clauseCtx.clauseName() instanceof ClauseNameArgsContext) {
        List<Concrete.Argument> arguments = visitLamTeles(((ClauseNameArgsContext) clauseCtx.clauseName()).tele());
        if (arguments == null) {
          myContext = oldContext;
          return null;
        }

        nameArguments = new ArrayList<>(arguments.size());
        for (Concrete.Argument argument : arguments) {
          if (argument instanceof Concrete.NameArgument) {
            nameArguments.add((Concrete.NameArgument) argument);
          } else {
            myModuleLoader.getErrors().add(new ParserError(myModule, argument.getPosition(), "Expected a variable"));
            myContext = oldContext;
            return null;
          }
        }

        myContext.clear();
        for (int i = 0; i < oldContext.size() - elimIndex - 1; ++i) {
          myContext.add(oldContext.get(i));
        }
        for (Concrete.NameArgument argument : nameArguments) {
          myContext.add(argument.getName());
        }
        for (int i = oldContext.size() - elimIndex; i < oldContext.size(); ++i) {
          myContext.add(oldContext.get(i));
        }
      }

      Concrete.Expression expr = visitExpr(clauseCtx.expr());
      if (expr == null) {
        myContext = oldContext;
        return null;
      }
      Concrete.Clause clause = new Concrete.Clause(tokenPosition(clauseCtx.getStart()), name, nameArguments, arrow, expr, null);

      if (name == null) {
        if (otherwise != null) {
          myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(clauseCtx.clauseName().getStart()), "Overlapping pattern matching"));
        }
        otherwise = clause;
      } else {
        clauses.add(clause);
      }

      myContext = new ArrayList<>(oldContext);
    }

    Abstract.ElimExpression.ElimType elimType = ctx.elimCase() instanceof ElimContext ? Abstract.ElimExpression.ElimType.ELIM : Abstract.ElimExpression.ElimType.CASE;
    Concrete.ElimExpression result = new Concrete.ElimExpression(tokenPosition(ctx.getStart()), elimType, elimExpr, clauses, otherwise);
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
    if (ctx == null) return null;
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    return isPrefix ? ((NameIdContext) ctx.name()).ID().getText() : ((NameBinOpContext) ctx.name()).BIN_OP().getText();
  }

  private static Concrete.Position tokenPosition(Token token) {
    return new Concrete.Position(token.getLine(), token.getCharPositionInLine());
  }

  @Override
  public Concrete.ErrorExpression visitHole(HoleContext ctx) {
    if (ctx == null) return null;
    return new Concrete.ErrorExpression(tokenPosition(ctx.getStart()));
  }
}
