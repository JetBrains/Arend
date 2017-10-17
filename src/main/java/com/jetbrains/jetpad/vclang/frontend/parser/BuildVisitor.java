package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.frontend.reference.LocalReference;
import com.jetbrains.jetpad.vclang.frontend.reference.ModuleReference;
import com.jetbrains.jetpad.vclang.frontend.term.group.*;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.LongUnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleUnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.reference.NamedUnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteLevelExpressionVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.parser.VcgrammarParser.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private final SourceId myModule;
  private final ErrorReporter myErrorReporter;

  public BuildVisitor(SourceId module, ErrorReporter errorReporter) {
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
    if (literal instanceof NameContext && ((NameContext) literal).prefix().PREFIX() != null) {
      return ((NameContext) literal).prefix().PREFIX().getText();
    }
    return null;
  }

  private boolean getVars(BinOpArgumentContext expr, List<LocalReference> vars) {
    if (expr.maybeNew() instanceof WithNewContext || expr.implementStatements() != null) {
      return false;
    }

    String firstArg = getVar(expr.atomFieldsAcc());
    if (firstArg == null) {
      return false;
    }
    if (firstArg.equals("_")) {
      firstArg = null;
    }

    vars.add(new LocalReference(tokenPosition(expr.atomFieldsAcc().start), firstArg));
    for (ArgumentContext argument : expr.argument()) {
      if (!(argument instanceof ArgumentExplicitContext)) {
        return false;
      }

      String arg = getVar(((ArgumentExplicitContext) argument).atomFieldsAcc());
      if (arg == null) {
        return false;
      }
      if (arg.equals("_")) {
        arg = null;
      }

      vars.add(new LocalReference(tokenPosition(((ArgumentExplicitContext) argument).atomFieldsAcc().start), arg));
    }
    return true;
  }

  private boolean getVars(ExprContext expr, List<LocalReference> vars) {
    if (!(expr instanceof BinOpContext && ((BinOpContext) expr).binOpArg() instanceof BinOpArgumentContext && ((BinOpContext) expr).postfix().isEmpty() && ((BinOpArgumentContext) ((BinOpContext) expr).binOpArg()).onlyLevelAtom().isEmpty())) {
      return false;
    }

    for (BinOpLeftContext leftCtx : ((BinOpContext) expr).binOpLeft()) {
      if (!(leftCtx.binOpArg() instanceof BinOpArgumentContext && leftCtx.postfix().isEmpty() && leftCtx.infix().INFIX() != null && ((BinOpArgumentContext) leftCtx.binOpArg()).onlyLevelAtom().isEmpty())) {
        return false;
      }
      if (!getVars((BinOpArgumentContext) leftCtx.binOpArg(), vars)) {
        return false;
      }

      vars.add(new LocalReference(tokenPosition(leftCtx.infix().start), leftCtx.infix().INFIX().getText()));
    }

    return getVars((BinOpArgumentContext) ((BinOpContext) expr).binOpArg(), vars);
  }

  private void getVarList(ExprContext expr, List<TerminalNode> infixList, List<LocalReference> vars) {
    if (getVars(expr, vars)) {
      for (TerminalNode infix : infixList) {
        vars.add(new LocalReference(tokenPosition(infix.getSymbol()), infix.getText()));
      }
    } else {
      myErrorReporter.report(new ParserError(tokenPosition(expr.getStart()), "Expected a list of variables"));
      throw new ParseException();
    }
  }

  private List<LocalReference> getVars(TypedVarsContext ctx) {
    List<LocalReference> result = new ArrayList<>(ctx.id().size() + 1);
    result.add(new LocalReference(tokenPosition(ctx.INFIX().getSymbol()), ctx.INFIX().getText()));
    for (IdContext idCtx : ctx.id()) {
      result.add(new LocalReference(tokenPosition(idCtx.start), idCtx.getText()));
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
    return new Concrete.ModuleCallExpression(tokenPosition(ctx.getStart()), new ModulePath(getModulePath(ctx.MODULE_PATH().getText())));
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
  public Group visitStatements(StatementsContext ctx) {
    List<Group> subgroups = new ArrayList<>();
    List<SimpleNamespaceCommand> namespaceCommands = new ArrayList<>();
    StaticGroup parentGroup = new StaticGroup(new ModuleReference(myModule.getModulePath()), subgroups, namespaceCommands, null);
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
    } else if (ctx instanceof DefClassViewContext) {
      return visitDefClassView((DefClassViewContext) ctx, parent);
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
    List<String> modulePath = ctx.nsCmdRoot().MODULE_PATH() == null ? null : getModulePath(ctx.nsCmdRoot().MODULE_PATH().getText());
    String name = ctx.nsCmdRoot().id() == null ? null : visitId(ctx.nsCmdRoot().id());
    if (modulePath == null && name == null) {
      throw new IllegalStateException();
    }

    List<String> path = new ArrayList<>();
    for (FieldAccContext fieldAccContext : ctx.fieldAcc()) {
      if (fieldAccContext instanceof ClassFieldAccContext) {
        path.add(visitId(((ClassFieldAccContext) fieldAccContext).id()));
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(fieldAccContext.getStart()), "Expected a name"));
      }
    }

    List<Referable> names;
    if (!ctx.id().isEmpty()) {
      names = new ArrayList<>(ctx.id().size());
      for (IdContext idCtx : ctx.id()) {
        names.add(new NamedUnresolvedReference(tokenPosition(idCtx.start), visitId(idCtx)));
      }
    } else {
      names = null;
    }
    return new SimpleNamespaceCommand(tokenPosition(ctx.start), kind, modulePath != null ? new ModuleUnresolvedReference(tokenPosition(ctx.nsCmdRoot().MODULE_PATH().getSymbol()), new ModulePath(modulePath), path) : LongUnresolvedReference.make(tokenPosition(ctx.nsCmdRoot().id().start), name, path), ctx.hidingOpt() instanceof WithHidingContext, names, parent);
  }

  @Override
  public NamespaceCommand.Kind visitOpenCmd(OpenCmdContext ctx) {
    return NamespaceCommand.Kind.OPEN;
  }

  @Override
  public NamespaceCommand.Kind visitExportCmd(ExportCmdContext ctx) {
    return NamespaceCommand.Kind.EXPORT;
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

    return new Precedence((Precedence.Associativity) visit(ctx.associativity()), (byte) priority);
  }

  @Override
  public Precedence.Associativity visitNonAssoc(NonAssocContext ctx) {
    return Precedence.Associativity.NON_ASSOC;
  }

  @Override
  public Precedence.Associativity visitLeftAssoc(LeftAssocContext ctx) {
    return Precedence.Associativity.LEFT_ASSOC;
  }

  @Override
  public Precedence.Associativity visitRightAssoc(RightAssocContext ctx) {
    return Precedence.Associativity.RIGHT_ASSOC;
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
      return new Concrete.NamePattern(tokenPosition(ctx.start), new LocalReference(tokenPosition(ctx.prefix().start), visitPrefix(ctx.prefix())));
    } else {
      List<Concrete.Pattern> patterns = new ArrayList<>(ctx.atomPatternOrID().size());
      for (AtomPatternOrIDContext atomCtx : ctx.atomPatternOrID()) {
        patterns.add(visitAtomPattern(atomCtx));
      }
      return new Concrete.ConstructorPattern(tokenPosition(ctx.start), new NamedUnresolvedReference(tokenPosition(ctx.prefix().start), visitPrefix(ctx.prefix())), patterns);
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
    return new Concrete.NamePattern(position, new LocalReference(tokenPosition(ctx.prefix().start), visitPrefix(ctx.prefix())));
  }

  @Override
  public Concrete.Pattern visitPatternAny(PatternAnyContext ctx) {
    Position position = tokenPosition(ctx.getStart());
    return new Concrete.NamePattern(position, null);
  }

  private ClassViewGroup visitDefClassView(DefClassViewContext ctx, ChildGroup parent) {
    List<Concrete.ClassViewField> fields = new ArrayList<>(ctx.classViewField().size());

    Concrete.Expression expr = visitExpr(ctx.expr());
    if (!(expr instanceof Concrete.ReferenceExpression)) {
      myErrorReporter.report(new ParserError((Position) expr.getData(), "Expected a class"));
      throw new ParseException();
    }

    GlobalReference reference = new GlobalReference(tokenPosition(ctx.start), visitId(ctx.id(0)), Precedence.DEFAULT);
    Concrete.ClassView classView = new Concrete.ClassView(reference, (Concrete.ReferenceExpression) expr, new NamedUnresolvedReference(tokenPosition(ctx.id(1).start), visitId(ctx.id(1))), fields);
    reference.setDefinition(classView);

    List<GlobalReference> fieldReferences = new ArrayList<>(ctx.classViewField().size());
    for (ClassViewFieldContext classViewFieldContext : ctx.classViewField()) {
      fieldReferences.add(visitClassViewField(classViewFieldContext, classView));
    }

    return new ClassViewGroup(reference, fieldReferences, parent);
  }

  private GlobalReference visitClassViewField(ClassViewFieldContext ctx, Concrete.ClassView classView) {
    String underlyingField = visitId(ctx.id(0));
    GlobalReference reference = new GlobalReference(tokenPosition(ctx.id(0).start), ctx.id().size() > 1 ? visitId(ctx.id(1)) : underlyingField, visitPrecedence(ctx.precedence()));
    Concrete.ClassViewField result = new Concrete.ClassViewField(reference, new NamedUnresolvedReference(tokenPosition(ctx.id(0).start), underlyingField), classView);
    reference.setDefinition(result);
    return reference;
  }

  private StaticGroup visitDefInstance(DefInstanceContext ctx, ChildGroup parent) {
    List<Concrete.Parameter> arguments = visitFunctionArguments(ctx.tele());
    Concrete.Expression term = visitExpr(ctx.expr());
    if (term instanceof Concrete.NewExpression) {
      Concrete.Expression type = ((Concrete.NewExpression) term).getExpression();
      if (type instanceof Concrete.ClassExtExpression) {
        Concrete.ClassExtExpression classExt = (Concrete.ClassExtExpression) type;
        if (classExt.getBaseClassExpression() instanceof Concrete.ReferenceExpression) {
          GlobalReference reference = new GlobalReference(tokenPosition(ctx.start), visitId(ctx.id()), Precedence.DEFAULT);
          reference.setDefinition(new Concrete.Instance(ctx.defaultInst() instanceof WithDefaultContext, reference, arguments, (Concrete.ReferenceExpression) classExt.getBaseClassExpression(), classExt.getStatements()));
          return new StaticGroup(reference, Collections.emptyList(), Collections.emptyList(), parent); // TODO[classes]: add static subgroups
        }
      }
    }

    myErrorReporter.report(new ParserError(tokenPosition(ctx.expr().getStart()), "Expected a class view extension"));
    throw new ParseException();
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
    GlobalReference reference = new GlobalReference(tokenPosition(ctx.start), visitId(ctx.id()), visitPrecedence(ctx.precedence()));
    Concrete.Definition funDef = new Concrete.FunctionDefinition(reference, visitFunctionArguments(ctx.tele()), resultType, body);
    reference.setDefinition(funDef);
    StaticGroup resultGroup = new StaticGroup(reference, subgroups, namespaceCommands, parent);
    visitWhere(ctx.where(), subgroups, namespaceCommands, resultGroup);
    return resultGroup;
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

    List<GlobalReference> constructors = new ArrayList<>();
    List<Concrete.ReferenceExpression> eliminatedReferences = ctx.dataBody() instanceof DataClausesContext ? visitElim(((DataClausesContext) ctx.dataBody()).elim()) : null;
    GlobalReference reference = new GlobalReference(tokenPosition(ctx.start), visitId(ctx.id()), visitPrecedence(ctx.precedence()));
    Concrete.DataDefinition dataDefinition = new Concrete.DataDefinition(reference, visitTeles(ctx.tele()), eliminatedReferences, ctx.isTruncated() instanceof TruncatedContext, universe, new ArrayList<>());
    reference.setDefinition(dataDefinition);
    visitDataBody(ctx.dataBody(), dataDefinition, constructors);
    return new DataGroup(reference, constructors, Collections.emptyList(), Collections.emptyList(), parent); // TODO[classes]: add static subgroups
  }

  private void visitDataBody(DataBodyContext ctx, Concrete.DataDefinition def, List<GlobalReference> constructors) {
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

  private List<Concrete.Constructor> visitConstructors(List<ConstructorContext> conContexts, Concrete.DataDefinition def, List<GlobalReference> constructors) {
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

        GlobalReference reference = new GlobalReference(tokenPosition(conCtx.start), visitId(conCtx.id()), visitPrecedence(conCtx.precedence()));
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
          GlobalReference reference = new GlobalReference(tokenPosition(fieldCtx.start), visitId(fieldCtx.id()), visitPrecedence(fieldCtx.precedence()));
          Concrete.ClassField field = new Concrete.ClassField(reference, parentClass, visitExpr(fieldCtx.expr()));
          reference.setDefinition(field);
          fields.add(field);
        } else if (statementCtx instanceof ClassImplementContext) {
          implementations.add(visitClassImplement((ClassImplementContext) statementCtx));
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
    List<Concrete.TypeParameter> polyParameters = visitTeles(ctx.tele());
    List<Concrete.ReferenceExpression> superClasses = ctx.atomFieldsAcc().isEmpty() ? Collections.emptyList() : new ArrayList<>(ctx.atomFieldsAcc().size());
    List<Concrete.ClassField> fields = ctx.classStat().isEmpty() ? Collections.emptyList() : new ArrayList<>();
    List<Concrete.ClassFieldImpl> implementations = ctx.classStat().isEmpty() ? Collections.emptyList() : new ArrayList<>();
    List<Group> staticSubgroups = new ArrayList<>();
    List<SimpleNamespaceCommand> namespaceCommands = new ArrayList<>();

    for (AtomFieldsAccContext exprCtx : ctx.atomFieldsAcc()) {
      Concrete.Expression superClass = visitAtomFieldsAcc(exprCtx);
      if (!(superClass instanceof Concrete.ReferenceExpression)) {
        myErrorReporter.report(new ParserError((Position) superClass.getData(), "Expected a class"));
        throw new ParseException();
      }
      superClasses.add((Concrete.ReferenceExpression) superClass);
    }

    GlobalReference reference = new GlobalReference(tokenPosition(ctx.start), visitId(ctx.id()), visitPrecedence(ctx.precedence()));
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(reference, polyParameters, superClasses, fields, implementations);
    reference.setDefinition(classDefinition);

    ClassGroup resultGroup;
    if (!ctx.classStat().isEmpty()) {
      List<Group> dynamicSubgroups = ctx.classStat().isEmpty() ? Collections.emptyList() : new ArrayList<>();
      List<GlobalReference> fieldReferences = new ArrayList<>(fields.size());
      resultGroup = new ClassGroup(reference, dynamicSubgroups, fieldReferences, staticSubgroups, namespaceCommands, parent);
      visitInstanceStatements(ctx.classStat(), fields, implementations, dynamicSubgroups, classDefinition, resultGroup);
      for (Concrete.ClassField field : fields) {
        fieldReferences.add((GlobalReference) field.getData());
      }
    } else {
      resultGroup = new ClassGroup(reference, Collections.emptyList(), Collections.emptyList(), staticSubgroups, namespaceCommands, parent);
    }

    visitWhere(ctx.where(), staticSubgroups, namespaceCommands, resultGroup);
    return resultGroup;
  }

  @Override
  public Concrete.ClassFieldImpl visitClassImplement(ClassImplementContext ctx) {
    return new Concrete.ClassFieldImpl(tokenPosition(ctx.start), new NamedUnresolvedReference(tokenPosition(ctx.id().start), visitId(ctx.id())), visitExpr(ctx.expr()));
  }

  @Override
  public Concrete.ReferenceExpression visitName(NameContext ctx) {
    return new Concrete.ReferenceExpression(tokenPosition(ctx.start), new NamedUnresolvedReference(tokenPosition(ctx.prefix().start), visitPrefix(ctx.prefix())));
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
      List<LocalReference> vars;
      if (typedExpr instanceof TypedVarsContext) {
        vars = getVars((TypedVarsContext) typedExpr);
        typeExpr = visitExpr(((TypedVarsContext) typedExpr).expr());
      } else {
        vars = new ArrayList<>();
        if (typedExpr instanceof TypedContext) {
          getVarList(((TypedContext) typedExpr).expr(0), ((TypedContext) typedExpr).INFIX(), vars);
          typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
        } else if (typedExpr instanceof NotTypedContext) {
          getVarList(((NotTypedContext) typedExpr).expr(), Collections.emptyList(), vars);
          for (LocalReference var : vars) {
            arguments.add(new Concrete.NameParameter(var.getPosition(), explicit, var));
          }
          typeExpr = null;
        } else {
          throw new IllegalStateException();
        }
      }
      if (typeExpr != null) {
        arguments.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, vars, typeExpr));
      }
    } else {
      boolean ok = tele instanceof TeleLiteralContext;
      if (ok) {
        LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
        if (literalContext instanceof NameContext && ((NameContext) literalContext).prefix().PREFIX() != null) {
          TerminalNode id = ((NameContext) literalContext).prefix().PREFIX();
          arguments.add(new Concrete.NameParameter(tokenPosition(id.getSymbol()), true, new LocalReference(tokenPosition(id.getSymbol()), id.getText())));
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

  @Override
  public Concrete.Expression visitBinOpArgument(BinOpArgumentContext ctx) {
    Concrete.Expression expr = visitAtomFieldsAcc(ctx.atomFieldsAcc());
    if (!ctx.onlyLevelAtom().isEmpty()) {
      if (expr instanceof Concrete.ReferenceExpression) {
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

        expr = new Concrete.ReferenceExpression(expr.getData(), ((Concrete.ReferenceExpression) expr).getReferent(), pLevel, hLevel);
      } else {
        myErrorReporter.report(new ParserError(tokenPosition(ctx.onlyLevelAtom(0).start), "Level annotations are allowed only after a reference"));
      }
    }

    expr = visitAtoms(expr, ctx.argument());

    ImplementStatementsContext implCtx = ctx.implementStatements();
    if (implCtx != null) {
      List<Concrete.ClassFieldImpl> implementStatements = new ArrayList<>(implCtx.implementStatement().size());
      for (ImplementStatementContext implementStatement : implCtx.implementStatement()) {
        implementStatements.add(new Concrete.ClassFieldImpl(tokenPosition(implementStatement.id().start), new NamedUnresolvedReference(tokenPosition(implementStatement.id().start), visitId(implementStatement.id())), visitExpr(implementStatement.expr())));
      }
      expr = new Concrete.ClassExtExpression(tokenPosition(ctx.atomFieldsAcc().start), expr, implementStatements);
    }

    if (ctx.maybeNew() instanceof WithNewContext) {
      expr = new Concrete.NewExpression(tokenPosition(ctx.maybeNew().start), expr);
    }

    return expr;
  }

  enum LevelType { PLevel, HLevel, Unknown }

  private LevelType getLevelType(Concrete.LevelExpression expr) {
    return expr.accept(new ConcreteLevelExpressionVisitor<Void, LevelType>() {
      @Override
      public LevelType visitInf(Concrete.InfLevelExpression expr, Void param) {
        return LevelType.HLevel;
      }

      @Override
      public LevelType visitLP(Concrete.PLevelExpression expr, Void param) {
        return LevelType.PLevel;
      }

      @Override
      public LevelType visitLH(Concrete.HLevelExpression expr, Void param) {
        return LevelType.HLevel;
      }

      @Override
      public LevelType visitNumber(Concrete.NumberLevelExpression expr, Void param) {
        return LevelType.Unknown;
      }

      @Override
      public LevelType visitSuc(Concrete.SucLevelExpression expr, Void param) {
        return expr.getExpression().accept(this, null);
      }

      @Override
      public LevelType visitMax(Concrete.MaxLevelExpression expr, Void param) {
        LevelType type1 = expr.getLeft().accept(this, null);
        LevelType type2 = expr.getRight().accept(this, null);
        return type1 == null || type2 == null || type1 != type2 && type1 != LevelType.Unknown && type2 != LevelType.Unknown ? null : type1 == LevelType.Unknown ? type2 : type1;
      }

      @Override
      public LevelType visitVar(Concrete.InferVarLevelExpression expr, Void param) {
        LevelVariable.LvlType type = expr.getVariable().getType();
        return type == LevelVariable.LvlType.PLVL ? LevelType.PLevel : type == LevelVariable.LvlType.HLVL ? LevelType.HLevel : null;
      }
    }, null);
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
    Position pos = tokenPosition(ctx.getStart());
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
        List<LocalReference> vars = new ArrayList<>();
        getVarList(((TypedContext) typedExpr).expr(0), ((TypedContext) typedExpr).INFIX(), vars);
        arguments.add(new Concrete.TelescopeParameter(tokenPosition(tele.getStart()), explicit, vars, visitExpr(((TypedContext) typedExpr).expr(1))));
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
      expr = new Concrete.AppExpression(expr.getData(), expr, new Concrete.Argument(expr1, !(argument instanceof ArgumentImplicitContext)));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOp(BinOpContext ctx) {
    return parseBinOpSequence(ctx.binOpLeft(), (Concrete.Expression) visit(ctx.binOpArg()), ctx.postfix(), ctx.start);
  }

  private Concrete.Expression parseBinOpSequence(List<BinOpLeftContext> leftCtxs, Concrete.Expression expression, List<PostfixContext> postfixCtxs, Token token) {
    Concrete.Expression left = null;
    Concrete.ReferenceExpression binOp = null;
    List<Concrete.BinOpSequenceElem> sequence = new ArrayList<>(leftCtxs.size() + postfixCtxs.size());

    for (BinOpLeftContext leftContext : leftCtxs) {
      String name = visitInfix(leftContext.infix());
      Concrete.Expression expr = (Concrete.Expression) visit(leftContext.binOpArg());

      if (left == null) {
        left = expr;
      } else {
        sequence.add(new Concrete.BinOpSequenceElem(binOp, expr));
      }

      for (PostfixContext postfixContext : leftContext.postfix()) {
        sequence.add(new Concrete.BinOpSequenceElem(new Concrete.ReferenceExpression(tokenPosition(postfixContext.start), new NamedUnresolvedReference(tokenPosition(postfixContext.start), visitPostfix(postfixContext))), null));
      }

      binOp = new Concrete.ReferenceExpression(tokenPosition(leftContext.infix().getStart()), new NamedUnresolvedReference(tokenPosition(leftContext.infix().start), name));
    }

    if (left == null) {
      left = expression;
    } else {
      sequence.add(new Concrete.BinOpSequenceElem(binOp, expression));
    }

    for (PostfixContext postfixContext : postfixCtxs) {
      sequence.add(new Concrete.BinOpSequenceElem(new Concrete.ReferenceExpression(tokenPosition(postfixContext.start), new NamedUnresolvedReference(tokenPosition(postfixContext.start), visitPostfix(postfixContext))), null));
    }

    return sequence.isEmpty() ? left : new Concrete.BinOpSequenceExpression(tokenPosition(token), left, sequence);
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
      if (ctx.atom() instanceof AtomLiteralContext && ((AtomLiteralContext) ctx.atom()).literal() instanceof NameContext && ((NameContext) ((AtomLiteralContext) ctx.atom()).literal()).prefix().PREFIX() != null) {
        String name = ((NameContext) ((AtomLiteralContext) ctx.atom()).literal()).prefix().PREFIX().getText();
        List<String> path = new ArrayList<>();
        for (; i < ctx.fieldAcc().size(); i++) {
          if (!(ctx.fieldAcc().get(i) instanceof ClassFieldAccContext)) {
            break;
          }
          path.add(visitId(((ClassFieldAccContext) ctx.fieldAcc().get(i)).id()));
        }
        expression = new Concrete.ReferenceExpression(tokenPosition(ctx.start), LongUnresolvedReference.make(tokenPosition(ctx.start), name, path));
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
      myErrorReporter.report(new ParserError(tokenPosition(errorToken), "Field accessor are not implemented"));
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
    return new Concrete.LetClause(new LocalReference(tokenPosition(ctx.id().start), visitId(ctx.id())), arguments, resultType, visitExpr(ctx.expr()));
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
