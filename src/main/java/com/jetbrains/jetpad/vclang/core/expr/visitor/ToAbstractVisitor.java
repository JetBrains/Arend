package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Name;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.factory.AbstractExpressionFactory;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.Util;

import java.util.*;
import java.util.function.Function;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Abstract.Expression> {
  public enum Flag {SHOW_CON_DATA_TYPE, SHOW_CON_PARAMS, SHOW_IMPLICIT_ARGS, SHOW_TYPES_IN_LAM, SHOW_PREFIX_PATH, SHOW_BIN_OP_IMPLICIT_ARGS}

  public static final EnumSet<Flag> DEFAULT = EnumSet.of(Flag.SHOW_IMPLICIT_ARGS);

  private final AbstractExpressionFactory myFactory;
  private final Map<Variable, Abstract.ReferableSourceNode> myNames;
  private final Stack<String> myFreeNames;
  private EnumSet<Flag> myFlags;

  public ToAbstractVisitor(AbstractExpressionFactory factory) {
    myFactory = factory;
    myFlags = DEFAULT;
    myNames = new HashMap<>();
    myFreeNames = new Stack<>();
  }

  public ToAbstractVisitor(AbstractExpressionFactory factory, Collection<String> names) {
    myFactory = factory;
    myFlags = DEFAULT;
    myNames = new HashMap<>();
    myFreeNames = new Stack<>();
    names.forEach(myFreeNames::push);
  }

  public void setFlags(EnumSet<Flag> flags) {
    myFlags = flags;
  }

  public ToAbstractVisitor addFlags(Flag flag) {
    if (myFlags == DEFAULT) {
      myFlags = DEFAULT.clone();
    }
    myFlags.add(flag);
    return this;
  }

  public Abstract.Pattern visitPattern(Pattern pattern, boolean isExplicit) {
    if (pattern instanceof BindingPattern) {
      return myFactory.makeNamePattern(isExplicit, ((BindingPattern) pattern).getBinding().getName());
    }
    if (pattern instanceof EmptyPattern) {
      return myFactory.makeEmptyPattern(isExplicit);
    }
    if (pattern instanceof ConstructorPattern) {
      return myFactory.makeConPattern(isExplicit, ((ConstructorPattern) pattern).getConstructor().getAbstractDefinition(), visitPatterns(((ConstructorPattern) pattern).getArguments(), ((ConstructorPattern) pattern).getConstructor().getParameters()));
    }
    throw new IllegalStateException();
  }

  private List<Abstract.Pattern> visitPatterns(List<Pattern> patterns, DependentLink parameters) {
    List<Abstract.Pattern> result = new ArrayList<>(patterns.size());
    for (Pattern pattern : patterns) {
      result.add(visitPattern(pattern, !parameters.hasNext() || parameters.isExplicit()));
      if (parameters.hasNext()) {
        parameters = parameters.getNext();
      }
    }
    return result;
  }

  private Abstract.Expression checkPath(DataCallExpression expr) {
    if (expr.getDefinition() != Prelude.PATH || myFlags.contains(Flag.SHOW_PREFIX_PATH)) {
      return null;
    }

    LamExpression expr1 = expr.getDefCallArguments().get(0).checkedCast(LamExpression.class);
    if (expr1 != null) {
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return myFactory.makeBinOp(expr.getDefCallArguments().get(1).accept(this, null), Prelude.PATH_INFIX.getAbstractDefinition(), expr.getDefCallArguments().get(2).accept(this, null));
      }
    }
    return null;
  }

  private Abstract.Expression checkBinOp(Expression expr) {
    List<Expression> args = new ArrayList<>(2);
    Expression fun = expr;
    while (fun.isInstance(AppExpression.class)) {
      args.add(fun.cast(AppExpression.class).getArgument());
      fun = fun.cast(AppExpression.class).getFunction();
    }
    Collections.reverse(args);
    DefCallExpression defCall = fun.checkedCast(DefCallExpression.class);

    if (defCall == null || new Name(defCall.getDefinition().getName()).fixity != Name.Fixity.INFIX) {
      return null;
    }
    boolean[] isExplicit = new boolean[defCall.getDefCallArguments().size() + args.size()];
    int i = 0;
    for (DependentLink link = defCall.getDefinition().getParameters(); link.hasNext(); link = link.getNext()) {
      isExplicit[i++] = link.isExplicit();
    }
    getArgumentsExplicitness(defCall, isExplicit, i);
    if (isExplicit.length < 2 || myFlags.contains(Flag.SHOW_BIN_OP_IMPLICIT_ARGS) && (!isExplicit[0] || !isExplicit[1])) {
      return null;
    }

    Expression[] visibleArgs = new Expression[2];
    i = 0;
    for (int j = 0; j < defCall.getDefCallArguments().size(); j++) {
      if (isExplicit[j]) {
        if (i == 2) {
          return null;
        }
        visibleArgs[i++] = defCall.getDefCallArguments().get(j);
      }
    }
    for (int j = 0; j < args.size(); j++) {
      if (isExplicit[defCall.getDefCallArguments().size() + j]) {
        if (i == 2) {
          return null;
        }
        visibleArgs[i++] = args.get(j);
      }
    }
    return i == 2 ? myFactory.makeBinOp(visibleArgs[0].accept(this, null), defCall.getDefinition().getAbstractDefinition(), visibleArgs[1].accept(this, null)) : null;
  }

  @Override
  public Abstract.Expression visitApp(AppExpression expr, Void params) {
    Abstract.Expression result = checkBinOp(expr);
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

  private void getArgumentsExplicitness(Expression expr, boolean[] isExplicit, int i) {
    List<SingleDependentLink> params = new ArrayList<>(isExplicit.length - i);
    expr.getType().getPiParameters(params, false);
    for (int j = 0; i < isExplicit.length; i++, j++) {
      isExplicit[i] = j >= params.size() || params.get(j).isExplicit();
    }
  }

  private Abstract.Expression visitApp(Abstract.Expression function, Expression argument, boolean isExplicit) {
    Abstract.Expression arg = isExplicit || myFlags.contains(Flag.SHOW_IMPLICIT_ARGS) ? argument.accept(this, null) : null;
    return arg != null ? myFactory.makeApp(function, isExplicit, arg) : function;
  }

  private Abstract.Expression visitParameters(Abstract.Expression expr, DependentLink parameters, List<? extends Expression> arguments) {
    for (Expression arg : arguments) {
      expr = myFactory.makeApp(expr, parameters.isExplicit(), arg.accept(this, null));
      parameters = parameters.getNext();
    }
    return expr;
  }

  @Override
  public Abstract.Expression visitDefCall(DefCallExpression expr, Void params) {
    Abstract.Expression result = checkBinOp(expr);
    if (result != null) {
      return result;
    }
    return visitParameters(myFactory.makeDefCall(null, expr.getDefinition().getAbstractDefinition()), expr.getDefinition().getParameters(), expr.getDefCallArguments());
  }

  @Override
  public Abstract.Expression visitFieldCall(FieldCallExpression expr, Void params) {
    return myFactory.makeDefCall(expr.getExpression().accept(this, null), expr.getDefinition().getAbstractDefinition());
  }

  @Override
  public Abstract.Expression visitConCall(ConCallExpression expr, Void params) {
    Integer num = getNum(expr);
    if (num != null) {
      return myFactory.makeNumericalLiteral(num);
    }

    Abstract.Expression conParams = null;
    if (expr.getDefinition().status().headerIsOK() && myFlags.contains(Flag.SHOW_CON_PARAMS) && (!expr.getDataTypeArguments().isEmpty() || myFlags.contains(Flag.SHOW_CON_DATA_TYPE))) {
      conParams = expr.getDataTypeExpression().accept(this, null);
    }
    return visitParameters(myFactory.makeDefCall(conParams, expr.getDefinition().getAbstractDefinition()), expr.getDefinition().getParameters(), expr.getDefCallArguments());
  }

  @Override
  public Abstract.Expression visitDataCall(DataCallExpression expr, Void params) {
    Abstract.Expression result = checkPath(expr);
    return result != null ? result : visitDefCall(expr, params);
  }

  @Override
  public Abstract.Expression visitClassCall(ClassCallExpression expr, Void params) {
    Collection<Map.Entry<ClassField, FieldSet.Implementation>> implHere = expr.getImplementedHere();
    Abstract.Expression enclExpr = null;
    List<Abstract.ClassFieldImpl> statements = new ArrayList<>(implHere.size());
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : implHere) {
      if (entry.getKey().equals(expr.getDefinition().getEnclosingThisField())) {
        enclExpr = entry.getValue().term.accept(this, params);
      } else {
        statements.add(myFactory.makeImplementStatement(entry.getKey(), null, entry.getValue().term.accept(this, params)));
      }
    }

    Abstract.Expression defCallExpr = myFactory.makeDefCall(enclExpr, expr.getDefinition().getAbstractDefinition());
    if (statements.isEmpty()) {
      return defCallExpr;
    } else {
      return myFactory.makeClassExt(defCallExpr, statements);
    }
  }

  private Abstract.Expression visitBinding(Binding var) {
    Abstract.ReferableSourceNode referable = myNames.get(var);
    return myFactory.makeVar(referable != null ? referable : myFactory.makeReferable(var.getName() == null ? "_" : var.getName()));
  }

  @Override
  public Abstract.Expression visitReference(ReferenceExpression expr, Void params) {
    return visitBinding(expr.getBinding());
  }

  @Override
  public Abstract.Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : myFactory.makeInferVar(expr.getVariable());
  }

  private <T extends Abstract.ReferableSourceNode> T makeReferable(Binding var, Function<String, T> fun) {
    String name = var.getName();
    if (name == null || name.equals("_")) {
      return null;
    }

    while (myFreeNames.contains(name)) {
      name = name + "'";
    }
    myFreeNames.push(name);
    T referable = fun.apply(name);
    myNames.put(var, referable);
    return referable;
  }

  private Abstract.ReferableSourceNode makeReferable(Binding var) {
    return makeReferable(var, myFactory::makeReferable);
  }

  private void freeVars(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      if (link.getName() != null && !link.getName().equals("_")) {
        myFreeNames.pop();
        myNames.remove(link);
      }
    }
  }

  @Override
  public Abstract.Expression visitLam(LamExpression lamExpr, Void params) {
    List<Abstract.Parameter> parameters = new ArrayList<>();
    Expression expr = lamExpr;
    for (; expr.isInstance(LamExpression.class); expr = expr.cast(LamExpression.class).getBody()) {
      if (myFlags.contains(Flag.SHOW_TYPES_IN_LAM)) {
        visitDependentLink(expr.cast(LamExpression.class).getParameters(), parameters, true);
      } else {
        for (DependentLink link = expr.cast(LamExpression.class).getParameters(); link.hasNext(); link = link.getNext()) {
          final DependentLink finalLink = link;
          parameters.add(makeReferable(link, name -> myFactory.makeNameParameter(finalLink.isExplicit(), name)));
        }
      }
    }

    Abstract.Expression result = myFactory.makeLam(parameters, expr.accept(this, null));
    for (expr = lamExpr; expr.isInstance(LamExpression.class); expr = expr.cast(LamExpression.class).getBody()) {
      freeVars(expr.cast(LamExpression.class).getParameters());
    }
    return result;
  }

  private void visitDependentLink(DependentLink parameters, List<? super Abstract.TypeParameter> args, boolean isNamed) {
    List<Abstract.ReferableSourceNode> referableList = new ArrayList<>(3);
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      if (!isNamed && link1 == link && link.getName() == null) {
        args.add(myFactory.makeTypeParameter(link.isExplicit(), link.getTypeExpr().accept(this, null)));
      } else {
        for (; link != link1; link = link.getNext()) {
          referableList.add(makeReferable(link));
        }
        referableList.add(makeReferable(link));
        args.add(myFactory.makeTelescopeParameter(link.isExplicit(), new ArrayList<>(referableList), link.getTypeExpr().accept(this, null)));
        referableList.clear();
      }
    }
  }

  @Override
  public Abstract.Expression visitPi(PiExpression piExpr, Void params) {
    List<List<Abstract.TypeParameter>> arguments = new ArrayList<>();
    Expression expr = piExpr;
    for (; expr.isInstance(PiExpression.class); expr = expr.cast(PiExpression.class).getCodomain()) {
      List<Abstract.TypeParameter> args = new ArrayList<>();
      visitDependentLink(expr.cast(PiExpression.class).getParameters(), args, false);
      if (!arguments.isEmpty() && arguments.get(arguments.size() - 1) instanceof Abstract.TelescopeParameter && !args.isEmpty() && args.get(0) instanceof Abstract.TelescopeParameter) {
        arguments.get(arguments.size() - 1).addAll(args);
      } else {
        arguments.add(args);
      }
    }

    Abstract.Expression result = expr.accept(this, null);
    for (int i = arguments.size() - 1; i >= 0; i--) {
      result = myFactory.makePi(arguments.get(i), result);
    }
    for (expr = piExpr; expr.isInstance(PiExpression.class); expr = expr.cast(PiExpression.class).getCodomain()) {
      freeVars(expr.cast(PiExpression.class).getParameters());
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
  public Abstract.Expression visitUniverse(UniverseExpression expr, Void params) {
    return visitSort(expr.getSort());
  }

  private Abstract.LevelExpression visitLevelNull(Level level) {
    return (level.getConstant() == 0 || level.getConstant() == -1) && level.getMaxConstant() == 0 && (level.getVar() == LevelVariable.PVAR || level.getVar() == LevelVariable.HVAR) ? null : visitLevel(level);
  }

  public Abstract.Expression visitSort(Sort sorts) {
    return myFactory.makeUniverse(visitLevelNull(sorts.getPLevel()), visitLevelNull(sorts.getHLevel()));
  }

  public Abstract.LevelExpression visitLevel(Level level) {
    if (level.isInfinity()) {
      return myFactory.makeInf();
    }
    if (level.isClosed()) {
      return myFactory.makeNumberLevel(level.getConstant());
    }

    Abstract.LevelExpression result;
    if (level.getVar() == LevelVariable.PVAR) {
      result = myFactory.makePLevel();
    } else if (level.getVar() == LevelVariable.HVAR) {
      result = myFactory.makeHLevel();
    } else if (level.getVar() instanceof InferenceLevelVariable) {
      result = myFactory.makeInferVarLevel((InferenceLevelVariable) level.getVar());
    } else {
      throw new IllegalStateException();
    }

    if (level.getMaxConstant() != 0) {
      result = myFactory.makeMaxLevel(result, visitLevel(new Level(null, level.getMaxConstant())));
    }

    for (int i = 0; i < level.getConstant(); i++) {
      result = myFactory.makeSucLevel(result);
    }

    return result;
  }

  @Override
  public Abstract.Expression visitError(ErrorExpression expr, Void params) {
    return myFactory.makeError(expr.getExpr() == null ? null : expr.getExpr().accept(this, null));
  }

  @Override
  public Abstract.Expression visitTuple(TupleExpression expr, Void params) {
    List<Abstract.Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return myFactory.makeTuple(fields);
  }

  @Override
  public Abstract.Expression visitSigma(SigmaExpression expr, Void params) {
    List<Abstract.TypeParameter> args = new ArrayList<>();
    visitDependentLink(expr.getParameters(), args, false);
    Abstract.Expression result = myFactory.makeSigma(args);
    freeVars(expr.getParameters());
    return result;
  }

  @Override
  public Abstract.Expression visitProj(ProjExpression expr, Void params) {
    return myFactory.makeProj(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Abstract.Expression visitNew(NewExpression expr, Void params) {
    return myFactory.makeNew(expr.getExpression().accept(this, null));
  }

  @Override
  public Abstract.Expression visitLet(LetExpression letExpression, Void params) {
    List<Abstract.LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      Abstract.Expression term = clause.getExpression().accept(this, null);
      clauses.add(myFactory.makeLetClause(makeReferable(clause), Collections.emptyList(), null, term));
    }

    Abstract.Expression result = myFactory.makeLet(clauses, letExpression.getExpression().accept(this, null));
    letExpression.getClauses().forEach(myNames::remove);
    return result;
  }

  @Override
  public Abstract.Expression visitCase(CaseExpression expr, Void params) {
    List<Abstract.Expression> arguments = new ArrayList<>(expr.getArguments().size());
    for (Expression argument : expr.getArguments()) {
      arguments.add(argument.accept(this, null));
    }
    return myFactory.makeCase(arguments, expr.getElimTree() != null ? visitElimTree(expr.getElimTree()) : Collections.emptyList());
  }

  private List<Abstract.FunctionClause> visitElimTree(ElimTree elimTree) {
    List<Abstract.FunctionClause> clauses = new ArrayList<>();
    new Util.ElimTreeWalker((patterns, expr) -> clauses.add(myFactory.makeClause(visitPatterns(patterns, new Patterns(patterns).getFirstBinding()), expr.accept(this, null)))).walk(elimTree);
    return clauses;
  }

  @Override
  public Abstract.Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }
}
