package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractLevelExpressionVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;
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

  private Concrete.LocalVariable getVar(AtomFieldsAccContext ctx) {
    if (!ctx.fieldAcc().isEmpty() || !(ctx.atom() instanceof AtomLiteralContext)) {
      return null;
    }
    LiteralContext literal = ((AtomLiteralContext) ctx.atom()).literal();
    if (literal instanceof UnknownContext) {
      Concrete.Position position = tokenPosition(literal.start);
      return new Concrete.LocalVariable(position, null);
    }
    if (literal instanceof NameContext && ((NameContext) literal).prefix().PREFIX() != null) {
      Concrete.Position position = tokenPosition(literal.start);
      return new Concrete.LocalVariable(position, ((NameContext) literal).prefix().PREFIX().getText());
    }
    return null;
  }

  private boolean getVars(BinOpArgumentContext expr, List<Concrete.LocalVariable> result) {
    Concrete.LocalVariable firstArg = getVar(expr.atomFieldsAcc());
    if (firstArg == null) {
      return false;
    }

    result.add(firstArg);
    for (ArgumentContext argument : expr.argument()) {
      if (!(argument instanceof ArgumentExplicitContext)) {
        return false;
      }

      Concrete.LocalVariable arg = getVar(((ArgumentExplicitContext) argument).atomFieldsAcc());
      if (arg == null) {
        return false;
      }
      result.add(arg);
    }
    return true;
  }

  private boolean getVars(ExprContext expr, List<Concrete.LocalVariable> result) {
    if (!(expr instanceof BinOpContext && ((BinOpContext) expr).binOpArg() instanceof BinOpArgumentContext && ((BinOpContext) expr).maybeNew() instanceof NoNewContext && ((BinOpContext) expr).implementStatements() == null && ((BinOpContext) expr).postfix().isEmpty() && ((BinOpArgumentContext) ((BinOpContext) expr).binOpArg()).onlyLevelAtom().isEmpty())) {
      return false;
    }

    for (BinOpLeftContext leftCtx : ((BinOpContext) expr).binOpLeft()) {
      if (!(leftCtx.maybeNew() instanceof NoNewContext && leftCtx.binOpArg() instanceof BinOpArgumentContext && leftCtx.implementStatements() == null && leftCtx.postfix().isEmpty() && leftCtx.infix().INFIX() != null && ((BinOpArgumentContext) leftCtx.binOpArg()).onlyLevelAtom().isEmpty())) {
        return false;
      }
      if (!getVars((BinOpArgumentContext) leftCtx.binOpArg(), result)) {
        return false;
      }
      result.add(new Concrete.LocalVariable(tokenPosition(leftCtx.infix().start), leftCtx.infix().INFIX().getText()));
    }

    return getVars((BinOpArgumentContext) ((BinOpContext) expr).binOpArg(), result);
  }

  private List<Concrete.LocalVariable> getVarList(ExprContext expr, List<TerminalNode> infixList) {
    List<Concrete.LocalVariable> result = new ArrayList<>();
    if (getVars(expr, result)) {
      for (TerminalNode infix : infixList) {
        result.add(new Concrete.LocalVariable(tokenPosition(infix.getSymbol()), infix.getText()));
      }
      return result;
    } else {
      myErrorReporter.report(new ParserError(tokenPosition(expr.getStart()), "Expected a list of variables"));
      throw new ParseException();
    }
  }

  private List<Concrete.LocalVariable> getVars(TypedVarsContext ctx) {
    List<Concrete.LocalVariable> result = new ArrayList<>(ctx.id().size() + 1);
    result.add(new Concrete.LocalVariable(tokenPosition(ctx.start), ctx.INFIX().getText()));
    for (IdContext idCtx : ctx.id()) {
      result.add(new Concrete.LocalVariable(tokenPosition(idCtx.start), idCtx.getText()));
    }
    return result;
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
  public String visitId(IdContext ctx) {
    if (ctx.PREFIX() != null) {
      return ctx.PREFIX().getText();
    }
    if (ctx.INFIX() != null) {
      return ctx.INFIX().getText();
    }
    throw new IllegalStateException();
  }

  @Override
  public String visitPrefix(PrefixContext ctx) {
    if (ctx.PREFIX() != null) {
      return ctx.PREFIX().getText();
    }
    if (ctx.PREFIX_INFIX() != null) {
      String s = ctx.PREFIX_INFIX().getText();
      return s.substring(1, s.length());
    }
    throw new IllegalStateException();
  }

  @Override
  public String visitInfix(InfixContext ctx) {
    if (ctx.INFIX() != null) {
      return ctx.INFIX().getText();
    }
    if (ctx.INFIX_PREFIX() != null) {
      String s = ctx.INFIX_PREFIX().getText();
      return s.substring(1, s.length());
    }
    throw new IllegalStateException();
  }

  @Override
  public String visitPostfix(PostfixContext ctx) {
    String s;
    if (ctx.POSTFIX_INFIX() != null) {
      s = ctx.POSTFIX_INFIX().getText();
    } else if (ctx.POSTFIX_PREFIX() != null) {
      s = ctx.POSTFIX_PREFIX().getText();
    } else {
      throw new IllegalStateException();
    }
    return s.substring(0, s.length() - 1);
  }

  private List<String> getModulePath(String module) {
    String[] modulePath = module.split("::");
    assert modulePath[0].isEmpty();
    return Arrays.asList(modulePath).subList(1, modulePath.length);
  }

  @Override
  public Concrete.ModuleCallExpression visitAtomModuleCall(AtomModuleCallContext ctx) {
    return new Concrete.ModuleCallExpression(tokenPosition(ctx.getStart()), getModulePath(ctx.MODULE_PATH().getText()));
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
    List<String> modulePath = ctx.nsCmdRoot().MODULE_PATH() == null ? null : getModulePath(ctx.nsCmdRoot().MODULE_PATH().getText());
    List<String> path = new ArrayList<>();
    if (ctx.nsCmdRoot().id() != null) {
      path.add(visitId(ctx.nsCmdRoot().id()));
    }
    for (FieldAccContext fieldAccContext : ctx.fieldAcc()) {
      if (fieldAccContext instanceof ClassFieldAccContext) {
        path.add(visitId(((ClassFieldAccContext) fieldAccContext).id()));
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(fieldAccContext.getStart()), "Expected a name"));
      }
    }

    List<String> names;
    if (!ctx.id().isEmpty()) {
      names = new ArrayList<>(ctx.id().size());
      for (IdContext idCtx : ctx.id()) {
        names.add(visitId(idCtx));
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
    if (ctx.atomPatternOrID().size() == 0) {
      return new Concrete.NamePattern(tokenPosition(ctx.start), visitPrefix(ctx.prefix()));
    } else {
      List<Concrete.Pattern> patterns = new ArrayList<>(ctx.atomPatternOrID().size());
      for (AtomPatternOrIDContext atomCtx : ctx.atomPatternOrID()) {
        patterns.add(visitAtomPattern(atomCtx));
      }
      return new Concrete.ConstructorPattern(tokenPosition(ctx.start), visitPrefix(ctx.prefix()), patterns);
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
    return new Concrete.NamePattern(position, visitPrefix(ctx.prefix()));
  }

  @Override
  public Concrete.Pattern visitPatternAny(PatternAnyContext ctx) {
    Concrete.Position position = tokenPosition(ctx.getStart());
    return new Concrete.NamePattern(position, "_");
  }

  @Override
  public Concrete.ClassField visitClassField(ClassFieldContext ctx) {
    return new Concrete.ClassField(tokenPosition(ctx.getStart()), visitId(ctx.id()), visitPrecedence(ctx.precedence()), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.ClassView visitDefClassView(DefClassViewContext ctx) {
    List<Concrete.ClassViewField> fields = new ArrayList<>(ctx.classViewField().size());

    Concrete.Expression expr = visitExpr(ctx.expr());
    if (!(expr instanceof Concrete.ReferenceExpression)) {
      myErrorReporter.report(new ParserError(expr.getPosition(), "Expected a class"));
      throw new ParseException();
    }

    Concrete.ClassView classView = new Concrete.ClassView(tokenPosition(ctx.getStart()), visitId(ctx.id(0)), (Concrete.ReferenceExpression) expr, visitId(ctx.id(1)), fields);
    for (ClassViewFieldContext classViewFieldContext : ctx.classViewField()) {
      fields.add(visitClassViewField(classViewFieldContext, classView));
    }

    return classView;
  }

  private Concrete.ClassViewField visitClassViewField(ClassViewFieldContext ctx, Concrete.ClassView classView) {
    String underlyingField = visitId(ctx.id(0));
    return new Concrete.ClassViewField(tokenPosition(ctx.id(0).start), ctx.id().size() > 1 ? visitId(ctx.id(1)) : underlyingField, ctx.precedence() == null ? Abstract.Precedence.DEFAULT : visitPrecedence(ctx.precedence()), underlyingField, classView);
  }

  @Override
  public Concrete.ClassViewInstance visitDefInstance(DefInstanceContext ctx) {
    List<Concrete.Parameter> arguments = visitFunctionArguments(ctx.tele());
    Concrete.Expression term = visitExpr(ctx.expr());
    if (term instanceof Concrete.NewExpression) {
      Concrete.Expression type = ((Concrete.NewExpression) term).getExpression();
      if (type instanceof Concrete.ClassExtExpression) {
        Concrete.ClassExtExpression classExt = (Concrete.ClassExtExpression) type;
        if (classExt.getBaseClassExpression() instanceof Concrete.ReferenceExpression) {
          return new Concrete.ClassViewInstance(tokenPosition(ctx.getStart()), ctx.defaultInst() instanceof WithDefaultContext, visitId(ctx.id()), Abstract.Precedence.DEFAULT, arguments, (Concrete.ReferenceExpression) classExt.getBaseClassExpression(), classExt.getStatements());
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
    if (ctx != null && ctx.atomFieldsAcc() != null && !ctx.atomFieldsAcc().isEmpty()) {
      List<Concrete.Expression> expressions = new ArrayList<>(ctx.atomFieldsAcc().size());
      for (AtomFieldsAccContext exprCtx : ctx.atomFieldsAcc()) {
        expressions.add(visitAtomFieldsAcc(exprCtx));
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
    Concrete.FunctionDefinition result = new Concrete.FunctionDefinition(tokenPosition(ctx.getStart()), visitId(ctx.id()), visitPrecedence(ctx.precedence()), visitFunctionArguments(ctx.tele()), resultType, body, statements);

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

  private List<Concrete.Parameter> visitFunctionArguments(List<TeleContext> teleCtx) {
    List<Concrete.Parameter> arguments = new ArrayList<>();
    for (TeleContext tele : teleCtx) {
      List<Concrete.Parameter> args = visitLamTele(tele);
      if (args != null) {
        if (args.get(0) instanceof Concrete.TelescopeParameter) {
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
    final Concrete.UniverseExpression universe;
    if (ctx.expr() != null) {
      Object expr = visit(ctx.expr());
      if (expr instanceof Concrete.UniverseExpression) {
        universe = (Concrete.UniverseExpression) expr;
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.expr().getStart()), "Specified type of the data definition is not a universe"));
        universe = null;
      }
    } else {
      universe = null;
    }
    List<Concrete.ReferenceExpression> eliminatedReferences = ctx.dataBody() instanceof DataClausesContext ? visitElim(((DataClausesContext) ctx.dataBody()).elim()) : null;
    Concrete.DataDefinition dataDefinition = new Concrete.DataDefinition(tokenPosition(ctx.getStart()), visitId(ctx.id()), visitPrecedence(ctx.precedence()), visitTeles(ctx.tele()), eliminatedReferences, ctx.isTruncated() instanceof TruncatedContext, universe, new ArrayList<>());
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
      try {
        List<Concrete.FunctionClause> clauses;
        if (conCtx.elim() != null || !conCtx.clause().isEmpty()) {
          clauses = new ArrayList<>(conCtx.clause().size());
          for (ClauseContext clauseCtx : conCtx.clause()) {
            clauses.add(visitClause(clauseCtx));
          }
        } else {
          clauses = Collections.emptyList();
        }

        result.add(new Concrete.Constructor(
          tokenPosition(conCtx.start),
          visitId(conCtx.id()),
          visitPrecedence(conCtx.precedence()),
          def,
          visitTeles(conCtx.tele()),
          visitElim(conCtx.elim()),
          clauses));
      } catch (ParseException ignored) {

      }
    }
    return result;
  }

  private void misplacedDefinitionError(Concrete.Position position) {
    myErrorReporter.report(new ParserError(position, "This definition is not allowed here"));
  }

  private void visitInstanceStatements(List<ClassStatContext> ctx, List<Concrete.ClassField> fields, List<Concrete.Implementation> implementations, List<Concrete.Definition> definitions) {
    for (ClassStatContext statementCtx : ctx) {
      if (statementCtx == null) {
        continue;
      }

      try {
        Concrete.SourceNode sourceNode = (Concrete.SourceNode) visit(statementCtx);
        if (sourceNode != null) {
          Concrete.Definition definition;
          if (sourceNode instanceof Concrete.Definition) {
            definition = (Concrete.Definition) sourceNode;
          } else
          if (sourceNode instanceof Concrete.DefineStatement) {
            definition = ((Concrete.DefineStatement) sourceNode).getDefinition();
          } else {
            misplacedDefinitionError(sourceNode.getPosition());
            continue;
          }

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
        }
      } catch (ParseException ignored) {

      }
    }
  }

  @Override
  public Concrete.Statement visitClassStatement(ClassStatementContext ctx) {
    return (Concrete.Statement) visit(ctx.statement());
  }

  @Override
  public Concrete.ClassDefinition visitDefClass(DefClassContext ctx) {
    List<Concrete.TypeParameter> polyParameters = visitTeles(ctx.tele());
    List<Concrete.SuperClass> superClasses = new ArrayList<>(ctx.atomFieldsAcc().size());
    List<Concrete.ClassField> fields = new ArrayList<>();
    List<Concrete.Implementation> implementations = new ArrayList<>();
    List<Concrete.Statement> globalStatements = visitWhere(ctx.where());
    List<Concrete.Definition> instanceDefinitions;

    if (ctx.classStat().isEmpty()) {
      instanceDefinitions = Collections.emptyList();
    } else {
      instanceDefinitions = new ArrayList<>(ctx.classStat().size());
      visitInstanceStatements(ctx.classStat(), fields, implementations, instanceDefinitions);
    }

    for (AtomFieldsAccContext exprCtx : ctx.atomFieldsAcc()) {
      superClasses.add(new Concrete.SuperClass(tokenPosition(exprCtx.getStart()), visitAtomFieldsAcc(exprCtx)));
    }

    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(tokenPosition(ctx.getStart()), visitId(ctx.id()), polyParameters, superClasses, fields, implementations, globalStatements, instanceDefinitions);
    for (Concrete.ClassField field : fields) {
      field.setParent(classDefinition);
    }
    for (Concrete.Implementation implementation : implementations) {
      implementation.setParent(classDefinition);
    }
    for (Concrete.Definition definition : instanceDefinitions) {
      definition.setParent(classDefinition);
      definition.setNotStatic();
    }
    for (Concrete.Statement statement : globalStatements) {
      if (statement instanceof Concrete.DefineStatement) {
        ((Concrete.DefineStatement) statement).getDefinition().setParent(classDefinition);
      }
    }
    return classDefinition;
  }

  @Override
  public Concrete.Implementation visitClassImplement(ClassImplementContext ctx) {
    return new Concrete.Implementation(tokenPosition(ctx.start), visitId(ctx.id()), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.ReferenceExpression visitName(NameContext ctx) {
    return new Concrete.ReferenceExpression(tokenPosition(ctx.start), null, visitPrefix(ctx.prefix()));
  }

  @Override
  public Concrete.InferHoleExpression visitUnknown(UnknownContext ctx) {
    return new Concrete.InferHoleExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.GoalExpression visitGoal(GoalContext ctx) {
    return new Concrete.GoalExpression(tokenPosition(ctx.start), ctx.id() == null ? null : visitId(ctx.id()), ctx.expr() == null ? null : visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    Concrete.Expression domain = visitExpr(ctx.expr(0));
    Concrete.Expression codomain = visitExpr(ctx.expr(1));
    List<Concrete.TypeParameter> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeParameter(domain.getPosition(), true, domain));
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

  private List<Concrete.Parameter> visitLamTele(TeleContext tele) {
    List<Concrete.Parameter> arguments = new ArrayList<>(3);
    if (tele instanceof ExplicitContext || tele instanceof ImplicitContext) {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      Concrete.Expression typeExpr;
      List<Concrete.LocalVariable> vars;
      if (typedExpr instanceof TypedVarsContext) {
        vars = getVars((TypedVarsContext) typedExpr);
        typeExpr = visitExpr(((TypedVarsContext) typedExpr).expr());
      } else
      if (typedExpr instanceof TypedContext) {
        vars = getVarList(((TypedContext) typedExpr).expr(0), ((TypedContext) typedExpr).INFIX());
        typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
      } else if (typedExpr instanceof NotTypedContext) {
        vars = getVarList(((NotTypedContext) typedExpr).expr(), Collections.emptyList());
        typeExpr = null;
      } else {
        throw new IllegalStateException();
      }
      if (typeExpr == null) {
        for (Concrete.LocalVariable var : vars) {
          arguments.add(new Concrete.NameParameter(var.getPosition(), explicit, var.getName()));
        }
      } else {
        arguments.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, vars, typeExpr));
      }
    } else {
      boolean ok = tele instanceof TeleLiteralContext;
      if (ok) {
        LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
        if (literalContext instanceof NameContext && ((NameContext) literalContext).prefix().PREFIX() != null) {
          TerminalNode id = ((NameContext) literalContext).prefix().PREFIX();
          arguments.add(new Concrete.NameParameter(tokenPosition(id.getSymbol()), true, id.getText()));
        } else if (literalContext instanceof UnknownContext) {
          arguments.add(new Concrete.NameParameter(tokenPosition(literalContext.getStart()), true, null));
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

  private List<Concrete.Parameter> visitLamTeles(List<TeleContext> tele) {
    List<Concrete.Parameter> arguments = new ArrayList<>();
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
  public Concrete.Expression visitBinOpArgument(BinOpArgumentContext ctx) {
    Concrete.Expression expr = visitAtomFieldsAcc(ctx.atomFieldsAcc());
    if (ctx.onlyLevelAtom().isEmpty()) {
      return visitAtoms(expr, ctx.argument());
    }

    if (!(expr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) expr).getExpression() == null)) {
      myErrorReporter.report(new ParserError(tokenPosition(ctx.onlyLevelAtom(0).start), "Level annotations are allowed only after a reference"));
      return visitAtoms(expr, ctx.argument());
    }

    Concrete.LevelExpression pLevel = null;
    Concrete.LevelExpression hLevel = null;
    for (OnlyLevelAtomContext levelCtx : ctx.onlyLevelAtom()) {
      Object obj = visit(levelCtx);
      if (obj instanceof Pair) {
        if ((pLevel == null || ((Pair) obj).proj1 == null) && (hLevel == null || ((Pair) obj).proj2 == null)) {
          if (((Pair) obj).proj1 != null) {
            pLevel = (Concrete.LevelExpression) ((Pair) obj).proj1;
          }
          if (((Pair) obj).proj2 != null) {
            hLevel = (Concrete.LevelExpression) ((Pair) obj).proj2;
          }
        } else {
          myErrorReporter.report(new ParserError(tokenPosition(levelCtx.start), (pLevel != null ? "p" : "h") + "-level is already specified"));
        }
      } else if (obj instanceof Concrete.LevelExpression) {
        LevelType type = getLevelType((Concrete.LevelExpression) obj);
        if (type == LevelType.PLevel) {
          if (pLevel != null) {
            myErrorReporter.report(new ParserError(tokenPosition(levelCtx.start), "p-level is already specified"));
          } else {
            pLevel = (Concrete.LevelExpression) obj;
          }
        } else if (type == LevelType.HLevel) {
          if (hLevel != null) {
            myErrorReporter.report(new ParserError(tokenPosition(levelCtx.start), "h-level is already specified"));
          } else {
            hLevel = (Concrete.LevelExpression) obj;
          }
        } else if (type == LevelType.Unknown) {
          if (pLevel == null) {
            pLevel = (Concrete.LevelExpression) obj;
          } else if (hLevel == null) {
            hLevel = (Concrete.LevelExpression) obj;
          } else {
            myErrorReporter.report(new ParserError(tokenPosition(levelCtx.start), "Both levels are already specified"));
          }
        } else {
          myErrorReporter.report(new ParserError(tokenPosition(levelCtx.start), "Cannot mix levels of different type"));
        }
      } else {
        throw new IllegalStateException();
      }
    }

    return visitAtoms(new Concrete.ReferenceExpression(expr.getPosition(), ((Concrete.ReferenceExpression) expr).getName(), pLevel, hLevel), ctx.argument());
  }

  enum LevelType { PLevel, HLevel, Unknown }

  private LevelType getLevelType(Concrete.LevelExpression expr) {
    return expr.accept(new AbstractLevelExpressionVisitor<Void, LevelType>() {
      @Override
      public LevelType visitInf(Abstract.InfLevelExpression expr, Void param) {
        return LevelType.HLevel;
      }

      @Override
      public LevelType visitLP(Abstract.PLevelExpression expr, Void param) {
        return LevelType.PLevel;
      }

      @Override
      public LevelType visitLH(Abstract.HLevelExpression expr, Void param) {
        return LevelType.HLevel;
      }

      @Override
      public LevelType visitNumber(Abstract.NumberLevelExpression expr, Void param) {
        return LevelType.Unknown;
      }

      @Override
      public LevelType visitSuc(Abstract.SucLevelExpression expr, Void param) {
        return expr.getExpression().accept(this, null);
      }

      @Override
      public LevelType visitMax(Abstract.MaxLevelExpression expr, Void param) {
        LevelType type1 = expr.getLeft().accept(this, null);
        LevelType type2 = expr.getRight().accept(this, null);
        return type1 == null || type2 == null || type1 != type2 && type1 != LevelType.Unknown && type2 != LevelType.Unknown ? null : type1 == LevelType.Unknown ? type2 : type1;
      }

      @Override
      public LevelType visitVar(Abstract.InferVarLevelExpression expr, Void param) {
        LevelVariable.LvlType type = expr.getVariable().getType();
        return type == LevelVariable.LvlType.PLVL ? LevelType.PLevel : type == LevelVariable.LvlType.HLVL ? LevelType.HLevel : null;
      }
    }, null);
  }

  private Concrete.Expression parseImplementations(MaybeNewContext newCtx, ImplementStatementsContext implCtx, Token token, Concrete.Expression expr) {
    if (implCtx != null) {
      List<Concrete.ClassFieldImpl> implementStatements = new ArrayList<>(implCtx.implementStatement().size());
      for (ImplementStatementContext implementStatement : implCtx.implementStatement()) {
        implementStatements.add(new Concrete.ClassFieldImpl(tokenPosition(implementStatement.id().start), visitId(implementStatement.id()), visitExpr(implementStatement.expr())));
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

  private Concrete.LevelExpression visitLevel(MaybeLevelAtomContext ctx) {
    return (Concrete.LevelExpression) visit(ctx);
  }

  @Override
  public Concrete.LevelExpression visitWithLevelAtom(WithLevelAtomContext ctx) {
    return visitLevel(ctx.levelAtom());
  }

  @Override
  public Concrete.LevelExpression visitWithoutLevelAtom(WithoutLevelAtomContext ctx) {
    return null;
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
  public Concrete.LevelExpression visitParenLevel(ParenLevelContext ctx) {
    return (Concrete.LevelExpression) visit(ctx.levelExpr());
  }

  @Override
  public Concrete.LevelExpression visitAtomLevel(AtomLevelContext ctx) {
    return visitLevel(ctx.levelAtom());
  }

  @Override
  public Concrete.SucLevelExpression visitSucLevel(SucLevelContext ctx) {
    return new Concrete.SucLevelExpression(tokenPosition(ctx.start), visitLevel(ctx.levelAtom()));
  }

  @Override
  public Concrete.MaxLevelExpression visitMaxLevel(MaxLevelContext ctx) {
    return new Concrete.MaxLevelExpression(tokenPosition(ctx.start), visitLevel(ctx.levelAtom(0)), visitLevel(ctx.levelAtom(1)));
  }

  @Override
  public Concrete.PLevelExpression visitPOnlyLevel(POnlyLevelContext ctx) {
    return new Concrete.PLevelExpression(tokenPosition(ctx.start));
  }

  @Override
  public Concrete.HLevelExpression visitHOnlyLevel(HOnlyLevelContext ctx) {
    return new Concrete.HLevelExpression(tokenPosition(ctx.start));
  }

  @Override
  public Object visitParenOnlyLevel(ParenOnlyLevelContext ctx) {
    return visit(ctx.onlyLevelExpr());
  }

  @Override
  public Object visitAtomOnlyLevel(AtomOnlyLevelContext ctx) {
    return visit(ctx.onlyLevelAtom());
  }

  @Override
  public Pair<Concrete.LevelExpression, Concrete.LevelExpression> visitLevelsOnlyLevel(LevelsOnlyLevelContext ctx) {
    return ctx.maybeLevelAtom().isEmpty() ? new Pair<>(new Concrete.NumberLevelExpression(tokenPosition(ctx.start), 0), new Concrete.NumberLevelExpression(tokenPosition(ctx.start), -1)) : new Pair<>(visitLevel(ctx.maybeLevelAtom(0)), visitLevel(ctx.maybeLevelAtom(1)));
  }

  @Override
  public Concrete.SucLevelExpression visitSucOnlyLevel(SucOnlyLevelContext ctx) {
    return new Concrete.SucLevelExpression(tokenPosition(ctx.start), visitLevel(ctx.levelAtom()));
  }

  @Override
  public Concrete.MaxLevelExpression visitMaxOnlyLevel(MaxOnlyLevelContext ctx) {
    return new Concrete.MaxLevelExpression(tokenPosition(ctx.start), visitLevel(ctx.levelAtom(0)), visitLevel(ctx.levelAtom(1)));
  }

  private List<Concrete.TypeParameter> visitTeles(List<TeleContext> teles) {
    List<Concrete.TypeParameter> arguments = new ArrayList<>(teles.size());
    for (TeleContext tele : teles) {
      boolean explicit = !(tele instanceof ImplicitContext);
      TypedExprContext typedExpr;
      if (explicit) {
        if (tele instanceof ExplicitContext) {
          typedExpr = ((ExplicitContext) tele).typedExpr();
        } else
        if (tele instanceof TeleLiteralContext) {
          arguments.add(new Concrete.TypeParameter(true, visitExpr(((TeleLiteralContext) tele).literal())));
          continue;
        } else
        if (tele instanceof TeleUniverseContext) {
          arguments.add(new Concrete.TypeParameter(true, visitExpr(((TeleUniverseContext) tele).universeAtom())));
          continue;
        } else {
          throw new IllegalStateException();
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        arguments.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, getVarList(((TypedContext) typedExpr).expr(0), ((TypedContext) typedExpr).INFIX()), visitExpr(((TypedContext) typedExpr).expr(1))));
      } else
      if (typedExpr instanceof TypedVarsContext) {
        arguments.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, getVars((TypedVarsContext) typedExpr), visitExpr(((TypedVarsContext) typedExpr).expr())));
      } else
      if (typedExpr instanceof NotTypedContext) {
        arguments.add(new Concrete.TypeParameter(explicit, visitExpr(((NotTypedContext) typedExpr).expr())));
      } else {
        throw new IllegalStateException();
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
    List<Concrete.TypeParameter> args = visitTeles(ctx.tele());
    for (Concrete.TypeParameter arg : args) {
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
      expr = new Concrete.AppExpression(expr.getPosition(), expr, new Concrete.Argument(expr1, !(argument instanceof ArgumentImplicitContext)));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOp(BinOpContext ctx) {
    return parseBinOpSequence(ctx.binOpLeft(), parseImplementations(ctx.maybeNew(), ctx.implementStatements(), ctx.start, (Concrete.Expression) visit(ctx.binOpArg())), ctx.postfix(), ctx.start);
  }

  private Concrete.Expression parseBinOpSequence(List<BinOpLeftContext> leftCtxs, Concrete.Expression expression, List<PostfixContext> postfixCtxs, Token token) {
    Concrete.Expression left = null;
    Concrete.ReferenceExpression binOp = null;
    List<Abstract.BinOpSequenceElem> sequence = new ArrayList<>(leftCtxs.size() + postfixCtxs.size());

    for (BinOpLeftContext leftContext : leftCtxs) {
      String name = visitInfix(leftContext.infix());
      Concrete.Expression expr = parseImplementations(leftContext.maybeNew(), leftContext.implementStatements(), leftContext.start, (Concrete.Expression) visit(leftContext.binOpArg()));

      if (left == null) {
        left = expr;
      } else {
        sequence.add(new Abstract.BinOpSequenceElem(binOp, expr));
      }

      for (PostfixContext postfixContext : leftContext.postfix()) {
        sequence.add(new Abstract.BinOpSequenceElem(new Concrete.ReferenceExpression(tokenPosition(postfixContext.start), null, visitPostfix(postfixContext)), null));
      }

      binOp = new Concrete.ReferenceExpression(tokenPosition(leftContext.infix().getStart()), null, name);
    }

    if (left == null) {
      left = expression;
    } else {
      sequence.add(new Abstract.BinOpSequenceElem(binOp, expression));
    }

    for (PostfixContext postfixContext : postfixCtxs) {
      sequence.add(new Abstract.BinOpSequenceElem(new Concrete.ReferenceExpression(tokenPosition(postfixContext.start), null, visitPostfix(postfixContext)), null));
    }

    return sequence.isEmpty() ? left : new Concrete.BinOpSequenceExpression(tokenPosition(token), left, sequence);
  }

  private Concrete.Expression visitExpr(Expr0Context ctx) {
    return parseBinOpSequence(ctx.binOpLeft(), (Concrete.Expression) visit(ctx.binOpArg()), ctx.postfix(), ctx.start);
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    Concrete.Expression expression = visitExpr(ctx.atom());
    for (FieldAccContext fieldAccContext : ctx.fieldAcc()) {
      if (fieldAccContext instanceof ClassFieldAccContext) {
        expression = new Concrete.ReferenceExpression(tokenPosition(fieldAccContext.getStart()), expression, visitId(((ClassFieldAccContext) fieldAccContext).id()));
      } else
      if (fieldAccContext instanceof SigmaFieldAccContext) {
        expression = new Concrete.ProjExpression(tokenPosition(fieldAccContext.getStart()), expression, Integer.parseInt(((SigmaFieldAccContext) fieldAccContext).NUMBER().getText()) - 1);
      } else {
        throw new IllegalStateException();
      }
    }
    return expression;
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
        throw new ParseException();
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
    List<Concrete.Parameter> arguments = visitLamTeles(ctx.tele());
    Concrete.Expression resultType = ctx.typeAnnotation() == null ? null : visitExpr(ctx.typeAnnotation().expr());
    return new Concrete.LetClause(tokenPosition(ctx.getStart()), visitId(ctx.id()), arguments, resultType, visitExpr(ctx.expr()));
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
