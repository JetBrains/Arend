package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.factory.AbstractExpressionFactory;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class ToAbstractVisitor extends BaseExpressionVisitor<Void, Abstract.Expression> implements ElimTreeNodeVisitor<Void, Abstract.Expression> {
  public enum Flag { SHOW_CON_DATA_TYPE, SHOW_CON_PARAMS, SHOW_HIDDEN_ARGS, SHOW_IMPLICIT_ARGS, SHOW_TYPES_IN_LAM, SHOW_PREFIX_PATH }
  public static final EnumSet<Flag> DEFAULT = EnumSet.of(Flag.SHOW_IMPLICIT_ARGS);

  private final AbstractExpressionFactory myFactory;
  private EnumSet<Flag> myFlags;

  public ToAbstractVisitor(AbstractExpressionFactory factory) {
    myFactory = factory;
    myFlags = DEFAULT;
  }

  public void setFlags(EnumSet<Flag> flags) {
    myFlags = flags;
  }

  private Abstract.Expression checkPath(Expression fun, List<ArgumentExpression> args) {
    if (!(args.size() == 3 && fun instanceof DefCallExpression && ((DefCallExpression) fun).getDefinition() == Prelude.PATH)) {
      return null;
    }
    for (ArgumentExpression arg : args) {
      if (!arg.isExplicit()) {
        return null;
      }
    }
    if (args.get(2).getExpression() instanceof LamExpression) {
      LamExpression expr = (LamExpression) args.get(2).getExpression();
      if (!expr.getBody().findBinding(expr.getParameters())) {
        return myFactory.makeBinOp(args.get(1).getExpression().accept(this, null), Prelude.PATH_INFIX, args.get(0).getExpression().accept(this, null));
      }
    }
    return null;
  }

  private Abstract.Expression checkBinOp(Expression fun, List<ArgumentExpression> args) {
    if (!(fun instanceof DefCallExpression && ((DefCallExpression) fun).getDefinition().getFixity() == Abstract.Definition.Fixity.INFIX)) {
      return null;
    }

    Expression[] visibleArgs = new Expression[2];
    int i = 1;
    for (ArgumentExpression arg : args) {
      if ((!arg.isHidden() || myFlags.contains(Flag.SHOW_HIDDEN_ARGS)) && (arg.isExplicit() || myFlags.contains(Flag.SHOW_IMPLICIT_ARGS))) {
        if (i < 0) {
          return null;
        }
        visibleArgs[i--] = arg.getExpression();
      }
    }
    return i < 0 ? myFactory.makeBinOp(visibleArgs[0].accept(this, null), ((DefCallExpression) fun).getDefinition(), visibleArgs[1].accept(this, null)) : null;
  }

  @Override
  public Abstract.Expression visitApp(AppExpression expr, Void params) {
    List<ArgumentExpression> args = new ArrayList<>();
    Expression fun = expr.getFunctionArgs(args);

    if (!myFlags.contains(Flag.SHOW_PREFIX_PATH)) {
      Abstract.Expression result = checkPath(fun, args);
      if (result != null) {
        return result;
      }
    }

    Abstract.Expression result = checkBinOp(fun, args);
    if (result != null) {
      return result;
    }

    if (expr.getFunction() instanceof FieldCallExpression) {
      return myFactory.makeDefCall(expr.getArgument().getExpression().accept(this, null), ((FieldCallExpression) expr.getFunction()).getDefinition());
    } else {
      boolean showArg = (!expr.getArgument().isHidden() || myFlags.contains(Flag.SHOW_HIDDEN_ARGS)) && (expr.getArgument().isExplicit() || myFlags.contains(Flag.SHOW_IMPLICIT_ARGS));
      Abstract.Expression abstractFun = expr.getFunction().accept(this, null);
      Abstract.Expression arg = showArg ? expr.getArgument().getExpression().accept(this, null) : expr.getArgument().isExplicit() ? myFactory.makeInferHole() : null;
      return arg != null ? myFactory.makeApp(abstractFun, expr.getArgument().isExplicit(), arg) : abstractFun;
    }
  }

  @Override
  public Abstract.Expression visitDefCall(DefCallExpression expr, Void params) {
    return myFactory.makeDefCall(null, expr.getDefinition());
  }

  @Override
  public Abstract.Expression visitConCall(ConCallExpression expr, Void params) {
    Abstract.Expression conParams = null;
    if (myFlags.contains(Flag.SHOW_CON_PARAMS) && (!expr.getDataTypeArguments().isEmpty() || myFlags.contains(Flag.SHOW_CON_DATA_TYPE))) {
      conParams = myFactory.makeDefCall(null, expr.getDefinition().getDataType());
      DependentLink link = expr.getDefinition().getDataTypeParameters();
      for (int i = 0; i < expr.getDataTypeArguments().size(); i++) {
        conParams = myFactory.makeApp(conParams, link.isExplicit(), expr.getDataTypeArguments().get(i).accept(this, null));
        if (link.hasNext()) {
          link = link.getNext();
        }
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
      List<Expression> args = new ArrayList<>();
      Expression expr = letExpression.getExpression().getFunction(args);
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
      clauses.add(myFactory.makeLetClause(clause.getName(), visitTypeArguments(clause.getParameters()), clause.getResultType() == null ? null : clause.getResultType().accept(this, null), clause.getElimTree().getArrow(), clause.getElimTree().accept(this, null)));
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
      clauses.add(myFactory.makeClause(myFactory.makeConPattern(clause.getConstructor().getName(), args), clause.getChild().getArrow(), clause.getChild().accept(this, null)));
    }
    if (branchNode.getOtherwiseClause() != null) {
      clauses.add(myFactory.makeClause(myFactory.makeNamePattern(null), branchNode.getOtherwiseClause().getChild().getArrow(), branchNode.getOtherwiseClause().getChild().accept(this, null)));
    }
    return clauses;
  }

  @Override
  public Abstract.Expression visitBranch(BranchElimTreeNode branchNode, Void params) {
    return myFactory.makeElim(branchNode.getReference().getName(), visitBranch(branchNode));
  }

  @Override
  public Abstract.Expression visitLeaf(LeafElimTreeNode leafNode, Void params) {
    return leafNode.getExpression() != null ? leafNode.getExpression().accept(this, null) : myFactory.makeError(null);
  }

  @Override
  public Abstract.Expression visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    // TODO: ???
    return null;
  }
}
