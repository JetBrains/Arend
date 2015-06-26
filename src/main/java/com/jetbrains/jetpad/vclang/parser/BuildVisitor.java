package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private ClassDefinition myParent;
  private List<String> myContext = new ArrayList<>();
  private final Module myModule;
  private final ModuleLoader myModuleLoader;

  public BuildVisitor(Module module, ClassDefinition parent, ModuleLoader moduleLoader) {
    myParent = parent;
    myModule = module;
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

  private Definition getDefinition(String name, Concrete.Position position, Definition defaultResult) {
    Definition typedDef = myParent.findChild(name);
    if (typedDef == null) {
      myParent.add(defaultResult);
      return defaultResult;
    }

    if (!(typedDef instanceof ClassDefinition)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, position, name + " is already defined"));
      return null;
    } else {
      return typedDef;
    }
  }

  @Override
  public List<Concrete.Definition> visitDefs(DefsContext ctx) {
    List<Concrete.Definition> defs = new ArrayList<>(ctx.def().size());
    for (DefContext def : ctx.def()) {
      ModuleLoader.TypeCheckingUnit unit = visitDef(def);
      if (unit != null) {
        myModuleLoader.getTypeCheckingUnits().add(unit);
        defs.add((Concrete.Definition) unit.rawDefinition);
      }
    }
    return defs;
  }

  public ModuleLoader.TypeCheckingUnit visitDef(DefContext ctx) {
    myContext.clear();
    if (ctx instanceof DefFunctionContext) {
      return visitDefFunction((DefFunctionContext) ctx);
    }
    if (ctx instanceof DefDataContext) {
      return visitDefData((DefDataContext) ctx);
    }
    if (ctx instanceof DefClassContext) {
      String name = ((DefClassContext) ctx).ID().getText();
      ClassDefinition typedDef = (ClassDefinition) getDefinition(name, tokenPosition(ctx.getStart()), new ClassDefinition(name, myParent));
      if (typedDef == null) return null;

      ClassDefinition oldParent = myParent;
      myParent = typedDef;
      Concrete.ClassDefinition concreteDef = visitDefClass((DefClassContext) ctx);
      myParent = oldParent;
      return new ModuleLoader.TypeCheckingUnit(concreteDef, typedDef);
    }
    if (ctx instanceof DefCmdContext) {
      return visitDefCmd((DefCmdContext) ctx);
    }
    if (ctx instanceof DefOverrideContext) {
      return visitDefOverride((DefOverrideContext) ctx);
    }
    throw new IllegalStateException();
  }

  @Override
  public ModuleLoader.TypeCheckingUnit visitDefCmd(DefCmdContext ctx) {
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
          myParent.add(definition, export);
        }
      }
    } else {
      Collection<? extends Definition> children = module.getChildren();
      if (children == null) return null;
      for (Definition definition : children) {
        if (remove) {
          myParent.remove(definition);
        } else {
          myParent.add(definition, export);
        }
      }
    }
    return null;
  }

  @Override
  public ModuleLoader.TypeCheckingUnit visitDefFunction(DefFunctionContext ctx) {
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
      if (args == null) return null;

      if (args.get(0) instanceof Concrete.TelescopeArgument) {
        arguments.add((Concrete.TelescopeArgument) args.get(0));
      } else {
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(tele.getStart()), "Expected a typed variable"));
        return null;
      }
    }
    Concrete.Expression type = ctx.typeTermOpt() instanceof WithTypeContext ? visitExpr(((WithTypeContext) ctx.typeTermOpt()).expr()) : ctx.typeTermOpt() instanceof WithTypeAndTermContext ? visitExpr(((WithTypeAndTermContext) ctx.typeTermOpt()).expr(0)) : null;
    ArrowContext arrowCtx = ctx.typeTermOpt() instanceof WithTermContext ? ((WithTermContext) ctx.typeTermOpt()).arrow() : ctx.typeTermOpt() instanceof WithTypeAndTermContext ? ((WithTypeAndTermContext) ctx.typeTermOpt()).arrow() : null;
    Definition.Arrow arrow = arrowCtx instanceof ArrowLeftContext ? Abstract.Definition.Arrow.LEFT : arrowCtx instanceof ArrowRightContext ? Abstract.Definition.Arrow.RIGHT : null;
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(position, name, visitPrecedence(ctx.precedence()), isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, arguments, type, arrow, null);

    Definition typedDef = getDefinition(def.getName(), def.getPosition(), new FunctionDefinition(def.getName(), myParent, def.getPrecedence(), def.getFixity(), def.getArrow()));
    if (typedDef == null) return null;
    if (ctx.typeTermOpt() instanceof WithTermContext) {
      def.setTerm(visitExpr(((WithTermContext) ctx.typeTermOpt()).expr()));
    } else
    if (ctx.typeTermOpt() instanceof WithTypeAndTermContext) {
      def.setTerm(visitExpr(((WithTypeAndTermContext) ctx.typeTermOpt()).expr(1)));
    }
    return new ModuleLoader.TypeCheckingUnit(def, typedDef);
  }

  @Override
  public ModuleLoader.TypeCheckingUnit visitDefOverride(DefOverrideContext ctx) {
    // TODO
    return null;
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
  public ModuleLoader.TypeCheckingUnit visitDefData(DefDataContext ctx) {
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
    if (parameters == null) return null;

    Concrete.Expression type = ctx.expr() == null ? null : visitExpr(ctx.expr());
    if (type != null && !(type instanceof Concrete.UniverseExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.expr().getStart()), "Expected a universe"));
      return null;
    }

    List<Concrete.Constructor> constructors = new ArrayList<>();
    Definition.Fixity fixity = isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX;
    Universe universe = type == null ? null : ((Concrete.UniverseExpression) type).getUniverse();
    Concrete.DataDefinition def = new Concrete.DataDefinition(position, name, visitPrecedence(ctx.precedence()), fixity, universe, parameters, constructors);

    DataDefinition typedDef = (DataDefinition) getDefinition(def.getName(), def.getPosition(), new DataDefinition(def.getName(), myParent, def.getPrecedence(), def.getFixity(), new ArrayList<Constructor>()));
    if (typedDef == null) return null;

    for (int i = 0; i < ctx.constructor().size(); ++i) {
      isPrefix = ctx.constructor(i).name() instanceof NameIdContext;
      if (isPrefix) {
        name = ((NameIdContext) ctx.constructor(i).name()).ID().getText();
        position = tokenPosition(((NameIdContext) ctx.constructor(i).name()).ID().getSymbol());
      } else {
        name = ((NameBinOpContext) ctx.constructor(i).name()).BIN_OP().getText();
        position = tokenPosition(((NameBinOpContext) ctx.constructor(i).name()).BIN_OP().getSymbol());
      }

      int size = myContext.size();
      List<Concrete.TypeArgument> arguments = visitTeles(ctx.constructor(i).tele());
      trimToSize(myContext, size);
      if (arguments == null) continue;
      Concrete.Constructor concreteConstructor = new Concrete.Constructor(position, name, visitPrecedence(ctx.constructor(i).precedence()), isPrefix ? Definition.Fixity.PREFIX : Definition.Fixity.INFIX, new Universe.Type(), arguments, def);

      Constructor typedConstructor = (Constructor) getDefinition(concreteConstructor.getName(), concreteConstructor.getPosition(), new Constructor(i, concreteConstructor.getName(), typedDef, concreteConstructor.getPrecedence(), concreteConstructor.getFixity()));
      if (typedConstructor == null) continue;
      constructors.add(concreteConstructor);
      typedDef.getConstructors().add(typedConstructor);
    }

    return new ModuleLoader.TypeCheckingUnit(def, typedDef);
  }

  @Override
  public Concrete.ClassDefinition visitDefClass(DefClassContext ctx) {
    List<Concrete.Definition> fields = new ArrayList<>();
    for (DefContext def : ctx.defs().def()) {
      ModuleLoader.TypeCheckingUnit unit = visitDef(def);
      if (unit != null) {
        myModuleLoader.getTypeCheckingUnits().add(unit);
        fields.add((Concrete.Definition) unit.rawDefinition);
      }
    }
    return new Concrete.ClassDefinition(tokenPosition(ctx.getStart()), ctx.ID().getText(), new Universe.Type(), fields);
  }

  @Override
  public Concrete.InferHoleExpression visitUnknown(UnknownContext ctx) {
    return new Concrete.InferHoleExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    Concrete.Expression domain = visitExpr(ctx.expr(0));
    if (domain == null) return null;
    List<Concrete.TypeArgument> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeArgument(domain.getPosition(), true, domain));
    myContext.add("_");
    Concrete.Expression codomain = visitExpr(ctx.expr(1));
    if (codomain == null) return null;
    myContext.remove(myContext.size() - 1);
    return new Concrete.PiExpression(tokenPosition(ctx.getToken(ARROW, 0).getSymbol()), arguments, codomain);
  }

  @Override
  public Concrete.Expression visitTuple(TupleContext ctx) {
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
        myContext.add("_");
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
    List<Concrete.Argument> arguments = new ArrayList<>(tele.size());
    for (TeleContext arg : tele) {
      List<Concrete.Argument> arguments1 = visitLamTele(arg);
      if (arguments1 == null) return null;
      arguments.addAll(arguments1);
    }
    return arguments;
  }

  @Override
  public Concrete.Expression visitLam(LamContext ctx) {
    int size = myContext.size();
    List<Concrete.Argument> args = visitLamTeles(ctx.tele());
    if (args == null) return null;
    Concrete.Expression body = visitExpr(ctx.expr());
    if (body == null) return null;
    trimToSize(myContext, size);
    return new Concrete.LamExpression(tokenPosition(ctx.getStart()), args, body);
  }

  @Override
  public Concrete.Expression visitId(IdContext ctx) {
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
        if (myContext.get(i).equals(name)) {
          return new Concrete.IndexExpression(position, myContext.size() - 1 - i);
        }
      }
    }

    for (Definition definition = myParent; definition != null; definition = definition.getParent()) {
      if (!(definition instanceof ClassDefinition)) {
        continue;
      }

      List<Definition> children = ((ClassDefinition) definition).getFields(name);
      if (children != null && !children.isEmpty()) {
        if (children.size() > 1) {
          String msg = "ambiguous occurrence";
          for (Definition child1 : children) {
            msg += "\n\t" + child1.getName() + (child1.getParent() == definition ? " defined in " : " imported from ") + child1.getParent().getFullName();
          }
          myModuleLoader.getErrors().add(new ParserError(myModule, position, msg));
          return null;
        } else {
          return new Concrete.DefCallExpression(position, children.get(0));
        }
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
          Concrete.Expression expr = visitExpr(((TeleLiteralContext) tele).literal());
          if (expr == null) return null;
          arguments.add(new Concrete.TypeArgument(true, expr));
          myContext.add("_");
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
        myContext.add("_");
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
    Concrete.Expression result = new Concrete.DefCallExpression(pos, Prelude.ZERO);
    for (int i = 0; i < number; ++i) {
      result = new Concrete.AppExpression(pos, new Concrete.DefCallExpression(pos, Prelude.SUC), new Concrete.ArgumentExpression(result, true, false));
    }
    return result;
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    int size = myContext.size();
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    if (args == null) return null;
    trimToSize(myContext, size);

    for (Concrete.TypeArgument arg : args) {
      if (!arg.getExplicit()) {
        myModuleLoader.getErrors().add(new ParserError(myModule, arg.getPosition(), "Fields in sigma types must be explicit"));
      }
    }
    return new Concrete.SigmaExpression(tokenPosition(ctx.getStart()), args);
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    int size = myContext.size();
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    if (args == null) return null;
    Concrete.Expression codomain = visitExpr(ctx.expr());
    if (codomain == null) return null;
    trimToSize(myContext, size);
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
            myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(nameCtx.getStart()), name + " is not defined in " + definition.getFullName()));
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
    Concrete.Expression expr = visitExpr(ctx.atom());
    return expr == null ? null : visitFieldsAcc(expr, ctx.fieldAcc());
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

      if (def == null) {
        stack = new ArrayList<>(ctx.binOpLeft().size() - stack.size());
        continue;
      }

      pushOnStack(stack, new StackElem(expr, def), position);
    }

    Concrete.Expression expr = visitAtomFieldsAcc(ctx.atomFieldsAcc());
    expr = expr == null ? null : visitAtoms(expr, ctx.argument());
    return expr == null ? null : rollUpStack(stack, expr);
  }

  @Override
  public Definition visitInfixBinOp(InfixBinOpContext ctx) {
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
        if (arguments == null) return null;

        nameArguments = new ArrayList<>(arguments.size());
        for (Concrete.Argument argument : arguments) {
          if (argument instanceof Concrete.NameArgument) {
            nameArguments.add((Concrete.NameArgument) argument);
          } else {
            myModuleLoader.getErrors().add(new ParserError(myModule, argument.getPosition(), "Expected a variable"));
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
      if (expr == null) return null;
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
    boolean isPrefix = ctx.name() instanceof NameIdContext;
    return isPrefix ? ((NameIdContext) ctx.name()).ID().getText() : ((NameBinOpContext) ctx.name()).BIN_OP().getText();
  }

  private static Concrete.Position tokenPosition(Token token) {
    return new Concrete.Position(token.getLine(), token.getCharPositionInLine());
  }

  @Override
  public Concrete.ErrorExpression visitHole(HoleContext ctx) {
    return new Concrete.ErrorExpression(tokenPosition(ctx.getStart()));
  }
}
