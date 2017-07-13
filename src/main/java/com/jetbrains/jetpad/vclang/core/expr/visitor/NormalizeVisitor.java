package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.*;
import java.util.stream.Collectors;

public class NormalizeVisitor extends BaseExpressionVisitor<NormalizeVisitor.Mode, Expression>  {
  public enum Mode { WHNF, NF, HUMAN_NF }

  @Override
  public Expression visitApp(AppExpression expr, Mode mode) {
    List<Expression> args = new ArrayList<>();
    Expression function = expr;
    while (function.toApp() != null) {
      args.add(function.toApp().getArgument());
      function = function.toApp().getFunction().normalize(Mode.WHNF);
    }
    Collections.reverse(args);

    if (function.toLam() != null) {
      return normalizeLam(function.toLam(), args).accept(this, mode);
    }

    if (mode == Mode.NF) {
      function = function.accept(this, mode);
    }
    for (Expression arg : args) {
      function = new AppExpression(function, mode == Mode.NF ? arg.accept(this, mode) : arg);
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

  private Expression applyDefCall(CallableCallExpression expr, Mode mode) {
    if (expr.getDefCallArguments().isEmpty() || (mode != Mode.NF && mode != Mode.HUMAN_NF)) {
      return (Expression) expr;
    }

    if (expr instanceof FieldCallExpression) {
      return ExpressionFactory.FieldCall((ClassField) expr.getDefinition(), ((FieldCallExpression) expr).getExpression().accept(this, mode));
    }

    if (expr instanceof FunCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new FunCallExpression((FunctionDefinition) expr.getDefinition(), ((FunCallExpression) expr).getSortArgument(), args);
    }

    if (expr instanceof DataCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new DataCallExpression((DataDefinition) expr.getDefinition(), ((DataCallExpression) expr).getSortArgument(), args);
    }

    if (expr instanceof ConCallExpression) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, mode));
      }
      return new ConCallExpression((Constructor) expr.getDefinition(), ((ConCallExpression) expr).getSortArgument(), ((ConCallExpression) expr).getDataTypeArguments(), args);
    }

    throw new IllegalStateException();
  }

  private Expression visitConstructorCall(ConCallExpression expr, Mode mode) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments());
    int take = DependentLink.Helper.size(expr.getDefinition().getDataTypeParameters()) - expr.getDataTypeArguments().size();
    if (take > 0) {
      if (take >= args.size()) {
        take = args.size();
      }
      List<Expression> parameters = new ArrayList<>(expr.getDataTypeArguments().size() + take);
      parameters.addAll(expr.getDataTypeArguments());
      for (int i = 0; i < take; i++) {
        parameters.add(args.get(i));
      }
      expr = new ConCallExpression(expr.getDefinition(), expr.getSortArgument(), parameters, args.subList(take, args.size()));
    }

    return visitCallableCall(expr, expr.getSortArgument().toLevelSubstitution(), mode);
  }

  private Expression visitCallableCall(CallableCallExpression expr, LevelSubstitution levelSubstitution, Mode mode) {
    if (expr.getDefinition() == Prelude.COERCE) {
      Expression result = null;

      Binding binding = new TypedBinding("i", ExpressionFactory.Interval());
      Expression normExpr = new AppExpression(expr.getDefCallArguments().get(0), new ReferenceExpression(binding)).normalize(NormalizeVisitor.Mode.NF);
      if (!normExpr.findBinding(binding)) {
        result = expr.getDefCallArguments().get(1);
      } else {
        if (normExpr.toFunCall() != null && normExpr.toFunCall().getDefinition() == Prelude.ISO) {
          List<? extends Expression> isoArgs = normExpr.toFunCall().getDefCallArguments();
          boolean noFreeVar = true;
          for (int i = 0; i < isoArgs.size() - 1; i++) {
            if (isoArgs.get(i).findBinding(binding)) {
              noFreeVar = false;
              break;
            }
          }
          if (noFreeVar) {
            ConCallExpression normedPtCon = expr.getDefCallArguments().get(2).normalize(NormalizeVisitor.Mode.NF).toConCall();
            if (normedPtCon != null && normedPtCon.getDefinition() == Prelude.RIGHT) {
              result = new AppExpression(isoArgs.get(2), expr.getDefCallArguments().get(1));
            }
          }
        }
      }

      if (result != null) {
        return result.normalize(mode);
      }
    }

    ElimTree elimTree;
    Body body = ((Function) expr.getDefinition()).getBody();
    if (body instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) body;
      int i0 = expr.getDefCallArguments().size() - elim.getCases().size();
      for (int i = i0; i < expr.getDefCallArguments().size(); i++) {
        Expression arg = expr.getDefCallArguments().get(i).accept(this, Mode.WHNF);
        if (arg.toConCall() != null) {
          ExprSubstitution substitution = getDataTypeArgumentsSubstitution(expr);
          DependentLink link = elim.getParameters();
          for (int j = 0; j < expr.getDefCallArguments().size(); j++) {
            if (j != i) {
              substitution.add(link, expr.getDefCallArguments().get(j));
            }
            link = link.getNext();
          }

          Expression result;
          if (arg.toConCall().getDefinition() == Prelude.LEFT) {
            result = elim.getCases().get(i - i0).proj1;
          } else if (arg.toConCall().getDefinition() == Prelude.RIGHT) {
            result = elim.getCases().get(i - i0).proj2;
          } else {
            throw new IllegalStateException();
          }
          return result == null ? applyDefCall(expr, mode) : result.subst(substitution).accept(this, mode);
        }
      }
      elimTree = elim.getOtherwise();
    } else {
      elimTree = (ElimTree) body;
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
      elimTree = ((BranchElimTree) elimTree).getChild(argument.toConCall() == null ? null : argument.toConCall().getDefinition());
      if (elimTree == null) {
        return null;
      }

      if (argument.toConCall() != null) {
        stack.pop();
        for (int i = argument.toConCall().getDefCallArguments().size() - 1; i >= 0; i--) {
          stack.push(argument.toConCall().getDefCallArguments().get(i));
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
      elimTree = ((BranchElimTree) elimTree).getChild(argument.toConCall() == null ? null : argument.toConCall().getDefinition());
      if (elimTree == null) {
        return false;
      }

      if (argument.toConCall() != null) {
        stack.pop();
        for (int i = argument.toConCall().getDefCallArguments().size() - 1; i >= 0; i--) {
          stack.push(argument.toConCall().getDefCallArguments().get(i));
        }
      }
    }
  }

  private ExprSubstitution getDataTypeArgumentsSubstitution(CallableCallExpression expr) {
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
      Expression thisExpr = ((FieldCallExpression) expr).getExpression().normalize(Mode.WHNF);
      if (thisExpr.toInferenceReference() == null || !(thisExpr.toInferenceReference().getVariable() instanceof TypeClassInferenceVariable)) {
        ClassCallExpression classCall = thisExpr.getType().normalize(Mode.WHNF).toClassCall();
        if (classCall != null) {
          FieldSet.Implementation impl = classCall.getFieldSet().getImplementation((ClassField) expr.getDefinition());
          if (impl != null) {
            return impl.substThisParam(thisExpr).accept(this, mode);
          }
        }
      }
    }

    if (expr.toConCall() != null) {
      return visitConstructorCall(expr.toConCall(), mode);
    }
    if (expr.getDefinition() instanceof Function) {
      return visitCallableCall(expr, expr.getSortArgument().toLevelSubstitution(), mode);
    }

    return applyDefCall(expr, mode);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Mode mode) {
    if (mode == Mode.WHNF) return expr;

    FieldSet fieldSet = FieldSet.applyVisitorToImplemented(expr.getFieldSet(), expr.getDefinition().getFieldSet(), this, mode);
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), fieldSet);
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Mode mode) {
    return (DataCallExpression) applyDefCall(expr, mode);
  }

  @Override
  public Expression visitLetClauseCall(LetClauseCallExpression expr, Mode mode) {
    return expr.getLetClause().getExpression().accept(this, mode);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Mode mode) {
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Mode mode) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, mode) : expr;
  }

  @Override
  public Expression visitLam(LamExpression expr, Mode mode) {
    if (mode == Mode.HUMAN_NF) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = DependentLink.Helper.subst(expr.getParameters(), substitution);
      for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
        link1 = link1.getNextTyped(null);
        link1.setType(link1.getType().normalize(mode));
      }
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
    if (mode == Mode.HUMAN_NF || mode == Mode.NF) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = DependentLink.Helper.subst(expr.getParameters(), substitution);
      for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
        link1 = link1.getNextTyped(null);
        link1.setType(link1.getType().normalize(mode));
      }
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
    return mode != Mode.NF && mode != Mode.HUMAN_NF || expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, mode), expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Mode mode) {
    if (mode != Mode.NF && mode != Mode.HUMAN_NF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, mode));
    }
    return new TupleExpression(fields, expr.getSigmaType());
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Mode mode) {
    if (mode != Mode.NF && mode != Mode.HUMAN_NF) {
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

  @Override
  public Expression visitProj(ProjExpression expr, Mode mode) {
    TupleExpression exprNorm = expr.getExpression().normalize(Mode.WHNF).toTuple();
    if (exprNorm != null) {
      return exprNorm.getFields().get(expr.getField()).accept(this, mode);
    } else {
      return mode == Mode.NF || mode == Mode.HUMAN_NF ? new ProjExpression(expr.getExpression().accept(this, mode), expr.getField()) : expr;
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, Mode mode) {
    return mode == Mode.WHNF ? expr : new NewExpression(visitClassCall(expr.getExpression(), mode));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Mode mode) {
    ExprSubstitution substitution = new ExprSubstitution();
    for (LetClause clause : letExpression.getClauses()) {
      substitution.add(clause, clause.getExpression());
    }
    return letExpression.getExpression().subst(substitution).accept(this, mode);
  }

  @Override
  public Expression visitCase(CaseExpression expr, Mode mode) {
    Expression result = eval(expr.getElimTree(), expr.getArguments(), new ExprSubstitution(), LevelSubstitution.EMPTY);
    if (result != null) {
      return result;
    }
    if (mode != Mode.NF) {
      return expr;
    }

    List<Expression> args = expr.getArguments().stream().map(arg -> arg.accept(this, mode)).collect(Collectors.toList());
    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink parameters = normalizeParameters(expr.getParameters(), mode, substitution);
    return new CaseExpression(parameters, expr.getResultType().subst(substitution).accept(this, mode), normalizeElimTree(expr.getElimTree()), args);
  }

  private ElimTree normalizeElimTree(ElimTree elimTree) {
    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink vars = DependentLink.Helper.subst(elimTree.getParameters(), substitution);
    if (elimTree instanceof LeafElimTree) {
      return new LeafElimTree(vars, ((LeafElimTree) elimTree).getExpression().subst(substitution).accept(this, Mode.NF));
    } else {
      Map<Constructor, ElimTree> children = new HashMap<>();
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        children.put(entry.getKey(), normalizeElimTree(entry.getValue()));
      }
      return new BranchElimTree(vars, children);
    }
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Mode mode) {
    return mode == Mode.NF ? new OfTypeExpression(expr.getExpression().accept(this, mode), expr.getTypeOf()) : expr.getExpression().accept(this, mode);
  }
}
