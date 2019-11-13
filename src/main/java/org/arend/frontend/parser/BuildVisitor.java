package org.arend.frontend.parser;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.arend.error.ErrorReporter;
import org.arend.frontend.group.SimpleNamespaceCommand;
import org.arend.frontend.reference.*;
import org.arend.module.ModulePath;
import org.arend.naming.reference.*;
import org.arend.term.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.*;
import org.arend.util.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.arend.frontend.parser.ArendParser.*;

public class BuildVisitor extends ArendBaseVisitor {
  private final ModulePath myModule;
  private final ErrorReporter myErrorReporter;

  public BuildVisitor(ModulePath module, ErrorReporter errorReporter) {
    myModule = module;
    myErrorReporter = errorReporter;
  }

  private String getVar(AtomFieldsAccContext ctx) {
    if (!ctx.NUMBER().isEmpty() || !(ctx.atom() instanceof AtomLiteralContext)) {
      return null;
    }
    LiteralContext literal = ((AtomLiteralContext) ctx.atom()).literal();
    if (literal instanceof UnknownContext) {
      return "_";
    }
    if (literal instanceof NameContext && ((NameContext) literal).longName() != null) {
      List<TerminalNode> ids = ((NameContext) literal).longName().ID();
      if (ids.size() == 1) {
        return ids.get(0).getText();
      }
    }
    return null;
  }

  private boolean getVars(ExprContext expr, List<ParsedLocalReferable> vars) {
    if (!(expr instanceof AppContext)) {
      return false;
    }
    NewExprContext newExpr = ((AppContext) expr).newExpr();
    if (!(newExpr.appExpr() instanceof AppArgumentContext && newExpr.appPrefix() == null && newExpr.implementStatements() == null)) {
      return false;
    }

    ArgumentAppExprContext argCtx = ((AppArgumentContext) newExpr.appExpr()).argumentAppExpr();
    if (!argCtx.onlyLevelAtom().isEmpty()) {
      return false;
    }
    String var = getVar(argCtx.atomFieldsAcc());
    if (var == null) {
      return false;
    }
    if (var.equals("_")) {
      var = null;
    }
    vars.add(new ParsedLocalReferable(tokenPosition(argCtx.start), var));

    for (ArgumentContext argument : argCtx.argument()) {
      if (argument instanceof ArgumentExplicitContext) {
        String arg = getVar(((ArgumentExplicitContext) argument).atomFieldsAcc());
        if (arg == null) {
          return false;
        }
        if (arg.equals("_")) {
          arg = null;
        }

        vars.add(new ParsedLocalReferable(tokenPosition(((ArgumentExplicitContext) argument).atomFieldsAcc().start), arg));
      } else {
        return false;
      }
    }

    return true;
  }

  private void getVarList(ExprContext expr, List<ParsedLocalReferable> vars) {
    if (!getVars(expr, vars)) {
      myErrorReporter.report(new ParserError(tokenPosition(expr.start), "Expected a list of variables"));
      throw new ParseException();
    }
  }

  private String getInfixText(TerminalNode node) {
    String name = node.getText();
    return name.substring(1, name.length() - 1);
  }

  private String getPostfixText(TerminalNode node) {
    return node.getText().substring(1);
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

  private void visitStatementList(List<StatementContext> statementCtxs, List<Group> subgroups, List<ChildNamespaceCommand> namespaceCommands, ChildGroup parent, TCClassReferable enclosingClass) {
    for (StatementContext statementCtx : statementCtxs) {
      try {
        Object statement = visitStatement(statementCtx, parent, enclosingClass);
        if (statement instanceof Group) {
          subgroups.add((Group) statement);
        } else if (statement instanceof SimpleNamespaceCommand) {
          namespaceCommands.add((SimpleNamespaceCommand) statement);
        } else {
          if (statementCtx != null) {
            myErrorReporter.report(new ParserError(tokenPosition(statementCtx.start), "Unknown statement"));
          }
        }
      } catch (ParseException ignored) {

      }
    }
  }

  private Object visitStatement(StatementContext statementCtx, ChildGroup parent, TCClassReferable enclosingClass) {
    if (statementCtx instanceof StatCmdContext) {
      return visitStatCmd((StatCmdContext) statementCtx, parent);
    } else if (statementCtx instanceof StatDefContext) {
      return visitDefinition(((StatDefContext) statementCtx).definition(), parent, enclosingClass);
    } else {
      return null;
    }
  }

  @Override
  public FileGroup visitStatements(StatementsContext ctx) {
    List<Group> subgroups = new ArrayList<>();
    List<ChildNamespaceCommand> namespaceCommands = new ArrayList<>();
    FileGroup parentGroup = new FileGroup(new ModuleReferable(myModule), subgroups, namespaceCommands);
    visitStatementList(ctx.statement(), subgroups, namespaceCommands, parentGroup, null);
    return parentGroup;
  }

  public ChildGroup visitDefinition(DefinitionContext ctx, ChildGroup parent, TCClassReferable enclosingClass) {
    if (ctx instanceof DefFunctionContext) {
      return visitDefFunction((DefFunctionContext) ctx, parent, enclosingClass);
    } else if (ctx instanceof DefDataContext) {
      return visitDefData((DefDataContext) ctx, parent, enclosingClass);
    } else if (ctx instanceof DefClassContext) {
      return visitDefClass((DefClassContext) ctx, parent, enclosingClass);
    } else if (ctx instanceof DefInstanceContext) {
      return visitDefInstance((DefInstanceContext) ctx, parent, enclosingClass);
    } else if (ctx instanceof DefModuleContext) {
      return visitDefModule((DefModuleContext) ctx, parent, enclosingClass);
    } else {
      if (ctx != null) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.start), "Unknown definition"));
      }
      throw new ParseException();
    }
  }

  private SimpleNamespaceCommand visitStatCmd(StatCmdContext ctx, ChildGroup parent) {
    NamespaceCommand.Kind kind = (NamespaceCommand.Kind) visit(ctx.nsCmd());
    List<String> path = visitLongNamePath(ctx.longName());

    List<SimpleNamespaceCommand.SimpleNameRenaming> openedReferences;
    NsUsingContext nsUsing = ctx.nsUsing();
    if (nsUsing == null) {
      openedReferences = Collections.emptyList();
    } else {
      openedReferences = new ArrayList<>();
      for (NsIdContext nsIdCtx : nsUsing.nsId()) {
        Position position = tokenPosition(nsIdCtx.ID(0).getSymbol());
        openedReferences.add(new SimpleNamespaceCommand.SimpleNameRenaming(position,
          new NamedUnresolvedReference(position, nsIdCtx.ID(0).getText()),
          nsIdCtx.precedence() == null ? null : visitPrecedence(nsIdCtx.precedence()),
          nsIdCtx.ID().size() < 2 ? null : nsIdCtx.ID(1).getText()));
      }
    }

    List<Referable> hiddenReferences = new ArrayList<>();
    for (TerminalNode id : ctx.ID()) {
      hiddenReferences.add(new NamedUnresolvedReference(tokenPosition(id.getSymbol()), id.getText()));
    }

    return new SimpleNamespaceCommand(tokenPosition(ctx.start), kind, path, nsUsing == null || nsUsing.USING() != null, openedReferences, hiddenReferences, parent);
  }

  @Override
  public NamespaceCommand.Kind visitOpenCmd(OpenCmdContext ctx) {
    return NamespaceCommand.Kind.OPEN;
  }

  @Override
  public NamespaceCommand.Kind visitImportCmd(ImportCmdContext ctx) {
    return NamespaceCommand.Kind.IMPORT;
  }

  private Precedence visitPrecedence(PrecedenceContext ctx) {
    return ctx == null ? Precedence.DEFAULT : (Precedence) visit(ctx);
  }

  @Override
  public Precedence visitNoPrecedence(NoPrecedenceContext ctx) {
    return Precedence.DEFAULT;
  }

  @Override
  public Precedence visitWithPrecedence(WithPrecedenceContext ctx) {
    int priority = Integer.parseInt(ctx.NUMBER().getText());
    if (priority < 0 || priority > Precedence.MAX_PRIORITY) {
      priority = Precedence.MAX_PRIORITY + 1;
    }

    PrecedenceWithoutPriority prec = (PrecedenceWithoutPriority) visit(ctx.associativity());
    return new Precedence(prec.associativity, (byte) priority, prec.isInfix);
  }

  private static class PrecedenceWithoutPriority {
    private Precedence.Associativity associativity;
    private boolean isInfix;

    private PrecedenceWithoutPriority(Precedence.Associativity associativity, boolean isInfix) {
      this.associativity = associativity;
      this.isInfix = isInfix;
    }
  }

  @Override
  public PrecedenceWithoutPriority visitNonAssocInfix(NonAssocInfixContext ctx) {
    return new PrecedenceWithoutPriority(Precedence.Associativity.NON_ASSOC, true);
  }

  @Override
  public PrecedenceWithoutPriority visitLeftAssocInfix(LeftAssocInfixContext ctx) {
    return new PrecedenceWithoutPriority(Precedence.Associativity.LEFT_ASSOC, true);
  }

  @Override
  public PrecedenceWithoutPriority visitRightAssocInfix(RightAssocInfixContext ctx) {
    return new PrecedenceWithoutPriority(Precedence.Associativity.RIGHT_ASSOC, true);
  }

  @Override
  public PrecedenceWithoutPriority visitNonAssoc(NonAssocContext ctx) {
    return new PrecedenceWithoutPriority(Precedence.Associativity.NON_ASSOC, false);
  }

  @Override
  public PrecedenceWithoutPriority visitLeftAssoc(LeftAssocContext ctx) {
    return new PrecedenceWithoutPriority(Precedence.Associativity.LEFT_ASSOC, false);
  }

  @Override
  public PrecedenceWithoutPriority visitRightAssoc(RightAssocContext ctx) {
    return new PrecedenceWithoutPriority(Precedence.Associativity.RIGHT_ASSOC, false);
  }

  private Concrete.Pattern visitPattern(PatternContext ctx) {
    return (Concrete.Pattern) visit(ctx);
  }

  @Override
  public Concrete.Pattern visitPatternAtom(PatternAtomContext ctx) {
    Concrete.Pattern pattern = (Concrete.Pattern) visit(ctx.atomPattern());
    TerminalNode id = ctx.ID();
    if (id == null) {
      return pattern;
    }

    ExprContext type = ctx.expr();
    Position position = tokenPosition(id.getSymbol());
    Concrete.TypedReferable typedRef = new Concrete.TypedReferable(position, new ParsedLocalReferable(position, id.getText()), type == null ? null : visitExpr(type));

    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      Referable referable = namePattern.getReferable();
      if (namePattern.type != null || !(referable instanceof ParsedLocalReferable)) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.AS().getSymbol()), "As-patterns are not allowed for variables"));
        return pattern;
      }
      return new Concrete.ConstructorPattern(namePattern.getData(), namePattern.isExplicit(), new NamedUnresolvedReference(((ParsedLocalReferable) referable).getPosition(), referable.textRepresentation()), Collections.emptyList(), Collections.singletonList(typedRef));
    }

    if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
      List<Concrete.TypedReferable> asRefs = new ArrayList<>(conPattern.getAsReferables());
      asRefs.add(typedRef);
      return new Concrete.ConstructorPattern(conPattern.getData(), conPattern.isExplicit(), conPattern.getConstructor(), conPattern.getPatterns(), asRefs);
    }

    if (pattern instanceof Concrete.TuplePattern) {
      Concrete.TuplePattern tuplePattern = (Concrete.TuplePattern) pattern;
      List<Concrete.TypedReferable> asRefs = new ArrayList<>(tuplePattern.getAsReferables());
      asRefs.add(typedRef);
      return new Concrete.TuplePattern(tuplePattern.getData(), tuplePattern.isExplicit(), tuplePattern.getPatterns(), asRefs);
    }

    if (pattern instanceof Concrete.NumberPattern) {
      Concrete.NumberPattern numberPattern = (Concrete.NumberPattern) pattern;
      List<Concrete.TypedReferable> asRefs = new ArrayList<>(numberPattern.getAsReferables());
      asRefs.add(typedRef);
      return new Concrete.NumberPattern(numberPattern.getData(), numberPattern.getNumber(), asRefs);
    }

    throw new IllegalStateException();
  }

  @Override
  public Concrete.Pattern visitPatternConstructor(PatternConstructorContext ctx) {
    List<AtomPatternOrIDContext> atomPatternOrIDs = ctx.atomPatternOrID();
    Position position = tokenPosition(ctx.start);
    List<String> longName = visitLongNamePath(ctx.longName());
    ExprContext typeCtx = ctx.expr();
    TerminalNode id = ctx.ID();

    if (atomPatternOrIDs.isEmpty() && longName.size() == 1 && id == null) {
      return new Concrete.NamePattern(position, true, new ParsedLocalReferable(position, longName.get(0)), typeCtx == null ? null : visitExpr(typeCtx));
    } else {
      if (typeCtx != null && id == null) {
        myErrorReporter.report(new ParserError(tokenPosition(typeCtx.start), "Type annotation is allowed only for variables"));
      }
      List<Concrete.Pattern> patterns = new ArrayList<>(atomPatternOrIDs.size());
      for (AtomPatternOrIDContext atomCtx : atomPatternOrIDs) {
        patterns.add(visitAtomPattern(atomCtx));
      }

      Position pos = id == null ? null : tokenPosition(id.getSymbol());
      return new Concrete.ConstructorPattern(position, LongUnresolvedReference.make(position, longName), patterns,
        pos == null ? Collections.emptyList() : Collections.singletonList(new Concrete.TypedReferable(pos, new ParsedLocalReferable(pos, id.getText()), typeCtx == null ? null : visitExpr(typeCtx))));
    }
  }

  private Concrete.Pattern visitAtomPattern(AtomPatternOrIDContext ctx) {
    return (Concrete.Pattern) visit(ctx);
  }

  @Override
  public Concrete.Pattern visitPatternExplicit(PatternExplicitContext ctx) {
    List<PatternContext> patternCtxs = ctx.pattern();
    if (patternCtxs.isEmpty()) {
      return new Concrete.TuplePattern(tokenPosition(ctx.start), Collections.emptyList(), Collections.emptyList());
    }
    if (patternCtxs.size() == 1) {
      return visitPattern(patternCtxs.get(0));
    }

    List<Concrete.Pattern> patterns = new ArrayList<>(patternCtxs.size());
    for (PatternContext patternCtx : patternCtxs) {
      patterns.add(visitPattern(patternCtx));
    }
    return new Concrete.TuplePattern(tokenPosition(ctx.start), patterns, Collections.emptyList());
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
  public Concrete.Pattern visitPatternOrIDAtom(PatternOrIDAtomContext ctx) {
    return (Concrete.Pattern) visit(ctx.atomPattern());
  }

  @Override
  public Concrete.Pattern visitPatternID(PatternIDContext ctx) {
    Position position = tokenPosition(ctx.start);
    List<String> longName = visitLongNamePath(ctx.longName());
    return longName.size() == 1
      ? new Concrete.NamePattern(position, true, new ParsedLocalReferable(position, longName.get(0)), null)
      : new Concrete.ConstructorPattern(position, true, LongUnresolvedReference.make(position, longName), Collections.emptyList(), Collections.emptyList());
  }

  @Override
  public Concrete.Pattern visitPatternNumber(PatternNumberContext ctx) {
    String text = ctx.NUMBER().getText();
    int value;
    if (text.length() >= 10) {
      value = Concrete.NumberPattern.MAX_VALUE;
    } else {
      value = Integer.parseInt(ctx.NUMBER().getText(), 10);
    }

    return new Concrete.NumberPattern(tokenPosition(ctx.start), value, Collections.emptyList());
  }

  @Override
  public Concrete.Pattern visitPatternNegativeNumber(PatternNegativeNumberContext ctx) {
    String text = ctx.NEGATIVE_NUMBER().getText();
    int value;
    if (text.length() >= 9) {
      value = -Concrete.NumberPattern.MAX_VALUE;
    } else {
      value = Integer.parseInt(ctx.NEGATIVE_NUMBER().getText(), 10);
    }

    return new Concrete.NumberPattern(tokenPosition(ctx.start), value, Collections.emptyList());
  }

  @Override
  public Concrete.Pattern visitPatternAny(PatternAnyContext ctx) {
    return new Concrete.NamePattern(tokenPosition(ctx.start), true, null, null);
  }

  private ConcreteLocatedReferable makeReferable(Position position, String name, Precedence precedence, ChildGroup parent, GlobalReferable.Kind kind) {
    return parent instanceof FileGroup
      ? new ConcreteLocatedReferable(position, name, precedence, myModule, kind)
      : new ConcreteLocatedReferable(position, name, precedence, (TCReferable) parent.getReferable(), kind);
  }

  private StaticGroup visitDefInstance(DefInstanceContext ctx, ChildGroup parent, TCClassReferable enclosingClass) {
    boolean isInstance = ctx.instanceKw() instanceof FuncKwInstanceContext;
    List<Concrete.TelescopeParameter> parameters = visitFunctionParameters(ctx.tele());
    ConcreteLocatedReferable reference = makeReferable(tokenPosition(ctx.start), ctx.ID().getText(), Precedence.DEFAULT, parent, isInstance ? LocatedReferableImpl.Kind.TYPECHECKABLE : GlobalReferable.Kind.DEFINED_CONSTRUCTOR);
    Pair<Concrete.Expression,Concrete.Expression> returnPair = visitReturnExpr(ctx.returnExpr());

    Concrete.FunctionBody body;
    InstanceBodyContext bodyCtx = ctx.instanceBody();
    if (bodyCtx instanceof InstanceWithElimContext) {
      InstanceWithElimContext elimCtx = (InstanceWithElimContext) bodyCtx;
      body = new Concrete.ElimFunctionBody(tokenPosition(elimCtx.start), visitElim(elimCtx.elim()), visitClauses(elimCtx.clauses()));
    } else if (bodyCtx instanceof InstanceWithoutElimContext) {
      body = new Concrete.TermFunctionBody(tokenPosition(ctx.start), visitExpr(((InstanceWithoutElimContext) bodyCtx).expr()));
    } else if (bodyCtx instanceof InstanceCowithElimContext) {
      body = new Concrete.CoelimFunctionBody(tokenPosition(bodyCtx.start), visitCoClauses(((InstanceCowithElimContext) bodyCtx).coClauses()));
    } else if (bodyCtx instanceof InstanceCoclausesContext) {
      body = new Concrete.CoelimFunctionBody(tokenPosition(bodyCtx.start), visitCoClauses(((InstanceCoclausesContext) bodyCtx).coClause()));
    } else {
      throw new IllegalStateException();
    }

    Concrete.FunctionDefinition funcDef = new Concrete.FunctionDefinition(isInstance ? FunctionKind.INSTANCE : FunctionKind.CONS, reference, parameters, returnPair.proj1, returnPair.proj2, body);
    funcDef.enclosingClass = enclosingClass;
    reference.setDefinition(funcDef);
    List<Group> subgroups = new ArrayList<>();
    List<ChildNamespaceCommand> namespaceCommands = new ArrayList<>();
    StaticGroup resultGroup = new StaticGroup(reference, subgroups, namespaceCommands, parent);
    visitWhere(ctx.where(), subgroups, namespaceCommands, resultGroup, enclosingClass);
    return resultGroup;
  }

  private List<Concrete.ClassFieldImpl> visitCoClauses(CoClausesContext ctx) {
    if (ctx instanceof CoClausesWithBracesContext) {
      return visitCoClausesWithBraces((CoClausesWithBracesContext) ctx);
    }
    if (ctx instanceof CoClausesWithoutBracesContext) {
      return visitCoClausesWithoutBraces((CoClausesWithoutBracesContext) ctx);
    }
    throw new IllegalStateException();
  }

  private List<Concrete.ClassFieldImpl> visitCoClauses(List<CoClauseContext> coClausesCtx) {
    List<Concrete.ClassFieldImpl> coClauses = new ArrayList<>(coClausesCtx.size());
    for (CoClauseContext coClause : coClausesCtx) {
      Concrete.ClassFieldImpl impl = visitCoClause(coClause);
      if (impl != null) {
        coClauses.add(impl);
      }
    }
    return coClauses;
  }

  @Override
  public List<Concrete.ClassFieldImpl> visitCoClausesWithoutBraces(CoClausesWithoutBracesContext ctx) {
    return visitCoClauses(ctx.coClause());
  }

  @Override
  public List<Concrete.ClassFieldImpl> visitCoClausesWithBraces(CoClausesWithBracesContext ctx) {
    return visitCoClauses(ctx.coClause());
  }

  private void visitWhere(WhereContext ctx, List<Group> subgroups, List<ChildNamespaceCommand> namespaceCommands, ChildGroup parent, TCClassReferable enclosingClass) {
    if (ctx != null) {
      visitStatementList(ctx.statement(), subgroups, namespaceCommands, parent, enclosingClass);
    }
  }

  @Override
  public List<Concrete.ReferenceExpression> visitElim(ElimContext ctx) {
    if (ctx == null) {
      return Collections.emptyList();
    }
    List<TerminalNode> ids = ctx.ID();
    if (ids != null && !ids.isEmpty()) {
      List<Concrete.Expression> expressions = new ArrayList<>(ids.size());
      for (TerminalNode id : ids) {
        Position position = tokenPosition(id.getSymbol());
        expressions.add(new Concrete.ReferenceExpression(position, new NamedUnresolvedReference(position, id.getText())));
      }
      return checkElimExpressions(expressions);
    } else {
      return Collections.emptyList();
    }
  }

  private Pair<Concrete.Expression,Concrete.Expression> visitReturnExpr(ReturnExprContext returnExprCtx) {
    Concrete.Expression resultType = null;
    Concrete.Expression resultTypeLevel = null;
    if (returnExprCtx instanceof ReturnExprExprContext) {
      resultType = visitExpr(((ReturnExprExprContext) returnExprCtx).expr());
    } else if (returnExprCtx instanceof ReturnExprLevelContext) {
      resultType = visitAtomFieldsAcc(((ReturnExprLevelContext) returnExprCtx).atomFieldsAcc(0));
      resultTypeLevel = visitAtomFieldsAcc(((ReturnExprLevelContext) returnExprCtx).atomFieldsAcc(1));
    }
    return new Pair<>(resultType, resultTypeLevel);
  }

  private StaticGroup visitDefFunction(DefFunctionContext ctx, ChildGroup parent, TCClassReferable enclosingClass) {
    Concrete.FunctionBody body;
    FunctionBodyContext functionBodyCtx = ctx.functionBody();
    if (functionBodyCtx instanceof WithElimContext) {
      WithElimContext elimCtx = (WithElimContext) functionBodyCtx;
      body = new Concrete.ElimFunctionBody(tokenPosition(elimCtx.start), visitElim(elimCtx.elim()), visitClauses(elimCtx.clauses()));
    } else if (functionBodyCtx instanceof CowithElimContext) {
      CowithElimContext elimCtx = (CowithElimContext) functionBodyCtx;
      body = new Concrete.CoelimFunctionBody(tokenPosition(elimCtx.start), visitCoClauses(elimCtx.coClauses()));
    } else {
      body = new Concrete.TermFunctionBody(tokenPosition(ctx.start), visitExpr(((WithoutElimContext) functionBodyCtx).expr()));
    }

    List<Group> subgroups = new ArrayList<>();
    List<ChildNamespaceCommand> namespaceCommands = new ArrayList<>();
    FuncKwContext funcKw = ctx.funcKw();
    ConcreteLocatedReferable referable = makeReferable(tokenPosition(ctx.start), ctx.ID().getText(), visitPrecedence(ctx.precedence()), parent, GlobalReferable.Kind.TYPECHECKABLE);
    boolean isUse = funcKw instanceof FuncKwUseContext;
    Pair<Concrete.Expression,Concrete.Expression> returnPair = visitReturnExpr(ctx.returnExpr());
    Concrete.FunctionDefinition funDef = Concrete.UseDefinition.make(
      isUse ? (((FuncKwUseContext) funcKw).useMod() instanceof UseCoerceContext
              ? FunctionKind.COERCE
              : FunctionKind.LEVEL)
            : funcKw instanceof FuncKwLemmaContext
              ? FunctionKind.LEMMA
              : funcKw instanceof FuncKwSFuncContext
                ? FunctionKind.SFUNC
                : FunctionKind.FUNC,
      referable, visitFunctionParameters(ctx.tele()), returnPair.proj1, returnPair.proj2, body, parent.getReferable());
    if (isUse && !funDef.getKind().isUse()) {
      myErrorReporter.report(new ParserError(tokenPosition(ctx.funcKw().start), "\\use is not allowed on the top level"));
    }

    funDef.enclosingClass = enclosingClass;
    referable.setDefinition(funDef);
    StaticGroup resultGroup = new StaticGroup(referable, subgroups, namespaceCommands, parent);
    visitWhere(ctx.where(), subgroups, namespaceCommands, resultGroup, enclosingClass);

    List<TCReferable> usedDefinitions = collectUsedDefinitions(subgroups, null);
    if (usedDefinitions != null) {
      funDef.setUsedDefinitions(usedDefinitions);
    }

    return resultGroup;
  }

  private List<Concrete.TelescopeParameter> visitFunctionParameters(List<TeleContext> teleCtx) {
    List<Concrete.TelescopeParameter> arguments = new ArrayList<>();
    for (TeleContext tele : teleCtx) {
      List<Concrete.Parameter> args = visitLamTele(tele);
      if (args.get(0) instanceof Concrete.TelescopeParameter) {
        arguments.add((Concrete.TelescopeParameter) args.get(0));
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(tele.getStart()), "Expected a typed variable"));
      }
    }
    return arguments;
  }

  private StaticGroup visitDefData(DefDataContext ctx, ChildGroup parent, TCClassReferable enclosingClass) {
    final Concrete.UniverseExpression universe;
    ExprContext exprCtx = ctx.expr();
    if (exprCtx != null) {
      Concrete.Expression expr = visitExpr(exprCtx);
      if (expr instanceof Concrete.UniverseExpression) {
        universe = (Concrete.UniverseExpression) expr;
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(exprCtx.start), "Specified type of the data definition is not a universe"));
        universe = null;
      }
    } else {
      universe = null;
    }

    List<InternalConcreteLocatedReferable> constructors = new ArrayList<>();
    DataBodyContext dataBodyCtx = ctx.dataBody();
    List<Concrete.ReferenceExpression> eliminatedReferences = dataBodyCtx instanceof DataClausesContext ? visitElim(((DataClausesContext) dataBodyCtx).elim()) : null;
    ConcreteLocatedReferable referable = makeReferable(tokenPosition(ctx.start), ctx.ID().getText(), visitPrecedence(ctx.precedence()), parent, LocatedReferableImpl.Kind.TYPECHECKABLE);
    Concrete.DataDefinition dataDefinition = new Concrete.DataDefinition(referable, visitTeles(ctx.tele()), eliminatedReferences, ctx.TRUNCATED() != null, universe, new ArrayList<>());
    dataDefinition.enclosingClass = enclosingClass;
    referable.setDefinition(dataDefinition);
    visitDataBody(dataBodyCtx, dataDefinition, constructors);

    List<Group> subgroups = new ArrayList<>();
    List<ChildNamespaceCommand> namespaceCommands = new ArrayList<>();
    DataGroup resultGroup = new DataGroup(referable, constructors, subgroups, namespaceCommands, parent);
    visitWhere(ctx.where(), subgroups, namespaceCommands, resultGroup, enclosingClass);

    List<TCReferable> usedDefinitions = collectUsedDefinitions(subgroups, null);
    if (usedDefinitions != null) {
      dataDefinition.setUsedDefinitions(usedDefinitions);
    }

    return resultGroup;
  }

  private List<TCReferable> collectUsedDefinitions(List<Group> groups, List<TCReferable> usedDefinitions) {
    for (Group subgroup : groups) {
      if (subgroup.getReferable() instanceof ConcreteLocatedReferable) {
        Concrete.ReferableDefinition def = ((ConcreteLocatedReferable) subgroup.getReferable()).getDefinition();
        if (def instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) def).getKind().isUse()) {
          if (usedDefinitions == null) {
            usedDefinitions = new ArrayList<>();
          }
          usedDefinitions.add((TCReferable) subgroup.getReferable());
        }
      }
    }
    return usedDefinitions;
  }

  private void visitDataBody(DataBodyContext ctx, Concrete.DataDefinition def, List<InternalConcreteLocatedReferable> constructors) {
    if (ctx instanceof DataClausesContext) {
      ConstructorClausesContext conClauses = ((DataClausesContext) ctx).constructorClauses();
      List<ConstructorClauseContext> clauseCtxList;
      if (conClauses instanceof ConClausesWithBracesContext) {
        clauseCtxList = ((ConClausesWithBracesContext) conClauses).constructorClause();
      } else if (conClauses instanceof ConClausesWithoutBracesContext) {
        clauseCtxList = ((ConClausesWithoutBracesContext) conClauses).constructorClause();
      } else {
        clauseCtxList = Collections.emptyList();
      }

      for (ConstructorClauseContext clauseCtx : clauseCtxList) {
        try {
          List<Concrete.Pattern> patterns = new ArrayList<>(clauseCtx.pattern().size());
          for (PatternContext patternCtx : clauseCtx.pattern()) {
            patterns.add(visitPattern(patternCtx));
          }
          def.getConstructorClauses().add(new Concrete.ConstructorClause(tokenPosition(clauseCtx.start), patterns, visitConstructors(clauseCtx.constructor(), def, constructors)));
        } catch (ParseException ignored) {

        }
      }
    } else if (ctx instanceof DataConstructorsContext) {
      def.getConstructorClauses().add(new Concrete.ConstructorClause(tokenPosition(ctx.start), null, visitConstructors(((DataConstructorsContext) ctx).constructor(), def, constructors)));
    }
  }

  private List<Concrete.Constructor> visitConstructors(List<ConstructorContext> conContexts, Concrete.DataDefinition def, List<InternalConcreteLocatedReferable> constructors) {
    List<Concrete.Constructor> result = new ArrayList<>(conContexts.size());
    for (ConstructorContext conCtx : conContexts) {
      try {
        List<Concrete.FunctionClause> clauses = new ArrayList<>();
        List<ClauseContext> clauseCtxs = conCtx.clause();
        ElimContext elimCtx = conCtx.elim();
        if (elimCtx != null || !clauseCtxs.isEmpty()) {
          for (ClauseContext clauseCtx : clauseCtxs) {
            clauses.add(visitClause(clauseCtx));
          }
        }

        InternalConcreteLocatedReferable reference = new InternalConcreteLocatedReferable(tokenPosition(conCtx.start), conCtx.ID().getText(), visitPrecedence(conCtx.precedence()), true, def.getData(), LocatedReferableImpl.Kind.CONSTRUCTOR);
        Concrete.Constructor constructor = new Concrete.Constructor(reference, def, visitTeles(conCtx.tele()), visitElim(elimCtx), clauses);
        reference.setDefinition(constructor);
        /* TODO[hits]
        ExprContext type = conCtx.expr();
        if (type != null) {
          constructor.setResultType(visitExpr(type));
        }
        */
        constructors.add(reference);
        result.add(constructor);
      } catch (ParseException ignored) {

      }
    }
    return result;
  }

  @Override
  public ClassFieldKind visitFieldPipe(FieldPipeContext ctx) {
    return ClassFieldKind.ANY;
  }

  @Override
  public ClassFieldKind visitFieldField(FieldFieldContext ctx) {
    return ClassFieldKind.FIELD;
  }

  @Override
  public ClassFieldKind visitFieldProperty(FieldPropertyContext ctx) {
    return ClassFieldKind.PROPERTY;
  }

  private void visitInstanceStatement(ClassFieldOrImplContext ctx, List<Concrete.ClassField> fields, List<Concrete.ClassFieldImpl> implementations, Concrete.ClassDefinition parentClass) {
    if (ctx instanceof ClassFieldContext) {
      ClassFieldContext fieldCtx = (ClassFieldContext) ctx;
      List<TeleContext> teleCtxs = fieldCtx.tele();
      List<Concrete.TypeParameter> parameters = visitTeles(teleCtxs);
      Pair<Concrete.Expression,Concrete.Expression> returnPair = visitReturnExpr(fieldCtx.returnExpr());
      ConcreteClassFieldReferable reference = new ConcreteClassFieldReferable(tokenPosition(fieldCtx.start), fieldCtx.ID().getText(), visitPrecedence(fieldCtx.precedence()), true, true, false, parentClass.getData(), LocatedReferableImpl.Kind.FIELD);
      Concrete.ClassField field = new Concrete.ClassField(reference, parentClass, true, (ClassFieldKind) visit(fieldCtx.fieldMod()), parameters, returnPair.proj1, returnPair.proj2);
      reference.setDefinition(field);
      fields.add(field);
    } else if (ctx instanceof ClassImplContext) {
      Concrete.ClassFieldImpl impl = visitClassImpl((ClassImplContext) ctx);
      if (impl != null) {
        implementations.add(impl);
      }
    }
  }

  private void visitInstanceStatements(List<ClassFieldOrImplContext> ctx, List<Concrete.ClassField> fields, List<Concrete.ClassFieldImpl> implementations, Concrete.ClassDefinition parentClass) {
    for (ClassFieldOrImplContext statCtx : ctx) {
      if (statCtx != null) {
        try {
          visitInstanceStatement(statCtx, fields, implementations, parentClass);
        } catch (ParseException ignored) {

        }
      }
    }
  }

  private void visitInstanceStatements(List<ClassStatContext> ctx, List<Concrete.ClassField> fields, List<Concrete.ClassFieldImpl> implementations, List<Group> subgroups, Concrete.ClassDefinition parentClass, ChildGroup parent) {
    for (ClassStatContext statementCtx : ctx) {
      if (statementCtx == null) {
        continue;
      }

      try {
        if (statementCtx instanceof ClassFieldOrImplStatContext) {
          visitInstanceStatement(((ClassFieldOrImplStatContext) statementCtx).classFieldOrImpl(), fields, implementations, parentClass);
        } else if (statementCtx instanceof ClassDefinitionStatContext) {
          subgroups.add(visitDefinition(((ClassDefinitionStatContext) statementCtx).definition(), parent, parentClass.getData()));
        } else {
          myErrorReporter.report(new ParserError(tokenPosition(statementCtx.start), "Unknown class statement"));
        }
      } catch (ParseException ignored) {

      }
    }
  }

  private StaticGroup visitDefModule(DefModuleContext ctx, ChildGroup parent, TCClassReferable enclosingClass) {
    WhereContext where = ctx.where();
    List<Group> staticSubgroups = where == null ? Collections.emptyList() : new ArrayList<>();
    List<ChildNamespaceCommand> namespaceCommands = where == null ? Collections.emptyList() : new ArrayList<>();

    Position position = tokenPosition(ctx.start);
    String name = ctx.ID().getText();
    ConcreteLocatedReferable reference = parent instanceof FileGroup
        ? new ConcreteLocatedReferable(position, name, Precedence.DEFAULT, myModule, GlobalReferable.Kind.OTHER)
        : new ConcreteLocatedReferable(position, name, Precedence.DEFAULT, (TCReferable) parent.getReferable(), GlobalReferable.Kind.OTHER);

    StaticGroup resultGroup = new StaticGroup(reference, staticSubgroups, namespaceCommands, parent);
    visitWhere(where, staticSubgroups, namespaceCommands, resultGroup, enclosingClass);
    return resultGroup;
  }

  private ClassGroup visitDefClass(DefClassContext ctx, ChildGroup parent, TCClassReferable enclosingClass) {
    WhereContext where = ctx.where();

    List<Concrete.ClassFieldImpl> implementations = Collections.emptyList();
    List<Group> staticSubgroups = where == null ? Collections.emptyList() : new ArrayList<>();
    List<ChildNamespaceCommand> namespaceCommands = where == null ? Collections.emptyList() : new ArrayList<>();

    List<Concrete.ReferenceExpression> superClasses = new ArrayList<>();
    for (LongNameContext longNameCtx : ctx.longName()) {
      superClasses.add(visitLongNameRef(longNameCtx, null, null));
    }

    List<? extends InternalConcreteLocatedReferable> fieldReferables;
    Position pos = tokenPosition(ctx.start);
    String name = ctx.ID().getText();
    Precedence prec = visitPrecedence(ctx.precedence());
    ConcreteClassReferable reference;
    ClassGroup resultGroup = null;
    boolean isRecord = ctx.classKw() instanceof ClassKwRecordContext;
    ClassBodyContext classBodyCtx = ctx.classBody();
    List<Concrete.ClassField> fields = new ArrayList<>();
    List<ClassStatContext> classStatCtxs = classBodyCtx instanceof ClassBodyStatsContext ? ((ClassBodyStatsContext) classBodyCtx).classStat() : Collections.emptyList();
    List<ClassFieldOrImplContext> classFieldOrImplCtxs = classBodyCtx instanceof ClassBodyFieldOrImplContext ? ((ClassBodyFieldOrImplContext) classBodyCtx).classFieldOrImpl() : Collections.emptyList();
    if (!classStatCtxs.isEmpty() || !classFieldOrImplCtxs.isEmpty()) {
      implementations = new ArrayList<>();
    }

    List<ConcreteClassFieldReferable> fieldReferables1 = new ArrayList<>();
    reference = parent instanceof FileGroup
      ? new ConcreteClassReferable(pos, name, prec, fieldReferables1, superClasses, parent, myModule)
      : new ConcreteClassReferable(pos, name, prec, fieldReferables1, superClasses, parent, (TCReferable) parent.getReferable());

    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(reference, isRecord, new ArrayList<>(superClasses), fields, implementations);
    reference.setDefinition(classDefinition);
    visitFieldTeles(ctx.fieldTele(), classDefinition, fields);

    List<TCReferable> usedDefinitions = null;
    if (!classStatCtxs.isEmpty()) {
      List<Group> dynamicSubgroups = new ArrayList<>();
      resultGroup = new ClassGroup(reference, fieldReferables1, dynamicSubgroups, staticSubgroups, namespaceCommands, parent);
      visitInstanceStatements(classStatCtxs, fields, implementations, dynamicSubgroups, classDefinition, resultGroup);
      usedDefinitions = collectUsedDefinitions(dynamicSubgroups, null);
    }
    visitInstanceStatements(classFieldOrImplCtxs, fields, implementations, classDefinition);

    for (Concrete.ClassField field : fields) {
      fieldReferables1.add((ConcreteClassFieldReferable) field.getData());
    }
    fieldReferables = fieldReferables1;

    if (resultGroup == null) {
      resultGroup = new ClassGroup(reference, fieldReferables, Collections.emptyList(), staticSubgroups, namespaceCommands, parent);
    }
    visitWhere(where, staticSubgroups, namespaceCommands, resultGroup, enclosingClass);

    usedDefinitions = collectUsedDefinitions(staticSubgroups, usedDefinitions);
    if (usedDefinitions != null) {
      classDefinition.setUsedDefinitions(usedDefinitions);
    }

    return resultGroup;
  }

  @Override
  public Concrete.ClassFieldImpl visitClassImpl(ClassImplContext ctx) {
    return visitCoClause(ctx.coClause());
  }

  @Override
  public Concrete.ReferenceExpression visitName(NameContext ctx) {
    TerminalNode infixCtx = ctx.INFIX();
    TerminalNode postfixCtx = infixCtx == null ? ctx.POSTFIX() : null;
    return visitLongNameRef(ctx.longName(), infixCtx != null ? getInfixText(infixCtx) : postfixCtx != null ? getPostfixText(postfixCtx) : null, infixCtx != null ? Fixity.INFIX : postfixCtx != null ? Fixity.POSTFIX : null);
  }

  @Override
  public Concrete.HoleExpression visitUnknown(UnknownContext ctx) {
    return new Concrete.HoleExpression(tokenPosition(ctx.start));
  }

  @Override
  public Concrete.FixityReferenceExpression visitInfix(InfixContext ctx) {
    Position position = tokenPosition(ctx.start);
    return new Concrete.FixityReferenceExpression(position, new NamedUnresolvedReference(position, getInfixText(ctx.INFIX())), Fixity.INFIX);
  }

  @Override
  public Concrete.FixityReferenceExpression visitPostfix(PostfixContext ctx) {
    Position position = tokenPosition(ctx.start);
    return new Concrete.FixityReferenceExpression(position, new NamedUnresolvedReference(position, getPostfixText(ctx.POSTFIX())), Fixity.POSTFIX);
  }

  @Override
  public Concrete.GoalExpression visitGoal(GoalContext ctx) {
    TerminalNode id = ctx.ID();
    ExprContext exprCtx = ctx.expr();
    return new Concrete.GoalExpression(tokenPosition(ctx.start), id == null ? null : id.getText(), exprCtx == null ? null : visitExpr(exprCtx));
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    Concrete.Expression domain = visitExpr(ctx.expr(0));
    Concrete.Expression codomain = visitExpr(ctx.expr(1));
    List<Concrete.TypeParameter> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeParameter(domain.getData(), true, domain));
    return new Concrete.PiExpression(tokenPosition(ctx.getToken(ARROW, 0).getSymbol()), arguments, codomain);
  }

  @Override
  public Concrete.Expression visitTupleExpr(TupleExprContext ctx) {
    List<ExprContext> exprs = ctx.expr();
    Concrete.Expression expr = visitExpr(exprs.get(0));
    return exprs.size() > 1 ? new Concrete.TypedExpression(expr.getData(), expr, visitExpr(exprs.get(1))) : expr;
  }

  @Override
  public Concrete.Expression visitTuple(TupleContext ctx) {
    List<TupleExprContext> exprs = ctx.tupleExpr();
    if (exprs.size() == 1) {
      return visitTupleExpr(exprs.get(0));
    } else {
      List<Concrete.Expression> fields = new ArrayList<>(exprs.size());
      for (TupleExprContext exprCtx : exprs) {
        fields.add(visitTupleExpr(exprCtx));
      }
      return new Concrete.TupleExpression(tokenPosition(ctx.start), fields);
    }
  }

  private List<Concrete.Parameter> visitLamTele(TeleContext tele) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    if (tele instanceof ExplicitContext || tele instanceof ImplicitContext) {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      Concrete.Expression typeExpr = null;
      List<ParsedLocalReferable> vars = new ArrayList<>();
      if (typedExpr instanceof TypedContext) {
        getVarList(((TypedContext) typedExpr).expr(0), vars);
        typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
      } else if (typedExpr instanceof NotTypedContext) {
        getVarList(((NotTypedContext) typedExpr).expr(), vars);
        for (ParsedLocalReferable var : vars) {
          parameters.add(new Concrete.NameParameter(var.getPosition(), explicit, var));
        }
      } else {
        throw new IllegalStateException();
      }
      if (typeExpr != null) {
        parameters.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, vars, typeExpr));
      }
    } else {
      boolean ok = tele instanceof TeleLiteralContext;
      if (ok) {
        LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
        if (literalContext instanceof NameContext && ((NameContext) literalContext).longName() != null) {
          List<TerminalNode> ids = ((NameContext) literalContext).longName().ID();
          if (ids.size() == 1) {
            Position position = tokenPosition(ids.get(0).getSymbol());
            parameters.add(new Concrete.NameParameter(position, true, new ParsedLocalReferable(position, ids.get(0).getText())));
          } else {
            ok = false;
          }
        } else if (literalContext instanceof UnknownContext) {
          parameters.add(new Concrete.NameParameter(tokenPosition(literalContext.getStart()), true, null));
        } else {
          ok = false;
        }
      }
      if (!ok) {
        myErrorReporter.report(new ParserError(tokenPosition(tele.start), "Unexpected token, expected an identifier"));
        throw new ParseException();
      }
    }
    return parameters;
  }

  private List<Concrete.Parameter> visitLamTeles(List<TeleContext> tele) {
    List<Concrete.Parameter> arguments = new ArrayList<>();
    for (TeleContext arg : tele) {
      arguments.addAll(visitLamTele(arg));
    }
    return arguments;
  }

  @Override
  public Concrete.LamExpression visitLam(LamContext ctx) {
    return new Concrete.LamExpression(tokenPosition(ctx.start), visitLamTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  private Concrete.Expression visitAppExpr(AppExprContext ctx) {
    return (Concrete.Expression) visit(ctx);
  }

  @Override
  public Concrete.Expression visitAppArgument(AppArgumentContext ctx) {
    return visitArgumentAppExpr(ctx.argumentAppExpr());
  }

  @Override
  public Concrete.Expression visitArgumentAppExpr(ArgumentAppExprContext ctx) {
    Concrete.Expression expr = visitAtomFieldsAcc(ctx.atomFieldsAcc());
    List<OnlyLevelAtomContext> onlyLevelAtoms = ctx.onlyLevelAtom();
    if (!onlyLevelAtoms.isEmpty()) {
      if (expr instanceof Concrete.ReferenceExpression) {
        Object obj1 = visit(onlyLevelAtoms.get(0));
        Object obj2 = onlyLevelAtoms.size() < 2 ? null : visit(onlyLevelAtoms.get(1));
        if (onlyLevelAtoms.size() > 2 || obj1 instanceof Pair && obj2 != null || obj2 instanceof Pair) {
          myErrorReporter.report(new ParserError(tokenPosition(onlyLevelAtoms.get(0).start), "too many level specifications"));
        }

        Concrete.LevelExpression level1;
        Concrete.LevelExpression level2;
        if (obj1 instanceof Pair) {
          level1 = (Concrete.LevelExpression) ((Pair) obj1).proj1;
          level2 = (Concrete.LevelExpression) ((Pair) obj1).proj2;
        } else {
          level1 = (Concrete.LevelExpression) obj1;
          level2 = obj2 instanceof Concrete.LevelExpression ? (Concrete.LevelExpression) obj2 : null;
        }

        expr = new Concrete.ReferenceExpression(expr.getData(), ((Concrete.ReferenceExpression) expr).getReferent(), level1, level2);
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(onlyLevelAtoms.get(0).start), "Level annotations are allowed only after a reference"));
      }
    }

    List<ArgumentContext> argumentCtxs = ctx.argument();
    if (argumentCtxs.isEmpty()) {
      return expr;
    }

    List<Concrete.BinOpSequenceElem> sequence = new ArrayList<>(argumentCtxs.size());
    sequence.add(new Concrete.BinOpSequenceElem(expr));
    for (ArgumentContext argumentCtx : argumentCtxs) {
      sequence.add(visitArgument(argumentCtx));
    }

    return new Concrete.BinOpSequenceExpression(expr.getData(), sequence);
  }

  private Concrete.BinOpSequenceElem visitArgument(ArgumentContext ctx) {
    return (Concrete.BinOpSequenceElem) visit(ctx);
  }

  @Override
  public Concrete.BinOpSequenceElem visitArgumentExplicit(ArgumentExplicitContext ctx) {
    AtomFieldsAccContext atomFieldsAcc = ctx.atomFieldsAcc();
    return new Concrete.BinOpSequenceElem(visitAtomFieldsAcc(atomFieldsAcc), atomFieldsAcc.atom() instanceof AtomLiteralContext && isName(((AtomLiteralContext) atomFieldsAcc.atom()).literal()) && atomFieldsAcc.NUMBER().isEmpty() ? Fixity.UNKNOWN : Fixity.NONFIX, true);
  }

  private boolean isName(LiteralContext ctx) {
    return ctx instanceof NameContext || ctx instanceof InfixContext || ctx instanceof PostfixContext;
  }

  @Override
  public Concrete.BinOpSequenceElem visitArgumentNew(ArgumentNewContext ctx) {
    return new Concrete.BinOpSequenceElem(visitNew(ctx.appPrefix(), ctx.appExpr(), ctx.implementStatements()), Fixity.NONFIX, true);
  }

  @Override
  public Concrete.BinOpSequenceElem visitArgumentUniverse(ArgumentUniverseContext ctx) {
    return new Concrete.BinOpSequenceElem(visitExpr(ctx.universeAtom()), Fixity.NONFIX, true);
  }

  @Override
  public Concrete.BinOpSequenceElem visitArgumentImplicit(ArgumentImplicitContext ctx) {
    return new Concrete.BinOpSequenceElem(visitExpr(ctx.expr()), Fixity.NONFIX, false);
  }

  @Override
  public Concrete.ClassFieldImpl visitCoClause(CoClauseContext ctx) {
    List<String> path = visitLongNamePath(ctx.longName());
    Position position = tokenPosition(ctx.start);
    List<TeleContext> teleCtxs = ctx.tele();
    List<Concrete.Parameter> parameters = visitLamTeles(teleCtxs);
    Concrete.Expression term = null;
    List<Concrete.ClassFieldImpl> subClassFieldImpls = Collections.emptyList();
    ExprContext exprCtx = ctx.expr();
    if (exprCtx != null) {
      term = visitExpr(exprCtx);
      if (!parameters.isEmpty()) {
        term = new Concrete.LamExpression(tokenPosition(teleCtxs.get(0).start), parameters, term);
      }
    } else {
      if (!parameters.isEmpty()) {
        myErrorReporter.report(new ParserError(tokenPosition(teleCtxs.get(0).start), "Parameters are allowed only before '=> <expression>'"));
      }
      subClassFieldImpls = visitCoClauses(ctx.coClause());
    }

    return new Concrete.ClassFieldImpl(position, LongUnresolvedReference.make(position, path), term, subClassFieldImpls);
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
    Position position = tokenPosition(ctx.start);
    Concrete.LevelExpression lp;
    Concrete.LevelExpression lh;

    String text = ctx.UNIVERSE().getText().substring("\\Type".length());
    lp = text.isEmpty() ? null : new Concrete.NumberLevelExpression(position, Integer.parseInt(text));

    List<MaybeLevelAtomContext> maybeLevelAtomCtxs = ctx.maybeLevelAtom();
    if (!maybeLevelAtomCtxs.isEmpty()) {
      if (lp == null) {
        lp = visitLevel(maybeLevelAtomCtxs.get(0));
        lh = null;
      } else {
        lh = visitLevel(maybeLevelAtomCtxs.get(0));
      }

      if (maybeLevelAtomCtxs.size() >= 2) {
        if (lh == null) {
          lh = visitLevel(maybeLevelAtomCtxs.get(1));
        } else {
          myErrorReporter.report(new ParserError(tokenPosition(maybeLevelAtomCtxs.get(1).start), "h-level is already specified"));
        }
      }
    } else {
      lh = null;
    }

    return new Concrete.UniverseExpression(position, lp, lh);
  }

  @Override
  public Concrete.UniverseExpression visitTruncatedUniverse(TruncatedUniverseContext ctx) {
    Position position = tokenPosition(ctx.start);
    String text = ctx.TRUNCATED_UNIVERSE().getText();
    text = text.substring(text.indexOf('-') + "-Type".length());
    return new Concrete.UniverseExpression(position, getLevelExpression(position, text, ctx.maybeLevelAtom()), parseTruncatedUniverse(ctx.TRUNCATED_UNIVERSE()));
  }

  @Override
  public Concrete.UniverseExpression visitSetUniverse(SetUniverseContext ctx) {
    Position position = tokenPosition(ctx.start);
    return new Concrete.UniverseExpression(position, getLevelExpression(position, ctx.SET().getText().substring("\\Set".length()), ctx.maybeLevelAtom()), new Concrete.NumberLevelExpression(position, 0));
  }

  private Concrete.LevelExpression getLevelExpression(Position position, String text, MaybeLevelAtomContext maybeLevelAtomCtx) {
    if (text.isEmpty()) {
      return maybeLevelAtomCtx == null ? null : visitLevel(maybeLevelAtomCtx);
    }

    if (maybeLevelAtomCtx instanceof WithLevelAtomContext) {
      myErrorReporter.report(new ParserError(tokenPosition(maybeLevelAtomCtx.start), "p-level is already specified"));
    }
    return new Concrete.NumberLevelExpression(position, Integer.parseInt(text));
  }

  @Override
  public Concrete.UniverseExpression visitUniTruncatedUniverse(UniTruncatedUniverseContext ctx) {
    Position position = tokenPosition(ctx.start);
    String text = ctx.TRUNCATED_UNIVERSE().getText();
    text = text.substring(text.indexOf('-') + "-Type".length());
    Concrete.LevelExpression pLevel = text.isEmpty() ? null : new Concrete.NumberLevelExpression(position, Integer.parseInt(text));
    return new Concrete.UniverseExpression(position, pLevel, parseTruncatedUniverse(ctx.TRUNCATED_UNIVERSE()));
  }

  @Override
  public Concrete.UniverseExpression visitUniUniverse(UniUniverseContext ctx) {
    Position position = tokenPosition(ctx.start);
    String text = ctx.UNIVERSE().getText().substring("\\Type".length());
    Concrete.LevelExpression lp = text.isEmpty() ? null : new Concrete.NumberLevelExpression(position, Integer.parseInt(text));
    return new Concrete.UniverseExpression(position, lp, null);
  }

  @Override
  public Concrete.UniverseExpression visitUniSetUniverse(UniSetUniverseContext ctx) {
    Position position = tokenPosition(ctx.start);
    String text = ctx.SET().getText().substring("\\Set".length());
    Concrete.LevelExpression pLevel = text.isEmpty() ? null : new Concrete.NumberLevelExpression(position, Integer.parseInt(text));
    return new Concrete.UniverseExpression(position, pLevel, new Concrete.NumberLevelExpression(position, 0));
  }

  @Override
  public Concrete.UniverseExpression visitProp(PropContext ctx) {
    Position pos = tokenPosition(ctx.start);
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
    return new Concrete.PLevelExpression(tokenPosition(ctx.start));
  }

  @Override
  public Concrete.HLevelExpression visitHLevel(HLevelContext ctx) {
    return new Concrete.HLevelExpression(tokenPosition(ctx.start));
  }

  @Override
  public Concrete.InfLevelExpression visitInfLevel(InfLevelContext ctx) {
    return new Concrete.InfLevelExpression(tokenPosition(ctx.start));
  }

  @Override
  public Concrete.NumberLevelExpression visitNumLevel(NumLevelContext ctx) {
    return new Concrete.NumberLevelExpression(tokenPosition(ctx.start), Integer.parseInt(ctx.NUMBER().getText()));
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
  public Concrete.InfLevelExpression visitInfOnlyLevel(InfOnlyLevelContext ctx) {
    return new Concrete.InfLevelExpression(tokenPosition(ctx.start));
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
    Position position = tokenPosition(ctx.start);
    List<MaybeLevelAtomContext> maybeLevelAtomCtxs = ctx.maybeLevelAtom();
    return maybeLevelAtomCtxs.isEmpty() ? new Pair<>(new Concrete.NumberLevelExpression(position, 0), new Concrete.NumberLevelExpression(position, -1)) : new Pair<>(visitLevel(maybeLevelAtomCtxs.get(0)), visitLevel(maybeLevelAtomCtxs.get(1)));
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
    List<Concrete.TypeParameter> parameters = new ArrayList<>(teles.size());
    for (TeleContext tele : teles) {
      boolean explicit = !(tele instanceof ImplicitContext);
      TypedExprContext typedExpr;
      if (explicit) {
        if (tele instanceof ExplicitContext) {
          typedExpr = ((ExplicitContext) tele).typedExpr();
        } else
        if (tele instanceof TeleLiteralContext) {
          parameters.add(new Concrete.TypeParameter(true, visitExpr(((TeleLiteralContext) tele).literal())));
          continue;
        } else
        if (tele instanceof TeleUniverseContext) {
          parameters.add(new Concrete.TypeParameter(true, visitExpr(((TeleUniverseContext) tele).universeAtom())));
          continue;
        } else {
          throw new IllegalStateException();
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        List<ParsedLocalReferable> vars = new ArrayList<>();
        getVarList(((TypedContext) typedExpr).expr(0), vars);
        parameters.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, vars, visitExpr(((TypedContext) typedExpr).expr(1))));
      } else
      if (typedExpr instanceof NotTypedContext) {
        parameters.add(new Concrete.TypeParameter(explicit, visitExpr(((NotTypedContext) typedExpr).expr())));
      } else {
        throw new IllegalStateException();
      }
    }
    return parameters;
  }

  private void visitFieldTeles(List<FieldTeleContext> teles, Concrete.ClassDefinition classDef, List<Concrete.ClassField> fields) {
    TCFieldReferable coercingField = null;
    boolean isForced = false;

    for (FieldTeleContext tele : teles) {
      boolean explicit;
      List<TerminalNode> vars;
      ExprContext exprCtx;
      boolean forced;
      if (tele instanceof ExplicitFieldTeleContext) {
        explicit = true;
        vars = ((ExplicitFieldTeleContext) tele).ID();
        exprCtx = ((ExplicitFieldTeleContext) tele).expr();
        forced = ((ExplicitFieldTeleContext) tele).CLASSIFYING() != null;
      } else if (tele instanceof ImplicitFieldTeleContext) {
        explicit = false;
        vars = ((ImplicitFieldTeleContext) tele).ID();
        exprCtx = ((ImplicitFieldTeleContext) tele).expr();
        forced = ((ImplicitFieldTeleContext) tele).CLASSIFYING() != null;
      } else {
        throw new IllegalStateException();
      }

      Concrete.Expression type = visitExpr(exprCtx);
      for (TerminalNode var : vars) {
        ConcreteClassFieldReferable fieldRef = new ConcreteClassFieldReferable(tokenPosition(var.getSymbol()), var.getText(), Precedence.DEFAULT, false, explicit, true, classDef.getData(), LocatedReferableImpl.Kind.FIELD);
        Concrete.ClassField field = new Concrete.ClassField(fieldRef, classDef, explicit, ClassFieldKind.ANY, new ArrayList<>(), type, null);
        fieldRef.setDefinition(field);
        fields.add(field);

        if (forced) {
          if (isForced) {
            myErrorReporter.report(new ParserError(tokenPosition(var.getSymbol()), "Class can have at most one classifying field"));
          } else {
            coercingField = fieldRef;
            isForced = true;
          }
        } else if (coercingField == null && explicit) {
          coercingField = fieldRef;
        }
      }
    }

    if (coercingField != null) {
      classDef.setCoercingField(coercingField, isForced);
    }
  }

  @Override
  public Concrete.Expression visitAtomLiteral(AtomLiteralContext ctx) {
    return visitExpr(ctx.literal());
  }

  @Override
  public Concrete.NumericLiteral visitAtomNumber(AtomNumberContext ctx) {
    return new Concrete.NumericLiteral(tokenPosition(ctx.start), new BigInteger(ctx.NUMBER().getText(), 10));
  }

  @Override
  public Concrete.NumericLiteral visitAtomNegativeNumber(AtomNegativeNumberContext ctx) {
    return new Concrete.NumericLiteral(tokenPosition(ctx.start), new BigInteger(ctx.NEGATIVE_NUMBER().getText(), 10));
  }

  @Override
  public Concrete.ThisExpression visitAtomThis(AtomThisContext ctx) {
    return new Concrete.ThisExpression(tokenPosition(ctx.start), null);
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    return new Concrete.SigmaExpression(tokenPosition(ctx.start), visitTeles(ctx.tele()));
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    return new Concrete.PiExpression(tokenPosition(ctx.start), visitTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.Expression visitApp(AppContext ctx) {
    return visitNewExpr(ctx.newExpr());
  }

  @Override
  public Concrete.Expression visitNewExpr(NewExprContext ctx) {
    Concrete.Expression expr = visitNew(ctx.appPrefix(), ctx.appExpr(), ctx.implementStatements());
    List<ArgumentContext> argumentCtxs = ctx.argument();
    if (!argumentCtxs.isEmpty()) {
      List<Concrete.BinOpSequenceElem> sequence = new ArrayList<>(argumentCtxs.size() + 1);
      sequence.add(new Concrete.BinOpSequenceElem(expr));
      for (ArgumentContext argCtx : argumentCtxs) {
        sequence.add(visitArgument(argCtx));
      }
      expr = new Concrete.BinOpSequenceExpression(expr.getData(), sequence);
    }
    return expr;
  }

  private Concrete.Expression visitNew(AppPrefixContext prefixCtx, AppExprContext appCtx, ImplementStatementsContext implCtx) {
    Concrete.Expression expr = visitAppExpr(appCtx);

    if (implCtx != null) {
      expr = Concrete.ClassExtExpression.make(tokenPosition(appCtx.start), expr, visitCoClauses(implCtx.coClause()));
    }

    if (prefixCtx != null) {
      TerminalNode peval = prefixCtx.PEVAL();
      if (peval != null) {
        expr = new Concrete.EvalExpression(tokenPosition(peval.getSymbol()), true, expr);
      } else {
        TerminalNode eval = prefixCtx.EVAL();
        if (eval != null) {
          expr = new Concrete.EvalExpression(tokenPosition(eval.getSymbol()), false, expr);
        }
      }

      TerminalNode newNode = prefixCtx.NEW();
      if (newNode != null) {
        expr = new Concrete.NewExpression(tokenPosition(newNode.getSymbol()), expr);
      }
    }

    return expr;
  }

  private ArrayList<String> visitLongNamePath(LongNameContext ctx) {
    List<TerminalNode> ids = ctx.ID();
    ArrayList<String> result = new ArrayList<>(ids.size());
    for (TerminalNode id : ids) {
      result.add(id.getText());
    }
    return result;
  }

  private Concrete.ReferenceExpression visitLongNameRef(LongNameContext ctx, String name, Fixity fixity) {
    Position position = tokenPosition(ctx.start);
    ArrayList<String> names = visitLongNamePath(ctx);
    if (name != null) {
      names.add(name);
    }

    Referable referable = LongUnresolvedReference.make(position, names);
    return fixity == null
      ? new Concrete.ReferenceExpression(position, referable)
      : new Concrete.FixityReferenceExpression(position, referable, fixity);
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    Concrete.Expression expression = visitExpr(ctx.atom());
    for (TerminalNode projCtx : ctx.NUMBER()) {
      expression = new Concrete.ProjExpression(tokenPosition(projCtx.getSymbol()), expression, Integer.parseInt(projCtx.getText()) - 1);
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
      if (!(elimExpr instanceof Concrete.ReferenceExpression)) {
        myErrorReporter.report(new ParserError((Position) elimExpr.getData(), "\\elim can be applied only to a local variable"));
        throw new ParseException();
      }
      result.add((Concrete.ReferenceExpression) elimExpr);
    }
    return result;
  }

  @Override
  public Concrete.Expression visitCase(CaseContext ctx) {
    List<Concrete.CaseArgument> caseArgs = new ArrayList<>();
    for (CaseArgContext caseArgCtx : ctx.caseArg()) {
      ExprContext typeCtx = caseArgCtx.expr(1);
      TerminalNode id = caseArgCtx.ID();
      caseArgs.add(new Concrete.CaseArgument(visitExpr(caseArgCtx.expr(0)), id == null ? null : new ParsedLocalReferable(tokenPosition(id.getSymbol()), id.getText()), typeCtx == null ? null : visitExpr(typeCtx)));
    }
    List<Concrete.FunctionClause> clauses = new ArrayList<>();
    for (ClauseContext clauseCtx : ctx.clause()) {
      clauses.add(visitClause(clauseCtx));
    }

    Pair<Concrete.Expression,Concrete.Expression> returnPair = visitReturnExpr(ctx.returnExpr());
    Concrete.Expression result = new Concrete.CaseExpression(tokenPosition(ctx.start), ctx.SCASE() != null, caseArgs, returnPair.proj1, returnPair.proj2, clauses);
    boolean isPEval = ctx.PEVAL() != null;
    boolean isEval = !isPEval && ctx.EVAL() != null;
    return isPEval || isEval ? new Concrete.EvalExpression(result.getData(), isPEval, result) : result;
  }

  private Concrete.LetClausePattern visitLetClausePattern(TuplePatternContext tuplePattern) {
    if (tuplePattern instanceof TuplePatternIDContext) {
      TypeAnnotationContext typeAnnotation = ((TuplePatternIDContext) tuplePattern).typeAnnotation();
      Concrete.Expression type = typeAnnotation == null ? null : visitExpr(typeAnnotation.expr());
      return new Concrete.LetClausePattern(new ParsedLocalReferable(tokenPosition(tuplePattern.start), ((TuplePatternIDContext) tuplePattern).ID().getText()), type);
    } else if (tuplePattern instanceof TuplePatternListContext) {
      List<Concrete.LetClausePattern> patterns = new ArrayList<>();
      for (TuplePatternContext subPattern : ((TuplePatternListContext) tuplePattern).tuplePattern()) {
        patterns.add(visitLetClausePattern(subPattern));
      }
      return new Concrete.LetClausePattern(tokenPosition(tuplePattern.start), patterns);
    } else if (tuplePattern instanceof TuplePatternUnknownContext) {
      return new Concrete.LetClausePattern(tokenPosition(tuplePattern.start));
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Concrete.LetClause visitLetClause(LetClauseContext ctx) {
    List<Concrete.Parameter> arguments = visitLamTeles(ctx.tele());
    TypeAnnotationContext typeAnnotationCtx = ctx.typeAnnotation();
    Concrete.Expression resultType = typeAnnotationCtx == null ? null : visitExpr(typeAnnotationCtx.expr());

    TerminalNode id = ctx.ID();
    TuplePatternContext tuplePattern = ctx.tuplePattern();
    if (id == null && tuplePattern instanceof TuplePatternIDContext) {
      id = ((TuplePatternIDContext) tuplePattern).ID();
    }
    if (id != null) {
      return new Concrete.LetClause(new ParsedLocalReferable(tokenPosition(ctx.start), id.getText()), arguments, resultType, visitExpr(ctx.expr()));
    }

    return new Concrete.LetClause(visitLetClausePattern(tuplePattern), resultType, visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.LetExpression visitLet(LetContext ctx) {
    List<Concrete.LetClause> clauses = new ArrayList<>();
    for (LetClauseContext clauseCtx : ctx.letClause()) {
      clauses.add(visitLetClause(clauseCtx));
    }

    return new Concrete.LetExpression(tokenPosition(ctx.start), ctx.LETS() != null, clauses, visitExpr(ctx.expr()));
  }

  private Position tokenPosition(Token token) {
    return new Position(myModule, token.getLine(), token.getCharPositionInLine());
  }
}
