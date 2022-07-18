package org.arend.frontend.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.arend.core.pattern.Pattern;
import org.arend.ext.concrete.definition.ClassFieldKind;
import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.ext.concrete.expr.SigmaFieldKind;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.reference.Precedence;
import org.arend.frontend.group.SimpleNamespaceCommand;
import org.arend.frontend.reference.*;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.InternalConcreteLocatedReferable;
import org.arend.naming.reference.*;
import org.arend.term.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.DefinableMetaDefinition;
import org.arend.term.group.*;
import org.arend.ext.util.Pair;
import org.arend.util.SingletonList;
import org.arend.util.StringEscapeUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.frontend.parser.ArendParser.*;

public class BuildVisitor extends ArendBaseVisitor<Object> {
  private final ModuleLocation myModule;
  private final ErrorReporter myErrorReporter;

  public BuildVisitor(ModuleLocation module, ErrorReporter errorReporter) {
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
    AppContext appExpr = (AppContext) expr;
    if (!(appExpr.appExpr() instanceof AppArgumentContext && appExpr.appPrefix() == null && appExpr.implementStatements() == null)) {
      return false;
    }

    ArgumentAppExprContext argCtx = ((AppArgumentContext) appExpr.appExpr()).argumentAppExpr();
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

  public Concrete.Expression visitExpr(Expr2Context expr) {
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

  private void visitStatementList(List<StatementContext> statementCtxs, List<Statement> statements, ChildGroup parent, TCDefReferable enclosingClass) {
    for (StatementContext statementCtx : statementCtxs) {
      try {
        statements.add(visitStatement(statementCtx, parent, enclosingClass));
      } catch (ParseException ignored) {}
    }
  }

  private Statement visitStatement(StatementContext ctx, ChildGroup parent, TCDefReferable enclosingClass) {
    if (ctx instanceof StatCmdContext) {
      return visitStatCmd((StatCmdContext) ctx, parent);
    } else if (ctx instanceof StatDefContext) {
      return visitDefinition(((StatDefContext) ctx).definition(), parent, enclosingClass);
    } else if (ctx instanceof StatPLevelsContext) {
      return visitStatPLevels((StatPLevelsContext) ctx, parent.getReferable());
    } else if (ctx instanceof StatHLevelsContext) {
      return visitStatHLevels((StatHLevelsContext) ctx, parent.getReferable());
    } else {
      if (ctx != null) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.start), "Unknown statement"));
      }
      throw new ParseException();
    }
  }

  @Override
  public FileGroup visitStatements(StatementsContext ctx) {
    List<Statement> statements = new ArrayList<>();
    FileGroup parentGroup = new FileGroup(new FullModuleReferable(myModule), statements);
    visitStatementList(ctx.statement(), statements, parentGroup, null);
    return parentGroup;
  }

  public StaticGroup visitDefinition(DefinitionContext ctx, ChildGroup parent, TCDefReferable enclosingClass) {
    if (ctx instanceof DefFunctionContext) {
      return visitDefFunction((DefFunctionContext) ctx, parent, enclosingClass);
    } else if (ctx instanceof DefDataContext) {
      return visitDefData((DefDataContext) ctx, parent, enclosingClass);
    } else if (ctx instanceof DefClassContext) {
      return visitDefClass((DefClassContext) ctx, parent, enclosingClass);
    } else if (ctx instanceof DefInstanceContext) {
      return visitDefInstance((DefInstanceContext) ctx, parent, enclosingClass);
    } else if (ctx instanceof DefModuleContext) {
      DefModuleContext moduleCtx = (DefModuleContext) ctx;
      return visitDefModule(moduleCtx.ID(), moduleCtx.where(), parent, enclosingClass);
    } else if (ctx instanceof DefMetaContext) {
      return visitDefMeta((DefMetaContext) ctx, parent, enclosingClass);
    } else {
      if (ctx != null) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.start), "Unknown definition"));
      }
      throw new ParseException();

    }
  }

  private SimpleNamespaceCommand visitStatCmd(StatCmdContext ctx, ChildGroup parent) {
    NamespaceCommand.Kind kind = (NamespaceCommand.Kind) visit(ctx.nsCmd());
    var longName = ctx.longName();
    List<String> path = visitLongNamePath(longName);

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

    return new SimpleNamespaceCommand(tokenPosition(longName.start), kind, path, nsUsing == null || nsUsing.USING() != null, openedReferences, hiddenReferences, parent);
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
    private final Precedence.Associativity associativity;
    private final boolean isInfix;

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

//  @Override
//  public Concrete.Pattern visitPatternConstructor(PatternConstructorContext ctx) {
//    Concrete.Pattern pattern = (Concrete.Pattern) visit(ctx.atomPattern());
//    TerminalNode id = ctx.ID();
//    if (id == null) {
//      return pattern;
//    }
//
//    ExprContext type = ctx.expr();
//    Position position = tokenPosition(id.getSymbol());
//    Concrete.TypedReferable typedRef = new Concrete.TypedReferable(position, new ParsedLocalReferable(position, id.getText()), type == null ? null : visitExpr(type));
//
//    if (pattern instanceof Concrete.NamePattern) {
//      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
//      Referable referable = namePattern.getReferable();
//      if (namePattern.type != null || !(referable instanceof ParsedLocalReferable)) {
//        myErrorReporter.report(new ParserError(tokenPosition(ctx.AS().getSymbol()), "As-patterns are not allowed for variables"));
//        return pattern;
//      }
//      return new Concrete.ConstructorPattern(namePattern.getData(), namePattern.isExplicit(), new NamedUnresolvedReference(((ParsedLocalReferable) referable).getPosition(), referable.textRepresentation()), Collections.emptyList(), typedRef);
//    }
//

//    return pattern;
//  }

  public Concrete.Pattern visitPatternConstructor(PatternConstructorContext ctx) {
    List<AtomPatternContext> atomPatterns = ctx.atomPattern();
    Position position = tokenPosition(ctx.start);
    Concrete.Pattern basePattern = visitAtomPattern(atomPatterns.get(0));
    ExprContext typeCtx = ctx.expr();
    TerminalNode id = ctx.ID();

    if (atomPatterns.size() == 1 && basePattern instanceof Concrete.NamePattern && id == null) {
      Referable referable = ((Concrete.NamePattern) basePattern).getRef() == null ? null : new NamedUnresolvedReference(position, ((Concrete.NamePattern) basePattern).getRef().getRefName());
      return new Concrete.NamePattern(position, basePattern.isExplicit(), referable, typeCtx == null ? null : visitExpr(typeCtx));
    } if (atomPatterns.size() == 1) {
      Concrete.Pattern innerPattern = (Concrete.Pattern) visit(ctx.atomPattern(0));
      if (id == null) {
        return innerPattern;
      }
      ExprContext type = ctx.expr();
      Concrete.TypedReferable typedRef = new Concrete.TypedReferable(position, new ParsedLocalReferable(position, id.getText()), type == null ? null : visitExpr(type));

      if (innerPattern.getAsReferable() != null) {
        myErrorReporter.report(new ParserError(GeneralError.Level.WARNING_UNUSED, tokenPosition(ctx.AS().getSymbol()), "\\as binding is ignored"));
      } else {
        innerPattern.setAsReferable(typedRef);
      }
      return innerPattern;
//          TerminalNode id = ctx.ID();
//    if (id == null) {
//      return pattern;
//    }
    } else {
      if (typeCtx != null && id == null) {
        myErrorReporter.report(new ParserError(tokenPosition(typeCtx.start), "Type annotation is allowed only for variables"));
      }
      List<Concrete.BinOpSequenceElem<Concrete.Pattern>> patterns = new ArrayList<>(atomPatterns.size());
      for (AtomPatternContext atomCtx : atomPatterns) {
        Concrete.Pattern pattern = visitAtomPattern(atomCtx);
        patterns.add(new Concrete.BinOpSequenceElem<>(pattern, Fixity.NONFIX, pattern.isExplicit()));
      }

      Position pos = id == null ? null : tokenPosition(id.getSymbol());
      return new Concrete.UnparsedConstructorPattern(position, true, patterns,
              pos == null ? null : new Concrete.TypedReferable(pos, new ParsedLocalReferable(pos, id.getText()), typeCtx == null ? null : visitExpr(typeCtx)));
    }
  }

  private Concrete.Pattern visitAtomPattern(AtomPatternContext ctx) {
    return (Concrete.Pattern) visit(ctx);
  }

  @Override
  public Concrete.Pattern visitPatternExplicit(PatternExplicitContext ctx) {
    List<PatternContext> patternCtxs = ctx.pattern();
    if (patternCtxs.isEmpty()) {
      return new Concrete.TuplePattern(tokenPosition(ctx.start), Collections.emptyList(), null);
    }
    if (patternCtxs.size() == 1) {
      return visitPattern(patternCtxs.get(0));
    }

    List<Concrete.Pattern> patterns = new ArrayList<>(patternCtxs.size());
    for (PatternContext patternCtx : patternCtxs) {
      patterns.add(visitPattern(patternCtx));
    }
    return new Concrete.TuplePattern(tokenPosition(ctx.start), patterns, null);
  }

  @Override
  public Concrete.Pattern visitPatternImplicit(PatternImplicitContext ctx) {
    Concrete.Pattern pattern = visitPattern(ctx.pattern());
    if (pattern != null) {
      pattern.setExplicit(false);
    }
    return pattern;
  }

//  @Override
//  public Concrete.Pattern visitPatternAtom(PatternAtomContext ctx) {
//    return (Concrete.Pattern) visit(ctx.atomPattern());
//  }

  @Override
  public Concrete.Pattern visitPatternID(PatternIDContext ctx) {
    Position position = tokenPosition(ctx.start);
    TerminalNode infixCtx = ctx.INFIX();
    TerminalNode postfixCtx = infixCtx == null ? ctx.POSTFIX() : null;
    TerminalNode idCtx = postfixCtx == null ? ctx.ID() : null;
    TerminalNode targetCtx = infixCtx != null ? infixCtx : postfixCtx != null ? postfixCtx : idCtx;
    Fixity fixity = infixCtx != null ? Fixity.INFIX : postfixCtx != null ? Fixity.POSTFIX : Fixity.NONFIX;
    LongNameContext longName = ctx.longName();
    Referable targetReferable;
    String refText = infixCtx != null ? getInfixText(infixCtx) : postfixCtx != null ? getPostfixText(postfixCtx) : idCtx.getText();
    if (longName != null) {
      Concrete.ReferenceExpression expression = visitLongNameRef(longName, refText, infixCtx != null ? Fixity.INFIX : postfixCtx != null ? Fixity.POSTFIX : null);
      targetReferable = expression.getReferent();
    } else {
      targetReferable = new NamedUnresolvedReference(tokenPosition(targetCtx.getSymbol()), refText);
    }

    return new Concrete.NamePattern(position, true, targetReferable, null, fixity);
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

    return new Concrete.NumberPattern(tokenPosition(ctx.start), value, null);
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

    return new Concrete.NumberPattern(tokenPosition(ctx.start), value, null);
  }

  @Override
  public Concrete.Pattern visitPatternAny(PatternAnyContext ctx) {
    return new Concrete.NamePattern(tokenPosition(ctx.start), true, null, null);
  }

  private ConcreteLocatedReferable makeReferable(Position position, String name, Precedence precedence, String aliasName, Precedence aliasPrecedence, ChildGroup parent, GlobalReferable.Kind kind) {
    return parent instanceof FileGroup
      ? new ConcreteLocatedReferable(position, name, precedence, aliasName, aliasPrecedence, new FullModuleReferable(myModule), kind)
      : new ConcreteLocatedReferable(position, name, precedence, aliasName, aliasPrecedence, parent.getReferable(), kind);
  }

  private Object parseLevelParameters(Token token, List<TerminalNode> ids, LocatedReferable parent, boolean isPLevels) {
    if (ids.isEmpty()) return parent == null ? new Concrete.LevelParameters(tokenPosition(token), Collections.emptyList(), true) : new Concrete.LevelsDefinition(tokenPosition(token), Collections.emptyList(), true, isPLevels);
    if (ids.size() % 2 == 0) {
      myErrorReporter.report(new ParserError(tokenPosition(ids.get(0).getSymbol()), "Cannot parse level parameters"));
      return null;
    }
    boolean linear = true;
    List<LevelReferable> refs = new ArrayList<>();
    //noinspection unchecked
    LevelDefinition defParent = parent == null ? null : new LevelDefinition(isPLevels, true, (List<TCLevelReferable>) (List<? extends LevelReferable>) refs, parent);
    Boolean increasing = null;
    for (int i = -1; i < ids.size(); i += 2) {
      if (i >= 0) {
        String op = ids.get(i).getText();
        Boolean inc = op.equals(">=") ? Boolean.FALSE : op.equals("<=") ? true : null;
        if (inc == null) {
          myErrorReporter.report(new ParserError(tokenPosition(ids.get(i).getSymbol()), "Expected either '<=' or '>='"));
          return null;
        }
        if (increasing == null) {
          increasing = inc;
        } else if (linear && increasing != inc) {
          myErrorReporter.report(new ParserError(tokenPosition(ids.get(i).getSymbol()), "Level parameters must be linearly ordered"));
          linear = false;
        }
      }

      Position position = tokenPosition(ids.get(i + 1).getSymbol());
      String name = ids.get(i + 1).getText();
      refs.add(defParent == null ? new DataLevelReferable(position, name, isPLevels) : new TCLevelReferable(position, name, defParent));
    }
    if (defParent != null) {
      defParent.setIsIncreasing(increasing == null || increasing);
    }
    //noinspection unchecked
    return parent == null ? new Concrete.LevelParameters(tokenPosition(token), refs, increasing == null || increasing) : new Concrete.LevelsDefinition(tokenPosition(token), (List<TCLevelReferable>) (List<? extends LevelReferable>) refs, defParent.isIncreasing(), isPLevels);
  }

  @Override
  public Concrete.LevelParameters visitPlevelParams(PlevelParamsContext ctx) {
    return ctx == null ? null : (Concrete.LevelParameters) parseLevelParameters(ctx.start, ctx.ID(), null, true);
  }

  @Override
  public Concrete.LevelParameters visitHlevelParams(HlevelParamsContext ctx) {
    return ctx == null ? null : (Concrete.LevelParameters) parseLevelParameters(ctx.start, ctx.ID(), null, false);
  }

  private List<LevelReferable> visitMetaLevels(List<TerminalNode> ids, boolean isPLevels) {
    List<LevelReferable> refs = new ArrayList<>();
    for (TerminalNode id : ids) {
      refs.add(new DataLevelReferable(tokenPosition(id.getSymbol()), id.getText(), isPLevels));
    }
    return refs;
  }

  @Override
  public List<LevelReferable> visitMetaPLevels(MetaPLevelsContext ctx) {
    return ctx == null ? null : visitMetaLevels(ctx.ID(), true);
  }

  @Override
  public List<LevelReferable> visitMetaHLevels(MetaHLevelsContext ctx) {
    return ctx == null ? null : visitMetaLevels(ctx.ID(), false);
  }

  private StaticGroup visitDefInstance(DefInstanceContext ctx, ChildGroup parent, TCDefReferable enclosingClass) {
    boolean isInstance = ctx.instanceKw() instanceof FuncKwInstanceContext;
    List<Concrete.Parameter> parameters = visitLamTeles(ctx.tele(), true);
    TopDefIdContext topDefId = ctx.topDefId();
    DefIdContext defId = topDefId.defId();
    Pair<String, Precedence> alias = visitAlias(defId.alias());
    ConcreteLocatedReferable reference = makeReferable(tokenPosition(defId.ID().getSymbol()), defId.ID().getText(), visitPrecedence(defId.precedence()), alias.proj1, alias.proj2, parent, isInstance ? LocatedReferableImpl.Kind.INSTANCE : GlobalReferable.Kind.DEFINED_CONSTRUCTOR);
    Pair<Concrete.Expression,Concrete.Expression> returnPair = visitReturnExpr(ctx.returnExpr2());

    List<Statement> statements = new ArrayList<>();
    StaticGroup resultGroup = new StaticGroup(reference, statements, parent);

    Concrete.FunctionBody body;
    InstanceBodyContext bodyCtx = ctx.instanceBody();
    List<CoClauseContext> coClauses = null;
    if (bodyCtx instanceof InstanceWithElimContext) {
      InstanceWithElimContext elimCtx = (InstanceWithElimContext) bodyCtx;
      body = new Concrete.ElimFunctionBody(tokenPosition(elimCtx.start), visitElim(elimCtx.elim()), visitClauses(elimCtx.clauses()));
    } else if (bodyCtx instanceof InstanceWithoutElimContext) {
      body = new Concrete.TermFunctionBody(tokenPosition(ctx.start), visitExpr(((InstanceWithoutElimContext) bodyCtx).expr()));
    } else if (bodyCtx instanceof InstanceCowithElimContext) {
      body = new Concrete.CoelimFunctionBody(tokenPosition(bodyCtx.start), new ArrayList<>());
      coClauses = getCoClauses(((InstanceCowithElimContext) bodyCtx).coClauses());
    } else if (bodyCtx instanceof InstanceCoclausesContext) {
      body = new Concrete.CoelimFunctionBody(tokenPosition(bodyCtx.start), new ArrayList<>());
      coClauses = ((InstanceCoclausesContext) bodyCtx).coClause();
    } else {
      throw new IllegalStateException();
    }

    Concrete.FunctionDefinition funcDef = new Concrete.FunctionDefinition(isInstance ? FunctionKind.INSTANCE : FunctionKind.CONS, reference, visitPlevelParams(topDefId.plevelParams()), visitHlevelParams(topDefId.hlevelParams()), parameters, returnPair.proj1, returnPair.proj2, body);
    if (coClauses != null) {
      visitCoClauses(coClauses, statements, resultGroup, reference, enclosingClass, body.getCoClauseElements());
    }
    funcDef.enclosingClass = enclosingClass;
    reference.setDefinition(funcDef);
    visitWhere(ctx.where(), statements, resultGroup, enclosingClass);
    return resultGroup;
  }

  private List<CoClauseContext> getCoClauses(CoClausesContext ctx) {
    if (ctx instanceof CoClausesWithBracesContext) {
      return ((CoClausesWithBracesContext) ctx).coClause();
    }
    if (ctx instanceof CoClausesWithoutBracesContext) {
      return ((CoClausesWithoutBracesContext) ctx).coClause();
    }
    throw new IllegalStateException();
  }

  private void visitCoClauses(List<CoClauseContext> coClausesCtx, List<? super EmptyGroup> statements, ChildGroup parentGroup, TCDefReferable enclosingDefinition, TCDefReferable enclosingClass, List<Concrete.CoClauseElement> result) {
    for (CoClauseContext coClause : coClausesCtx) {
      result.add(visitCoClause(coClause, statements, parentGroup, enclosingClass, enclosingDefinition, false));
    }
  }

  private void visitWhere(WhereContext ctx, List<Statement> statements, ChildGroup parent, TCDefReferable enclosingClass) {
    if (ctx != null) {
      visitStatementList(ctx.statement(), statements, parent, enclosingClass);
    }
  }

  @Override
  public List<Concrete.ReferenceExpression> visitElim(ElimContext ctx) {
    if (ctx == null) {
      return new ArrayList<>();
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
      return new ArrayList<>();
    }
  }

  @Override
  public Pair<Concrete.Expression,Concrete.Expression> visitReturnExprExpr(ReturnExprExprContext ctx) {
    List<ExprContext> exprs = ctx.expr();
    Concrete.Expression resultType = visitExpr(exprs.get(0));
    return new Pair<>(resultType, exprs.size() > 1 ? visitExpr(exprs.get(1)) : null);
  }

  @Override
  public Pair<Concrete.Expression,Concrete.Expression> visitReturnExprExpr2(ReturnExprExpr2Context ctx) {
    List<Expr2Context> exprs = ctx.expr2();
    Concrete.Expression resultType = visitExpr(exprs.get(0));
    return new Pair<>(resultType, exprs.size() > 1 ? visitExpr(exprs.get(1)) : null);
  }

  @Override
  public Pair<Concrete.Expression,Concrete.Expression> visitReturnExprLevel(ReturnExprLevelContext ctx) {
    return new Pair<>(visitAtomFieldsAcc(ctx.atomFieldsAcc(0)), visitAtomFieldsAcc(ctx.atomFieldsAcc(1)));
  }

  @Override
  public Pair<Concrete.Expression,Concrete.Expression> visitReturnExprLevel2(ReturnExprLevel2Context ctx) {
    return new Pair<>(visitAtomFieldsAcc(ctx.atomFieldsAcc(0)), visitAtomFieldsAcc(ctx.atomFieldsAcc(1)));
  }

  private Pair<Concrete.Expression,Concrete.Expression> visitReturnExpr(ReturnExprContext returnExprCtx) {
    //noinspection unchecked
    return returnExprCtx == null ? new Pair<>(null, null) : (Pair<Concrete.Expression,Concrete.Expression>) visit(returnExprCtx);
  }

  private Pair<Concrete.Expression,Concrete.Expression> visitReturnExpr(ReturnExpr2Context returnExprCtx) {
    //noinspection unchecked
    return returnExprCtx == null ? new Pair<>(null, null) : (Pair<Concrete.Expression,Concrete.Expression>) visit(returnExprCtx);
  }

  private StaticGroup visitDefMeta(DefMetaContext ctx, ChildGroup parent, TCDefReferable enclosingClass) {
    DefIdContext defId = ctx.defId();
    var where = ctx.where();
    List<Statement> statements = where == null ? Collections.emptyList() : new ArrayList<>();

    String name = defId.ID().getText();
    var precedence = visitPrecedence(defId.precedence());
    var alias = visitAlias(defId.alias());
    var reference = new MetaReferable(precedence, name, alias.proj2, alias.proj1, "", null, null, parent.getReferable());
    var body = ctx.expr();
    if (body != null) {
      var params = ctx.ID().stream()
        .map(r -> new Concrete.NameParameter(tokenPosition(r.getSymbol()), true, new LocalReferable(r.getText())))
        .collect(Collectors.toList());
      reference.setDefinition(new DefinableMetaDefinition(reference, visitMetaPLevels(ctx.metaPLevels()), visitMetaHLevels(ctx.metaHLevels()), params, visitExpr(body)));
    }

    var resultGroup = new StaticGroup(reference, statements, parent);
    visitWhere(where, statements, resultGroup, enclosingClass);
    return resultGroup;
  }

  private Concrete.LevelsDefinition visitStatPLevels(StatPLevelsContext ctx, LocatedReferable parent) {
    return (Concrete.LevelsDefinition) parseLevelParameters(ctx.start, ctx.ID(), parent, true);
  }

  private Concrete.LevelsDefinition visitStatHLevels(StatHLevelsContext ctx, LocatedReferable parent) {
    return (Concrete.LevelsDefinition) parseLevelParameters(ctx.start, ctx.ID(), parent, false);
  }

  private StaticGroup visitDefFunction(DefFunctionContext ctx, ChildGroup parent, TCDefReferable enclosingClass) {
    Concrete.FunctionBody body;
    FunctionBodyContext functionBodyCtx = ctx.functionBody();
    TopDefIdContext topDefId = ctx.topDefId();
    DefIdContext defId = topDefId.defId();
    Pair<String, Precedence> alias = visitAlias(defId.alias());
    ConcreteLocatedReferable referable = makeReferable(tokenPosition(defId.ID().getSymbol()), defId.ID().getText(), visitPrecedence(defId.precedence()), alias.proj1, alias.proj2, parent, GlobalReferable.Kind.FUNCTION);
    List<Statement> statements = new ArrayList<>();
    StaticGroup resultGroup = new StaticGroup(referable, statements, parent);
    Pair<Concrete.Expression,Concrete.Expression> returnPair = visitReturnExpr(ctx.returnExpr2());

    List<CoClauseContext> coClauses = null;
    if (functionBodyCtx instanceof WithElimContext) {
      WithElimContext elimCtx = (WithElimContext) functionBodyCtx;
      body = new Concrete.ElimFunctionBody(tokenPosition(elimCtx.start), visitElim(elimCtx.elim()), visitClauses(elimCtx.clauses()));
    } else if (functionBodyCtx instanceof CowithElimContext) {
      coClauses = getCoClauses(((CowithElimContext) functionBodyCtx).coClauses());
      body = new Concrete.CoelimFunctionBody(tokenPosition(functionBodyCtx.start), new ArrayList<>());
    } else if (functionBodyCtx instanceof WithoutElimContext) {
      body = new Concrete.TermFunctionBody(tokenPosition(ctx.start), visitExpr(((WithoutElimContext) functionBodyCtx).expr()));
    } else {
      body = null;
    }

    if (body != null) {
      FuncKwContext funcKw = ctx.funcKw();
      boolean isUse = funcKw instanceof FuncKwUseContext;
      Concrete.FunctionDefinition funDef = Concrete.UseDefinition.make(
        isUse ? (((FuncKwUseContext) funcKw).useMod() instanceof UseCoerceContext
          ? FunctionKind.COERCE
          : FunctionKind.LEVEL)
        : funcKw instanceof FuncKwLemmaContext
          ? FunctionKind.LEMMA
          : funcKw instanceof FuncKwTypeContext
            ? FunctionKind.TYPE
            : funcKw instanceof FuncKwSFuncContext
              ? FunctionKind.SFUNC
              : FunctionKind.FUNC,
        referable, visitPlevelParams(topDefId.plevelParams()), visitHlevelParams(topDefId.hlevelParams()), visitLamTeles(ctx.tele(), true), returnPair.proj1, returnPair.proj2, body, parent.getReferable());
      if (coClauses != null) {
        visitCoClauses(coClauses, statements, resultGroup, referable, enclosingClass, body.getCoClauseElements());
      }
      if (isUse && !funDef.getKind().isUse()) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.funcKw().start), "\\use is not allowed on the top level"));
      }

      funDef.enclosingClass = enclosingClass;
      referable.setDefinition(funDef);
      visitWhere(ctx.where(), statements, resultGroup, enclosingClass);

      List<TCDefReferable> usedDefinitions = new ArrayList<>();
      collectUsedDefinitions(statements, usedDefinitions);
      if (!usedDefinitions.isEmpty()) {
        funDef.setUsedDefinitions(usedDefinitions);
      }
    }

    return resultGroup;
  }

  private StaticGroup visitDefData(DefDataContext ctx, ChildGroup parent, TCDefReferable enclosingClass) {
    final Concrete.UniverseExpression universe;
    Expr2Context exprCtx = ctx.expr2();
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
    TopDefIdContext topDefId = ctx.topDefId();
    DefIdContext defId = topDefId.defId();
    Pair<String, Precedence> alias = visitAlias(defId.alias());
    ConcreteLocatedReferable referable = makeReferable(tokenPosition(defId.ID().getSymbol()), defId.ID().getText(), visitPrecedence(defId.precedence()), alias.proj1, alias.proj2, parent, LocatedReferableImpl.Kind.DATA);
    Concrete.DataDefinition dataDefinition = new Concrete.DataDefinition(referable, visitPlevelParams(topDefId.plevelParams()), visitHlevelParams(topDefId.hlevelParams()), visitTeles(ctx.tele(), true), eliminatedReferences, ctx.TRUNCATED() != null, universe, new ArrayList<>());
    dataDefinition.enclosingClass = enclosingClass;
    referable.setDefinition(dataDefinition);
    visitDataBody(dataBodyCtx, dataDefinition, constructors);

    List<Statement> statements = new ArrayList<>();
    DataGroup resultGroup = new DataGroup(referable, constructors, statements, parent);
    visitWhere(ctx.where(), statements, resultGroup, enclosingClass);

    List<TCDefReferable> usedDefinitions = new ArrayList<>();
    collectUsedDefinitions(statements, usedDefinitions);
    if (!usedDefinitions.isEmpty()) {
      dataDefinition.setUsedDefinitions(usedDefinitions);
    }

    return resultGroup;
  }

  private void collectUsedDefinitions(Group group, List<TCDefReferable> usedDefinitions) {
    if (group.getReferable() instanceof ConcreteLocatedReferable) {
      ConcreteLocatedReferable ref = (ConcreteLocatedReferable) group.getReferable();
      Concrete.ReferableDefinition def = ref.getDefinition();
      if (def instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) def).getKind().isUse()) {
        usedDefinitions.add(ref);
      }
    }
  }

  private void collectUsedDefinitions(List<? extends Statement> statements, List<TCDefReferable> usedDefinitions) {
    for (Statement statement : statements) {
      Group group = statement.getGroup();
      if (group != null) {
        collectUsedDefinitions(group, usedDefinitions);
      }
    }
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

        DefIdContext defId = conCtx.defId();
        Pair<String, Precedence> alias = visitAlias(defId.alias());
        InternalConcreteLocatedReferable reference = new InternalConcreteLocatedReferable(tokenPosition(defId.ID().getSymbol()), defId.ID().getText(), visitPrecedence(defId.precedence()), alias.proj1, alias.proj2, true, def.getData(), LocatedReferableImpl.Kind.CONSTRUCTOR);
        Concrete.Constructor constructor = new Concrete.Constructor(reference, def, visitTeles(conCtx.tele(), true), visitElim(elimCtx), clauses, conCtx.COERCE() != null);
        reference.setDefinition(constructor);
        Expr2Context type = conCtx.expr2();
        if (type != null) {
          constructor.setResultType(visitExpr(type));
        }
        constructors.add(reference);
        result.add(constructor);
      } catch (ParseException ignored) {

      }
    }
    return result;
  }

  @Override
  public Pair<String, Precedence> visitAlias(AliasContext ctx) {
    return new Pair<>(ctx == null ? null : ctx.ID().getText(), ctx == null ? null : visitPrecedence(ctx.precedence()));
  }

  @Override
  public ClassFieldKind visitFieldField(FieldFieldContext ctx) {
    return ClassFieldKind.FIELD;
  }

  @Override
  public ClassFieldKind visitFieldProperty(FieldPropertyContext ctx) {
    return ClassFieldKind.PROPERTY;
  }

  private Concrete.ClassField visitClassFieldDef(ClassFieldDefContext ctx, ClassFieldKind kind, Concrete.ClassDefinition parentClass) {
    List<Concrete.TypeParameter> parameters = visitTeles(ctx.tele(), false);
    Pair<Concrete.Expression,Concrete.Expression> returnPair = visitReturnExpr(ctx.returnExpr());
    DefIdContext defId = ctx.defId();
    Pair<String, Precedence> alias = visitAlias(defId.alias());
    ConcreteClassFieldReferable reference = new ConcreteClassFieldReferable(tokenPosition(defId.ID().getSymbol()), defId.ID().getText(), visitPrecedence(defId.precedence()), alias.proj1, alias.proj2, true, true, false, parentClass.getData());
    Concrete.ClassField field = new Concrete.ClassField(reference, parentClass, true, kind, parameters, returnPair.proj1, returnPair.proj2, ctx.COERCE() != null);
    reference.setDefinition(field);
    if (ctx.CLASSIFYING() != null) {
      setClassifyingField(parentClass, reference, true);
    }
    return field;
  }

  private void visitInstanceStatement(ClassFieldOrImplContext ctx, List<Concrete.ClassElement> elements, Concrete.ClassDefinition parentClass) {
    if (ctx instanceof ClassFieldContext) {
      elements.add(visitClassFieldDef(((ClassFieldContext) ctx).classFieldDef(), ClassFieldKind.ANY, parentClass));
    } else if (ctx instanceof ClassImplContext) {
      elements.add(visitLocalCoClause(((ClassImplContext) ctx).localCoClause()));
    }
  }

  private void visitInstanceStatements(List<ClassFieldOrImplContext> ctx, List<Concrete.ClassElement> elements, Concrete.ClassDefinition parentClass) {
    for (ClassFieldOrImplContext statCtx : ctx) {
      if (statCtx != null) {
        try {
          visitInstanceStatement(statCtx, elements, parentClass);
        } catch (ParseException ignored) {}
      }
    }
  }

  private void visitInstanceStatements(List<ClassStatContext> ctx, List<Concrete.ClassElement> elements, List<? super Group> statements, Concrete.ClassDefinition parentClass, ChildGroup parent) {
    for (ClassStatContext statementCtx : ctx) {
      if (statementCtx == null) {
        continue;
      }

      try {
        if (statementCtx instanceof ClassFieldOrImplStatContext) {
          visitInstanceStatement(((ClassFieldOrImplStatContext) statementCtx).classFieldOrImpl(), elements, parentClass);
        } else if (statementCtx instanceof ClassDefinitionStatContext) {
          statements.add(visitDefinition(((ClassDefinitionStatContext) statementCtx).definition(), parent, parentClass.getData()));
        } else if (statementCtx instanceof ClassFieldStatContext) {
          ClassFieldStatContext fieldStatCtx = (ClassFieldStatContext) statementCtx;
          elements.add(visitClassFieldDef(fieldStatCtx.classFieldDef(), (ClassFieldKind) visit(fieldStatCtx.fieldMod()), parentClass));
        } else if (statementCtx instanceof ClassOverrideStatContext) {
          ClassOverrideStatContext overrideCtx = (ClassOverrideStatContext) statementCtx;
          LongNameContext longName = overrideCtx.longName();
          Pair<Concrete.Expression, Concrete.Expression> pair = visitReturnExpr(overrideCtx.returnExpr());
          elements.add(new Concrete.OverriddenField(tokenPosition(overrideCtx.start), LongUnresolvedReference.make(tokenPosition(longName.start), visitLongNamePath(longName)), visitTeles(overrideCtx.tele(), false), pair.proj1, pair.proj2));
        } else if (statementCtx instanceof ClassDefaultStatContext) {
          elements.add(visitCoClause(((ClassDefaultStatContext) statementCtx).coClause(), statements, parent, parentClass.getData(), parentClass.getData(), true));
        } else {
          throw new IllegalStateException();
        }
      } catch (ParseException ignored) {

      }
    }
  }

  private StaticGroup visitDefModule(TerminalNode id, WhereContext where, ChildGroup parent, TCDefReferable enclosingClass) {
    List<Statement> statements = where == null ? Collections.emptyList() : new ArrayList<>();
    var reference = makeReferable(tokenPosition(id.getSymbol()), id.getText(), Precedence.DEFAULT, null, null, parent, GlobalReferable.Kind.OTHER);
    StaticGroup resultGroup = new StaticGroup(reference, statements, parent);
    visitWhere(where, statements, resultGroup, enclosingClass);
    return resultGroup;
  }

  @Override
  public Concrete.ReferenceExpression visitSuperClass(SuperClassContext ctx) {
    Concrete.ReferenceExpression result = visitLongNameRef(ctx.longName(), null, null);
    List<MaybeLevelAtomsContext> levelCtxs = ctx.maybeLevelAtoms();
    if (!levelCtxs.isEmpty()) {
      result.setPLevels(visitLevels(levelCtxs.get(0)));
    }
    if (levelCtxs.size() >= 2) {
      result.setHLevels(visitLevels(levelCtxs.get(1)));
    }
    return result;
  }

  private ClassGroup visitDefClass(DefClassContext ctx, ChildGroup parent, TCDefReferable enclosingClass) {
    WhereContext where = ctx.where();

    List<Statement> statements = where == null ? Collections.emptyList() : new ArrayList<>();
    List<Group> dynamicSubgroups = new ArrayList<>();

    List<Concrete.ReferenceExpression> superClasses = new ArrayList<>();
    for (SuperClassContext superClassCtx : ctx.superClass()) {
      superClasses.add(visitSuperClass(superClassCtx));
    }

    TopDefIdContext topDefId = ctx.topDefId();
    DefIdContext defId = topDefId.defId();
    Position pos = tokenPosition(defId.ID().getSymbol());
    String name = defId.ID().getText();
    Precedence prec = visitPrecedence(defId.precedence());
    ConcreteClassReferable reference;
    List<ConcreteClassFieldReferable> fieldReferables = new ArrayList<>();
    Pair<String, Precedence> alias = visitAlias(defId.alias());
    reference = parent instanceof FileGroup
      ? new ConcreteClassReferable(pos, name, prec, alias.proj1, alias.proj2, fieldReferables, superClasses, new FullModuleReferable(myModule))
      : new ConcreteClassReferable(pos, name, prec, alias.proj1, alias.proj2, fieldReferables, superClasses, parent.getReferable());
    ClassGroup resultGroup = new ClassGroup(reference, fieldReferables, dynamicSubgroups, statements, parent);
    boolean isRecord = ctx.classKw() instanceof ClassKwRecordContext;
    ClassBodyContext classBodyCtx = ctx.classBody();
    List<ClassStatContext> classStatCtxs = classBodyCtx instanceof ClassBodyStatsContext ? ((ClassBodyStatsContext) classBodyCtx).classStat() : Collections.emptyList();
    List<ClassFieldOrImplContext> classFieldOrImplCtxs = classBodyCtx instanceof ClassBodyFieldOrImplContext ? ((ClassBodyFieldOrImplContext) classBodyCtx).classFieldOrImpl() : Collections.emptyList();

    List<Concrete.ClassElement> elements = new ArrayList<>();
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(reference, visitPlevelParams(topDefId.plevelParams()), visitHlevelParams(topDefId.hlevelParams()), isRecord, ctx.NO_CLASSIFYING() != null, new ArrayList<>(superClasses), elements);
    reference.setDefinition(classDefinition);
    visitFieldTeles(ctx.fieldTele(), classDefinition, elements);

    List<TCDefReferable> usedDefinitions = new ArrayList<>();
    if (!classStatCtxs.isEmpty()) {
      visitInstanceStatements(classStatCtxs, elements, dynamicSubgroups, classDefinition, resultGroup);
      for (Group subgroup : dynamicSubgroups) {
        collectUsedDefinitions(subgroup, usedDefinitions);
      }
    }
    visitInstanceStatements(classFieldOrImplCtxs, elements, classDefinition);
    reference.setGroup(resultGroup);

    for (Concrete.ClassElement element : elements) {
      if (element instanceof Concrete.ClassField) {
        fieldReferables.add((ConcreteClassFieldReferable) element.getData());
      } else if (element instanceof Concrete.CoClauseFunctionReference) {
        usedDefinitions.add(((Concrete.CoClauseFunctionReference) element).getFunctionReference());
      }
    }

    visitWhere(where, statements, resultGroup, enclosingClass);

    collectUsedDefinitions(statements, usedDefinitions);
    if (!usedDefinitions.isEmpty()) {
      classDefinition.setUsedDefinitions(usedDefinitions);
    }

    return resultGroup;
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

  private Concrete.PiExpression visitArr(ParserRuleContext ctx, Concrete.Expression domain, Concrete.Expression codomain) {
    List<Concrete.TypeParameter> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeParameter(domain.getData(), true, domain));
    return new Concrete.PiExpression(tokenPosition(ctx.getToken(ARROW, 0).getSymbol()), arguments, codomain);
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    return visitArr(ctx, visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)));
  }

  @Override
  public Concrete.PiExpression visitArr2(Arr2Context ctx) {
    return visitArr(ctx, visitExpr(ctx.expr2(0)), visitExpr(ctx.expr2(1)));
  }

  @Override
  public Concrete.Expression visitTupleExpr(TupleExprContext ctx) {
    List<ExprContext> exprs = ctx.expr();
    Concrete.Expression expr = visitExpr(exprs.get(0));
    return exprs.size() > 1 ? new Concrete.TypedExpression(expr.getData(), expr, visitExpr(exprs.get(1))) : expr;
  }

  private Concrete.Expression visitTupleExprs(List<TupleExprContext> exprs, List<TerminalNode> commas, ParserRuleContext parentCtx) {
    if (exprs.size() == 1 && commas.isEmpty()) {
      return visitTupleExpr(exprs.get(0));
    } else {
      List<Concrete.Expression> fields = new ArrayList<>();
      for (TupleExprContext exprCtx : exprs) {
        fields.add(visitTupleExpr(exprCtx));
      }
      if (commas.size() == exprs.size() && !commas.isEmpty()) {
        fields.add(new Concrete.IncompleteExpression(tokenPosition(commas.get(commas.size() - 1).getSymbol())));
      }
      return new Concrete.TupleExpression(tokenPosition(parentCtx.start), fields);
    }
  }

  @Override
  public Concrete.Expression visitTuple(TupleContext ctx) {
    return visitTupleExprs(ctx.tupleExpr(), ctx.COMMA(), ctx);
  }

  private void checkStrict(TypedExprContext ctx) {
    if (ctx.STRICT() != null) {
      myErrorReporter.report(new ParserError(tokenPosition(ctx.STRICT().getSymbol()), "\\strict is not allowed here"));
    }
  }

  private List<Concrete.Parameter> visitLamTele(TeleContext tele, boolean isDefinition) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    if (tele instanceof ExplicitContext || tele instanceof ImplicitContext) {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      List<ParsedLocalReferable> vars = new ArrayList<>();
      List<ExprContext> exprs = typedExpr.expr();
      if (exprs.size() == 2) {
        getVarList(exprs.get(0), vars);
        if (isDefinition && typedExpr.STRICT() != null) {
          parameters.add(new Concrete.DefinitionTelescopeParameter(tokenPosition(tele.start), explicit, true, vars, visitExpr(exprs.get(1))));
        } else {
          checkStrict(typedExpr);
          parameters.add(new Concrete.TelescopeParameter(tokenPosition(tele.start), explicit, vars, visitExpr(exprs.get(1))));
        }
      } else {
        getVarList(exprs.get(0), vars);
        if (!isDefinition) {
          checkStrict(typedExpr);
        }
        for (ParsedLocalReferable var : vars) {
          parameters.add(new Concrete.NameParameter(var.getPosition(), explicit, var));
        }
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
          parameters.add(new Concrete.NameParameter(tokenPosition(literalContext.start), true, null));
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

  private List<Concrete.Parameter> visitLamTeles(List<TeleContext> tele, boolean isDefinition) {
    List<Concrete.Parameter> arguments = new ArrayList<>();
    for (TeleContext arg : tele) {
      arguments.addAll(visitLamTele(arg, isDefinition));
    }
    return arguments;
  }

  private Referable visitIdOrUnknown(IdOrUnknownContext ctx) {
    return ctx instanceof IuIdContext ? new ParsedLocalReferable(tokenPosition(ctx.start), ((IuIdContext) ctx).ID().getText()) : null;
  }

  private List<Concrete.Parameter> visitNameTele(NameTeleContext tele) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    if (tele instanceof NameIdContext) {
      parameters.add(new Concrete.NameParameter(tokenPosition(tele.start), true, visitIdOrUnknown(((NameIdContext) tele).idOrUnknown())));
    } else {
      boolean explicit = tele instanceof NameExplicitContext;
      List<IdOrUnknownContext> ids = explicit ? ((NameExplicitContext) tele).idOrUnknown() : ((NameImplicitContext) tele).idOrUnknown();
      ExprContext type = explicit ? ((NameExplicitContext) tele).expr() : ((NameImplicitContext) tele).expr();
      if (type == null) {
        for (IdOrUnknownContext id : ids) {
          parameters.add(new Concrete.NameParameter(tokenPosition(id.start), explicit, visitIdOrUnknown(id)));
        }
      } else {
        List<Referable> vars = new ArrayList<>(ids.size());
        for (IdOrUnknownContext id : ids) {
          vars.add(visitIdOrUnknown(id));
        }
        parameters.add(new Concrete.TelescopeParameter(tokenPosition(tele.start), explicit, vars, visitExpr(type)));
      }
    }
    return parameters;
  }

  private List<Concrete.Pattern> visitLamParams(List<LamParamContext> list, List<Concrete.Parameter> parameters) {
    List<Concrete.Pattern> patterns = Collections.emptyList();
    for (LamParamContext ctx : list) {
      if (ctx instanceof LamTeleContext) {
        parameters.addAll(visitNameTele(((LamTeleContext) ctx).nameTele()));
        if (!patterns.isEmpty()) patterns.add(null);
      } else if (ctx instanceof LamPatternContext) {
        if (patterns.isEmpty()) {
          patterns = new ArrayList<>();
          for (Concrete.Parameter ignored : parameters) {
            patterns.add(null);
          }
        }
        patterns.add((Concrete.Pattern) visit(((LamPatternContext) ctx).atomPattern()));
      } else {
        throw new IllegalStateException();
      }
    }
    return patterns;
  }

  @Override
  public Concrete.LamExpression visitLam(LamContext ctx) {
    return visitLamExpr(ctx.lamExpr());
  }

  @Override
  public Concrete.LamExpression visitLam2(Lam2Context ctx) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    return Concrete.PatternLamExpression.make(tokenPosition(ctx.start), parameters, visitLamParams(ctx.lamParam(), parameters), visitIncompleteExpression(ctx.expr2(), ctx));
  }

  @Override
  public Concrete.LamExpression visitLamExpr(LamExprContext ctx) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    return Concrete.PatternLamExpression.make(tokenPosition(ctx.start), parameters, visitLamParams(ctx.lamParam(), parameters), visitIncompleteExpression(ctx.expr(), ctx));
  }

  private Concrete.Expression visitAppExpr(AppExprContext ctx) {
    return (Concrete.Expression) visit(ctx);
  }

  @Override
  public Concrete.Expression visitAppArgument(AppArgumentContext ctx) {
    return visitArgumentAppExpr(ctx.argumentAppExpr());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
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

        List<Concrete.LevelExpression> levels1;
        List<Concrete.LevelExpression> levels2;
        if (obj1 instanceof Pair) {
          levels1 = (List<Concrete.LevelExpression>) ((Pair) obj1).proj1;
          levels2 = (List<Concrete.LevelExpression>) ((Pair) obj1).proj2;
        } else {
          levels1 = new SingletonList<>((Concrete.LevelExpression) obj1);
          levels2 = obj2 instanceof Concrete.LevelExpression ? new SingletonList<>((Concrete.LevelExpression) obj2) : null;
        }

        expr = new Concrete.ReferenceExpression(expr.getData(), ((Concrete.ReferenceExpression) expr).getReferent(), levels1, levels2);
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(onlyLevelAtoms.get(0).start), "Level annotations are allowed only after a reference"));
      }
    }

    List<ArgumentContext> argumentCtxs = ctx.argument();
    if (argumentCtxs.isEmpty()) {
      return expr;
    }

    List<Concrete.BinOpSequenceElem<Concrete.Expression>> sequence = new ArrayList<>(argumentCtxs.size());
    sequence.add(new Concrete.BinOpSequenceElem(expr));
    for (ArgumentContext argumentCtx : argumentCtxs) {
      sequence.add(visitArgument(argumentCtx));
    }

    return new Concrete.BinOpSequenceExpression(expr.getData(), sequence, null);
  }

  private Concrete.BinOpSequenceElem<Concrete.Expression> visitArgument(ArgumentContext ctx) {
    //noinspection unchecked
    return (Concrete.BinOpSequenceElem<Concrete.Expression>) visit(ctx);
  }

  @Override
  public Concrete.BinOpSequenceElem<Concrete.Expression> visitArgumentExplicit(ArgumentExplicitContext ctx) {
    AtomFieldsAccContext atomFieldsAcc = ctx.atomFieldsAcc();
    return new Concrete.BinOpSequenceElem<>(visitAtomFieldsAcc(atomFieldsAcc), atomFieldsAcc.atom() instanceof AtomLiteralContext && isName(((AtomLiteralContext) atomFieldsAcc.atom()).literal()) && atomFieldsAcc.NUMBER().isEmpty() ? Fixity.UNKNOWN : Fixity.NONFIX, true);
  }

  private boolean isName(LiteralContext ctx) {
    return ctx instanceof NameContext || ctx instanceof InfixContext || ctx instanceof PostfixContext;
  }

  @Override
  public Concrete.BinOpSequenceElem<Concrete.Expression> visitArgumentNew(ArgumentNewContext ctx) {
    return new Concrete.BinOpSequenceElem<>(visitNew(ctx.appPrefix(), ctx.appExpr(), ctx.implementStatements()), Fixity.NONFIX, true);
  }

  @Override
  public Concrete.BinOpSequenceElem<Concrete.Expression> visitArgumentUniverse(ArgumentUniverseContext ctx) {
    return new Concrete.BinOpSequenceElem<>(visitExpr(ctx.universeAtom()), Fixity.NONFIX, true);
  }

  @Override
  public Concrete.BinOpSequenceElem<Concrete.Expression> visitArgumentImplicit(ArgumentImplicitContext ctx) {
    return new Concrete.BinOpSequenceElem<>(visitTupleExprs(ctx.tupleExpr(), ctx.COMMA(), ctx), Fixity.NONFIX, false);
  }

  @Override
  public Object visitArgumentLam(ArgumentLamContext ctx) {
    return new Concrete.BinOpSequenceElem<>(visitLamExpr(ctx.lamExpr()), Fixity.NONFIX, true);
  }

  @Override
  public Object visitArgumentCase(ArgumentCaseContext ctx) {
    return new Concrete.BinOpSequenceElem<>(visitCaseExpr(ctx.caseExpr()), Fixity.NONFIX, true);
  }

  private Concrete.CoClauseElement visitCoClause(CoClauseContext ctx, List<? super EmptyGroup> statements, ChildGroup parentGroup, TCDefReferable enclosingClass, TCDefReferable enclosingDefinition, boolean isDefault) {
    List<String> path = visitLongNamePath(ctx.longName());
    Position position = tokenPosition(ctx.start);
    Concrete.Expression term = null;
    List<Concrete.ClassFieldImpl> subClassFieldImpls = null;
    CoClauseBodyContext body = ctx.coClauseBody();

    if (body instanceof CoClauseDefContext) {
      ReturnExpr2Context returnCtx = ((CoClauseDefContext) body).returnExpr2();
      CoClauseDefBodyContext defBody = ((CoClauseDefContext) body).coClauseDefBody();
      PrecedenceContext precCtx = ctx.precedence();
      TerminalNode id = ctx.ID();
      if (defBody instanceof CoClauseExprContext && precCtx == null && id == null && returnCtx == null) {
        List<LamParamContext> lamParamCtxs = ((CoClauseDefContext) body).lamParam();
        List<Concrete.Parameter> parameters = new ArrayList<>();
        List<Concrete.Pattern> patterns = visitLamParams(lamParamCtxs, parameters);
        term = visitExpr(((CoClauseExprContext) defBody).expr());
        if (!parameters.isEmpty() || !patterns.isEmpty()) {
          Position pos = tokenPosition(lamParamCtxs.get(0).start);
          term = Concrete.PatternLamExpression.make(pos, parameters, patterns, term);
        }
      } else {
        ConcreteLocatedReferable reference = makeReferable(position, id != null ? id.getText() : path.get(path.size() - 1), precCtx == null || precCtx instanceof NoPrecedenceContext ? null : visitPrecedence(precCtx), null, Precedence.DEFAULT, parentGroup, LocatedReferableImpl.Kind.COCLAUSE_FUNCTION);
        EmptyGroup myGroup = new EmptyGroup(reference, parentGroup);
        statements.add(myGroup);
        Pair<Concrete.Expression, Concrete.Expression> pair = visitReturnExpr(returnCtx);
        Referable fieldRef = LongUnresolvedReference.make(position, path);
        Concrete.FunctionBody fBody;
        if (defBody instanceof CoClauseExprContext) {
          fBody = new Concrete.TermFunctionBody(tokenPosition(defBody.start), visitExpr(((CoClauseExprContext) defBody).expr()));
        } else if (defBody instanceof CoClauseWithContext) {
          CoClauseWithContext withBody = (CoClauseWithContext) defBody;
          List<Concrete.FunctionClause> clauses = new ArrayList<>();
          for (ClauseContext clauseCtx : withBody.clause()) {
            clauses.add(visitClause(clauseCtx));
          }
          fBody = new Concrete.ElimFunctionBody(tokenPosition(defBody.start), visitElim(withBody.elim()), clauses);
        } else if (defBody instanceof CoClauseCowithContext) {
          List<CoClauseContext> coClauses = getCoClauses(((CoClauseCowithContext) defBody).coClauses());
          fBody = new Concrete.CoelimFunctionBody(tokenPosition(defBody.start), new ArrayList<>());
          visitCoClauses(coClauses, statements, myGroup, reference, enclosingClass, fBody.getCoClauseElements());
        } else {
          throw new IllegalStateException();
        }
        List<LamParamContext> lamParamCtxs = ((CoClauseDefContext) body).lamParam();
        List<Concrete.Parameter> parameters = new ArrayList<>();
        List<Concrete.Pattern> patterns = visitLamParams(lamParamCtxs, parameters);
        if (!patterns.isEmpty()) {
          myErrorReporter.report(new ParserError((Position) patterns.get(0).getData(), "Patterns are not allowed in coclause functions"));
        }
        Concrete.CoClauseFunctionDefinition def = new Concrete.CoClauseFunctionDefinition(isDefault ? FunctionKind.CLASS_COCLAUSE : FunctionKind.FUNC_COCLAUSE, reference, enclosingDefinition, fieldRef, parameters, pair.proj1, pair.proj2, fBody);
        def.enclosingClass = enclosingClass;
        reference.setDefinition(def);
        return new Concrete.CoClauseFunctionReference(fieldRef, reference, isDefault);
      }
    } else if (body instanceof CoClauseRecContext) {
      subClassFieldImpls = visitLocalCoClauses(((CoClauseRecContext) body).localCoClause());
    } else {
      throw new IllegalStateException();
    }

    if (ctx.precedence() instanceof WithPrecedenceContext) {
      myErrorReporter.report(new ParserError(tokenPosition(ctx.precedence().start), "Precedence is ignored"));
    }

    return new Concrete.ClassFieldImpl(position, LongUnresolvedReference.make(position, path), term, subClassFieldImpls == null ? null : new Concrete.Coclauses(tokenPosition(body.start), subClassFieldImpls), isDefault);
  }

  private List<Concrete.ClassFieldImpl> visitLocalCoClauses(List<LocalCoClauseContext> contexts) {
    List<Concrete.ClassFieldImpl> result = new ArrayList<>();
    for (LocalCoClauseContext context : contexts) {
      result.add(visitLocalCoClause(context));
    }
    return result;
  }

  @Override
  public Concrete.ClassFieldImpl visitLocalCoClause(LocalCoClauseContext ctx) {
    List<String> path = visitLongNamePath(ctx.longName());
    Position position = tokenPosition(ctx.start);
    List<LamParamContext> lamParamCtxs = ctx.lamParam();
    List<Concrete.Parameter> parameters = new ArrayList<>();
    List<Concrete.Pattern> patterns = visitLamParams(lamParamCtxs, parameters);
    Concrete.Expression term = null;
    List<Concrete.ClassFieldImpl> subClassFieldImpls = null;
    ExprContext exprCtx = ctx.expr();
    if (exprCtx != null) {
      term = visitExpr(exprCtx);
      if (!parameters.isEmpty() || !patterns.isEmpty()) {
        Position pos = tokenPosition(lamParamCtxs.get(0).start);
        term = Concrete.PatternLamExpression.make(pos, parameters, patterns, term);
      }
    } else {
      if (!parameters.isEmpty() || !patterns.isEmpty()) {
        myErrorReporter.report(new ParserError(tokenPosition(lamParamCtxs.get(0).start), "Parameters are allowed only before '=> <expression>'"));
      }
      subClassFieldImpls = visitLocalCoClauses(ctx.localCoClause());
    }

    return new Concrete.ClassFieldImpl(position, LongUnresolvedReference.make(position, path), term, subClassFieldImpls == null ? null : new Concrete.Coclauses(tokenPosition(ctx.start), subClassFieldImpls), false);
  }

  private Concrete.LevelExpression parseTruncatedUniverse(TerminalNode terminal) {
    String universe = terminal.getText();
    if (universe.charAt(1) == 'o' || universe.charAt(1) == 'h') {
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
    text = text.substring(text.indexOf('T') + "Type".length());
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

  private List<Concrete.LevelExpression> visitLevels(MaybeLevelAtomsContext ctx) {
    //noinspection unchecked
    return (List<Concrete.LevelExpression>) visit(ctx);
  }

  @Override
  public List<Concrete.LevelExpression> visitMultiLevel(MultiLevelContext ctx) {
    List<Concrete.LevelExpression> result = new ArrayList<>();
    for (LevelExprContext expr : ctx.levelExpr()) {
      result.add((Concrete.LevelExpression) visit(expr));
    }
    return result;
  }

  @Override
  public List<Concrete.LevelExpression> visitSingleLevel(SingleLevelContext ctx) {
    MaybeLevelAtomContext level = ctx.maybeLevelAtom();
    return level instanceof WithoutLevelAtomContext ? null : new SingletonList<>(visitLevel(level));
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
  public Concrete.NumberLevelExpression visitNegLevel(NegLevelContext ctx) {
    return new Concrete.NumberLevelExpression(tokenPosition(ctx.start), Integer.parseInt(ctx.NEGATIVE_NUMBER().getText()));
  }

  @Override
  public Concrete.IdLevelExpression visitIdLevel(IdLevelContext ctx) {
    Position pos = tokenPosition(ctx.start);
    return new Concrete.IdLevelExpression(pos, new NamedUnresolvedReference(pos, ctx.ID().getText()));
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
  public Pair<List<Concrete.LevelExpression>, List<Concrete.LevelExpression>> visitLevelsOnlyLevel(LevelsOnlyLevelContext ctx) {
    List<MaybeLevelAtomsContext> maybeLevelAtomsCtxs = ctx.maybeLevelAtoms();
    return new Pair<>(visitLevels(maybeLevelAtomsCtxs.get(0)), visitLevels(maybeLevelAtomsCtxs.get(1)));
  }

  @Override
  public Concrete.SucLevelExpression visitSucOnlyLevel(SucOnlyLevelContext ctx) {
    return new Concrete.SucLevelExpression(tokenPosition(ctx.start), visitLevel(ctx.levelAtom()));
  }

  @Override
  public Concrete.MaxLevelExpression visitMaxOnlyLevel(MaxOnlyLevelContext ctx) {
    return new Concrete.MaxLevelExpression(tokenPosition(ctx.start), visitLevel(ctx.levelAtom(0)), visitLevel(ctx.levelAtom(1)));
  }

  private List<Concrete.TypeParameter> visitTeles(List<TeleContext> teles, boolean isDefinition) {
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

      List<ExprContext> exprs = typedExpr.expr();
      if (exprs.size() == 2) {
        List<ParsedLocalReferable> vars = new ArrayList<>();
        getVarList(exprs.get(0), vars);
        if (isDefinition && typedExpr.STRICT() != null) {
          parameters.add(new Concrete.DefinitionTelescopeParameter(tokenPosition(tele.getStart()), explicit, true, vars, visitExpr(exprs.get(1))));
        } else {
          checkStrict(typedExpr);
          parameters.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, vars, visitExpr(exprs.get(1))));
        }
      } else {
        if (isDefinition && typedExpr.STRICT() != null) {
          parameters.add(new Concrete.DefinitionTypeParameter(explicit, true, visitExpr(exprs.get(0))));
        } else {
          checkStrict(typedExpr);
          parameters.add(new Concrete.TypeParameter(explicit, visitExpr(exprs.get(0))));
        }
      }
    }
    return parameters;
  }

  private List<Concrete.TypeParameter> visitSigmaTeles(List<SigmaTeleContext> teles) {
    List<Concrete.TypeParameter> parameters = new ArrayList<>(teles.size());
    for (SigmaTeleContext tele : teles) {
      if (tele instanceof SigmaTeleLiteralContext) {
        parameters.add(new Concrete.SigmaTypeParameter(visitExpr(((SigmaTeleLiteralContext) tele).literal()), SigmaFieldKind.ANY));
      } else if (tele instanceof SigmaTeleUniverseContext) {
        parameters.add(new Concrete.SigmaTypeParameter(visitExpr(((SigmaTeleUniverseContext) tele).universeAtom()), SigmaFieldKind.ANY));
      } else if (tele instanceof SigmaEntryContext) {
        TypedExprContext typedExprContext = ((SigmaEntryContext) tele).typedExpr();
        SigmaModContext modContext = ((SigmaEntryContext) tele).sigmaMod();
        SigmaFieldKind kind;
        if (modContext instanceof SigmaPropertyContext) {
          kind = visitSigmaProperty((SigmaPropertyContext) modContext);
        } else {
          kind = SigmaFieldKind.ANY;
        }
        List<ExprContext> exprs = typedExprContext.expr();
        checkStrict(typedExprContext);
        if (exprs.size() == 2) {
          List<ParsedLocalReferable> vars = new ArrayList<>();
          getVarList(exprs.get(0), vars);
          parameters.add(new Concrete.SigmaTelescopeParameter(tokenPosition(tele.getStart()), vars, visitExpr(exprs.get(1)), kind));
        } else {
          parameters.add(new Concrete.SigmaTypeParameter(visitExpr(exprs.get(0)), kind));
        }
      }
    }
    return parameters;
  }



  @Override
  public SigmaFieldKind visitSigmaProperty(SigmaPropertyContext ctx) {
    return SigmaFieldKind.PROPERTY;
  }


  private void visitFieldTeles(List<FieldTeleContext> teles, Concrete.ClassDefinition classDef, List<Concrete.ClassElement> fields) {
    for (FieldTeleContext tele : teles) {
      boolean explicit;
      List<TerminalNode> vars;
      ExprContext exprCtx;
      boolean forced;
      boolean coerced;
      if (tele instanceof ExplicitFieldTeleContext) {
        explicit = true;
        vars = ((ExplicitFieldTeleContext) tele).ID();
        exprCtx = ((ExplicitFieldTeleContext) tele).expr();
        forced = ((ExplicitFieldTeleContext) tele).CLASSIFYING() != null;
        coerced = ((ExplicitFieldTeleContext) tele).COERCE() != null;
      } else if (tele instanceof ImplicitFieldTeleContext) {
        explicit = false;
        vars = ((ImplicitFieldTeleContext) tele).ID();
        exprCtx = ((ImplicitFieldTeleContext) tele).expr();
        forced = ((ImplicitFieldTeleContext) tele).CLASSIFYING() != null;
        coerced = ((ImplicitFieldTeleContext) tele).COERCE() != null;
      } else {
        throw new IllegalStateException();
      }

      Concrete.Expression type = visitExpr(exprCtx);
      for (TerminalNode var : vars) {
        ConcreteClassFieldReferable fieldRef = new ConcreteClassFieldReferable(tokenPosition(var.getSymbol()), var.getText(), Precedence.DEFAULT, null, Precedence.DEFAULT, false, explicit, true, classDef.getData());
        Concrete.ClassField field = new Concrete.ClassField(fieldRef, classDef, explicit, ClassFieldKind.ANY, new ArrayList<>(), type, null, coerced);
        fieldRef.setDefinition(field);
        fields.add(field);
        if (forced || explicit) {
          setClassifyingField(classDef, fieldRef, forced);
        }
      }
    }
  }

  private void setClassifyingField(Concrete.ClassDefinition classDef, ConcreteClassFieldReferable field, boolean isForced) {
    if (isForced && classDef.isForcedClassifyingField()) {
      myErrorReporter.report(new ParserError((Position) field.getData(), "Class can have at most one classifying field"));
    } else if (isForced && !classDef.isForcedClassifyingField() || classDef.getClassifyingField() == null) {
      classDef.setClassifyingField(field, isForced);
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
  public Object visitAtomString(AtomStringContext ctx) {
    var unescapedString = StringEscapeUtils.unescapeStringCharacters(ctx.STRING().getText());
    return new Concrete.StringLiteral(tokenPosition(ctx.start), unescapedString.substring(1, unescapedString.length() - 1));
  }

  @Override
  public Concrete.ApplyHoleExpression visitAtomApplyHole(AtomApplyHoleContext ctx) {
    return new Concrete.ApplyHoleExpression(tokenPosition(ctx.start));
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
    return new Concrete.SigmaExpression(tokenPosition(ctx.start), visitSigmaTeles(ctx.sigmaTele()));
  }

  @Override
  public Concrete.SigmaExpression visitSigma2(Sigma2Context ctx) {
    return new Concrete.SigmaExpression(tokenPosition(ctx.start), visitSigmaTeles(ctx.sigmaTele()));
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    return new Concrete.PiExpression(tokenPosition(ctx.start), visitTeles(ctx.tele(), false), visitExpr(ctx.expr()));
  }

  @Override
  public Object visitPi2(Pi2Context ctx) {
    return new Concrete.PiExpression(tokenPosition(ctx.start), visitTeles(ctx.tele(), false), visitExpr(ctx.expr2()));
  }

  private Concrete.Expression visitApp(AppPrefixContext prefixCtx, AppExprContext appCtx, ImplementStatementsContext implCtx, List<ArgumentContext> argumentCtxs, WithBodyContext body) {
    Concrete.Expression expr = visitNew(prefixCtx, appCtx, implCtx);
    if (!argumentCtxs.isEmpty() || body != null) {
      if (expr instanceof Concrete.BinOpSequenceExpression && argumentCtxs.isEmpty()) {
        return new Concrete.BinOpSequenceExpression(expr.getData(), ((Concrete.BinOpSequenceExpression) expr).getSequence(), new Concrete.FunctionClauses(tokenPosition(body.start), visitWithBody(body)));
      }

      List<Concrete.BinOpSequenceElem<Concrete.Expression>> sequence = new ArrayList<>(argumentCtxs.size() + 1);
      sequence.add(new Concrete.BinOpSequenceElem<>(expr));
      for (ArgumentContext argCtx : argumentCtxs) {
        sequence.add(visitArgument(argCtx));
      }
      return new Concrete.BinOpSequenceExpression(expr.getData(), sequence, body == null ? null : new Concrete.FunctionClauses(tokenPosition(body.start), visitWithBody(body)));
    }

    return expr;
  }

  @Override
  public Concrete.Expression visitApp(AppContext ctx) {
    return visitApp(ctx.appPrefix(), ctx.appExpr(), ctx.implementStatements(), ctx.argument(), ctx.withBody());
  }

  @Override
  public Concrete.Expression visitApp2(App2Context ctx) {
    return visitApp(ctx.appPrefix(), ctx.appExpr(), ctx.implementStatements(), ctx.argument(), null);
  }

  private Concrete.Expression visitNew(AppPrefixContext prefixCtx, AppExprContext appCtx, ImplementStatementsContext implCtx) {
    Concrete.Expression expr = visitAppExpr(appCtx);

    if (implCtx != null) {
      expr = Concrete.ClassExtExpression.make(tokenPosition(appCtx.start), expr, new Concrete.Coclauses(tokenPosition(implCtx.start), visitLocalCoClauses(implCtx.localCoClause())));
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
  public List<Concrete.FunctionClause> visitWithBody(WithBodyContext ctx) {
    List<Concrete.FunctionClause> clauses = new ArrayList<>();
    if (ctx != null) {
      for (ClauseContext clauseCtx : ctx.clause()) {
        clauses.add(visitClause(clauseCtx));
      }
    }
    return clauses;
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
  public Concrete.Expression visitCaseExpr(CaseExprContext ctx) {
    List<Concrete.CaseArgument> caseArgs = new ArrayList<>();
    for (CaseArgContext caseArgCtx : ctx.caseArg()) {
      Expr2Context typeCtx = caseArgCtx.expr2();
      Concrete.Expression type = typeCtx == null ? null : visitExpr(typeCtx);
      CaseArgExprAsContext caseArgExprAs = caseArgCtx.caseArgExprAs();
      if (caseArgExprAs instanceof CaseArgExprContext) {
        CaseArgExprContext caseArgExpr = (CaseArgExprContext) caseArgExprAs;
        TerminalNode id = caseArgExpr.ID();
        caseArgs.add(new Concrete.CaseArgument(visitExpr(caseArgExpr.expr2()), id == null ? null : new ParsedLocalReferable(tokenPosition(id.getSymbol()), id.getText()), type));
      } else if (caseArgExprAs instanceof CaseArgElimContext) {
        CaseArgElimContext caseArgElim = (CaseArgElimContext) caseArgExprAs;
        TerminalNode id = caseArgElim.ID();
        TerminalNode applyHole = caseArgElim.APPLY_HOLE();
        Concrete.CaseArgument argument;
        if (id != null) {
          Position position = tokenPosition(id.getSymbol());
          argument = new Concrete.CaseArgument(new Concrete.ReferenceExpression(position,
              new NamedUnresolvedReference(position, id.getText())), type);
        } else
          argument = new Concrete.CaseArgument(new Concrete.ApplyHoleExpression(
              tokenPosition(applyHole.getSymbol())), type);
        caseArgs.add(argument);
      }
    }

    List<Concrete.FunctionClause> clauses = visitWithBody(ctx.withBody());
    Pair<Concrete.Expression,Concrete.Expression> returnPair = visitReturnExpr(ctx.returnExpr2());
    Concrete.Expression result = new Concrete.CaseExpression(tokenPosition(ctx.start), ctx.SCASE() != null, caseArgs, returnPair.proj1, returnPair.proj2, clauses);
    boolean isPEval = ctx.PEVAL() != null;
    boolean isEval = !isPEval && ctx.EVAL() != null;
    return isPEval || isEval ? new Concrete.EvalExpression(result.getData(), isPEval, result) : result;
  }

  @Override
  public Concrete.Expression visitCase(CaseContext ctx) {
    return visitCaseExpr(ctx.caseExpr());
  }

  @Override
  public Concrete.Expression visitCase2(Case2Context ctx) {
    return visitCaseExpr(ctx.caseExpr());
  }

  @Override
  public Concrete.LetClause visitLetClause(LetClauseContext ctx) {
    List<Concrete.Parameter> arguments = visitLamTeles(ctx.tele(), false);
    TypeAnnotationContext typeAnnotationCtx = ctx.typeAnnotation();
    Concrete.Expression resultType = typeAnnotationCtx == null ? null : visitExpr(typeAnnotationCtx.expr());

    TerminalNode id = ctx.ID();
    if (id != null) {
      return new Concrete.LetClause(new ParsedLocalReferable(tokenPosition(ctx.start), id.getText()), arguments, resultType, visitExpr(ctx.expr()));
    }

    return new Concrete.LetClause(((Concrete.Pattern) visit(ctx.atomPattern())).toConstructor(), resultType, visitExpr(ctx.expr()));
  }

  private Concrete.Expression visitIncompleteExpression(ParserRuleContext exprCtx, ParserRuleContext parentCtx) {
    if (exprCtx == null) {
      return new Concrete.IncompleteExpression(new Position(myModule.getModulePath(), parentCtx.stop.getLine(), parentCtx.stop.getCharPositionInLine() + parentCtx.stop.getText().length()));
    } else {
      return (Concrete.Expression) visit(exprCtx);
    }
  }

  @Override
  public Concrete.LetExpression visitLet(LetContext ctx) {
    List<Concrete.LetClause> clauses = new ArrayList<>();
    for (LetClauseContext clauseCtx : ctx.letClause()) {
      clauses.add(visitLetClause(clauseCtx));
    }

    LetKwContext letKw = ctx.letKw();
    return new Concrete.LetExpression(tokenPosition(ctx.start), letKw.HAVE() != null || letKw.HAVES() != null, letKw.LETS() != null || letKw.HAVES() != null, clauses, visitIncompleteExpression(ctx.expr(), ctx));
  }

  @Override
  public Object visitLet2(Let2Context ctx) {
    List<Concrete.LetClause> clauses = new ArrayList<>();
    for (LetClauseContext clauseCtx : ctx.letClause()) {
      clauses.add(visitLetClause(clauseCtx));
    }

    LetKwContext letKw = ctx.letKw();
    return new Concrete.LetExpression(tokenPosition(ctx.start), letKw.HAVE() != null || letKw.HAVES() != null, letKw.LETS() != null || letKw.HAVES() != null, clauses, visitIncompleteExpression(ctx.expr2(), ctx));
  }

  private Position tokenPosition(Token token) {
    return new Position(myModule.getModulePath(), token.getLine(), token.getCharPositionInLine());
  }
}
