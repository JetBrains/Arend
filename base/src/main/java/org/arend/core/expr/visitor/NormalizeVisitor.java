package org.arend.core.expr.visitor;

import org.arend.core.constructor.IdpConstructor;
import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.pattern.Pattern;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.typechecking.computation.ComputationRunner;
import org.arend.util.Pair;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;

public class NormalizeVisitor extends BaseExpressionVisitor<NormalizationMode, Expression>  {
  public static final NormalizeVisitor INSTANCE = new NormalizeVisitor();

  private NormalizeVisitor() {
  }

  @Override
  public Expression visitApp(AppExpression expr, NormalizationMode mode) {
    Expression function = expr.getFunction().accept(this, mode);
    LamExpression lamExpr = function.cast(LamExpression.class);
    if (lamExpr != null) {
      return AppExpression.make(lamExpr, expr.getArgument(), expr.isExplicit()).accept(this, mode);
    } else {
      return AppExpression.make(function, mode == NormalizationMode.WHNF ? expr.getArgument() : expr.getArgument().accept(this, mode), expr.isExplicit());
    }
  }

  private Expression applyDefCall(DefCallExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF || expr.getDefCallArguments().isEmpty()) {
      return expr;
    }

    if (expr instanceof FieldCallExpression) {
      return FieldCallExpression.make((ClassField) expr.getDefinition(), expr.getSortArgument(), ((FieldCallExpression) expr).getArgument().accept(this, mode));
    }

    DependentLink link = expr.getDefinition().getParameters();
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(mode == NormalizationMode.RNF_EXP && !link.isExplicit() ? arg : arg.accept(this, mode));
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

  private Expression normalizePlus(DefCallExpression expr, NormalizationMode mode) {
    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    Expression arg1 = defCallArgs.get(0);
    Expression arg2 = defCallArgs.get(1).accept(this, NormalizationMode.WHNF);

    IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
    if (intExpr2 != null) {
      arg1 = arg1.accept(this, NormalizationMode.WHNF);
      IntegerExpression intExpr1 = arg1.cast(IntegerExpression.class);
      if (intExpr1 != null) {
        return intExpr1.plus(intExpr2);
      }
      if (intExpr2.isZero()) {
        return arg1.accept(this, mode);
      }

      if (mode != NormalizationMode.WHNF) {
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
    ConCallExpression conCall2 = arg2.cast(ConCallExpression.class);
    while (conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
      result = Suc(result);
      arg2 = conCall2.getDefCallArguments().get(0);
      conCall2 = arg2.cast(ConCallExpression.class);
    }
    newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(this, mode));
    return result;
  }

  private Expression normalizeMinus(DefCallExpression expr, NormalizationMode mode) {
    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    Expression arg1 = defCallArgs.get(0).accept(this, NormalizationMode.WHNF);
    Expression arg2 = defCallArgs.get(1);

    IntegerExpression intExpr1 = arg1.cast(IntegerExpression.class);
    if (intExpr1 != null) {
      arg2 = arg2.accept(this, NormalizationMode.WHNF);
      IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
      if (intExpr2 != null) {
        return intExpr1.minus(intExpr2);
      }
      if (intExpr1.isZero()) {
        return Neg(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(this, mode));
      }

      ConCallExpression conCall2 = arg2.cast(ConCallExpression.class);
      while (!intExpr1.isZero() && conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
        intExpr1 = intExpr1.pred();
        arg2 = conCall2.getDefCallArguments().get(0);
        conCall2 = arg2.cast(ConCallExpression.class);
      }

      if (conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
        return Neg(conCall2);
      }

      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(intExpr1);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(this, mode));
      return new FunCallExpression(Prelude.MINUS, expr.getSortArgument(), newDefCallArgs);
    }

    ConCallExpression conCall1 = arg1.cast(ConCallExpression.class);
    if (conCall1 == null || conCall1.getDefinition() != Prelude.SUC) {
      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(this, mode));
      newDefCallArgs.add(arg2.accept(this, mode));
      return new FunCallExpression(Prelude.MINUS, expr.getSortArgument(), newDefCallArgs);
    }

    arg2 = arg2.accept(this, NormalizationMode.WHNF);
    IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
    if (intExpr2 != null) {
      while (!intExpr2.isZero() && conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
        intExpr2 = intExpr2.pred();
        arg1 = conCall1.getDefCallArguments().get(0);
        conCall1 = arg1.cast(ConCallExpression.class);
      }

      if (conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
        return Pos(conCall1);
      }

      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(this, mode));
      newDefCallArgs.add(intExpr2);
      return new FunCallExpression(Prelude.MINUS, expr.getSortArgument(), newDefCallArgs);
    }

    ConCallExpression conCall2 = arg2.cast(ConCallExpression.class);
    while (conCall1 != null && conCall1.getDefinition() == Prelude.SUC && conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
      arg1 = conCall1.getDefCallArguments().get(0);
      conCall1 = arg1.cast(ConCallExpression.class);
      arg2 = conCall2.getDefCallArguments().get(0);
      conCall2 = arg2.cast(ConCallExpression.class);
    }

    List<Expression> newDefCallArgs = new ArrayList<>(2);
    newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(this, mode));
    newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(this, mode));
    return new FunCallExpression(Prelude.MINUS, expr.getSortArgument(), newDefCallArgs);
  }

  private Expression visitFunctionDefCall(DefCallExpression expr, NormalizationMode mode) {
    Definition definition = expr.getDefinition();
    if (definition == Prelude.COERCE || definition == Prelude.COERCE2) {
      LamExpression lamExpr = expr.getDefCallArguments().get(0).accept(this, NormalizationMode.WHNF).cast(LamExpression.class);
      if (lamExpr != null) {
        SingleDependentLink param = lamExpr.getParameters();
        Expression body = param.getNext().hasNext() ? new LamExpression(lamExpr.getResultSort(), param.getNext(), lamExpr.getBody()) : lamExpr.getBody();
        body = body.accept(this, NormalizationMode.WHNF);
        FunCallExpression funCall = body.cast(FunCallExpression.class);
        boolean checkSigma = true;

        if (funCall != null && funCall.getDefinition() == Prelude.ISO && definition == Prelude.COERCE) {
          List<? extends Expression> isoArgs = funCall.getDefCallArguments();
          ReferenceExpression refExpr = isoArgs.get(isoArgs.size() - 1).accept(this, NormalizationMode.WHNF).cast(ReferenceExpression.class);
          if (refExpr != null && refExpr.getBinding() == param) {
            checkSigma = false;
            ConCallExpression normedPtCon = expr.getDefCallArguments().get(2).accept(this, NormalizationMode.WHNF).cast(ConCallExpression.class);
            if (normedPtCon != null && normedPtCon.getDefinition() == Prelude.RIGHT) {
              boolean noFreeVar = true;
              for (int i = 0; i < isoArgs.size() - 1; i++) {
                if (NormalizingFindBindingVisitor.findBinding(isoArgs.get(i), param)) {
                  noFreeVar = false;
                  break;
                }
              }
              if (noFreeVar) {
                return AppExpression.make(isoArgs.get(2), expr.getDefCallArguments().get(1), true).accept(this, mode);
              }
              /* Stricter version of iso
              if (!NormalizingFindBindingVisitor.findBinding(isoArgs.get(0), param) && !NormalizingFindBindingVisitor.findBinding(isoArgs.get(1), param) && !NormalizingFindBindingVisitor.findBinding(isoArgs.get(2), param)) {
                return AppExpression.make(isoArgs.get(2), expr.getDefCallArguments().get(1), true).accept(this, mode);
              }
              */
            }
          }
        }

        if (checkSigma && !NormalizingFindBindingVisitor.findBinding(body, param)) {
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
      Expression arg2 = defCallArgs.get(1).accept(this, NormalizationMode.WHNF);
      IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
      if (intExpr2 != null) {
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

        Expression arg1 = defCallArgs.get(0).accept(this, NormalizationMode.WHNF);
        IntegerExpression intArg1 = arg1.cast(IntegerExpression.class);
        if (intArg1 != null) {
          if (definition == Prelude.MUL) {
            return intArg1.mul(intExpr2);
          }
          if (definition == Prelude.DIV_MOD) {
            return intArg1.divMod(intExpr2);
          }
          if (definition == Prelude.DIV) {
            return intArg1.div(intExpr2);
          }
          if (definition == Prelude.MOD) {
            return intArg1.mod(intExpr2);
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
      IntegerExpression intArg = arg.cast(IntegerExpression.class);
      return intArg != null ? intArg.suc() : Suc(arg);
    }

    Body body = ((Function) definition).getBody();
    if (body instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) body;
      int i0 = defCallArgs.size() - elim.getCases().size();
      for (int i = i0; i < defCallArgs.size(); i++) {
        Pair<Expression, Expression> thisCase = elim.getCases().get(i - i0);
        if (thisCase.proj1 == null && thisCase.proj2 == null) {
          continue;
        }

        Expression arg = defCallArgs.get(i).accept(this, NormalizationMode.WHNF);
        ConCallExpression conCall = arg.cast(ConCallExpression.class);
        if (conCall != null) {
          ExprSubstitution substitution = getDataTypeArgumentsSubstitution(expr);
          DependentLink link = definition.getParameters();
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
              ConCallExpression arg3 = defCallArgs.get(3).accept(this, NormalizationMode.WHNF).cast(ConCallExpression.class);
              if (arg3 != null && arg3.getDefinition() == Prelude.RIGHT) {
                return defCallArgs.get(2).accept(this, mode);
              } else {
                return applyDefCall(expr, mode);
              }
            }
          } else {
            throw new IllegalStateException();
          }
          if (result != null) {
            return result.subst(substitution, expr.getSortArgument().toLevelSubstitution()).accept(this, mode);
          }
        }
      }
      body = elim.getOtherwise();
    }

    Expression result;
    if (body instanceof Expression) {
      result = mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP ? null : ((Expression) body).subst(getDataTypeArgumentsSubstitution(expr).add(definition.getParameters(), defCallArgs), expr.getSortArgument().toLevelSubstitution());
    } else if (body instanceof ElimBody) {
      result = eval((ElimBody) body, defCallArgs, getDataTypeArgumentsSubstitution(expr), expr.getSortArgument().toLevelSubstitution());
    } else {
      assert body == null;
      result = null;
    }

    ComputationRunner.checkCanceled();

    return result == null ? applyDefCall(expr, mode) : result.accept(this, mode);
  }

  public Stack<Expression> makeStack(List<? extends Expression> arguments) {
    Stack<Expression> stack = new Stack<>();
    for (int i = arguments.size() - 1; i >= 0; i--) {
      stack.push(arguments.get(i));
    }
    return stack;
  }

  public Expression eval(Expression expr) {
    if (expr instanceof FunCallExpression) {
      FunCallExpression funCall = (FunCallExpression) expr;
      Body body = funCall.getDefinition().getActualBody();
      if (body instanceof Expression) {
        return ((Expression) body).subst(new ExprSubstitution().add(funCall.getDefinition().getParameters(), funCall.getDefCallArguments()), funCall.getSortArgument().toLevelSubstitution());
      } else if (body instanceof ElimBody) {
        return eval((ElimBody) body, funCall.getDefCallArguments(), getDataTypeArgumentsSubstitution(funCall), funCall.getSortArgument().toLevelSubstitution());
      } else {
        return null;
      }
    } else if (expr instanceof CaseExpression) {
      return eval(((CaseExpression) expr).getElimBody(), ((CaseExpression) expr).getArguments(), new ExprSubstitution(), LevelSubstitution.EMPTY);
    } else {
      return null;
    }
  }

  public Expression eval(ElimBody elimBody, List<? extends Expression> arguments, ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
    Stack<Expression> stack = makeStack(arguments);
    List<Expression> result = new ArrayList<>();

    ElimTree elimTree = elimBody.getElimTree();
    while (true) {
      for (int i = 0; i < elimTree.getSkip(); i++) {
        result.add(stack.pop());
      }

      if (elimTree instanceof LeafElimTree) {
        LeafElimTree leafElimTree = (LeafElimTree) elimTree;
        ElimClause<Pattern> clause = elimBody.getClauses().get(leafElimTree.getClauseIndex());
        int i = 0;
        for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext(), i++) {
          substitution.add(link, result.get(leafElimTree.getArgumentIndex(i)));
        }
        return Objects.requireNonNull(clause.getExpression()).subst(substitution, levelSubstitution);
      }

      elimTree = updateStack(stack, result, (BranchElimTree) elimTree);
      if (elimTree == null) {
        return null;
      }
    }
  }

  public boolean doesEvaluate(ElimTree elimTree, List<? extends Expression> arguments, boolean might) {
    Stack<Expression> stack = makeStack(arguments);

    while (true) {
      for (int i = 0; i < elimTree.getSkip(); i++) {
        if (stack.isEmpty()) {
          return true;
        }
        stack.pop();
      }
      if (elimTree instanceof LeafElimTree || stack.isEmpty()) {
        return true;
      }

      elimTree = updateStack(stack, null, (BranchElimTree) elimTree);
      if (elimTree == null) {
        if (!might) {
          return false;
        }
        Expression top = stack.peek().getUnderlyingExpression();
        return !(top instanceof ConCallExpression || top instanceof IntegerExpression);
      }
    }
  }

  private ElimTree updateStack(Stack<Expression> stack, List<Expression> argList, BranchElimTree branchElimTree) {
    Expression argument = stack.peek().accept(this, NormalizationMode.WHNF); // TODO[idp]: Normalize only until idp
    ConCallExpression conCall = argument.cast(ConCallExpression.class);
    Constructor constructor = conCall == null ? null : conCall.getDefinition();
    IntegerExpression intExpr = constructor == null ? argument.cast(IntegerExpression.class) : null;
    if (intExpr != null) {
      constructor = intExpr.isZero() ? Prelude.ZERO : Prelude.SUC;
    }

    ElimTree elimTree = constructor == null ? branchElimTree.getSingleConstructorChild() : branchElimTree.getChild(constructor);
    if (elimTree == null && constructor == Prelude.PATH_CON && branchElimTree.getSingleConstructorKey() instanceof IdpConstructor) {
      elimTree = branchElimTree.getSingleConstructorChild();
      constructor = null;
    }
    if (elimTree != null) {
      stack.pop();
      if (argList != null && branchElimTree.keepConCall()) {
        argList.add(argument);
      }

      List<? extends Expression> args;
      if (constructor != null) {
        args = conCall != null
          ? conCall.getDefCallArguments()
          : constructor == Prelude.ZERO
            ? Collections.emptyList()
            : Collections.singletonList(intExpr.pred());
      } else {
        SingleConstructor singleConstructor = branchElimTree.getSingleConstructorKey();
        if (singleConstructor == null) {
          return null;
        }

        args = singleConstructor.getMatchedArguments(argument, true);
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
  public Expression visitDefCall(DefCallExpression expr, NormalizationMode mode) {
    if (expr.getDefinition() instanceof FunctionDefinition && ((FunctionDefinition) expr.getDefinition()).isSFunc() ||
        expr.getDefinition() instanceof ClassField && ((ClassField) expr.getDefinition()).isProperty() ||
        expr.getDefinition().status() != Definition.TypeCheckingStatus.NO_ERRORS && expr.getDefinition() instanceof Function && ((Function) expr.getDefinition()).getBody() == null) {
      return applyDefCall(expr, mode);
    }

    if (expr instanceof FieldCallExpression) {
      Expression thisExpr = ((FieldCallExpression) expr).getArgument().accept(this, NormalizationMode.WHNF);
      if (!(thisExpr.getInferenceVariable() instanceof TypeClassInferenceVariable)) {
        Expression type = thisExpr.getType();
        ClassCallExpression classCall = type == null ? null : type.accept(this, NormalizationMode.WHNF).cast(ClassCallExpression.class);
        if (classCall != null) {
          Expression impl = classCall.getImplementation((ClassField) expr.getDefinition(), thisExpr);
          if (impl != null) {
            return impl.accept(this, mode);
          }
        }
      }
      return FieldCallExpression.make((ClassField) expr.getDefinition(), expr.getSortArgument(), mode == NormalizationMode.NF ? thisExpr.accept(this, mode) : thisExpr);
    }

    return expr.getDefinition() instanceof Function ? visitFunctionDefCall(expr, mode) : applyDefCall(expr, mode);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF) return expr;

    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassCallExpression result = new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), fieldSet, expr.getSort(), expr.getUniverseKind());
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      fieldSet.put(entry.getKey(), entry.getValue().accept(this, mode).subst(expr.getThisBinding(), new ReferenceExpression(result.getThisBinding())));
    }
    return result;
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, NormalizationMode mode) {
    return (DataCallExpression) applyDefCall(expr, mode);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP) {
      return expr;
    }
    if (expr.getBinding() instanceof EvaluatingBinding) {
      return ((EvaluatingBinding) expr.getBinding()).getExpression().accept(this, mode);
    }
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, NormalizationMode mode) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, mode) : expr;
  }

  @Override
  public Expression visitSubst(SubstExpression expr, NormalizationMode mode) {
    return expr.getSubstExpression().accept(this, mode);
  }

  @Override
  public Expression visitLam(LamExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = normalizeSingleParameters(expr.getParameters(), mode, substitution);
      return new LamExpression(expr.getResultSort(), link, expr.getBody().subst(substitution).accept(this, mode));
    }
    if (mode == NormalizationMode.NF) {
      return new LamExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public PiExpression visitPi(PiExpression expr, NormalizationMode mode) {
    if (mode != NormalizationMode.WHNF) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = normalizeSingleParameters(expr.getParameters(), mode, substitution);
      return new PiExpression(expr.getResultSort(), link, expr.getCodomain().subst(substitution).accept(this, mode));
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, NormalizationMode mode) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF || expr.getExpression() == null ? expr : expr.replaceExpression(expr.getExpression().accept(this, mode));
  }

  @Override
  public Expression visitTuple(TupleExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, mode));
    }
    return new TupleExpression(fields, expr.getSigmaType());
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF ? expr : new SigmaExpression(expr.getSort(), normalizeParameters(expr.getParameters(), mode, new ExprSubstitution()));
  }

  private DependentLink normalizeParameters(DependentLink parameters, NormalizationMode mode, ExprSubstitution substitution) {
    DependentLink link = DependentLink.Helper.subst(parameters, substitution);
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      link1 = link1.getNextTyped(null);
      link1.setType(link1.getType().normalize(mode));
    }
    return link;
  }

  private SingleDependentLink normalizeSingleParameters(SingleDependentLink parameters, NormalizationMode mode, ExprSubstitution substitution) {
    SingleDependentLink link = DependentLink.Helper.subst(parameters, substitution);
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      link1 = link1.getNextTyped(null);
      link1.setType(link1.getType().normalize(mode));
    }
    return link;
  }

  @Override
  public Expression visitProj(ProjExpression expr, NormalizationMode mode) {
    Expression newExpr = expr.getExpression().accept(this, NormalizationMode.WHNF);
    TupleExpression exprNorm = newExpr.cast(TupleExpression.class);
    if (exprNorm != null) {
      return exprNorm.getFields().get(expr.getField()).accept(this, mode);
    } else {
      return mode == NormalizationMode.WHNF ? ProjExpression.make(newExpr, expr.getField()) : ProjExpression.make(expr.getExpression().accept(this, mode), expr.getField());
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF ? expr : new NewExpression(expr.getRenewExpression() == null ? null : expr.getRenewExpression().accept(this, mode), visitClassCall(expr.getClassCall(), mode));
  }

  @Override
  public Expression visitLet(LetExpression let, NormalizationMode mode) {
    if (mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP) {
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
  public Expression visitCase(CaseExpression expr, NormalizationMode mode) {
    if (!expr.isSCase()) {
      Expression result = eval(expr.getElimBody(), expr.getArguments(), new ExprSubstitution(), LevelSubstitution.EMPTY);
      if (result != null) {
        return result.accept(this, mode);
      }
    }
    if (mode == NormalizationMode.WHNF) {
      return expr;
    }

    List<Expression> args = new ArrayList<>(expr.getArguments().size());
    for (Expression arg : expr.getArguments()) {
      args.add(arg.accept(this, mode));
    }
    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink parameters = normalizeParameters(expr.getParameters(), mode, substitution);
    return new CaseExpression(expr.isSCase(), parameters, expr.getResultType().subst(substitution).accept(this, mode), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().subst(substitution).accept(this, mode), normalizeElimBody(expr.getElimBody(), mode), args);
  }

  private ElimBody normalizeElimBody(ElimBody elimBody, NormalizationMode mode) {
    List<ElimClause<Pattern>> clauses = new ArrayList<>();
    for (ElimClause<Pattern> clause : elimBody.getClauses()) {
      ExprSubstitution substitution = new ExprSubstitution();
      DependentLink parameters = normalizeParameters(clause.getParameters(), mode, substitution);
      clauses.add(new ElimClause<>(Pattern.replaceBindings(clause.getPatterns(), parameters), clause.getExpression() == null ? null : clause.getExpression().subst(substitution).accept(this, mode)));
    }
    return new ElimBody(clauses, elimBody.getElimTree());
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.NF ? new OfTypeExpression(expr.getExpression().accept(this, mode), expr.getTypeOf()) : expr.getExpression().accept(this, mode);
  }

  @Override
  public IntegerExpression visitInteger(IntegerExpression expr, NormalizationMode params) {
    return expr;
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF ? expr : new PEvalExpression(expr.getExpression().accept(this, mode));
  }
}
