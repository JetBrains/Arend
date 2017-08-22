package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.LocalReference;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.local.GoalError;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.Util;

import java.util.*;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Concrete.Expression<Position>> {
  public enum Flag {SHOW_CON_DATA_TYPE, SHOW_CON_PARAMS, SHOW_IMPLICIT_ARGS, SHOW_TYPES_IN_LAM, SHOW_PREFIX_PATH, SHOW_BIN_OP_IMPLICIT_ARGS}

  private final EnumSet<Flag> myFlags;
  private final CollectFreeVariablesVisitor myFreeVariablesCollector;
  private final Map<Binding, Referable> myNames;

  private static final String unnamed = "unnamed";

  private ToAbstractVisitor(EnumSet<Flag> flags, CollectFreeVariablesVisitor collector, Map<Binding, Referable> names) {
    myFlags = flags;
    myFreeVariablesCollector = collector;
    myNames = names;
  }

  public static Concrete.Expression<Position> convert(Expression expression, EnumSet<Flag> flags) {
    CollectFreeVariablesVisitor collector = new CollectFreeVariablesVisitor();
    Set<Variable> variables = new HashSet<>();
    expression.accept(collector, variables);
    Map<Binding, Referable> names = new HashMap<>();
    ToAbstractVisitor visitor = new ToAbstractVisitor(flags, collector, names);
    for (Variable variable : variables) {
      if (variable instanceof Binding) {
        names.put((Binding) variable, new LocalReference(visitor.getFreshName((Binding) variable, variables)));
      }
    }
    return expression.accept(visitor, null);
  }

  public static Concrete.LevelExpression<Position> convert(Level level) {
    return new ToAbstractVisitor(EnumSet.noneOf(Flag.class), null, Collections.emptyMap()).visitLevel(level);
  }

  private String getFreshName(Binding binding, Set<Variable> variables) {
    String name = binding.getName();
    if (name == null) {
      name = unnamed;
    }

    String prefix = null;
    Set<Integer> indices = Collections.emptySet();
    for (Variable variable : variables) {
      if (variable != binding) {
        String otherName = null;
        if (variable instanceof Binding) {
          Referable referable = myNames.get(variable);
          if (referable != null) {
            otherName = referable.getName();
          }
        } else {
          otherName = variable.getName();
        }

        if (otherName != null) {
          if (prefix == null) {
            prefix = getPrefix(name);
          }
          if (prefix.equals(getPrefix(otherName))) {
            if (indices.isEmpty()) {
              indices = new HashSet<>();
            }
            indices.add(getSuffix(otherName));
          }
        }
      }
    }

    if (!indices.isEmpty()) {
      int suffix = getSuffix(name);
      if (indices.contains(suffix)) {
        suffix = 0;
        while (indices.contains(suffix)) {
          suffix++;
        }
        name = suffix == 0 ? prefix : prefix + suffix;
      }
    }

    return name;
  }

  private static String getPrefix(String name) {
    int i = name.length() - 1;
    while (Character.isDigit(name.charAt(i))) {
      i--;
    }
    return name.substring(0, i + 1);
  }

  private static int getSuffix(String name) {
    int i = name.length() - 1;
    while (Character.isDigit(name.charAt(i))) {
      i--;
    }
    return i + 1 == name.length() ? 0 : Integer.valueOf(name.substring(i + 1, name.length()));
  }

  private Concrete.Pattern<Position> visitPattern(Pattern pattern, boolean isExplicit) {
    if (pattern instanceof BindingPattern) {
      return cNamePattern(isExplicit, ref(((BindingPattern) pattern).getBinding().getName()));
    }
    if (pattern instanceof EmptyPattern) {
      return cEmptyPattern(isExplicit);
    }
    if (pattern instanceof ConstructorPattern) {
      return cConPattern(isExplicit, ((ConstructorPattern) pattern).getConstructor().getConcreteDefinition(), visitPatterns(((ConstructorPattern) pattern).getArguments(), ((ConstructorPattern) pattern).getConstructor().getParameters()));
    }
    throw new IllegalStateException();
  }

  private List<Concrete.Pattern<Position>> visitPatterns(List<Pattern> patterns, DependentLink parameters) {
    List<Concrete.Pattern<Position>> result = new ArrayList<>(patterns.size());
    for (Pattern pattern : patterns) {
      result.add(visitPattern(pattern, !parameters.hasNext() || parameters.isExplicit()));
      if (parameters.hasNext()) {
        parameters = parameters.getNext();
      }
    }
    return result;
  }

  private Concrete.Expression<Position> checkPath(DataCallExpression expr) {
    if (expr.getDefinition() != Prelude.PATH || myFlags.contains(Flag.SHOW_PREFIX_PATH)) {
      return null;
    }

    LamExpression expr1 = expr.getDefCallArguments().get(0).checkedCast(LamExpression.class);
    if (expr1 != null) {
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return cBinOp(expr.getDefCallArguments().get(1).accept(this, null), Prelude.PATH_INFIX.getConcreteDefinition(), expr.getDefCallArguments().get(2).accept(this, null));
      }
    }
    return null;
  }

  private Concrete.Expression<Position> checkBinOp(Expression expr) {
    List<Expression> args = new ArrayList<>(2);
    Expression fun = expr;
    while (fun.isInstance(AppExpression.class)) {
      args.add(fun.cast(AppExpression.class).getArgument());
      fun = fun.cast(AppExpression.class).getFunction();
    }
    Collections.reverse(args);
    DefCallExpression defCall = fun.checkedCast(DefCallExpression.class);
    ReferenceExpression refExpr = fun.checkedCast(ReferenceExpression.class);

    if (refExpr == null && defCall == null || PrettyPrintVisitor.isPrefix(defCall != null ? defCall.getDefinition().getName() : refExpr.getBinding().getName())) {
      return null;
    }

    int defCallArgsSize = defCall != null ? defCall.getDefCallArguments().size() : 0;
    boolean[] isExplicit = new boolean[defCallArgsSize + args.size()];
    Expression[] visibleArgs = new Expression[2];
    int i = 0;

    if (defCall != null) {
      for (DependentLink link = defCall.getDefinition().getParameters(); link.hasNext(); link = link.getNext()) {
        isExplicit[i++] = link.isExplicit();
      }
      if (!getArgumentsExplicitness(defCall, isExplicit, i)) {
        return null;
      }
      if (isExplicit.length < 2 || myFlags.contains(Flag.SHOW_BIN_OP_IMPLICIT_ARGS) && (!isExplicit[0] || !isExplicit[1])) {
        return null;
      }

      i = 0;
      for (int j = 0; j < defCall.getDefCallArguments().size(); j++) {
        if (isExplicit[j]) {
          if (i == 2) {
            return null;
          }
          visibleArgs[i++] = defCall.getDefCallArguments().get(j);
        }
      }
    }

    for (int j = 0; j < args.size(); j++) {
      if (isExplicit[defCallArgsSize + j]) {
        if (i == 2) {
          return null;
        }
        visibleArgs[i++] = args.get(j);
      }
    }
    return i == 2 ? cBinOp(visibleArgs[0].accept(this, null), defCall != null ? defCall.getDefinition().getConcreteDefinition() : myNames.get(refExpr.getBinding()), visibleArgs[1].accept(this, null)) : null;
  }

  @Override
  public Concrete.Expression<Position> visitApp(AppExpression expr, Void params) {
    Concrete.Expression<Position> result = checkBinOp(expr);
    if (result != null) {
      return result;
    }

    List<Expression> args = new ArrayList<>();
    Expression fun = expr;
    while (fun.isInstance(AppExpression.class)) {
      args.add(fun.cast(AppExpression.class).getArgument());
      fun = fun.cast(AppExpression.class).getFunction();
    }
    Collections.reverse(args);

    result = fun.accept(this, null);
    boolean[] isExplicit = new boolean[args.size()];
    getArgumentsExplicitness(expr.getFunction(), isExplicit, 0);
    for (int index = 0; index < args.size(); index++) {
      result = visitApp(result, args.get(index), isExplicit[index]);
    }
    return result;
  }

  private boolean getArgumentsExplicitness(Expression expr, boolean[] isExplicit, int i) {
    List<SingleDependentLink> params = new ArrayList<>(isExplicit.length - i);
    Expression type = expr.getType();
    if (type == null) {
      return false;
    }

    type.getPiParameters(params, false);
    for (int j = 0; i < isExplicit.length; i++, j++) {
      isExplicit[i] = j >= params.size() || params.get(j).isExplicit();
    }
    return true;
  }

  private Concrete.Expression<Position> visitApp(Concrete.Expression<Position> function, Expression argument, boolean isExplicit) {
    Concrete.Expression<Position> arg = isExplicit || myFlags.contains(Flag.SHOW_IMPLICIT_ARGS) ? argument.accept(this, null) : null;
    return arg != null ? cApps(function, arg, isExplicit) : function;
  }

  private Concrete.Expression<Position> visitParameters(Concrete.Expression<Position> expr, DependentLink parameters, List<? extends Expression> arguments) {
    for (Expression arg : arguments) {
      expr = cApps(expr, arg.accept(this, null), parameters.isExplicit());
      parameters = parameters.getNext();
    }
    return expr;
  }

  private static Concrete.Expression<Position> makeReference(Concrete.Expression<Position> expr, Referable referable) {
    return cDefCall(expr, referable == null ? new UnresolvedReference("\\this") : referable);
  }

  @Override
  public Concrete.Expression<Position> visitDefCall(DefCallExpression expr, Void params) {
    Concrete.Expression<Position> result = checkBinOp(expr);
    if (result != null) {
      return result;
    }
    return visitParameters(makeReference(null, expr.getDefinition().getConcreteDefinition()), expr.getDefinition().getParameters(), expr.getDefCallArguments());
  }

  @Override
  public Concrete.Expression<Position> visitFieldCall(FieldCallExpression expr, Void params) {
    return makeReference(expr.getExpression().accept(this, null), expr.getDefinition().getConcreteDefinition());
  }

  @Override
  public Concrete.Expression<Position> visitConCall(ConCallExpression expr, Void params) {
    Integer num = getNum(expr);
    if (num != null) {
      return cNum(num);
    }

    Concrete.Expression<Position> conParams = null;
    if (expr.getDefinition().status().headerIsOK() && myFlags.contains(Flag.SHOW_CON_PARAMS) && (!expr.getDataTypeArguments().isEmpty() || myFlags.contains(Flag.SHOW_CON_DATA_TYPE))) {
      conParams = expr.getDataTypeExpression().accept(this, null);
    }
    return visitParameters(makeReference(conParams, expr.getDefinition().getConcreteDefinition()), expr.getDefinition().getParameters(), expr.getDefCallArguments());
  }

  @Override
  public Concrete.Expression<Position> visitDataCall(DataCallExpression expr, Void params) {
    Concrete.Expression<Position> result = checkPath(expr);
    return result != null ? result : visitDefCall(expr, params);
  }

  @Override
  public Concrete.Expression<Position> visitClassCall(ClassCallExpression expr, Void params) {
    Collection<Map.Entry<ClassField, Expression>> implHere = expr.getImplementedHere().entrySet();
    Concrete.Expression<Position> enclExpr = null;
    List<Concrete.ClassFieldImpl<Position>> statements = new ArrayList<>(implHere.size());
    for (Map.Entry<ClassField, Expression> entry : implHere) {
      if (entry.getKey().equals(expr.getDefinition().getEnclosingThisField())) {
        enclExpr = entry.getValue().accept(this, params);
      } else {
        statements.add(cImplStatement(entry.getKey().getConcreteDefinition(), entry.getValue().accept(this, params)));
      }
    }

    Concrete.Expression<Position> defCallExpr = makeReference(enclExpr, expr.getDefinition().getConcreteDefinition());
    if (statements.isEmpty()) {
      return defCallExpr;
    } else {
      return cClassExt(defCallExpr, statements);
    }
  }

  @Override
  public Concrete.Expression<Position> visitReference(ReferenceExpression expr, Void params) {
    return makeReference(null, myNames.get(expr.getBinding()));
  }

  @Override
  public Concrete.Expression<Position> visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : new Concrete.InferenceReferenceExpression<>(null, expr.getVariable());
  }

  private LocalReference makeLocalReference(Binding var, Set<Variable> freeVars, boolean nullable) {
    if (nullable && !freeVars.contains(var)) {
      return null;
    }
    LocalReference reference = new LocalReference(getFreshName(var, freeVars));
    myNames.put(var, reference);
    return reference;
  }

  @Override
  public Concrete.Expression<Position> visitLam(LamExpression lamExpr, Void ignore) {
    List<Concrete.Parameter<Position>> parameters = new ArrayList<>();
    Expression expr = lamExpr;
    for (; expr.isInstance(LamExpression.class); expr = expr.cast(LamExpression.class).getBody()) {
      if (myFlags.contains(Flag.SHOW_TYPES_IN_LAM)) {
        visitDependentLink(expr.cast(LamExpression.class).getParameters(), parameters, true);
      } else {
        SingleDependentLink params = expr.cast(LamExpression.class).getParameters();
        Set<Variable> freeVars = myFreeVariablesCollector.getFreeVariables(params.getNextTyped(null));
        for (SingleDependentLink link = params; link.hasNext(); link = link.getNext()) {
          parameters.add(cName(link.isExplicit(), makeLocalReference(link, freeVars, true)));
        }
      }
    }

    return cLam(parameters, expr.accept(this, null));
  }

  private void visitDependentLink(DependentLink parameters, List<? super Concrete.TypeParameter<Position>> args, boolean isNamed) {
    List<Referable> referableList = new ArrayList<>(3);
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      Set<Variable> freeVars = myFreeVariablesCollector.getFreeVariables(link1);
      for (; link != link1; link = link.getNext()) {
        referableList.add(makeLocalReference(link, freeVars, true));
      }

      Referable referable = makeLocalReference(link, freeVars, true);
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
  public Concrete.Expression<Position> visitPi(PiExpression piExpr, Void ignore) {
    List<List<Concrete.TypeParameter<Position>>> parameters = new ArrayList<>();
    Expression expr = piExpr;
    for (; expr.isInstance(PiExpression.class); expr = expr.cast(PiExpression.class).getCodomain()) {
      List<Concrete.TypeParameter<Position>> params = new ArrayList<>();
      visitDependentLink(expr.cast(PiExpression.class).getParameters(), params, false);
      if (!parameters.isEmpty() && parameters.get(parameters.size() - 1) instanceof Concrete.TelescopeParameter && !params.isEmpty() && params.get(0) instanceof Concrete.TelescopeParameter) {
        parameters.get(parameters.size() - 1).addAll(params);
      } else {
        parameters.add(params);
      }
    }

    Concrete.Expression<Position> result = expr.accept(this, null);
    for (int i = parameters.size() - 1; i >= 0; i--) {
      result = cPi(parameters.get(i), result);
    }
    return result;
  }

  private Integer getNum(Expression expr) {
    ConCallExpression conCall = expr.checkedCast(ConCallExpression.class);
    if (conCall == null) {
      return null;
    }
    if (conCall.getDefinition() == Prelude.ZERO) {
      return 0;
    }
    if (conCall.getDefinition() == Prelude.SUC) {
      Integer result = getNum(conCall.getDefCallArguments().get(0));
      if (result != null) {
        return result + 1;
      }
    }
    return null;
  }

  @Override
  public Concrete.Expression<Position> visitUniverse(UniverseExpression expr, Void params) {
    return visitSort(expr.getSort());
  }

  private Concrete.LevelExpression<Position> visitLevelNull(Level level) {
    return (level.getConstant() == 0 || level.getConstant() == -1) && level.getMaxConstant() == 0 && (level.getVar() == LevelVariable.PVAR || level.getVar() == LevelVariable.HVAR) ? null : visitLevel(level);
  }

  private Concrete.Expression<Position> visitSort(Sort sorts) {
    return cUniverse(visitLevelNull(sorts.getPLevel()), visitLevelNull(sorts.getHLevel()));
  }

  private Concrete.LevelExpression<Position> visitLevel(Level level) {
    if (level.isInfinity()) {
      return new Concrete.InfLevelExpression<>(null);
    }
    if (level.isClosed()) {
      return new Concrete.NumberLevelExpression<>(null, level.getConstant());
    }

    Concrete.LevelExpression<Position> result;
    if (level.getVar() == LevelVariable.PVAR) {
      result = new Concrete.PLevelExpression<>(null);
    } else if (level.getVar() == LevelVariable.HVAR) {
      result = new Concrete.HLevelExpression<>(null);
    } else if (level.getVar() instanceof InferenceLevelVariable) {
      result = new Concrete.InferVarLevelExpression<>(null, (InferenceLevelVariable) level.getVar());
    } else {
      throw new IllegalStateException();
    }

    if (level.getMaxConstant() != 0) {
      result = new Concrete.MaxLevelExpression<>(null, result, visitLevel(new Level(null, level.getMaxConstant())));
    }

    for (int i = 0; i < level.getConstant(); i++) {
      result = new Concrete.SucLevelExpression<>(null, result);
    }

    return result;
  }

  @Override
  public Concrete.Expression<Position> visitError(ErrorExpression expr, Void params) {
    return cGoal(expr.getError() instanceof GoalError ? ((GoalError) expr.getError()).name : "error", expr.getExpression() == null ? null : expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression<Position> visitTuple(TupleExpression expr, Void params) {
    List<Concrete.Expression<Position>> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return cTuple(fields);
  }

  @Override
  public Concrete.Expression<Position> visitSigma(SigmaExpression expr, Void params) {
    List<Concrete.TypeParameter<Position>> parameters = new ArrayList<>();
    visitDependentLink(expr.getParameters(), parameters, false);
    return cSigma(parameters);
  }

  @Override
  public Concrete.Expression<Position> visitProj(ProjExpression expr, Void params) {
    return cProj(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Concrete.Expression<Position> visitNew(NewExpression expr, Void params) {
    return cNew(expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression<Position> visitLet(LetExpression letExpression, Void params) {
    List<Concrete.LetClause<Position>> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      Concrete.Expression<Position> term = clause.getExpression().accept(this, null);
      Referable referable = makeLocalReference(clause, myFreeVariablesCollector.getFreeVariables(clause), false);
      clauses.add(clet(referable, Collections.emptyList(), null, term));
    }

    return cLet(clauses, letExpression.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression<Position> visitCase(CaseExpression expr, Void params) {
    List<Concrete.Expression<Position>> arguments = new ArrayList<>(expr.getArguments().size());
    for (Expression argument : expr.getArguments()) {
      arguments.add(argument.accept(this, null));
    }
    return cCase(arguments, expr.getElimTree() != null ? visitElimTree(expr.getElimTree()) : Collections.emptyList());
  }

  private List<Concrete.FunctionClause<Position>> visitElimTree(ElimTree elimTree) {
    List<Concrete.FunctionClause<Position>> clauses = new ArrayList<>();
    new Util.ElimTreeWalker((patterns, expr) -> clauses.add(cClause(visitPatterns(patterns, new Patterns(patterns).getFirstBinding()), expr.accept(this, null)))).walk(elimTree); // TODO: It seems that bindings in patterns and in the expression differ
    return clauses;
  }

  @Override
  public Concrete.Expression<Position> visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }
}
