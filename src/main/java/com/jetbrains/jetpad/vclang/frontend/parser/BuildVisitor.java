package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
      Concrete.Position position = tokenPosition(literal.getStart());
      return new Concrete.NameArgument(position, true, new Concrete.LocalVariable(position, "_"));
    }
    if (literal instanceof IdContext && ((IdContext) literal).name() instanceof NameIdContext) {
      Concrete.Position position = tokenPosition(literal.getStart());
      return new Concrete.NameArgument(position, true, new Concrete.LocalVariable(position, ((NameIdContext) ((IdContext) literal).name()).ID().getText()));
    }
    return null;
  }

  private List<Concrete.NameArgument> getVarsNull(ExprContext expr) {
    if (!(expr instanceof BinOpContext && ((BinOpContext) expr).binOpLeft().isEmpty() && ((BinOpContext) expr).binOpArg() instanceof BinOpArgumentContext && ((BinOpContext) expr).maybeNew() instanceof NoNewContext && ((BinOpContext) expr).implementStatements() == null)) {
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
      } else
      if (argument instanceof ArgumentImplicitContext) {
        List<Concrete.NameArgument> arguments = getVarsNull(((ArgumentImplicitContext) argument).expr());
        if (arguments == null) {
          return null;
        }
        for (Concrete.NameArgument arg : arguments) {
          arg.setExplicit(false);
          result.add(arg);
        }
      } else {
        return null;
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

  private Concrete.Expression visitExpr(AtomContext expr) {
    return (Concrete.Expression) visit(expr);
  }

  private Concrete.Expression visitExpr(LiteralContext expr) {
    return (Concrete.Expression) visit(expr);
  }

  private Concrete.UniverseExpression visitExpr(UniverseAtomContext expr) {
    return (Concrete.UniverseExpression) visit(expr);
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
    Concrete.NamespaceCommandStatement.Kind kind = (Concrete.NamespaceCommandStatement.Kind) visit(ctx.nsCmd());
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
  public Concrete.NamespaceCommandStatement.Kind visitOpenCmd(OpenCmdContext ctx) {
    return Concrete.NamespaceCommandStatement.Kind.OPEN;
  }

  @Override
  public Concrete.NamespaceCommandStatement.Kind visitExportCmd(ExportCmdContext ctx) {
    return Concrete.NamespaceCommandStatement.Kind.EXPORT;
  }

  private Abstract.Precedence visitPrecedence(PrecedenceContext ctx) {
    return (Abstract.Precedence) visit(ctx);
  }

  @Override
  public Abstract.Precedence visitNoPrecedence(NoPrecedenceContext ctx) {
    return Abstract.Precedence.DEFAULT;
  }

  @Override
  public Abstract.Precedence visitWithPrecedence(WithPrecedenceContext ctx) {
    int priority = Integer.parseInt(ctx.NUMBER().getText());
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

  private Concrete.Pattern visitPattern(PatternContext ctx) {
    return (Concrete.Pattern) visit(ctx);
  }

  @Override
  public Concrete.Pattern visitPatternAtom(PatternAtomContext ctx) {
    return (Concrete.Pattern) visit(ctx.atomPattern());
  }

  @Override
  public Concrete.Pattern visitPatternConstructor(PatternConstructorContext ctx) {
    if (ctx.name() instanceof NameIdContext && ctx.atomPatternOrID().size() == 0) {
      return new Concrete.NamePattern(tokenPosition(ctx.start), new Concrete.LocalVariable(tokenPosition(((NameIdContext) ctx.name()).ID().getSymbol()), ((NameIdContext) ctx.name()).ID().getText()));
    } else {
      List<Concrete.Pattern> patterns = new ArrayList<>(ctx.atomPatternOrID().size());
      for (AtomPatternOrIDContext atomCtx : ctx.atomPatternOrID()) {
        patterns.add(visitAtomPattern(atomCtx));
      }
      return new Concrete.ConstructorPattern(tokenPosition(ctx.start), visitName(ctx.name()), patterns);
    }
  }

  private Concrete.Pattern visitAtomPattern(AtomPatternOrIDContext ctx) {
    return (Concrete.Pattern) visit(ctx);
  }

  @Override
  public Concrete.Pattern visitPatternExplicit(PatternExplicitContext ctx) {
    return visitPattern(ctx.pattern());
  }

  @Override
  public Concrete.Pattern visitPatternImplicit(PatternImplicitContext ctx) {
    Concrete.Pattern pattern = visitPattern(ctx.pattern());
    if (pattern != null) {
      pattern.setExplicit(false);
    }
    return pattern;
  }

  @Override
  public Concrete.Pattern visitPatternEmpty(PatternEmptyContext ctx) {
    return new Concrete.EmptyPattern(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.Pattern visitPatternOrIDAtom(PatternOrIDAtomContext ctx) {
    return (Concrete.Pattern) visit(ctx.atomPattern());
  }

  @Override
  public Concrete.Pattern visitPatternID(PatternIDContext ctx) {
    Concrete.Position position = tokenPosition(ctx.getStart());
    return new Concrete.NamePattern(position, new Concrete.LocalVariable(position, ctx.ID().getText()));
  }

  @Override
  public Concrete.Pattern visitPatternAny(PatternAnyContext ctx) {
    Concrete.Position position = tokenPosition(ctx.getStart());
    return new Concrete.NamePattern(position, new Concrete.LocalVariable(position, "_"));
  }

  @Override
  public Concrete.ClassField visitDefAbstract(DefAbstractContext ctx) {
    return new Concrete.ClassField(tokenPosition(ctx.getStart()), visitName(ctx.name()), visitPrecedence(ctx.precedence()), Collections.emptyList(), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.ClassView visitDefClassView(DefClassViewContext ctx) {
    List<Concrete.ClassViewField> fields = new ArrayList<>(ctx.classViewField().size());

    Concrete.Expression expr = visitExpr(ctx.expr());
    if (!(expr instanceof Concrete.ReferenceExpression)) {
      myErrorReporter.report(new ParserError(expr.getPosition(), "Expected a class"));
      throw new ParseException();
    }

    Concrete.ClassView classView = new Concrete.ClassView(tokenPosition(ctx.getStart()), ctx.ID().getText(), (Concrete.ReferenceExpression) expr, visitName(ctx.name()), fields);
    for (ClassViewFieldContext classViewFieldContext : ctx.classViewField()) {
      fields.add(visitClassViewField(classViewFieldContext, classView));
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
    if (term instanceof Concrete.NewExpression) {
      Concrete.Expression type = ((Concrete.NewExpression) term).getExpression();
      if (type instanceof Concrete.ClassExtExpression) {
        Concrete.ClassExtExpression classExt = (Concrete.ClassExtExpression) type;
        if (classExt.getBaseClassExpression() instanceof Concrete.ReferenceExpression) {
          return new Concrete.ClassViewInstance(tokenPosition(ctx.getStart()), ctx.defaultInst() instanceof WithDefaultContext, ctx.ID().getText(), Abstract.Precedence.DEFAULT, arguments, (Concrete.ReferenceExpression) classExt.getBaseClassExpression(), classExt.getStatements());
        }
      }
    }

    myErrorReporter.report(new ParserError(tokenPosition(ctx.expr().getStart()), "Expected a class view extension"));
    throw new ParseException();
  }

  @Override
  public List<Concrete.Statement> visitWhere(WhereContext ctx) {
    return ctx == null || ctx.statement().isEmpty() ? Collections.emptyList() : visitStatementList(ctx.statement());
  }

  @Override
  public List<Concrete.ReferenceExpression> visitElim(ElimContext ctx) {
    if (ctx != null && ctx.expr0() != null && !ctx.expr0().isEmpty()) {
      List<Concrete.Expression> expressions = new ArrayList<>(ctx.expr0().size());
      for (Expr0Context exprCtx : ctx.expr0()) {
        expressions.add(visitExpr(exprCtx));
      }
      return checkElimExpressions(expressions);
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public Concrete.FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    Concrete.Expression resultType = ctx.expr() != null ? visitExpr(ctx.expr()) : null;
    Concrete.FunctionBody body;
    if (ctx.functionBody() instanceof WithElimContext) {
      WithElimContext elimCtx = ((WithElimContext) ctx.functionBody());
      body = new Concrete.ElimFunctionBody(tokenPosition(elimCtx.start), visitElim(elimCtx.elim()), visitClauses(elimCtx.clauses()));
    } else {
      body = new Concrete.TermFunctionBody(tokenPosition(ctx.start), visitExpr(((WithoutElimContext) ctx.functionBody()).expr()));
    }
    List<Concrete.Statement> statements = visitWhere(ctx.where());
    Concrete.FunctionDefinition result = new Concrete.FunctionDefinition(tokenPosition(ctx.getStart()), visitName(ctx.name()), visitPrecedence(ctx.precedence()), visitFunctionArguments(ctx.tele()), resultType, body, statements);

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
    List<Concrete.ReferenceExpression> eliminatedReferences = ctx.dataBody() instanceof DataClausesContext ? visitElim(((DataClausesContext) ctx.dataBody()).elim()) : null;
    Concrete.DataDefinition dataDefinition = new Concrete.DataDefinition(tokenPosition(ctx.getStart()), visitName(ctx.name()), visitPrecedence(ctx.precedence()), visitTeles(ctx.tele()), eliminatedReferences, ctx.isTruncated() instanceof TruncatedContext, universe, new ArrayList<>());
    visitDataBody(ctx.dataBody(), dataDefinition);
    return dataDefinition;
  }

  private void visitDataBody(DataBodyContext ctx, Concrete.DataDefinition def) {
    if (ctx instanceof DataClausesContext) {
      for (ConstructorClauseContext clauseCtx : ((DataClausesContext) ctx).constructorClause()) {
        try {
          List<Concrete.Pattern> patterns = new ArrayList<>(clauseCtx.pattern().size());
          for (PatternContext patternCtx : clauseCtx.pattern()) {
            patterns.add(visitPattern(patternCtx));
          }
          def.getConstructorClauses().add(new Concrete.ConstructorClause(tokenPosition(clauseCtx.start), patterns, visitConstructors(clauseCtx.constructor(), def)));
        } catch (ParseException ignored) {

        }
      }
    } else if (ctx instanceof DataConstructorsContext) {
      def.getConstructorClauses().add(new Concrete.ConstructorClause(tokenPosition(ctx.start), null, visitConstructors(((DataConstructorsContext) ctx).constructor(), def)));
    }
  }

  private List<Concrete.Constructor> visitConstructors(List<ConstructorContext> conContexts, Concrete.DataDefinition def) {
    List<Concrete.Constructor> result = new ArrayList<>(conContexts.size());
    for (ConstructorContext conCtx : conContexts) {
      boolean hasConditions = conCtx.elim() != null || !conCtx.clause().isEmpty();
      try {
        List<Concrete.FunctionClause> clauses;
        if (hasConditions) {
          clauses = new ArrayList<>(conCtx.clause().size());
          for (ClauseContext clauseCtx : conCtx.clause()) {
            clauses.add(visitClause(clauseCtx));
          }
        } else {
          clauses = null;
        }

        result.add(new Concrete.Constructor(
          tokenPosition(conCtx.start),
          visitName(conCtx.name()),
          visitPrecedence(conCtx.precedence()),
          def,
          visitTeles(conCtx.tele()),
          hasConditions ? visitElim(conCtx.elim()) : null,
          clauses));
      } catch (ParseException ignored) {

      }
    }
    return result;
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
    List<Concrete.SuperClass> superClasses = new ArrayList<>(ctx.expr0().size());
    List<Concrete.ClassField> fields = new ArrayList<>();
    List<Concrete.Implementation> implementations = new ArrayList<>();
    List<Concrete.Statement> globalStatements = visitWhere(ctx.where());
    List<Concrete.Definition> instanceDefinitions =
        ctx.statement().isEmpty() ?
        Collections.emptyList() :
        visitInstanceStatements(ctx.statement(), fields, implementations);
    for (Expr0Context exprCtx : ctx.expr0()) {
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
    if (tele instanceof ExplicitContext || tele instanceof ImplicitContext) {
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
            arguments.add(new Concrete.NameArgument(var.getPosition(), false, var.getReferable()));
          }
        }
      } else {
        List<Abstract.ReferableSourceNode> args = new ArrayList<>(vars.size());
        for (Concrete.NameArgument var : vars) {
          args.add(var.getReferable());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, args, typeExpr));
      }
    } else {
      boolean ok = tele instanceof TeleLiteralContext;
      if (ok) {
        LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
        if (literalContext instanceof IdContext && ((IdContext) literalContext).name() instanceof NameIdContext) {
          TerminalNode id = ((NameIdContext) ((IdContext) literalContext).name()).ID();
          Concrete.Position position = tokenPosition(id.getSymbol());
          arguments.add(new Concrete.NameArgument(position, true, new Concrete.LocalVariable(position, id.getText())));
        } else if (literalContext instanceof UnknownContext) {
          arguments.add(new Concrete.NameArgument(tokenPosition(literalContext.getStart()), true, null));
        } else {
          ok = false;
        }
      }
      if (!ok) {
        myErrorReporter.report(new ParserError(tokenPosition(tele.start), "Unexpected token, expected an identifier"));
        throw new ParseException();
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
  public Concrete.ReferenceExpression visitId(IdContext ctx) {
    return new Concrete.ReferenceExpression(tokenPosition(ctx.name().getStart()), null, visitName(ctx.name()));
  }

  @Override
  public Concrete.Expression visitBinOpArgument(BinOpArgumentContext ctx) {
    return visitAtoms(visitAtomFieldsAcc(ctx.atomFieldsAcc()), ctx.argument());
  }

  private Concrete.Expression parseImplementations(MaybeNewContext newCtx, ImplementStatementsContext implCtx, Token token, Concrete.Expression expr) {
    if (implCtx != null) {
      List<Concrete.ClassFieldImpl> implementStatements = new ArrayList<>(implCtx.implementStatement().size());
      for (ImplementStatementContext implementStatement : implCtx.implementStatement()) {
        implementStatements.add(new Concrete.ClassFieldImpl(tokenPosition(implementStatement.name().getStart()), visitName(implementStatement.name()), visitExpr(implementStatement.expr())));
      }
      expr = new Concrete.ClassExtExpression(tokenPosition(token), expr, implementStatements);
    }

    if (newCtx instanceof WithNewContext) {
      expr = new Concrete.NewExpression(tokenPosition(token), expr);
    }

    return expr;
  }

  private Concrete.LevelExpression parseTruncatedUniverse(TerminalNode terminal) {
    String universe = terminal.getText();
    if (universe.charAt(1) == 'o') {
      return new Concrete.InfLevelExpression(tokenPosition(terminal.getSymbol()));
    }

    return new Concrete.NumberLevelExpression(tokenPosition(terminal.getSymbol()), Integer.parseInt(universe.substring(1, universe.indexOf('-'))));
  }

  @Override
  public Concrete.UniverseExpression visitUniverse(UniverseContext ctx) {
    Concrete.Position position = tokenPosition(ctx.getStart());
    Concrete.LevelExpression lp;
    Concrete.LevelExpression lh;

    String text = ctx.UNIVERSE().getText().substring("\\Type".length());
    lp = text.isEmpty() ? null : new Concrete.NumberLevelExpression(tokenPosition(ctx.UNIVERSE().getSymbol()), Integer.parseInt(text));

    if (ctx.levelAtom().size() >= 1) {
      if (lp == null) {
        lp = visitLevel(ctx.levelAtom(0));
        lh = null;
      } else {
        lh = visitLevel(ctx.levelAtom(0));
      }

      if (ctx.levelAtom().size() >= 2) {
        if (lh == null) {
          lh = visitLevel(ctx.levelAtom(1));
        } else {
          myErrorReporter.report(new ParserError(tokenPosition(ctx.levelAtom(1).getStart()), "h-level is already specified"));
        }
      }
    } else {
      lh = null;
    }

    return new Concrete.UniverseExpression(position, lp, lh);
  }

  @Override
  public Concrete.UniverseExpression visitTruncatedUniverse(TruncatedUniverseContext ctx) {
    Concrete.Position position = tokenPosition(ctx.getStart());
    Concrete.LevelExpression pLevel;

    String text = ctx.TRUNCATED_UNIVERSE().getText();
    text = text.substring(text.indexOf('-') + "-Type".length());
    if (text.isEmpty()) {
      pLevel = ctx.levelAtom() == null ? null : visitLevel(ctx.levelAtom());
    } else {
      pLevel = new Concrete.NumberLevelExpression(tokenPosition(ctx.TRUNCATED_UNIVERSE().getSymbol()), Integer.parseInt(text));
      if (ctx.levelAtom() != null) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.levelAtom().getStart()), "p-level is already specified"));
      }
    }

    return new Concrete.UniverseExpression(position, pLevel, parseTruncatedUniverse(ctx.TRUNCATED_UNIVERSE()));
  }

  @Override
  public Concrete.UniverseExpression visitSetUniverse(SetUniverseContext ctx) {
    Concrete.Position position = tokenPosition(ctx.getStart());
    Concrete.LevelExpression pLevel;

    String text = ctx.SET().getText().substring("\\Set".length());
    if (text.isEmpty()) {
      pLevel = ctx.levelAtom() == null ? null : visitLevel(ctx.levelAtom());
    } else {
      pLevel = new Concrete.NumberLevelExpression(tokenPosition(ctx.SET().getSymbol()), Integer.parseInt(text));
      if (ctx.levelAtom() != null) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.levelAtom().getStart()), "p-level is already specified"));
      }
    }

    return new Concrete.UniverseExpression(position, pLevel, new Concrete.NumberLevelExpression(position, 0));
  }

  @Override
  public Concrete.UniverseExpression visitUniTruncatedUniverse(UniTruncatedUniverseContext ctx) {
    String text = ctx.TRUNCATED_UNIVERSE().getText();
    text = text.substring(text.indexOf('-') + "-Type".length());
    Concrete.LevelExpression pLevel = text.isEmpty() ? null : new Concrete.NumberLevelExpression(tokenPosition(ctx.TRUNCATED_UNIVERSE().getSymbol()), Integer.parseInt(text));
    return new Concrete.UniverseExpression(tokenPosition(ctx.getStart()), pLevel, parseTruncatedUniverse(ctx.TRUNCATED_UNIVERSE()));
  }

  @Override
  public Concrete.UniverseExpression visitUniUniverse(UniUniverseContext ctx) {
    String text = ctx.UNIVERSE().getText().substring("\\Type".length());
    Concrete.LevelExpression lp = text.isEmpty() ? null : new Concrete.NumberLevelExpression(tokenPosition(ctx.UNIVERSE().getSymbol()), Integer.parseInt(text));
    return new Concrete.UniverseExpression(tokenPosition(ctx.getStart()), lp, null);
  }

  @Override
  public Concrete.UniverseExpression visitUniSetUniverse(UniSetUniverseContext ctx) {
    Concrete.Position position = tokenPosition(ctx.getStart());
    String text = ctx.SET().getText().substring("\\Set".length());
    Concrete.LevelExpression pLevel = text.isEmpty() ? null : new Concrete.NumberLevelExpression(tokenPosition(ctx.SET().getSymbol()), Integer.parseInt(text));
    return new Concrete.UniverseExpression(position, pLevel, new Concrete.NumberLevelExpression(position, 0));
  }

  @Override
  public Concrete.UniverseExpression visitProp(PropContext ctx) {
    Concrete.Position pos = tokenPosition(ctx.getStart());
    return new Concrete.UniverseExpression(pos, new Concrete.NumberLevelExpression(pos, 0), new Concrete.NumberLevelExpression(pos, -1));
  }

  private Concrete.LevelExpression visitLevel(LevelAtomContext ctx) {
    return (Concrete.LevelExpression) visit(ctx);
  }

  @Override
  public Concrete.PLevelExpression visitPLevel(PLevelContext ctx) {
    return new Concrete.PLevelExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.HLevelExpression visitHLevel(HLevelContext ctx) {
    return new Concrete.HLevelExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.NumberLevelExpression visitNumLevel(NumLevelContext ctx) {
    return new Concrete.NumberLevelExpression(tokenPosition(ctx.NUMBER().getSymbol()), Integer.parseInt(ctx.NUMBER().getText()));
  }

  @Override
  public Concrete.LevelExpression visitExprLevel(ExprLevelContext ctx) {
    return (Concrete.LevelExpression) visit(ctx.levelExpr());
  }

  @Override
  public Concrete.LevelExpression visitAtomLevelExpr(AtomLevelExprContext ctx) {
    return visitLevel(ctx.levelAtom());
  }

  @Override
  public Concrete.SucLevelExpression visitSucLevelExpr(SucLevelExprContext ctx) {
    return new Concrete.SucLevelExpression(tokenPosition(ctx.getStart()), visitLevel(ctx.levelAtom()));
  }

  @Override
  public Concrete.MaxLevelExpression visitMaxLevelExpr(MaxLevelExprContext ctx) {
    return new Concrete.MaxLevelExpression(tokenPosition(ctx.getStart()), visitLevel(ctx.levelAtom(0)), visitLevel(ctx.levelAtom(1)));
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
        } else
        if (tele instanceof TeleUniverseContext) {
          arguments.add(new Concrete.TypeArgument(true, visitExpr(((TeleUniverseContext) tele).universeAtom())));
          continue;
        } else {
          throw new IllegalStateException();
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        List<Concrete.NameArgument> args = getVars(((TypedContext) typedExpr).expr(0));
        List<Abstract.ReferableSourceNode> vars = new ArrayList<>(args.size());
        for (Concrete.NameArgument arg : args) {
          vars.add(arg.getReferable());
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
    return new Concrete.NumericLiteral(tokenPosition(ctx.NUMBER().getSymbol()), Integer.parseInt(ctx.NUMBER().getText()));
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
      Concrete.Expression expr1;
      if (argument instanceof ArgumentExplicitContext) {
        expr1 = visitAtomFieldsAcc(((ArgumentExplicitContext) argument).atomFieldsAcc());
      } else
      if (argument instanceof ArgumentUniverseContext) {
        expr1 = visitExpr(((ArgumentUniverseContext) argument).universeAtom());
      } else
      if (argument instanceof ArgumentImplicitContext) {
        expr1 = visitExpr(((ArgumentImplicitContext) argument).expr());
      } else {
        throw new IllegalStateException();
      }
      expr = new Concrete.AppExpression(expr.getPosition(), expr, new Concrete.ArgumentExpression(expr1, !(argument instanceof ArgumentImplicitContext), false));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOp(BinOpContext ctx) {
    return parseBinOpSequence(ctx.binOpLeft(), parseImplementations(ctx.maybeNew(), ctx.implementStatements(), ctx.start, (Concrete.Expression) visit(ctx.binOpArg())), ctx.start);
  }

  private Concrete.Expression parseBinOpSequence(List<BinOpLeftContext> leftCtxs, Concrete.Expression expression, Token token) {
    Concrete.Expression left = null;
    Concrete.ReferenceExpression binOp = null;
    List<Abstract.BinOpSequenceElem> sequence = new ArrayList<>(leftCtxs.size());

    for (BinOpLeftContext leftContext : leftCtxs) {
      String name = (String) visit(leftContext.infix());
      Concrete.Expression expr = parseImplementations(leftContext.maybeNew(), leftContext.implementStatements(), leftContext.start, (Concrete.Expression) visit(leftContext.binOpArg()));

      if (left == null) {
        left = expr;
      } else {
        sequence.add(new Abstract.BinOpSequenceElem(binOp, expr));
      }
      binOp = new Concrete.ReferenceExpression(tokenPosition(leftContext.infix().getStart()), null, name);
    }

    if (left == null) {
      return expression;
    }

    sequence.add(new Abstract.BinOpSequenceElem(binOp, expression));
    return new Concrete.BinOpSequenceExpression(tokenPosition(token), left, sequence);
  }

  private Concrete.Expression visitExpr(Expr0Context ctx) {
    return parseBinOpSequence(ctx.binOpLeft(), (Concrete.Expression) visit(ctx.binOpArg()), ctx.start);
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    Concrete.Expression expression = visitExpr(ctx.atom());
    for (FieldAccContext fieldAccContext : ctx.fieldAcc()) {
      if (fieldAccContext instanceof ClassFieldContext) {
        expression = new Concrete.ReferenceExpression(tokenPosition(fieldAccContext.getStart()), expression, visitName(((ClassFieldContext) fieldAccContext).name()));
      } else
      if (fieldAccContext instanceof SigmaFieldContext) {
        expression = new Concrete.ProjExpression(tokenPosition(fieldAccContext.getStart()), expression, Integer.parseInt(((SigmaFieldContext) fieldAccContext).NUMBER().getText()) - 1);
      } else {
        throw new IllegalStateException();
      }
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

  private List<Concrete.FunctionClause> visitClauses(ClausesContext ctx) {
    List<ClauseContext> clauses = ctx instanceof ClausesWithBracesContext ? ((ClausesWithBracesContext) ctx).clause() : ((ClausesWithoutBracesContext) ctx).clause();
    List<Concrete.FunctionClause> result = new ArrayList<>(clauses.size());
    for (ClauseContext clause : clauses) {
      result.add(visitClause(clause));
    }
    return result;
  }

  @Override
  public Concrete.FunctionClause visitClause(ClauseContext clauseCtx) {
    List<Concrete.Pattern> patterns = new ArrayList<>(clauseCtx.pattern().size());
    for (PatternContext patternCtx : clauseCtx.pattern()) {
      patterns.add(visitPattern(patternCtx));
    }
    return new Concrete.FunctionClause(tokenPosition(clauseCtx.start), patterns, clauseCtx.expr() == null ? null : visitExpr(clauseCtx.expr()));
  }

  private List<Concrete.ReferenceExpression> checkElimExpressions(List<? extends Concrete.Expression> expressions) {
    List<Concrete.ReferenceExpression> result = new ArrayList<>(expressions.size());
    for (Concrete.Expression elimExpr : expressions) {
      if (!(elimExpr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) elimExpr).getExpression() == null)) {
        myErrorReporter.report(new ParserError(elimExpr.getPosition(), "\\elim can be applied only to a local variable"));
        return null;
      }
      result.add((Concrete.ReferenceExpression) elimExpr);
    }
    return result;
  }

  @Override
  public Concrete.Expression visitCase(CaseContext ctx) {
    List<Concrete.Expression> elimExprs = new ArrayList<>(ctx.expr0().size());
    for (Expr0Context exprCtx : ctx.expr0()) {
      elimExprs.add(visitExpr(exprCtx));
    }
    List<Concrete.FunctionClause> clauses = new ArrayList<>(ctx.clause().size());
    for (ClauseContext clauseCtx : ctx.clause()) {
      clauses.add(visitClause(clauseCtx));
    }
    return new Concrete.CaseExpression(tokenPosition(ctx.getStart()), elimExprs, clauses);
  }

  @Override
  public Concrete.LetClause visitLetClause(LetClauseContext ctx) {
    List<Concrete.Argument> arguments = visitLamTeles(ctx.tele());
    Concrete.Expression resultType = ctx.typeAnnotation() == null ? null : visitExpr(ctx.typeAnnotation().expr());
    return new Concrete.LetClause(tokenPosition(ctx.getStart()), ctx.ID().getText(), arguments, resultType, visitExpr(ctx.expr()));
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
