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
import com.jetbrains.jetpad.vclang.frontend.reference.ParsedLocalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.NamedUnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.GoalError;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.Util;

import java.math.BigInteger;
import java.util.*;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Concrete.Expression> {
  public enum Flag { SHOW_CON_PARAMS, SHOW_FIELD_INSTANCE, SHOW_CLASS_INSTANCE, SHOW_IMPLICIT_ARGS, SHOW_TYPES_IN_LAM, SHOW_PREFIX_PATH, SHOW_BIN_OP_IMPLICIT_ARGS }

  private final EnumSet<Flag> myFlags;
  private final CollectFreeVariablesVisitor myFreeVariablesCollector;
  private final Map<Binding, Referable> myNames;

  private static final String unnamed = "unnamed";

  private ToAbstractVisitor(EnumSet<Flag> flags, CollectFreeVariablesVisitor collector, Map<Binding, Referable> names) {
    myFlags = flags;
    myFreeVariablesCollector = collector;
    myNames = names;
  }

  public static Concrete.Expression convert(Expression expression, EnumSet<Flag> flags) {
    CollectFreeVariablesVisitor collector = new CollectFreeVariablesVisitor();
    Set<Variable> variables = new HashSet<>();
    expression.accept(collector, variables);
    Map<Binding, Referable> names = new HashMap<>();
    ToAbstractVisitor visitor = new ToAbstractVisitor(flags, collector, names);
    for (Variable variable : variables) {
      if (variable instanceof Binding) {
        names.put((Binding) variable, ref(visitor.getFreshName((Binding) variable, variables)));
      }
    }
    return expression.accept(visitor, null);
  }

  public static Concrete.LevelExpression convert(Level level) {
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
            otherName = referable.textRepresentation();
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

  private Concrete.Pattern visitPattern(Pattern pattern, boolean isExplicit) {
    if (pattern instanceof BindingPattern) {
      return cNamePattern(isExplicit, ref(((BindingPattern) pattern).getBinding().getName()));
    }
    if (pattern instanceof EmptyPattern) {
      return cEmptyPattern(isExplicit);
    }
    if (pattern instanceof ConstructorPattern) {
      return cConPattern(isExplicit, ((ConstructorPattern) pattern).getConstructor().getReferable(), visitPatterns(((ConstructorPattern) pattern).getArguments(), ((ConstructorPattern) pattern).getConstructor().getParameters()));
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
    if (expr.getDefinition() != Prelude.PATH || myFlags.contains(Flag.SHOW_PREFIX_PATH)) {
      return null;
    }

    LamExpression expr1 = expr.getDefCallArguments().get(0).checkedCast(LamExpression.class);
    if (expr1 != null) {
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return cBinOp(expr.getDefCallArguments().get(1).accept(this, null), Prelude.PATH_INFIX.getReferable(), expr.getDefCallArguments().get(2).accept(this, null));
      }
    }
    return null;
  }

  @Override
  public Concrete.Expression visitApp(AppExpression expr, Void params) {
    List<Expression> args = new ArrayList<>();
    Expression fun = expr;
    while (fun.isInstance(AppExpression.class)) {
      args.add(fun.cast(AppExpression.class).getArgument());
      fun = fun.cast(AppExpression.class).getFunction();
    }
    Collections.reverse(args);

    Concrete.Expression result = fun.accept(this, null);
    boolean[] isExplicit = new boolean[args.size()];
    getArgumentsExplicitness(expr.getFunction(), isExplicit);
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
    Concrete.Expression arg = isExplicit || myFlags.contains(Flag.SHOW_IMPLICIT_ARGS) ? argument.accept(this, null) : null;
    return arg != null ? cApps(function, arg, isExplicit) : function;
  }

  private Concrete.Expression visitParameters(Concrete.Expression expr, DependentLink parameters, List<? extends Expression> arguments) {
    for (Expression arg : arguments) {
      expr = cApps(expr, arg.accept(this, null), parameters.isExplicit());
      parameters = parameters.getNext();
    }
    return expr;
  }

  private static Concrete.Expression makeReference(Referable referable) {
    return cVar(referable == null ? new NamedUnresolvedReference(null, "\\this") : referable);
  }

  @Override
  public Concrete.Expression visitDefCall(DefCallExpression expr, Void params) {
    return visitParameters(makeReference(expr.getDefinition().getReferable()), expr.getDefinition().getParameters(), expr.getDefCallArguments());
  }

  @Override
  public Concrete.Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Concrete.Expression result = makeReference(expr.getDefinition().getReferable());
    if (myFlags.contains(Flag.SHOW_FIELD_INSTANCE)) {
      result = new Concrete.AppExpression(null, result, new Concrete.Argument(expr.getExpression().accept(this, null), false));
    }
    return result;
  }

  @Override
  public Concrete.Expression visitConCall(ConCallExpression expr, Void params) {
    BigInteger num = getNum(expr);
    if (num != null) {
      return cNum(num);
    }

    Concrete.Expression result = makeReference(expr.getDefinition().getReferable());
    if (expr.getDefinition().status().headerIsOK() && myFlags.contains(Flag.SHOW_CON_PARAMS)) {
      for (Expression arg : expr.getDataTypeArguments()) {
        result = new Concrete.AppExpression(null, result, new Concrete.Argument(arg.accept(this, null), false));
      }
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
    Collection<Map.Entry<ClassField, Expression>> implHere = expr.getImplementedHere().entrySet();
    Concrete.Expression enclExpr = null;
    List<Concrete.ClassFieldImpl> statements = new ArrayList<>(implHere.size());
    for (Map.Entry<ClassField, Expression> entry : implHere) {
      if (entry.getKey().equals(expr.getDefinition().getEnclosingThisField())) {
        enclExpr = entry.getValue().accept(this, params);
      } else {
        statements.add(cImplStatement(entry.getKey().getReferable(), entry.getValue().accept(this, params)));
      }
    }

    Concrete.Expression defCallExpr = makeReference(expr.getDefinition().getReferable());
    if (myFlags.contains(Flag.SHOW_CLASS_INSTANCE)) {
      defCallExpr = new Concrete.AppExpression(null, defCallExpr, new Concrete.Argument(enclExpr, false));
    }
    if (statements.isEmpty()) {
      return defCallExpr;
    } else {
      return cClassExt(defCallExpr, statements);
    }
  }

  @Override
  public Concrete.Expression visitReference(ReferenceExpression expr, Void params) {
    return makeReference(myNames.get(expr.getBinding()));
  }

  @Override
  public Concrete.Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : new Concrete.InferenceReferenceExpression(null, expr.getVariable());
  }

  private ParsedLocalReferable makeLocalReference(Binding var, Set<Variable> freeVars) {
    if (!freeVars.contains(var)) {
      return null;
    }
    ParsedLocalReferable reference = ref(getFreshName(var, freeVars));
    myNames.put(var, reference);
    return reference;
  }

  @Override
  public Concrete.Expression visitLam(LamExpression lamExpr, Void ignore) {
    List<Concrete.Parameter> parameters = new ArrayList<>();
    Expression expr = lamExpr;
    for (; expr.isInstance(LamExpression.class); expr = expr.cast(LamExpression.class).getBody()) {
      if (myFlags.contains(Flag.SHOW_TYPES_IN_LAM)) {
        visitDependentLink(expr.cast(LamExpression.class).getParameters(), parameters, true);
      } else {
        SingleDependentLink params = expr.cast(LamExpression.class).getParameters();
        Set<Variable> freeVars = myFreeVariablesCollector.getFreeVariables(params.getNextTyped(null));
        for (SingleDependentLink link = params; link.hasNext(); link = link.getNext()) {
          parameters.add(cName(link.isExplicit(), makeLocalReference(link, freeVars)));
        }
      }
    }

    return cLam(parameters, expr.accept(this, null));
  }

  private void visitDependentLink(DependentLink parameters, List<? super Concrete.TypeParameter> args, boolean isNamed) {
    List<Referable> referableList = new ArrayList<>(3);
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      Set<Variable> freeVars = myFreeVariablesCollector.getFreeVariables(link1);
      for (; link != link1; link = link.getNext()) {
        referableList.add(makeLocalReference(link, freeVars));
      }

      Referable referable = makeLocalReference(link, freeVars);
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
    for (; expr.isInstance(PiExpression.class); expr = expr.cast(PiExpression.class).getCodomain()) {
      List<Concrete.TypeParameter> params = new ArrayList<>();
      visitDependentLink(expr.cast(PiExpression.class).getParameters(), params, false);
      if (!parameters.isEmpty() && parameters.get(parameters.size() - 1) instanceof Concrete.TelescopeParameter && !params.isEmpty() && params.get(0) instanceof Concrete.TelescopeParameter) {
        parameters.get(parameters.size() - 1).addAll(params);
      } else {
        parameters.add(params);
      }
    }

    Concrete.Expression result = expr.accept(this, null);
    for (int i = parameters.size() - 1; i >= 0; i--) {
      result = cPi(parameters.get(i), result);
    }
    return result;
  }

  private BigInteger getNum(Expression expr) {
    ConCallExpression conCall = expr.checkedCast(ConCallExpression.class);
    if (conCall == null) {
      return null;
    }
    if (conCall.getDefinition() == Prelude.ZERO) {
      return BigInteger.ZERO;
    }
    if (conCall.getDefinition() == Prelude.SUC) {
      BigInteger result = getNum(conCall.getDefCallArguments().get(0));
      if (result != null) {
        return result.add(BigInteger.ONE);
      }
    }
    return null;
  }

  @Override
  public Concrete.Expression visitUniverse(UniverseExpression expr, Void params) {
    return visitSort(expr.getSort());
  }

  private Concrete.LevelExpression visitLevelNull(Level level) {
    return (level.getConstant() == 0 || level.getConstant() == -1) && level.getMaxConstant() == 0 && (level.getVar() == LevelVariable.PVAR || level.getVar() == LevelVariable.HVAR) ? null : visitLevel(level);
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
      result = new Concrete.InferVarLevelExpression(null, (InferenceLevelVariable) level.getVar());
    } else {
      throw new IllegalStateException();
    }

    if (level.getMaxConstant() != 0) {
      result = new Concrete.MaxLevelExpression(null, result, visitLevel(new Level(null, level.getMaxConstant())));
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
    return cNew(expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitLet(LetExpression letExpression, Void params) {
    List<Concrete.LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      Concrete.Expression term = clause.getExpression().accept(this, null);
      Referable referable = makeLocalReference(clause, myFreeVariablesCollector.getFreeVariables(clause));
      if (referable != null) {
        clauses.add(clet(referable, Collections.emptyList(), null, term));
      }
    }

    Concrete.Expression expr = letExpression.getExpression().accept(this, null);
    return clauses.isEmpty() ? expr : cLet(clauses, expr);
  }

  @Override
  public Concrete.Expression visitCase(CaseExpression expr, Void params) {
    List<Concrete.Expression> arguments = new ArrayList<>(expr.getArguments().size());
    for (Expression argument : expr.getArguments()) {
      arguments.add(argument.accept(this, null));
    }
    return cCase(arguments, expr.getElimTree() != null ? visitElimTree(expr.getElimTree()) : Collections.emptyList());
  }

  private List<Concrete.FunctionClause> visitElimTree(ElimTree elimTree) {
    List<Concrete.FunctionClause> clauses = new ArrayList<>();
    new Util.ElimTreeWalker((patterns, expr) -> clauses.add(cClause(visitPatterns(patterns, new Patterns(patterns).getFirstBinding()), expr.accept(this, null)))).walk(elimTree); // TODO: It seems that bindings in patterns and in the expression differ
    return clauses;
  }

  @Override
  public Concrete.Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }
}
