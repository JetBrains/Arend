package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.factory.AbstractExpressionFactory;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.*;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Abstract.Expression> implements ElimTreeNodeVisitor<Void, Abstract.Expression> {
  public enum Flag { SHOW_CON_DATA_TYPE, SHOW_CON_PARAMS, SHOW_HIDDEN_ARGS, SHOW_IMPLICIT_ARGS, SHOW_TYPES_IN_LAM, SHOW_PREFIX_PATH, SHOW_BIN_OP_IMPLICIT_ARGS }
  public static final EnumSet<Flag> DEFAULT = EnumSet.of(Flag.SHOW_IMPLICIT_ARGS, Flag.SHOW_CON_PARAMS);

  private final AbstractExpressionFactory myFactory;
  private EnumSet<Flag> myFlags;

  public ToAbstractVisitor(AbstractExpressionFactory factory) {
    myFactory = factory;
    myFlags = DEFAULT;
  }

  public void setFlags(EnumSet<Flag> flags) {
    myFlags = flags;
  }

  public ToAbstractVisitor addFlags(Flag flag) {
    myFlags.add(flag);
    return this;
  }

  private Abstract.Expression checkPath(AppExpression expr) {
    Expression fun = expr.getFunction();
    List<? extends Expression> args = expr.getArguments();

    if (!(args.size() == 3 && fun instanceof DefCallExpression && Prelude.isPath(((DefCallExpression) fun).getDefinition()))) {
      return null;
    }
    for (EnumSet<AppExpression.Flag> flag : expr.getFlags()) {
      if (!flag.contains(AppExpression.Flag.EXPLICIT)) {
        return null;
      }
    }
    if (args.get(2) instanceof LamExpression) {
      LamExpression expr1 = (LamExpression) args.get(2);
      if (!expr1.getBody().findBinding(expr1.getParameters())) {
        return myFactory.makeBinOp(args.get(1).accept(this, null), Prelude.getLevelDefs(Prelude.getLevel(((DefCallExpression) fun).getDefinition())).pathInfix, args.get(0).accept(this, null));
      }
    }
    return null;
  }

  private Abstract.Expression checkBinOp(AppExpression expr) {
    Expression fun = expr.getFunction();

    if (!(fun instanceof DefCallExpression && new Name(((DefCallExpression) fun).getDefinition().getName()).fixity == Name.Fixity.INFIX)) {
      return null;
    }
    if (expr.getFlags().size() < 2 || myFlags.contains(Flag.SHOW_BIN_OP_IMPLICIT_ARGS) && (!expr.getFlags().get(0).contains(AppExpression.Flag.EXPLICIT) || !expr.getFlags().get(1).contains(AppExpression.Flag.EXPLICIT))) {
      return null;
    }

    Expression[] visibleArgs = new Expression[2];
    int i = 0;
    for (int j = 0; j < expr.getArguments().size(); j++) {
      if (expr.getFlags().get(j).contains(AppExpression.Flag.EXPLICIT) && (expr.getFlags().get(j).contains(AppExpression.Flag.VISIBLE) || myFlags.contains(Flag.SHOW_HIDDEN_ARGS))) {
        if (i == 2) {
          return null;
        }
        visibleArgs[i++] = expr.getArguments().get(j);
      }
    }
    return i < 0 ? myFactory.makeBinOp(visibleArgs[0].accept(this, null), ((DefCallExpression) fun).getDefinition(), visibleArgs[1].accept(this, null)) : null;
  }

  @Override
  public Abstract.Expression visitApp(AppExpression expr, Void params) {
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

    int index;
    if (expr.getFunction() instanceof FieldCallExpression) {
      result = myFactory.makeDefCall(expr.getArguments().get(0).accept(this, null), ((FieldCallExpression) expr.getFunction()).getDefinition());
      index = 1;
    } else {
      result = expr.getFunction().accept(this, null);
      index = 0;
    }

    for (; index < expr.getArguments().size(); index++) {
      result = visitApp(result, expr.getArguments().get(index), expr.getFlags().get(index));
    }
    return result;
  }

  private Abstract.Expression visitApp(Abstract.Expression function, Expression argument, EnumSet<AppExpression.Flag> flag) {
    boolean showArg = (flag.contains(AppExpression.Flag.VISIBLE) || myFlags.contains(Flag.SHOW_HIDDEN_ARGS)) && (flag.contains(AppExpression.Flag.EXPLICIT) || myFlags.contains(Flag.SHOW_IMPLICIT_ARGS));
    Abstract.Expression arg = showArg ? argument.accept(this, null) : flag.contains(AppExpression.Flag.EXPLICIT) ? myFactory.makeInferHole() : null;
    return arg != null ? myFactory.makeApp(function, flag.contains(AppExpression.Flag.EXPLICIT), arg) : function;
  }

  @Override
  public Abstract.Expression visitDefCall(DefCallExpression expr, Void params) {
    return myFactory.makeDefCall(null, expr.getDefinition());
  }

  @Override
  public Abstract.Expression visitConCall(ConCallExpression expr, Void params) {
    Abstract.Expression conParams = null;
    if (!expr.getDefinition().hasErrors() && myFlags.contains(Flag.SHOW_CON_PARAMS) && (!expr.getDataTypeArguments().isEmpty() || myFlags.contains(Flag.SHOW_CON_DATA_TYPE))) {
      conParams = myFactory.makeDefCall(null, expr.getDefinition().getDataType());
      DependentLink link = expr.getDefinition().getDataTypeParameters();
      for (int i = 0; i < expr.getDataTypeArguments().size() && link.hasNext(); i++, link = link.getNext()) {
        conParams = myFactory.makeApp(conParams, link.isExplicit(), expr.getDataTypeArguments().get(i).accept(this, null));
      }
    }
    return myFactory.makeDefCall(conParams, expr.getDefinition());
  }

  @Override
  public Abstract.Expression visitClassCall(ClassCallExpression expr, Void params) {
    Abstract.Expression defCallExpr = myFactory.makeDefCall(null, expr.getDefinition());
    if (expr.getImplementStatements().isEmpty()) {
      return defCallExpr;
    } else {
      List<Abstract.ImplementStatement> statements = new ArrayList<>(expr.getImplementStatements().size());
      for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> entry : expr.getImplementStatements().entrySet()) {
        statements.add(myFactory.makeImplementStatement(entry.getKey(), entry.getValue().type == null ? null : entry.getValue().type.accept(this, null), entry.getValue().term == null ? null : entry.getValue().term.accept(this, null)));
      }
      return myFactory.makeClassExt(defCallExpr, statements);
    }
  }

  @Override
  public Abstract.Expression visitReference(ReferenceExpression expr, Void params) {
    String name = expr.getBinding().getName() == null ? "_" : expr.getBinding().getName();
    return myFactory.makeVar((expr.getBinding() instanceof InferenceBinding ? "?" : "") + name);
  }

  @Override
  public Abstract.Expression visitLam(LamExpression expr, Void params) {
    List<Abstract.Argument> arguments = new ArrayList<>();
    if (myFlags.contains(Flag.SHOW_TYPES_IN_LAM)) {
      List<String> names = new ArrayList<>(3);
      for (DependentLink link = expr.getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(names);
        if (names.isEmpty()) {
          names.add(null);
        }
        arguments.add(myFactory.makeTelescopeArgument(link.isExplicit(), new ArrayList<>(names), link.getType().accept(this, null)));
        names.clear();
      }
    } else {
      for (DependentLink link = expr.getParameters(); link.hasNext(); link = link.getNext()) {
        arguments.add(myFactory.makeNameArgument(link.isExplicit(), link.getName()));
      }
    }
    return myFactory.makeLam(arguments, expr.getBody().accept(this, null));
  }

  @Override
  public Abstract.Expression visitPi(PiExpression expr, Void params) {
    return myFactory.makePi(visitTypeArguments(expr.getParameters()), expr.getCodomain().accept(this, null));
  }

  private List<Abstract.TypeArgument> visitTypeArguments(DependentLink arguments) {
    List<Abstract.TypeArgument> args = new ArrayList<>();
    List<String> names = new ArrayList<>(3);
    for (DependentLink link = arguments; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(names);
      if (names.isEmpty() || names.get(0) == null) {
        args.add(myFactory.makeTypeArgument(link.isExplicit(), link.getType().accept(this, null)));
      } else {
        args.add(myFactory.makeTelescopeArgument(link.isExplicit(), new ArrayList<>(names), link.getType().accept(this, null)));
        names.clear();
      }
    }
    return args;
  }

  @Override
  public Abstract.Expression visitUniverse(UniverseExpression expr, Void params) {
    return myFactory.makeUniverse(expr.getUniverse());
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
    return myFactory.makeSigma(visitTypeArguments(expr.getParameters()));
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
      Expression expr = letExpression.getExpression().getFunction();
      List<? extends Expression> args = letExpression.getExpression().getArguments();
      if (expr instanceof ReferenceExpression && ((ReferenceExpression) expr).getBinding() == letExpression.getClauses().get(0)) {
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
      clauses.add(myFactory.makeLetClause(clause.getName(), visitTypeArguments(clause.getParameters()), clause.getResultType() == null ? null : clause.getResultType().accept(this, null), getTopLevelArrow(clause.getElimTree()), visitElimTree(clause.getElimTree(), clause.getParameters())));
    }
    return myFactory.makeLet(clauses, letExpression.getExpression().accept(this, null));
  }

  private List<Abstract.Clause> visitBranch(BranchElimTreeNode branchNode) {
    List<Abstract.Clause> clauses = new ArrayList<>(branchNode.getConstructorClauses().size());
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      List<Abstract.PatternArgument> args = new ArrayList<>();
      for (DependentLink link = clause.getConstructor().getParameters(); link.hasNext(); link = link.getNext()) {
        args.add(myFactory.makePatternArgument(myFactory.makeNamePattern(link.getName()), link.isExplicit()));
      }
      clauses.add(myFactory.makeClause(Collections.singletonList(myFactory.makeConPattern(clause.getConstructor().getName(), args)), clause.getChild().getArrow(), clause.getChild() == EmptyElimTreeNode.getInstance() ? null : clause.getChild().accept(this, null)));
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
        exprs.add(myFactory.makeVar(link.getName()));
      }
      return myFactory.makeElim(exprs, Collections.<Abstract.Clause>emptyList());
    }
    return elimTree.accept(this, null);
  }

  private Abstract.Definition.Arrow getTopLevelArrow(ElimTreeNode elimTreeNode) {
    if (elimTreeNode == EmptyElimTreeNode.getInstance()) {
      return Abstract.Definition.Arrow.RIGHT;
    } else {
      return elimTreeNode.getArrow();
    }
  }

  @Override
  public Abstract.Expression visitBranch(BranchElimTreeNode branchNode, Void params) {
    return myFactory.makeElim(Collections.singletonList(myFactory.makeVar(branchNode.getReference().getName())), visitBranch(branchNode));
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
