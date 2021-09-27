package org.arend.core.expr.visitor;

import org.arend.core.constructor.ArrayConstructor;
import org.arend.core.constructor.IdpConstructor;
import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.typechecking.computation.ComputationRunner;
import org.arend.util.Pair;
import org.arend.util.SingletonList;

import java.math.BigInteger;
import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;

public class NormalizeVisitor extends ExpressionTransformer<NormalizationMode>  {
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
      return AppExpression.make(function, mode == NormalizationMode.WHNF || mode == NormalizationMode.ENF ? expr.getArgument() : expr.getArgument().accept(this, mode), expr.isExplicit());
    }
  }

  private Expression applyDefCall(DefCallExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF || mode == NormalizationMode.ENF || expr.getDefCallArguments().isEmpty()) {
      return expr;
    }

    if (expr instanceof FieldCallExpression) {
      return FieldCallExpression.make((ClassField) expr.getDefinition(), ((FieldCallExpression) expr).getArgument().accept(this, mode));
    }

    assert expr instanceof LeveledDefCallExpression;
    Levels levels = ((LeveledDefCallExpression) expr).getLevels();

    DependentLink link = expr.getDefinition().getParameters();
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(mode == NormalizationMode.RNF_EXP && !link.isExplicit() ? arg : arg.accept(this, mode));
      if (link.hasNext()) {
        link = link.getNext();
      }
    }

    if (expr instanceof FunCallExpression) {
      return FunCallExpression.make((FunctionDefinition) expr.getDefinition(), levels, args);
    }

    if (expr instanceof DataCallExpression) {
      return new DataCallExpression((DataDefinition) expr.getDefinition(), levels, args);
    }

    if (expr instanceof ConCallExpression) {
      return ConCallExpression.make((Constructor) expr.getDefinition(), levels, ((ConCallExpression) expr).getDataTypeArguments(), args);
    }

    throw new IllegalStateException();
  }

  private Expression normalizePlus(FunCallExpression expr, NormalizationMode mode) {
    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    Expression arg1 = defCallArgs.get(0).accept(this, mode);
    Expression arg2 = defCallArgs.get(1).accept(this, mode);

    if (arg1 instanceof IntegerExpression) {
      IntegerExpression intExpr1 = (IntegerExpression) arg1;
      if (arg2 instanceof IntegerExpression) {
        return intExpr1.plus((IntegerExpression) arg2);
      }
      for (int i = 0; intExpr1.compare(i) > 0; i++) {
        arg2 = Suc(arg2);
      }
      return arg2;
    }

    if (arg2 instanceof IntegerExpression) {
      IntegerExpression intExpr2 = (IntegerExpression) arg2;
      for (int i = 0; intExpr2.compare(i) > 0; i++) {
        arg1 = Suc(arg1);
      }
      return arg1;
    }

    List<Expression> newDefCallArgs = new ArrayList<>(2);
    Expression result = FunCallExpression.make(Prelude.PLUS, expr.getLevels(), newDefCallArgs);
    result = addSucs(arg1, newDefCallArgs, result);
    result = addSucs(arg2, newDefCallArgs, result);
    return result;
  }

  private Expression addSucs(Expression arg1, List<Expression> defCallArgs, Expression result) {
    ConCallExpression conCall1 = arg1.cast(ConCallExpression.class);
    while (conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
      result = Suc(result);
      arg1 = conCall1.getDefCallArguments().get(0).accept(this, NormalizationMode.WHNF);
      conCall1 = arg1.cast(ConCallExpression.class);
    }
    defCallArgs.add(arg1);
    return result;
  }

  private Expression normalizeMinus(FunCallExpression expr, NormalizationMode mode) {
    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    Expression arg1 = defCallArgs.get(0).accept(this, NormalizationMode.WHNF);
    Expression arg2 = defCallArgs.get(1);

    IntegerExpression intExpr1 = arg1.cast(IntegerExpression.class);
    if (intExpr1 != null) {
      if (intExpr1.isZero()) {
        Expression result = arg2.accept(this, mode);
        IntegerExpression intResult = result.cast(IntegerExpression.class);
        return intResult != null && intResult.isZero() ? Pos(intResult) : Neg(result);
      }
      arg2 = arg2.accept(this, NormalizationMode.WHNF);
      IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
      if (intExpr2 != null) {
        return intExpr1.minus(intExpr2);
      }

      ConCallExpression conCall2 = arg2.cast(ConCallExpression.class);
      while (!intExpr1.isZero() && conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
        intExpr1 = intExpr1.pred();
        arg2 = conCall2.getDefCallArguments().get(0).accept(this, NormalizationMode.WHNF);
        conCall2 = arg2.cast(ConCallExpression.class);
      }

      if (conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
        return Neg(conCall2);
      }

      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(intExpr1);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(this, mode));
      return FunCallExpression.make(Prelude.MINUS, expr.getLevels(), newDefCallArgs);
    }

    arg2 = arg2.accept(this, NormalizationMode.WHNF);
    if (arg2 instanceof IntegerExpression && ((IntegerExpression) arg2).isZero()) {
      return Pos(arg1);
    }

    ConCallExpression conCall1 = arg1.cast(ConCallExpression.class);
    if (conCall1 == null || conCall1.getDefinition() != Prelude.SUC) {
      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(this, mode));
      newDefCallArgs.add(arg2.accept(this, mode));
      return FunCallExpression.make(Prelude.MINUS, expr.getLevels(), newDefCallArgs);
    }

    IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
    if (intExpr2 != null) {
      while (!intExpr2.isZero() && conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
        intExpr2 = intExpr2.pred();
        arg1 = conCall1.getDefCallArguments().get(0).accept(this, NormalizationMode.WHNF);
        conCall1 = arg1.cast(ConCallExpression.class);
      }

      if (conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
        return Pos(conCall1);
      }

      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(this, mode));
      newDefCallArgs.add(intExpr2);
      return FunCallExpression.make(Prelude.MINUS, expr.getLevels(), newDefCallArgs);
    }

    ConCallExpression conCall2 = arg2.cast(ConCallExpression.class);
    while (conCall1 != null && conCall1.getDefinition() == Prelude.SUC && conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
      arg1 = conCall1.getDefCallArguments().get(0).accept(this, NormalizationMode.WHNF);
      conCall1 = arg1.cast(ConCallExpression.class);
      arg2 = conCall2.getDefCallArguments().get(0).accept(this, NormalizationMode.WHNF);
      conCall2 = arg2.cast(ConCallExpression.class);
    }

    List<Expression> newDefCallArgs = new ArrayList<>(2);
    newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(this, mode));
    newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(this, mode));
    return FunCallExpression.make(Prelude.MINUS, expr.getLevels(), newDefCallArgs);
  }

  private Expression visitFunctionDefCall(LeveledDefCallExpression expr, NormalizationMode mode) {
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
      return normalizeMinus((FunCallExpression) expr, mode);
    }
    if (definition == Prelude.PLUS) {
      return normalizePlus((FunCallExpression) expr, mode);
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
            return new TupleExpression(list, finDivModType(Suc(result)));
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
            return new TupleExpression(list, ExpressionFactory.finDivModType(new SmallIntegerExpression(1)));
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
        Expression arg1 = defCallArgs.get(0);
        if (definition == Prelude.DIV_MOD || definition == Prelude.DIV || definition == Prelude.MOD) {
          arg1 = arg1.accept(this, mode);
          if (arg1 instanceof IntegerExpression && ((IntegerExpression) arg1).isZero()) {
            if (definition == Prelude.DIV_MOD) {
              List<Expression> list = new ArrayList<>(2);
              list.add(arg1);
              list.add(arg1);
              return new TupleExpression(list, finDivModType(new SmallIntegerExpression(1)));
            } else {
              return arg1;
            }
          }
        }
        List<Expression> newDefCallArgs = new ArrayList<>(2);
        newDefCallArgs.add(arg1);
        newDefCallArgs.add(arg2);
        defCallArgs = newDefCallArgs;
      }
    }

    if (definition == Prelude.ARRAY_INDEX) {
      Expression arg = defCallArgs.get(0).normalize(NormalizationMode.WHNF);
      if (arg instanceof ArrayExpression) {
        var pair = getNumber(defCallArgs.get(1));
        if (pair.proj1 != null) {
          ArrayExpression array = (ArrayExpression) arg;
          BigInteger s = BigInteger.valueOf(array.getElements().size());
          if (pair.proj1.compareTo(s) < 0) {
            return array.getElements().get(pair.proj1.intValue()).accept(this, mode);
          }
          if (array.getTail() != null) {
            Expression indexArg = pair.proj2;
            BigInteger b = pair.proj1.subtract(s);
            if (indexArg instanceof IntegerExpression) {
              indexArg = ((IntegerExpression) indexArg).plus(new BigIntegerExpression(b));
            } else {
              for (BigInteger i = BigInteger.ZERO; i.compareTo(b) < 0; i = i.add(BigInteger.ONE)) {
                indexArg = Suc(indexArg);
              }
            }
            return FunCallExpression.make(Prelude.ARRAY_INDEX, expr.getLevels(), Arrays.asList(array.getTail(), indexArg)).accept(this, mode);
          }
        }
      } else {
        Expression type = arg.getType().normalize(NormalizationMode.WHNF);
        if (type instanceof ClassCallExpression) {
          Expression at = ((ClassCallExpression) type).getImplementationHere(Prelude.ARRAY_AT, arg);
          if (at != null) {
            return AppExpression.make(at, defCallArgs.get(1), true).accept(this, mode);
          }
        }
      }

      return applyDefCall(expr, mode);
    }

    Expression result = visitBody(((Function) definition).getBody(), defCallArgs, expr, mode);
    return result == null ? applyDefCall(expr, mode) : result;
  }

  private Expression visitBody(Body body, List<? extends Expression> defCallArgs, LeveledDefCallExpression expr, NormalizationMode mode) {
    ComputationRunner.checkCanceled();
    Definition definition = expr.getDefinition();

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
            return result.subst(substitution, expr.getLevelSubstitution()).accept(this, mode);
          }
        }
      }
      body = elim.getOtherwise();
    }

    if (body == null || body instanceof Expression && (mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP)) {
      return null;
    }

    if (definition.hasStrictParameters()) {
      List<Expression> normDefCalls = new ArrayList<>(defCallArgs.size());
      for (int i = 0; i < defCallArgs.size(); i++) {
        normDefCalls.add(definition.isStrict(i) ? defCallArgs.get(i).accept(this, NormalizationMode.WHNF) : defCallArgs.get(i));
      }
      defCallArgs = normDefCalls;
    }

    if (body instanceof Expression) {
      ExprSubstitution substitution = addArguments(getDataTypeArgumentsSubstitution(expr), defCallArgs, definition);
      LevelSubstitution levelSubstitution = expr.getLevelSubstitution();
      if (body instanceof CaseExpression && !((CaseExpression) body).isSCase()) {
        CaseExpression caseExpr = (CaseExpression) body;
        List<Expression> args = new ArrayList<>(caseExpr.getArguments().size());
        for (Expression arg : caseExpr.getArguments()) {
          args.add(arg.subst(substitution, levelSubstitution));
        }
        Expression result = eval(caseExpr.getElimBody(), args, substitution, levelSubstitution, SubstExpression.make(caseExpr, substitution, levelSubstitution), mode);
        return result == null ? caseExpr.subst(substitution, levelSubstitution) : result;
      } else {
        return ((Expression) body).subst(substitution, levelSubstitution).accept(this, mode);
      }
    } else if (body instanceof ElimBody) {
      return eval((ElimBody) body, defCallArgs, getDataTypeArgumentsSubstitution(expr), expr.getLevelSubstitution(), expr, mode);
    } else {
      throw new IllegalStateException();
    }
  }

  private ExprSubstitution addArguments(ExprSubstitution substitution, List<? extends Expression> args, Definition definition) {
    DependentLink link = definition.getParameters();
    for (int i = 0; i < args.size(); i++) {
      substitution.add(link, definition.isStrict(i) ? args.get(i).accept(this, NormalizationMode.WHNF) : args.get(i));
      link = link.getNext();
    }
    return substitution;
  }

  public Deque<Expression> makeStack(List<? extends Expression> arguments) {
    Deque<Expression> stack = new ArrayDeque<>();
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
        return ((Expression) body).subst(new ExprSubstitution().add(funCall.getDefinition().getParameters(), funCall.getDefCallArguments()), funCall.getLevelSubstitution());
      } else if (body instanceof ElimBody) {
        return eval((ElimBody) body, funCall.getDefCallArguments(), new ExprSubstitution(), funCall.getLevelSubstitution(), expr, null);
      } else {
        return null;
      }
    } else if (expr instanceof CaseExpression) {
      return eval(((CaseExpression) expr).getElimBody(), ((CaseExpression) expr).getArguments(), new ExprSubstitution(), LevelSubstitution.EMPTY, expr, null);
    } else {
      return null;
    }
  }

  private static boolean isBlocked(FunctionDefinition def) {
    return def.isSFunc() || def == Prelude.PLUS || def == Prelude.MUL || def == Prelude.MINUS || def == Prelude.DIV || def == Prelude.MOD || def == Prelude.DIV_MOD || def == Prelude.COERCE || def == Prelude.COERCE2;
  }

  public Expression eval(ElimBody elimBody, List<? extends Expression> arguments, ExprSubstitution substitution, LevelSubstitution levelSubstitution, Expression resultExpr, NormalizationMode mode) {
    Deque<Expression> stack = makeStack(arguments);
    List<Expression> argList = new ArrayList<>();
    Expression result = null;

    List<Expression> conArgs = null;
    int recursiveParam = -1;
    int sucs = 0;

    ElimTree elimTree = elimBody.getElimTree();
    while (true) {
      for (int i = 0; i < elimTree.getSkip(); i++) {
        argList.add(stack.pop());
      }

      if (elimTree instanceof LeafElimTree) {
        LeafElimTree leafElimTree = (LeafElimTree) elimTree;
        ElimClause<Pattern> clause = elimBody.getClauses().get(leafElimTree.getClauseIndex());
        int i = 0;
        for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext(), i++) {
          substitution.add(link, argList.get(leafElimTree.getArgumentIndex(i)));
        }
        resultExpr = Objects.requireNonNull(clause.getExpression());

        if (mode == null) {
          return resultExpr.subst(substitution, levelSubstitution);
        }

        while (true) {
          if (mode != NormalizationMode.RNF && mode != NormalizationMode.RNF_EXP && resultExpr instanceof LetExpression) {
            LetExpression let = (LetExpression) resultExpr;
            if (let.isStrict()) {
              for (HaveClause letClause : let.getClauses()) {
                substitution.add(letClause, LetExpression.normalizeClauseExpression(letClause.getPattern(), letClause.getExpression().subst(substitution, levelSubstitution)));
              }
            } else {
              for (HaveClause letClause : let.getClauses()) {
                substitution.add(letClause, letClause instanceof LetClause
                  ? new ReferenceExpression(LetClause.make(true, letClause.getName(), letClause.getPattern(), letClause.getExpression().subst(substitution, levelSubstitution)))
                  : LetExpression.normalizeClauseExpression(letClause.getPattern(), letClause.getExpression().subst(substitution, levelSubstitution)));
              }
            }
            resultExpr = let.getExpression();
          } else if (mode != NormalizationMode.WHNF && resultExpr instanceof ConCallExpression) {
            ConCallExpression conCall = (ConCallExpression) resultExpr;
            if (conCall.getDefinition() == Prelude.SUC) {
              sucs++;
              resultExpr = conCall.getDefCallArguments().get(0);
            } else if (conCall.getDefinition().getRecursiveParameter() >= 0) {
              int recParam = conCall.getDefinition().getRecursiveParameter();
              List<Expression> newDataTypeArgs;
              List<Expression> newConArgs;
              if (substitution.isEmpty() && levelSubstitution.isEmpty()) {
                newDataTypeArgs = conCall.getDataTypeArguments();
                newConArgs = new ArrayList<>(conCall.getDefCallArguments());
              } else {
                newDataTypeArgs = new ArrayList<>(conCall.getDataTypeArguments().size());
                for (Expression arg : conCall.getDataTypeArguments()) {
                  newDataTypeArgs.add(arg.subst(substitution, levelSubstitution));
                }
                newConArgs = new ArrayList<>(conCall.getDefCallArguments().size());
                for (int j = 0; j < conCall.getDefCallArguments().size(); j++) {
                  if (j != recParam) {
                    newConArgs.add(conCall.getDefCallArguments().get(j).subst(substitution, levelSubstitution));
                  } else {
                    newConArgs.add(conCall.getDefCallArguments().get(j));
                  }
                }
              }
              Expression newExpr = ConCallExpression.make(conCall.getDefinition(), conCall.getLevels().subst(levelSubstitution), newDataTypeArgs, newConArgs);
              if (conArgs == null) {
                result = newExpr;
              } else {
                conArgs.set(recursiveParam, newExpr);
              }
              conArgs = newConArgs;
              recursiveParam = recParam;
              resultExpr = conArgs.get(recursiveParam);
            } else {
              break;
            }
          } else if (resultExpr instanceof FunCallExpression && ((FunCallExpression) resultExpr).getDefinition().getBody() instanceof Expression) {
            FunCallExpression funCall = (FunCallExpression) resultExpr;
            resultExpr = Objects.requireNonNull((Expression) funCall.getDefinition().getBody()).subst(addArguments(new ExprSubstitution(), funCall.getDefCallArguments(), funCall.getDefinition()), funCall.getLevelSubstitution());
          } else if (resultExpr instanceof ReferenceExpression && ((ReferenceExpression) resultExpr).getBinding() instanceof EvaluatingBinding) {
            resultExpr = ((EvaluatingBinding) ((ReferenceExpression) resultExpr).getBinding()).getExpression();
          } else if (resultExpr instanceof SubstExpression) {
            Expression expr = ((SubstExpression) resultExpr).eval();
            if (resultExpr == expr) {
              break;
            }
            resultExpr = expr;
          } else if (resultExpr instanceof ReferenceExpression) {
            Binding binding = ((ReferenceExpression) resultExpr).getBinding();
            Expression expr = substitution.get(binding);
            if (!substitution.isEmpty()) {
              substitution = new ExprSubstitution();
            }
            levelSubstitution = LevelSubstitution.EMPTY;
            if (expr != null) {
              resultExpr = expr;
            } else {
              break;
            }
          } else {
            break;
          }
        }

        if (resultExpr instanceof FunCallExpression && ((FunCallExpression) resultExpr).getDefinition().getBody() instanceof ElimBody && !isBlocked(((FunCallExpression) resultExpr).getDefinition()) || resultExpr instanceof CaseExpression && !((CaseExpression) resultExpr).isSCase()) {
          FunCallExpression funCall = resultExpr instanceof FunCallExpression ? (FunCallExpression) resultExpr : null;
          elimBody = funCall != null ? (ElimBody) funCall.getDefinition().getBody() : ((CaseExpression) resultExpr).getElimBody();
          assert elimBody != null;
          elimTree = elimBody.getElimTree();
          argList.clear();
          stack.clear();

          ComputationRunner.checkCanceled();

          List<? extends Expression> args = funCall != null ? funCall.getDefCallArguments() : ((CaseExpression) resultExpr).getArguments();
          for (int j = args.size() - 1; j >= 0; j--) {
            stack.push(resultExpr instanceof CaseExpression ? args.get(j).subst(substitution, levelSubstitution) : funCall.getDefinition().isStrict(j) ? args.get(j).subst(substitution, levelSubstitution).accept(this, NormalizationMode.WHNF) : SubstExpression.make(args.get(j), substitution, levelSubstitution));
          }
          resultExpr = SubstExpression.make(resultExpr, substitution, levelSubstitution);
          if (funCall != null) {
            substitution = new ExprSubstitution();
            levelSubstitution = funCall.getLevelSubstitution().subst(levelSubstitution);
          }
          continue;
        }

        resultExpr = resultExpr.subst(substitution, levelSubstitution);
        if (result == null) {
          result = resultExpr;
        } else {
          conArgs.set(recursiveParam, resultExpr);
        }
        return result == null ? null : addSucs(result, sucs).accept(this, mode);
      }

      elimTree = updateStack(stack, argList, (BranchElimTree) elimTree);
      if (elimTree == null) {
        if (resultExpr instanceof SubstExpression) {
          resultExpr = ((SubstExpression) resultExpr).eval();
        }
        if (mode == NormalizationMode.WHNF && resultExpr instanceof FunCallExpression && ((FunCallExpression) resultExpr).getDefinition().getBody() instanceof ElimBody) {
          FunCallExpression funCall = (FunCallExpression) resultExpr;
          List<Expression> newArgs = ((ElimBody) Objects.requireNonNull(funCall.getDefinition().getBody())).getElimTree().normalizeArguments(funCall.getDefCallArguments());
          resultExpr = FunCallExpression.make(funCall.getDefinition(), funCall.getLevels(), newArgs);
        }
        if (result == null) {
          result = resultExpr;
        } else {
          conArgs.set(recursiveParam, resultExpr);
        }
        result = addSucs(result, sucs);
        return mode == NormalizationMode.WHNF || mode == null || !(result instanceof DefCallExpression) ? (result != null && mode != NormalizationMode.WHNF ? result.accept(this, mode) : result) : applyDefCall((DefCallExpression) result, mode);
      }
    }
  }

  private Expression addSucs(Expression result, int sucs) {
    if (sucs > 0) {
      if (result instanceof IntegerExpression) {
        return ((IntegerExpression) result).plus(sucs);
      }
      for (int j = 0; j < sucs; j++) {
        result = Suc(result);
      }
    }
    return result;
  }

  public boolean doesEvaluate(ElimTree elimTree, List<? extends Expression> arguments, boolean might) {
    Deque<Expression> stack = makeStack(arguments);

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
        Expression top = stack.isEmpty() ? null : TypeCoerceExpression.unfoldExpression(stack.peek());
        return !(top instanceof ConCallExpression || top instanceof IntegerExpression || top instanceof ArrayExpression);
      }
    }
  }

  private ElimTree updateStack(Deque<Expression> stack, List<Expression> argList, BranchElimTree branchElimTree) {
    Expression argument = TypeCoerceExpression.unfoldExpression(stack.pop());
    ArrayExpression array = argument instanceof ArrayExpression ? (ArrayExpression) argument : null;
    BranchKey key = argument instanceof ConCallExpression ? ((ConCallExpression) argument).getDefinition() : argument instanceof IntegerExpression ? (((IntegerExpression) argument).isZero() ? Prelude.ZERO : Prelude.SUC) : array != null ? new ArrayConstructor(array.getElements().isEmpty(), true, true) : argument instanceof PathExpression ? Prelude.PATH_CON : null;

    ElimTree elimTree = key == null ? branchElimTree.getSingleConstructorChild() : branchElimTree.getChild(key);
    if (elimTree == null && key == Prelude.PATH_CON && branchElimTree.getSingleConstructorKey() instanceof IdpConstructor) {
      elimTree = branchElimTree.getSingleConstructorChild();
      key = null;
    }
    if (elimTree == null && branchElimTree.isArray()) {
      Expression type = argument.getType().normalize(NormalizationMode.WHNF);
      if (type instanceof ClassCallExpression && ((ClassCallExpression) type).getDefinition() == Prelude.DEP_ARRAY) {
        ClassCallExpression classCall = (ClassCallExpression) type;
        Expression length = classCall.getImplementationHere(Prelude.ARRAY_LENGTH, argument);
        if (length != null) {
          length = length.normalize(NormalizationMode.WHNF);
          if (length instanceof IntegerExpression || length instanceof ConCallExpression && ((ConCallExpression) length).getDefinition() == Prelude.SUC) {
            Expression elementsType = classCall.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE);
            if (elementsType == null) elementsType = FieldCallExpression.make(Prelude.ARRAY_ELEMENTS_TYPE, argument);
            LevelPair levelPair = classCall.getLevels().toLevelPair();
            if (length instanceof IntegerExpression && ((IntegerExpression) length).isZero()) {
              array = ArrayExpression.makeArray(levelPair, elementsType, Collections.emptyList(), null);
              key = new ArrayConstructor(true, true, true);
            } else {
              Expression length_1 = length.pred();
              TypedSingleDependentLink param = new TypedSingleDependentLink(true, "j", Fin(length_1));
              Sort sort = levelPair.toSort().max(Sort.SET0);
              Expression at = classCall.getImplementationHere(Prelude.ARRAY_AT, argument);
              Map<ClassField, Expression> impls = new HashMap<>();
              impls.put(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(sort, param, AppExpression.make(elementsType, Suc(new ReferenceExpression(param)), true)));
              impls.put(Prelude.ARRAY_LENGTH, length_1);
              impls.put(Prelude.ARRAY_AT, new LamExpression(sort, param, at != null ? AppExpression.make(at, Suc(new ReferenceExpression(param)), true) : FunCallExpression.make(Prelude.ARRAY_INDEX, classCall.getLevels(), Arrays.asList(argument, Suc(new ReferenceExpression(param))))));
              array = ArrayExpression.makeArray(levelPair, elementsType, new SingletonList<>(at != null ? AppExpression.make(at, new SmallIntegerExpression(0), true) : FunCallExpression.make(Prelude.ARRAY_INDEX, classCall.getLevels(), Arrays.asList(argument, new SmallIntegerExpression(0)))), new NewExpression(null, new ClassCallExpression(Prelude.DEP_ARRAY, classCall.getLevels(), impls, Sort.PROP, UniverseKind.NO_UNIVERSES)));
              key = new ArrayConstructor(false, true, true);
            }
            elimTree = branchElimTree.getChild(key);
          }
        }
      }
    }
    if (elimTree != null) {
      if (argList != null && branchElimTree.keepConCall()) {
        argList.add(argument);
      }

      List<? extends Expression> args;
      if (key != null) {
        args = argument instanceof ConCallExpression
          ? ((ConCallExpression) argument).getDefCallArguments()
          : array != null
            ? array.getConstructorArguments(!branchElimTree.withArrayElementsType(), !branchElimTree.withArrayLength())
            : argument instanceof PathExpression
              ? Collections.singletonList(((PathExpression) argument).getArgument())
              : key == Prelude.ZERO
                ? Collections.emptyList()
                : Collections.singletonList(((IntegerExpression) argument).pred());
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
    } else {
      stack.push(argument);
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
    if (expr.getDefinition() instanceof FunctionDefinition && ((FunctionDefinition) expr.getDefinition()).isSFunc() || !(expr.getDefinition() instanceof Function) || ((Function) expr.getDefinition()).getBody() == null && expr.getDefinition() != Prelude.DIV_MOD && expr.getDefinition() != Prelude.ARRAY_INDEX) {
      return applyDefCall(expr, mode);
    } else {
      assert expr instanceof LeveledDefCallExpression;
      return visitFunctionDefCall((LeveledDefCallExpression) expr, mode);
    }
  }

  public Expression evalFieldCall(ClassField field, Expression arg) {
    if (arg instanceof FunCallExpression && ((FunCallExpression) arg).getDefinition().getResultType() instanceof ClassCallExpression) {
      FunCallExpression funCall = (FunCallExpression) arg;
      Expression impl = ((ClassCallExpression) funCall.getDefinition().getResultType()).getImplementation(field, arg);
      if (impl != null) {
        ExprSubstitution substitution = new ExprSubstitution().add(funCall.getDefinition().getParameters(), funCall.getDefCallArguments());
        return impl.subst(substitution, funCall.getLevelSubstitution());
      } else if (funCall.getDefinition().getBody() == null) {
        return null;
      }
    }

    Expression type = arg.getType();
    Expression normType = type == null ? null : type.accept(this, NormalizationMode.WHNF);
    return normType instanceof ClassCallExpression ? ((ClassCallExpression) normType).getImplementation(field, arg) : null;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, NormalizationMode mode) {
    if (expr.getDefinition().isProperty()) {
      return applyDefCall(expr, mode);
    }

    if (expr.getDefinition() == Prelude.ARRAY_AT) {
      ClassCallExpression classCall = expr.getArgument().getType().normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
      if (classCall != null) {
        LevelPair levelPair = classCall.getLevels().toLevelPair();
        TypedSingleDependentLink lamParam = new TypedSingleDependentLink(true, "j", Fin(FieldCallExpression.make(Prelude.ARRAY_LENGTH, expr.getArgument())));
        return new LamExpression(new Sort(levelPair.get(LevelVariable.PVAR), levelPair.get(LevelVariable.HVAR).max(new Level(0))), lamParam, FunCallExpression.make(Prelude.ARRAY_INDEX, levelPair, Arrays.asList(expr.getArgument(), new ReferenceExpression(lamParam))));
      }
    }

    Expression thisExpr = expr.getArgument().accept(this, mode);
    if (!(thisExpr.getInferenceVariable() instanceof TypeClassInferenceVariable) && (!(mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP) || thisExpr instanceof NewExpression)) {
      Expression impl = evalFieldCall(expr.getDefinition(), thisExpr);
      if (impl != null) {
        return impl.accept(this, mode);
      }
    }

    return FieldCallExpression.make(expr.getDefinition(), mode == NormalizationMode.NF ? thisExpr.accept(this, mode) : thisExpr);
  }

  @Override
  protected Expression visitDataTypeArgument(Expression expr, NormalizationMode mode) {
    return mode == NormalizationMode.NF ? expr.accept(this, NormalizationMode.NF) : expr;
  }

  @Override
  protected Expression preVisitConCall(ConCallExpression expr, NormalizationMode mode) {
    return visitBody(expr.getDefinition().getBody(), expr.getDefCallArguments(), expr, mode);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, NormalizationMode mode) {
    Constructor constructor = expr.getDefinition();
    if ((mode == NormalizationMode.WHNF || constructor.status() != Definition.TypeCheckingStatus.NO_ERRORS) && constructor.getBody() == null) {
      return applyDefCall(expr, mode);
    }

    return super.visitConCall(expr, mode);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF) return expr;

    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassCallExpression result = new ClassCallExpression(expr.getDefinition(), expr.getLevels(), fieldSet, expr.getSort(), expr.getUniverseKind());
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
    return expr.getExpression() instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) expr.getExpression()).getSubstExpression() == null ? expr : expr.eval().accept(this, mode);
  }

  @Override
  public Expression visitLam(LamExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF) {
      return expr;
    }
    if (mode != NormalizationMode.NF) {
      return new LamExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, mode));
    }

    ExprSubstitution substitution = new ExprSubstitution();
    SingleDependentLink link = normalizeSingleParameters(expr.getParameters(), mode, substitution);
    return new LamExpression(expr.getResultSort(), link, expr.getBody().subst(substitution).accept(this, mode));
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
    return mode == NormalizationMode.NF && expr.getExpression() != null ? expr.replaceExpression(expr.getExpression().accept(this, mode)) : expr;
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
    Expression newExpr = expr.getExpression().accept(this, mode);
    TupleExpression exprNorm = newExpr.cast(TupleExpression.class);
    if (exprNorm != null) {
      return exprNorm.getFields().get(expr.getField()).accept(this, mode);
    } else {
      return mode == NormalizationMode.WHNF ? ProjExpression.make(newExpr, expr.getField()) : ProjExpression.make(expr.getExpression().accept(this, mode), expr.getField());
    }
  }

  private static Pair<BigInteger,Expression> getNumber(Expression expr) {
    expr = expr.normalize(NormalizationMode.WHNF);
    int s = 0;
    while (expr instanceof ConCallExpression && ((ConCallExpression) expr).getDefinition() == Prelude.SUC) {
      s++;
      expr = ((ConCallExpression) expr).getDefCallArguments().get(0).normalize(NormalizationMode.WHNF);
    }
    return new Pair<>(expr instanceof IntegerExpression || s > 0 ? (expr instanceof IntegerExpression ? ((IntegerExpression) expr).getBigInteger() : BigInteger.ZERO).add(BigInteger.valueOf(s)) : null, expr);
  }

  @Override
  public Expression visitNew(NewExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF ? expr : new NewExpression(expr.getRenewExpression() == null ? null : expr.getRenewExpression().accept(this, mode), visitClassCall(expr.getClassCall(), mode));
  }

  @Override
  public Expression visitLet(LetExpression let, NormalizationMode mode) {
    if ((mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP) && !let.isStrict()) {
      ExprSubstitution substitution = new ExprSubstitution();
      List<HaveClause> newClauses = new ArrayList<>(let.getClauses().size());
      for (HaveClause clause : let.getClauses()) {
        HaveClause newClause = LetClause.make(clause instanceof LetClause, clause.getName(), clause.getPattern(), clause.getExpression().accept(this, mode).subst(substitution));
        substitution.add(clause, new ReferenceExpression(newClause));
        newClauses.add(newClause);
      }
      return new LetExpression(let.isStrict(), newClauses, let.getExpression().accept(this, mode).subst(substitution));
    } else {
      return let.getResult().accept(this, mode);
    }
  }

  @Override
  public Expression visitCase(CaseExpression expr, NormalizationMode mode) {
    if (!expr.isSCase()) {
      Expression result = eval(expr.getElimBody(), expr.getArguments(), new ExprSubstitution(), LevelSubstitution.EMPTY, mode == NormalizationMode.WHNF ? expr : null, mode);
      if (result != null) {
        return result;
      }
    }

    List<Expression> args = new ArrayList<>(expr.getArguments().size());
    for (Expression arg : expr.getArguments()) {
      args.add(arg.accept(this, mode));
    }
    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink parameters = normalizeParameters(expr.getParameters(), mode, substitution);
    return new CaseExpression(expr.isSCase(), parameters, expr.getResultType().subst(substitution).accept(this, mode), expr.getResultTypeLevel() != null && mode == NormalizationMode.NF ? expr.getResultTypeLevel().subst(substitution).accept(this, mode) : expr.getResultTypeLevel(), mode == NormalizationMode.WHNF ? expr.getElimBody() : normalizeElimBody(expr.getElimBody(), mode), args);
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
  public IntegerExpression visitInteger(IntegerExpression expr, NormalizationMode mode) {
    return expr;
  }

  @Override
  public Expression visitTypeCoerce(TypeCoerceExpression expr, NormalizationMode mode) {
    Expression arg = expr.getArgument().accept(this, mode);
    if (arg instanceof TypeCoerceExpression && ((TypeCoerceExpression) arg).getDefinition() == expr.getDefinition() && ((TypeCoerceExpression) arg).isFromLeftToRight() != expr.isFromLeftToRight()) {
      Expression result = ((TypeCoerceExpression) arg).getArgument();
      return mode == NormalizationMode.WHNF ? result.accept(this, mode) : result;
    }

    List<Expression> args;
    if (mode == NormalizationMode.NF) {
      args = new ArrayList<>(expr.getClauseArguments().size());
      for (Expression argument : expr.getClauseArguments()) {
        args.add(argument.accept(this, NormalizationMode.NF));
      }
    } else {
      args = expr.getClauseArguments();
    }
    return TypeCoerceExpression.make(expr.getDefinition(), expr.getLevels(), expr.getClauseIndex(), args, arg, expr.isFromLeftToRight());
  }

  @Override
  public Expression visitArray(ArrayExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF) return expr;
    List<Expression> elements = new ArrayList<>(expr.getElements().size());
    for (Expression element : expr.getElements()) {
      elements.add(element.accept(this, mode));
    }
    return ArrayExpression.make(expr.getLevels(), mode == NormalizationMode.NF ? expr.getElementsType().accept(this, NormalizationMode.NF) : expr.getElementsType(), elements, expr.getTail() == null ? null : expr.getTail().accept(this, mode));
  }

  @Override
  public Expression visitPath(PathExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF) return expr;
    return new PathExpression(expr.getLevels(), expr.getArgumentType() == null ? null : expr.getArgumentType().accept(this, mode), expr.getArgument().accept(this, mode));
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF ? expr : new PEvalExpression(expr.getExpression().accept(this, mode));
  }
}
