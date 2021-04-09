package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.let.LetClausePattern;
import org.arend.core.expr.visitor.BaseExpressionVisitor;
import org.arend.extImpl.definitionRenamer.ConflictDefinitionRenamer;
import org.arend.ext.variable.Variable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DConstructor;
import org.arend.core.definition.Definition;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.module.LongName;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.NamedUnresolvedReference;
import org.arend.naming.reference.Referable;
import org.arend.naming.renamer.ReferableRenamer;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.VoidConcreteVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.arend.term.concrete.ConcreteExpressionFactory.*;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Concrete.Expression> {
  private final PrettyPrinterConfig myConfig;
  private final DefinitionRenamer myDefinitionRenamer;
  private final CollectFreeVariablesVisitor myFreeVariablesCollector;
  private final ReferableRenamer myRenamer;

  private ToAbstractVisitor(PrettyPrinterConfig config, DefinitionRenamer definitionRenamer, CollectFreeVariablesVisitor collector, ReferableRenamer renamer) {
    myConfig = config;
    myDefinitionRenamer = definitionRenamer;
    myFreeVariablesCollector = collector;
    myRenamer = renamer;
  }

  public static Concrete.Expression convert(Expression expression, PrettyPrinterConfig config) {
    DefinitionRenamer definitionRenamer = config.getDefinitionRenamer();
    if (definitionRenamer == null) {
      definitionRenamer = new ConflictDefinitionRenamer();
    }
    if (definitionRenamer instanceof ConflictDefinitionRenamer) {
      expression.accept((ConflictDefinitionRenamer) definitionRenamer, null);
    }
    CollectFreeVariablesVisitor collector = new CollectFreeVariablesVisitor(definitionRenamer);
    Set<Variable> variables = new HashSet<>();
    NormalizationMode mode = config.getNormalizationMode();
    if (mode != null) {
      expression = expression.normalize(mode);
    }
    expression.accept(collector, variables);
    ReferableRenamer renamer = new ReferableRenamer();
    ToAbstractVisitor visitor = new ToAbstractVisitor(config, definitionRenamer, collector, renamer);
    renamer.generateFreshNames(variables);
    return expression.accept(visitor, null);
  }

  public static Concrete.LevelExpression convert(Level level) {
    return new ToAbstractVisitor(new PrettyPrinterConfig() {
        @NotNull
        @Override
        public EnumSet<PrettyPrinterFlag> getExpressionFlags() {
          return EnumSet.of(PrettyPrinterFlag.SHOW_LEVELS);
        }
      }, null, null, new ReferableRenamer()).visitLevel(level);
  }

  private boolean hasFlag(PrettyPrinterFlag flag) {
    return myConfig.getExpressionFlags().contains(flag);
  }

  private Concrete.Pattern visitPattern(ExpressionPattern pattern, boolean isExplicit) {
    if (pattern instanceof BindingPattern) {
      return cNamePattern(isExplicit, makeLocalReference(((BindingPattern) pattern).getBinding(), myFreeVariablesCollector.getFreeVariables(((BindingPattern) pattern).getBinding().getNextTyped(null)), false));
    }
    if (pattern instanceof EmptyPattern) {
      return cEmptyPattern(isExplicit);
    }
    if (pattern instanceof ConstructorExpressionPattern) {
      Definition def = pattern.getDefinition();
      return def instanceof Constructor || def instanceof DConstructor
        ? cConPattern(isExplicit, def.getReferable(), visitPatterns(pattern.getSubPatterns(), def.getParameters()))
        : cTuplePattern(isExplicit, visitPatterns(pattern.getSubPatterns(), EmptyDependentLink.getInstance()));
    }
    throw new IllegalStateException();
  }

  private List<Concrete.Pattern> visitPatterns(List<? extends ExpressionPattern> patterns, DependentLink parameters) {
    List<Concrete.Pattern> result = new ArrayList<>(patterns.size());
    for (ExpressionPattern pattern : patterns) {
      result.add(visitPattern(pattern, !parameters.hasNext() || parameters.isExplicit()));
      if (parameters.hasNext()) {
        parameters = parameters.getNext();
      }
    }
    return result;
  }

  private Concrete.Expression checkPath(DataCallExpression expr) {
    if (expr.getDefinition() != Prelude.PATH || hasFlag(PrettyPrinterFlag.SHOW_PREFIX_PATH)) {
      return null;
    }

    LamExpression expr1 = expr.getDefCallArguments().get(0).cast(LamExpression.class);
    if (expr1 != null) {
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return cBinOp(expr.getDefCallArguments().get(1).accept(this, null), Prelude.PATH_INFIX.getReferable(), hasFlag(PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS) ? expr1.getBody().accept(this, null) : null, expr.getDefCallArguments().get(2).accept(this, null));
      }
    }
    return null;
  }

  private Concrete.Expression checkApp(Concrete.Expression expression) {
    if (hasFlag(PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS) || !(expression instanceof Concrete.AppExpression)) {
      return expression;
    }

    Concrete.Expression fun = ((Concrete.AppExpression) expression).getFunction();
    List<Concrete.Argument> args = ((Concrete.AppExpression) expression).getArguments();
    boolean infix = false;
    if (fun instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) fun).getReferent() instanceof GlobalReferable && ((GlobalReferable) ((Concrete.ReferenceExpression) fun).getReferent()).getPrecedence().isInfix) {
      if (args.size() >= 2 && args.get(args.size() - 1).isExplicit() && args.get(args.size() - 2).isExplicit()) {
        infix = true;
        for (int i = 0; i < args.size() - 2; i++) {
          if (args.get(i).isExplicit()) {
            infix = false;
            break;
          }
        }
      }
    }

    if (infix) {
      args.subList(0, args.size() - 2).clear();
    }
    return expression;
  }

  @Override
  public Concrete.Expression visitApp(AppExpression expr, Void params) {
    Concrete.Expression function = expr.getFunction().accept(this, null);
    Concrete.Expression arg = expr.isExplicit() || hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS) ? expr.getArgument().accept(this, null) : null;
    return arg != null ? checkApp(Concrete.AppExpression.make(null, function, arg, expr.isExplicit())) : function;
  }

  private void visitArgument(Expression arg, boolean isExplicit, List<Concrete.Argument> arguments) {
    ReferenceExpression refExpr = arg.cast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding().isHidden()) {
      if (isExplicit) {
        arguments.add(new Concrete.Argument(new Concrete.ThisExpression(null, null), true));
      }
    } else if (isExplicit || hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS)) {
      arguments.add(new Concrete.Argument(arg.accept(this, null), isExplicit));
    }
  }

  private Concrete.Expression visitParameters(Concrete.Expression expr, DependentLink parameters, List<? extends Expression> arguments) {
    List<Concrete.Argument> concreteArguments = new ArrayList<>(arguments.size());
    for (Expression arg : arguments) {
      visitArgument(arg, parameters.isExplicit(), concreteArguments);
      if (parameters.hasNext()) {
        parameters = parameters.getNext();
      }
    }
    return checkApp(Concrete.AppExpression.make(null, expr, concreteArguments));
  }

  private static Concrete.ReferenceExpression makeReference(Referable referable) {
    return cVar(referable == null ? new NamedUnresolvedReference(null, "\\this") : referable);
  }

  private Concrete.ReferenceExpression makeReference(DefCallExpression defCall) {
    Referable ref = defCall.getDefinition().getReferable();
    return hasFlag(PrettyPrinterFlag.SHOW_LEVELS) ? cDefCall(myDefinitionRenamer.renameDefinition(defCall.getDefinition().getRef()), ref, visitLevelNull(defCall.getSortArgument().getPLevel()), visitLevelNull(defCall.getSortArgument().getHLevel())) : cVar(myDefinitionRenamer.renameDefinition(defCall.getDefinition().getRef()), ref);
  }

  @Override
  public Concrete.Expression visitDefCall(DefCallExpression expr, Void params) {
    if (expr.getDefinition().isHideable() && !hasFlag(PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS)) {
      int index = 0;
      for (DependentLink link = expr.getDefinition().getParameters(); link.hasNext(); link = link.getNext()) {
        if (index == expr.getDefinition().getVisibleParameter()) {
          return expr.getDefCallArguments().get(index).accept(this, null);
        }
        index++;
      }
    }

    int skip = hasFlag(PrettyPrinterFlag.SHOW_CON_PARAMS) || !(expr.getDefinition() instanceof DConstructor) ? 0 : ((DConstructor) expr.getDefinition()).getNumberOfParameters();
    return visitParameters(makeReference(expr), DependentLink.Helper.get(expr.getDefinition().getParameters(), skip), skip == 0 || DependentLink.Helper.size(expr.getDefinition().getParameters()) != expr.getDefCallArguments().size() ? expr.getDefCallArguments() : expr.getDefCallArguments().subList(skip, expr.getDefCallArguments().size()));
  }

  @Override
  public Concrete.Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Concrete.Expression letResult = simplifyLetClause(expr);
    if (letResult != null) return letResult;

    Expression argument = expr.getArgument();
    if (argument instanceof ReferenceExpression && ((ReferenceExpression) argument).getBinding().isHidden()) {
      return makeReference(expr);
    }


    if (expr.getDefinition().isHideable() && !hasFlag(PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS)) {
      return argument.accept(this, null);
    }

    String name = null;
    boolean ok = false;
    if (argument instanceof ReferenceExpression && hasFlag(PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE)) {
      ok = true;
      name = ((ReferenceExpression) argument).getBinding().getName();
    } else if (argument instanceof FunCallExpression && hasFlag(PrettyPrinterFlag.SHOW_GLOBAL_FIELD_INSTANCE)) {
      ok = true;
      for (DependentLink param = ((FunCallExpression) argument).getDefinition().getParameters(); param.hasNext(); param = param.getNext()) {
        param = param.getNextTyped(null);
        if (param.isExplicit()) {
          ok = false;
          break;
        }
      }
      name = ((FunCallExpression) argument).getDefinition().getName();
    }
    if (ok) {
      GlobalReferable ref = expr.getDefinition().getReferable();
      return cVar(new LongName(name == null ? "_" : name, ref.getRepresentableName()), ref);
    }

    Concrete.ReferenceExpression result = makeReference(expr);
    return argument instanceof ReferenceExpression && hasFlag(PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE) || argument instanceof FunCallExpression && hasFlag(PrettyPrinterFlag.SHOW_GLOBAL_FIELD_INSTANCE)
      ? Concrete.AppExpression.make(null, result, argument.accept(this, null), false) : result;
  }

  @Override
  public Concrete.Expression visitConCall(ConCallExpression expr, Void params) {
    Expression it = expr;
    Concrete.Expression result = null;
    List<Concrete.Argument> args = null;
    int concreteParam = -1;
    boolean concreteExplicit = true;
    do {
      expr = (ConCallExpression) it;
      boolean showImplicitArgs = hasFlag(PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS) || !expr.getDefinition().getReferable().getPrecedence().isInfix;

      Concrete.Expression cExpr = makeReference(expr);
      if (showImplicitArgs && expr.getDefinition().status().headerIsOK() && hasFlag(PrettyPrinterFlag.SHOW_CON_PARAMS)) {
        List<Concrete.Argument> arguments = new ArrayList<>(expr.getDataTypeArguments().size());
        for (Expression arg : expr.getDataTypeArguments()) {
          visitArgument(arg, false, arguments);
        }
        cExpr = Concrete.AppExpression.make(null, cExpr, arguments);
      }

      int recursiveParam = expr.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        cExpr = visitParameters(cExpr, expr.getDefinition().getParameters(), expr.getDefCallArguments());
        if (args != null) {
          args.set(concreteParam, new Concrete.Argument(cExpr, concreteExplicit));
        } else {
          result = cExpr;
        }
        return result;
      }

      List<Concrete.Argument> newArgs = new ArrayList<>();
      DependentLink parameters = expr.getDefinition().getParameters();
      int newConcreteParam = -1;
      boolean newConcreteExplicit = true;
      for (int i = 0; i < expr.getDefCallArguments().size(); i++) {
        if (i != recursiveParam) {
          if (showImplicitArgs || parameters.isExplicit()) {
            visitArgument(expr.getDefCallArguments().get(i), parameters.isExplicit(), newArgs);
          }
        } else {
          if (parameters.isExplicit() || showImplicitArgs && hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS)) {
            newConcreteParam = newArgs.size();
            newConcreteExplicit = parameters.isExplicit();
            newArgs.add(null);
          }
        }
        if (parameters.isExplicit()) {
          showImplicitArgs = true;
        }
        parameters = parameters.getNext();
      }

      cExpr = Concrete.AppExpression.make(null, cExpr, newArgs);
      if (args != null) {
        args.set(concreteParam, new Concrete.Argument(cExpr, concreteExplicit));
      } else {
        result = cExpr;
      }

      if (newConcreteParam == -1) {
        return result;
      }

      args = cExpr instanceof Concrete.AppExpression ? ((Concrete.AppExpression) cExpr).getArguments() : newArgs;
      concreteParam = newConcreteParam;
      concreteExplicit = newConcreteExplicit;
      if (args.size() > newArgs.size()) {
        concreteParam += args.size() - newArgs.size();
      }

      it = expr.getDefCallArguments().get(recursiveParam);
    } while (it instanceof ConCallExpression);

    ReferenceExpression refExpr = it.cast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding().isHidden()) {
      args.remove(concreteParam);
    } else {
      args.set(concreteParam, new Concrete.Argument(it.accept(this, null), concreteExplicit));
    }

    return result;
  }

  @Override
  public Concrete.Expression visitDataCall(DataCallExpression expr, Void params) {
    Concrete.Expression result = checkPath(expr);
    return result != null ? result : visitDefCall(expr, params);
  }

  private List<Concrete.ClassFieldImpl> visitClassFieldImpls(ClassCallExpression expr, List<Concrete.Argument> arguments) {
    List<Concrete.ClassFieldImpl> statements = new ArrayList<>();
    boolean canBeArgument = arguments != null;
    for (ClassField field : expr.getDefinition().getFields()) {
      Expression implementation = expr.getAbsImplementationHere(field);
      if (implementation != null) {
        if (canBeArgument && field.getReferable().isParameterField()) {
          visitArgument(implementation, field.getReferable().isExplicitField(), arguments);
        } else {
          statements.add(cImplStatement(field.getReferable(), implementation.accept(this, null)));
          canBeArgument = false;
        }
      } else if (canBeArgument && !expr.getDefinition().isImplemented(field)) {
        canBeArgument = false;
      }
    }
    return statements;
  }

  @Override
  public Concrete.Expression visitClassCall(ClassCallExpression expr, Void params) {
    List<Concrete.Argument> arguments = new ArrayList<>();
    List<Concrete.ClassFieldImpl> statements = visitClassFieldImpls(expr, arguments);
    Concrete.Expression defCallExpr = checkApp(Concrete.AppExpression.make(null, makeReference(expr), arguments));
    if (statements.isEmpty()) {
      return defCallExpr;
    } else {
      return cClassExt(defCallExpr, statements);
    }
  }

  @Override
  public Concrete.ReferenceExpression visitReference(ReferenceExpression expr, Void params) {
    return makeReference(myRenamer.getNewReferable(expr.getBinding()));
  }

  @Override
  public Concrete.Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : new Concrete.ReferenceExpression(null, new LocalReferable(expr.getVariable().toString()));
  }

  @Override
  public Concrete.Expression visitSubst(SubstExpression expr, Void params) {
    return expr.getSubstExpression().accept(this, null);
  }

  private LocalReferable makeLocalReference(Binding var, Set<Variable> freeVars, boolean genName) {
    return !genName && !freeVars.contains(var) ? null : myRenamer.generateFreshReferable(var, freeVars);
  }

  private Concrete.Expression etaReduce(Concrete.LamExpression lamExpr) {
    if (!(lamExpr.getBody() instanceof Concrete.AppExpression)) {
      return lamExpr;
    }
    Concrete.Expression fun = ((Concrete.AppExpression) lamExpr.getBody()).getFunction();
    List<Concrete.Argument> args = ((Concrete.AppExpression) lamExpr.getBody()).getArguments();
    int i = args.size() - 1;
    Set<Referable> refs = new HashSet<>();

    List<Concrete.Parameter> parameters = lamExpr.getParameters();
    loop:
    for (int j = parameters.size() - 1; j >= 0; j--) {
      for (int k = parameters.get(j).getReferableList().size() - 1; k >= 0; k--) {
        Referable referable = parameters.get(j).getReferableList().get(k);
        if (referable == null || i < 0 || !(args.get(i).getExpression() instanceof Concrete.ReferenceExpression && referable.equals(((Concrete.ReferenceExpression) args.get(i).getExpression()).getReferent()))) {
          break loop;
        }
        refs.add(referable);
        i--;
      }
    }

    if (refs.isEmpty()) {
      return lamExpr;
    }

    List<? extends Referable> lastRefs = parameters.get(parameters.size() - 1).getReferableList();
    Referable lastRef = lastRefs.get(lastRefs.size() - 1);
    VoidConcreteVisitor<Void,Void> visitor = new VoidConcreteVisitor<>() {
      @Override
      public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
        refs.remove(expr.getReferent());
        return null;
      }
    };

    fun.accept(visitor, null);
    for (int j = 0; j <= i; j++) {
      if (!refs.contains(lastRef)) {
        return lamExpr;
      }
      args.get(j).getExpression().accept(visitor, null);
    }

    int numberOfVars = 0;
    loop:
    for (int j = parameters.size() - 1; j >= 0; j--) {
      for (int k = parameters.get(j).getReferableList().size() - 1; k >= 0; k--) {
        if (!refs.contains(parameters.get(j).getReferableList().get(k))) {
          break loop;
        }
        numberOfVars++;
      }
    }
    if (numberOfVars == 0) {
      return lamExpr;
    }

    int varsCounter = numberOfVars;

    for (int j = parameters.size() - 1; j >= 0; j--) {
      List<? extends Referable> refList = parameters.get(j).getReferableList();
      if (varsCounter == refList.size()) {
        parameters = parameters.subList(0, j);
        break;
      }
      if (varsCounter < refList.size()) {
        parameters = new ArrayList<>(parameters.subList(0, j));
        Concrete.Parameter param = parameters.get(j);
        parameters.add(new Concrete.TelescopeParameter(param.getData(), param.isExplicit(), param.getReferableList().subList(0, refList.size() - varsCounter), param.getType()));
        break;
      }
      varsCounter -= refList.size();
    }

    Concrete.Expression body = args.size() == numberOfVars ? fun : checkApp(Concrete.AppExpression.make(lamExpr.body.getData(), fun, args.subList(0, args.size() - numberOfVars)));
    return parameters.isEmpty() ? body : new Concrete.LamExpression(lamExpr.getData(), parameters, body);
  }

  @Override
  public Concrete.Expression visitLam(LamExpression lamExpr, Void ignore) {
    Expression body = lamExpr.getBody();
    List<Concrete.Parameter> parameters = new ArrayList<>();
    Expression expr = lamExpr;
    for (; lamExpr != null; lamExpr = expr.cast(LamExpression.class)) {
      if (hasFlag(PrettyPrinterFlag.SHOW_TYPES_IN_LAM)) {
        visitDependentLink(lamExpr.getParameters(), parameters, true);
      } else {
        SingleDependentLink params = lamExpr.getParameters();
        Set<Variable> freeVars = myFreeVariablesCollector.getFreeVariables(params.getNextTyped(null));
        for (SingleDependentLink link = params; link.hasNext(); link = link.getNext()) {
          parameters.add(cName(link.isExplicit(), makeLocalReference(link, freeVars, false)));
        }
      }
      expr = lamExpr.getBody();
    }

    Concrete.LamExpression result = cLam(parameters, expr.accept(this, null));
    return body.isInstance(ClassCallExpression.class) ? result : etaReduce(result);
  }

  private void visitDependentLink(DependentLink parameters, List<? super Concrete.TypeParameter> args, boolean isNamed) {
    List<Referable> referableList = new ArrayList<>(3);
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      Set<Variable> freeVars = myFreeVariablesCollector.getFreeVariables(link1);
      for (; link != link1; link = link.getNext()) {
        referableList.add(makeLocalReference(link, freeVars, !link.isExplicit()));
      }

      Referable referable = makeLocalReference(link, freeVars, !link.isExplicit());
      if (referable == null && !isNamed && referableList.isEmpty()) {
        args.add(cTypeArg(link.isExplicit(), link.getTypeExpr().accept(this, null)));
      } else {
        referableList.add(referable);
        args.add(cTele(link.isExplicit(), new ArrayList<>(referableList), link.getTypeExpr().accept(this, null)));
        referableList.clear();
      }
    }
  }

  @Override
  public Concrete.Expression visitPi(PiExpression piExpr, Void ignore) {
    List<List<Concrete.TypeParameter>> parameters = new ArrayList<>();
    Expression expr = piExpr;
    for (; piExpr != null; piExpr = expr.cast(PiExpression.class)) {
      List<Concrete.TypeParameter> params = new ArrayList<>();
      visitDependentLink(piExpr.getParameters(), params, false);
      if (!parameters.isEmpty() && parameters.get(parameters.size() - 1) instanceof Concrete.TelescopeParameter && !params.isEmpty() && params.get(0) instanceof Concrete.TelescopeParameter) {
        parameters.get(parameters.size() - 1).addAll(params);
      } else {
        parameters.add(params);
      }
      expr = piExpr.getCodomain();
    }

    assert expr != null;
    Concrete.Expression result = expr.accept(this, null);
    for (int i = parameters.size() - 1; i >= 0; i--) {
      result = cPi(parameters.get(i), result);
    }
    return result;
  }

  @Override
  public Concrete.Expression visitUniverse(UniverseExpression expr, Void params) {
    return visitSort(expr.getSort());
  }

  private Concrete.LevelExpression visitLevelNull(Level level) {
    return level.isClosed() || !level.isVarOnly() && hasFlag(PrettyPrinterFlag.SHOW_LEVELS) ? visitLevel(level) : null;
  }

  private Concrete.Expression visitSort(Sort sort) {
    return cUniverse(sort.isOmega() ? new Concrete.PLevelExpression(null) : visitLevelNull(sort.getPLevel()), visitLevelNull(sort.getHLevel()));
  }

  private Concrete.LevelExpression visitLevel(Level level) {
    if (level.isInfinity()) {
      return new Concrete.InfLevelExpression(null);
    }
    if (level.isClosed()) {
      return new Concrete.NumberLevelExpression(null, level.getConstant());
    }

    Concrete.LevelExpression result;
    if (level.getVar() == LevelVariable.PVAR) {
      result = new Concrete.PLevelExpression(null);
    } else if (level.getVar() == LevelVariable.HVAR) {
      result = new Concrete.HLevelExpression(null);
    } else if (level.getVar() instanceof InferenceLevelVariable) {
      if (!hasFlag(PrettyPrinterFlag.SHOW_LEVELS)) {
        return null;
      }
      result = new Concrete.InferVarLevelExpression(null, (InferenceLevelVariable) level.getVar());
    } else {
      throw new IllegalStateException();
    }

    for (int i = 0; i < level.getConstant(); i++) {
      result = new Concrete.SucLevelExpression(null, result);
    }

    if (level.getMaxConstant() > 0 || level.getMaxConstant() == 0 && level.getVar() == LevelVariable.HVAR) {
      result = new Concrete.MaxLevelExpression(null, result, visitLevel(new Level(level.getMaxConstant())));
    }

    return result;
  }

  @Override
  public Concrete.Expression visitError(ErrorExpression expr, Void params) {
    return cGoal(expr.isGoal() ? "" : "error", expr.getExpression() == null ? null : expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitTuple(TupleExpression expr, Void params) {
    List<Concrete.Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    Concrete.Expression result = cTuple(fields);
    if (hasFlag(PrettyPrinterFlag.SHOW_TUPLE_TYPE)) {
      result = new Concrete.TypedExpression(null, result, visitSigma(expr.getSigmaType(), null));
    }
    return result;
  }

  @Override
  public Concrete.Expression visitSigma(SigmaExpression expr, Void params) {
    List<Concrete.TypeParameter> parameters = new ArrayList<>();
    visitDependentLink(expr.getParameters(), parameters, false);
    return cSigma(parameters);
  }

  private Concrete.Expression simplifyLetClause(Expression expr) {
    List<Object> list = new ArrayList<>();
    while (true) {
      expr = expr.getUnderlyingExpression();
      if (expr instanceof ProjExpression) {
        list.add(((ProjExpression) expr).getField());
        expr = ((ProjExpression) expr).getExpression();
      } else if (expr instanceof FieldCallExpression) {
        list.add(((FieldCallExpression) expr).getDefinition());
        expr = ((FieldCallExpression) expr).getArgument();
      } else {
        break;
      }
    }

    if (expr instanceof ReferenceExpression && ((ReferenceExpression) expr).getBinding() instanceof HaveClause) {
      LetClausePattern pattern = ((HaveClause) ((ReferenceExpression) expr).getBinding()).getPattern();
      for (int i = list.size() - 1; i >= 0; i--) {
        if (pattern == null) return null;
        int index;
        if (list.get(i) instanceof Integer) {
          index = (int) list.get(i);
        } else if (list.get(i) instanceof ClassField) {
          ClassField field = (ClassField) list.get(i);
          if (pattern.getFields() == null) return null;
          index = pattern.getFields().indexOf(field);
        } else return null;
        List<? extends LetClausePattern> patterns = pattern.getPatterns();
        if (patterns == null || index < 0 || index >= patterns.size()) return null;
        pattern = patterns.get(index);
      }
      if (pattern.getName() != null) {
        return new Concrete.ReferenceExpression(null, new LocalReferable(pattern.getName()));
      }
    }

    return null;
  }

  @Override
  public Concrete.Expression visitProj(ProjExpression expr, Void params) {
    Concrete.Expression result = simplifyLetClause(expr);
    if (result != null) return result;

    if (expr.getField() == 0 || expr.getField() == 1) {
      FunCallExpression funCall = expr.getExpression().cast(FunCallExpression.class);
      if (funCall != null && funCall.getDefinition() == Prelude.DIV_MOD) {
        return FunCallExpression.make(expr.getField() == 0 ? Prelude.DIV : Prelude.MOD, funCall.getSortArgument(), new ArrayList<>(funCall.getDefCallArguments())).accept(this, null);
      }
    }
    return cProj(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Concrete.Expression visitNew(NewExpression expr, Void params) {
    if (expr.getRenewExpression() == null) {
      return cNew(visitClassCall(expr.getClassCall(), null));
    } else {
      return cNew(cClassExt(expr.getRenewExpression().accept(this, null), visitClassFieldImpls(expr.getClassCall(), null)));
    }
  }

  @Override
  public Concrete.Expression visitPEval(PEvalExpression expr, Void params) {
    return cEval(true, expr.getExpression().accept(this, null));
  }

  private Concrete.LetClausePattern makeLetClausePattern(LetClausePattern pattern) {
    if (pattern == null) return null;
    if (pattern.getName() != null) {
      return new Concrete.LetClausePattern(new LocalReferable(pattern.getName()), (Concrete.Expression) null);
    }
    List<? extends LetClausePattern> patterns = pattern.getPatterns();
    if (patterns == null) return null;
    List<Concrete.LetClausePattern> cPatterns = new ArrayList<>(patterns.size());
    for (LetClausePattern subpattern : patterns) {
      Concrete.LetClausePattern cSubpattern = makeLetClausePattern(subpattern);
      if (cSubpattern == null) return null;
      cPatterns.add(cSubpattern);
    }
    return new Concrete.LetClausePattern(null, cPatterns);
  }

  @Override
  public Concrete.Expression visitLet(LetExpression letExpression, Void params) {
    boolean isHave = true;
    List<Concrete.LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (HaveClause clause : letExpression.getClauses()) {
      if (clause instanceof LetClause) {
        isHave = false;
      }
      Concrete.LetClausePattern pattern;
      if (clause.getPattern().getName() != null) {
        pattern = new Concrete.LetClausePattern(makeLocalReference(clause, myFreeVariablesCollector.getFreeVariables(clause), false), (Concrete.Expression) null);
      } else {
        pattern = makeLetClausePattern(clause.getPattern());
      }
      if (pattern == null) {
        Referable referable = makeLocalReference(clause, myFreeVariablesCollector.getFreeVariables(clause), false);
        if (referable != null) {
          clauses.add(new Concrete.LetClause(referable, Collections.emptyList(), null, clause.getExpression().accept(this, null)));
        }
      } else {
        clauses.add(new Concrete.LetClause(pattern, null, clause.getExpression().accept(this, null)));
      }
    }

    Concrete.Expression expr = letExpression.getExpression().accept(this, null);
    return clauses.isEmpty() ? expr : new Concrete.LetExpression(null, isHave, letExpression.isStrict(), clauses, expr);
  }

  @Override
  public Concrete.Expression visitCase(CaseExpression expr, Void params) {
    List<Concrete.CaseArgument> arguments = new ArrayList<>(expr.getArguments().size());
    if (hasFlag(PrettyPrinterFlag.SHOW_CASE_RESULT_TYPE)) {
      List<Concrete.TypeParameter> parameters = new ArrayList<>();
      visitDependentLink(expr.getParameters(), parameters, true);
      int i = 0;
      for (Concrete.TypeParameter parameter : parameters) {
        for (Referable ref : parameter.getReferableList()) {
          arguments.add(new Concrete.CaseArgument(expr.getArguments().get(i++).accept(this, null), ref, parameter.getType()));
        }
      }
    } else {
      for (Expression argument : expr.getArguments()) {
        arguments.add(new Concrete.CaseArgument(argument.accept(this, null), null, null));
      }
    }

    Concrete.Expression resultType = null;
    Concrete.Expression resultTypeLevel = null;
    if (hasFlag(PrettyPrinterFlag.SHOW_CASE_RESULT_TYPE) && !(expr.getResultType() instanceof ErrorExpression)) {
      resultType = expr.getResultType().accept(this, null);
      if (expr.getResultTypeLevel() != null) {
        resultTypeLevel = expr.getResultTypeLevel().accept(this, null);
      }
    }

    List<Concrete.FunctionClause> clauses = new ArrayList<>();
    for (ElimClause<Pattern> clause : expr.getElimBody().getClauses()) {
      List<Concrete.Pattern> patterns = new ArrayList<>();
      DependentLink link = expr.getParameters();
      for (Pattern pattern : clause.getPatterns()) {
        visitElimPattern(pattern, link.isExplicit(), patterns);
        link = link.getNext();
      }
      clauses.add(cClause(patterns, clause.getExpression() == null ? null : clause.getExpression().accept(this, null)));
    }
    return cCase(expr.isSCase(), arguments, resultType, resultTypeLevel, clauses);
  }

  private void visitElimPattern(Pattern pattern, boolean isExplicit, List<Concrete.Pattern> patterns) {
    if (pattern instanceof BindingPattern) {
      DependentLink link = pattern.getFirstBinding();
      patterns.add(cNamePattern(isExplicit, makeLocalReference(link, myFreeVariablesCollector.getFreeVariables(link.getNextTyped(null)), false)));
    } else if (pattern == EmptyPattern.INSTANCE) {
      patterns.add(cEmptyPattern(isExplicit));
    } else {
      Definition def = pattern.getConstructor();
      if (def == Prelude.ZERO) {
        patterns.add(new Concrete.NumberPattern(null, 0, Collections.emptyList()));
      } else {
        List<Concrete.Pattern> subPatterns = new ArrayList<>();
        DependentLink param = def == null ? null : def.getParameters();
        for (Pattern subPattern : pattern.getSubPatterns()) {
          visitElimPattern(subPattern, param == null || param.isExplicit(), subPatterns);
          if (param != null && param.hasNext()) {
            param = param.getNext();
          }
        }

        if (def == null) {
          patterns.add(cTuplePattern(isExplicit, subPatterns));
        } else {
          patterns.add(cConPattern(isExplicit, def.getReferable(), subPatterns));
        }
      }
    }
  }

  @Override
  public Concrete.Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Concrete.Expression visitInteger(IntegerExpression expr, Void params) {
    return new Concrete.NumericLiteral(null, expr.getBigInteger());
  }

  @Override
  public Concrete.Expression visitTypeCoerce(TypeCoerceExpression expr, Void params) {
    return expr.getArgument().accept(this, null);
  }

  @Override
  public Concrete.Expression visitArray(ArrayExpression expr, Void params) {
    Concrete.Expression elementsType = hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS) ? expr.getElementsType().accept(this, null) : null;

    Concrete.Expression result;
    if (expr.getTail() == null) {
      result = new Concrete.ReferenceExpression(null, Prelude.EMPTY_ARRAY.getReferable());
      if (elementsType != null) {
        result = Concrete.AppExpression.make(null, result, elementsType, false);
      }
    } else {
      result = expr.getTail().accept(this, null);
    }

    for (int i = expr.getElements().size() - 1; i >= 0; i--) {
      List<Concrete.Argument> arguments = new ArrayList<>(3);
      if (elementsType != null) {
        arguments.add(new Concrete.Argument(elementsType, false));
      }
      arguments.add(new Concrete.Argument(expr.getElements().get(i).accept(this, null), true));
      arguments.add(new Concrete.Argument(result, true));
      result = Concrete.AppExpression.make(null, new Concrete.ReferenceExpression(null, Prelude.ARRAY_CONS.getReferable()), arguments);
    }

    return result;
  }
}
