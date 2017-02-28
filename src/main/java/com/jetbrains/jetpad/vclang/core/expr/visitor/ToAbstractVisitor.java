package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Name;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.factory.AbstractExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelMax;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.*;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Abstract.Expression> implements ElimTreeNodeVisitor<Void, Abstract.Expression> {
  public enum Flag { SHOW_CON_DATA_TYPE, SHOW_CON_PARAMS, SHOW_IMPLICIT_ARGS, SHOW_TYPES_IN_LAM, SHOW_PREFIX_PATH, SHOW_BIN_OP_IMPLICIT_ARGS }
  public static final EnumSet<Flag> DEFAULT = EnumSet.of(Flag.SHOW_IMPLICIT_ARGS);

  private final AbstractExpressionFactory myFactory;
  private final Map<Variable, String> myNames;
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
    for (String name : names) {
      myFreeNames.push(name);
    }
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

  private Abstract.Expression checkPath(DataCallExpression expr) {
    if (expr.getDefinition() != Prelude.PATH || myFlags.contains(Flag.SHOW_PREFIX_PATH)) {
      return null;
    }

    LamExpression expr1 = expr.getDefCallArguments().get(0).toLam();
    if (expr1 != null) {
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return myFactory.makeBinOp(expr.getDefCallArguments().get(1).accept(this, null), Prelude.PATH_INFIX.getAbstractDefinition(), expr.getDefCallArguments().get(2).accept(this, null));
      }
    }
    return null;
  }

  private Abstract.Expression checkBinOp(Expression expr) {
    Expression fun = expr.getFunction();
    DefCallExpression defCall = fun.toDefCall();

    if (defCall == null || new Name(defCall.getDefinition().getName()).fixity != Name.Fixity.INFIX) {
      return null;
    }
    boolean[] isExplicit = new boolean[defCall.getDefCallArguments().size() + expr.getArguments().size()];
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
    for (int j = 0; j < expr.getArguments().size(); j++) {
      if (isExplicit[defCall.getDefCallArguments().size() + j]) {
        if (i == 2) {
          return null;
        }
        visibleArgs[i++] = expr.getArguments().get(j);
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

    result = expr.getFunction().accept(this, null);

    boolean[] isExplicit = new boolean[expr.getArguments().size()];
    getArgumentsExplicitness(expr.getFunction(), isExplicit, 0);
    for (int index = 0; index < expr.getArguments().size(); index++) {
      result = visitApp(result, expr.getArguments().get(index), isExplicit[index]);
    }
    return result;
  }

  private void getArgumentsExplicitness(Expression expr, boolean[] isExplicit, int i) {
    List<DependentLink> params = new ArrayList<>(isExplicit.length - i);
    TypeMax type = expr.getType();
    if (type != null) {
      type.getPiParameters(params, true, false);
    }
    for (int j = 0; i < isExplicit.length; i++, j++) {
      isExplicit[i] = j < params.size() ? params.get(j).isExplicit() : true;
    }
  }

  private Abstract.Expression visitApp(Abstract.Expression function, Expression argument, boolean isExplicit) {
    Abstract.Expression arg = isExplicit || myFlags.contains(Flag.SHOW_IMPLICIT_ARGS) ? argument.accept(this, null) : null;
    return arg != null ? myFactory.makeApp(function, isExplicit, arg) : function;
  }

  private Abstract.Expression visitArguments(Abstract.Expression expr, DefCallExpression defCall) {
    DependentLink link = defCall.getDefinition().getParameters();
    for (Expression arg : defCall.getDefCallArguments()) {
      expr = myFactory.makeApp(expr, link.isExplicit(), arg.accept(this, null));
      link = link.getNext();
    }
    return expr;
  }

  @Override
  public Abstract.Expression visitDefCall(DefCallExpression expr, Void params) {
    Abstract.Expression result = checkBinOp(expr);
    if (result != null) {
      return result;
    }
    return visitArguments(myFactory.makeDefCall(null, expr.getDefinition().getAbstractDefinition()), expr);
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
      ExprSubstitution substitution = new ExprSubstitution();
      DependentLink link = expr.getDefinition().getDataTypeParameters();
      for (int i = 0; i < expr.getDataTypeArguments().size() && link.hasNext(); i++, link = link.getNext()) {
        substitution.add(link, expr.getDataTypeArguments().get(i));
      }
      conParams = expr.getDefinition().getDataTypeExpression(substitution, expr.getLevelArguments()).accept(this, null);
    }
    return visitArguments(myFactory.makeDefCall(conParams, expr.getDefinition().getAbstractDefinition()), expr);
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
    String name = myNames.get(var);
    if (name == null) {
      name = var.getName();
    }
    if (name == null) {
      name = "_";
    }
    return myFactory.makeVar((var instanceof InferenceVariable && !name.equals("_") ? "?" : "") + name);
  }

  @Override
  public Abstract.Expression visitReference(ReferenceExpression expr, Void params) {
    return visitBinding(expr.getBinding());
  }

  @Override
  public Abstract.Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : visitBinding(expr.getVariable());
  }

  private String renameVar(Binding var) {
    String name = var.getName();
    if (name == null || name.equals("_")) {
      return "_";
    }

    while (myFreeNames.contains(name)) {
      name = name + "'";
    }
    myFreeNames.push(name);
    myNames.put(var, name);
    return name;
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
  public Abstract.Expression visitLam(LamExpression expr, Void params) {
    List<Abstract.Argument> arguments = new ArrayList<>();
    if (myFlags.contains(Flag.SHOW_TYPES_IN_LAM)) {
      List<String> names = new ArrayList<>(3);
      for (DependentLink link = expr.getParameters(); link.hasNext(); link = link.getNext()) {
        DependentLink link1 = link.getNextTyped(null);
        for (; link != link1; link = link.getNext()) {
          names.add(renameVar(link));
        }
        names.add(renameVar(link));
        if (names.isEmpty()) {
          names.add(null);
        }
        arguments.add(myFactory.makeTelescopeArgument(link.isExplicit(), new ArrayList<>(names), link.getType().toExpression().accept(this, null)));
        names.clear();
      }
    } else {
      for (DependentLink link = expr.getParameters(); link.hasNext(); link = link.getNext()) {
        arguments.add(myFactory.makeNameArgument(link.isExplicit(), link.getName()));
      }
    }

    Abstract.Expression result = myFactory.makeLam(arguments, expr.getBody().accept(this, null));
    freeVars(expr.getParameters());
    return result;
  }

  public List<Abstract.TypeArgument> visitTypeArguments(DependentLink arguments) {
    List<Abstract.TypeArgument> args = new ArrayList<>();
    List<String> names = new ArrayList<>(3);
    for (DependentLink link = arguments; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      for (; link != link1; link = link.getNext()) {
        names.add(renameVar(link));
      }
      names.add(renameVar(link));
      if (names.isEmpty() || names.get(0).equals("_")) {
        args.add(myFactory.makeTypeArgument(link.isExplicit(), link.getType().toExpression().accept(this, null)));
      } else {
        args.add(myFactory.makeTelescopeArgument(link.isExplicit(), new ArrayList<>(names), link.getType().toExpression().accept(this, null)));
        names.clear();
      }
    }
    return args;
  }

  @Override
  public Abstract.Expression visitPi(PiExpression expr, Void params) {
    List<Abstract.TypeArgument> arguments = visitTypeArguments(expr.getParameters());
    Abstract.Expression result = myFactory.makePi(arguments, expr.getCodomain().accept(this, null));
    freeVars(expr.getParameters());
    return result;
  }

  private Integer getNum(Expression expr) {
    if (expr.toConCall() == null) {
      return null;
    }
    if (expr.toConCall().getDefinition() == Prelude.ZERO) {
      return 0;
    }
    if (expr.toConCall().getDefinition() == Prelude.SUC) {
      Integer result = getNum(expr.toConCall().getDefCallArguments().get(0));
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

  private Abstract.LevelExpression visitLevelNull(Level level, int add) {
    return level.getConstant() == 0 && (level.getVar() == LevelVariable.PVAR || level.getVar() == LevelVariable.HVAR) ? null : visitLevel(level, add);
  }

  public Abstract.Expression visitSort(Sort sorts) {
    return myFactory.makeUniverse(visitLevelNull(sorts.getPLevel(), 0), visitLevelNull(sorts.getHLevel(), -1));
  }

  public Abstract.Expression visitSortMax(SortMax sorts) {
    Level pLevel = sorts.getPLevel().toLevel();
    Level hLevel = sorts.getHLevel().toLevel();
    return myFactory.makeUniverse(
      pLevel != null ? visitLevelNull(pLevel, 0) : visitLevelMax(sorts.getPLevel(), 0),
      hLevel != null ? visitLevelNull(hLevel, -1) : visitLevelMax(sorts.getHLevel(), -1));
  }

  public Abstract.LevelExpression visitLevel(Level level, int add) {
    if (level.isInfinity()) {
      return myFactory.makeInf();
    }
    if (level.isClosed()) {
      return myFactory.makeNumberLevel(level.getConstant() + add);
    }

    Abstract.LevelExpression result;
    if (level.getVar() == LevelVariable.PVAR) {
      result = myFactory.makePLevel();
    } else
    if (level.getVar() == LevelVariable.HVAR) {
      result = myFactory.makeHLevel();
    } else
    if (level.getVar() instanceof InferenceLevelVariable) {
      result = myFactory.makeInferVarLevel((InferenceLevelVariable) level.getVar());
    } else {
      throw new IllegalStateException();
    }

    for (int i = 0; i < level.getConstant(); i++) {
      result = myFactory.makeSucLevel(result);
    }
    return result;
  }

  public Abstract.LevelExpression visitLevelMax(LevelMax levelMax, int add) {
    List<Level> levels = levelMax.toListOfLevels();
    if (levels.isEmpty()) {
      return myFactory.makeNumberLevel(add);
    }

    Abstract.LevelExpression result = visitLevel(levels.get(0), add);
    for (int i = 1; i < levels.size(); i++) {
      result = myFactory.makeMaxLevel(result, visitLevel(levels.get(i), add));
    }
    return result;
  }

  public Abstract.Expression visitTypeMax(TypeMax type) {
    if (type.toExpression() != null) {
      return type.toExpression().accept(this, null);
    }
    return myFactory.makePi(visitTypeArguments(type.getPiParameters()), visitSortMax(type.getPiCodomain().toSorts()));
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
    Abstract.Expression result = myFactory.makeSigma(visitTypeArguments(expr.getParameters()));
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

  private Abstract.Expression checkCase(LetExpression letExpression) {
    if (letExpression.getClauses().size() == 1 && Abstract.CaseExpression.FUNCTION_NAME.equals(letExpression.getClauses().get(0).getName()) && letExpression.getClauses().get(0).getElimTree() instanceof BranchElimTreeNode) {
      ReferenceExpression ref = letExpression.getExpression().getFunction().toReference();
      List<? extends Expression> args = letExpression.getExpression().getArguments();
      if (ref != null && ref.getBinding() == letExpression.getClauses().get(0)) {
        for (Expression arg : args) {
          if (arg.findBinding(letExpression.getClauses().get(0))) {
            return null;
          }
        }
        List<Abstract.Expression> caseArgs = new ArrayList<>(args.size());
        for (int i = args.size() - 1; i >= 0; i--) {
          caseArgs.add(args.get(i).accept(this, null));
        }
        return myFactory.makeCase(caseArgs, visitBranch((BranchElimTreeNode) letExpression.getClauses().get(0).getElimTree()));
      }
    }
    return null;
  }

  @Override
  public Abstract.Expression visitLet(LetExpression letExpression, Void params) {
    Abstract.Expression result = checkCase(letExpression);
    if (result != null) {
      return result;
    }

    List<Abstract.LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      List<Abstract.TypeArgument> arguments = visitTypeArguments(clause.getParameters());
      Abstract.Expression resultType = clause.getResultType() == null ? null : visitTypeMax(clause.getResultType());
      Abstract.Expression term = visitElimTree(clause.getElimTree(), clause.getParameters());
      freeVars(clause.getParameters());
      clauses.add(myFactory.makeLetClause(renameVar(clause), arguments, resultType, getTopLevelArrow(clause.getElimTree()), term));
    }

    result = myFactory.makeLet(clauses, letExpression.getExpression().accept(this, null));
    for (LetClause clause : letExpression.getClauses()) {
      myNames.remove(clause);
    }
    return result;
  }

  @Override
  public Abstract.Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  private List<Abstract.Clause> visitBranch(BranchElimTreeNode branchNode) {
    List<Abstract.Clause> clauses = new ArrayList<>(branchNode.getConstructorClauses().size());
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      List<Abstract.PatternArgument> args = new ArrayList<>();
      for (DependentLink link = clause.getConstructor().getParameters(); link.hasNext(); link = link.getNext()) {
        args.add(myFactory.makePatternArgument(myFactory.makeNamePattern(renameVar(link)), link.isExplicit()));
      }
      clauses.add(myFactory.makeClause(Collections.singletonList(myFactory.makeConPattern(clause.getConstructor().getName(), args)), clause.getChild().getArrow(), clause.getChild() == EmptyElimTreeNode.getInstance() ? null : clause.getChild().accept(this, null)));
      freeVars(clause.getConstructor().getParameters());
    }
    if (branchNode.getOtherwiseClause() != null) {
      clauses.add(myFactory.makeClause(Collections.singletonList(myFactory.makeNamePattern(null)), branchNode.getOtherwiseClause().getChild().getArrow(), branchNode.getOtherwiseClause().getChild() == EmptyElimTreeNode.getInstance() ? null : branchNode.getOtherwiseClause().getChild().accept(this, null)));
    }
    return clauses;
  }

  private Abstract.Expression visitElimTree(ElimTreeNode elimTree, DependentLink params) {
    if (elimTree == EmptyElimTreeNode.getInstance()) {
      if (params.hasNext() && params.getName().equals("\\this")) {
        params = params.getNext();
      }
      List<Abstract.Expression> exprs = new ArrayList<>();
      for (DependentLink link = params; link.hasNext(); link = link.getNext()) {
        exprs.add(visitBinding(link));
      }
      return myFactory.makeElim(exprs, Collections.<Abstract.Clause>emptyList());
    }
    return elimTree.accept(this, null);
  }

  @SuppressWarnings("SuspiciousNameCombination")
  private Abstract.Definition.Arrow getTopLevelArrow(ElimTreeNode elimTreeNode) {
    if (elimTreeNode == EmptyElimTreeNode.getInstance()) {
      return Abstract.Definition.Arrow.RIGHT;
    } else {
      return elimTreeNode.getArrow();
    }
  }

  @Override
  public Abstract.Expression visitBranch(BranchElimTreeNode branchNode, Void params) {
    return myFactory.makeElim(Collections.singletonList(visitBinding(branchNode.getReference())), visitBranch(branchNode));
  }

  @Override
  public Abstract.Expression visitLeaf(LeafElimTreeNode leafNode, Void params) {
    return leafNode.getExpression() != null ? leafNode.getExpression().accept(this, null) : myFactory.makeError(null);
  }

  @Override
  public Abstract.Expression visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    throw new IllegalStateException();
  }
}
