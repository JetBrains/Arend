package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.ParamLevelVariable;
import org.arend.core.context.binding.PersistentEvaluatingBinding;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.let.LetClausePattern;
import org.arend.core.expr.visitor.BaseExpressionVisitor;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.EmptyPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.Levels;
import org.arend.ext.concrete.definition.ClassFieldKind;
import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.module.LongName;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.util.Pair;
import org.arend.ext.variable.Variable;
import org.arend.extImpl.definitionRenamer.ConflictDefinitionRenamer;
import org.arend.naming.reference.*;
import org.arend.naming.renamer.ReferableRenamer;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.DefinableMetaDefinition;
import org.arend.typechecking.visitor.VoidConcreteVisitor;
import org.arend.util.SingletonList;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.arend.term.concrete.ConcreteExpressionFactory.*;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Concrete.Expression> implements DefinitionVisitor<Void, Concrete.GeneralDefinition> {
  private final PrettyPrinterConfig myConfig;
  private final DefinitionRenamer myDefinitionRenamer;
  private final CollectFreeVariablesVisitor myFreeVariablesCollector;
  private final ReferableRenamer myRenamer;

  ToAbstractVisitor(PrettyPrinterConfig config, DefinitionRenamer definitionRenamer, CollectFreeVariablesVisitor collector, ReferableRenamer renamer) {
    myConfig = config;
    myDefinitionRenamer = definitionRenamer;
    myFreeVariablesCollector = collector;
    myRenamer = renamer;
  }

  public static Concrete.Expression convert(Expression expression, PrettyPrinterConfig config) {
    return convert(expression, config, new ReferableRenamer());
  }

  public static Concrete.Expression convert(Expression expression, PrettyPrinterConfig config, @NotNull ReferableRenamer renamer) {
    return convert(expression, null, null, config, renamer);
  }

  public static Concrete.Expression convert(Expression expression, Expression subexpr, Levels levels, PrettyPrinterConfig config, @NotNull ReferableRenamer renamer) {
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
    if (mode != null && subexpr == null) {
      expression = expression.normalize(mode);
    }
    expression.accept(collector, variables);
    ToAbstractVisitor visitor = subexpr == null ? new ToAbstractVisitor(config, definitionRenamer, collector, renamer) : new ToAbstractWithSubexprVisitor(config, definitionRenamer, collector, renamer, subexpr, levels);
    renamer.generateFreshNames(variables);
    return visitor.convertExpr(expression);
  }

  public static List<Concrete.TypeParameter> convert(DependentLink params, PrettyPrinterConfig config) {
    DefinitionRenamer definitionRenamer = config.getDefinitionRenamer();
    if (definitionRenamer == null) {
      definitionRenamer = new ConflictDefinitionRenamer();
    }
    CollectFreeVariablesVisitor collector = new CollectFreeVariablesVisitor(definitionRenamer);
    Set<Variable> variables = new HashSet<>();
    collector.visitParameters(params, variables);
    ReferableRenamer renamer = new ReferableRenamer();
    ToAbstractVisitor visitor = new ToAbstractVisitor(config, definitionRenamer, collector, renamer);
    renamer.generateFreshNames(variables);
    List<Concrete.TypeParameter> result = new ArrayList<>();
    visitor.visitDependentLink(params, result, true, true);
    return result;
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

  public static Concrete.GeneralDefinition convert(Definition definition, PrettyPrinterConfig config) {
    DefinitionRenamer definitionRenamer = config.getDefinitionRenamer();
    if (definitionRenamer == null) {
      definitionRenamer = new ConflictDefinitionRenamer();
    }
    if (definitionRenamer instanceof ConflictDefinitionRenamer) {
      definition.accept((ConflictDefinitionRenamer) definitionRenamer, null);
    }
    CollectFreeVariablesVisitor collector = new CollectFreeVariablesVisitor(definitionRenamer);
    Set<Variable> variables = new HashSet<>();
    definition.accept(collector, variables);
    ReferableRenamer renamer = new ReferableRenamer();
    ToAbstractVisitor visitor = new ToAbstractVisitor(config, definitionRenamer, collector, renamer);
    renamer.generateFreshNames(variables);
    return definition.accept(visitor, null);
  }

  Concrete.Expression convertExpr(Expression expr) {
    return expr.accept(this, null);
  }

  private boolean hasFlag(PrettyPrinterFlag flag) {
    return myConfig.getExpressionFlags().contains(flag);
  }

  private int getVerboseLevel(Expression coreExpression) {
    return myConfig.getVerboseLevel(coreExpression);
  }

  private boolean checkAppArgExplicitness(AppExpression app) {
    Expression expr = app;
    int implicits = 0;
    while (expr instanceof AppExpression) {
      if (!((AppExpression) expr).isExplicit()) {
        implicits += 1;
      }
      expr = expr.getFunction();
    }
    int baseVerboseLevel = getVerboseLevel(expr);
    return baseVerboseLevel >= implicits;
  }

  private boolean shouldBeVerbose(DependentLink link) {
    for (DependentLink thisLink = link; thisLink.hasNext(); thisLink = thisLink.getNext()) {
      if (myConfig.getVerboseLevel(thisLink) > 0) {
        return true;
      }
    }
    return false;
  }

  private Concrete.Expression checkPath(DataCallExpression expr) {
    if (expr.getDefinition() != Prelude.PATH || hasFlag(PrettyPrinterFlag.SHOW_PREFIX_PATH)) {
      return null;
    }

    LamExpression expr1 = expr.getDefCallArguments().get(0).cast(LamExpression.class);
    if (expr1 != null) {
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return cBinOp(convertExpr(expr.getDefCallArguments().get(1)), Prelude.PATH_INFIX.getReferable(), hasFlag(PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS) || convertSubexpr(expr1.getBody()) ? convertExpr(expr1.getBody()) : null, convertExpr(expr.getDefCallArguments().get(2)));
      }
    }
    return null;
  }

  private Concrete.Expression checkApp(Concrete.Expression expression, boolean isVerbose) {
    if (isVerbose || hasFlag(PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS) || !(expression instanceof Concrete.AppExpression)) {
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

  protected boolean convertSubexpr(Expression expr) {
    return false;
  }

  private boolean convertSubexprs(List<? extends Expression> exprs) {
    for (Expression expr : exprs) {
      if (expr != null && convertSubexpr(expr)) {
        return true;
      }
    }
    return false;
  }

  protected boolean convertParameters(DependentLink param) {
    for (; param.hasNext(); param = param.getNext()) {
      param = param.getNextTyped(null);
      if (convertSubexpr(param.getTypeExpr())) {
        return true;
      }
    }
    return false;
  }

  protected boolean convertLevels(DefCallExpression defCall) {
    return false;
  }

  @Override
  public Concrete.Expression visitApp(AppExpression expr, Void params) {
    Concrete.Expression function = convertExpr(expr.getFunction());
    Concrete.Expression arg = expr.isExplicit() || hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS) || checkAppArgExplicitness(expr) || convertSubexpr(expr.getArgument()) ? convertExpr(expr.getArgument()) : null;
    return arg != null ? checkApp(Concrete.AppExpression.make(expr, function, arg, expr.isExplicit()), false) : function;
  }

  private void visitArgument(Expression arg, boolean isExplicit, List<Concrete.Argument> arguments, boolean genGoal, boolean alwaysShow) {
    ReferenceExpression refExpr = arg.cast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding().isHidden()) {
      if (isExplicit) {
        Concrete.Expression mappedExpression = myRenamer.getConcreteExpression(refExpr.getBinding());
        if (mappedExpression != null) {
          arguments.add(new Concrete.Argument(mappedExpression, isExplicit));
          return;
        }
        arguments.add(new Concrete.Argument(new Concrete.ThisExpression(arg, null), true));
      }
    } else if (isExplicit || alwaysShow || hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS) || !genGoal && convertSubexpr(arg)) {
      arguments.add(new Concrete.Argument(genGoal ? generateHiddenGoal(null) : convertExpr(arg), isExplicit));
    }
  }

  private Concrete.Expression visitParameters(Concrete.Expression expr, DependentLink parameters, List<? extends Expression> arguments, int parentVerboseLevel) {
    List<Concrete.Argument> concreteArguments = new ArrayList<>(arguments.size());
    int implicitArgumentsCounter = 0;
    for (Expression arg : arguments) {
      implicitArgumentsCounter += parameters.isExplicit() ? 0 : 1;
      boolean genGoal = false;
      if (parameters.isProperty()) {
        genGoal = !hasFlag(PrettyPrinterFlag.SHOW_PROOFS);
        BoxExpression boxArg = arg.cast(BoxExpression.class);
        if (boxArg != null) {
          arg = boxArg.getExpression();
        }
      }
      visitArgument(arg, parameters.isExplicit(), concreteArguments, genGoal, (parentVerboseLevel >= implicitArgumentsCounter));
      if (parameters.hasNext()) {
        parameters = parameters.getNext();
      }
    }
    return checkApp(Concrete.AppExpression.make(expr, expr, concreteArguments), parentVerboseLevel > 0);
  }

  private Concrete.ReferenceExpression makeReference(DefCallExpression defCall) {
    Definition def = defCall.getDefinition();
    Referable ref = def.getRef();
    boolean showStdVar = convertLevels(defCall);
    if (!(showStdVar || hasFlag(PrettyPrinterFlag.SHOW_LEVELS))) {
      return cVar(defCall, myDefinitionRenamer.renameDefinition(ref), ref);
    }

    List<Level> pLevels;
    List<Level> hLevels;
    if (defCall instanceof LeveledDefCallExpression) {
      List<? extends LevelVariable> params = def.getLevelParameters();
      LevelSubstitution subst = ((LeveledDefCallExpression) defCall).getLevelSubstitution();
      if (params == null) {
        pLevels = Collections.singletonList((Level) subst.get(LevelVariable.PVAR));
        hLevels = Collections.singletonList((Level) subst.get(LevelVariable.HVAR));
      } else {
        int pNum = def.getNumberOfPLevelParameters();
        pLevels = new ArrayList<>(pNum);
        hLevels = new ArrayList<>(params.size() - pNum);
        for (int i = 0; i < pNum; i++) {
          pLevels.add((Level) subst.get(params.get(i)));
        }
        for (int i = pNum; i < params.size(); i++) {
          hLevels.add((Level) subst.get(params.get(i)));
        }
      }
    } else {
      pLevels = Collections.singletonList(null);
      hLevels = Collections.singletonList(null);
    }

    return cDefCall(defCall, myDefinitionRenamer.renameDefinition(ref), ref, visitLevelsNull(pLevels, showStdVar), visitLevelsNull(hLevels, showStdVar));
  }

  @Override
  public Concrete.Expression visitDefCall(DefCallExpression expr, Void params) {
    if (!hasFlag(PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS)) {
      if (expr.getDefinition().isHideable()) {
        int index = 0;
        for (DependentLink link = expr.getDefinition().getParameters(); link.hasNext(); link = link.getNext()) {
          if (index == expr.getDefinition().getVisibleParameter()) {
            return convertExpr(expr.getDefCallArguments().get(index));
          }
          index++;
        }
      }
      if (expr.getDefinition() == Prelude.ARRAY_INDEX) {
        return Concrete.AppExpression.make(expr, convertExpr(expr.getDefCallArguments().get(0)), convertExpr(expr.getDefCallArguments().get(1)), true);
      }
    }

    int verbosity = getVerboseLevel(expr);
    int skip = verbosity != 0 || hasFlag(PrettyPrinterFlag.SHOW_CON_PARAMS) || !(expr.getDefinition() instanceof DConstructor dCon) || convertSubexprs(expr.getDefCallArguments().subList(0, dCon.getNumberOfParameters())) ? 0 : dCon.getNumberOfParameters();
    return visitParameters(makeReference(expr), DependentLink.Helper.get(expr.getDefinition().getParameters(), skip), skip == 0 || DependentLink.Helper.size(expr.getDefinition().getParameters()) != expr.getDefCallArguments().size() ? expr.getDefCallArguments() : expr.getDefCallArguments().subList(skip, expr.getDefCallArguments().size()), verbosity);
  }

  @Override
  public Concrete.Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Concrete.Expression letResult = simplifyLetClause(expr);
    if (letResult != null) return letResult;

    Expression argument = expr.getArgument();
    if (argument instanceof ReferenceExpression && ((ReferenceExpression) argument).getBinding().isHidden()) {
      if (((ReferenceExpression) argument).getBinding() instanceof ClassCallExpression.ClassCallBinding && hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS)) {
        return Concrete.AppExpression.make(null, makeReference(expr), new Concrete.ThisExpression(expr, null), false);
      } else {
        return makeReference(expr);
      }
    }


    if (expr.getDefinition().isHideable() && !hasFlag(PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS)) {
      return convertExpr(argument);
    }

    boolean isGlobalInstance = argument instanceof FunCallExpression && !expr.getDefinition().getParentClass().isRecord();
    String name = null;
    Concrete.Expression qualifier = null;
    boolean ok = false;
    if (argument instanceof ReferenceExpression && (hasFlag(PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE) || getVerboseLevel(argument) > 0 || convertSubexpr(argument))) {
      ok = true;
      qualifier = visitReference((ReferenceExpression) argument, null);
      name = ((ReferenceExpression) argument).getBinding().getName();
    } else if (isGlobalInstance && hasFlag(PrettyPrinterFlag.SHOW_GLOBAL_FIELD_INSTANCE)) {
      ok = true;
      for (DependentLink param = ((FunCallExpression) argument).getDefinition().getParameters(); param.hasNext(); param = param.getNext()) {
        param = param.getNextTyped(null);
        if (param.isExplicit() || hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS)) {
          ok = false;
          break;
        }
      }
      qualifier = convertExpr(argument.getFunction());
      name = ((FunCallExpression) argument).getDefinition().getName();
    }
    if (ok && qualifier instanceof Concrete.ReferenceExpression) {
      GlobalReferable ref = expr.getDefinition().getReferable();
      return cVar(expr, (Concrete.ReferenceExpression) qualifier, new LongName(name == null ? "_" : name, ref.getRepresentableName()), ref);
    }

    Concrete.ReferenceExpression result = makeReference(expr);
    return (!isGlobalInstance && hasFlag(PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE) || isGlobalInstance && hasFlag(PrettyPrinterFlag.SHOW_GLOBAL_FIELD_INSTANCE) || getVerboseLevel(expr) > 0 || convertSubexpr(argument))
      ? Concrete.AppExpression.make(null, result, convertExpr(argument), false) : result;
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
      if (showImplicitArgs && expr.getDefinition().status().headerIsOK() && hasFlag(PrettyPrinterFlag.SHOW_CON_PARAMS) || convertSubexprs(expr.getDataTypeArguments())) {
        List<Concrete.Argument> arguments = new ArrayList<>(expr.getDataTypeArguments().size());
        for (Expression arg : expr.getDataTypeArguments()) {
          visitArgument(arg, false, arguments, false, false);
        }
        cExpr = Concrete.AppExpression.make(expr, cExpr, arguments);
      }

      int recursiveParam = expr.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        cExpr = visitParameters(cExpr, expr.getDefinition().getParameters(), expr.getDefCallArguments(), getVerboseLevel(expr));
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
          if (showImplicitArgs || parameters.isExplicit() || convertSubexpr(expr.getDefCallArguments().get(i))) {
            visitArgument(expr.getDefCallArguments().get(i), parameters.isExplicit(), newArgs, false, false);
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

      cExpr = Concrete.AppExpression.make(expr, cExpr, newArgs);
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
      args.set(concreteParam, new Concrete.Argument(convertExpr(it), concreteExplicit));
    }

    return result;
  }

  @Override
  public Concrete.Expression visitDataCall(DataCallExpression expr, Void params) {
    Concrete.Expression result = checkPath(expr);
    return result != null ? result : visitDefCall(expr, params);
  }

  private Concrete.Expression generateHiddenGoal(Object data) {
    return new Concrete.GoalExpression(data, "hidden", null);
  }

  private List<Concrete.ClassFieldImpl> visitClassFieldImpls(ClassCallExpression expr, List<Concrete.Argument> arguments) {
    List<Concrete.ClassFieldImpl> statements = new ArrayList<>();
    boolean canBeArgument = arguments != null;
    int verboseLevel = myConfig.getVerboseLevel(expr);
    int implicitCounter = 0;
    for (ClassField field : expr.getDefinition().getFields()) {
      implicitCounter += field.getReferable().isExplicitField() ? 0 : 1;
      Expression implementation = expr.getAbsImplementationHere(field);
      if (implementation != null) {
        boolean genGoal = getVerboseLevel(implementation) == 0 && !hasFlag(PrettyPrinterFlag.SHOW_PROOFS) && field.isProperty();
        if (canBeArgument && field.getReferable().isParameterField()) {
          visitArgument(implementation, field.getReferable().isExplicitField(), arguments, genGoal, verboseLevel >= implicitCounter);
        } else {
          statements.add(cImplStatement(field.getReferable(), genGoal ? generateHiddenGoal(implementation) : convertExpr(implementation)));
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
    if (expr.getDefinition() == Prelude.DEP_ARRAY) {
      Expression impl = expr.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE);
      if (impl != null) {
        Expression constType = impl.removeConstLam();
        if (constType != null) {
          List<Concrete.Argument> args = new ArrayList<>(3);
          args.add(new Concrete.Argument(convertExpr(constType), true));
          Expression length = expr.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
          if (length != null) {
            args.add(new Concrete.Argument(convertExpr(length), true));
          }
          Expression at = expr.getAbsImplementationHere(Prelude.ARRAY_AT);
          if (at != null) {
            args.add(new Concrete.Argument(convertExpr(at), true));
          }
          return Concrete.AppExpression.make(expr, makeReference(FunCallExpression.makeFunCall(Prelude.ARRAY, expr.getLevels(), Collections.emptyList())), args);
        }
      }
    }

    List<Concrete.Argument> arguments = new ArrayList<>();
    List<Concrete.ClassFieldImpl> statements = visitClassFieldImpls(expr, arguments);
    Concrete.Expression defCallExpr = checkApp(Concrete.AppExpression.make(expr, makeReference(expr), arguments), false);
    if (statements.isEmpty()) {
      return defCallExpr;
    } else {
      return cClassExt(defCallExpr, statements);
    }
  }

  @Override
  public Concrete.Expression visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding() instanceof PersistentEvaluatingBinding ? convertExpr(((PersistentEvaluatingBinding) expr.getBinding()).getExpression()) : myRenamer.getConcreteExpression(expr.getBinding());
  }

  @Override
  public Concrete.Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? convertExpr(expr.getSubstExpression()) : new Concrete.ReferenceExpression(null, new LocalReferable(expr.getVariable().toString()));
  }

  @Override
  public Concrete.Expression visitSubst(SubstExpression expr, Void params) {
    return convertExpr(expr.getSubstExpression());
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
    VoidConcreteVisitor<Void> visitor = new VoidConcreteVisitor<>() {
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
        parameters.add(new Concrete.TelescopeParameter(param.getData(), param.isExplicit(), param.getReferableList().subList(0, refList.size() - varsCounter), param.getType(), false));
        break;
      }
      varsCounter -= refList.size();
    }

    Concrete.Expression body = args.size() == numberOfVars ? fun : checkApp(Concrete.AppExpression.make(lamExpr.body.getData(), fun, args.subList(0, args.size() - numberOfVars)), false);
    return parameters.isEmpty() ? body : new Concrete.LamExpression(lamExpr.getData(), parameters, body);
  }

  @Override
  public Concrete.Expression visitLam(LamExpression lamExpr, Void ignore) {
    Expression body = lamExpr.getBody();
    List<Concrete.Parameter> parameters = new ArrayList<>();
    Expression expr = lamExpr;
    for (; lamExpr != null; lamExpr = expr.cast(LamExpression.class)) {
      if (hasFlag(PrettyPrinterFlag.SHOW_TYPES_IN_LAM) || shouldBeVerbose(lamExpr.getParameters()) || convertParameters(lamExpr.getParameters())) {
        visitDependentLink(lamExpr.getParameters(), parameters, true);
      } else {
        SingleDependentLink params = lamExpr.getParameters();
        Set<Variable> freeVars = myFreeVariablesCollector.getFreeVariables(params.getNextTyped(null));
        for (SingleDependentLink link = params; link.hasNext(); link = link.getNext()) {
          parameters.add(cName(link, link.isExplicit(), makeLocalReference(link, freeVars, false)));
        }
      }
      expr = lamExpr.getBody();
    }

    Concrete.LamExpression result = cLam(parameters, convertExpr(expr));
    return body.isInstance(ClassCallExpression.class) ? result : etaReduce(result);
  }

  private void visitDependentLink(DependentLink parameters, List<? super Concrete.TypeParameter> args, boolean isNamed) {
    visitDependentLink(parameters, args, isNamed, false);
  }

  private void visitDependentLink(DependentLink parameters, List<? super Concrete.TypeParameter> args, boolean isNamed, boolean genName) {
    List<Referable> referableList = new ArrayList<>(3);
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      Set<Variable> freeVars = myFreeVariablesCollector.getFreeVariables(link1);
      for (; link != link1; link = link.getNext()) {
        referableList.add(makeLocalReference(link, freeVars, genName || !link.isExplicit()));
      }

      Referable referable = makeLocalReference(link, freeVars, genName || !link.isExplicit());
      if (referable == null && !isNamed && referableList.isEmpty()) {
        args.add(new Concrete.TypeParameter(link.isExplicit(), convertExpr(link.getTypeExpr()), link.isProperty()));
      } else {
        referableList.add(referable);
        args.add(new Concrete.TelescopeParameter(null, link.isExplicit(), new ArrayList<>(referableList), convertExpr(link.getTypeExpr()), link.isProperty()));
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
    Concrete.Expression result = convertExpr(expr);
    for (int i = parameters.size() - 1; i >= 0; i--) {
      result = cPi(parameters.get(i), result);
    }
    return result;
  }

  @Override
  public Concrete.Expression visitUniverse(UniverseExpression expr, Void params) {
    return visitSort(expr.getSort());
  }

  private Concrete.LevelExpression visitLevelNull(Level level, boolean showStdVar) {
    return level != null && (showStdVar || level.isClosed() || (!level.isVarOnly() || level.getVar() != LevelVariable.PVAR && level.getVar() != LevelVariable.HVAR) && hasFlag(PrettyPrinterFlag.SHOW_LEVELS)) ? visitLevel(level) : null;
  }

  private List<Concrete.LevelExpression> visitLevelsNull(List<Level> levels, boolean showStdVar) {
    if (levels.size() == 1) {
      Concrete.LevelExpression result = visitLevelNull(levels.get(0), showStdVar);
      return result == null ? null : new SingletonList<>(result);
    }

    List<Concrete.LevelExpression> result = new ArrayList<>(levels.size());
    for (Level level : levels) {
      result.add(visitLevel(level));
    }
    return result;
  }

  private Concrete.UniverseExpression visitSort(Sort sort) {
    return cUniverse(sort.isOmega() ? new Concrete.PLevelExpression(null) : visitLevelNull(sort.getPLevel(), false), visitLevelNull(sort.getHLevel(), false));
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
    } else {
      if (!hasFlag(PrettyPrinterFlag.SHOW_LEVELS)) {
        return null;
      }
      result = new Concrete.VarLevelExpression(null, new LocalReferable(level.getVar().getName()), level.getVar() instanceof InferenceLevelVariable, level.getVar().getType());
    }

    for (int i = 0; i < level.getConstant(); i++) {
      result = new Concrete.SucLevelExpression(null, result);
    }

    if (level.getMaxConstant() > 0 || level.getMaxConstant() == 0 && level.getVar() != null && level.getVar().getType() == LevelVariable.LvlType.HLVL) {
      result = new Concrete.MaxLevelExpression(null, result, visitLevel(new Level(level.getMaxConstant())));
    }

    return result;
  }

  @Override
  public Concrete.Expression visitError(ErrorExpression expr, Void params) {
    return cGoal(expr.isGoal() ? expr.getGoalName() : "error", expr.getExpression() == null ? null : convertExpr(expr.getExpression()));
  }

  @Override
  public Concrete.Expression visitTuple(TupleExpression expr, Void params) {
    List<Concrete.Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(convertExpr(field));
    }
    Concrete.Expression result = cTuple(expr, fields);
    if (hasFlag(PrettyPrinterFlag.SHOW_TUPLE_TYPE) || getVerboseLevel(expr) > 0 || convertSubexpr(expr.getSigmaType())) {
      result = new Concrete.TypedExpression(expr, result, visitSigma(expr.getSigmaType(), null));
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
        } else if (list.get(i) instanceof ClassField field) {
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
        return convertExpr(FunCallExpression.make(expr.getField() == 0 ? Prelude.DIV : Prelude.MOD, funCall.getLevels(), new ArrayList<>(funCall.getDefCallArguments())));
      }
    }
    return cProj(convertExpr(expr.getExpression()), expr.getField());
  }

  @Override
  public Concrete.Expression visitNew(NewExpression expr, Void params) {
    if (expr.getRenewExpression() == null) {
      return cNew(visitClassCall(expr.getClassCall(), null));
    } else {
      return cNew(cClassExt(convertExpr(expr.getRenewExpression()), visitClassFieldImpls(expr.getClassCall(), null)));
    }
  }

  @Override
  public Concrete.Expression visitPEval(PEvalExpression expr, Void params) {
    return cEval(true, convertExpr(expr.getExpression()));
  }

  @Override
  public Concrete.Expression visitBox(BoxExpression expr, Void params) {
    return hasFlag(PrettyPrinterFlag.SHOW_PROOFS) ? new Concrete.BoxExpression(null, convertExpr(expr.getExpression())) : generateHiddenGoal(null);
  }

  private Concrete.Pattern makeLetClausePattern(LetClausePattern pattern) {
    if (pattern == null) return null;
    if (pattern.getName() != null) {
      return new Concrete.NamePattern(null, true, new LocalReferable(pattern.getName()), null);
    }
    List<? extends LetClausePattern> patterns = pattern.getPatterns();
    if (patterns == null) return null;
    List<Concrete.Pattern> cPatterns = new ArrayList<>(patterns.size());
    for (LetClausePattern subpattern : patterns) {
      Concrete.Pattern cSubpattern = makeLetClausePattern(subpattern);
      if (cSubpattern == null) return null;
      cPatterns.add(cSubpattern);
    }
    return new Concrete.TuplePattern(null, cPatterns, null);
  }

  @Override
  public Concrete.Expression visitLet(LetExpression letExpression, Void params) {
    boolean isHave = true;
    List<Concrete.LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (HaveClause clause : letExpression.getClauses()) {
      if (clause instanceof LetClause) {
        isHave = false;
      }
      Concrete.Pattern pattern;
      if (clause.getPattern().getName() != null) {
        pattern = new Concrete.NamePattern(null, true, makeLocalReference(clause, myFreeVariablesCollector.getFreeVariables(clause), false), null);
      } else {
        pattern = makeLetClausePattern(clause.getPattern());
      }
      if (pattern == null) {
        Referable referable = makeLocalReference(clause, myFreeVariablesCollector.getFreeVariables(clause), false);
        if (referable != null) {
          clauses.add(new Concrete.LetClause(referable, Collections.emptyList(), null, convertExpr(clause.getExpression())));
        }
      } else {
        clauses.add(new Concrete.LetClause(pattern, null, convertExpr(clause.getExpression())));
      }
    }

    Concrete.Expression expr = convertExpr(letExpression.getExpression());
    return clauses.isEmpty() ? expr : new Concrete.LetExpression(null, isHave, letExpression.isStrict(), clauses, expr);
  }

  @Override
  public Concrete.Expression visitCase(CaseExpression expr, Void params) {
    List<Concrete.CaseArgument> arguments = new ArrayList<>(expr.getArguments().size());
    if (hasFlag(PrettyPrinterFlag.SHOW_CASE_RESULT_TYPE) || convertParameters(expr.getParameters())) {
      List<Concrete.TypeParameter> parameters = new ArrayList<>();
      visitDependentLink(expr.getParameters(), parameters, true);
      int i = 0;
      for (Concrete.TypeParameter parameter : parameters) {
        for (Referable ref : parameter.getReferableList()) {
          arguments.add(new Concrete.CaseArgument(convertExpr(expr.getArguments().get(i++)), ref, parameter.getType()));
        }
      }
    } else {
      for (Expression argument : expr.getArguments()) {
        arguments.add(new Concrete.CaseArgument(convertExpr(argument), null, null));
      }
    }

    Concrete.Expression resultType = null;
    Concrete.Expression resultTypeLevel = null;
    if (hasFlag(PrettyPrinterFlag.SHOW_CASE_RESULT_TYPE) && !(expr.getResultType() instanceof ErrorExpression) || convertSubexprs(Arrays.asList(expr.getResultType(), expr.getResultTypeLevel()))) {
      resultType = convertExpr(expr.getResultType());
      if (expr.getResultTypeLevel() != null) {
        resultTypeLevel = convertExpr(expr.getResultTypeLevel());
      }
    }

    return cCase(expr.isSCase(), arguments, resultType, resultTypeLevel, visitElimBody(expr.getParameters(), expr.getElimBody()));
  }

  private List<Concrete.FunctionClause> visitElimBody(DependentLink parameters, ElimBody body) {
    List<Concrete.FunctionClause> clauses = new ArrayList<>();
    for (ElimClause<Pattern> clause : body.getClauses()) {
      clauses.add(cClause(visitPatterns(parameters, clause.getPatterns()), clause.getExpression() == null ? null : convertExpr(clause.getExpression())));
    }
    return clauses;
  }

  private List<Concrete.Pattern> visitPatterns(DependentLink parameters, List<? extends Pattern> patterns) {
    if (patterns == null) return null;
    List<Concrete.Pattern> result = new ArrayList<>();
    for (Pattern pattern : patterns) {
      visitElimPattern(pattern, parameters.isExplicit(), result);
      parameters = parameters.getNext();
    }
    return result;
  }

  private void visitElimPattern(Pattern pattern, boolean isExplicit, List<Concrete.Pattern> patterns) {
    if (pattern instanceof BindingPattern) {
      DependentLink link = pattern.getFirstBinding();
      patterns.add(cNamePattern(isExplicit, makeLocalReference(link, myFreeVariablesCollector.getFreeVariables(link.getNextTyped(null)), false)));
    } else if (pattern instanceof EmptyPattern) {
      patterns.add(cEmptyPattern(isExplicit));
    } else {
      Definition def = pattern.getConstructor();
      if (def == Prelude.ZERO) {
        patterns.add(new Concrete.NumberPattern(null, 0, null));
      } else {
        List<Concrete.Pattern> subPatterns = new ArrayList<>();
        DependentLink param = pattern.getParameters();
        for (Pattern subPattern : pattern.getSubPatterns()) {
          visitElimPattern(subPattern, param.isExplicit(), subPatterns);
          if (param.hasNext()) {
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
    return convertExpr(expr.getExpression());
  }

  @Override
  public Concrete.Expression visitInteger(IntegerExpression expr, Void params) {
    return new Concrete.NumericLiteral(null, expr.getBigInteger());
  }

  @Override
  public Concrete.Expression visitString(StringExpression expr, Void params) {
    return new Concrete.StringLiteral(null, expr.getString());
  }

  @Override
  public Concrete.Expression visitTypeConstructor(TypeConstructorExpression expr, Void params) {
    return convertExpr(expr.getArgument());
  }

  @Override
  public Concrete.Expression visitTypeDestructor(TypeDestructorExpression expr, Void params) {
    return convertExpr(expr.getArgument());
  }

  @Override
  public Concrete.Expression visitArray(ArrayExpression expr, Void params) {
    Concrete.Expression elementsType = hasFlag(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS) && hasFlag(PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS) || convertSubexpr(expr.getElementsType()) ? convertExpr(expr.getElementsType()) : null;

    Concrete.Expression result;
    if (expr.getTail() == null) {
      result = new Concrete.ReferenceExpression(null, Prelude.EMPTY_ARRAY.getReferable());
      if (elementsType != null) {
        result = Concrete.AppExpression.make(null, result, elementsType, false);
      }
    } else {
      result = convertExpr(expr.getTail());
    }

    for (int i = expr.getElements().size() - 1; i >= 0; i--) {
      List<Concrete.Argument> arguments = new ArrayList<>(3);
      if (elementsType != null) {
        arguments.add(new Concrete.Argument(elementsType, false));
      }
      arguments.add(new Concrete.Argument(convertExpr(expr.getElements().get(i)), true));
      arguments.add(new Concrete.Argument(result, true));
      result = Concrete.AppExpression.make(null, new Concrete.ReferenceExpression(null, Prelude.ARRAY_CONS.getReferable()), arguments);
    }

    return result;
  }

  @Override
  public Concrete.Expression visitPath(PathExpression expr, Void params) {
    return Concrete.AppExpression.make(null, new Concrete.ReferenceExpression(null, Prelude.PATH_CON.getRef()), convertExpr(expr.getArgument()), true);
  }

  @Override
  public Concrete.Expression visitAt(AtExpression expr, Void params) {
    return Concrete.AppExpression.make(null, Concrete.AppExpression.make(null, new Concrete.ReferenceExpression(null, Prelude.AT.getRef()), convertExpr(expr.getPathArgument()), true), convertExpr(expr.getIntervalArgument()), true);
  }

  private FunctionKind visitFunctionKind(CoreFunctionDefinition.Kind kind) {
    return switch (kind) {
      case FUNC -> FunctionKind.FUNC;
      case SFUNC -> FunctionKind.SFUNC;
      case TYPE -> FunctionKind.TYPE;
      case LEMMA -> FunctionKind.LEMMA;
      case INSTANCE -> FunctionKind.INSTANCE;
    };
  }

  public static Concrete.LevelParameters visitLevelParameters(List<? extends LevelVariable> parameters, boolean isPLevels) {
    if (parameters.size() == 1 && parameters.get(0).equals(parameters.get(0).getStd())) {
      return null;
    }
    List<LevelReferable> refs = new ArrayList<>(parameters.size());
    for (LevelVariable var : parameters) {
      refs.add(new DataLevelReferable(null, var.toString(), isPLevels));
    }
    return new Concrete.LevelParameters(null, refs, !(parameters.size() > 1 && parameters.get(0) instanceof ParamLevelVariable && parameters.get(1) instanceof ParamLevelVariable && ((ParamLevelVariable) parameters.get(0)).getSize() > ((ParamLevelVariable) parameters.get(1)).getSize()));
  }

  private Pair<Concrete.LevelParameters, Concrete.LevelParameters> visitLevelParameters(List<? extends LevelVariable> parameters, int n) {
    return new Pair<>(visitLevelParameters(parameters.subList(0, n), true), visitLevelParameters(parameters.subList(n, parameters.size()), false));
  }

  private List<Concrete.FunctionClause> visitIntervalElim(DependentLink parameters, Body body) {
    if (body instanceof ElimBody) {
      return visitElimBody(parameters, (ElimBody) body);
    } else if (body instanceof IntervalElim elim) {
      // TODO: Add interval clauses
      return elim.getOtherwise() == null ? new ArrayList<>() : visitElimBody(parameters, elim.getOtherwise());
    } else if (body == null) {
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Concrete.FunctionDefinition visitFunction(FunctionDefinition def, Void params) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    visitDependentLink(def.getParameters(), parameters, true);
    Pair<Concrete.LevelParameters, Concrete.LevelParameters> pair = visitLevelParameters(def.getLevelParameters(), def.getNumberOfPLevelParameters());
    Body body = def.getReallyActualBody();
    Concrete.FunctionBody cBody;
    if (body instanceof Expression) {
      cBody = new Concrete.TermFunctionBody(null, convertExpr((Expression) body));
    } else if (body == null) {
      ClassCallExpression classCall = def.getResultType().normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
      if (classCall != null && classCall.getNumberOfNotImplementedFields() == 0) {
        cBody = new Concrete.TermFunctionBody(null, new Concrete.NewExpression(null, new Concrete.ReferenceExpression(null, classCall.getDefinition().getRef())));
      } else {
        cBody = new Concrete.ElimFunctionBody(null, Collections.emptyList(), Collections.emptyList());
      }
    } else {
      cBody = new Concrete.ElimFunctionBody(null, Collections.emptyList(), visitIntervalElim(def.getParameters(), body));
    }
    return new Concrete.FunctionDefinition(def.isAxiom() ? FunctionKind.AXIOM : visitFunctionKind(def.getKind()), def.getRef(), pair.proj1, pair.proj2, parameters, convertExpr(def.getResultType()), def.getResultTypeLevel() == null ? null : convertExpr(def.getResultTypeLevel()), cBody);
  }

  @Override
  public Concrete.DataDefinition visitData(DataDefinition def, Void params) {
    Pair<Concrete.LevelParameters, Concrete.LevelParameters> pair = visitLevelParameters(def.getLevelParameters(), def.getNumberOfPLevelParameters());
    List<Concrete.TypeParameter> parameters = new ArrayList<>();
    visitDependentLink(def.getParameters(), parameters, false);
    boolean hasPatterns = !def.getConstructors().isEmpty() && def.getConstructors().get(0).getPatterns() != null;
    List<Concrete.ConstructorClause> constructors = new ArrayList<>();
    Concrete.DataDefinition result = new Concrete.DataDefinition(def.getRef(), pair.proj1, pair.proj2, parameters, hasPatterns ? Collections.emptyList() : null, def.isTruncated(), def.isTruncated() ? visitSort(def.getSort()) : null, constructors);
    for (Constructor constructor : def.getConstructors()) {
      constructors.add(new Concrete.ConstructorClause(null, visitPatterns(def.getParameters(), constructor.getPatterns()), Collections.singletonList(visitConstructor(constructor, result))));
    }
    return result;
  }

  private Concrete.Constructor visitConstructor(Constructor constructor, Concrete.DataDefinition dataDef) {
    List<Concrete.TypeParameter> parameters = new ArrayList<>();
    visitDependentLink(constructor.getParameters(), parameters, false);
    List<Concrete.FunctionClause> clauses = visitIntervalElim(constructor.getParameters(), constructor.getBody());
    return new Concrete.Constructor(constructor.getRef(), dataDef, parameters, Collections.emptyList(), clauses == null ? Collections.emptyList() : clauses, false);
  }

  @Override
  public Concrete.Constructor visitConstructor(Constructor constructor, Void params) {
    return visitConstructor(constructor, (Concrete.DataDefinition) null);
  }

  @Override
  public Concrete.ClassDefinition visitClass(ClassDefinition def, Void params) {
    Pair<Concrete.LevelParameters, Concrete.LevelParameters> pair = visitLevelParameters(def.getLevelParameters(), def.getNumberOfPLevelParameters());
    List<Concrete.ReferenceExpression> superClasses = new ArrayList<>(def.getSuperClasses().size());
    for (ClassDefinition superClass : def.getSuperClasses()) {
      superClasses.add(new Concrete.ReferenceExpression(null, superClass.getRef()));
    }

    List<Concrete.ClassElement> elements = new ArrayList<>();
    Concrete.ClassDefinition result = new Concrete.ClassDefinition(def.getRef(), pair.proj1, pair.proj2, def.isRecord(), false, superClasses, elements);
    for (ClassField field : def.getPersonalFields()) {
      elements.add(visitField(field, result));
    }
    for (Map.Entry<ClassField, AbsExpression> entry : def.getImplemented()) {
      boolean implementedHere = true;
      for (ClassDefinition superClass : def.getSuperClasses()) {
        if (superClass.isImplemented(entry.getKey())) {
          implementedHere = false;
          break;
        }
      }
      if (!implementedHere) continue;
      elements.add(new Concrete.ClassFieldImpl(null, entry.getKey().getRef(), convertExpr(entry.getValue().getExpression()), null));
    }
    // TODO: Add other elements of the class
    return result;
  }

  private Concrete.ClassField visitField(ClassField field, Concrete.ClassDefinition classDef) {
    ClassFieldKind kind;
    if (field.isProperty()) {
      kind = ClassFieldKind.ANY;
    } else {
      Sort sort = field.getType().getCodomain().getSortOfType();
      kind = sort == null || sort.isProp() ? ClassFieldKind.FIELD : ClassFieldKind.ANY;
    }

    List<Concrete.TypeParameter> parameters = new ArrayList<>();
    Concrete.Expression type = convertExpr(field.getType().getCodomain());
    while (type instanceof Concrete.PiExpression) {
      parameters.addAll(((Concrete.PiExpression) type).getParameters());
      type = ((Concrete.PiExpression) type).getCodomain();
    }

    return new Concrete.ClassField(field.getReferable(), classDef, field.getReferable().isExplicitField(), kind, parameters, type, field.getTypeLevel() == null ? null : convertExpr(field.getTypeLevel()), false);
  }

  @Override
  public Concrete.ClassField visitField(ClassField field, Void params) {
    return visitField(field, (Concrete.ClassDefinition) null);
  }

  @Override
  public Concrete.GeneralDefinition visitMeta(MetaTopDefinition def, Void params) {
    Pair<Concrete.LevelParameters, Concrete.LevelParameters> pair = visitLevelParameters(def.getLevelParameters(), def.getNumberOfPLevelParameters());
    List<Concrete.Parameter> parameters = new ArrayList<>();
    visitDependentLink(def.getParameters(), parameters, true);
    return new DefinableMetaDefinition(def.getReferable(), pair.proj1, pair.proj2, parameters, null);
  }
}
