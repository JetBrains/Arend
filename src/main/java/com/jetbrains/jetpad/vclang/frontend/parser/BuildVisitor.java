package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteClassReferable;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteLocatedReferable;
import com.jetbrains.jetpad.vclang.frontend.reference.InternalConcreteLocatedReferable;
import com.jetbrains.jetpad.vclang.frontend.reference.ParsedLocalReferable;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.Fixity;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.*;
import com.jetbrains.jetpad.vclang.util.Pair;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.parser.VcgrammarParser.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private final ModulePath myModule;
  private final ErrorReporter myErrorReporter;

  public BuildVisitor(ModulePath module, ErrorReporter errorReporter) {
    myModule = module;
    myErrorReporter = errorReporter;
  }

  private String getVar(AtomFieldsAccContext ctx) {
    if (!ctx.fieldAcc().isEmpty() || !(ctx.atom() instanceof AtomLiteralContext)) {
      return null;
    }
    LiteralContext literal = ((AtomLiteralContext) ctx.atom()).literal();
    if (literal instanceof UnknownContext) {
      return "_";
    }
    if (literal instanceof NameContext && ((NameContext) literal).ID() != null) {
      return ((NameContext) literal).ID().getText();
    }
    return null;
  }

  private boolean getVars(ExprContext expr, List<ParsedLocalReferable> vars) {
    if (!(expr instanceof AppContext && ((AppContext) expr).appExpr() instanceof AppArgumentContext && ((AppContext) expr).NEW() == null && ((AppContext) expr).implementStatements() == null)) {
      return false;
    }

    AppArgumentContext argCtx = (AppArgumentContext) ((AppContext) expr).appExpr();
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
    vars.add(new ParsedLocalReferable(tokenPosition(argCtx.atomFieldsAcc().start), var));

    for (ArgumentContext argument : argCtx.argument()) {
      if (argument instanceof ArgumentInfixContext) {
        vars.add(new ParsedLocalReferable(tokenPosition(((ArgumentInfixContext) argument).INFIX().getSymbol()), getInfixText(((ArgumentInfixContext) argument).INFIX())));
      } else if (argument instanceof ArgumentPostfixContext) {
        vars.add(new ParsedLocalReferable(tokenPosition(((ArgumentPostfixContext) argument).POSTFIX().getSymbol()), getPostfixText(((ArgumentPostfixContext) argument).POSTFIX())));
      } else if (argument instanceof ArgumentExplicitContext) {
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
      myErrorReporter.report(new ParserError(tokenPosition(expr.getStart()), "Expected a list of variables"));
      throw new ParseException();
    }
  }

  private String getInfixText(TerminalNode node) {
    String name = node.getText();
    return name.substring(1, name.length() - 1);
  }

  private String getPostfixText(TerminalNode node) {
    String name = node.getText();
    return name.substring(1, name.length());
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

  private void visitStatementList(List<StatementContext> statementCtxs, List<Group> subgroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    for (StatementContext statementCtx : statementCtxs) {
      try {
        Object statement = visitStatement(statementCtx, parent);
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

  private Object visitStatement(StatementContext statementCtx, ChildGroup parent) {
    if (statementCtx instanceof StatCmdContext) {
      return visitStatCmd((StatCmdContext) statementCtx, parent);
    } else if (statementCtx instanceof StatDefContext) {
      return visitDefinition(((StatDefContext) statementCtx).definition(), parent);
    } else {
      return null;
    }
  }

  @Override
  public FileGroup visitStatements(StatementsContext ctx) {
    List<Group> subgroups = new ArrayList<>();
    List<SimpleNamespaceCommand> namespaceCommands = new ArrayList<>();
    FileGroup parentGroup = new FileGroup(new ModuleReferable(myModule), subgroups, namespaceCommands);
    visitStatementList(ctx.statement(), subgroups, namespaceCommands, parentGroup);
    return parentGroup;
  }

  public ChildGroup visitDefinition(DefinitionContext ctx, ChildGroup parent) {
    if (ctx instanceof DefFunctionContext) {
      return visitDefFunction((DefFunctionContext) ctx, parent);
    } else if (ctx instanceof DefDataContext) {
      return visitDefData((DefDataContext) ctx, parent);
    } else if (ctx instanceof DefClassContext) {
      return visitDefClass((DefClassContext) ctx, parent);
    } else if (ctx instanceof DefInstanceContext) {
      return visitDefInstance((DefInstanceContext) ctx, parent);
    } else {
      if (ctx != null) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.start), "Unknown definition"));
      }
      throw new ParseException();
    }
  }

  private SimpleNamespaceCommand visitStatCmd(StatCmdContext ctx, ChildGroup parent) {
    NamespaceCommand.Kind kind = (NamespaceCommand.Kind) visit(ctx.nsCmd());
    List<String> path = visitAtomFieldsAccRef(ctx.atomFieldsAcc());
    if (path == null) {
      throw new ParseException();
    }

    List<SimpleNamespaceCommand.SimpleNameRenaming> openedReferences;
    if (ctx.nsUsing() == null) {
      openedReferences = Collections.emptyList();
    } else {
      openedReferences = new ArrayList<>(ctx.nsUsing().nsId().size());
      for (NsIdContext nsIdCtx : ctx.nsUsing().nsId()) {
        Position position = tokenPosition(nsIdCtx.ID(0).getSymbol());
        openedReferences.add(new SimpleNamespaceCommand.SimpleNameRenaming(position,
          new NamedUnresolvedReference(position, nsIdCtx.ID(0).getText()),
          nsIdCtx.precedence() == null ? null : visitPrecedence(nsIdCtx.precedence()),
          nsIdCtx.ID().size() < 2 ? null : nsIdCtx.ID(1).getText()));
      }
    }

    List<Referable> hiddenReferences = new ArrayList<>(ctx.ID().size());
    for (TerminalNode id : ctx.ID()) {
      hiddenReferences.add(new NamedUnresolvedReference(tokenPosition(id.getSymbol()), id.getText()));
    }

    return new SimpleNamespaceCommand(tokenPosition(ctx.start), kind, path, ctx.nsUsing() == null || ctx.nsUsing().USING() != null, openedReferences, hiddenReferences, parent);
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
    if (priority < 1 || priority > 9) {
      myErrorReporter.report(new ParserError(tokenPosition(ctx.NUMBER().getSymbol()), "Precedence out of range: " + priority));

      if (priority < 1) {
        priority = 1;
      } else {
        priority = 9;
      }
    }

    PrecedenceWithoutPriority prec = (PrecedenceWithoutPriority) visit(ctx.associativity());
    return new Precedence(prec.associativity, (byte) priority, prec.isInfix);
  }

  private class PrecedenceWithoutPriority {
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
    return (Concrete.Pattern) visit(ctx.atomPattern());
  }

  @Override
  public Concrete.Pattern visitPatternConstructor(PatternConstructorContext ctx) {
    if (ctx.atomPatternOrID().size() == 0) {
      return new Concrete.NamePattern(tokenPosition(ctx.start), new ParsedLocalReferable(tokenPosition(ctx.ID().getSymbol()), ctx.ID().getText()));
    } else {
      List<Concrete.Pattern> patterns = new ArrayList<>(ctx.atomPatternOrID().size());
      for (AtomPatternOrIDContext atomCtx : ctx.atomPatternOrID()) {
        patterns.add(visitAtomPattern(atomCtx));
      }
      return new Concrete.ConstructorPattern(tokenPosition(ctx.start), new NamedUnresolvedReference(tokenPosition(ctx.ID().getSymbol()), ctx.ID().getText()), patterns);
    }
  }

  private Concrete.Pattern visitAtomPattern(AtomPatternOrIDContext ctx) {
    return (Concrete.Pattern) visit(ctx);
  }

  @Override
  public Concrete.Pattern visitPatternExplicit(PatternExplicitContext ctx) {
    return ctx.pattern() == null ? new Concrete.EmptyPattern(tokenPosition(ctx.start)) : visitPattern(ctx.pattern());
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
    Position position = tokenPosition(ctx.getStart());
    return new Concrete.NamePattern(position, new ParsedLocalReferable(tokenPosition(ctx.ID().getSymbol()), ctx.ID().getText()));
  }

  @Override
  public Concrete.Pattern visitPatternAny(PatternAnyContext ctx) {
    Position position = tokenPosition(ctx.getStart());
    return new Concrete.NamePattern(position, null);
  }

  private ConcreteLocatedReferable makeReferable(Position position, String name, Precedence precedence, ChildGroup parent) {
    return parent instanceof FileGroup
      ? new ConcreteLocatedReferable(position, name, precedence, myModule)
      : new ConcreteLocatedReferable(position, name, precedence, (TCReferable) parent.getReferable(), true);
  }

  private StaticGroup visitDefInstance(DefInstanceContext ctx, ChildGroup parent) {
    List<Concrete.Parameter> parameters = visitFunctionParameters(ctx.tele());
    List<String> classPath = visitAtomFieldsAccRef(ctx.classCall().atomFieldsAcc());
    if (classPath == null) {
      throw new ParseException();
    }

    ConcreteLocatedReferable reference = makeReferable(tokenPosition(ctx.start), ctx.ID().getText(), Precedence.DEFAULT, parent);
    Position position = tokenPosition(ctx.classCall().start);
    reference.setDefinition(new Concrete.Instance(reference, parameters, new Concrete.ReferenceExpression(position, LongUnresolvedReference.make(position, classPath)), visitCoClauses(ctx.coClauses())));
    List<Group> subgroups = new ArrayList<>();
    List<SimpleNamespaceCommand> namespaceCommands = new ArrayList<>();
    StaticGroup resultGroup = new StaticGroup(reference, subgroups, namespaceCommands, parent);
    visitWhere(ctx.where(), subgroups, namespaceCommands, resultGroup);
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

  private void visitWhere(WhereContext ctx, List<Group> subgroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    if (ctx != null) {
      visitStatementList(ctx.statement(), subgroups, namespaceCommands, parent);
    }
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

  private StaticGroup visitDefFunction(DefFunctionContext ctx, ChildGroup parent) {
    Concrete.Expression resultType = ctx.expr() != null ? visitExpr(ctx.expr()) : null;
    Concrete.FunctionBody body;
    if (ctx.functionBody() instanceof WithElimContext) {
      WithElimContext elimCtx = ((WithElimContext) ctx.functionBody());
      body = new Concrete.ElimFunctionBody(tokenPosition(elimCtx.start), visitElim(elimCtx.elim()), visitClauses(elimCtx.clauses()));
    } else {
      body = new Concrete.TermFunctionBody(tokenPosition(ctx.start), visitExpr(((WithoutElimContext) ctx.functionBody()).expr()));
    }

    List<Group> subgroups = new ArrayList<>();
    List<SimpleNamespaceCommand> namespaceCommands = new ArrayList<>();
    ConcreteLocatedReferable referable = makeReferable(tokenPosition(ctx.start), ctx.ID().getText(), visitPrecedence(ctx.precedence()), parent);
    Concrete.Definition funDef = new Concrete.FunctionDefinition(referable, visitFunctionParameters(ctx.tele()), resultType, body);
    referable.setDefinition(funDef);
    StaticGroup resultGroup = new StaticGroup(referable, subgroups, namespaceCommands, parent);
    visitWhere(ctx.where(), subgroups, namespaceCommands, resultGroup);
    return resultGroup;
  }

  private List<Concrete.Parameter> visitFunctionParameters(List<TeleContext> teleCtx) {
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

  private StaticGroup visitDefData(DefDataContext ctx, ChildGroup parent) {
    final Concrete.UniverseExpression universe;
    if (ctx.expr() != null) {
      Concrete.Expression expr = visitExpr(ctx.expr());
      if (expr instanceof Concrete.UniverseExpression) {
        universe = (Concrete.UniverseExpression) expr;
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.expr().getStart()), "Specified type of the data definition is not a universe"));
        universe = null;
      }
    } else {
      universe = null;
    }

    List<InternalConcreteLocatedReferable> constructors = new ArrayList<>();
    List<Concrete.ReferenceExpression> eliminatedReferences = ctx.dataBody() instanceof DataClausesContext ? visitElim(((DataClausesContext) ctx.dataBody()).elim()) : null;
    ConcreteLocatedReferable referable = makeReferable(tokenPosition(ctx.start), ctx.ID().getText(), visitPrecedence(ctx.precedence()), parent);
    Concrete.DataDefinition dataDefinition = new Concrete.DataDefinition(referable, visitTeles(ctx.tele()), eliminatedReferences, ctx.TRUNCATED() != null, universe, new ArrayList<>());
    referable.setDefinition(dataDefinition);
    visitDataBody(ctx.dataBody(), dataDefinition, constructors);

    List<Group> subgroups = new ArrayList<>();
    List<SimpleNamespaceCommand> namespaceCommands = new ArrayList<>();
    DataGroup resultGroup = new DataGroup(referable, constructors, subgroups, namespaceCommands, parent);
    visitWhere(ctx.where(), subgroups, namespaceCommands, resultGroup);
    return resultGroup;
  }

  private void visitDataBody(DataBodyContext ctx, Concrete.DataDefinition def, List<InternalConcreteLocatedReferable> constructors) {
    if (ctx instanceof DataClausesContext) {
      for (ConstructorClauseContext clauseCtx : ((DataClausesContext) ctx).constructorClause()) {
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
        List<Concrete.FunctionClause> clauses;
        if (conCtx.elim() != null || !conCtx.clause().isEmpty()) {
          clauses = new ArrayList<>(conCtx.clause().size());
          for (ClauseContext clauseCtx : conCtx.clause()) {
            clauses.add(visitClause(clauseCtx));
          }
        } else {
          clauses = Collections.emptyList();
        }

        InternalConcreteLocatedReferable reference = new InternalConcreteLocatedReferable(tokenPosition(conCtx.start), conCtx.ID().getText(), visitPrecedence(conCtx.precedence()), true, def.getData());
        Concrete.Constructor constructor = new Concrete.Constructor(reference, def, visitTeles(conCtx.tele()), visitElim(conCtx.elim()), clauses);
        reference.setDefinition(constructor);
        constructors.add(reference);
        result.add(constructor);
      } catch (ParseException ignored) {

      }
    }
    return result;
  }

  private void visitInstanceStatements(List<ClassStatContext> ctx, List<Concrete.ClassField> fields, List<Concrete.ClassFieldImpl> implementations, List<Group> subgroups, Concrete.ClassDefinition parentClass, ChildGroup parent) {
    for (ClassStatContext statementCtx : ctx) {
      if (statementCtx == null) {
        continue;
      }

      try {
        if (statementCtx instanceof ClassFieldContext) {
          ClassFieldContext fieldCtx = (ClassFieldContext) statementCtx;
          List<Concrete.TypeParameter> parameters = visitTeles(fieldCtx.tele());
          Concrete.Expression type = visitExpr(fieldCtx.expr());
          if (!parameters.isEmpty()) {
            type = new Concrete.PiExpression(tokenPosition(fieldCtx.tele(0).start), parameters, type);
          }

          InternalConcreteLocatedReferable reference = new InternalConcreteLocatedReferable(tokenPosition(fieldCtx.start), fieldCtx.ID().getText(), visitPrecedence(fieldCtx.precedence()), true, parentClass.getData());
          Concrete.ClassField field = new Concrete.ClassField(reference, parentClass, type);
          reference.setDefinition(field);
          fields.add(field);
        } else if (statementCtx instanceof ClassImplementContext) {
          Concrete.ClassFieldImpl impl = visitClassImplement((ClassImplementContext) statementCtx);
          if (impl != null) {
            implementations.add(impl);
          }
        } else if (statementCtx instanceof ClassDefinitionContext) {
          subgroups.add(visitDefinition(((ClassDefinitionContext) statementCtx).definition(), parent));
        } else {
          myErrorReporter.report(new ParserError(tokenPosition(statementCtx.start), "Unknown class statement"));
        }
      } catch (ParseException ignored) {

      }
    }
  }

  private ClassGroup visitDefClass(DefClassContext ctx, ChildGroup parent) {
    WhereContext where = ctx.where();

    List<Concrete.ReferenceExpression> superClasses = new ArrayList<>(ctx.classCall().size());
    List<Concrete.ClassFieldImpl> implementations = Collections.emptyList();
    List<Group> staticSubgroups = where == null ? Collections.emptyList() : new ArrayList<>();
    List<SimpleNamespaceCommand> namespaceCommands = where == null ? Collections.emptyList() : new ArrayList<>();

    for (ClassCallContext classCallCtx : ctx.classCall()) {
      List<String> superClass = visitAtomFieldsAccRef(classCallCtx.atomFieldsAcc());
      if (superClass != null) {
        Position position = tokenPosition(classCallCtx.start);
        superClasses.add(new Concrete.ReferenceExpression(position, LongUnresolvedReference.make(position, superClass)));
      }
    }

    List<InternalConcreteLocatedReferable> fieldReferences = new ArrayList<>();
    Position pos = tokenPosition(ctx.start);
    String name = ctx.ID().getText();
    Precedence prec = visitPrecedence(ctx.precedence());
    ConcreteClassReferable reference = parent instanceof FileGroup
      ? new ConcreteClassReferable(pos, name, prec, fieldReferences, superClasses, parent, myModule)
      : new ConcreteClassReferable(pos, name, prec, fieldReferences, superClasses, parent, (TCReferable) parent.getReferable());
    Concrete.Definition classDefinition;
    ClassGroup resultGroup = null;
    if (ctx.classBody() instanceof ClassSynContext) {
      List<Concrete.ClassFieldSynonym> fieldSynonyms = new ArrayList<>();
      classDefinition = new Concrete.ClassSynonym(reference, superClasses, null, fieldSynonyms);

      if (!ctx.tele().isEmpty()) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.tele(0).start), "Class synonyms cannot have parameters"));
      }

      for (FieldSynContext fieldSyn : ((ClassSynContext) ctx.classBody()).fieldSyn()) {
        InternalConcreteLocatedReferable fieldSynRef = new InternalConcreteLocatedReferable(tokenPosition(fieldSyn.start), fieldSyn.ID(1).getText(), visitPrecedence(fieldSyn.precedence()), true, reference);
        Position position = tokenPosition(fieldSyn.ID(0).getSymbol());
        Concrete.ClassFieldSynonym concreteFieldSyn = new Concrete.ClassFieldSynonym(fieldSynRef, new Concrete.ReferenceExpression(position, new NamedUnresolvedReference(position, fieldSyn.ID(0).getText())), (Concrete.ClassSynonym) classDefinition);
        fieldSynRef.setDefinition(concreteFieldSyn);
        fieldSynonyms.add(concreteFieldSyn);
        fieldReferences.add(fieldSynRef);
      }
    } else {
      List<Concrete.ClassField> fields = new ArrayList<>();
      if (ctx.classBody() != null && !((ClassImplContext) ctx.classBody()).classStat().isEmpty()) {
        implementations = new ArrayList<>();
      }
      classDefinition = new Concrete.ClassDefinition(reference, superClasses, fields, implementations, false);

      Concrete.ClassField firstField = visitUniqueFieldTele(ctx.tele(), (Concrete.ClassDefinition) classDefinition);
      if (firstField != null) {
        fields.add(firstField);
        ((Concrete.ClassDefinition) classDefinition).setHasParameter();
      }

      if (ctx.classBody() != null && !((ClassImplContext) ctx.classBody()).classStat().isEmpty()) {
        List<Group> dynamicSubgroups = new ArrayList<>();
        resultGroup = new ClassGroup(reference, fieldReferences, dynamicSubgroups, staticSubgroups, namespaceCommands, parent);
        visitInstanceStatements(((ClassImplContext) ctx.classBody()).classStat(), fields, implementations, dynamicSubgroups, (Concrete.ClassDefinition) classDefinition, resultGroup);
      }

      for (Concrete.ClassField field : fields) {
        fieldReferences.add((InternalConcreteLocatedReferable) field.getData());
      }
    }

    reference.setDefinition(classDefinition);
    if (resultGroup == null) {
      resultGroup = new ClassGroup(reference, fieldReferences, Collections.emptyList(), staticSubgroups, namespaceCommands, parent);
    }
    visitWhere(where, staticSubgroups, namespaceCommands, resultGroup);
    return resultGroup;
  }

  private Concrete.ClassField visitUniqueFieldTele(List<TeleContext> teles, Concrete.ClassDefinition classDef) {
    if (teles.isEmpty()) {
      return null;
    }

    TypedExprContext typedExpr;
    if (teles.get(0) instanceof ExplicitContext) {
      typedExpr = ((ExplicitContext) teles.get(0)).typedExpr();
    } else
    if (teles.get(0) instanceof ImplicitContext) {
      typedExpr = ((ImplicitContext) teles.get(0)).typedExpr();
      myErrorReporter.report(new ParserError(tokenPosition(teles.get(0).start), "Class parameter must be explicit"));
    } else {
      myErrorReporter.report(new ParserError(tokenPosition(teles.get(0).start), "Expected a field name with a type"));
      return null;
    }

    List<ParsedLocalReferable> vars = new ArrayList<>(1);
    getVarList(((TypedContext) typedExpr).expr(0), vars);
    Concrete.Expression type = visitExpr(((TypedContext) typedExpr).expr(1));

    if (vars.isEmpty()) {
      return null;
    }
    if (teles.size() > 1 || vars.size() > 1) {
      myErrorReporter.report(new ParserError(tokenPosition(teles.get(1).start), "Class can have at most one parameter"));
    }

    InternalConcreteLocatedReferable referable = new InternalConcreteLocatedReferable(tokenPosition(teles.get(0).start), vars.get(0).textRepresentation(), Precedence.DEFAULT, false, classDef.getData());
    Concrete.ClassField field = new Concrete.ClassField(referable, classDef, type);
    referable.setDefinition(field);
    return field;
  }

  @Override
  public Concrete.ClassFieldImpl visitClassImplement(ClassImplementContext ctx) {
    List<Concrete.Parameter> parameters = visitLamTeles(ctx.tele());
    Concrete.Expression term = visitExpr(ctx.expr());
    if (!parameters.isEmpty()) {
      term = new Concrete.LamExpression(tokenPosition(ctx.tele(0).start), parameters, term);
    }

    List<String> path = visitAtomFieldsAccRef(ctx.atomFieldsAcc());
    return path == null ? null : new Concrete.ClassFieldImpl(tokenPosition(ctx.start), LongUnresolvedReference.make(tokenPosition(ctx.atomFieldsAcc().start), path), term);
  }

  @Override
  public Concrete.ReferenceExpression visitName(NameContext ctx) {
    return new Concrete.ReferenceExpression(tokenPosition(ctx.start), new NamedUnresolvedReference(tokenPosition(ctx.ID().getSymbol()), ctx.ID().getText()));
  }

  @Override
  public Concrete.InferHoleExpression visitUnknown(UnknownContext ctx) {
    return new Concrete.InferHoleExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.GoalExpression visitGoal(GoalContext ctx) {
    return new Concrete.GoalExpression(tokenPosition(ctx.start), ctx.ID() == null ? null : ctx.ID().getText(), ctx.expr() == null ? null : visitExpr(ctx.expr()));
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
      List<ParsedLocalReferable> vars = new ArrayList<>();
      if (typedExpr instanceof TypedContext) {
        getVarList(((TypedContext) typedExpr).expr(0), vars);
        typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
      } else if (typedExpr instanceof NotTypedContext) {
        getVarList(((NotTypedContext) typedExpr).expr(), vars);
        for (ParsedLocalReferable var : vars) {
          arguments.add(new Concrete.NameParameter(var.getPosition(), explicit, var));
        }
        typeExpr = null;
      } else {
        throw new IllegalStateException();
      }
      if (typeExpr != null) {
        arguments.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, vars, typeExpr));
      }
    } else {
      boolean ok = tele instanceof TeleLiteralContext;
      if (ok) {
        LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
        if (literalContext instanceof NameContext && ((NameContext) literalContext).ID() != null) {
          TerminalNode id = ((NameContext) literalContext).ID();
          arguments.add(new Concrete.NameParameter(tokenPosition(id.getSymbol()), true, new ParsedLocalReferable(tokenPosition(id.getSymbol()), id.getText())));
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
  public Concrete.LamExpression visitLam(LamContext ctx) {
    return new Concrete.LamExpression(tokenPosition(ctx.getStart()), visitLamTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  private Concrete.Expression visitAppExpr(AppExprContext ctx) {
    return (Concrete.Expression) visit(ctx);
  }

  @Override
  public Concrete.Expression visitAppArgument(AppArgumentContext ctx) {
    Concrete.Expression expr = visitAtomFieldsAcc(ctx.atomFieldsAcc());
    if (!ctx.onlyLevelAtom().isEmpty()) {
      if (expr instanceof Concrete.ReferenceExpression) {
        Object obj1 = ctx.onlyLevelAtom().isEmpty() ? null : visit(ctx.onlyLevelAtom(0));
        Object obj2 = ctx.onlyLevelAtom().size() < 2 ? null : visit(ctx.onlyLevelAtom(1));
        if (ctx.onlyLevelAtom().size() > 2 || obj1 instanceof Pair && obj2 != null || obj2 instanceof Pair) {
          myErrorReporter.report(new ParserError(tokenPosition(ctx.onlyLevelAtom(0).start), "too many level specifications"));
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

        expr = Concrete.ReferenceExpression.make(expr.getData(), ((Concrete.ReferenceExpression) expr).getReferent(), level1, level2);
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.onlyLevelAtom(0).start), "Level annotations are allowed only after a reference"));
      }
    }

    if (ctx.argument().isEmpty()) {
      return expr;
    }

    List<Concrete.BinOpSequenceElem> sequence = new ArrayList<>(ctx.argument().size());
    sequence.add(new Concrete.BinOpSequenceElem(expr, Fixity.NONFIX, true));
    for (ArgumentContext argumentCtx : ctx.argument()) {
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
    return new Concrete.BinOpSequenceElem(visitAtomFieldsAcc(atomFieldsAcc), atomFieldsAcc.atom() instanceof AtomLiteralContext && ((AtomLiteralContext) atomFieldsAcc.atom()).literal() instanceof NameContext ? Fixity.UNKNOWN : Fixity.NONFIX, true);
  }

  @Override
  public Concrete.BinOpSequenceElem visitArgumentNew(ArgumentNewContext ctx) {
    return new Concrete.BinOpSequenceElem(visitNew(ctx.NEW(), ctx.appExpr(), ctx.implementStatements()), Fixity.NONFIX, true);
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
  public Concrete.BinOpSequenceElem visitArgumentInfix(ArgumentInfixContext ctx) {
    Position position = tokenPosition(ctx.INFIX().getSymbol());
    return new Concrete.BinOpSequenceElem(new Concrete.ReferenceExpression(position, new NamedUnresolvedReference(position, getInfixText(ctx.INFIX()))), Fixity.INFIX, true);
  }

  @Override
  public Concrete.BinOpSequenceElem visitArgumentPostfix(ArgumentPostfixContext ctx) {
    Position position = tokenPosition(ctx.POSTFIX().getSymbol());
    return new Concrete.BinOpSequenceElem(new Concrete.ReferenceExpression(position, new NamedUnresolvedReference(position, getPostfixText(ctx.POSTFIX()))), Fixity.POSTFIX, true);
  }

  @Override
  public Concrete.ClassFieldImpl visitCoClause(CoClauseContext ctx) {
    List<String> path = visitAtomFieldsAccRef(ctx.atomFieldsAcc());
    if (path == null) {
      return null;
    }

    Position position = tokenPosition(ctx.atomFieldsAcc().start);
    List<Concrete.Parameter> parameters = visitLamTeles(ctx.tele());
    Concrete.Expression term = visitExpr(ctx.expr());
    if (!parameters.isEmpty()) {
      term = new Concrete.LamExpression(tokenPosition(ctx.tele(0).start), parameters, term);
    }

    return new Concrete.ClassFieldImpl(position, LongUnresolvedReference.make(position, path), term);
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
    Position position = tokenPosition(ctx.getStart());
    Concrete.LevelExpression lp;
    Concrete.LevelExpression lh;

    String text = ctx.UNIVERSE().getText().substring("\\Type".length());
    lp = text.isEmpty() ? null : new Concrete.NumberLevelExpression(tokenPosition(ctx.UNIVERSE().getSymbol()), Integer.parseInt(text));

    if (!ctx.maybeLevelAtom().isEmpty()) {
      if (lp == null) {
        lp = visitLevel(ctx.maybeLevelAtom(0));
        lh = null;
      } else {
        lh = visitLevel(ctx.maybeLevelAtom(0));
      }

      if (ctx.maybeLevelAtom().size() >= 2) {
        if (lh == null) {
          lh = visitLevel(ctx.maybeLevelAtom(1));
        } else {
          myErrorReporter.report(new ParserError(tokenPosition(ctx.maybeLevelAtom(1).start), "h-level is already specified"));
        }
      }
    } else {
      lh = null;
    }

    return new Concrete.UniverseExpression(position, lp, lh);
  }

  @Override
  public Concrete.UniverseExpression visitTruncatedUniverse(TruncatedUniverseContext ctx) {
    Position position = tokenPosition(ctx.getStart());
    Concrete.LevelExpression pLevel;

    String text = ctx.TRUNCATED_UNIVERSE().getText();
    text = text.substring(text.indexOf('-') + "-Type".length());
    if (text.isEmpty()) {
      pLevel = ctx.maybeLevelAtom() == null ? null : visitLevel(ctx.maybeLevelAtom());
    } else {
      pLevel = new Concrete.NumberLevelExpression(tokenPosition(ctx.TRUNCATED_UNIVERSE().getSymbol()), Integer.parseInt(text));
      if (ctx.maybeLevelAtom() instanceof WithLevelAtomContext) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.maybeLevelAtom().start), "p-level is already specified"));
      }
    }

    return new Concrete.UniverseExpression(position, pLevel, parseTruncatedUniverse(ctx.TRUNCATED_UNIVERSE()));
  }

  @Override
  public Concrete.UniverseExpression visitSetUniverse(SetUniverseContext ctx) {
    Position position = tokenPosition(ctx.getStart());
    Concrete.LevelExpression pLevel;

    String text = ctx.SET().getText().substring("\\Set".length());
    if (text.isEmpty()) {
      pLevel = ctx.maybeLevelAtom() == null ? null : visitLevel(ctx.maybeLevelAtom());
    } else {
      pLevel = new Concrete.NumberLevelExpression(tokenPosition(ctx.SET().getSymbol()), Integer.parseInt(text));
      if (ctx.maybeLevelAtom() instanceof WithLevelAtomContext) {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.maybeLevelAtom().start), "p-level is already specified"));
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
    Position position = tokenPosition(ctx.getStart());
    String text = ctx.SET().getText().substring("\\Set".length());
    Concrete.LevelExpression pLevel = text.isEmpty() ? null : new Concrete.NumberLevelExpression(tokenPosition(ctx.SET().getSymbol()), Integer.parseInt(text));
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
        List<ParsedLocalReferable> vars = new ArrayList<>();
        getVarList(((TypedContext) typedExpr).expr(0), vars);
        arguments.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, vars, visitExpr(((TypedContext) typedExpr).expr(1))));
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
    return new Concrete.NumericLiteral(tokenPosition(ctx.NUMBER().getSymbol()), new BigInteger(ctx.NUMBER().getText(), 10));
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    List<Concrete.TypeParameter> args = visitTeles(ctx.tele());
    for (Concrete.TypeParameter arg : args) {
      if (!arg.getExplicit()) {
        myErrorReporter.report(new ParserError((Position) arg.getData(), "Fields in sigma types must be explicit"));
      }
    }
    return new Concrete.SigmaExpression(tokenPosition(ctx.getStart()), args);
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    return new Concrete.PiExpression(tokenPosition(ctx.getStart()), visitTeles(ctx.tele()), visitExpr(ctx.expr()));
  }

  public Concrete.Expression visitApp(AppContext ctx) {
    Concrete.Expression expr = visitNew(ctx.NEW(), ctx.appExpr(), ctx.implementStatements());
    if (!ctx.argument().isEmpty()) {
      List<Concrete.BinOpSequenceElem> sequence = new ArrayList<>(ctx.argument().size() + 1);
      sequence.add(new Concrete.BinOpSequenceElem(expr, Fixity.NONFIX, true));
      for (ArgumentContext argCtx : ctx.argument()) {
        sequence.add(visitArgument(argCtx));
      }
      expr = new Concrete.BinOpSequenceExpression(expr.getData(), sequence);
    }
    return expr;
  }

  private Concrete.Expression visitNew(TerminalNode newNode, AppExprContext appCtx, ImplementStatementsContext implCtx) {
    Concrete.Expression expr = visitAppExpr(appCtx);

    if (implCtx != null) {
      expr = new Concrete.ClassExtExpression(tokenPosition(appCtx.start), expr, visitCoClauses(implCtx.coClause()));
    }

    if (newNode != null) {
      expr = new Concrete.NewExpression(tokenPosition(newNode.getSymbol()), expr);
    }

    return expr;
  }

  private List<String> visitAtomFieldsAccRef(AtomFieldsAccContext ctx) {
    if (ctx.atom() instanceof AtomLiteralContext && ((AtomLiteralContext) ctx.atom()).literal() instanceof NameContext) {
      List<String> result = new ArrayList<>();
      result.add(((NameContext) ((AtomLiteralContext) ctx.atom()).literal()).ID().getText());
      boolean ok = true;
      for (FieldAccContext fieldAccCtx : ctx.fieldAcc()) {
        if (fieldAccCtx instanceof ClassFieldAccContext) {
          result.add(((ClassFieldAccContext) fieldAccCtx).ID().getText());
        } else {
          ok = false;
          break;
        }
      }
      if (ok) {
        return result;
      }
    }

    myErrorReporter.report(new ParserError(tokenPosition(ctx.start), "Expected a reference"));
    return null;
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    if (ctx.fieldAcc().isEmpty()) {
      return visitExpr(ctx.atom());
    }

    Concrete.Expression expression = null;
    Token errorToken = null;
    int i = 0;
    if (ctx.fieldAcc().get(0) instanceof ClassFieldAccContext) {
      if (ctx.atom() instanceof AtomLiteralContext && ((AtomLiteralContext) ctx.atom()).literal() instanceof NameContext) {
        String name = ((NameContext) ((AtomLiteralContext) ctx.atom()).literal()).ID().getText();
        List<String> path = new ArrayList<>();
        for (; i < ctx.fieldAcc().size(); i++) {
          if (!(ctx.fieldAcc().get(i) instanceof ClassFieldAccContext)) {
            break;
          }
          path.add(((ClassFieldAccContext) ctx.fieldAcc().get(i)).ID().getText());
        }
        expression = new Concrete.ReferenceExpression(tokenPosition(ctx.start), new LongUnresolvedReference(tokenPosition(ctx.start), name, path));
      } else {
        errorToken = ctx.start;
      }
    } else {
      expression = visitExpr(ctx.atom());
    }

    if (errorToken == null) {
      for (; i < ctx.fieldAcc().size(); i++) {
        FieldAccContext fieldAccCtx = ctx.fieldAcc().get(i);
        if (fieldAccCtx instanceof ClassFieldAccContext) {
          errorToken = fieldAccCtx.start;
          break;
        } else if (fieldAccCtx instanceof SigmaFieldAccContext) {
          expression = new Concrete.ProjExpression(tokenPosition(fieldAccCtx.start), expression, Integer.parseInt(((SigmaFieldAccContext) fieldAccCtx).NUMBER().getText()) - 1);
        } else {
          throw new IllegalStateException();
        }
      }
    }

    if (errorToken != null) {
      myErrorReporter.report(new ParserError(tokenPosition(errorToken), "Field accessors can be applied only to identifiers"));
      throw new ParseException();
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
    List<Concrete.Expression> elimExprs = new ArrayList<>(ctx.expr().size());
    for (ExprContext exprCtx : ctx.expr()) {
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
    return new Concrete.LetClause(new ParsedLocalReferable(tokenPosition(ctx.ID().getSymbol()), ctx.ID().getText()), arguments, resultType, visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.LetExpression visitLet(LetContext ctx) {
    List<Concrete.LetClause> clauses = new ArrayList<>();
    for (LetClauseContext clauseCtx : ctx.letClause()) {
      clauses.add(visitLetClause(clauseCtx));
    }

    return new Concrete.LetExpression(tokenPosition(ctx.getStart()), clauses, visitExpr(ctx.expr()));
  }

  private Position tokenPosition(Token token) {
    return new Position(myModule, token.getLine(), token.getCharPositionInLine());
  }
}
