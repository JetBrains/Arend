package org.arend.core.expr.visitor;

import org.arend.core.constructor.IdpConstructor;
import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.variable.Variable;
import org.arend.prelude.Prelude;
import org.arend.typechecking.computation.ComputationRunner;
import org.arend.util.FList;
import org.arend.util.Pair;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;

public class NormalizeVisitor extends ExpressionTransformer<NormalizationMode>  {
  private FList<SubstVisitor> mySubstStack;

  public static final NormalizeVisitor INSTANCE = new NormalizeVisitor(null);

  private NormalizeVisitor(FList<SubstVisitor> substStack) {
    mySubstStack = substStack;
  }

  private NormalizeVisitor makeVisitor(ExprSubstitution substitution, LevelSubstitution levelSubstitution) {
    return substitution.isEmpty() && levelSubstitution.isEmpty() ? this : new NormalizeVisitor(mySubstStack != null && (substitution.isEmpty() || mySubstStack.getHead().getExprSubstitution().isEmpty()) ? new FList<>(new SubstVisitor(substitution.isEmpty() ? mySubstStack.getHead().getExprSubstitution() : substitution, levelSubstitution.subst(mySubstStack.getHead().getLevelSubstitution())), mySubstStack.getTail()) : new FList<>(new SubstVisitor(substitution, levelSubstitution), mySubstStack));
  }

  @Override
  public Expression visitApp(AppExpression expr, NormalizationMode mode) {
    Expression function = expr.getFunction().accept(this, mode);
    LamExpression lamExpr = function.cast(LamExpression.class);
    if (lamExpr != null) {
      return AppExpression.make(lamExpr, expr.getArgument(), expr.isExplicit()).accept(this, mode);
    } else {
      return AppExpression.make(function, mode == NormalizationMode.WHNF ? applySubst(expr.getArgument()) : expr.getArgument().accept(this, mode), expr.isExplicit());
    }
  }

  private Expression applyDefCall(DefCallExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF || expr.getDefCallArguments().isEmpty()) {
      return applySubst(expr);
    }

    if (expr instanceof FieldCallExpression) {
      return FieldCallExpression.make((ClassField) expr.getDefinition(), applySubst(expr.getSortArgument()), ((FieldCallExpression) expr).getArgument().accept(this, mode));
    }

    DependentLink link = expr.getDefinition().getParameters();
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(mode == NormalizationMode.RNF_EXP && !link.isExplicit() ? applySubst(arg) : arg.accept(this, mode));
      if (link.hasNext()) {
        link = link.getNext();
      }
    }

    if (expr instanceof FunCallExpression) {
      return FunCallExpression.make((FunctionDefinition) expr.getDefinition(), applySubst(expr.getSortArgument()), args);
    }

    if (expr instanceof DataCallExpression) {
      return new DataCallExpression((DataDefinition) expr.getDefinition(), applySubst(expr.getSortArgument()), args);
    }

    if (expr instanceof ConCallExpression) {
      List<Expression> oldDataTypeArgs = ((ConCallExpression) expr).getDataTypeArguments();
      List<Expression> dataTypeArgs;
      if (mySubstStack != null) {
        dataTypeArgs = new ArrayList<>(oldDataTypeArgs.size());
        for (Expression argument : oldDataTypeArgs) {
          dataTypeArgs.add(applySubst(argument));
        }
      } else {
        dataTypeArgs = oldDataTypeArgs;
      }
      return ConCallExpression.make((Constructor) expr.getDefinition(), applySubst(expr.getSortArgument()), dataTypeArgs, args);
    }

    throw new IllegalStateException();
  }

  private Expression normalizePlus(DefCallExpression expr, NormalizationMode mode) {
    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    Expression arg1 = defCallArgs.get(0);
    Expression arg2 = defCallArgs.get(1).accept(this, NormalizationMode.WHNF);

    IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
    if (intExpr2 != null) {
      if (intExpr2.isZero()) {
        return arg1.accept(this, mode);
      }
      arg1 = arg1.accept(this, NormalizationMode.WHNF);
      IntegerExpression intExpr1 = arg1.cast(IntegerExpression.class);
      if (intExpr1 != null) {
        return intExpr1.plus(intExpr2);
      }

      if (mode != NormalizationMode.WHNF) {
        arg1 = arg1.accept(INSTANCE, mode);
      }
      for (int i = 0; intExpr2.compare(i) > 0; i++) {
        arg1 = Suc(arg1);
      }
      return arg1;
    }

    List<Expression> newDefCallArgs = new ArrayList<>(2);
    newDefCallArgs.add(arg1.accept(this, mode));
    Expression result = FunCallExpression.make(Prelude.PLUS, Sort.PROP, newDefCallArgs);
    ConCallExpression conCall2 = arg2.cast(ConCallExpression.class);
    while (conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
      result = Suc(result);
      arg2 = conCall2.getDefCallArguments().get(0).accept(INSTANCE, NormalizationMode.WHNF);
      conCall2 = arg2.cast(ConCallExpression.class);
    }
    newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(INSTANCE, mode));
    return result;
  }

  private Expression normalizeMinus(DefCallExpression expr, NormalizationMode mode) {
    List<? extends Expression> defCallArgs = expr.getDefCallArguments();
    Expression arg1 = defCallArgs.get(0).accept(this, NormalizationMode.WHNF);
    Expression arg2 = defCallArgs.get(1);

    IntegerExpression intExpr1 = arg1.cast(IntegerExpression.class);
    if (intExpr1 != null) {
      if (intExpr1.isZero()) {
        return Neg(arg2.accept(this, mode));
      }
      arg2 = arg2.accept(this, NormalizationMode.WHNF);
      IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
      if (intExpr2 != null) {
        return intExpr1.minus(intExpr2);
      }

      ConCallExpression conCall2 = arg2.cast(ConCallExpression.class);
      while (!intExpr1.isZero() && conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
        intExpr1 = intExpr1.pred();
        arg2 = conCall2.getDefCallArguments().get(0).accept(INSTANCE, NormalizationMode.WHNF);
        conCall2 = arg2.cast(ConCallExpression.class);
      }

      if (conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
        return Neg(conCall2);
      }

      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(intExpr1);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(INSTANCE, mode));
      return FunCallExpression.make(Prelude.MINUS, Sort.PROP, newDefCallArgs);
    }

    ConCallExpression conCall1 = arg1.cast(ConCallExpression.class);
    if (conCall1 == null || conCall1.getDefinition() != Prelude.SUC) {
      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(INSTANCE, mode));
      newDefCallArgs.add(arg2.accept(this, mode));
      return FunCallExpression.make(Prelude.MINUS, Sort.PROP, newDefCallArgs);
    }

    arg2 = arg2.accept(this, NormalizationMode.WHNF);
    IntegerExpression intExpr2 = arg2.cast(IntegerExpression.class);
    if (intExpr2 != null) {
      while (!intExpr2.isZero() && conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
        intExpr2 = intExpr2.pred();
        arg1 = conCall1.getDefCallArguments().get(0).accept(INSTANCE, NormalizationMode.WHNF);
        conCall1 = arg1.cast(ConCallExpression.class);
      }

      if (conCall1 != null && conCall1.getDefinition() == Prelude.SUC) {
        return Pos(conCall1);
      }

      List<Expression> newDefCallArgs = new ArrayList<>(2);
      newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(INSTANCE, mode));
      newDefCallArgs.add(intExpr2);
      return FunCallExpression.make(Prelude.MINUS, Sort.PROP, newDefCallArgs);
    }

    ConCallExpression conCall2 = arg2.cast(ConCallExpression.class);
    while (conCall1 != null && conCall1.getDefinition() == Prelude.SUC && conCall2 != null && conCall2.getDefinition() == Prelude.SUC) {
      arg1 = conCall1.getDefCallArguments().get(0).accept(INSTANCE, NormalizationMode.WHNF);
      conCall1 = arg1.cast(ConCallExpression.class);
      arg2 = conCall2.getDefCallArguments().get(0).accept(INSTANCE, NormalizationMode.WHNF);
      conCall2 = arg2.cast(ConCallExpression.class);
    }

    List<Expression> newDefCallArgs = new ArrayList<>(2);
    newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg1 : arg1.accept(INSTANCE, mode));
    newDefCallArgs.add(mode == NormalizationMode.WHNF ? arg2 : arg2.accept(INSTANCE, mode));
    return FunCallExpression.make(Prelude.MINUS, Sort.PROP, newDefCallArgs);
  }

  private Expression visitFunctionDefCall(DefCallExpression expr, NormalizationMode mode) {
    Definition definition = expr.getDefinition();
    if (definition == Prelude.COERCE || definition == Prelude.COERCE2) {
      LamExpression lamExpr = expr.getDefCallArguments().get(0).accept(this, NormalizationMode.WHNF).cast(LamExpression.class);
      if (lamExpr != null) {
        SingleDependentLink param = lamExpr.getParameters();
        Expression body = param.getNext().hasNext() ? new LamExpression(lamExpr.getResultSort(), param.getNext(), lamExpr.getBody()) : lamExpr.getBody().accept(INSTANCE, NormalizationMode.WHNF);
        FunCallExpression funCall = body.cast(FunCallExpression.class);
        boolean checkSigma = true;

        if (funCall != null && funCall.getDefinition() == Prelude.ISO && definition == Prelude.COERCE) {
          List<? extends Expression> isoArgs = funCall.getDefCallArguments();
          ReferenceExpression refExpr = isoArgs.get(isoArgs.size() - 1).accept(INSTANCE, NormalizationMode.WHNF).cast(ReferenceExpression.class);
          if (refExpr != null && refExpr.getBinding() == param) {
            checkSigma = false;
            ConCallExpression normedPtCon = expr.getDefCallArguments().get(2).accept(INSTANCE, NormalizationMode.WHNF).cast(ConCallExpression.class);
            if (normedPtCon != null && normedPtCon.getDefinition() == Prelude.RIGHT) {
              boolean noFreeVar = true;
              for (int i = 0; i < isoArgs.size() - 1; i++) {
                if (NormalizingFindBindingVisitor.findBinding(isoArgs.get(i), param)) {
                  noFreeVar = false;
                  break;
                }
              }
              if (noFreeVar) {
                return AppExpression.make(isoArgs.get(2), applySubst(expr.getDefCallArguments().get(1)), true).accept(INSTANCE, mode);
              }
              /* Stricter version of iso
              if (!NormalizingFindBindingVisitor.findBinding(isoArgs.get(0), param) && !NormalizingFindBindingVisitor.findBinding(isoArgs.get(1), param) && !NormalizingFindBindingVisitor.findBinding(isoArgs.get(2), param)) {
                return AppExpression.make(isoArgs.get(2), applySubst(expr.getDefCallArguments().get(1)), true).accept(INSTANCE, mode);
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
      Expression arg1 = defCallArgs.get(0);
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

        arg1 = arg1.accept(this, NormalizationMode.WHNF);
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
      } else if (definition == Prelude.MUL) {
        arg1 = arg1.normalize(NormalizationMode.WHNF);
      }

      if (definition == Prelude.MUL) {
        List<Expression> newDefCallArgs = new ArrayList<>(2);
        newDefCallArgs.add(arg1);
        newDefCallArgs.add(arg2);
        Expression result = INSTANCE.visitBody(Prelude.MUL.getBody(), expr, newDefCallArgs, mode);
        return result == null ? applyDefCall(expr, mode) : result;
      } else {
        return applyDefCall(expr, mode);
      }
    }

    Expression result = (defCallArgs == expr.getDefCallArguments() ? this : INSTANCE).visitBody(((Function) definition).getBody(), expr, defCallArgs, mode);
    return result == null ? applyDefCall(expr, mode) : result;
  }

  private Expression visitBody(Body body, DefCallExpression expr, List<? extends Expression> defCallArgs, NormalizationMode mode) {
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
            return result.accept(makeVisitor(substitution, expr.getSortArgument().toLevelSubstitution()), mode);
          }
        }
      }
      body = elim.getOtherwise();
    }

    if (body instanceof Expression) {
      return mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP ? null : ((Expression) body).accept(makeVisitor(addArguments(getDataTypeArgumentsSubstitution(expr), definition.getParameters(), defCallArgs, definition.isStrict()), expr.getSortArgument().toLevelSubstitution()), mode);
    } else if (body instanceof ElimBody) {
      if (definition.isStrict()) {
        List<Expression> normDefCalls = new ArrayList<>(defCallArgs.size());
        for (Expression arg : defCallArgs) {
          normDefCalls.add(arg.accept(this, NormalizationMode.WHNF));
        }
        ExprSubstitution substitution = getDataTypeArgumentsSubstitution(expr);
        if (mySubstStack != null) {
          for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
            entry.setValue(applySubst(entry.getValue()));
          }
        }
        return new NormalizeVisitor(null).makeVisitor(substitution, applySubst(expr.getSortArgument()).toLevelSubstitution()).eval((ElimBody) body, normDefCalls, mode);
      } else {
        return new NormalizeVisitor(mySubstStack).makeVisitor(getDataTypeArgumentsSubstitution(expr), expr.getSortArgument().toLevelSubstitution()).eval((ElimBody) body, defCallArgs, mode);
      }
    } else {
      assert body == null;
      return null;
    }
  }

  private ExprSubstitution addArguments(ExprSubstitution substitution, DependentLink link, List<? extends Expression> args, boolean isStrict) {
    if (isStrict) {
      for (Expression arg : args) {
        substitution.add(link, arg.accept(this, NormalizationMode.WHNF));
        link = link.getNext();
      }
    } else {
      substitution.add(link, args);
    }
    return substitution;
  }

  private Expression makeSubstExpr(Expression expr) {
    for (FList<SubstVisitor> list = mySubstStack; list != null; list = list.getTail()) {
      expr = new SubstExpression(expr, list.getHead().getExprSubstitution(), list.getHead().getLevelSubstitution());
    }
    return expr;
  }

  public Deque<Expression> makeStack(List<? extends Expression> arguments) {
    Deque<Expression> stack = new ArrayDeque<>();
    for (int i = arguments.size() - 1; i >= 0; i--) {
      stack.push(makeSubstExpr(arguments.get(i)));
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
        return new NormalizeVisitor(mySubstStack).makeVisitor(new ExprSubstitution(), funCall.getSortArgument().toLevelSubstitution()).eval((ElimBody) body, funCall.getDefCallArguments(), null);
      } else {
        return null;
      }
    } else if (expr instanceof CaseExpression) {
      return new NormalizeVisitor(mySubstStack).eval(((CaseExpression) expr).getElimBody(), ((CaseExpression) expr).getArguments(), null);
    } else {
      return null;
    }
  }

  private static boolean isBlocked(FunctionDefinition def) {
    return def.isSFunc() || def == Prelude.PLUS || def == Prelude.MUL || def == Prelude.MINUS || def == Prelude.DIV || def == Prelude.MOD || def == Prelude.DIV_MOD || def == Prelude.COERCE || def == Prelude.COERCE2;
  }

  public Expression eval(ElimBody elimBody, List<? extends Expression> arguments, NormalizationMode mode) {
    Deque<Expression> stack = makeStack(arguments);
    List<Expression> argList = new ArrayList<>();
    Expression result = null;
    Expression resultExpr = null;

    List<Expression> conArgs = null;
    int recursiveParam = -1;
    int sucs = 0;

    ElimTree elimTree = elimBody.getElimTree();
    while (true) {
      for (int i = 0; i < elimTree.getSkip(); i++) {
        argList.add(stack.pop());
      }

      if (elimTree instanceof LeafElimTree) {
        ExprSubstitution argSubst = new ExprSubstitution();
        LeafElimTree leafElimTree = (LeafElimTree) elimTree;
        ElimClause<Pattern> clause = elimBody.getClauses().get(leafElimTree.getClauseIndex());
        int i = 0;
        for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext(), i++) {
          argSubst.add(link, argList.get(leafElimTree.getArgumentIndex(i)));
        }
        resultExpr = Objects.requireNonNull(clause.getExpression());

        if (mode == null) {
          return makeVisitor(argSubst, LevelSubstitution.EMPTY).applySubst(resultExpr);
        }

        if (!argSubst.isEmpty()) {
          mySubstStack = new FList<>(new SubstVisitor(argSubst, LevelSubstitution.EMPTY), mySubstStack);
        }

        while (true) {
          if (mode != NormalizationMode.RNF && mode != NormalizationMode.RNF_EXP && resultExpr instanceof LetExpression) {
            LetExpression let = (LetExpression) resultExpr;
            if (let.isStrict() || mySubstStack != null) {
              ExprSubstitution substitution = new ExprSubstitution();
              mySubstStack = new FList<>(new SubstVisitor(substitution, LevelSubstitution.EMPTY), mySubstStack);
              for (LetClause letClause : let.getClauses()) {
                substitution.add(letClause, let.isStrict() ? LetExpression.normalizeClauseExpression(letClause.getPattern(), letClause.getExpression().accept(this, NormalizationMode.WHNF)) : letClause.getExpression());
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
              if (mySubstStack == null) {
                newDataTypeArgs = conCall.getDataTypeArguments();
                newConArgs = new ArrayList<>(conCall.getDefCallArguments());
              } else {
                newDataTypeArgs = new ArrayList<>(conCall.getDataTypeArguments().size());
                for (Expression arg : conCall.getDataTypeArguments()) {
                  newDataTypeArgs.add(applySubst(arg));
                }
                newConArgs = new ArrayList<>(conCall.getDefCallArguments().size());
                for (int j = 0; j < conCall.getDefCallArguments().size(); j++) {
                  if (j != recParam) {
                    newConArgs.add(applySubst(conCall.getDefCallArguments().get(j)));
                  } else {
                    newConArgs.add(conCall.getDefCallArguments().get(j));
                  }
                }
              }
              Expression newExpr = ConCallExpression.make(conCall.getDefinition(), applySubst(conCall.getSortArgument()), newDataTypeArgs, newConArgs);
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
            mySubstStack = new FList<>(new SubstVisitor(addArguments(new ExprSubstitution(), funCall.getDefinition().getParameters(), funCall.getDefCallArguments(), funCall.getDefinition().isStrict()), funCall.getSortArgument().toLevelSubstitution()), mySubstStack);
            resultExpr = (Expression) funCall.getDefinition().getBody();
          } else if (resultExpr instanceof ReferenceExpression && ((ReferenceExpression) resultExpr).getBinding() instanceof EvaluatingBinding) {
            resultExpr = ((EvaluatingBinding) ((ReferenceExpression) resultExpr).getBinding()).getExpression();
          } else if (mySubstStack != null && resultExpr instanceof ReferenceExpression) {
            Binding binding = ((ReferenceExpression) resultExpr).getBinding();
            for (; mySubstStack != null; mySubstStack = mySubstStack.getTail()) {
              Expression expr = mySubstStack.getHead().getExprSubstitution().get(binding);
              if (expr != null) {
                mySubstStack = mySubstStack.getTail();
                resultExpr = expr;
                break;
              }
            }
          } else if (resultExpr instanceof SubstExpression) {
            resultExpr = unfoldSubstExpression((SubstExpression) resultExpr);
          } else {
            break;
          }
        }

        if (resultExpr instanceof FunCallExpression && ((FunCallExpression) resultExpr).getDefinition().getBody() instanceof ElimBody && !isBlocked(((FunCallExpression) resultExpr).getDefinition()) || resultExpr instanceof CaseExpression && !((CaseExpression) resultExpr).isSCase()) {
          elimBody = resultExpr instanceof FunCallExpression ? (ElimBody) ((FunCallExpression) resultExpr).getDefinition().getBody() : ((CaseExpression) resultExpr).getElimBody();
          elimTree = elimBody.getElimTree();
          argList.clear();
          stack.clear();

          ComputationRunner.checkCanceled();

          List<? extends Expression> args = resultExpr instanceof FunCallExpression ? ((FunCallExpression) resultExpr).getDefCallArguments() : ((CaseExpression) resultExpr).getArguments();
          if (!args.isEmpty()) {
            LevelSubstitution levelSubstitution = resultExpr instanceof FunCallExpression ? ((FunCallExpression) resultExpr).getSortArgument().toLevelSubstitution() : LevelSubstitution.EMPTY;
            boolean isStrict = resultExpr instanceof FunCallExpression && ((FunCallExpression) resultExpr).getDefinition().isStrict();
            NormalizeVisitor visitor = levelSubstitution.isEmpty() ? this : makeVisitor(new ExprSubstitution(), levelSubstitution);
            for (int j = args.size() - 1; j >= 0; j--) {
              stack.push(isStrict ? args.get(j).accept(visitor, NormalizationMode.WHNF) : visitor.makeSubstExpr(args.get(j)));
            }
          }
          continue;
        }

        resultExpr = applySubst(resultExpr);
        if (result == null) {
          result = resultExpr;
        } else {
          conArgs.set(recursiveParam, resultExpr);
        }
        return addSucs(result, sucs).accept(INSTANCE, mode);
      }

      elimTree = updateStack(stack, argList, (BranchElimTree) elimTree);
      if (elimTree == null) {
        if (resultExpr != null) {
          resultExpr = applySubst(resultExpr);
        }
        if (result == null) {
          result = resultExpr;
        } else {
          conArgs.set(recursiveParam, resultExpr);
        }
        return result == null ? null : addSucs(result, sucs).accept(INSTANCE, mode);
      }
    }
  }

  private Expression unfoldSubstExpression(SubstExpression expr) {
    Expression subExpr = expr.getExpression();
    return (subExpr instanceof SubstExpression ? unfoldSubstExpression((SubstExpression) subExpr) : subExpr).subst(expr.getSubstitution(), expr.getLevelSubstitution());
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
        Expression top = stack.isEmpty() ? null : stack.peek().getUnderlyingExpression();
        return !(top instanceof ConCallExpression || top instanceof IntegerExpression);
      }
    }
  }

  private ElimTree updateStack(Deque<Expression> stack, List<Expression> argList, BranchElimTree branchElimTree) {
    Expression argument = stack.pop().accept(INSTANCE, NormalizationMode.WHNF);
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
    if (expr.getDefinition() instanceof FunctionDefinition && ((FunctionDefinition) expr.getDefinition()).isSFunc() ||
        expr.getDefinition().status() != Definition.TypeCheckingStatus.NO_ERRORS && expr.getDefinition() instanceof Function && ((Function) expr.getDefinition()).getBody() == null) {
      return applyDefCall(expr, mode);
    }

    return expr.getDefinition() instanceof Function ? visitFunctionDefCall(expr, mode) : applyDefCall(expr, mode);
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, NormalizationMode mode) {
    if (expr.getDefinition().isProperty()) {
      return applyDefCall(expr, mode);
    }

    Expression thisExpr = expr.getArgument().accept(this, NormalizationMode.WHNF);
    if (!(thisExpr.getInferenceVariable() instanceof TypeClassInferenceVariable)) {
      Expression type = thisExpr.getType();
      ClassCallExpression classCall = type == null ? null : type.accept(INSTANCE, NormalizationMode.WHNF).cast(ClassCallExpression.class);
      if (classCall != null) {
        Expression impl = classCall.getImplementation(expr.getDefinition(), thisExpr);
        if (impl != null) {
          return impl.accept(INSTANCE, mode);
        }
      }
    }
    return FieldCallExpression.make(expr.getDefinition(), applySubst(expr.getSortArgument()), mode == NormalizationMode.NF ? thisExpr.accept(INSTANCE, mode) : thisExpr);
  }

  @Override
  protected Expression visitDataTypeArgument(Expression expr, NormalizationMode mode) {
    return mode == NormalizationMode.NF ? expr.accept(this, NormalizationMode.NF) : applySubst(expr);
  }

  @Override
  protected Expression preVisitConCall(ConCallExpression expr, NormalizationMode mode) {
    return visitBody(expr.getDefinition().getBody(), expr, expr.getDefCallArguments(), mode);
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
    if (mode == NormalizationMode.WHNF) return (ClassCallExpression) applySubst(expr);

    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassCallExpression result = new ClassCallExpression(expr.getDefinition(), applySubst(expr.getSortArgument()), fieldSet, applySubst(expr.getSort()), expr.getUniverseKind());
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
    for (FList<SubstVisitor> list = mySubstStack; list != null; list = list.getTail()) {
      Expression res = list.getHead().getExprSubstitution().get(expr.getBinding());
      if (res != null) {
        return res.accept(new NormalizeVisitor(list.getTail()), mode);
      }
    }

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
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, mode) : applySubst(expr);
  }

  @Override
  public Expression visitSubst(SubstExpression expr, NormalizationMode mode) {
    return expr.getExpression().accept(makeVisitor(expr.getSubstitution(), expr.getLevelSubstitution()), mode);
  }

  @Override
  public Expression visitLam(LamExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF) {
      return applySubst(expr);
    }

    ExprSubstitution substitution = new ExprSubstitution();
    SingleDependentLink link = normalizeSingleParameters(expr.getParameters(), mode, substitution);
    return new LamExpression(applySubst(expr.getResultSort()), link, expr.getBody().accept(makeVisitor(substitution, LevelSubstitution.EMPTY), mode));
  }

  @Override
  public PiExpression visitPi(PiExpression expr, NormalizationMode mode) {
    if (mode != NormalizationMode.WHNF) {
      ExprSubstitution substitution = new ExprSubstitution();
      SingleDependentLink link = normalizeSingleParameters(expr.getParameters(), mode, substitution);
      return new PiExpression(applySubst(expr.getResultSort()), link, expr.getCodomain().accept(makeVisitor(substitution, LevelSubstitution.EMPTY), mode));
    } else {
      return (PiExpression) applySubst(expr);
    }
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, NormalizationMode mode) {
    return mySubstStack == null ? expr : new UniverseExpression(applySubst(expr.getSort()));
  }

  @Override
  public Expression visitError(ErrorExpression expr, NormalizationMode mode) {
    return expr.getExpression() == null ? expr : mode == NormalizationMode.WHNF ? applySubst(expr) : expr.replaceExpression(expr.getExpression().accept(this, mode));
  }

  @Override
  public Expression visitTuple(TupleExpression expr, NormalizationMode mode) {
    if (mode == NormalizationMode.WHNF) return applySubst(expr);
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, mode));
    }
    return new TupleExpression(fields, mode == NormalizationMode.NF ? (SigmaExpression) applySubst(expr.getSigmaType()) : expr.getSigmaType());
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF ? (SigmaExpression) applySubst(expr) : new SigmaExpression(applySubst(expr.getSort()), normalizeParameters(expr.getParameters(), mode, new ExprSubstitution()));
  }

  private DependentLink normalizeParameters(DependentLink parameters, NormalizationMode mode, ExprSubstitution substitution) {
    DependentLink link = DependentLink.Helper.subst(parameters, substitution);
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      link1 = link1.getNextTyped(null);
      link1.setType(link1.getType().normalize(this, mode));
    }
    return link;
  }

  private SingleDependentLink normalizeSingleParameters(SingleDependentLink parameters, NormalizationMode mode, ExprSubstitution substitution) {
    SingleDependentLink link = DependentLink.Helper.subst(parameters, substitution);
    for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext()) {
      link1 = link1.getNextTyped(null);
      link1.setType(link1.getType().normalize(this, mode));
    }
    return link;
  }

  @Override
  public Expression visitProj(ProjExpression expr, NormalizationMode mode) {
    Expression newExpr = expr.getExpression().accept(this, NormalizationMode.WHNF);
    TupleExpression exprNorm = newExpr.cast(TupleExpression.class);
    if (exprNorm != null) {
      return exprNorm.getFields().get(expr.getField()).accept(INSTANCE, mode);
    } else {
      return ProjExpression.make(mode == NormalizationMode.WHNF ? newExpr : newExpr.accept(INSTANCE, mode), expr.getField());
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF ? applySubst(expr) : new NewExpression(expr.getRenewExpression() == null ? null : expr.getRenewExpression().accept(this, mode), visitClassCall(expr.getClassCall(), mode));
  }

  @Override
  public Expression visitLet(LetExpression let, NormalizationMode mode) {
    if (mode == NormalizationMode.RNF || mode == NormalizationMode.RNF_EXP) {
      List<LetClause> newClauses = new ArrayList<>(let.getClauses().size());
      ExprSubstitution substitution = new ExprSubstitution();
      NormalizeVisitor visitor = new NormalizeVisitor(new FList<>(new SubstVisitor(substitution, LevelSubstitution.EMPTY), mySubstStack));
      for (LetClause clause : let.getClauses()) {
        LetClause newClause = new LetClause(clause.getName(), clause.getPattern(), clause.getExpression().accept(visitor, mode));
        substitution.add(clause, new ReferenceExpression(newClause));
        newClauses.add(newClause);
      }
      return new LetExpression(let.isStrict(), newClauses, let.getExpression().accept(visitor, mode));
    } else {
      if (!let.isStrict() && mySubstStack == null) {
        return let.getExpression().accept(this, mode);
      }

      ExprSubstitution substitution = new ExprSubstitution();
      NormalizeVisitor visitor = new NormalizeVisitor(new FList<>(new SubstVisitor(substitution, LevelSubstitution.EMPTY), mySubstStack));
      for (LetClause clause : let.getClauses()) {
        substitution.add(clause, let.isStrict() ? LetExpression.normalizeClauseExpression(clause.getPattern(), clause.getExpression().accept(visitor, NormalizationMode.WHNF)) : clause.getExpression());
      }

      return let.getExpression().accept(visitor, mode);
    }
  }

  @Override
  public Expression visitCase(CaseExpression expr, NormalizationMode mode) {
    if (!expr.isSCase()) {
      Expression result = new NormalizeVisitor(mySubstStack).eval(expr.getElimBody(), expr.getArguments(), mode);
      if (result != null) {
        return result;
      }
    }
    if (mode == NormalizationMode.WHNF) {
      return applySubst(expr);
    }

    List<Expression> args = new ArrayList<>(expr.getArguments().size());
    for (Expression arg : expr.getArguments()) {
      args.add(arg.accept(this, mode));
    }
    if (mode == NormalizationMode.NF) {
      ExprSubstitution substitution = new ExprSubstitution();
      NormalizeVisitor visitor = new NormalizeVisitor(new FList<>(new SubstVisitor(substitution, LevelSubstitution.EMPTY), mySubstStack));
      DependentLink parameters = normalizeParameters(expr.getParameters(), mode, substitution);
      return new CaseExpression(expr.isSCase(), parameters, expr.getResultType().accept(visitor, mode), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(visitor, mode), normalizeElimBody(expr.getElimBody(), mode), args);
    } else {
      return new CaseExpression(expr.isSCase(), expr.getParameters(), expr.getResultType().accept(this, mode), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(this, mode), normalizeElimBody(expr.getElimBody(), mode), args);
    }
  }

  private ElimBody normalizeElimBody(ElimBody elimBody, NormalizationMode mode) {
    List<ElimClause<Pattern>> clauses = new ArrayList<>();
    if (mode == NormalizationMode.NF) {
      for (ElimClause<Pattern> clause : elimBody.getClauses()) {
        ExprSubstitution substitution = new ExprSubstitution();
        DependentLink parameters = normalizeParameters(clause.getParameters(), mode, substitution);
        clauses.add(new ElimClause<>(Pattern.replaceBindings(clause.getPatterns(), parameters), clause.getExpression() == null ? null : clause.getExpression().accept(makeVisitor(substitution, LevelSubstitution.EMPTY), mode)));
      }
    } else {
      for (ElimClause<Pattern> clause : elimBody.getClauses()) {
        clauses.add(new ElimClause<>(clause.getPatterns(), clause.getExpression() == null ? null : clause.getExpression().accept(this, mode)));
      }
    }
    return new ElimBody(clauses, elimBody.getElimTree());
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, NormalizationMode mode) {
    return expr.getExpression().accept(this, mode);
  }

  @Override
  public IntegerExpression visitInteger(IntegerExpression expr, NormalizationMode params) {
    return expr;
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, NormalizationMode mode) {
    return mode == NormalizationMode.WHNF ? applySubst(expr) : new PEvalExpression(expr.getExpression().accept(this, mode));
  }

  private Expression applySubst(Expression expr) {
    for (FList<SubstVisitor> list = mySubstStack; list != null; list = list.getTail()) {
      if (!list.getHead().isEmpty()) {
        expr = expr.accept(list.getHead(), null);
      }
    }
    return expr;
  }

  private Sort applySubst(Sort sort) {
    for (FList<SubstVisitor> list = mySubstStack; list != null; list = list.getTail()) {
      sort = sort.subst(list.getHead().getLevelSubstitution());
    }
    return sort;
  }
}
