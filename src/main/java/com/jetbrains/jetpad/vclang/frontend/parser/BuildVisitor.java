package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

import static com.jetbrains.jetpad.vclang.frontend.parser.VcgrammarParser.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private final SourceId myModule;
  private final ErrorReporter myErrorReporter;

  public BuildVisitor(SourceId module, ErrorReporter errorReporter) {
    myModule = module;
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
    if (!(expr instanceof BinOpContext && ((BinOpContext) expr).binOpLeft().isEmpty() && ((BinOpContext) expr).binOpArg() instanceof BinOpArgumentContext)) {
      return null;
    }
    Concrete.NameArgument firstArg = getVar(((BinOpArgumentContext)((BinOpContext) expr).binOpArg()).atomFieldsAcc());
    if (firstArg == null) {
      return null;
    }

    List<Concrete.NameArgument> result = new ArrayList<>();
    result.add(firstArg);
    for (ArgumentContext argument : ((BinOpArgumentContext)((BinOpContext) expr).binOpArg()).argument()) {
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
      myErrorReporter.report(new ParserError(tokenPosition(expr.getStart()), "Expected a list of variables"));
      throw new ParseException();
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
  public List<String> visitModulePath(ModulePathContext ctx) {
    List<String> path = new ArrayList<>(ctx.ID().size());
    for (TerminalNode id : ctx.ID()) {
      path.add(id.getText());
    }
    return path;
  }

  @Override
  public Concrete.ModuleCallExpression visitAtomModuleCall(AtomModuleCallContext ctx) {
    return new Concrete.ModuleCallExpression(tokenPosition(ctx.getStart()), visitModulePath(ctx.modulePath()));
  }

  private List<Concrete.Statement> visitStatementList(List<StatementContext> statementCtxs) {
    List<Concrete.Statement> statements = new ArrayList<>(statementCtxs.size());
    for (StatementContext statementCtx : statementCtxs) {
      try {
        Object statement = visit(statementCtx);
        if (statement != null) {
          statements.add((Concrete.Statement) statement);
        }
      } catch (ParseException ignored) {

      }
    }
    return statements;
  }

  @Override
  public List<Concrete.Statement> visitStatements(StatementsContext ctx) {
    return visitStatementList(ctx.statement());
  }

  public Concrete.Definition visitDefinition(DefinitionContext ctx) {
    return (Concrete.Definition) visit(ctx);
  }

  @Override
  public Concrete.DefineStatement visitStatDef(StatDefContext ctx) {
    Concrete.Definition definition = visitDefinition(ctx.definition());
    return new Concrete.DefineStatement(definition.getPosition(), definition);
  }

  @Override
  public Concrete.NamespaceCommandStatement visitStatCmd(StatCmdContext ctx) {
    Abstract.NamespaceCommandStatement.Kind kind = (Abstract.NamespaceCommandStatement.Kind) visit(ctx.nsCmd());
    List<String> modulePath = ctx.nsCmdRoot().modulePath() == null ? null : visitModulePath(ctx.nsCmdRoot().modulePath());
    List<String> path = new ArrayList<>();
    if (ctx.nsCmdRoot().name() != null) {
      path.add(visitName(ctx.nsCmdRoot().name()));
    }
    for (FieldAccContext fieldAccContext : ctx.fieldAcc()) {
      if (fieldAccContext instanceof ClassFieldContext) {
        path.add(visitName(((ClassFieldContext) fieldAccContext).name()));
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(fieldAccContext.getStart()), "Expected a name"));
      }
    }

    List<String> names;
    if (!ctx.name().isEmpty()) {
      names = new ArrayList<>(ctx.name().size());
      for (NameContext nameCtx : ctx.name()) {
        names.add(visitName(nameCtx));
      }
    } else {
      names = null;
    }
    return new Concrete.NamespaceCommandStatement(tokenPosition(ctx.getStart()), kind, modulePath, path, ctx.hidingOpt() instanceof WithHidingContext, names);
  }

  @Override
  public Abstract.NamespaceCommandStatement.Kind visitOpenCmd(OpenCmdContext ctx) {
    return Abstract.NamespaceCommandStatement.Kind.OPEN;
  }

  @Override
  public Abstract.NamespaceCommandStatement.Kind visitExportCmd(ExportCmdContext ctx) {
    return Abstract.NamespaceCommandStatement.Kind.EXPORT;
  }

  public Abstract.Definition.Arrow visitArrow(ArrowContext arrowCtx) {
    if (arrowCtx instanceof ArrowLeftContext) {
      return Abstract.Definition.Arrow.LEFT;
    }
    if (arrowCtx instanceof ArrowRightContext) {
      return Abstract.Definition.Arrow.RIGHT;
    }
    throw new IllegalStateException();
  }

  public Abstract.Precedence visitPrecedence(PrecedenceContext ctx) {
    return (Abstract.Precedence) visit(ctx);
  }

  @Override
  public Abstract.Precedence visitNoPrecedence(NoPrecedenceContext ctx) {
    return Abstract.Precedence.DEFAULT;
  }

  @Override
  public Abstract.Precedence visitWithPrecedence(WithPrecedenceContext ctx) {
    int priority = Integer.valueOf(ctx.NUMBER().getText());
    if (priority < 1 || priority > 9) {
      myErrorReporter.report(new ParserError(tokenPosition(ctx.NUMBER().getSymbol()), "Precedence out of range: " + priority));

      if (priority < 1) {
        priority = 1;
      } else {
        priority = 9;
      }
    }

    return new Abstract.Precedence((Abstract.Precedence.Associativity) visit(ctx.associativity()), (byte) priority);
  }

  @Override
  public Abstract.Precedence.Associativity visitNonAssoc(NonAssocContext ctx) {
    return Abstract.Precedence.Associativity.NON_ASSOC;
  }

  @Override
  public Abstract.Precedence.Associativity visitLeftAssoc(LeftAssocContext ctx) {
    return Abstract.Precedence.Associativity.LEFT_ASSOC;
  }

  @Override
  public Abstract.Precedence.Associativity visitRightAssoc(RightAssocContext ctx) {
    return Abstract.Precedence.Associativity.RIGHT_ASSOC;
  }

  @Override
  public Concrete.NamePattern visitAnyPatternAny(AnyPatternAnyContext ctx) {
    return new Concrete.NamePattern(tokenPosition(ctx.start), null);
  }

  @Override
  public Concrete.AnyConstructorPattern visitAnyPatternConstructor(AnyPatternConstructorContext ctx) {
    return new Concrete.AnyConstructorPattern(tokenPosition(ctx.start));
  }

  @Override
  public Concrete.Pattern visitPatternAny(PatternAnyContext ctx) {
    return (Concrete.Pattern) visit(ctx.anyPattern());
  }

  @Override
  public Concrete.Pattern visitPatternConstructor(PatternConstructorContext ctx) {
    if (ctx.name() instanceof NameIdContext && ctx.patternArg().size() == 0) {
      return new Concrete.NamePattern(tokenPosition(ctx.start), ((NameIdContext) ctx.name()).ID().getText());
    } else {
      return new Concrete.ConstructorPattern(tokenPosition(ctx.start), visitName(ctx.name()), visitPatternArgs(ctx.patternArg()));
    }
  }

  @Override
  public Concrete.PatternArgument visitPatternArgExplicit(PatternArgExplicitContext ctx) {
    return new Concrete.PatternArgument(tokenPosition(ctx.start), (Concrete.Pattern) visit(ctx.pattern()), true, false);
  }

  @Override
  public Concrete.PatternArgument visitPatternArgImplicit(PatternArgImplicitContext ctx) {
    return new Concrete.PatternArgument(tokenPosition(ctx.start), (Concrete.Pattern) visit(ctx.pattern()), false, false);
  }

  @Override
  public Concrete.PatternArgument visitPatternArgAny(PatternArgAnyContext ctx) {
    return new Concrete.PatternArgument(tokenPosition(ctx.start), (Concrete.Pattern) visit(ctx.anyPattern()), true, false);
  }

  @Override
  public Concrete.PatternArgument visitPatternArgID(PatternArgIDContext ctx) {
    return new Concrete.PatternArgument(tokenPosition(ctx.start), new Concrete.NamePattern(tokenPosition(ctx.start), ctx.ID().getText()), true, false);
  }

  private List<Concrete.PatternArgument> visitPatternArgs(List<PatternArgContext> patternArgContexts) {
    List<Concrete.PatternArgument> patterns = new ArrayList<>();
    for (PatternArgContext patternArgCtx : patternArgContexts) {
      patterns.add((Concrete.PatternArgument) visit(patternArgCtx));
    }
    return patterns;
  }

  @Override
  public Concrete.ClassField visitDefAbstract(DefAbstractContext ctx) {
    return new Concrete.ClassField(tokenPosition(ctx.getStart()), visitName(ctx.name()), visitPrecedence(ctx.precedence()), Collections.<Concrete.Argument>emptyList(), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.ClassView visitDefClassView(DefClassViewContext ctx) {
    List<Concrete.ClassViewField> fields = new ArrayList<>(ctx.classViewField().size());

    Concrete.Expression expr = visitExpr(ctx.expr());
    if (!(expr instanceof Concrete.DefCallExpression)) {
      myErrorReporter.report(new ParserError(expr.getPosition(), "Expected a class"));
      throw new ParseException();
    }

    Concrete.ClassView classView = new Concrete.ClassView(tokenPosition(ctx.getStart()), ctx.ID().getText(), (Concrete.DefCallExpression) expr, visitName(ctx.name()), fields);
    for (ClassViewFieldContext classViewFieldContext : ctx.classViewField()) {
      Concrete.ClassViewField field = visitClassViewField(classViewFieldContext, classView);
      if (field != null) {
        fields.add(field);
      }
    }

    return classView;
  }

  private Concrete.ClassViewField visitClassViewField(ClassViewFieldContext ctx, Concrete.ClassView classView) {
    String underlyingField = visitName(ctx.name(0));
    return new Concrete.ClassViewField(tokenPosition(ctx.name(0).getStart()), ctx.name().size() > 1 ? visitName(ctx.name(1)) : underlyingField, ctx.precedence() == null ? Abstract.Precedence.DEFAULT : visitPrecedence(ctx.precedence()), underlyingField, classView);
  }

  @Override
  public Concrete.ClassViewInstance visitDefInstance(DefInstanceContext ctx) {
    List<Concrete.Argument> arguments = visitFunctionArguments(ctx.tele());
    Concrete.Expression term = visitExpr(ctx.expr());
    return new Concrete.ClassViewInstance(tokenPosition(ctx.getStart()), ctx.defaultInst() instanceof WithDefaultContext, ctx.ID().getText(), Abstract.Precedence.DEFAULT, arguments, term);
  }

  @Override
  public List<Concrete.Statement> visitWhere(WhereContext ctx) {
    if (ctx == null) {
      return Collections.emptyList();
    }
    if (ctx.statements() != null) {
      return visitStatementList(ctx.statements().statement());
    }
    if (ctx.statement() != null) {
      return visitStatementList(Collections.singletonList(ctx.statement()));
    }
    return Collections.emptyList();
  }

  @Override
  public Concrete.FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    Concrete.Expression resultType = ctx.expr().size() == 2 ? visitExpr(ctx.expr(0)) : null;
    Concrete.Expression term = visitExpr(ctx.expr().size() == 2 ? ctx.expr(1) : ctx.expr(0));
    List<Concrete.Statement> statements = visitWhere(ctx.where());
    Concrete.FunctionDefinition result = new Concrete.FunctionDefinition(tokenPosition(ctx.getStart()), visitName(ctx.name()), visitPrecedence(ctx.precedence()), visitFunctionArguments(ctx.tele()), resultType, visitArrow(ctx.arrow()), term, statements);

    for (Iterator<Concrete.Statement> iterator = statements.iterator(); iterator.hasNext(); ) {
      Concrete.Statement statement = iterator.next();
      if (statement instanceof Concrete.DefineStatement) {
        Concrete.Definition definition = ((Concrete.DefineStatement) statement).getDefinition();
        if (definition instanceof Concrete.ClassField || definition instanceof Concrete.Implementation) {
          misplacedDefinitionError(definition.getPosition());
          iterator.remove();
        } else {
          definition.setParent(result);
        }
      }
    }

    return result;
  }

  private List<Concrete.Argument> visitFunctionArguments(List<TeleContext> teleCtx) {
    List<Concrete.Argument> arguments = new ArrayList<>();
    for (TeleContext tele : teleCtx) {
      List<Concrete.Argument> args = visitLamTele(tele);
      if (args != null) {
        if (args.get(0) instanceof Concrete.TelescopeArgument) {
          arguments.add(args.get(0));
        } else {
          myErrorReporter.report(new ParserError(tokenPosition(tele.getStart()), "Expected a typed variable"));
        }
      }
    }
    return arguments;
  }

  @Override
  public Concrete.DataDefinition visitDefData(DefDataContext ctx) {
    Concrete.Expression universe = ctx.expr() == null ? null : (Concrete.Expression) visit(ctx.expr());
    List<Concrete.Constructor> constructors = new ArrayList<>(ctx.constructorDef().size());
    List<Concrete.Condition> conditions = ctx.conditionDef() != null ? visitConditionDef(ctx.conditionDef()) : null;
    Concrete.DataDefinition dataDefinition = new Concrete.DataDefinition(tokenPosition(ctx.getStart()), visitName(ctx.name()), visitPrecedence(ctx.precedence()), visitTeles(ctx.tele()), ctx.isTruncated() instanceof TruncatedContext, universe, constructors, conditions);
    for (ConstructorDefContext constructorDefContext : ctx.constructorDef()) {
      try {
        visitConstructorDef(constructorDefContext, dataDefinition);
      } catch (ParseException ignored) {

      }
    }
    return dataDefinition;
  }

  @Override
  public List<Concrete.Condition> visitConditionDef(ConditionDefContext ctx) {
    List<Concrete.Condition> result = new ArrayList<>(ctx.condition().size());
    for (ConditionContext conditionCtx : ctx.condition()) {
      try {
        result.add(visitCondition(conditionCtx));
      } catch (ParseException ignored) {

      }
    }
    return result;
  }

  @Override
  public Concrete.Condition visitCondition(ConditionContext ctx) {
    return new Concrete.Condition(tokenPosition(ctx.start), visitName(ctx.name()), visitPatternArgs(ctx.patternArg()), visitExpr(ctx.expr()));
  }

  private void visitConstructorDef(ConstructorDefContext ctx, Concrete.DataDefinition def) {
    List<Concrete.PatternArgument> patterns = null;

    if (ctx instanceof WithPatternsContext) {
      WithPatternsContext wpCtx = (WithPatternsContext) ctx;
      String dataDefName = visitName(wpCtx.name());
      if (!def.getName().equals(dataDefName)) {
        myErrorReporter.report(new ParserError(tokenPosition(wpCtx.name().getStart()), "Expected a data type name: " + def.getName()));
        return;
      }

      patterns = visitPatternArgs(wpCtx.patternArg());
    }

    List<ConstructorContext> constructorCtxs = ctx instanceof WithPatternsContext ?
        ((WithPatternsContext) ctx).constructor() : Collections.singletonList(((NoPatternsContext) ctx).constructor());

    for (ConstructorContext conCtx : constructorCtxs) {
      def.getConstructors().add(new Concrete.Constructor(tokenPosition(conCtx.name().getStart()), visitName(conCtx.name()), visitPrecedence(conCtx.precedence()), visitTeles(conCtx.tele()), def, patterns));
    }
  }

  private void misplacedDefinitionError(Concrete.Position position) {
    myErrorReporter.report(new ParserError(position, "This definition is not allowed here"));
  }

  private List<Concrete.Definition> visitInstanceStatements(List<StatementContext> ctx, List<Concrete.ClassField> fields, List<Concrete.Implementation> implementations) {
    List<Concrete.Definition> definitions = new ArrayList<>(ctx.size());
    for (StatementContext statementCtx : ctx) {
      if (statementCtx == null) {
        continue;
      }

      try {
        Concrete.SourceNode sourceNode = (Concrete.SourceNode) visit(statementCtx);
        if (sourceNode != null) {
          if (sourceNode instanceof Concrete.DefineStatement) {
            Concrete.Definition definition = ((Concrete.DefineStatement) sourceNode).getDefinition();
            if (definition instanceof Concrete.ClassField) {
              if (fields != null) {
                fields.add((Concrete.ClassField) definition);
              } else {
                misplacedDefinitionError(definition.getPosition());
              }
            } else if (definition instanceof Concrete.Implementation) {
              if (implementations != null) {
                implementations.add((Concrete.Implementation) definition);
              } else {
                misplacedDefinitionError(definition.getPosition());
              }
            } else if (definition instanceof Concrete.FunctionDefinition || definition instanceof Concrete.DataDefinition || definition instanceof Concrete.ClassDefinition) {
              definitions.add(definition);
            } else {
              misplacedDefinitionError(definition.getPosition());
            }
          } else {
            misplacedDefinitionError(sourceNode.getPosition());
          }
        }
      } catch (ParseException ignored) {

      }
    }
    return definitions;
  }

  @Override
  public Concrete.ClassDefinition visitDefClass(DefClassContext ctx) {
    List<Concrete.TypeArgument> polyParameters = visitTeles(ctx.tele());
    List<Concrete.SuperClass> superClasses = new ArrayList<>(ctx.expr().size());
    List<Concrete.ClassField> fields = new ArrayList<>();
    List<Concrete.Implementation> implementations = new ArrayList<>();
    List<Concrete.Statement> globalStatements = visitWhere(ctx.where());
    List<Concrete.Definition> instanceDefinitions =
        ctx.statements() == null ?
        Collections.<Concrete.Definition>emptyList() :
        visitInstanceStatements(ctx.statements().statement(), fields, implementations);
    for (ExprContext exprCtx : ctx.expr()) {
      superClasses.add(new Concrete.SuperClass(tokenPosition(exprCtx.getStart()), visitExpr(exprCtx)));
    }

    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(tokenPosition(ctx.getStart()), ctx.ID().getText(), polyParameters, superClasses, fields, implementations, globalStatements, instanceDefinitions);
    for (Concrete.ClassField field : fields) {
      field.setParent(classDefinition);
    }
    for (Concrete.Implementation implementation : implementations) {
      implementation.setParent(classDefinition);
    }
    for (Concrete.Definition definition : instanceDefinitions) {
      definition.setParent(classDefinition);
      definition.setIsStatic(false);
    }
    for (Concrete.Statement statement : globalStatements) {
      if (statement instanceof Concrete.DefineStatement) {
        ((Concrete.DefineStatement) statement).getDefinition().setParent(classDefinition);
      }
    }
    return classDefinition;
  }

  @Override
  public Concrete.Implementation visitDefImplement(DefImplementContext ctx) {
    return new Concrete.Implementation(tokenPosition(ctx.getStart()), visitName(ctx.name()), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.InferHoleExpression visitUnknown(UnknownContext ctx) {
    return new Concrete.InferHoleExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.ErrorExpression visitHole(HoleContext ctx) {
    return new Concrete.ErrorExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    Concrete.Expression domain = visitExpr(ctx.expr(0));
    Concrete.Expression codomain = visitExpr(ctx.expr(1));
    List<Concrete.TypeArgument> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeArgument(domain.getPosition(), true, domain));
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
        TerminalNode id = ((NameIdContext) ((IdContext) literalContext).name()).ID();
        arguments.add(new Concrete.NameArgument(tokenPosition(id.getSymbol()), true, id.getText()));
      } else
      if (literalContext instanceof UnknownContext) {
        arguments.add(new Concrete.NameArgument(tokenPosition(literalContext.getStart()), true, null));
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(literalContext.getStart()), "Unexpected token. Expected an identifier."));
        throw new ParseException();
      }
    } else {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      ExprContext varsExpr;
      Concrete.Expression typeExpr;
      if (typedExpr instanceof TypedContext) {
        varsExpr = ((TypedContext) typedExpr).expr(0);
        typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
      } else {
        varsExpr = ((NotTypedContext) typedExpr).expr();
        typeExpr = null;
      }

      List<Concrete.NameArgument> vars = getVars(varsExpr);
      if (typeExpr == null) {
        if (explicit) {
          arguments.addAll(vars);
        } else {
          for (Concrete.NameArgument var : vars) {
            arguments.add(new Concrete.NameArgument(var.getPosition(), false, var.getName()));
          }
        }
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
    List<Concrete.Argument> arguments = new ArrayList<>();
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
  public Concrete.DefCallExpression visitId(IdContext ctx) {
    return new Concrete.DefCallExpression(tokenPosition(ctx.name().getStart()), null, visitName(ctx.name()));
  }

  private List<Concrete.Expression> visitMaxArgs(List<ExprContext> exprList) {
    if (exprList == null || exprList.size() == 0) {
      return null;
    }
    List<Concrete.Expression> pLevels = new ArrayList<>();
    for (ExprContext expr : exprList) {
      pLevels.add(visitExpr(expr));
    }
    return pLevels;
  }

  @Override
  public Concrete.Expression visitBinOpArgument(BinOpArgumentContext ctx) {
    Concrete.Expression expr = visitAtoms(visitAtomFieldsAcc(ctx.atomFieldsAcc()), ctx.argument());
    if (ctx.maybeNew() instanceof WithNewContext) {
      return new Concrete.NewExpression(tokenPosition(ctx.getStart()), expr);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitPolyUniverse(PolyUniverseContext ctx) {
    //int hLevel = ctx.TRUNCATED_UNIVERSE() != null ? Integer.parseInt(ctx.NUMBER().getText()) : Abstract.PolyUniverseExpression.NOT_TRUNCATED;
    int hLevel = Abstract.PolyUniverseExpression.NOT_TRUNCATED;
    if (ctx.TRUNCATED_UNIVERSE_PREFIX() != null) {
      String text = ctx.TRUNCATED_UNIVERSE_PREFIX().getText();
      int indexOfMinusSign = text.indexOf('-');
      hLevel = Integer.valueOf(text.substring(1, indexOfMinusSign));
    }
    return new Concrete.PolyUniverseExpression(tokenPosition(ctx.getStart()), ctx.atom() != null ? Collections.singletonList((Concrete.Expression)visit(ctx.atom())) : visitMaxArgs(ctx.expr()), hLevel);
  }

  @Override
  public Concrete.Expression visitPolySet(PolySetContext ctx) {
    return new Concrete.PolyUniverseExpression(tokenPosition(ctx.getStart()), ctx.atom() != null ? Collections.singletonList((Concrete.Expression)visit(ctx.atom())) : visitMaxArgs(ctx.expr()), Abstract.PolyUniverseExpression.SET);
  }

  @Override
  public Concrete.PolyUniverseExpression visitUniverse(UniverseContext ctx) {
    int plevel = Integer.valueOf(ctx.UNIVERSE().getText().substring("\\Type".length()));
    Concrete.Position pos = tokenPosition(ctx.UNIVERSE().getSymbol());
    return new Concrete.PolyUniverseExpression(pos, Collections.singletonList(new Concrete.NumericLiteral(pos, plevel)), Abstract.PolyUniverseExpression.NOT_TRUNCATED);
  }

  @Override
  public Concrete.PolyUniverseExpression visitTruncatedUniverse(TruncatedUniverseContext ctx) {
    String text = ctx.TRUNCATED_UNIVERSE().getText();
    int indexOfMinusSign = text.indexOf('-');
    int plevel = Integer.valueOf(text.substring(indexOfMinusSign + "-Type".length()));
    int hlevel = Integer.valueOf(text.substring(1, indexOfMinusSign));
    Concrete.Position pos = tokenPosition(ctx.TRUNCATED_UNIVERSE().getSymbol());
    return new Concrete.PolyUniverseExpression(pos, Collections.singletonList(new Concrete.NumericLiteral(pos, plevel)), hlevel);
  }

  @Override
  public Concrete.PolyUniverseExpression visitSet(SetContext ctx) {
    int plevel = Integer.valueOf(ctx.SET().getText().substring("\\Set".length()));
    Concrete.Position pos = tokenPosition(ctx.SET().getSymbol());
    return new Concrete.PolyUniverseExpression(pos, Collections.singletonList(new Concrete.NumericLiteral(pos, plevel)), Abstract.PolyUniverseExpression.SET);
  }

  /*
  @Override
  public Concrete.PolyUniverseExpression visitUniversePref(UniversePrefContext ctx) {
    return new Concrete.PolyUniverseExpression(tokenPosition(ctx.UNIVERSE_PREFIX().getSymbol()), null, Abstract.PolyUniverseExpression.NOT_TRUNCATED);
  }

  @Override
  public Concrete.PolyUniverseExpression visitTruncatedUniversePref(TruncatedUniversePrefContext ctx) {
    String text = ctx.TRUNCATED_UNIVERSE_PREFIX().getText();
    int indexOfMinusSign = text.indexOf('-');
    int hlevel = Integer.valueOf(text.substring(1, indexOfMinusSign));
    return new Concrete.PolyUniverseExpression(tokenPosition(ctx.TRUNCATED_UNIVERSE_PREFIX().getSymbol()), null, hlevel);
  }

  @Override
  public Concrete.PolyUniverseExpression visitSetPref(SetPrefContext ctx) {
    return new Concrete.PolyUniverseExpression(tokenPosition(ctx.SET_PREFIX().getSymbol()), null, Abstract.PolyUniverseExpression.SET);
  }
  /**/

  @Override
  public Concrete.LvlExpression visitLvl(LvlContext ctx) {
    return new Concrete.LvlExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.PolyUniverseExpression visitProp(PropContext ctx) {
    Concrete.Position pos = tokenPosition(ctx.getStart());
    return new Concrete.PolyUniverseExpression(pos, Collections.singletonList(new Concrete.NumericLiteral(pos, 0)), Abstract.PolyUniverseExpression.PROP);
  }

  private List<Concrete.TypeArgument> visitTeles(List<TeleContext> teles) {
    List<Concrete.TypeArgument> arguments = new ArrayList<>(teles.size());
    for (TeleContext tele : teles) {
      boolean explicit = !(tele instanceof ImplicitContext);
      TypedExprContext typedExpr;
      if (explicit) {
        if (tele instanceof ExplicitContext) {
          typedExpr = ((ExplicitContext) tele).typedExpr();
        } else
        if (tele instanceof TeleLiteralContext) {
          arguments.add(new Concrete.TypeArgument(true, visitExpr(((TeleLiteralContext) tele).literal())));
          continue;
        } else {
          throw new IllegalStateException();
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        List<Concrete.NameArgument> args = getVars(((TypedContext) typedExpr).expr(0));
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
  public Concrete.NumericLiteral visitAtomNumber(AtomNumberContext ctx) {
    return new Concrete.NumericLiteral(tokenPosition(ctx.NUMBER().getSymbol()), Integer.valueOf(ctx.NUMBER().getText()));
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    for (Concrete.TypeArgument arg : args) {
      if (!arg.getExplicit()) {
        myErrorReporter.report(new ParserError(arg.getPosition(), "Fields in sigma types must be explicit"));
      }
    }
    return new Concrete.SigmaExpression(tokenPosition(ctx.getStart()), args);
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    return new Concrete.PiExpression(tokenPosition(ctx.getStart()), visitTeles(ctx.tele()), visitExpr(ctx.expr()));
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
    Concrete.Expression left = null;
    Concrete.DefCallExpression binOp = null;
    List<Abstract.BinOpSequenceElem> sequence = new ArrayList<>(ctx.binOpLeft().size());

    for (BinOpLeftContext leftContext : ctx.binOpLeft()) {
      String name = (String) visit(leftContext.infix());
      Concrete.Expression expr = (Concrete.Expression)visit(leftContext.binOpArg());

      if (left == null) {
        left = expr;
      } else {
        sequence.add(new Abstract.BinOpSequenceElem(binOp, expr));
      }
      binOp = new Concrete.DefCallExpression(tokenPosition(leftContext.infix().getStart()), null, name);
    }

    Concrete.Expression expr = (Concrete.Expression)visit(ctx.binOpArg());

    if (left == null) {
      return expr;
    }

    sequence.add(new Abstract.BinOpSequenceElem(binOp, expr));
    return new Concrete.BinOpSequenceExpression(tokenPosition(ctx.getStart()), left, sequence);
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    Concrete.Expression expression = visitExpr(ctx.atom());
    for (FieldAccContext fieldAccContext : ctx.fieldAcc()) {
      if (fieldAccContext instanceof ClassFieldContext) {
        expression = new Concrete.DefCallExpression(tokenPosition(fieldAccContext.getStart()), expression, visitName(((ClassFieldContext) fieldAccContext).name()));
      } else
      if (fieldAccContext instanceof SigmaFieldContext) {
        expression = new Concrete.ProjExpression(tokenPosition(fieldAccContext.getStart()), expression, Integer.valueOf(((SigmaFieldContext) fieldAccContext).NUMBER().getText()) - 1);
      } else {
        throw new IllegalStateException();
      }
    }

    if (ctx.implementStatements() != null) {
      List<Concrete.ClassFieldImpl> implementStatements = new ArrayList<>(ctx.implementStatements().implementStatement().size());
      for (ImplementStatementContext implementStatement : ctx.implementStatements().implementStatement()) {
        implementStatements.add(new Concrete.ClassFieldImpl(tokenPosition(implementStatement.name().getStart()), visitName(implementStatement.name()), visitExpr(implementStatement.expr())));
      }
      expression = new Concrete.ClassExtExpression(tokenPosition(ctx.getStart()), expression, implementStatements);
    }
    return expression;
  }

  @Override
  public String visitInfixBinOp(InfixBinOpContext ctx) {
    return ctx.BIN_OP().getText();
  }

  @Override
  public String visitInfixId(InfixIdContext ctx) {
    return ctx.ID().getText();
  }

  private String visitName(NameContext ctx) {
    return (String) visit(ctx);
  }

  @Override
  public String visitNameId(NameIdContext ctx) {
    return ctx.ID().getText();
  }

  @Override
  public String visitNameBinOp(NameBinOpContext ctx) {
    return ctx.BIN_OP().getText();
  }

  @Override
  public Concrete.Expression visitExprElim(ExprElimContext ctx) {
    List<Concrete.Clause> clauses = new ArrayList<>(ctx.clause().size());
    List<Concrete.Expression> elimExprs = new ArrayList<>();
    for (ExprContext exprCtx : ctx.expr()) {
      elimExprs.add(visitExpr(exprCtx));
    }

    if (ctx.elimCase() instanceof ElimContext) {
      for (Concrete.Expression elimExpr : elimExprs) {
        if (!(elimExpr instanceof Concrete.DefCallExpression && ((Concrete.DefCallExpression) elimExpr).getExpression() == null)) {
          myErrorReporter.report(new ParserError(elimExpr.getPosition(), "\\elim can be applied only to a local variable"));
          return null;
        }
      }
    }

    for (ClauseContext clauseCtx : ctx.clause()) {
      List<Concrete.Pattern> patterns = new ArrayList<>();
      for (PatternContext patternCtx : clauseCtx.pattern()) {
        patterns.add((Concrete.Pattern) visit(patternCtx));
      }

      if (patterns.size() != ctx.expr().size()) {
        myErrorReporter.report(new ParserError(tokenPosition(clauseCtx.getStart()), "Expected: " + ctx.expr().size() + " patterns, got: " + patterns.size()));
        return null;
      }

      if (clauseCtx.arrow() == null) {
        clauses.add(new Concrete.Clause(tokenPosition(clauseCtx.start), patterns, null, null));
      } else {
        Abstract.Definition.Arrow arrow = clauseCtx.arrow() instanceof ArrowRightContext ? Abstract.Definition.Arrow.RIGHT : Abstract.Definition.Arrow.LEFT;
        clauses.add(new Concrete.Clause(tokenPosition(clauseCtx.getStart()), patterns, arrow, visitExpr(clauseCtx.expr())));
      }
    }

    return ctx.elimCase() instanceof CaseContext ?
      new Concrete.CaseExpression(tokenPosition(ctx.getStart()), elimExprs, clauses) :
      new Concrete.ElimExpression(tokenPosition(ctx.getStart()), elimExprs, clauses);
  }

  @Override
  public Concrete.LetClause visitLetClause(LetClauseContext ctx) {
    List<Concrete.Argument> arguments = visitFunctionArguments(ctx.tele());
    Concrete.Expression resultType = ctx.typeAnnotation() == null ? null : visitExpr(ctx.typeAnnotation().expr());
    return new Concrete.LetClause(tokenPosition(ctx.getStart()), ctx.ID().getText(), arguments, resultType, visitArrow(ctx.arrow()), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.LetExpression visitLet(LetContext ctx) {
    List<Concrete.LetClause> clauses = new ArrayList<>();
    for (LetClauseContext clauseCtx : ctx.letClause()) {
      clauses.add(visitLetClause(clauseCtx));
    }

    return new Concrete.LetExpression(tokenPosition(ctx.getStart()), clauses, visitExpr(ctx.expr()));
  }

  private Concrete.Position tokenPosition(Token token) {
    return new Concrete.Position(myModule, token.getLine(), token.getCharPositionInLine());
  }
}
