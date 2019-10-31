package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.Variable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.NamedUnresolvedReference;
import org.arend.naming.reference.Referable;
import org.arend.naming.renamer.ReferableRenamer;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.patternmatching.Util;
import org.arend.typechecking.visitor.VoidConcreteVisitor;

import java.util.*;

import static org.arend.term.concrete.ConcreteExpressionFactory.*;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Concrete.Expression> {
  public enum Flag { SHOW_COERCE_DEFINITIONS, SHOW_CON_PARAMS, SHOW_FIELD_INSTANCE, SHOW_IMPLICIT_ARGS, SHOW_TYPES_IN_LAM, SHOW_PREFIX_PATH, SHOW_BIN_OP_IMPLICIT_ARGS, SHOW_CASE_RESULT_TYPE, SHOW_INFERENCE_LEVEL_VARS }

  private final PrettyPrinterConfig myConfig;
  private final CollectFreeVariablesVisitor myFreeVariablesCollector;
  private final ReferableRenamer myRenamer;

  private ToAbstractVisitor(PrettyPrinterConfig config, CollectFreeVariablesVisitor collector, ReferableRenamer renamer) {
    myConfig = config;
    myFreeVariablesCollector = collector;
    myRenamer = renamer;
  }

  public static Concrete.Expression convert(Expression expression, PrettyPrinterConfig config) {
    CollectFreeVariablesVisitor collector = new CollectFreeVariablesVisitor(config.getExpressionFlags());
    Set<Variable> variables = new HashSet<>();
    NormalizeVisitor.Mode mode = config.getNormalizationMode();
    if (mode != null) {
      expression = expression.normalize(mode);
    }
    expression.accept(collector, variables);
    ReferableRenamer renamer = new ReferableRenamer();
    ToAbstractVisitor visitor = new ToAbstractVisitor(config, collector, renamer);
    renamer.generateFreshNames(variables);
    return expression.accept(visitor, null);
  }

  public static Concrete.LevelExpression convert(Level level) {
    return new ToAbstractVisitor(new PrettyPrinterConfig() {
        @Override
        public EnumSet<Flag> getExpressionFlags() {
          return EnumSet.of(Flag.SHOW_INFERENCE_LEVEL_VARS);
        }
      }, null, new ReferableRenamer()).visitLevel(level);
  }

  private boolean hasFlag(Flag flag) {
    return myConfig.getExpressionFlags().contains(flag);
  }

  private Concrete.Pattern visitPattern(Pattern pattern, boolean isExplicit) {
    if (pattern instanceof BindingPattern) {
      return cNamePattern(isExplicit, makeLocalReference(((BindingPattern) pattern).getBinding(), myFreeVariablesCollector.getFreeVariables(((BindingPattern) pattern).getBinding().getNextTyped(null)), false));
    }
    if (pattern instanceof EmptyPattern) {
      return cEmptyPattern(isExplicit);
    }
    if (pattern instanceof ConstructorPattern) {
      Definition def = ((ConstructorPattern) pattern).getDefinition();
      return def instanceof Constructor
        ? cConPattern(isExplicit, def.getReferable(), visitPatterns(((ConstructorPattern) pattern).getArguments(), def.getParameters()))
        : cTuplePattern(isExplicit, visitPatterns(((ConstructorPattern) pattern).getArguments(), EmptyDependentLink.getInstance()));
    }
    throw new IllegalStateException();
  }

  private List<Concrete.Pattern> visitPatterns(List<Pattern> patterns, DependentLink parameters) {
    List<Concrete.Pattern> result = new ArrayList<>(patterns.size());
    for (Pattern pattern : patterns) {
      result.add(visitPattern(pattern, !parameters.hasNext() || parameters.isExplicit()));
      if (parameters.hasNext()) {
        parameters = parameters.getNext();
      }
    }
    return result;
  }

  private Concrete.Expression checkPath(DataCallExpression expr) {
    if (expr.getDefinition() != Prelude.PATH || hasFlag(Flag.SHOW_PREFIX_PATH)) {
      return null;
    }

    LamExpression expr1 = expr.getDefCallArguments().get(0).cast(LamExpression.class);
    if (expr1 != null) {
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return cBinOp(expr.getDefCallArguments().get(1).accept(this, null), Prelude.PATH_INFIX.getReferable(), hasFlag(Flag.SHOW_BIN_OP_IMPLICIT_ARGS) ? expr1.getBody().accept(this, null) : null, expr.getDefCallArguments().get(2).accept(this, null));
      }
    }
    return null;
  }

  @Override
  public Concrete.Expression visitApp(AppExpression expr, Void params) {
    List<Expression> args = new ArrayList<>();
    Expression fun = expr.getArguments(args);
    Concrete.Expression result = fun.accept(this, null);
    boolean[] isExplicit = new boolean[args.size()];
    getArgumentsExplicitness(fun, isExplicit);
    for (int index = 0; index < args.size(); index++) {
      result = visitApp(result, args.get(index), isExplicit[index]);
    }
    return result;
  }

  private void getArgumentsExplicitness(Expression expr, boolean[] isExplicit) {
    List<SingleDependentLink> params = new ArrayList<>(isExplicit.length);
    Expression type = expr.getType();
    if (type != null) {
      type.getPiParameters(params, false);
      for (int i = 0; i < isExplicit.length; i++) {
        isExplicit[i] = i >= params.size() || params.get(i).isExplicit();
      }
    }
  }

  private Concrete.Expression visitApp(Concrete.Expression function, Expression argument, boolean isExplicit) {
    Concrete.Expression arg = isExplicit || hasFlag(Flag.SHOW_IMPLICIT_ARGS) ? argument.accept(this, null) : null;
    return arg != null ? Concrete.AppExpression.make(null, function, arg, isExplicit) : function;
  }

  private void visitArgument(Expression arg, boolean isExplicit, List<Concrete.Argument> arguments) {
    ReferenceExpression refExpr = arg.cast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding().isHidden()) {
      if (isExplicit) {
        arguments.add(new Concrete.Argument(new Concrete.ThisExpression(null, null), true));
      }
    } else if (isExplicit || hasFlag(Flag.SHOW_IMPLICIT_ARGS)) {
      arguments.add(new Concrete.Argument(arg.accept(this, null), isExplicit));
    }
  }

  private Concrete.Expression visitParameters(Concrete.Expression expr, DependentLink parameters, List<? extends Expression> arguments) {
    List<Concrete.Argument> concreteArguments = new ArrayList<>(arguments.size());
    for (Expression arg : arguments) {
      if (parameters.isExplicit() || hasFlag(Flag.SHOW_IMPLICIT_ARGS)) {
        visitArgument(arg, parameters.isExplicit(), concreteArguments);
      }
      parameters = parameters.getNext();
    }
    return Concrete.AppExpression.make(null, expr, concreteArguments);
  }

  private static Concrete.ReferenceExpression makeReference(Referable referable) {
    return cVar(referable == null ? new NamedUnresolvedReference(null, "\\this") : referable);
  }

  @Override
  public Concrete.Expression visitDefCall(DefCallExpression expr, Void params) {
    if (expr.getDefinition().isHideable() && !hasFlag(Flag.SHOW_COERCE_DEFINITIONS)) {
      int index = 0;
      for (DependentLink link = expr.getDefinition().getParameters(); link.hasNext(); link = link.getNext()) {
        if (index == expr.getDefinition().getVisibleParameter()) {
          return expr.getDefCallArguments().get(index).accept(this, null);
        }
        index++;
      }
    }

    return visitParameters(makeReference(expr.getDefinition().getReferable()), expr.getDefinition().getParameters(), expr.getDefCallArguments());
  }

  @Override
  public Concrete.Expression visitFieldCall(FieldCallExpression expr, Void params) {
    if (expr.getDefinition().isHideable() && !hasFlag(Flag.SHOW_COERCE_DEFINITIONS)) {
      return expr.getArgument().accept(this, null);
    }

    Concrete.ReferenceExpression result = makeReference(expr.getDefinition().getReferable());
    if (hasFlag(Flag.SHOW_FIELD_INSTANCE)) {
      ReferenceExpression refExpr = expr.getArgument().cast(ReferenceExpression.class);
      if (refExpr != null && refExpr.getBinding().isHidden()) {
        return result;
      }

      Concrete.Expression arg = expr.getArgument().accept(this, null);
      if (refExpr != null && arg instanceof Concrete.ReferenceExpression) {
        return new Concrete.ReferenceExpression(null, ref(((Concrete.ReferenceExpression) arg).getReferent().textRepresentation() + "." + result.getReferent().textRepresentation()));
      } else {
        return Concrete.AppExpression.make(null, result, arg, false);
      }
    }
    return result;
  }

  @Override
  public Concrete.Expression visitConCall(ConCallExpression expr, Void params) {
    Concrete.Expression result = makeReference(expr.getDefinition().getReferable());
    if (expr.getDefinition().status().headerIsOK() && hasFlag(Flag.SHOW_CON_PARAMS)) {
      List<Concrete.Argument> arguments = new ArrayList<>(expr.getDataTypeArguments().size());
      for (Expression arg : expr.getDataTypeArguments()) {
        visitArgument(arg, false, arguments);
      }
      result = Concrete.AppExpression.make(null, result, arguments);
    }
    return visitParameters(result, expr.getDefinition().getParameters(), expr.getDefCallArguments());
  }

  @Override
  public Concrete.Expression visitDataCall(DataCallExpression expr, Void params) {
    Concrete.Expression result = checkPath(expr);
    return result != null ? result : visitDefCall(expr, params);
  }

  @Override
  public Concrete.Expression visitClassCall(ClassCallExpression expr, Void params) {
    List<Concrete.Argument> arguments = new ArrayList<>();
    List<Concrete.ClassFieldImpl> statements = new ArrayList<>();
    boolean canBeArgument = true;
    for (ClassField field : expr.getDefinition().getFields()) {
      Expression implementation = expr.getImplementationHere(field);
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

    Concrete.Expression defCallExpr = Concrete.AppExpression.make(null, makeReference(expr.getDefinition().getReferable()), arguments);
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
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : new Concrete.InferenceReferenceExpression(null, expr.getVariable());
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
    VoidConcreteVisitor<Void,Void> visitor = new VoidConcreteVisitor<Void,Void>() {
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

    for (int j = parameters.size() - 1; j >= 0; j--) {
      List<? extends Referable> refList = parameters.get(j).getReferableList();
      if (numberOfVars == refList.size()) {
        parameters = parameters.subList(0, j);
        break;
      }
      if (numberOfVars < refList.size()) {
        parameters = new ArrayList<>(parameters.subList(0, j));
        Concrete.Parameter param = parameters.get(j);
        parameters.add(new Concrete.TelescopeParameter(param.getData(), param.isExplicit(), param.getReferableList().subList(0, refList.size() - numberOfVars), param.getType()));
        break;
      }
      numberOfVars -= refList.size();
    }

    Concrete.Expression body = args.size() == numberOfVars ? fun : Concrete.AppExpression.make(lamExpr.body.getData(), fun, args.subList(0, args.size() - numberOfVars));
    return parameters.isEmpty() ? body : new Concrete.LamExpression(lamExpr.getData(), parameters, body);
  }

  @Override
  public Concrete.Expression visitLam(LamExpression lamExpr, Void ignore) {
    Expression body = lamExpr.getBody();
    List<Concrete.Parameter> parameters = new ArrayList<>();
    Expression expr = lamExpr;
    for (; lamExpr != null; lamExpr = expr.cast(LamExpression.class)) {
      if (hasFlag(Flag.SHOW_TYPES_IN_LAM)) {
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
    return level.isVarOnly() ? null : visitLevel(level);
  }

  private Concrete.Expression visitSort(Sort sorts) {
    return cUniverse(visitLevelNull(sorts.getPLevel()), visitLevelNull(sorts.getHLevel()));
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
      if (!hasFlag(Flag.SHOW_INFERENCE_LEVEL_VARS)) {
        return null;
      }
      result = new Concrete.InferVarLevelExpression(null, (InferenceLevelVariable) level.getVar());
    } else {
      throw new IllegalStateException();
    }

    if (level.getMaxConstant() > 0 || level.getMaxConstant() == 0 && level.getVar() == LevelVariable.HVAR) {
      result = new Concrete.MaxLevelExpression(null, result, visitLevel(new Level(level.getMaxConstant())));
    }

    for (int i = 0; i < level.getConstant(); i++) {
      result = new Concrete.SucLevelExpression(null, result);
    }

    return result;
  }

  @Override
  public Concrete.Expression visitError(ErrorExpression expr, Void params) {
    return cGoal(expr.getError() instanceof GoalError ? ((GoalError) expr.getError()).name : "error", expr.getExpression() == null ? null : expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitTuple(TupleExpression expr, Void params) {
    List<Concrete.Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return cTuple(fields);
  }

  @Override
  public Concrete.Expression visitSigma(SigmaExpression expr, Void params) {
    List<Concrete.TypeParameter> parameters = new ArrayList<>();
    visitDependentLink(expr.getParameters(), parameters, false);
    return cSigma(parameters);
  }

  @Override
  public Concrete.Expression visitProj(ProjExpression expr, Void params) {
    return cProj(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Concrete.Expression visitNew(NewExpression expr, Void params) {
    return cNew(visitClassCall(expr.getExpression(), null));
  }

  @Override
  public Concrete.Expression visitPEval(PEvalExpression expr, Void params) {
    return cEval(true, expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitLet(LetExpression letExpression, Void params) {
    List<Concrete.LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      Concrete.Expression term = clause.getExpression().accept(this, null);
      Referable referable = makeLocalReference(clause, myFreeVariablesCollector.getFreeVariables(clause), false);
      if (referable != null) {
        clauses.add(clet(referable, Collections.emptyList(), null, term));
      }
    }

    Concrete.Expression expr = letExpression.getExpression().accept(this, null);
    return clauses.isEmpty() ? expr : new Concrete.LetExpression(null, letExpression.isStrict(), clauses, expr);
  }

  @Override
  public Concrete.Expression visitCase(CaseExpression expr, Void params) {
    List<Concrete.CaseArgument> arguments = new ArrayList<>(expr.getArguments().size());
    for (Expression argument : expr.getArguments()) {
      arguments.add(new Concrete.CaseArgument(argument.accept(this, null), null, null));
    }

    Concrete.Expression resultType = null;
    Concrete.Expression resultTypeLevel = null;
    if (hasFlag(Flag.SHOW_CASE_RESULT_TYPE)) {
      resultType = expr.getResultType().accept(this, null);
      if (expr.getResultType() != null) {
        resultTypeLevel = expr.getResultTypeLevel().accept(this, null);
      }
    }

    return cCase(expr.isSFunc(), arguments, resultType, resultTypeLevel, expr.getElimTree() != null ? visitElimTree(expr.getElimTree()) : Collections.emptyList());
  }

  private List<Concrete.FunctionClause> visitElimTree(ElimTree elimTree) {
    List<Concrete.FunctionClause> clauses = new ArrayList<>();
    new Util.ElimTreeWalker((patterns, expr) -> clauses.add(cClause(visitPatterns(patterns, new Patterns(patterns).getFirstBinding()), expr.accept(this, null)))).walk(elimTree);
    return clauses;
  }

  @Override
  public Concrete.Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Concrete.Expression visitInteger(IntegerExpression expr, Void params) {
    return new Concrete.NumericLiteral(null, expr.getBigInteger());
  }
}
