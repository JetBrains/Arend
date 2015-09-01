package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NamespaceNameResolver;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.collectPatternNames;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private Namespace myNamespace;
  private Namespace myLocalNamespace;
  private Namespace myPrivateNamespace;
  private List<String> myContext = new ArrayList<>();
  private CompositeNameResolver myLocalNameResolver;
  private CompositeNameResolver myNameResolver;
  private final ErrorReporter myErrorReporter;

  public BuildVisitor(Namespace namespace, Namespace localNamespace, NameResolver nameResolver, ErrorReporter errorReporter) {
    myNamespace = namespace;
    myLocalNamespace = localNamespace;
    myPrivateNamespace = new Namespace(myNamespace.getName(), null);
    myNameResolver = new CompositeNameResolver();
    myNameResolver.pushNameResolver(nameResolver);
    myNameResolver.pushNameResolver(new NamespaceNameResolver(namespace));
    myNameResolver.pushNameResolver(new NamespaceNameResolver(myPrivateNamespace));
    myLocalNameResolver = new CompositeNameResolver();
    myLocalNameResolver.pushNameResolver(new NamespaceNameResolver(myLocalNamespace));
    myErrorReporter = errorReporter;
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
      myErrorReporter.report(new ParserError(myNamespace, tokenPosition(expr.getStart()), "Expected a list of variables"));
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

  private void visitDefList(List<DefContext> defs) {
    for (DefContext def : defs) {
      visitDef(def);
    }
  }

  @Override
  public Void visitDefs(DefsContext ctx) {
    if (ctx == null) return null;
    visitDefList(ctx.def());
    return null;
  }

  public List<Concrete.FunctionDefinition> visitDefsRaw(DefsContext ctx) {
    if (ctx == null) return null;
    List<Concrete.FunctionDefinition> definitions = new ArrayList<>(ctx.def().size());
    for (DefContext def : ctx.def()) {
      if (def instanceof DefOverrideContext) {
        DefOverrideContext defOverride = (DefOverrideContext) def;
        if (defOverride.where() != null) {
          myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.getStart()), "Where clause is not allowed in expression"));
          continue;
        }
        FunctionContext functionCtx = (FunctionContext) visit(defOverride.typeTermOpt());
        Concrete.FunctionDefinition definition = visitFunctionRawBegin(true, defOverride.name().size() == 1 ? null : defOverride.name(0), null, defOverride.name().size() == 1 ? defOverride.name(0) : defOverride.name(1), defOverride.tele(), functionCtx);
        if (definition != null) {
          if (functionCtx.termCtx != null) {
            definition.setTerm(visitExpr(functionCtx.termCtx));
          }
          visitFunctionRawEnd(definition);
          definitions.add(definition);
        }
      } else {
        myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.getStart()), "Expected an overridden function"));
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
      boolean isStatic = ((DefClassContext) ctx).staticMod() instanceof StaticStaticContext;
      if (!isStatic && myLocalNamespace == null) {
        myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.getStart()), "Non-static definition in a static context"));
        return null;
      }

      Namespace parentNamespace = isStatic ? myNamespace : myLocalNamespace;
      String name = ((DefClassContext) ctx).ID().getText();
      Namespace newNamespace = parentNamespace.getChild(new Utils.Name(name));
      if (parentNamespace.getDefinition(name) != null) {
        myErrorReporter.report(new GeneralError(myNamespace, "Name '" + name + "' is already defined"));
        return null;
      }
      ClassDefinition result = new ClassDefinition(newNamespace);

      Namespace oldNamespace = myNamespace;
      Namespace oldLocalNamespace = myLocalNamespace;
      Namespace oldPrivateNamespace = myPrivateNamespace;
      myNamespace = newNamespace;
      myLocalNamespace = result.getLocalNamespace();
      myPrivateNamespace = new Namespace(myPrivateNamespace.getName(), null);
      CompositeNameResolver oldLocalNameResolver = myLocalNameResolver;
      if (isStatic) {
        myLocalNameResolver = new CompositeNameResolver();
      }
      myLocalNameResolver.pushNameResolver(new NamespaceNameResolver(myLocalNamespace));
      myNameResolver.pushNameResolver(new NamespaceNameResolver(myNamespace));
      myNameResolver.pushNameResolver(new NamespaceNameResolver(myPrivateNamespace));
      visitDefClass((DefClassContext) ctx);
      myNamespace = oldNamespace;
      myLocalNamespace = oldLocalNamespace;
      myPrivateNamespace = oldPrivateNamespace;
      myNameResolver.popNameResolver();
      myNameResolver.popNameResolver();
      myLocalNameResolver.popNameResolver();
      myLocalNameResolver = oldLocalNameResolver;

      parentNamespace.addDefinition(result);
      return result;
    } else
    if (ctx instanceof DefCmdContext) {
      visitDefCmd((DefCmdContext) ctx);
      return null;
    } else
    if (ctx instanceof DefOverrideContext) {
      if (myLocalNamespace != null) {
        return visitDefOverride((DefOverrideContext) ctx);
      } else {
        myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.getStart()), "\\override is allowed only inside class definitions"));
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public Void visitDefCmd(DefCmdContext ctx) {
    if (ctx == null) return null;
    Namespace namespace = (Namespace) visitNamespaceMember(ctx.name(0), ctx.fieldAcc(), true, true);
    if (namespace == null) return null;
    boolean remove = ctx.nsCmd() instanceof CloseCmdContext;
    boolean export = ctx.nsCmd() instanceof ExportCmdContext;

    if (ctx.name().size() > 1) {
      for (int i = 1; i < ctx.name().size(); ++i) {
        String name = ctx.name(i) instanceof NameBinOpContext ? ((NameBinOpContext) ctx.name(i)).BIN_OP().getText() : ((NameIdContext) ctx.name(i)).ID().getText();
        Definition definition = namespace.getDefinition(name);
        Namespace child = namespace.findChild(name);
        if (definition == null && child == null) {
          myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.name(i).getStart()), name + " is not a static field of " + namespace.getFullName()));
        }
        if (definition != null) {
          processDefCmd(definition, export, remove);
        }
        if (child != null) {
          processDefCmd(child, export, remove);
        }
      }
    } else {
      for (Namespace child : namespace.getChildren()) {
        processDefCmd(child, export, remove);
      }

      for (Definition member : namespace.getDefinitions()) {
        processDefCmd(member, export, remove);
      }
    }
    return null;
  }

  private void processDefCmd(NamespaceMember member, boolean export, boolean remove) {
    if (export) {
      myNamespace.addMember(member);
    } else
    if (remove) {
      myPrivateNamespace.removeMember(member);
    } else {
      myPrivateNamespace.addMember(member);
    }
  }

  private static class FunctionContext {
    ExprContext typeCtx;
    ArrowContext arrowCtx;
    ExprContext termCtx;
  }

  @Override
  public FunctionContext visitWithType(WithTypeContext ctx) {
    FunctionContext result = new FunctionContext();
    result.typeCtx = ctx.expr();
    result.arrowCtx = null;
    result.termCtx = null;
    return result;
  }

  @Override
  public FunctionContext visitWithTypeAndTerm(WithTypeAndTermContext ctx) {
    FunctionContext result = new FunctionContext();
    result.typeCtx = ctx.expr(0);
    result.arrowCtx = ctx.arrow();
    result.termCtx = ctx.expr(1);
    return result;
  }

  @Override
  public FunctionContext visitWithTerm(WithTermContext ctx) {
    FunctionContext result = new FunctionContext();
    result.typeCtx = null;
    result.arrowCtx = ctx.arrow();
    result.termCtx = ctx.expr();
    return result;
  }

  @Override
  public FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    if (ctx == null) return null;
    return visitDefFunction(ctx.staticMod() instanceof StaticStaticContext, false, ctx.getStart(), null, ctx.precedence(), ctx.name(), ctx.tele(), (FunctionContext) visit(ctx.typeTermOpt()), ctx.where() == null ? null :  ctx.where().def());
  }

  @Override
  public FunctionDefinition visitDefOverride(DefOverrideContext ctx) {
    if (ctx == null) return null;
    return visitDefFunction(false, true, ctx.getStart(), ctx.name().size() == 1 ? null : ctx.name(0), null, ctx.name().size() == 1 ? ctx.name(0) : ctx.name(1), ctx.tele(), (FunctionContext) visit(ctx.typeTermOpt()), ctx.where() == null ? null : ctx.where().def());
  }

  private FunctionDefinition visitDefFunction(boolean isStatic, boolean overridden, Token token, NameContext originalName, PrecedenceContext precCtx, NameContext nameCtx, List<TeleContext> teleCtx, FunctionContext functionCtx, List<DefContext> defs) {
    if (isStatic && functionCtx.termCtx == null) {
      myErrorReporter.report(new ParserError(myNamespace, tokenPosition(token), "Non-static abstract definition"));
      return null;
    }
    if (!isStatic && myLocalNamespace == null) {
      myErrorReporter.report(new ParserError(myNamespace, tokenPosition(token), "Non-static definition in a static context"));
      return null;
    }

    Namespace parentNamespace = isStatic ? myNamespace : myLocalNamespace;
    Name name = getName(nameCtx);
    if (name == null) return null;

    // TODO: Do not create child namespace if the definition does not type check.
    FunctionDefinition typedDef;
    if (overridden) {
      typedDef = new OverriddenDefinition(parentNamespace.getChild(name), visitPrecedence(precCtx), null, null, visitArrow(functionCtx.arrowCtx), null, null);
    } else {
      typedDef = new FunctionDefinition(parentNamespace.getChild(name), visitPrecedence(precCtx), null, null, visitArrow(functionCtx.arrowCtx), null);
    }

    Namespace oldNamespace = myNamespace;
    Namespace oldLocalNamespace = myLocalNamespace;
    Namespace oldPrivateNamespace = myPrivateNamespace;
    myNamespace = parentNamespace.getChild(name);
    myLocalNamespace = isStatic ? null : typedDef.getNamespace();
    myPrivateNamespace = new Namespace(myPrivateNamespace.getName(), null);
    CompositeNameResolver oldLocalNameResolver = myLocalNameResolver;
    if (isStatic) {
      myLocalNameResolver = new CompositeNameResolver(new ArrayList<NameResolver>(0));
    }
    myNameResolver.pushNameResolver(new NamespaceNameResolver(myNamespace));
    myNameResolver.pushNameResolver(new NamespaceNameResolver(myPrivateNamespace));

    if (defs != null) {
      visitDefList(defs);
    }

    Concrete.FunctionDefinition def = visitFunctionRawBegin(overridden, originalName, precCtx, nameCtx, teleCtx, functionCtx);
    if (def == null) {
      myNamespace = oldNamespace;
      myLocalNamespace = oldLocalNamespace;
      myPrivateNamespace = oldPrivateNamespace;
      myNameResolver.popNameResolver();
      myNameResolver.popNameResolver();
      myLocalNameResolver = oldLocalNameResolver;
      return null;
    }

    List<Binding> localContext = new ArrayList<>();
    if (TypeChecking.typeCheckFunctionBegin(myErrorReporter, myNamespace, def, localContext, null, typedDef)) {
      Concrete.Expression term = functionCtx.termCtx == null ? null : visitExpr(functionCtx.termCtx);
      TypeChecking.typeCheckFunctionEnd(myErrorReporter, myNamespace, term, typedDef, localContext, null);
    } else {
      typedDef = null;
    }

    myNamespace = oldNamespace;
    myLocalNamespace = oldLocalNamespace;
    myPrivateNamespace = oldPrivateNamespace;
    myNameResolver.popNameResolver();
    myNameResolver.popNameResolver();
    myLocalNameResolver = oldLocalNameResolver;
    visitFunctionRawEnd(def);
    return typedDef;
  }

  private Concrete.FunctionDefinition visitFunctionRawBegin(boolean overridden, NameContext originalNameCtx, PrecedenceContext precCtx, NameContext nameCtx, List<TeleContext> teleCtx, FunctionContext functionCtx) {
    Name name = getName(nameCtx);
    if (name == null) {
      return null;
    }

    Name originalName = getName(originalNameCtx);
    int size = myContext.size();
    List<Concrete.Argument> arguments = visitFunctionArguments(teleCtx, overridden);
    if (arguments == null) {
      trimToSize(myContext, size);
      return null;
    }

    Concrete.Expression type = functionCtx.typeCtx == null ? null : visitExpr(functionCtx.typeCtx);
    Definition.Arrow arrow = visitArrow(functionCtx.arrowCtx);
    return new Concrete.FunctionDefinition(name.position, name, precCtx == null ? null : visitPrecedence(precCtx), arguments, type, arrow, null, overridden, originalName == null ? null : originalName);
  }

  private Abstract.Definition.Arrow visitArrow(ArrowContext arrowCtx) {
    return arrowCtx instanceof ArrowLeftContext ? Abstract.Definition.Arrow.LEFT : arrowCtx instanceof ArrowRightContext ? Abstract.Definition.Arrow.RIGHT : null;
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

  private List<Concrete.Argument> visitFunctionArguments(List<TeleContext> teleCtx) {
    return visitFunctionArguments(teleCtx, false);
  }

  private List<Concrete.Argument> visitFunctionArguments(List<TeleContext> teleCtx, boolean overridden) {
    List<Concrete.Argument> arguments = new ArrayList<>();
    for (TeleContext tele : teleCtx) {
      List<Concrete.Argument> args = visitLamTele(tele);
      if (args == null) {
        return null;
      }

      if (overridden || args.get(0) instanceof Concrete.TelescopeArgument) {
        arguments.add(args.get(0));
      } else {
        myErrorReporter.report(new ParserError(myNamespace, tokenPosition(tele.getStart()), "Expected a typed variable"));
        return null;
      }
    }
    return arguments;
  }

  private Abstract.Definition.Arrow getArrow(ArrowContext arrowCtx) {
    return arrowCtx instanceof ArrowLeftContext ? Abstract.Definition.Arrow.LEFT : arrowCtx instanceof ArrowRightContext ? Abstract.Definition.Arrow.RIGHT : null;
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
      myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.NUMBER().getSymbol()), "Precedence out of range: " + priority));

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
  public Concrete.NamePattern visitPatternAny(PatternAnyContext ctx) {
    return new Concrete.NamePattern(tokenPosition(ctx.getStart()), null);
  }

  @Override
  public Concrete.Pattern visitPatternID(PatternIDContext ctx) {
    String name = ctx.ID().getText();
    Concrete.Position pos = tokenPosition(ctx.getStart());
    Concrete.Expression constructorCall = findId(name, false, pos, false);
    if (constructorCall != null && constructorCall instanceof Concrete.DefCallExpression
        && ((Concrete.DefCallExpression) constructorCall).getDefinition() instanceof Constructor) {
      Constructor constructor = (Constructor) ((Concrete.DefCallExpression) constructorCall).getDefinition();
      boolean hasExplicit = false;
      for (TypeArgument arg : constructor.getArguments()) {
        if (arg.getExplicit())
          hasExplicit = true;
      }
      if (!hasExplicit)
        return new Concrete.ConstructorPattern(pos, new Name(name), new ArrayList<Concrete.Pattern>());
    }
    return new Concrete.NamePattern(pos, name);
  }

  private List<Concrete.Pattern> visitPatterns(List<PatternxContext> patternContexts) {
    List<Concrete.Pattern> patterns = new ArrayList<>();
    for (PatternxContext pattern : patternContexts) {
      Concrete.Pattern result = null;
      if (pattern instanceof PatternImplicitContext) {
        result = (Concrete.Pattern) visit(((PatternImplicitContext) pattern).pattern());
        result.setExplicit(false);
      } else if (pattern instanceof PatternExplicitContext){
        result = (Concrete.Pattern) visit(((PatternExplicitContext) pattern).pattern());
      }
      patterns.add(result);
    }
    return patterns;
  }

  private void applyPatternToContext(Abstract.Pattern pattern, int varIndex) {
    if (pattern instanceof Abstract.NamePattern && ((Abstract.NamePattern) pattern).getName() == null)
      return;
    List<String> names = new ArrayList<>();
    collectPatternNames(pattern, names);
    List<String> oldContext = new ArrayList<>(myContext);
    myContext.clear();
    for (int i = 0; i < varIndex; ++i) {
      myContext.add(oldContext.get(i));
    }
    myContext.addAll(names);
    for (int i = varIndex + 1; i < oldContext.size(); ++i) {
      myContext.add(oldContext.get(i));
    }
  }

  @Override
  public Concrete.ConstructorPattern visitPatternConstructor(PatternConstructorContext ctx) {
    return new Concrete.ConstructorPattern(tokenPosition(ctx.getStart()), getName(ctx.name()), visitPatterns(ctx.patternx()));
  }

  private void visitConstructorDef(ConstructorDefContext ctx, Concrete.DataDefinition def) {
    List<String> oldContext = new ArrayList<>(myContext);
    List<Concrete.Pattern> patterns = null;

    if (ctx instanceof WithPatternsContext) {
      WithPatternsContext wpCtx = (WithPatternsContext) ctx;
      Name dataDefName = getName(wpCtx.name());
      if (dataDefName == null) {
        return;
      }
      if (!def.getName().equals(dataDefName)) {
        myErrorReporter.report(new ParserError(myNamespace, dataDefName.position, "Expected a data type name: " + def.getName()));
        return;
      }

      patterns = visitPatterns(wpCtx.patternx());

      ProcessImplicitResult result = processImplicit(patterns, def.getParameters());
      if (result.patterns == null) {
        if (result.wrongImplicitPosition < patterns.size()) {
          myErrorReporter.report(new ParserError(myNamespace,
              tokenPosition(wpCtx.patternx(result.wrongImplicitPosition).start), "Unexpected implicit argument"));
        } else {
          myErrorReporter.report(new ParserError(myNamespace, tokenPosition(wpCtx.name().start), "Too few explicit arguments, expected: " + result.numExplicit));
        }
        return;
      }
      for (int i = 0; i < result.patterns.size(); i++) {
        applyPatternToContext(result.patterns.get(i), myContext.size() - result.patterns.size() + i);
      }
    }

    List<ConstructorContext> constructorCtxs = ctx instanceof WithPatternsContext ?
        ((WithPatternsContext) ctx).constructor() : Collections.singletonList(((NoPatternsContext) ctx).constructor());

    for (ConstructorContext conCtx : constructorCtxs) {
      Name conName = getName(conCtx.name());
      if (conName == null) {
        continue;
      }

      int size = myContext.size();
      List<Concrete.TypeArgument> arguments = visitTeles(conCtx.tele());
      trimToSize(myContext, size);

      if (arguments == null) {
        continue;
      }
      def.getConstructors().add(new Concrete.Constructor(conName.position, conName, visitPrecedence(conCtx.precedence()), new Universe.Type(), arguments, def, patterns));
    }

    myContext = oldContext;
 }

  @Override
  public DataDefinition visitDefData(DefDataContext ctx) {
    if (ctx == null) return null;
    Name name = getName(ctx.name());
    if (name == null) {
      return null;
    }
    int origSize = myContext.size();
    List<Concrete.TypeArgument> parameters = visitTeles(ctx.tele());
    if (parameters == null) {
      trimToSize(myContext, origSize);
      return null;
    }

    Concrete.Expression type = ctx.expr() == null ? null : visitExpr(ctx.expr());
    if (type != null && !(type instanceof Concrete.UniverseExpression)) {
      myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.expr().getStart()), "Expected a universe"));
      trimToSize(myContext, origSize);
      return null;
    }

    boolean isStatic = ctx.staticMod() instanceof StaticStaticContext;
    if (!isStatic && myLocalNamespace == null) {
      myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.getStart()), "Non-static definition in a static context"));
      trimToSize(myContext, origSize);
      return null;
    }

    Universe universe = type == null ? null : ((Concrete.UniverseExpression) type).getUniverse();
    Concrete.DataDefinition def = new Concrete.DataDefinition(name.position, name, visitPrecedence(ctx.precedence()), universe, parameters);
    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(myErrorReporter, myNamespace, !isStatic ? myLocalNamespace : null, def, localContext);
    if (typedDef == null) {
      return null;
    }

    for (ConstructorDefContext constructorDefCtx : ctx.constructorDef()) {
      visitConstructorDef(constructorDefCtx, def);
    }

    //TODO: indexing
    for (int i = 0; i < def.getConstructors().size(); i++) {
      TypeChecking.typeCheckConstructor(myErrorReporter, myNamespace, typedDef, def.getConstructors().get(i), localContext, i);
    }

    trimToSize(myContext, origSize);
    TypeChecking.typeCheckDataEnd(myErrorReporter, myNamespace, def, typedDef, null);
    return typedDef;
  }

  @Override
  public Void visitDefClass(DefClassContext ctx) {
    if (ctx == null || ctx.classFields() == null) return null;
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
        arguments.add(new Concrete.NameArgument(tokenPosition(literalContext.getStart()), true, null));
        myContext.add(null);
      } else {
        myErrorReporter.report(new ParserError(myNamespace, tokenPosition(literalContext.getStart()), "Unexpected token. Expected an identifier."));
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
        if (explicit) {
          arguments.addAll(vars);
        } else {
          for (Concrete.NameArgument var : vars) {
            arguments.add(new Concrete.NameArgument(var.getPosition(), false, var.getName()));
          }
        }
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
    return findId(name, binOp, position, true);
  }

  private Concrete.Expression findId(String name, boolean binOp, Concrete.Position position, boolean reportError) {
    Definition child = Prelude.PRELUDE.getDefinition(name);
    if (child != null) {
      return new Concrete.DefCallExpression(position, null, child);
    }

    if (!binOp && myContext.contains(name)) {
      return new Concrete.VarExpression(position, name);
    }

    NamespaceMember member = myLocalNameResolver.locateName(name);
    if (member == null) {
      member = myNameResolver.locateName(name);
    }
    if (member instanceof Definition) {
      return new Concrete.DefCallExpression(position, null, (Definition) member);
    } else
    if (reportError) {
      if (member != null) {
        myErrorReporter.report(new ParserError(myNamespace, position, "Cannot find '" + name + "'"));
      } else {
        myErrorReporter.report(new ParserError(myNamespace, position, "Not in scope: " + name));
      }
    }
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
    Concrete.Expression result = new Concrete.DefCallExpression(pos, null, Prelude.ZERO);
    for (int i = 0; i < number; ++i) {
      result = new Concrete.AppExpression(pos, new Concrete.DefCallExpression(pos, null, Prelude.SUC), new Concrete.ArgumentExpression(result, true, false));
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
        myErrorReporter.report(new ParserError(myNamespace, arg.getPosition(), "Fields in sigma types must be explicit"));
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
        Name name = getName(((ClassFieldContext) field).name());
        if (name == null) {
          return null;
        }

        if (expr instanceof Concrete.DefCallExpression && ((Concrete.DefCallExpression) expr).getDefinition() != null) {
          Definition definition = ((Concrete.DefCallExpression) expr).getDefinition();
          Definition classField = definition.getNamespace().getDefinition(name.name);
          if (classField == null) {
            NamespaceMember member = myNameResolver.getMember(definition.getNamespace(), name.name);
            if (member instanceof Definition) {
              classField = (Definition) member;
            }
          }
          if (classField == null && definition instanceof FunctionDefinition) {
            FunctionDefinition functionDefinition = (FunctionDefinition) definition;
            if (!functionDefinition.typeHasErrors() && functionDefinition.getArguments().isEmpty() && functionDefinition.getResultType() instanceof DefCallExpression && ((DefCallExpression) functionDefinition.getResultType()).getDefinition() instanceof ClassDefinition) {
              expr = new Concrete.DefCallNameExpression(expr.getPosition(), expr, name);
              continue;
            }
          }
          if (classField == null) {
            myErrorReporter.report(new ParserError(myNamespace, tokenPosition(((ClassFieldContext) field).name().getStart()), name.name + " is not a static field of " + definition.getNamespace().getFullName()));
            return null;
          }
          expr = new Concrete.DefCallExpression(expr.getPosition(), null, classField);
          continue;
        }
        expr = new Concrete.DefCallNameExpression(expr.getPosition(), expr, name);
      } else {
        expr = new Concrete.ProjExpression(expr.getPosition(), expr, Integer.valueOf(((SigmaFieldContext) field).NUMBER().getText()) - 1);
      }
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    if (ctx == null) return null;
    if (!ctx.fieldAcc().isEmpty() && ctx.atom() instanceof AtomLiteralContext && ((AtomLiteralContext) ctx.atom()).literal() instanceof IdContext) {
      Definition definition = (Definition) visitNamespaceMember(((IdContext) ((AtomLiteralContext) ctx.atom()).literal()).name(), ctx.fieldAcc(), false, false);
      if (definition != null) {
        return new Concrete.DefCallExpression(tokenPosition(ctx.getStart()), null, definition);
      }
    }

    Concrete.Expression expr = visitExpr(ctx.atom());
    if (expr == null) return null;

    expr = visitFieldsAcc(expr, ctx.fieldAcc());
    if (expr == null) return null;

    if (ctx.classFields() != null) {
      if (!(expr instanceof Concrete.DefCallExpression && ((Concrete.DefCallExpression) expr).getDefinition() instanceof ClassDefinition)) {
        myErrorReporter.report(new ParserError(myNamespace, expr.getPosition(), "Expected a class name"));
        return null;
      }

      ClassDefinition parent = (ClassDefinition) ((Concrete.DefCallExpression) expr).getDefinition();
      Namespace oldNamespace = myNamespace;
      Namespace oldLocalNamespace = myLocalNamespace;
      Namespace oldPrivateNamespace = myPrivateNamespace;
      myNamespace = parent.getNamespace();
      myLocalNamespace = parent.getLocalNamespace();
      myPrivateNamespace = new Namespace(myPrivateNamespace.getName(), null);
      List<Concrete.FunctionDefinition> definitions = visitDefsRaw(ctx.classFields().defs());
      myNamespace = oldNamespace;
      myLocalNamespace = oldLocalNamespace;
      myPrivateNamespace = oldPrivateNamespace;

      // TODO: Modify NameResolvers?

      expr = new Concrete.ClassExtExpression(tokenPosition(ctx.getStart()), parent, definitions);
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
      String msg = "Precedence parsing error: cannot mix (" + topElem.definition.getName().name + ") [" + prec + "] and (" + elem.definition.getName().name + ") [" + prec2 + "] in the same infix expression";
      myErrorReporter.report(new ParserError(myNamespace, position, msg));
    }
    stack.remove(stack.size() - 1);
    pushOnStack(stack, new StackElem(new Concrete.BinOpExpression(position, topElem.argument, topElem.definition, elem.argument), elem.definition), position);
  }

  private Concrete.Expression rollUpStack(List<StackElem> stack, Concrete.Expression expr) {
    for (int i = stack.size() - 1; i >= 0; --i) {
      expr = new Concrete.BinOpExpression(stack.get(i).argument.getPosition(), stack.get(i).argument, stack.get(i).definition, expr);
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
      myErrorReporter.report(new ParserError(myNamespace, position, "Infix notation cannot be used with local variables"));
      return null;
    }

    return ((Concrete.DefCallExpression) expr).getDefinition();
  }

  @Override
  public Definition visitInfixId(InfixIdContext ctx) {
    if (ctx == null) return null;
    return (Definition) visitNamespaceMember(ctx.name(), ctx.fieldAcc(), false, true);
  }

  private NamespaceMember visitNamespaceMember(NameContext nameCtx, List<FieldAccContext> fieldAccCtx, boolean isNamespace, boolean reportErrors) {
    boolean binOp = nameCtx instanceof NameBinOpContext;
    String name = binOp ? ((NameBinOpContext) nameCtx).BIN_OP().getText() : ((NameIdContext) nameCtx).ID().getText();
    if (isNamespace || !fieldAccCtx.isEmpty()) {
      NamespaceMember member = myNameResolver.locateName(name);
      if (member == null) {
        if (reportErrors) {
          myErrorReporter.report(new ParserError(myNamespace, tokenPosition(nameCtx.getStart()), "Not in scope: " + name));
        }
        return null;
      }

      Namespace namespace = member.getNamespace();
      for (int i = 0; i < fieldAccCtx.size() ; ++i) {
        if (!(fieldAccCtx.get(i) instanceof ClassFieldContext)) {
          if (reportErrors) {
            myErrorReporter.report(new ParserError(myNamespace, tokenPosition(nameCtx.getStart()), "Expected a global definition"));
          }
          return null;
        }

        Name name1 = getName(((ClassFieldContext) fieldAccCtx.get(i)).name());
        if (name1 == null) {
          return null;
        }

        if (i == fieldAccCtx.size() - 1 && !isNamespace) {
          Definition definition = namespace.getDefinition(name1.name);
          if (definition == null && reportErrors) {
            myErrorReporter.report(new ParserError(myNamespace, tokenPosition(nameCtx.getStart()), "Not in scope: " + namespace + "." + name1.name));
          }
          return definition;
        } else {
          NamespaceMember member1 = myNameResolver.getMember(namespace, name1.name);
          if (member1 != null) {
            namespace = member1.getNamespace();
          } else {
            if (reportErrors) {
              myErrorReporter.report(new ParserError(myNamespace, tokenPosition(nameCtx.getStart()), "Not in scope: " + namespace + "." + name1.name));
            }
            return null;
          }
        }
      }
      return namespace;
    } else {
      Concrete.Expression expr = findId(name, binOp, tokenPosition(nameCtx.getStart()));
      if (expr == null) return null;
      if (!(expr instanceof Concrete.DefCallExpression)) {
        if (reportErrors) {
          myErrorReporter.report(new ParserError(myNamespace, tokenPosition(nameCtx.getStart()), "Expected a global definition"));
        }
        return null;
      }
      return ((Concrete.DefCallExpression) expr).getDefinition();
    }
  }

  @Override
  public Concrete.Expression visitExprElim(ExprElimContext ctx) {
    if (ctx == null) return null;
    List<Concrete.Clause> clauses = new ArrayList<>(ctx.clause().size());

    Concrete.Expression elimExpr = visitExpr(ctx.expr());
    if (elimExpr == null) return null;

    int elimIndex = -1;
    if (ctx.elimCase() instanceof ElimContext) {
      if (!(elimExpr instanceof Concrete.VarExpression)) {
        myErrorReporter.report(new ParserError(myNamespace, elimExpr.getPosition(), "\\elim can be applied only to a local variable"));
        return null;
      }
      String name = ((Concrete.VarExpression) elimExpr).getName();
      elimIndex = myContext.lastIndexOf(name);
      if (elimIndex == -1) {
        myErrorReporter.report(new ParserError(myNamespace, elimExpr.getPosition(), "Not in scope: " + name));
        return null;
      }
    }

    List<String> oldContext = new ArrayList<>(myContext);
    boolean wasOtherwise = false;
    for (ClauseContext clauseCtx : ctx.clause()) {
      Concrete.Pattern pattern;
      if (clauseCtx.name() != null) {
        pattern = new Concrete.ConstructorPattern(tokenPosition(clauseCtx.name().start), getName(clauseCtx.name()), visitPatterns(clauseCtx.patternx()));
        for (Concrete.Pattern subPattern : ((Concrete.ConstructorPattern) pattern).getArguments()) {
          if (subPattern instanceof Concrete.ConstructorPattern) {
            myErrorReporter.report(new ParserError(myNamespace, subPattern.getPosition(), "Only simple constructor patterns are allowed under elim"));
            return null;
          }
        }
      } else {
        if (wasOtherwise) {
          myErrorReporter.report(new ParserError(myNamespace, tokenPosition(clauseCtx.start), "Multiple otherwise clauses"));
          continue;
        }
        wasOtherwise = true;
        pattern = new Concrete.NamePattern(tokenPosition(clauseCtx.start), null);
      }

      applyPatternToContext(pattern, ctx.elimCase() instanceof  ElimContext  ? elimIndex : myContext.size());
      Definition.Arrow arrow = clauseCtx.arrow() instanceof ArrowRightContext ? Definition.Arrow.RIGHT : Definition.Arrow.LEFT;

      Concrete.Expression expr = visitExpr(clauseCtx.expr());
      if (expr == null) {
        myContext = oldContext;
        return null;
      }

      clauses.add(new Concrete.Clause(tokenPosition(clauseCtx.getStart()), pattern, arrow, expr, null));
      myContext = new ArrayList<>(oldContext);
    }

    Concrete.ElimCaseExpression result = ctx.elimCase() instanceof ElimContext ?
            new Concrete.ElimExpression(tokenPosition(ctx.getStart()), elimExpr, clauses) :
            new Concrete.CaseExpression(tokenPosition(ctx.getStart()), elimExpr, clauses);

    for (Concrete.Clause clause : clauses) {
      clause.setElimExpression(result);
    }
    return result;
  }

  @Override
  public Concrete.LetClause visitLetClause(LetClauseContext ctx) {
    final int oldContextSize = myContext.size();
    final String name = ctx.ID().getText();

    final List<Concrete.Argument> arguments = visitFunctionArguments(ctx.tele());
    if (arguments == null) {
      trimToSize(myContext, oldContextSize);
      return null;
    }
    final Concrete.Expression resultType = ctx.typeAnnotation() == null ? null: visitExpr(ctx.typeAnnotation().expr());
    final Abstract.Definition.Arrow arrow = getArrow(ctx.arrow());
    final Concrete.Expression term = visitExpr(ctx.expr());

    trimToSize(myContext, oldContextSize);

    myContext.add(name);
    return new Concrete.LetClause(tokenPosition(ctx.getStart()), name, new ArrayList<>(arguments), resultType, arrow, term);
  }

  @Override
  public Concrete.LetExpression visitLet(LetContext ctx) {
    final int oldContextSize = myContext.size();
    final List<Concrete.LetClause> clauses = new ArrayList<>();
    for (LetClauseContext clauseCtx : ctx.letClause()) {
      Concrete.LetClause clause = visitLetClause(clauseCtx);
      if (clause == null)
        return null;
      clauses.add(visitLetClause(clauseCtx));
    }

    final Concrete.Expression expr = visitExpr(ctx.expr());
    trimToSize(myContext, oldContextSize);
    return new Concrete.LetExpression(tokenPosition(ctx.getStart()), clauses, expr);
  }

  private static Concrete.Position tokenPosition(Token token) {
    return new Concrete.Position(token.getLine(), token.getCharPositionInLine());
  }

  @Override
  public Concrete.ErrorExpression visitHole(HoleContext ctx) {
    if (ctx == null) return null;
    return new Concrete.ErrorExpression(tokenPosition(ctx.getStart()));
  }

  private static Name getName(NameContext ctx) {
    if (ctx == null) {
      return null;
    }

    Name result = new Name(null, null);
    result.fixity = ctx instanceof NameIdContext ? Abstract.Definition.Fixity.PREFIX : Abstract.Definition.Fixity.INFIX;
    if (result.fixity == Abstract.Definition.Fixity.PREFIX) {
      result.name = ((NameIdContext) ctx).ID().getText();
      result.position = tokenPosition(((NameIdContext) ctx).ID().getSymbol());
    } else {
      result.name = ((NameBinOpContext) ctx).BIN_OP().getText();
      result.position = tokenPosition(((NameBinOpContext) ctx).BIN_OP().getSymbol());
    }
    return result;
  }

  private static class Name extends Utils.Name {
    Concrete.Position position;

    public Name(String name, Abstract.Definition.Fixity fixity) {
      super(name, fixity);
    }

    public Name(String name) {
      super(name);
    }
  }
}
