package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.*;

public class NormalizeVisitor extends BaseExpressionVisitor<NormalizeVisitor.Mode, Expression>  {
  public enum Mode { WHNF, NF, RNF }

  public static final NormalizeVisitor INSTANCE = new NormalizeVisitor();

  private NormalizeVisitor() {
  }

  @Override
  public Expression visitApp(AppExpression expr, Mode mode) {
    List<Expression> args = new ArrayList<>();
    Expression function = expr;
    while (function.isInstance(AppExpression.class)) {
      args.add(function.cast(AppExpression.class).getArgument());
      function = function.cast(AppExpression.class).getFunction().accept(this, Mode.WHNF);
    }
    Collections.reverse(args);

    if (function.isInstance(LamExpression.class)) {
      return normalizeLam(function.cast(LamExpression.class), args).accept(this, mode);
    }

    if (mode == Mode.NF) {
      function = function.accept(this, mode);
    }
    for (Expression arg : args) {
      function = new AppExpression(function, mode == Mode.WHNF ? arg : arg.accept(this, mode));
    }
    return function;
  }

  private Expression normalizeLam(LamExpression fun, List<? extends Expression> arguments) {
    int i = 0;
    SingleDependentLink link = fun.getParameters();
    ExprSubstitution subst = new ExprSubstitution();
    while (link.hasNext() && i < arguments.size()) {
      subst.add(link, arguments.get(i++));
      link = link.getNext();
    }

    Expression result = fun.getBody();
    if (link.hasNext()) {
      result = new LamExpression(fun.getResultSort(), link, result);
    }
    result = result.subst(subst);
    for (; i < arguments.size(); i++) {
      result = new AppExpression(result, arguments.get(i));
    }
    return result;
  }

  private Expression applyDefCall(DefCallExpression expr, Mode mode) {
    if (mode == Mode.WHNF || expr.getDefCallArguments().isEmpty()) {
      return expr;
    }

    if (expr instanceof FieldCallExpression) {
      return ExpressionFactory.FieldCall((ClassField) expr.getDefinition(), ((FieldCallExpression) expr).getExpression().accept(this, mode));
    }

    if (expr instanceof FunCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new FunCallExpression((FunctionDefinition) expr.getDefinition(), expr.getSortArgument(), args);
    }

    if (expr instanceof DataCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new DataCallExpression((DataDefinition) expr.getDefinition(), expr.getSortArgument(), args);
    }

    if (expr instanceof ConCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new ConCallExpression((Constructor) expr.getDefinition(), expr.getSortArgument(), ((ConCallExpression) expr).getDataTypeArguments(), args);
    }

    throw new IllegalStateException();
  }

  private Expression visitDefCall(DefCallExpression expr, LevelSubstitution levelSubstitution, Mode mode) {
    if (expr.getDefinition() == Prelude.COERCE) {
      LamExpression lamExpr = expr.getDefCallArguments().get(0).accept(this, Mode.WHNF).checkedCast(LamExpression.class);
      if (lamExpr != null) {
        Expression body = lamExpr.getParameters().getNext().hasNext() ? new LamExpression(lamExpr.getResultSort(), lamExpr.getParameters().getNext(), lamExpr.getBody()) : lamExpr.getBody();
        body = body.accept(this, Mode.WHNF);
        FunCallExpression funCall = body.checkedCast(FunCallExpression.class);
        boolean checkSigma = true;

        if (funCall != null && funCall.getDefinition() == Prelude.ISO) {
          List<? extends Expression> isoArgs = funCall.getDefCallArguments();
          ReferenceExpression refExpr = isoArgs.get(isoArgs.size() - 1).accept(this, Mode.WHNF).checkedCast(ReferenceExpression.class);
          if (refExpr != null && refExpr.getBinding() == lamExpr.getParameters()) {
            checkSigma = false;
            ConCallExpression normedPtCon = expr.getDefCallArguments().get(2).accept(this, Mode.WHNF).checkedCast(ConCallExpression.class);
            if (normedPtCon != null && normedPtCon.getDefinition() == Prelude.RIGHT) {
              boolean noFreeVar = true;
              for (int i = 0; i < isoArgs.size() - 1; i++) {
                if (NormalizingFindBindingVisitor.findBinding(isoArgs.get(i), lamExpr.getParameters())) {
                  noFreeVar = false;
                  break;
                }
              }
              if (noFreeVar) {
                return visitApp(new AppExpression(isoArgs.get(2), expr.getDefCallArguments().get(1)), mode);
              }
            }
          }
        }

        if (checkSigma && !NormalizingFindBindingVisitor.findBinding(body, lamExpr.getParameters())) {
          return expr.getDefCallArguments().get(1).accept(this, mode);
        }
      }
    }

    ElimTree elimTree;
    Body body = ((Function) expr.getDefinition()).getBody();
    if (body instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) body;
      int i0 = expr.getDefCallArguments().size() - elim.getCases().size();
      for (int i = i0; i < expr.getDefCallArguments().size(); i++) {
        Expression arg = expr.getDefCallArguments().get(i).accept(this, Mode.WHNF);
        ConCallExpression conCall = arg.checkedCast(ConCallExpression.class);
        if (conCall != null) {
          ExprSubstitution substitution = getDataTypeArgumentsSubstitution(expr);
          DependentLink link = elim.getParameters();
          for (int j = 0; j < expr.getDefCallArguments().size(); j++) {
            if (j != i) {
              substitution.add(link, expr.getDefCallArguments().get(j));
            }
            link = link.getNext();
          }

          Expression result;
          if (conCall.getDefinition() == Prelude.LEFT) {
            result = elim.getCases().get(i - i0).proj1;
          } else if (conCall.getDefinition() == Prelude.RIGHT) {
            result = elim.getCases().get(i - i0).proj2;
          } else {
            throw new IllegalStateException();
          }
          return result == null ? applyDefCall(expr, mode) : result.subst(substitution).accept(this, mode);
        }
      }
      elimTree = elim.getOtherwise();
    } else {
      elimTree = mode == Mode.RNF && body instanceof LeafElimTree ? null : (ElimTree) body;
    }

    if (elimTree == null) {
      return applyDefCall(expr, mode);
    }

    Expression result = eval(elimTree, expr.getDefCallArguments(), getDataTypeArgumentsSubstitution(expr), levelSubstitution);

    if (Thread.interrupted()) {
      throw new ComputationInterruptedException();
    }

    return result == null ? applyDefCall(expr, mode) : result.accept(this, mode);
  }

  public Expression eval(ElimTree elimTree, List<? extends Expression> arguments, ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
    Stack<Expression> stack = new Stack<>();
    for (int i = arguments.size() - 1; i >= 0; i--) {
      stack.push(arguments.get(i));
    }

    while (true) {
      for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
        substitution.add(link, stack.pop());
      }
      if (elimTree instanceof LeafElimTree) {
        return ((LeafElimTree) elimTree).getExpression().subst(substitution, levelSubstitution);
      }

      Expression argument = stack.peek().accept(this, Mode.WHNF);
      ConCallExpression conCall = argument.checkedCast(ConCallExpression.class);
      elimTree = ((BranchElimTree) elimTree).getChild(conCall == null ? null : conCall.getDefinition());
      if (elimTree == null) {
        return null;
      }

      if (conCall != null) {
        stack.pop();
        for (int i = conCall.getDefCallArguments().size() - 1; i >= 0; i--) {
          stack.push(conCall.getDefCallArguments().get(i));
        }
      }
    }
  }

  public boolean doesEvaluate(ElimTree elimTree, List<? extends Expression> arguments) {
    Stack<Expression> stack = new Stack<>();
    for (int i = arguments.size() - 1; i >= 0; i--) {
      stack.push(arguments.get(i));
    }

    while (true) {
      for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
        if (stack.isEmpty()) {
          return true;
        }
        stack.pop();
      }
      if (elimTree instanceof LeafElimTree || stack.isEmpty()) {
        return true;
      }

      Expression argument = stack.peek().accept(this, Mode.WHNF);
      ConCallExpression conCall = argument.checkedCast(ConCallExpression.class);
      elimTree = ((BranchElimTree) elimTree).getChild(conCall == null ? null : conCall.getDefinition());
      if (elimTree == null) {
        return false;
      }

      if (conCall != null) {
        stack.pop();
        for (int i = conCall.getDefCallArguments().size() - 1; i >= 0; i--) {
          stack.push(conCall.getDefCallArguments().get(i));
        }
      }
    }
  }

  private ExprSubstitution getDataTypeArgumentsSubstitution(DefCallExpression expr) {
    ExprSubstitution substitution = new ExprSubstitution();
    if (expr instanceof ConCallExpression) {
      int i = 0;
      List<Expression> args = ((ConCallExpression) expr).getDataTypeArguments();
      for (DependentLink link = ((ConCallExpression) expr).getDefinition().getDataTypeParameters(); link.hasNext(); link = link.getNext()) {
        substitution.add(link, args.get(i++));
      }
    }
    return substitution;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Mode mode) {
    if (!expr.getDefinition().status().bodyIsOK()) {
      return applyDefCall(expr, mode);
    }

    if (expr instanceof FieldCallExpression) {
      Expression thisExpr = ((FieldCallExpression) expr).getExpression().accept(this, Mode.WHNF);
      if (!thisExpr.isInstance(InferenceReferenceExpression.class) || !(thisExpr.cast(InferenceReferenceExpression.class).getVariable() instanceof TypeClassInferenceVariable)) {
        Expression type = thisExpr.getType();
        ClassCallExpression classCall = type == null ? null : type.accept(this, Mode.WHNF).checkedCast(ClassCallExpression.class);
        if (classCall != null) {
          Expression impl = classCall.getImplementation((ClassField) expr.getDefinition(), thisExpr);
          if (impl != null) {
            return impl.accept(this, mode);
          }
        }
      }
      return FieldCallExpression.make((ClassField) expr.getDefinition(), mode == Mode.WHNF ? thisExpr : thisExpr.accept(this, mode));
    }

    if (expr.getDefinition() instanceof Function) {
      return visitDefCall(expr, expr.getSortArgument().toLevelSubstitution(), mode);
    }

    return applyDefCall(expr, mode);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Mode mode) {
    if (mode == Mode.WHNF) return expr;

    Map<ClassField, Expression> fieldSet = new HashMap<>();
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      fieldSet.put(entry.getKey(), entry.getValue().accept(this, mode));
    }
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), fieldSet, expr.getSort());
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Mode mode) {
    return (DataCallExpression) applyDefCall(expr, mode);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Mode mode) {
    return expr.getBinding() instanceof LetClause && mode != Mode.RNF ? ((LetClause) expr.getBinding()).getExpression().accept(this, mode) : expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Mode mode) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, mode) : expr;
  }

  @Override
  public Expression visitLam(LamExpression expr, Mode mode) {
    if (mode == Mode.RNF) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = normalizeSingleParameters(expr.getParameters(), mode, substitution);
      return new LamExpression(expr.getResultSort(), link, expr.getBody().subst(substitution).accept(this, mode));
    }
    if (mode == Mode.NF) {
      return new LamExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Mode mode) {
    if (mode != Mode.WHNF) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = normalizeSingleParameters(expr.getParameters(), mode, substitution);
      return new PiExpression(expr.getResultSort(), link, expr.getCodomain().subst(substitution).accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Mode mode) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Mode mode) {
    return mode == Mode.WHNF || expr.getExpression() == null ? expr : new ErrorExpression(expr.getExpression().accept(this, mode), expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Mode mode) {
    if (mode == Mode.WHNF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, mode));
    }
    return new TupleExpression(fields, expr.getSigmaType());
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Mode mode) {
    if (mode == Mode.WHNF) {
      return expr;
    }

    return new SigmaExpression(expr.getSort(), normalizeParameters(expr.getParameters(), mode, new ExprSubstitution()));
  }

  private DependentLink normalizeParameters(DependentLink parameters, Mode mode, ExprSubstitution substitution) {
    DependentLink link = DependentLink.Helper.subst(parameters, substitution);
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      link1 = link1.getNextTyped(null);
      link1.setType(link1.getType().normalize(mode));
    }
    return link;
  }

  private SingleDependentLink normalizeSingleParameters(SingleDependentLink parameters, Mode mode, ExprSubstitution substitution) {
    SingleDependentLink link = DependentLink.Helper.subst(parameters, substitution);
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      link1 = link1.getNextTyped(null);
      link1.setType(link1.getType().normalize(mode));
    }
    return link;
  }

  @Override
  public Expression visitProj(ProjExpression expr, Mode mode) {
    Expression newExpr = expr.getExpression().accept(this, Mode.WHNF);
    TupleExpression exprNorm = newExpr.checkedCast(TupleExpression.class);
    if (exprNorm != null) {
      return exprNorm.getFields().get(expr.getField()).accept(this, mode);
    } else {
      return mode == Mode.WHNF ? ProjExpression.make(newExpr, expr.getField()) : ProjExpression.make(expr.getExpression().accept(this, mode), expr.getField());
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, Mode mode) {
    return mode == Mode.WHNF ? expr : new NewExpression(visitClassCall(expr.getExpression(), mode));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Mode mode) {
    if (mode == Mode.RNF) {
      List<LetClause> newClauses = new ArrayList<>(letExpression.getClauses().size());
      for (LetClause clause : letExpression.getClauses()) {
        newClauses.add(new LetClause(clause.getName(), clause.getExpression().accept(this, mode)));
      }
      return new LetExpression(newClauses, letExpression.getExpression().accept(this, mode));
    } else {
      return letExpression.getExpression().subst(letExpression.getClausesSubstitution()).accept(this, mode);
    }
  }

  @Override
  public Expression visitCase(CaseExpression expr, Mode mode) {
    Expression result = eval(expr.getElimTree(), expr.getArguments(), new ExprSubstitution(), LevelSubstitution.EMPTY);
    if (result != null) {
      return result;
    }
    if (mode == Mode.WHNF) {
      return expr;
    }

    List<Expression> args = new ArrayList<>(expr.getArguments().size());
    for (Expression arg : expr.getArguments()) {
      args.add(arg.accept(this, mode));
    }
    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink parameters = normalizeParameters(expr.getParameters(), mode, substitution);
    return new CaseExpression(parameters, expr.getResultType().subst(substitution).accept(this, mode), normalizeElimTree(expr.getElimTree(), mode), args);
  }

  private ElimTree normalizeElimTree(ElimTree elimTree, Mode mode) {
    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink vars = DependentLink.Helper.subst(elimTree.getParameters(), substitution);
    if (elimTree instanceof LeafElimTree) {
      return new LeafElimTree(vars, ((LeafElimTree) elimTree).getExpression().subst(substitution).accept(this, mode));
    } else {
      Map<Constructor, ElimTree> children = new HashMap<>();
      SubstVisitor visitor = new SubstVisitor(substitution, LevelSubstitution.EMPTY);
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        children.put(entry.getKey(), visitor.substElimTree(normalizeElimTree(entry.getValue(), mode)));
      }
      return new BranchElimTree(vars, children);
    }
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Mode mode) {
    return mode == Mode.NF ? new OfTypeExpression(expr.getExpression().accept(this, mode), expr.getTypeOf()) : expr.getExpression().accept(this, mode);
  }
}
