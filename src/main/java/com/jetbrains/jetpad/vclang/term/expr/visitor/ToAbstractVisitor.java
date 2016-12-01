package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.factory.AbstractExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.LevelMax;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.*;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Abstract.Expression> implements ElimTreeNodeVisitor<Void, Abstract.Expression> {
  public enum Flag { SHOW_CON_DATA_TYPE, SHOW_CON_PARAMS, SHOW_IMPLICIT_ARGS, SHOW_LEVEL_ARGS, SHOW_TYPES_IN_LAM, SHOW_PREFIX_PATH, SHOW_BIN_OP_IMPLICIT_ARGS }
  public static final EnumSet<Flag> DEFAULT = EnumSet.of(Flag.SHOW_IMPLICIT_ARGS, Flag.SHOW_CON_PARAMS);

  private final AbstractExpressionFactory myFactory;
  private final Map<String, String> myNames;
  private EnumSet<Flag> myFlags;

  public ToAbstractVisitor(AbstractExpressionFactory factory) {
    myFactory = factory;
    myFlags = DEFAULT;
    myNames = new HashMap<>();
  }

  public ToAbstractVisitor(AbstractExpressionFactory factory, List<String> names) {
    myFactory = factory;
    myFlags = DEFAULT;

    myNames = new HashMap<>();
    for (String name : names) {
      myNames.put(name, name);
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

  private Abstract.Expression checkPath(AppExpression expr) {
    Expression fun = expr.getFunction();
    List<? extends Expression> args = expr.getArguments();

    DataCallExpression dataCall = fun.toDataCall();
    if (!(args.size() == 3 && dataCall != null && dataCall.getDefinition() == Prelude.PATH)) {
      return null;
    }
    LamExpression expr1 = args.get(0).toLam();
    if (expr1 != null) {
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return myFactory.makeBinOp(args.get(1).accept(this, null), Prelude.PATH_INFIX.getAbstractDefinition(), args.get(2).accept(this, null));
      }
    }
    return null;
  }

  private Abstract.Expression checkBinOp(AppExpression expr) {
    Expression fun = expr.getFunction();
    DefCallExpression defCall = fun.toDefCall();

    if (!(defCall != null && new Name(defCall.getDefinition().getName()).fixity == Name.Fixity.INFIX)) {
      return null;
    }
    boolean[] isExplicit = new boolean[expr.getArguments().size()];
    getArgumentsExplicitness(defCall, isExplicit);
    if (isExplicit.length < 2 || myFlags.contains(Flag.SHOW_BIN_OP_IMPLICIT_ARGS) && (!isExplicit[0] || !isExplicit[1])) {
      return null;
    }

    Expression[] visibleArgs = new Expression[2];
    int i = 0;
    for (int j = 0; j < expr.getArguments().size(); j++) {
      if (isExplicit[j]) {
        if (i == 2) {
          return null;
        }
        visibleArgs[i++] = expr.getArguments().get(j);
      }
    }
    return i < 0 ? myFactory.makeBinOp(visibleArgs[0].accept(this, null), defCall.getDefinition().getAbstractDefinition(), visibleArgs[1].accept(this, null)) : null;
  }

  @Override
  public Abstract.Expression visitApp(AppExpression expr, Void params) {
    Integer num = getNum(expr);
    if (num != null) {
      return myFactory.makeNumericalLiteral(num);
    }

    if (!myFlags.contains(Flag.SHOW_PREFIX_PATH)) {
      Abstract.Expression result = checkPath(expr);
      if (result != null) {
        return result;
      }
    }

    Abstract.Expression result = checkBinOp(expr);
    if (result != null) {
      return result;
    }

    result = expr.getFunction().accept(this, null);

    boolean[] isExplicit = new boolean[expr.getArguments().size()];
    getArgumentsExplicitness(expr.getFunction(), isExplicit);
    for (int index = 0; index < expr.getArguments().size(); index++) {
      result = visitApp(result, expr.getArguments().get(index), isExplicit[index]);
    }
    return result;
  }

  private void getArgumentsExplicitness(Expression expr, boolean[] isExplicit) {
    List<DependentLink> params = new ArrayList<>(isExplicit.length);
    expr.getType().getPiParameters(params, true, false);
    for (int i = 0; i < isExplicit.length; i++) {
      isExplicit[i] = i < params.size() ? params.get(i).isExplicit() : true;
    }
  }

  private Abstract.Expression visitApp(Abstract.Expression function, Expression argument, boolean isExplicit) {
    Abstract.Expression arg = isExplicit || myFlags.contains(Flag.SHOW_IMPLICIT_ARGS) ? argument.accept(this, null) : null;
    return arg != null ? myFactory.makeApp(function, isExplicit, arg) : function;
  }

  private Abstract.Expression visitArguments(Abstract.Expression expr, DefCallExpression defCall, boolean withLevelArgs) {
    if (withLevelArgs && myFlags.contains(Flag.SHOW_LEVEL_ARGS)) {
      List<Integer> userPolyParams = Binding.Helper.getSublistOfUserBindings(defCall.getDefinition().getPolyParams());
      for (Integer paramInd : userPolyParams) {
        expr = myFactory.makeApp(expr, false, visitLevel(defCall.getPolyArguments().getLevels().get(paramInd), 0));
      }
    }
    DependentLink link = defCall.getDefinition().getParameters();
    for (Expression arg : defCall.getDefCallArguments()) {
      expr = myFactory.makeApp(expr, link.isExplicit(), arg.accept(this, null));
      link = link.getNext();
    }
    return expr;
  }

  @Override
  public Abstract.Expression visitDefCall(DefCallExpression expr, Void params) {
    return visitArguments(myFactory.makeDefCall(null, expr.getDefinition().getAbstractDefinition(), expr.getDefinition().getAbstractDefinition()), expr, true);
  }

  @Override
  public Abstract.Expression visitFieldCall(FieldCallExpression expr, Void params) {
    TypeMax type = expr.getExpression().getType();
    Abstract.Definition definition = expr.getDefinition().getAbstractDefinition();
    if (type instanceof Expression) {
      ClassCallExpression classCall = ((Expression) type).normalize(NormalizeVisitor.Mode.WHNF).toClassCall();
      if (classCall instanceof ClassViewCallExpression) {
        for (Abstract.ClassViewField viewField : ((ClassViewCallExpression) classCall).getClassView().getFields()) {
          if (viewField.getUnderlyingField() == expr.getDefinition().getAbstractDefinition()) {
            definition = viewField;
            break;
          }
        }
      }
    }
    return myFactory.makeDefCall(expr.getExpression().accept(this, null), definition, expr.getDefinition().getAbstractDefinition());
  }

  @Override
  public Abstract.Expression visitConCall(ConCallExpression expr, Void params) {
    Integer num = getNum(expr);
    if (num != null) {
      return myFactory.makeNumericalLiteral(num);
    }

    Abstract.Expression conParams = null;
    if (!expr.getDefinition().typeHasErrors() && myFlags.contains(Flag.SHOW_CON_PARAMS) && (!expr.getDataTypeArguments().isEmpty() || myFlags.contains(Flag.SHOW_CON_DATA_TYPE))) {
      ExprSubstitution substitution = new ExprSubstitution();
      DependentLink link = expr.getDefinition().getDataTypeParameters();
      for (int i = 0; i < expr.getDataTypeArguments().size() && link.hasNext(); i++, link = link.getNext()) {
        substitution.add(link, expr.getDataTypeArguments().get(i));
      }
      conParams = expr.getDefinition().getDataTypeExpression(substitution, expr.getPolyArguments()).accept(this, null);
    }
    return visitArguments(myFactory.makeDefCall(conParams, expr.getDefinition().getAbstractDefinition(), expr.getDefinition().getAbstractDefinition()), expr, false);
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

    Abstract.Expression defCallExpr = myFactory.makeDefCall(enclExpr, expr.getDefinition().getAbstractDefinition(), expr.getDefinition().getAbstractDefinition());
    if (statements.isEmpty()) {
      return defCallExpr;
    } else {
      return myFactory.makeClassExt(defCallExpr, statements);
    }
  }

  private Abstract.Expression visitVariable(Variable var) {
    String name = var.getName() == null ? "_" : var.getName();
    if (var instanceof InferenceLevelVariable && name.charAt(0) == '\\') {
      name = name.substring(1);
    }

    String name1 = myNames.get(name);
    if (name1 == null) {
      name1 = name;
    }

    return myFactory.makeVar((var instanceof InferenceVariable || var instanceof InferenceLevelVariable ? "?" : "") + name1);
  }

  @Override
  public Abstract.Expression visitReference(ReferenceExpression expr, Void params) {
    return visitVariable(expr.getBinding());
  }

  @Override
  public Abstract.Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : visitVariable(expr.getVariable());
  }

  private String renameVar(String name) {
    if (!myNames.containsKey(name)) {
      return name;
    }

    String name0 = name;
    while (myNames.containsKey(name)) {
      name = name + "'";
    }
    myNames.put(name0, name);
    return name;
  }

  private void freeVars(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      myNames.remove(link.getName());
    }
  }

  @Override
  public Abstract.Expression visitLam(LamExpression expr, Void params) {
    List<Abstract.Argument> arguments = new ArrayList<>();
    if (myFlags.contains(Flag.SHOW_TYPES_IN_LAM)) {
      List<String> names = new ArrayList<>(3);
      for (DependentLink link = expr.getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(names);
        for (int i = 0; i < names.size(); i++) {
          names.set(i, renameVar(names.get(i)));
        }
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
      link = link.getNextTyped(names);
      for (int i = 0; i < names.size(); i++) {
        names.set(i, renameVar(names.get(i)));
      }
      if (names.isEmpty() || names.get(0) == null) {
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

  private Integer getHNum(Level expr) {
    if (expr.isClosed()) {
      if (expr.isInfinity()) {
        return Abstract.UniverseExpression.Universe.NOT_TRUNCATED;
      }
      return expr.getConstant() - 1;
    }
    return null;
  }

  private Integer getPNum(Level expr) {
    if (expr.isClosed()) {
      return expr.getConstant();
    }
    return null;
  }

  @Override
  public Abstract.Expression visitUniverse(UniverseExpression expr, Void params) {
    return visitSort(expr.getSort());
  }

  public Abstract.Expression visitSort(Sort sort) {
    Integer pNum = getPNum(sort.getPLevel());
    Integer hNum = getHNum(sort.getHLevel());
    if (pNum != null && hNum != null) {
      return myFactory.makeUniverse(pNum, hNum);
    } else {
      return myFactory.makeUniverse(visitLevel(sort.getPLevel(), 0), visitLevel(sort.getHLevel(), -1));
    }
  }

  public Abstract.Expression visitSortMax(SortMax sort) {
    if (sort.isOmega()) {
      return myFactory.makeUniverse();
    }
    // TODO: generate ordinary universe when levels are constants
    return myFactory.makeUniverse(visitLevelMax(sort.getPLevel(), 0), visitLevelMax(sort.getHLevel(), -1));
  }

  public Abstract.Expression visitLevel(Level level, int add) {
    if (level.isInfinity()) {
      return myFactory.makeVar("inf");
    }
    if (level.isClosed()) {
      return myFactory.makeNumericalLiteral(level.getConstant() + add);
    }

    Abstract.Expression result = visitVariable(level.getVar());
    for (int i = 0; i < level.getConstant() + add; i++) {
      result = myFactory.makeApp(myFactory.makeVar("suc"), true, result);
    }
    return result;
  }

  public Abstract.Expression visitLevelMax(LevelMax levelMax, int add) {
    if (levelMax.isInfinity()) {
      return myFactory.makeVar("inf");
    }

    List<Level> levels = levelMax.toListOfLevels();
    if (levels.isEmpty()) {
      return myFactory.makeNumericalLiteral(add);
    }
    if (levels.size() == 1) {
      return visitLevel(levels.get(0), add);
    }

    Abstract.Expression result = myFactory.makeVar("max");
    for (Level level : levels) {
      result = myFactory.makeApp(result, true, visitLevel(level, add));
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
      clauses.add(myFactory.makeLetClause(renameVar(clause.getName()), arguments, resultType, getTopLevelArrow(clause.getElimTree()), term));
    }

    result = myFactory.makeLet(clauses, letExpression.getExpression().accept(this, null));
    for (LetClause clause : letExpression.getClauses()) {
      myNames.remove(clause.getName());
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
        args.add(myFactory.makePatternArgument(myFactory.makeNamePattern(renameVar(link.getName())), link.isExplicit()));
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
        exprs.add(visitVariable(link));
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
    return myFactory.makeElim(Collections.singletonList(visitVariable(branchNode.getReference())), visitBranch(branchNode));
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
