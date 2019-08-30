package org.arend.core.expr.visitor;

import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.prelude.Prelude;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.util.Pair;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;

public class NormalizeVisitor extends BaseExpressionVisitor<NormalizeVisitor.Mode, Expression>  {
  public enum Mode { WHNF, NF, RNF, RNF_EXP }

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
      function = AppExpression.make(function, mode == Mode.WHNF ? arg : arg.accept(this, mode));
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
      result = AppExpression.make(result, arguments.get(i));
    }
    return result;
  }

  private Expression applyDefCall(DefCallExpression expr, Mode mode) {
    if (mode == Mode.WHNF || expr.getDefCallArguments().isEmpty()) {
      return expr;
    }

    if (expr instanceof FieldCallExpression) {
      return FieldCallExpression.make((ClassField) expr.getDefinition(), expr.getSortArgument(), ((FieldCallExpression) expr).getArgument().accept(this, mode));
    }

    DependentLink link = expr.getDefinition().getParameters();
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(mode == Mode.RNF_EXP && !link.isExplicit() ? arg : arg.accept(this, mode));
      if (link.hasNext()) {
        link = link.getNext();
      }
    }

    if (expr instanceof FunCallExpression) {
      return new FunCallExpression((FunctionDefinition) expr.getDefinition(), expr.getSortArgument(), args);
    }

    if (expr instanceof DataCallExpression) {
      return new DataCallExpression((DataDefinition) expr.getDefinition(), expr.getSortArgument(), args);
    }

    if (expr instanceof ConCallExpression) {
      return ConCallExpression.make((Constructor) expr.getDefinition(), expr.getSortArgument(), ((ConCallExpression) expr).getDataTypeArguments(), args);
    }

    throw new IllegalStateException();
  }

  private Expression normalizePlus(DefCallExpression expr, Mode mode) {
    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    Expression arg1 = defCallArgs.get(0);
    Expression arg2 = defCallArgs.get(1).accept(this, Mode.WHNF);

    if (arg2.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
      arg1 = arg1.accept(this, Mode.WHNF);
      if (arg1.isInstance(IntegerExpression.class)) {
        return arg1.cast(IntegerExpression.class).plus(intExpr2);
      }
      if (intExpr2.isZero()) {
        return arg1.accept(this, mode);
      }

      if (mode != Mode.WHNF) {
        arg1 = arg1.accept(this, mode);
      }
      for (int i = 0; intExpr2.compare(i) > 0; i++) {
        arg1 = Suc(arg1);
      }
      return arg1;
    }

    List<Expression> newDefCallArgs = new ArrayList<>(2);
    newDefCallArgs.add(arg1.accept(this, mode));
    Expression result = new FunCallExpression(Prelude.PLUS, expr.getSortArgument(), newDefCallArgs);
    ConCallExpression conCall2 = arg2.checkedCast(ConCallExpression.class);
    while (conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
      result = Suc(result);
      arg2 = conCall2.getDefCallArguments().get(0);
      conCall2 = arg2.checkedCast(ConCallExpression.class);
    }
    newDefCallArgs.add(mode == Mode.WHNF ? arg2 : arg2.accept(this, mode));
    return result;
  }

  private Expression normalizeMinus(DefCallExpression expr, Mode mode) {
    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    Expression arg1 = defCallArgs.get(0).accept(this, Mode.WHNF);
    Expression arg2 = defCallArgs.get(1);

    if (arg1.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr1 = arg1.cast(IntegerExpression.class);

      arg2 = arg2.accept(this, Mode.WHNF);
      if (arg2.isInstance(IntegerExpression.class)) {
        return intExpr1.minus(arg2.cast(IntegerExpression.class));
      }
      if (intExpr1.isZero()) {
        return Neg(mode == Mode.WHNF ? arg2 : arg2.accept(this, mode));
      }

      ConCallExpression conCall2 = arg2.checkedCast(ConCallExpression.class);
      while (!intExpr1.isZero() && conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
        intExpr1 = intExpr1.pred();
        arg2 = conCall2.getDefCallArguments().get(0);
        conCall2 = arg2.checkedCast(ConCallExpression.class);
      }

      if (conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
        return Neg(conCall2);
      }

      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(intExpr1);
      newDefCallArgs.add(mode == Mode.WHNF ? arg2 : arg2.accept(this, mode));
      return new FunCallExpression(Prelude.MINUS, expr.getSortArgument(), newDefCallArgs);
    }

    ConCallExpression conCall1 = arg1.checkedCast(ConCallExpression.class);
    if (conCall1 == null || conCall1.getDefinition() != Prelude.SUC) {
      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(mode == Mode.WHNF ? arg1 : arg1.accept(this, mode));
      newDefCallArgs.add(arg2.accept(this, mode));
      return new FunCallExpression(Prelude.MINUS, expr.getSortArgument(), newDefCallArgs);
    }

    arg2 = arg2.accept(this, Mode.WHNF);
    if (arg2.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
      while (!intExpr2.isZero() && conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
        intExpr2 = intExpr2.pred();
        arg1 = conCall1.getDefCallArguments().get(0);
        conCall1 = arg1.checkedCast(ConCallExpression.class);
      }

      if (conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
        return Pos(conCall1);
      }

      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(mode == Mode.WHNF ? arg1 : arg1.accept(this, mode));
      newDefCallArgs.add(intExpr2);
      return new FunCallExpression(Prelude.MINUS, expr.getSortArgument(), newDefCallArgs);
    }

    ConCallExpression conCall2 = arg2.checkedCast(ConCallExpression.class);
    while (conCall1 != null && conCall1.getDefinition() == Prelude.SUC && conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
      arg1 = conCall1.getDefCallArguments().get(0);
      conCall1 = arg1.checkedCast(ConCallExpression.class);
      arg2 = conCall2.getDefCallArguments().get(0);
      conCall2 = arg2.checkedCast(ConCallExpression.class);
    }

    List<Expression> newDefCallArgs = new ArrayList<>(2);
    newDefCallArgs.add(mode == Mode.WHNF ? arg1 : arg1.accept(this, mode));
    newDefCallArgs.add(mode == Mode.WHNF ? arg2 : arg2.accept(this, mode));
    return new FunCallExpression(Prelude.MINUS, expr.getSortArgument(), newDefCallArgs);
  }

  private Expression visitDefCall(DefCallExpression expr, LevelSubstitution levelSubstitution, Mode mode) {
    Definition definition = expr.getDefinition();
    if (definition == Prelude.COERCE || definition == Prelude.COERCE2) {
      LamExpression lamExpr = expr.getDefCallArguments().get(0).accept(this, Mode.WHNF).checkedCast(LamExpression.class);
      if (lamExpr != null) {
        Expression body = lamExpr.getParameters().getNext().hasNext() ? new LamExpression(lamExpr.getResultSort(), lamExpr.getParameters().getNext(), lamExpr.getBody()) : lamExpr.getBody();
        body = body.accept(this, Mode.WHNF);
        FunCallExpression funCall = body.checkedCast(FunCallExpression.class);
        boolean checkSigma = true;

        if (funCall != null && funCall.getDefinition() == Prelude.ISO && definition == Prelude.COERCE) {
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
                return AppExpression.make(isoArgs.get(2), expr.getDefCallArguments().get(1)).accept(this, mode);
              }
            }
          }
        }

        if (checkSigma && !NormalizingFindBindingVisitor.findBinding(body, lamExpr.getParameters())) {
          return expr.getDefCallArguments().get(definition == Prelude.COERCE ? 1 : 2).accept(this, mode);
        }
      }
    }

    if (definition == Prelude.MINUS) {
      return normalizeMinus(expr, mode);
    }
    if (definition == Prelude.PLUS) {
      return normalizePlus(expr, mode);
    }

    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    if (definition == Prelude.MUL || definition == Prelude.DIV_MOD || definition == Prelude.DIV || definition == Prelude.MOD) {
      Expression arg2 = defCallArgs.get(1).accept(this, Mode.WHNF);
      if (arg2.isInstance(IntegerExpression.class)) {
        IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
        if (intExpr2.isZero()) {
          if (definition == Prelude.MUL) {
            return intExpr2;
          } else if (definition == Prelude.DIV_MOD) {
            Expression result = defCallArgs.get(0).accept(this, mode);
            List<Expression> list = new ArrayList<>(2);
            list.add(result);
            list.add(result);
            return new TupleExpression(list, Prelude.DIV_MOD_TYPE);
          } else {
            return defCallArgs.get(0).accept(this, mode);
          }
        }

        if (intExpr2.isOne()) {
          if (definition == Prelude.DIV) {
            return defCallArgs.get(0).accept(this, mode);
          }
          if (definition == Prelude.MOD) {
            return Zero();
          }
          if (definition == Prelude.DIV_MOD) {
            List<Expression> list = new ArrayList<>(2);
            list.add(defCallArgs.get(0).accept(this, mode));
            list.add(Zero());
            return new TupleExpression(list, Prelude.DIV_MOD_TYPE);
          }
        }

        Expression arg1 = defCallArgs.get(0).accept(this, Mode.WHNF);
        if (arg1.isInstance(IntegerExpression.class)) {
          if (definition == Prelude.MUL) {
            return arg1.cast(IntegerExpression.class).mul(intExpr2);
          }
          if (definition == Prelude.DIV_MOD) {
            return arg1.cast(IntegerExpression.class).divMod(intExpr2);
          }
          if (definition == Prelude.DIV) {
            return arg1.cast(IntegerExpression.class).div(intExpr2);
          }
          if (definition == Prelude.MOD) {
            return arg1.cast(IntegerExpression.class).mod(intExpr2);
          }
          throw new IllegalStateException();
        }

        List<Expression> newDefCallArgs = new ArrayList<>(2);
        newDefCallArgs.add(arg1);
        newDefCallArgs.add(arg2);
        defCallArgs = newDefCallArgs;
      } else {
        List<Expression> newDefCallArgs = new ArrayList<>(2);
        newDefCallArgs.add(defCallArgs.get(0));
        newDefCallArgs.add(arg2);
        defCallArgs = newDefCallArgs;
      }
    }

    if (definition == Prelude.SUC) {
      Expression arg = defCallArgs.get(0).accept(this, mode);
      IntegerExpression intArg = arg.checkedCast(IntegerExpression.class);
      return intArg != null ? intArg.suc() : Suc(arg);
    }

    ElimTree elimTree;
    Body body = ((Function) definition).getBody();
    if (body instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) body;
      int i0 = defCallArgs.size() - elim.getCases().size();
      for (int i = i0; i < defCallArgs.size(); i++) {
        Pair<Expression, Expression> thisCase = elim.getCases().get(i - i0);
        if (thisCase.proj1 == null && thisCase.proj2 == null) {
          continue;
        }

        Expression arg = defCallArgs.get(i).accept(this, Mode.WHNF);
        ConCallExpression conCall = arg.checkedCast(ConCallExpression.class);
        if (conCall != null) {
          ExprSubstitution substitution = getDataTypeArgumentsSubstitution(expr);
          DependentLink link = elim.getParameters();
          for (int j = 0; j < defCallArgs.size(); j++) {
            if (j != i) {
              substitution.add(link, defCallArgs.get(j));
            }
            link = link.getNext();
          }

          Expression result;
          if (conCall.getDefinition() == Prelude.LEFT) {
            result = thisCase.proj1;
          } else if (conCall.getDefinition() == Prelude.RIGHT) {
            result = thisCase.proj2;
            if (definition == Prelude.COERCE2 && i == 1) { // Just a shortcut
              Expression arg3 = defCallArgs.get(3).accept(this, Mode.WHNF);
              if (arg3.isInstance(ConCallExpression.class) && arg3.cast(ConCallExpression.class).getDefinition() == Prelude.RIGHT) {
                return defCallArgs.get(2).accept(this, mode);
              } else {
                return applyDefCall(expr, mode);
              }
            }
          } else {
            throw new IllegalStateException();
          }
          if (result != null) {
            return result.subst(substitution).accept(this, mode);
          }
        }
      }
      elimTree = elim.getOtherwise();
    } else {
      elimTree = (mode == Mode.RNF || mode == Mode.RNF_EXP) && body instanceof LeafElimTree ? null : (ElimTree) body;
    }

    if (elimTree == null) {
      return applyDefCall(expr, mode);
    }

    Expression result = eval(elimTree, defCallArgs, getDataTypeArgumentsSubstitution(expr), levelSubstitution);

    TypecheckingOrderingListener.checkCanceled();

    return result == null ? applyDefCall(expr, mode) : result.accept(this, mode);
  }

  private Stack<Expression> makeStack(List<? extends Expression> arguments) {
    Stack<Expression> stack = new Stack<>();
    for (int i = arguments.size() - 1; i >= 0; i--) {
      stack.push(arguments.get(i));
    }
    return stack;
  }

  public Expression eval(ElimTree elimTree, List<? extends Expression> arguments, ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
    Stack<Expression> stack = makeStack(arguments);

    while (true) {
      for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
        substitution.add(link, stack.pop());
      }
      if (elimTree instanceof LeafElimTree) {
        return ((LeafElimTree) elimTree).getExpression().subst(substitution, levelSubstitution);
      }

      elimTree = updateStack(stack, (BranchElimTree) elimTree);
      if (elimTree == null) {
        return null;
      }
    }
  }

  public boolean doesEvaluate(ElimTree elimTree, List<? extends Expression> arguments, boolean might) {
    Stack<Expression> stack = makeStack(arguments);

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

      elimTree = updateStack(stack, (BranchElimTree) elimTree);
      if (elimTree == null) {
        if (!might) {
          return false;
        }
        Expression top = stack.peek();
        return !top.isInstance(ConCallExpression.class) && !top.isInstance(IntegerExpression.class);
      }
    }
  }

  private ElimTree updateStack(Stack<Expression> stack, BranchElimTree branchElimTree) {
    Expression argument = stack.peek().accept(this, Mode.WHNF);
    ConCallExpression conCall = argument.checkedCast(ConCallExpression.class);
    Constructor constructor = conCall == null ? null : conCall.getDefinition();
    if (constructor == null) {
      IntegerExpression intExpr = argument.checkedCast(IntegerExpression.class);
      if (intExpr != null) {
        constructor = intExpr.isZero() ? Prelude.ZERO : Prelude.SUC;
      }
    }

    ElimTree elimTree = constructor == null ? branchElimTree.getTupleChild() : branchElimTree.getChild(constructor);
    if (elimTree != null) {
      stack.pop();

      List<? extends Expression> args;
      if (constructor != null) {
        args = conCall != null
          ? conCall.getDefCallArguments()
          : constructor == Prelude.ZERO
            ? Collections.emptyList()
            : Collections.singletonList(argument.cast(IntegerExpression.class).pred());
      } else {
        BranchElimTree.TupleConstructor tupleConstructor = branchElimTree.getTupleConstructor();
        if (tupleConstructor == null) {
          return null;
        }

        args = tupleConstructor.getMatchedArguments(argument);
        if (args == null) {
          return null;
        }
      }

      for (int i = args.size() - 1; i >= 0; i--) {
        stack.push(args.get(i));
      }
    }

    return elimTree;
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
    if (expr.getDefinition() instanceof FunctionDefinition && ((FunctionDefinition) expr.getDefinition()).isLemma() ||
        expr.getDefinition() instanceof ClassField && ((ClassField) expr.getDefinition()).isProperty()) {
      return expr;
    }
    if (!expr.getDefinition().status().bodyIsOK()) {
      return applyDefCall(expr, mode);
    }

    if (expr instanceof FieldCallExpression) {
      Expression thisExpr = ((FieldCallExpression) expr).getArgument().accept(this, Mode.WHNF);
      if (!(thisExpr.getInferenceVariable() instanceof TypeClassInferenceVariable)) {
        Expression type = thisExpr.getType();
        ClassCallExpression classCall = type == null ? null : type.accept(this, Mode.WHNF).checkedCast(ClassCallExpression.class);
        if (classCall != null) {
          Expression impl = classCall.getImplementation((ClassField) expr.getDefinition(), thisExpr);
          if (impl != null) {
            return impl.accept(this, mode);
          }
        }
      }
      return FieldCallExpression.make((ClassField) expr.getDefinition(), expr.getSortArgument(), mode == Mode.NF ? thisExpr.accept(this, mode) : thisExpr);
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
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), fieldSet, expr.getSort(), expr.hasUniverses());
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Mode mode) {
    return (DataCallExpression) applyDefCall(expr, mode);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Mode mode) {
    if (mode == Mode.RNF || mode == Mode.RNF_EXP) {
      return expr;
    }
    if (expr.getBinding() instanceof EvaluatingBinding) {
      return ((EvaluatingBinding) expr.getBinding()).getExpression().accept(this, mode);
    }
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Mode mode) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, mode) : expr;
  }

  @Override
  public Expression visitLam(LamExpression expr, Mode mode) {
    if (mode == Mode.RNF || mode == Mode.RNF_EXP) {
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
    return mode == Mode.WHNF ? expr : new SigmaExpression(expr.getSort(), normalizeParameters(expr.getParameters(), mode, new ExprSubstitution()));
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
  public Expression visitLet(LetExpression let, Mode mode) {
    if (mode == Mode.RNF || mode == Mode.RNF_EXP) {
      ExprSubstitution substitution = new ExprSubstitution();
      List<LetClause> newClauses = new ArrayList<>(let.getClauses().size());
      for (LetClause clause : let.getClauses()) {
        LetClause newClause = new LetClause(clause.getName(), clause.getPattern(), clause.getExpression().accept(this, mode).subst(substitution));
        substitution.add(clause, new ReferenceExpression(newClause));
        newClauses.add(newClause);
      }
      return new LetExpression(let.isStrict(), newClauses, let.getExpression().accept(this, mode).subst(substitution));
    } else {
      return let.isStrict() ? let.getExpression().subst(let.getClausesSubstitution()).accept(this, mode) : let.getExpression().accept(this, mode);
    }
  }

  @Override
  public Expression visitCase(CaseExpression expr, Mode mode) {
    Expression result = eval(expr.getElimTree(), expr.getArguments(), new ExprSubstitution(), LevelSubstitution.EMPTY);
    if (result != null) {
      return result.accept(this, mode);
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
    return new CaseExpression(parameters, expr.getResultType().subst(substitution).accept(this, mode), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().subst(substitution).accept(this, mode), normalizeElimTree(expr.getElimTree(), mode), args);
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

  @Override
  public IntegerExpression visitInteger(IntegerExpression expr, Mode params) {
    return expr;
  }
}
