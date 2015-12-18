package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingDefCall;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.error.*;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.ImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.OldArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.*;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final List<Binding> myLocalContext;
  private final ErrorReporter myErrorReporter;
  private TypeCheckingDefCall myTypeCheckingDefCall;
  private TypeCheckingElim myTypeCheckingElim;
  private ImplicitArgsInference myArgsInference;

  public static class Result {
    public Expression expression;
    public Expression type;
    public Equations equations;

    public Result(Expression expression, Expression type, Equations equations) {
      this.expression = expression;
      this.type = type;
      this.equations = equations;
    }
  }

  public static class LetClauseResult {
    LetClause letClause;
    Equations equations;

    public LetClauseResult(LetClause letClause, Equations equations) {
      this.letClause = letClause;
      this.equations = equations;
    }
  }

  private CheckTypeVisitor(List<Binding> localContext, ErrorReporter errorReporter, TypeCheckingDefCall typeCheckingDefCall, ImplicitArgsInference argsInference) {
    myLocalContext = localContext;
    myErrorReporter = errorReporter;
    myTypeCheckingDefCall = typeCheckingDefCall;
    myArgsInference = argsInference;
  }

  public static class Builder {
    private final List<Binding> myLocalContext;
    private final ErrorReporter myErrorReporter;
    private TypeCheckingDefCall myTypeCheckingDefCall;
    private ImplicitArgsInference myArgsInference;
    private ClassDefinition myThisClass;

    public Builder(List<Binding> localContext, ErrorReporter errorReporter) {
      myLocalContext = localContext;
      myErrorReporter = errorReporter;
    }

    public Builder typeCheckingDefCall(TypeCheckingDefCall typeCheckingDefCall) {
      myTypeCheckingDefCall = typeCheckingDefCall;
      return this;
    }

    public Builder argsInference(ImplicitArgsInference argsInference) {
      myArgsInference = argsInference;
      return this;
    }

    public Builder thisClass(ClassDefinition thisClass) {
      myThisClass = thisClass;
      return this;
    }

    public CheckTypeVisitor build() {
      CheckTypeVisitor visitor = new CheckTypeVisitor(myLocalContext, myErrorReporter, myTypeCheckingDefCall, myArgsInference);
      if (myTypeCheckingDefCall == null) {
        visitor.myTypeCheckingDefCall = new TypeCheckingDefCall(visitor);
        visitor.myTypeCheckingDefCall.setThisClass(myThisClass);
      }
      visitor.myTypeCheckingElim = new TypeCheckingElim(visitor);
      if (myArgsInference == null) {
        visitor.myArgsInference = new OldArgsInference(visitor);
      }
      return visitor;
    }
  }

  public TypeCheckingDefCall getTypeCheckingDefCall() {
    return myTypeCheckingDefCall;
  }

  public TypeCheckingElim getTypeCheckingElim() {
    return myTypeCheckingElim;
  }

  public void setThisClass(ClassDefinition thisClass) {
    myTypeCheckingDefCall.setThisClass(thisClass);
  }

  public List<Binding> getLocalContext() {
    return myLocalContext;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  public Result checkResult(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myLocalContext, result.expression);
      return result;
    }
    Expression actualNorm = result.type.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
    if (NewCompareVisitor.compare(result.equations, Equations.CMP.GE, myLocalContext, expectedNorm, actualNorm)) {
      expression.setWellTyped(myLocalContext, result.expression);
      return result;
    } else {
      TypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.NFH, myLocalContext), result.type.normalize(NormalizeVisitor.Mode.NFH, myLocalContext), expression, getNames(myLocalContext));
      expression.setWellTyped(myLocalContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private Result checkResultImplicit(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myLocalContext, result.expression);
      return result;
    }
    return checkResult(expectedType, myArgsInference.inferTail(result, expectedType, expression), expression);
  }

  public Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    }
    return expr.accept(this, expectedType);
  }

  public Result checkType(Abstract.Expression expr, Expression expectedType) {
    int size = myLocalContext.size();
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;

    if (myLocalContext.size() > size) {
      myErrorReporter.report(new InferenceError(expr, myLocalContext, myLocalContext.size() - size));
    }

    return result;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    return checkResult(expectedType, myArgsInference.infer(expr, expectedType), expr);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    return checkResultImplicit(expectedType, myTypeCheckingDefCall.typeCheckDefCall(expr), expr);
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    Result result = getIndex(expr);
    result.type = result.type.liftIndex(0, ((IndexExpression) result.expression).getIndex() + 1);
    return checkResultImplicit(expectedType, result, expr);
  }

  private Result getIndex(Abstract.IndexExpression expr) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getType();
    return new Result(Index(expr.getIndex()), actualType, null);
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    // TODO
    return null;
  }

  /*
  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    Equations equations = myArgsInference.newEquations();
    ContextSaver saver = new ContextSaver(myLocalContext);

    int liftOn = 0;
    List<TypeArgument> actualPiArgs = null; // This is used when expectedType is unknown yet, and we must infer the type ourselves.
    for (Abstract.Argument argument : expr.getArguments()) {
      if (argument instanceof Abstract.TelescopeArgument) {
        Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) argument;
        Result result = typeCheck(teleArg.getType(), Universe());
        if (result == null) return null;
        result.equations.lift(-liftOn);
        equations.add(result.equations);
        liftOn += teleArg.getNames().size();

        int from = 0;
        if (actualPiArgs != null) {
          actualPiArgs.add(Tele(teleArg.getExplicit(), teleArg.getNames(), result.expression));
        } else {
          List<TypeArgument> piArgs = new ArrayList<>(teleArg.getNames().size());
          expectedType = expectedType.splitAt(teleArg.getNames().size(), piArgs, myLocalContext);
          from = piArgs.size();
          if (from < teleArg.getNames().size()) {
            if (isStuckOnInferVar(expectedType)) {
              actualPiArgs = new ArrayList<>();
              actualPiArgs.add(Tele(argument.getExplicit(), new ArrayList<>(teleArg.getNames().subList(from, teleArg.getNames().size())), result.expression));
            } else {
              TypeCheckingError error = new TypeCheckingError("Lambda has too many arguments", expr, getNames(myLocalContext));
              saver.close();
              expr.setWellTyped(myLocalContext, Error(null, error));
              myErrorReporter.report(error);
              return null;
            }
          }

          for (int i = 0; i < from; i++) {
            Expression expr = result.expression.liftIndex(0, i);
            if (!NewCompareVisitor.compare(equations, Equations.CMP.EQ, myLocalContext, expr, piArgs.get(i).getType())) {
              TypeCheckingError error = new TypeMismatchError(result.expression, piArgs.get(i).getType(), expr, getNames(myLocalContext));
              saver.close();
              expr.setWellTyped(myLocalContext, Error(null, error));
              myErrorReporter.report(error);
              return null;
            }
            myLocalContext.add(new TypedBinding(teleArg.getNames().get(i), expr));
          }
        }

        for (int i = from; i < teleArg.getNames().size(); i++) {
          myLocalContext.add(new TypedBinding(teleArg.getNames().get(i), result.expression.liftIndex(0, i)));
        }
      } else
      if (argument instanceof Abstract.NameArgument) {
        liftOn++;
        if (actualPiArgs != null) {
          TypeCheckingError error = new ArgInferenceError(lambdaArg(liftOn), expr, getNames(myLocalContext), expr);
          saver.close();
          expr.setWellTyped(myLocalContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        } else {
          List<TypeArgument> piArgs = new ArrayList<>(1);
          expectedType = expectedType.splitAt(1, piArgs, myLocalContext);
          if (piArgs.size() >= 1) {
            myLocalContext.add(new TypedBinding(((Abstract.NameArgument) argument).getName(), piArgs.get(0).getType()));
          } else {
            TypeCheckingError error;
            if (isStuckOnInferVar(expectedType)) {
              error = new ArgInferenceError(lambdaArg(liftOn), expr, getNames(myLocalContext), expr);
            } else {
              error = new TypeCheckingError("Lambda has too many arguments", expr, getNames(myLocalContext));
            }
            saver.close();
            expr.setWellTyped(myLocalContext, Error(null, error));
            myErrorReporter.report(error);
            return null;
          }
        }
      } else {
        throw new IllegalStateException();
      }
    }

    Result result = typeCheck(expr.getBody(), actualPiArgs == null ? expectedType : null);
    if (result == null) return null;
    if (actualPiArgs != null) {
      equations.add(expectedType, Pi(actualPiArgs, result.type), Equations.CMP.EQ);
    }
  }
  */

  private boolean isStuckOnInferVar(Expression expr) {
    int liftOn = 0;
    while (true) {
      if (expr instanceof AppExpression) {
        expr = ((AppExpression) expr).getFunction();
      } else
      if (expr instanceof LetExpression) {
        liftOn += ((LetExpression) expr).getClauses().size();
        expr = ((LetExpression) expr).getExpression();
      } else
      if (expr instanceof ProjExpression) {
        expr = ((ProjExpression) expr).getExpression();
      } else
      if (expr instanceof IndexExpression) {
        int index = ((IndexExpression) expr).getIndex();
        return index >= liftOn && index - liftOn < myLocalContext.size() && myLocalContext.get(myLocalContext.size() - 1 - (index - liftOn)).isInference();
      } else {
        return false;
      }
    }
  }

  /*
  private Result typeCheckLam(List<? extends Abstract.Argument> arguments, Abstract.Expression body, Expression expectedType) {
    Equations resultEquations = myArgsInference.newEquations();

    class Arg {
      boolean isExplicit;
      String name;
      Abstract.Expression type;

      Arg(boolean isExplicit, String name, Abstract.Expression type) {
        this.isExplicit = isExplicit;
        this.name = name;
        this.type = type;
      }
    }

    List<Arg> lambdaArgs = new ArrayList<>();
    for (Abstract.Argument arg : expr.getArguments()) {
      if (arg instanceof Abstract.NameArgument) {
        lambdaArgs.add(new Arg(arg.getExplicit(), ((Abstract.NameArgument) arg).getName(), null));
      } else {
        for (String name : ((Abstract.TelescopeArgument) arg).getNames()) {
          lambdaArgs.add(new Arg(arg.getExplicit(), name, ((Abstract.TelescopeArgument) arg).getType()));
        }
      }
    }

    List<TypeArgument> piArgs = new ArrayList<>();
    int actualNumberOfPiArgs;
    Expression resultType;
    Expression fresultType;
    if (expectedType == null) {
      for (Arg ignored : lambdaArgs) {
        piArgs.add(null);
      }
      actualNumberOfPiArgs = 0;
      resultType = null;
      fresultType = null;
    } else {
      resultType = expectedType.splitAt(lambdaArgs.size(), piArgs, myLocalContext).normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      fresultType = resultType.getFunction(new ArrayList<Expression>());
      actualNumberOfPiArgs = piArgs.size();
      if (fresultType instanceof InferHoleExpression) {
        for (int i = piArgs.size(); i < lambdaArgs.size(); ++i) {
          piArgs.add(null);
        }
        actualNumberOfPiArgs = 0;
        resultType = null;
        fresultType = null;
      } else {
        resultType = expectedType.splitAt(lambdaArgs.size(), piArgs, myLocalContext);
        fresultType = resultType.getFunction(new ArrayList<Expression>());
        actualNumberOfPiArgs = piArgs.size();
        if (fresultType instanceof InferHoleExpression) {
          for (int i = piArgs.size(); i < lambdaArgs.size(); ++i) {
            piArgs.add(null);
          }
        }
      }
    }

    if (piArgs.size() < lambdaArgs.size()) {
      TypeCheckingError error = new TypeCheckingError("Expected a function of " + piArgs.size() + " arguments, but the lambda has " + lambdaArgs.size(), expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    List<TypeCheckingError> errors = new ArrayList<>(lambdaArgs.size());
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (piArgs.get(i) == null && lambdaArgs.get(i).type == null) {
        TypeCheckingError error = new ArgInferenceError(lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
        errors.add(error);
        if (fresultType instanceof InferHoleExpression) {
          expr.setWellTyped(myLocalContext, Error(null, error));
          return new InferErrorResult((InferHoleExpression) fresultType, error, null);
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).type == null) {
        InferHoleExpression hole = piArgs.get(i).getType().accept(new FindHoleVisitor(), null);
        if (hole != null) {
          if (!errors.isEmpty()) {
            break;
          } else {
            TypeCheckingError error = new ArgInferenceError(lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
            expr.setWellTyped(myLocalContext, Error(null, error));
            return new InferErrorResult(hole, error, null);
          }
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).type != null) {
        if (piArgs.get(i).getExplicit() != lambdaArgs.get(i).isExplicit) {
          errors.add(new TypeCheckingError((i + 1) + suffix(i + 1) + " argument of the lambda should be " + (piArgs.get(i).getExplicit() ? "explicit" : "implicit"), expr, getNames(myLocalContext)));
        }
      }
    }
    if (!errors.isEmpty()) {
      expr.setWellTyped(myLocalContext, Error(null, errors.get(0)));
      for (TypeCheckingError error : errors) {
        myErrorReporter.report(error);
      }
      return null;
    }

    List<TypeArgument> argumentTypes = new ArrayList<>(lambdaArgs.size());
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (lambdaArgs.get(i).type != null) {
        Result argResult = typeCheck(lambdaArgs.get(i).type, Universe());
        if (argResult == null) {
          while (i-- > 0) {
            myLocalContext.remove(myLocalContext.size() - 1);
          }
          return null;
        }
        argResult.equations.lift(-i);
        resultEquations.add(argResult.equations);
        argumentTypes.add(Tele(lambdaArgs.get(i).isExplicit, vars(lambdaArgs.get(i).name), argResult.expression));

        if (piArgs.get(i) != null) {
          Expression argExpectedType = piArgs.get(i).getType().normalize(NormalizeVisitor.Mode.NF, myLocalContext);
          Expression argActualType = argumentTypes.get(i).getType().normalize(NormalizeVisitor.Mode.NF, myLocalContext);
          Equations equations = myArgsInference.newEquations();
          if (!NewCompareVisitor.compare(equations, Equations.CMP.LE, myLocalContext, argExpectedType, argActualType)) {
            errors.add(new TypeMismatchError(piArgs.get(i).getType(), lambdaArgs.get(i).type, expr, getNames(myLocalContext)));
          } else {
            equations.lift(-i);
            resultEquations.add(equations);
          }
        }
      } else {
        argumentTypes.add(Tele(piArgs.get(i).getExplicit(), vars(lambdaArgs.get(i).name), piArgs.get(i).getType()));
      }
      myLocalContext.add(new TypedBinding(lambdaArgs.get(i).name, argumentTypes.get(i).getType()));
    }

    Result bodyResult = typeCheck(expr.getBody(), fresultType instanceof InferHoleExpression ? null : resultType);
    if (bodyResult == null) {
      for (int i = 0; i < lambdaArgs.size(); ++i) {
        myLocalContext.remove(myLocalContext.size() - 1);
      }
      return null;
    }
    bodyResult.equations.lift(-lambdaArgs.size());
    resultEquations.add(bodyResult.equations);

    /* TODO
    if (resultType instanceof InferHoleExpression) {
      Expression actualType = okBodyResult.type;
      if (lambdaArgs.size() > actualNumberOfPiArgs) {
        actualType = Pi(argumentTypes.subList(actualNumberOfPiArgs, lambdaArgs.size()), actualType);
      }
      Expression expr1 = actualType.normalize(NormalizeVisitor.Mode.NF, myLocalContext).liftIndex(0, -actualNumberOfPiArgs);
      if (expr1 != null) {
        resultEquations.add(new CompareVisitor.Equation((InferHoleExpression) resultType, expr1));
      }
    }
    // Here it ends

    for (int i = 0; i < lambdaArgs.size(); ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }

    List<TelescopeArgument> resultLambdaArgs = new ArrayList<>(argumentTypes.size());
    for (TypeArgument argumentType : argumentTypes) {
      resultLambdaArgs.add((TelescopeArgument) argumentType);
    }
    Result result = new Result(Lam(resultLambdaArgs, bodyResult.expression), Pi(argumentTypes, bodyResult.type), resultEquations);
    expr.setWellTyped(myLocalContext, result.expression);
    return result;
  }
  */

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    Result[] domainResults = new Result[expr.getArguments().size()];
    int numberOfVars = 0;
    Equations equations = myArgsInference.newEquations();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArguments().get(i).getType(), Universe());
      if (result == null) return null;
      /* TODO
      domainResults[i] = result;
      domainResults[i].equations.lift(-numberOfVars);
      */
      equations.add(domainResults[i].equations);
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        List<String> names = ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames();
        for (int j = 0; j < names.size(); ++j) {
          myLocalContext.add(new TypedBinding(names.get(j), domainResults[i].expression.liftIndex(0, j)));
          ++numberOfVars;
        }
      } else {
        myLocalContext.add(new TypedBinding((Name) null, domainResults[i].expression));
        ++numberOfVars;
      }
    }

    Result codomainResult = typeCheck(expr.getCodomain(), Universe());
    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }
    if (codomainResult == null) return null;

    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (int i = 0; i < domainResults.length; ++i) {
      Universe argUniverse = ((UniverseExpression) domainResults[i].type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        String msg = "Universe " + argUniverse + " of " + (i + 1) + suffix(i + 1) + " argument is not compatible with universe " + universe + " of previous arguments";
        TypeCheckingError error = new TypeCheckingError(msg, expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      universe = maxUniverse;
    }
    Universe codomainUniverse = ((UniverseExpression) codomainResult.type).getUniverse();
    Universe maxUniverse = universe.max(codomainUniverse);
    if (maxUniverse == null) {
      String msg = "Universe " + codomainUniverse + " the codomain is not compatible with universe " + universe + " of arguments";
      TypeCheckingError error = new TypeCheckingError(msg, expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    Expression actualType = new UniverseExpression(maxUniverse);

    /* TODO
    codomainResult.equations.lift(-numberOfVars);
    equations.add(codomainResult.equations);
    */

    List<TypeArgument> resultArguments = new ArrayList<>(domainResults.length);
    for (int i = 0; i < domainResults.length; ++i) {
      resultArguments.add(argFromArgResult(expr.getArguments().get(i), domainResults[i]));
    }
    return checkResult(expectedType, new Result(Pi(resultArguments, codomainResult.expression), actualType, equations), expr);
  }

  private TypeArgument argFromArgResult(Abstract.TypeArgument arg, Result argResult) {
    if (arg instanceof Abstract.TelescopeArgument) {
      return new TelescopeArgument(arg.getExplicit(), ((Abstract.TelescopeArgument) arg).getNames(), argResult.expression);
    } else {
      return new TypeArgument(arg.getExplicit(), argResult.expression);
    }
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(new UniverseExpression(expr.getUniverse()), new UniverseExpression(expr.getUniverse().succ()), null), expr);
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    TypeCheckingError error = new GoalError(myLocalContext, expectedType == null ? null : expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext), expr);
    expr.setWellTyped(myLocalContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    // TODO
    TypeCheckingError error = new ArgInferenceError(expression(), expr, getNames(myLocalContext), null);
    expr.setWellTyped(myLocalContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm;
    if (expectedType != null) {
      expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      if (!(expectedTypeNorm instanceof SigmaExpression || expectedType instanceof InferHoleExpression)) {
        Expression fExpectedTypeNorm = expectedTypeNorm.getFunction(new ArrayList<Expression>());
        if (fExpectedTypeNorm instanceof InferHoleExpression) {
          return new InferErrorResult((InferHoleExpression) fExpectedTypeNorm, ((InferHoleExpression) fExpectedTypeNorm).getError(), null);
        }

        TypeCheckingError error = new TypeMismatchError(expectedTypeNorm, Sigma(typeArgs(TypeArg(Error(null, null)), TypeArg(Error(null, null)))), expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }

      if (expectedTypeNorm instanceof SigmaExpression) {
        InferHoleExpression hole = expectedTypeNorm.accept(new FindHoleVisitor(), null);
        if (hole != null) {
          return new InferErrorResult(hole, hole.getError(), null);
        }

        List<TypeArgument> sigmaArgs = splitArguments(((SigmaExpression) expectedTypeNorm).getArguments());

        if (expr.getFields().size() != sigmaArgs.size()) {
          TypeCheckingError error = new TypeCheckingError("Expected a tuple with " + sigmaArgs.size() + " fields, but given " + expr.getFields().size(), expr, getNames(myLocalContext));
          expr.setWellTyped(myLocalContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Expression expression = Tuple(fields, (SigmaExpression) expectedTypeNorm);
        Equations equations = myArgsInference.newEquations();
        for (int i = 0; i < sigmaArgs.size(); ++i) {
          List<Expression> substExprs = new ArrayList<>(fields.size());
          for (int j = fields.size() - 1; j >= 0; --j) {
            substExprs.add(fields.get(j));
          }

          Expression expType = sigmaArgs.get(i).getType().subst(substExprs, 0);
          Result result = typeCheck(expr.getFields().get(i), expType);
          if (result == null) return null;
          fields.add(result.expression);
          if (result.equations != null) {
            equations.add(result.equations);
          }
        }
        return new Result(expression, expectedType, equations);
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    List<TypeArgument> arguments = new ArrayList<>(expr.getFields().size());
    SigmaExpression type = Sigma(arguments);
    Expression expression = Tuple(fields, type);
    Equations equations = myArgsInference.newEquations();
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (result == null) return null;
      fields.add(result.expression);
      arguments.add(TypeArg(result.type.liftIndex(0, i)));
      if (result.equations != null) {
        equations.add(result.equations);
      }
    }

    /* TODO
    if (expectedTypeNorm instanceof InferHoleExpression) {
      equations.add(new CompareVisitor.Equation((InferHoleExpression) expectedTypeNorm, type));
    }
    */
    return new Result(expression, type, equations);
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression expectedType) {
    Result[] domainResults = new Result[expr.getArguments().size()];
    int numberOfVars = 0;
    Equations equations = myArgsInference.newEquations();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArguments().get(i).getType(), Universe());
      if (result == null) return null;
      domainResults[i] = result;
      /* TODO
      domainResults[i].type = domainResults[i].type.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
      domainResults[i].equations.lift(-numberOfVars);
      equations.add(domainResults[i].equations);
      */
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        List<String> names = ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames();
        for (int j = 0; j < names.size(); ++j) {
          myLocalContext.add(new TypedBinding(names.get(j), domainResults[i].expression.liftIndex(0, j)));
          ++numberOfVars;
        }
      } else {
        myLocalContext.add(new TypedBinding((Name) null, domainResults[i].expression));
        ++numberOfVars;
      }
    }

    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }

    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (int i = 0; i < domainResults.length; ++i) {
      Universe argUniverse = ((UniverseExpression) domainResults[i].type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        String msg = "Universe " + argUniverse + " of " + (i + 1) + suffix(i + 1) + " argument is not compatible with universe " + universe + " of previous arguments";
        TypeCheckingError error = new TypeCheckingError(msg, expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      universe = maxUniverse;
    }
    Expression actualType = new UniverseExpression(universe);

    List<TypeArgument> resultArguments = new ArrayList<>(domainResults.length);
    for (int i = 0; i < domainResults.length; ++i) {
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        resultArguments.add(new TelescopeArgument(expr.getArguments().get(i).getExplicit(), ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames(), domainResults[i].expression));
      } else {
        resultArguments.add(new TypeArgument(expr.getArguments().get(i).getExplicit(), domainResults[i].expression));
      }
    }
    return checkResult(expectedType, new Result(Sigma(resultArguments), actualType, equations), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression expectedType) {
    return checkResult(expectedType, myArgsInference.infer(expr, expectedType), expr);
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Expression expectedType) {
    assert expr.getSequence().isEmpty();
    return typeCheck(expr.getLeft(), expectedType);
  }

  public abstract static class ExpandPatternResult {}

  public static class ExpandPatternOKResult extends ExpandPatternResult {
    public final Expression expression;
    public final Pattern pattern;
    public final int numBindings;

    public ExpandPatternOKResult(Expression expression, Pattern pattern, int numBindings) {
      this.expression = expression;
      this.pattern = pattern;
      this.numBindings = numBindings;
    }
  }

  public static class ExpandPatternErrorResult extends  ExpandPatternResult {
    public final TypeCheckingError error;

    public ExpandPatternErrorResult(TypeCheckingError error) {
      this.error = error;
    }
  }

  public enum PatternExpansionMode {
    FUNCTION, DATATYPE, CONDITION
  }

  private ExpandPatternResult expandPattern(Abstract.Pattern pattern, Binding binding, PatternExpansionMode mode) {
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      if (name == null) {
        myLocalContext.add(binding);
      } else {
        myLocalContext.add(new TypedBinding(((Abstract.NamePattern) pattern).getName(), binding.getType()));
      }
      pattern.setWellTyped(new NamePattern(name));
      return new ExpandPatternOKResult(Index(0), new NamePattern(name), 1);
    } else if (pattern instanceof Abstract.AnyConstructorPattern || pattern instanceof Abstract.ConstructorPattern) {
      TypeCheckingError error = null;

      Expression type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      List<Expression> parameters = new ArrayList<>();
      Expression ftype = type.getFunction(parameters);
      Collections.reverse(parameters);

      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        error = new TypeCheckingError("Pattern expected a data type, got: " + type.prettyPrint(getNames(myLocalContext)), pattern, getNames(myLocalContext));
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }

      DataDefinition dataType = (DataDefinition) ((DefCallExpression) ftype).getDefinition();

      if (mode == PatternExpansionMode.DATATYPE && dataType.getConditions() != null) {
        error = new TypeCheckingError("Pattern matching on a data type with conditions is not allowed here: " + type.prettyPrint(getNames(myLocalContext)), pattern, getNames(myLocalContext));
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }

      if ((mode == PatternExpansionMode.FUNCTION || mode == PatternExpansionMode.DATATYPE) && dataType == Prelude.INTERVAL) {
        error = new TypeCheckingError("Pattern matching on an interval is not allowed here", pattern, getNames(myLocalContext));
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (dataType.getConstructors(parameters, myLocalContext) == null) {
        error = new TypeCheckingError("Elimination is not possible here, cannot determine the set of eligible constructors", pattern, getNames(myLocalContext));
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (pattern instanceof Abstract.AnyConstructorPattern) {
        pattern.setWellTyped(new AnyConstructorPattern());
        myLocalContext.add(binding);
        return new ExpandPatternOKResult(Index(0), new AnyConstructorPattern(), 1);
      }

      Abstract.ConstructorPattern constructorPattern = (Abstract.ConstructorPattern) pattern;

      Constructor constructor = null;
      for (int index = 0; index < dataType.getConstructors().size(); ++index) {
        if (dataType.getConstructors().get(index).getName().equals(constructorPattern.getConstructorName())) {
          constructor = dataType.getConstructors().get(index);
          break;
        }
      }

      if (constructor == null) {
        error = new NotInScopeError(pattern, constructorPattern.getConstructorName());
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (constructor.hasErrors()) {
        error = new HasErrors(constructor.getName(), pattern);
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }

      List<Expression> matchedParameters = null;
      if (constructor.getPatterns() != null) {
        Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), parameters, myLocalContext);
        if (matchResult instanceof PatternMatchMaybeResult) {
          throw new IllegalStateException();
        } else if (matchResult instanceof PatternMatchFailedResult) {
          error = new TypeCheckingError("Constructor is not appropriate, failed to match data type parameters. " +
              "Expected " + ((PatternMatchFailedResult) matchResult).failedPattern + ", got " + ((PatternMatchFailedResult) matchResult).actualExpression.prettyPrint(getNames(myLocalContext)), pattern, getNames(myLocalContext));
        } else if (matchResult instanceof PatternMatchOKResult) {
          matchedParameters = ((PatternMatchOKResult) matchResult).expressions;
        } else {
          throw new IllegalStateException();
        }

        if (error != null) {
          myErrorReporter.report(error);
          return new ExpandPatternErrorResult(error);
        }
      } else {
        matchedParameters = new ArrayList<>(parameters);
      }
      Expression substExpression = ConCall(constructor, matchedParameters);
      Collections.reverse(matchedParameters);
      List<TypeArgument> constructorArguments = new ArrayList<>();
      splitArguments(constructor.getType().subst(matchedParameters, 0), constructorArguments, myLocalContext);

      Utils.ProcessImplicitResult implicitResult = processImplicit(constructorPattern.getArguments(), constructorArguments);
      if (implicitResult.patterns == null) {
        if (implicitResult.numExcessive != 0) {
          error = new TypeCheckingError("Too many arguments: " + implicitResult.numExcessive + " excessive", pattern,
              getNames(myLocalContext));
        } else if (implicitResult.wrongImplicitPosition < constructorPattern.getArguments().size()) {
          error = new TypeCheckingError("Unexpected implicit argument", constructorPattern.getArguments().get(implicitResult.wrongImplicitPosition), getNames(myLocalContext));
        } else {
          error = new TypeCheckingError("Too few explicit arguments, expected: " + implicitResult.numExplicit, pattern, getNames(myLocalContext));
        }
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }
      List<Abstract.PatternArgument> patterns = implicitResult.patterns;

      List<PatternArgument> resultPatterns = new ArrayList<>();
      List<Expression> substituteExpressions = new ArrayList<>();
      int numBindings = 0;
      for (int i = 0; i < constructorArguments.size(); ++i) {
        Expression argumentType = constructorArguments.get(i).getType();
        for (int j = 0; j < i; j++) {
          argumentType = expandPatternSubstitute(resultPatterns.get(j).getPattern(), i - j - 1, substituteExpressions.get(j), argumentType);
        }
        ExpandPatternResult result = expandPattern(patterns.get(i).getPattern(), new TypedBinding((Name) null, argumentType), mode);
        if (result instanceof ExpandPatternErrorResult)
          return result;
        ExpandPatternOKResult okResult  = (ExpandPatternOKResult) result;
        substituteExpressions.add(okResult.expression);
        substExpression = Apps(substExpression.liftIndex(0, okResult.numBindings), okResult.expression);
        resultPatterns.add(new PatternArgument(okResult.pattern, patterns.get(i).isExplicit(), patterns.get(i).isHidden()));
        numBindings += okResult.numBindings;
      }

      pattern.setWellTyped(new ConstructorPattern(constructor, resultPatterns));
      return new ExpandPatternOKResult(substExpression, new ConstructorPattern(constructor, resultPatterns), numBindings);
    } else {
      throw new IllegalStateException();
    }
  }

  public ExpandPatternResult expandPatternOn(Abstract.Pattern pattern, int varIndex, PatternExpansionMode mode) {
    int varContextIndex = myLocalContext.size() - 1 - varIndex;

    Binding binding = myLocalContext.get(varContextIndex);
    List<Binding> tail = new ArrayList<>(myLocalContext.subList(varContextIndex + 1, myLocalContext.size()));
    myLocalContext.subList(varContextIndex, myLocalContext.size()).clear();

    ExpandPatternResult result = expandPattern(pattern, binding, mode);
    if (result instanceof ExpandPatternErrorResult)
      return result;
    ExpandPatternOKResult okResult = (ExpandPatternOKResult) result;
    for (int i = 0; i < tail.size(); i++) {
      myLocalContext.add(new TypedBinding(tail.get(i).getName(), expandPatternSubstitute(okResult.pattern, i, okResult.expression, tail.get(i).getType())));
    }

    return result;
  }

  public Result lookupLocalVar(Abstract.Expression expression, Abstract.Expression expr) {
    Result exprOKResult;
    if (expression instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expression).getExpression() == null && ((Abstract.DefCallExpression) expression).getResolvedName() == null) {
      exprOKResult = myTypeCheckingDefCall.getLocalVar(((Abstract.DefCallExpression) expression).getName(), expression);
    } else if (expression instanceof Abstract.IndexExpression) {
      exprOKResult = getIndex((Abstract.IndexExpression) expression);
    } else {
      TypeCheckingError error = new TypeCheckingError("\\elim can be applied only to a local variable", expression, getNames(myLocalContext));
      myErrorReporter.report(error);
      expr.setWellTyped(myLocalContext, Error(null, error));
      return null;
    }
    return exprOKResult;
  }

  public Result visitElim(Abstract.ElimExpression expr, Expression expectedType) {
    TypeCheckingError error = new TypeCheckingError("\\elim is allowed only at the root of a definition", expr, getNames(myLocalContext));
    myErrorReporter.report(error);
    expr.setWellTyped(myLocalContext, Error(null, error));
    return null;
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Expression expectedType) {
    if (expectedType == null) {
      TypeCheckingError error = new TypeCheckingError("Cannot infer type of the type", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Equations equations = myArgsInference.newEquations();
    Expression letTerm = Index(0);
    List<TypeArgument> args = new ArrayList<>();
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      Result exprResult = typeCheck(expr.getExpressions().get(i), null);
      if (exprResult == null) return null;
      if (exprResult.equations != null) {
        equations.add(exprResult.equations);
      }
      args.add(Tele(vars(Abstract.CaseExpression.ARGUMENT_NAME + i), exprResult.type.liftIndex(0, i)));
      letTerm = Apps(letTerm, exprResult.expression.liftIndex(0, 1));
    }
    for (int i = 0; i < args.size(); i++) {
      myLocalContext.add(new TypedBinding(Abstract.CaseExpression.ARGUMENT_NAME + i, args.get(i).getType()));
    }
    Abstract.ElimExpression elim = wrapCaseToElim(expr);
    ElimTreeNode elimTree = myTypeCheckingElim.typeCheckElim(elim, myLocalContext.size() - args.size(), expectedType.liftIndex(0, 1));
    if (elimTree == null) return null;
    myLocalContext.subList(myLocalContext.size() - expr.getExpressions().size(), myLocalContext.size()).clear();

    LetExpression letExpression = Let(lets(let(Abstract.CaseExpression.FUNCTION_NAME, args, expectedType.liftIndex(0, 1), elimTree)), letTerm);

    expr.setWellTyped(myLocalContext, letExpression);
    return new Result(letExpression, expectedType.liftIndex(0, 1 - expr.getExpressions().size()), equations);
  }


  private Abstract.ElimExpression wrapCaseToElim(final Abstract.CaseExpression expr) {
    final List<Abstract.Expression> expressions = new ArrayList<>();
    for (int i = 0; i < expr.getExpressions().size(); i++)
      expressions.add(Index(expr.getExpressions().size() - 1 - i));
    return new Abstract.ElimExpression() {
      @Override
      public List<Abstract.Expression> getExpressions() {
        return expressions;
      }

      @Override
      public List<? extends Abstract.Clause> getClauses() {
        return expr.getClauses();
      }

      @Override
      public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
        return visitor.visitElim(this, params);
      }

      @Override
      public void setWellTyped(List<Binding> context, Expression wellTyped) {

      }

      @Override
      public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
        accept(new PrettyPrintVisitor(builder, names, 0), prec);
      }
    };
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
    if (!(type instanceof SigmaExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected an type of a sigma type", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    List<TypeArgument> splitArgs = splitArguments(((SigmaExpression) type).getArguments());
    if (expr.getField() < 0 || expr.getField() >= splitArgs.size()) {
      TypeCheckingError error = new TypeCheckingError("Index " + (expr.getField() + 1) + " out of range", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    List<Expression> exprs = new ArrayList<>(expr.getField());
    for (int i = expr.getField() - 1; i >= 0; --i) {
      exprs.add(Proj(exprResult.expression, i));
    }
    return checkResult(expectedType, new Result(Proj(exprResult.expression, expr.getField()), splitArgs.get(expr.getField()).getType().subst(exprs, 0), exprResult.equations), expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression expectedType) {
    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result result = typeCheck(baseClassExpr, null);
    if (result == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = result.expression.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
    if (!(normalizedBaseClassExpr instanceof ClassCallExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected a class", baseClassExpr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassDefinition baseClass = ((ClassCallExpression) normalizedBaseClassExpr).getDefinition();
    if (baseClass.hasErrors()) {
      TypeCheckingError error = new HasErrors(baseClass.getName(), baseClassExpr);
      expr.setWellTyped(myLocalContext, Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    Collection<? extends Abstract.ImplementStatement> statements = expr.getStatements();
    if (statements.isEmpty()) {
      return checkResult(expectedType, new Result(normalizedBaseClassExpr, baseClass.getType(), null), expr);
    }

    class ImplementStatement {
      ClassField classField;
      Abstract.Expression term;

      public ImplementStatement(ClassField classField, Abstract.Expression term) {
        this.classField = classField;
        this.term = term;
      }
    }

    List<ImplementStatement> fields = new ArrayList<>(statements.size());
    for (Abstract.ImplementStatement statement : statements) {
      Abstract.Identifier identifier = statement.getIdentifier();
      Name name = identifier.getName();
      ClassField field = baseClass.removeField(name.name);
      if (field == null) {
        TypeCheckingError error = new TypeCheckingError("Class '" + baseClass.getName() + "' does not have field '" + name + "'", identifier, null);
        myErrorReporter.report(error);
      } else {
        fields.add(new ImplementStatement(field, statement.getExpression()));
      }
    }

    Equations equations = myArgsInference.newEquations();
    Map<ClassField, ClassCallExpression.ImplementStatement> typeCheckedStatements = new HashMap<>();
    for (int i = 0; i < fields.size(); i++) {
      ImplementStatement field = fields.get(i);
      Expression thisExpr = New(ClassCall(baseClass, typeCheckedStatements));
      Result result1 = typeCheck(field.term, field.classField.getBaseType().subst(thisExpr, 0));
      baseClass.addField(field.classField);
      if (result1 == null) {
        for (i++; i < fields.size(); i++) {
          typeCheck(fields.get(i).term, fields.get(i).classField.getBaseType().subst(thisExpr, 0));
          baseClass.addField(fields.get(i).classField);
        }
        return null;
      }

      typeCheckedStatements.put(field.classField, new ClassCallExpression.ImplementStatement(result1.type, result1.expression));
      equations.add(result1.equations);
    }

    ClassCallExpression resultExpr = ClassCall(baseClass, typeCheckedStatements);
    return checkResult(expectedType, new Result(resultExpr, new UniverseExpression(resultExpr.getUniverse()), equations), expr);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
    if (!(normExpr instanceof ClassCallExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected a class", expr.getExpression(), getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassCallExpression classCall = (ClassCallExpression) normExpr;
    if (classCall.getImplementStatements().size() == classCall.getDefinition().getFields().size()) {
      return checkResult(expectedType, new Result(New(normExpr), normExpr, exprResult.equations), expr);
    } else {
      TypeCheckingError error = new TypeCheckingError("Class '" + classCall.getDefinition().getName() + "' has " + classCall.getDefinition().getNumberOfVisibleFields() + " fields", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private LetClauseResult typeCheckLetClause(Abstract.LetClause clause) {
    List<TypeArgument> args = new ArrayList<>();
    Expression resultType;
    ElimTreeNode elimTree;
    Equations equations = myArgsInference.newEquations();

    /* TODO
    try (ContextSaver ignore = new ContextSaver(myLocalContext)) {
      int numVarsPassed = 0;
      for (int i = 0; i < clause.getArguments().size(); i++) {
        if (clause.getArguments().get(i) instanceof Abstract.TypeArgument) {
          Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) clause.getArguments().get(i);
          Result result = typeCheck(typeArgument.getType(), Universe());
          if (result == null) return null;
          args.add(argFromArgResult(typeArgument, result));
          result.equations.lift(-numVarsPassed);
          equations.add(result.equations);
          if (typeArgument instanceof Abstract.TelescopeArgument) {
            List<String> names = ((Abstract.TelescopeArgument) typeArgument).getNames();
            for (int j = 0; j < names.size(); ++j) {
              myLocalContext.add(new TypedBinding(names.get(j), result.expression.liftIndex(0, j)));
              ++numVarsPassed;
            }
          } else {
            myLocalContext.add(new TypedBinding((Name) null, result.expression));
            ++numVarsPassed;
          }
        } else {
          throw new IllegalStateException();
        }
      }

      Expression expectedType = null;
      if (clause.getResultType() != null) {
        Result result = typeCheck(clause.getResultType(), null);
        if (result == null) return null;
        result.equations.lift(-numVarsPassed);
        equations.add(result.equations);
        expectedType = result.expression;
      }

      if (clause.getTerm() instanceof Abstract.ElimExpression)  {
        elimTree = myTypeCheckingElim.typeCheckElim((Abstract.ElimExpression) clause.getTerm(), clause.getArrow() == Abstract.Definition.Arrow.LEFT ? myLocalContext.size() - numVarsPassed : null, expectedType);
        if (elimTree == null)
          return null;
        resultType = expectedType;
      } else {
        Result termResult = typeCheck(clause.getTerm(), expectedType);
        if (termResult == null) return null;
        termResult.equations.lift(-numVarsPassed);
        equations.add(termResult.equations);
        elimTree = new LeafElimTreeNode(clause.getArrow(), termResult.expression);
        resultType = termResult.type;
      }

      TypeCheckingError error = TypeCheckingElim.checkCoverage(clause, myLocalContext, elimTree);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
      error = TypeCheckingElim.checkConditions(clause, myLocalContext, elimTree);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
    }

    LetClause result = new LetClause(clause.getName(), args, resultType, elimTree);
    myLocalContext.add(result);
    return new LetClauseResult(result, equations);
    */
    return null;
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Expression expectedType) {
    /* TODO
    Result finalResult;
    try (ContextSaver ignore = new ContextSaver(myLocalContext)) {
      List<LetClause> clauses = new ArrayList<>();
      Equations equations = myArgsInference.newEquations();
      for (int i = 0; i < expr.getClauses().size(); i++) {
        LetClauseResult clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (clauseResult == null) return null;
        clauseResult.equations.lift(-i);
        equations.add(clauseResult.equations);
        clauses.add(clauseResult.letClause);
      }
      Result result = typeCheck(expr.getExpression(), expectedType == null ? null : expectedType.liftIndex(0, expr.getClauses().size()));
      if (result == null) return null;
      result.equations.lift(-expr.getClauses().size());
      equations.add(result.equations);

      Expression normalizedResultType = result.type.normalize(NormalizeVisitor.Mode.NF, myLocalContext).liftIndex(0, -expr.getClauses().size());
      if (normalizedResultType == null) {
        TypeCheckingError error = new TypeCheckingError("Let result type depends on a bound variable.", expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      finalResult = new Result(Let(clauses, result.expression), normalizedResultType, equations);
    }
    return finalResult;
    */
    return null;
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, Expression expectedType) {
    int number = expr.getNumber();
    Expression expression = Zero();
    for (int i = 0; i < number; ++i) {
      expression = Suc(expression);
    }
    return checkResult(expectedType, new Result(expression, Nat(), null), expr);
  }

  public List<PatternArgument> visitPatternArgs(List<Abstract.PatternArgument> patternArgs, List<Expression> substIn, PatternExpansionMode mode) {
    List<PatternArgument> typedPatterns = new ArrayList<>();
    for (int i = 0; i < patternArgs.size(); i++) {
      ExpandPatternResult result = expandPatternOn(patternArgs.get(i).getPattern(), patternArgs.size() - 1 - i, mode);
      if (result == null || result instanceof ExpandPatternErrorResult)
        return null;

      typedPatterns.add(new PatternArgument(((ExpandPatternOKResult) result).pattern, patternArgs.get(i).isExplicit(), patternArgs.get(i).isHidden()));

      for (int j = 0; j < substIn.size(); j++) {
        substIn.set(j, expandPatternSubstitute(((ExpandPatternOKResult) result).pattern, patternArgs.size() - i - 1, ((ExpandPatternOKResult) result).expression, substIn.get(j)));
      }
    }
    return typedPatterns;
  }
}
