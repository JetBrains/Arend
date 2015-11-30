package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ArgsElimTreeExpander;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ArgsElimTreeExpander.ArgsBranch;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ConditionViolationsCollector;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.CoverageChecker;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ReplaceElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingDefCall;
import com.jetbrains.jetpad.vclang.typechecking.error.*;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.ImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.OldArgsInference;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.*;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.*;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final List<Binding> myLocalContext;
  private final ErrorReporter myErrorReporter;
  private Integer myArgsStartCtxIndex;
  private TypeCheckingDefCall myTypeCheckingDefCall;
  private ImplicitArgsInference myArgsInference;

  private static class Arg {
    boolean isExplicit;
    String name;
    Abstract.Expression expression;

    Arg(boolean isExplicit, String name, Abstract.Expression expression) {
      this.isExplicit = isExplicit;
      this.name = name;
      this.expression = expression;
    }
  }

  public static abstract class Result {
    public Expression expression;
    public List<CompareVisitor.Equation> equations;
  }

  public static class OKResult extends Result {
    public Expression type;

    public OKResult(Expression expression, Expression type, List<CompareVisitor.Equation> equations) {
      this.expression = expression;
      this.type = type;
      this.equations = equations;
    }
  }

  public static class LetClauseResult extends Result {
    LetClause letClause;

    public LetClauseResult(LetClause letClause, List<CompareVisitor.Equation> equations) {
      this.letClause = letClause;
      this.equations = equations;
    }
  }

  public static class InferErrorResult extends Result {
    public TypeCheckingError error;

    public InferErrorResult(InferHoleExpression hole, TypeCheckingError error, List<CompareVisitor.Equation> equations) {
      expression = hole;
      this.error = error;
      this.equations = equations;
    }
  }

  private CheckTypeVisitor(List<Binding> localContext, Integer argsStartCtxIndex, ErrorReporter errorReporter, TypeCheckingDefCall typeCheckingDefCall, ImplicitArgsInference argsInference) {
    myLocalContext = localContext;
    myErrorReporter = errorReporter;
    myArgsStartCtxIndex = argsStartCtxIndex;
    myTypeCheckingDefCall = typeCheckingDefCall;
    myArgsInference = argsInference;
  }

  public static class Builder {
    private final List<Binding> myLocalContext;
    private final ErrorReporter myErrorReporter;
    private Integer myArgsStartCtxIndex;
    private TypeCheckingDefCall myTypeCheckingDefCall;
    private ImplicitArgsInference myArgsInference;
    private ClassDefinition myThisClass;

    public Builder(List<Binding> localContext, ErrorReporter errorReporter) {
      myLocalContext = localContext;
      myErrorReporter = errorReporter;
    }

    public Builder argsStartCtxIndex(Integer index) {
      myArgsStartCtxIndex = index;
      return this;
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
      CheckTypeVisitor visitor = new CheckTypeVisitor(myLocalContext, myArgsStartCtxIndex, myErrorReporter, myTypeCheckingDefCall, myArgsInference);
      if (myTypeCheckingDefCall == null) {
        visitor.myTypeCheckingDefCall = new TypeCheckingDefCall(visitor);
        visitor.myTypeCheckingDefCall.setThisClass(myThisClass);
      }
      if (myArgsInference == null) {
        visitor.myArgsInference = new OldArgsInference(visitor);
      }
      return visitor;
    }
  }

  public void setArgsStartCtxIndex(int index) {
    myArgsStartCtxIndex = index;
  }

  public TypeCheckingDefCall getTypeCheckingDefCall() {
    return myTypeCheckingDefCall;
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

  public Result checkResult(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myLocalContext, result.expression);
      return result;
    }
    Expression actualNorm = result.type.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result1 = compare(expectedNorm, actualNorm, equations);
    if (result1 instanceof CompareVisitor.MaybeResult || result1.isOK() == CompareVisitor.CMP.GREATER || result1.isOK() == CompareVisitor.CMP.EQUALS) {
      if (result.equations != null) {
        result.equations.addAll(equations);
      } else {
        result.equations = equations;
      }
      expression.setWellTyped(myLocalContext, result.expression);
      return result;
    } else {
      TypeCheckingError error = new TypeMismatchError(expectedNorm, actualNorm, expression, getNames(myLocalContext));
      expression.setWellTyped(myLocalContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private Result checkResultImplicit(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myLocalContext, result.expression);
      return result;
    }

    Result result1 = myArgsInference.inferTail(result, expectedType, expression);
    return result1 instanceof OKResult ? checkResult(expectedType, (OKResult) result1, expression) : result1;
  }

  public Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    } else
    if (!(expr instanceof Abstract.ElimExpression) && myArgsStartCtxIndex != null){
      Integer oldArgsStartIndex = myArgsStartCtxIndex;
      myArgsStartCtxIndex = null;
      Result result = expr.accept(this, expectedType);
      myArgsStartCtxIndex = oldArgsStartIndex;
      return result;
    } else {
      return expr.accept(this, expectedType);
    }
  }

  private Result typeCheckLocal(Abstract.Expression expr, Integer argsStartIndex, Expression expectedType) {
    Integer oldArgsStartIndex = myArgsStartCtxIndex;
    myArgsStartCtxIndex = argsStartIndex;
    Result result = typeCheck(expr, expectedType);
    myArgsStartCtxIndex = oldArgsStartIndex;
    return result;
  }

  public OKResult checkType(Abstract.Expression expr, Expression expectedType) {
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;
    if (result instanceof OKResult) return (OKResult) result;
    InferErrorResult errorResult = (InferErrorResult) result;
    myErrorReporter.report(errorResult.error);
    return null;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    Result result = myArgsInference.infer(expr, expectedType);
    return result instanceof OKResult ? checkResult(expectedType, (OKResult) result, expr) : result;
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    Result result = myTypeCheckingDefCall.typeCheckDefCall(expr);
    return result instanceof OKResult ? checkResultImplicit(expectedType, (OKResult) result, expr) : result;
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    OKResult result = getIndex(expr);
    result.type = result.type.liftIndex(0, ((IndexExpression) result.expression).getIndex() + 1);
    return checkResultImplicit(expectedType, result, expr);
  }

  private OKResult getIndex(Abstract.IndexExpression expr) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getType();
    return new OKResult(Index(expr.getIndex()), actualType, null);
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    List<CompareVisitor.Equation> resultEquations = new ArrayList<>();

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
      if (piArgs.get(i) == null && lambdaArgs.get(i).expression == null) {
        TypeCheckingError error = new ArgInferenceError(lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
        errors.add(error);
        if (fresultType instanceof InferHoleExpression) {
          expr.setWellTyped(myLocalContext, Error(null, error));
          return new InferErrorResult((InferHoleExpression) fresultType, error, null);
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression == null) {
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
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression != null) {
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
      if (lambdaArgs.get(i).expression != null) {
        Result argResult = typeCheck(lambdaArgs.get(i).expression, Universe());
        if (!(argResult instanceof OKResult)) {
          while (i-- > 0) {
            myLocalContext.remove(myLocalContext.size() - 1);
          }
          return argResult;
        }
        OKResult okArgResult = (OKResult) argResult;
        addLiftedEquations(okArgResult, resultEquations, i);
        argumentTypes.add(Tele(lambdaArgs.get(i).isExplicit, vars(lambdaArgs.get(i).name), okArgResult.expression));

        if (piArgs.get(i) != null) {
          Expression argExpectedType = piArgs.get(i).getType().normalize(NormalizeVisitor.Mode.NF, myLocalContext);
          Expression argActualType = argumentTypes.get(i).getType().normalize(NormalizeVisitor.Mode.NF, myLocalContext);
          List<CompareVisitor.Equation> equations = new ArrayList<>();
          CompareVisitor.Result result = compare(argExpectedType, argActualType, equations);
          if (result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
            errors.add(new TypeMismatchError(piArgs.get(i).getType(), lambdaArgs.get(i).expression, expr, getNames(myLocalContext)));
          } else {
            for (int j = 0; j < equations.size(); ++j) {
              Expression expression = equations.get(j).expression.liftIndex(0, -i);
              if (expression == null) {
                equations.remove(j--);
              } else {
                equations.set(j, new CompareVisitor.Equation(equations.get(j).hole, expression));
              }
            }
            resultEquations.addAll(equations);
          }
        }
      } else {
        argumentTypes.add(Tele(piArgs.get(i).getExplicit(), vars(lambdaArgs.get(i).name), piArgs.get(i).getType()));
      }
      myLocalContext.add(new TypedBinding(lambdaArgs.get(i).name, argumentTypes.get(i).getType()));
    }

    Result bodyResult = typeCheck(expr.getBody(), fresultType instanceof InferHoleExpression ? null : resultType);

    if (!(bodyResult instanceof OKResult)) {
      for (int i = 0; i < lambdaArgs.size(); ++i) {
        myLocalContext.remove(myLocalContext.size() - 1);
      }
      return bodyResult;
    }
    OKResult okBodyResult = (OKResult) bodyResult;
    addLiftedEquations(okBodyResult, resultEquations, lambdaArgs.size());

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

    for (int i = 0; i < lambdaArgs.size(); ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }

    List<Argument> resultLambdaArgs = new ArrayList<>(argumentTypes.size());
    for (TypeArgument argumentType : argumentTypes) {
      resultLambdaArgs.add(argumentType);
    }
    OKResult result = new OKResult(Lam(resultLambdaArgs, okBodyResult.expression), Pi(argumentTypes, okBodyResult.type), resultEquations);
    expr.setWellTyped(myLocalContext, result.expression);
    return result;
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    OKResult[] domainResults = new OKResult[expr.getArguments().size()];
    int numberOfVars = 0;
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArguments().get(i).getType(), Universe());
      if (!(result instanceof OKResult)) return result;
      domainResults[i] = (OKResult) result;
      addLiftedEquations(domainResults[i], equations, numberOfVars);
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
    if (!(codomainResult instanceof OKResult)) return codomainResult;
    OKResult okCodomainResult = (OKResult) codomainResult;

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
    Universe codomainUniverse = ((UniverseExpression) okCodomainResult.type).getUniverse();
    Universe maxUniverse = universe.max(codomainUniverse);
    if (maxUniverse == null) {
      String msg = "Universe " + codomainUniverse + " the codomain is not compatible with universe " + universe + " of arguments";
      TypeCheckingError error = new TypeCheckingError(msg, expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    Expression actualType = new UniverseExpression(maxUniverse);

    addLiftedEquations(okCodomainResult, equations, numberOfVars);

    List<TypeArgument> resultArguments = new ArrayList<>(domainResults.length);
    for (int i = 0; i < domainResults.length; ++i) {
      resultArguments.add(argFromArgResult(expr.getArguments().get(i), domainResults[i]));
    }
    return checkResult(expectedType, new OKResult(Pi(resultArguments, okCodomainResult.expression), actualType, equations), expr);
  }

  private TypeArgument argFromArgResult(Abstract.TypeArgument arg, OKResult argResult) {
    if (arg instanceof Abstract.TelescopeArgument) {
      return new TelescopeArgument(arg.getExplicit(), ((Abstract.TelescopeArgument) arg).getNames(), argResult.expression);
    } else {
      return new TypeArgument(arg.getExplicit(), argResult.expression);
    }
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    return checkResult(expectedType, new OKResult(new UniverseExpression(expr.getUniverse()), new UniverseExpression(expr.getUniverse().succ()), null), expr);
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    TypeCheckingError error = new GoalError(myLocalContext, expectedType == null ? null : expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext), expr);
    return new InferErrorResult(new InferHoleExpression(error), error, null);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    TypeCheckingError error = new ArgInferenceError(expression(), expr, getNames(myLocalContext), null);
    expr.setWellTyped(myLocalContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType != null) {
      expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      if (!(expectedTypeNorm instanceof SigmaExpression || expectedType instanceof InferHoleExpression)) {
        Expression fExpectedTypeNorm = expectedTypeNorm.getFunction(new ArrayList<Expression>());
        if (fExpectedTypeNorm instanceof InferHoleExpression) {
          return new InferErrorResult((InferHoleExpression) fExpectedTypeNorm, ((InferHoleExpression) fExpectedTypeNorm).getError(), null);
        }

        TypeCheckingError error = new TypeMismatchError(expectedTypeNorm, Sigma(args(TypeArg(Error(null, null)), TypeArg(Error(null, null)))), expr, getNames(myLocalContext));
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
        List<CompareVisitor.Equation> equations = new ArrayList<>();
        for (int i = 0; i < sigmaArgs.size(); ++i) {
          List<Expression> substExprs = new ArrayList<>(fields.size());
          for (int j = fields.size() - 1; j >= 0; --j) {
            substExprs.add(fields.get(j));
          }

          Expression expType = sigmaArgs.get(i).getType().subst(substExprs, 0);
          Result result = typeCheck(expr.getFields().get(i), expType);
          if (!(result instanceof OKResult)) return result;
          OKResult okResult = (OKResult) result;
          fields.add(okResult.expression);
          if (okResult.equations != null) {
            equations.addAll(okResult.equations);
          }
        }
        return new OKResult(expression, expectedType, equations);
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    List<TypeArgument> arguments = new ArrayList<>(expr.getFields().size());
    SigmaExpression type = Sigma(arguments);
    Expression expression = Tuple(fields, type);
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (!(result instanceof OKResult)) return result;
      OKResult okResult = (OKResult) result;
      fields.add(okResult.expression);
      arguments.add(TypeArg(okResult.type.liftIndex(0, i)));
      if (okResult.equations != null) {
        equations.addAll(okResult.equations);
      }
    }

    if (expectedTypeNorm instanceof InferHoleExpression) {
      equations.add(new CompareVisitor.Equation((InferHoleExpression) expectedTypeNorm, type));
    }
    return new OKResult(expression, type, equations);
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression expectedType) {
    OKResult[] domainResults = new OKResult[expr.getArguments().size()];
    int numberOfVars = 0;
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArguments().get(i).getType(), Universe());
      if (!(result instanceof OKResult)) return result;
      domainResults[i] = (OKResult) result;
      addLiftedEquations(domainResults[i], equations, numberOfVars);
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
    return checkResult(expectedType, new OKResult(Sigma(resultArguments), actualType, equations), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression expectedType) {
    Result result = myArgsInference.infer(expr, expectedType);
    return result instanceof OKResult ? checkResult(expectedType, (OKResult) result, expr) : result;
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

  private ExpandPatternResult expandPattern(Abstract.Pattern pattern, Binding binding) {
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      if (name == null) {
        myLocalContext.add(binding);
      } else {
        myLocalContext.add(new TypedBinding(((Abstract.NamePattern) pattern).getName(), binding.getType()));
      }
      pattern.setWellTyped(new NamePattern(name, pattern.getExplicit()));
      return new ExpandPatternOKResult(Index(0), new NamePattern(name, pattern.getExplicit()), 1);
    } else if (pattern instanceof Abstract.AnyConstructorPattern) {
      Expression type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      Expression ftype = type.getFunction(new ArrayList<Expression>());
      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        TypeCheckingError error = new TypeCheckingError("Pattern expected a data type, got: " + type.prettyPrint(getNames(myLocalContext)), pattern, getNames(myLocalContext));
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }
      pattern.setWellTyped(new AnyConstructorPattern(pattern.getExplicit()));
      myLocalContext.add(binding);
      return new ExpandPatternOKResult(Index(0), new AnyConstructorPattern(pattern.getExplicit()), 1);
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      TypeCheckingError error = null;
      Abstract.ConstructorPattern constructorPattern = (Abstract.ConstructorPattern) pattern;

      List<Expression> parameters = new ArrayList<>();
      Expression type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      Expression ftype = type.getFunction(parameters);
      Collections.reverse(parameters);
      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        error = new TypeCheckingError("Pattern expected a data type, got: " + type.prettyPrint(getNames(myLocalContext)), pattern, getNames(myLocalContext));
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }
      DataDefinition dataType = (DataDefinition) ((DefCallExpression) ftype).getDefinition();

      Constructor constructor = null;
      for (int index = 0; index < dataType.getConstructors().size(); ++index) {
        if (dataType.getConstructors().get(index).getName().equals(constructorPattern.getConstructorName())) {
          constructor = dataType.getConstructors().get(index);
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
          error = new TypeCheckingError("Constructor is not appropriate, failed to match data type parameters. " +
              "Expected " + ((PatternMatchMaybeResult) matchResult).maybePattern + ", got " + ((PatternMatchMaybeResult) matchResult).actualExpression.prettyPrint(getNames(myLocalContext)), pattern, getNames(myLocalContext));
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

      Utils.ProcessImplicitResult implicitResult = processImplicit(constructorPattern.getPatterns(), constructorArguments);
      if (implicitResult.patterns == null) {
        if (implicitResult.numExcessive != 0) {
          error = new TypeCheckingError("Too many arguments: " + implicitResult.numExcessive + " excessive", constructorPattern,
              getNames(myLocalContext));
        } else if (implicitResult.wrongImplicitPosition < constructorPattern.getPatterns().size()) {
          error = new TypeCheckingError("Unexpected implicit argument", constructorPattern.getPatterns().get(implicitResult.wrongImplicitPosition), getNames(myLocalContext));
        } else {
          error = new TypeCheckingError("Too few explicit arguments, expected: " + implicitResult.numExplicit, constructorPattern, getNames(myLocalContext));
        }
        myErrorReporter.report(error);
        return new ExpandPatternErrorResult(error);
      }
      List<Abstract.Pattern> patterns = implicitResult.patterns;

      List<Pattern> resultPatterns = new ArrayList<>();
      List<Expression> substituteExpressions = new ArrayList<>();
      int numBindings = 0;
      for (int i = 0; i < constructorArguments.size(); ++i) {
        Expression argumentType = constructorArguments.get(i).getType();
        for (int j = 0; j < i; j++) {
          argumentType = expandPatternSubstitute(resultPatterns.get(j), i - j - 1, substituteExpressions.get(j), argumentType);
        }
        ExpandPatternResult result = expandPattern(patterns.get(i), new TypedBinding((Name) null, argumentType));
        if (result instanceof ExpandPatternErrorResult)
          return result;
        ExpandPatternOKResult okResult  = (ExpandPatternOKResult) result;
        substituteExpressions.add(okResult.expression);
        substExpression = Apps(substExpression.liftIndex(0, okResult.numBindings), okResult.expression);
        resultPatterns.add(okResult.pattern);
        numBindings += okResult.numBindings;
      }

      pattern.setWellTyped(new ConstructorPattern(constructor, resultPatterns, pattern.getExplicit()));
      return new ExpandPatternOKResult(substExpression, new ConstructorPattern(constructor, resultPatterns, pattern.getExplicit()), numBindings);
    } else {
      throw new IllegalStateException();
    }
  }

  public ExpandPatternResult expandPatternOn(Abstract.Pattern pattern, int varIndex) {
    int varContextIndex = myLocalContext.size() - 1 - varIndex;

    Binding binding = myLocalContext.get(varContextIndex);
    List<Binding> tail = new ArrayList<>(myLocalContext.subList(varContextIndex + 1, myLocalContext.size()));
    myLocalContext.subList(varContextIndex, myLocalContext.size()).clear();

    ExpandPatternResult result = expandPattern(pattern, binding);
    if (result instanceof ExpandPatternErrorResult)
      return result;
    ExpandPatternOKResult okResult = (ExpandPatternOKResult) result;
    for (int i = 0; i < tail.size(); i++) {
      myLocalContext.add(new TypedBinding(tail.get(i).getName(), expandPatternSubstitute(okResult.pattern, i, okResult.expression, tail.get(i).getType())));
    }

    return result;
  }

  public OKResult lookupLocalVar(Abstract.Expression expression, Abstract.Expression expr) {
    OKResult exprOKResult;
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

  @Override
  public Result visitElim(final Abstract.ElimExpression expr, Expression expectedType) {
    TypeCheckingError error = null;
    if (expectedType == null) {
      error = new TypeCheckingError("Cannot infer type of the expression", expr, getNames(myLocalContext));
    }
    if (myArgsStartCtxIndex == null && error == null) {
      error = new TypeCheckingError("\\elim is allowed only at the root of a definition", expr, getNames(myLocalContext));
    }

    if (error != null) {
      myErrorReporter.report(error);
      expr.setWellTyped(myLocalContext, Error(null, error));
      return null;
     }

    final List<IndexExpression> elimExprs = new ArrayList<>(expr.getExpressions().size());
    for (Abstract.Expression var : expr.getExpressions()){
      OKResult exprOKResult = lookupLocalVar(var, expr);
      if (exprOKResult == null) {
        return null;
      }

      if (myLocalContext.size() - 1 - ((IndexExpression) exprOKResult.expression).getIndex() < myArgsStartCtxIndex) {
        error = new TypeCheckingError("\\elim can be applied only to arguments of the innermost definition", var, getNames(myLocalContext));
        myErrorReporter.report(error);
        var.setWellTyped(myLocalContext, Error(null, error));
        return null;
      }

      if (!elimExprs.isEmpty() && ((IndexExpression) exprOKResult.expression).getIndex() >= elimExprs.get(elimExprs.size() - 1).getIndex()) {
        error = new TypeCheckingError("Variable elimination must be in the order of variable introduction", var, getNames(myLocalContext));
        myErrorReporter.report(error);
        var.setWellTyped(myLocalContext, Error(null, error));
        return null;
      }

      Expression ftype = exprOKResult.type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext).getFunction(new ArrayList<Expression>());
      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        error = new TypeCheckingError("Elimination is allowed only for a data type variable.", var, getNames(myLocalContext));
        myErrorReporter.report(error);
        var.setWellTyped(myLocalContext, Error(null, error));
        return null;
      }
      elimExprs.add((IndexExpression) exprOKResult.expression);
    }

    Result errorResult = null;
    boolean wasError = false;

    List<Clause> clauses = new ArrayList<>();
    List<List<Pattern>> emptyPatterns = new ArrayList<>();
    List<Abstract.Clause> emptyClauses = new ArrayList<>();
    clause_loop:
    for (Abstract.Clause clause : expr.getClauses()) {
      try (CompleteContextSaver ignore = new CompleteContextSaver<>(myLocalContext)) {
        List<Pattern> patterns = new ArrayList<>();
        Expression clauseExpectedType = expectedType;
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          ExpandPatternResult result = expandPatternOn(clause.getPatterns().get(i), elimExprs.get(i).getIndex());
          if (result instanceof ExpandPatternErrorResult) {
            expr.getExpressions().get(i).setWellTyped(myLocalContext, Error(null, ((ExpandPatternErrorResult) result).error));
            wasError = true;
            continue clause_loop;
          }
          ExpandPatternOKResult okResult = (ExpandPatternOKResult) result;
          patterns.add(okResult.pattern);
          clauseExpectedType = expandPatternSubstitute(okResult.pattern, elimExprs.get(i).getIndex(), okResult.expression, clauseExpectedType);
        }
        if (clause.getExpression() == null) {
          emptyPatterns.add(patterns);
          emptyClauses.add(clause);
          continue;
        }
        Result clauseResult = typeCheckLocal(clause.getExpression(), clause.getArrow() == Abstract.Definition.Arrow.RIGHT ? null : myArgsStartCtxIndex, clauseExpectedType);
        if (!(clauseResult instanceof OKResult)) {
          wasError = true;
          if (errorResult == null) {
            errorResult = clauseResult;
          } else if (clauseResult instanceof InferErrorResult) {
            myErrorReporter.report(((InferErrorResult) errorResult).error);
            errorResult = clauseResult;
          }
        } else {
          clauses.add(new Clause(patterns, clause.getArrow(), clauseResult.expression, null));
        }
      }
    }

    if (wasError) {
      return errorResult;
    }

    if (clauses.isEmpty() && emptyClauses.isEmpty()) {
      if (!new CoverageChecker<Void>(myLocalContext).checkEmptyContext(elimExprs.get(0).getIndex())) {
        error = new TypeCheckingError("Empty elim, while there are non-empty data types", expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
    } else {
      List<List<Pattern>> patterns = new ArrayList<>();
      List<Expression> types = new ArrayList<>();
      for (int i = elimExprs.get(0).getIndex(); i >= 0; i--) {
        patterns.add(new ArrayList<Pattern>(Collections.nCopies(clauses.size() + emptyPatterns.size(), match(null))));
        types.add(myLocalContext.get(myLocalContext.size() - 1 - i).getType());
      }

      for (int j = 0; j < clauses.size(); j++) {
       for (int i = 0; i < clauses.get(j).getPatterns().size(); i++) {
          patterns.get(types.size() - 1 - elimExprs.get(i).getIndex()).set(j, clauses.get(j).getPatterns().get(i));
        }
      }

      for (int j = 0; j < emptyPatterns.size(); j++) {
        for (int i = 0; i < emptyPatterns.get(j).size(); i++) {
          patterns.get(types.size() - 1 - elimExprs.get(i).getIndex()).set(clauses.size() + j, emptyPatterns.get(j).get(i));
        }
      }

      List<Binding> tail = new ArrayList<>(myLocalContext.subList(myLocalContext.size() - types.size(), myLocalContext.size()));
      myLocalContext.subList(myLocalContext.size() - types.size(), myLocalContext.size()).clear();
      ArgsElimTreeExpander.ArgsExpansionResult treeExpansionResult = new ArgsElimTreeExpander(myLocalContext).expandElimTree(types, patterns, clauses.size() + emptyPatterns.size());
      myLocalContext.addAll(tail);

      for (int i = 0; i < emptyPatterns.size(); i++) {
        for (ArgsBranch branch : treeExpansionResult.branches) {
          if (branch.leaf.getValue().contains(clauses.size() + i)) {
            error = new TypeCheckingError("Empty clause is reachable", emptyClauses.get(i), getNames(myLocalContext));
            expr.setWellTyped(myLocalContext, Error(null, error));
            myErrorReporter.report(error);
            wasError = true;
          }
        }
      }

      if (wasError) {
        return null;
      }

      Map<List<Integer>, Clause> clauseMap = new IdentityHashMap<>();
      for (ArgsBranch branch : treeExpansionResult.branches) {
        clauseMap.put(branch.leaf.getValue(), clauses.get(branch.leaf.getValue().get(0)));
      }
      ElimTreeNode<Clause> treeNode = treeExpansionResult.tree.accept(new ReplaceElimTreeNodeVisitor<List<Integer>, Clause>(), clauseMap);

      StringBuilder coverageCheckMsg = new StringBuilder();
      coverageCheckMsg.append("Coverage checking failed: \n");
      for (List<Pattern> failed : treeNode.accept(new CoverageChecker<Clause>(myLocalContext), true)) {
        coverageCheckMsg.append("missing pattern: ");
        for (IndexExpression elimIdx : elimExprs) {
          coverageCheckMsg.append(failed.get(elimIdx.getIndex())).append(" ");
        }
        coverageCheckMsg.append("\n");
        wasError = true;
      }

      if (wasError) {
        error = new TypeCheckingError(coverageCheckMsg.toString(), expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }

      class ClauseConditionChecker implements ConditionViolationsCollector.ConditionViolationChecker<Clause> {
        boolean wasError = false;

        @Override
        public void check(List<Binding> context, Clause v1, List<Expression> subst1, Clause v2, List<Expression> subst2) {
          Expression expr1 = v1.getExpression().subst(match(v1, subst1), 0).normalize(NormalizeVisitor.Mode.NF, context);
          Expression expr2 = v2.getExpression().subst(match(v2, subst2), 0).normalize(NormalizeVisitor.Mode.NF, context);
          if (!expr1.equals(expr2)){
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Condition check failed: \n");
            errorMsg.append(" 1)");
            for (IndexExpression expr : elimExprs) {
              errorMsg.append(" (").append(subst1.get(expr.getIndex())).append(")");
            }
            errorMsg.append(" = ").append(expr1).append("\n");
            errorMsg.append(" 2)");
            for (IndexExpression expr : elimExprs) {
              errorMsg.append(" (").append(subst2.get(expr.getIndex())).append(")");
            }
            errorMsg.append(" = ").append(expr2).append("\n");

            TypeCheckingError error = new TypeCheckingError(errorMsg.toString(), expr, getNames(myLocalContext));
            myErrorReporter.report(error);
            wasError = true;
          }
        }

        private List<Expression> match(Clause v, List<Expression> subst) {
          List<Expression> result = new ArrayList<>(subst);
          for (int i = 0; i < elimExprs.size(); i++) {
            int var = elimExprs.get(i).getIndex();
            List<Expression> matched1 = ((PatternMatchOKResult) v.getPatterns().get(i).match(result.get(var))).expressions;
            result.remove(var);
            result.addAll(var, matched1);
          }
          return result;
        }
      }

      ClauseConditionChecker checker = new ClauseConditionChecker();
      ConditionViolationsCollector.check(myLocalContext, treeNode, checker);
      if (checker.wasError)
        return null;
    }

    ElimExpression result = new ElimExpression(elimExprs, clauses);
    for (Clause clause : clauses) {
      clause.setElimExpression(result);
    }

    expr.setWellTyped(myLocalContext, result);
    return new OKResult(result, expectedType, null);
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Expression expectedType) {
    if (expectedType == null) {
      TypeCheckingError error = new TypeCheckingError("Cannot infer type of the expression", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    List<CompareVisitor.Equation> equations = new ArrayList<>();
    Expression letTerm = Index(0);
    List<Argument> args = new ArrayList<>();
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      Result exprResult = typeCheck(expr.getExpressions().get(i), null);
      if (!(exprResult instanceof OKResult)) return exprResult;
      OKResult exprOKResult = (OKResult) exprResult;
      if (exprOKResult.equations != null) {
        equations.addAll(exprOKResult.equations);
      }
      args.add(Tele(vars("caseA" + i), exprOKResult.type.liftIndex(0, i)));
      letTerm = Apps(letTerm, exprOKResult.expression.liftIndex(0, 1));
    }
    for (int i = 0; i < args.size(); i++) {
      myLocalContext.add(new TypedBinding("caseA" + i, ((TelescopeArgument) args.get(i)).getType()));
    }
    Abstract.ElimExpression elim = wrapCaseToElim(expr);
    Integer oldArgsStartCtxIndex = myArgsStartCtxIndex;
    myArgsStartCtxIndex = myLocalContext.size() - args.size();
    Result elimResult = visitElim(elim, expectedType.liftIndex(0, 1));
    myArgsStartCtxIndex = oldArgsStartCtxIndex;
    if (!(elimResult instanceof OKResult)) return elimResult;
    OKResult elimOKResult = (OKResult) elimResult;
    addLiftedEquations(elimOKResult, equations, 1);
    myLocalContext.subList(myLocalContext.size() - expr.getExpressions().size(), myLocalContext.size()).clear();

    LetExpression letExpression = Let(lets(let("caseF", args, elimOKResult.type,
        Abstract.Definition.Arrow.LEFT, elimOKResult.expression)), letTerm);

    expr.setWellTyped(myLocalContext, letExpression);
    return new OKResult(letExpression, elimOKResult.type.liftIndex(0, -expr.getExpressions().size()), equations);
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
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression type = okExprResult.type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
    if (!(type instanceof SigmaExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected an expression of a sigma type", expr, getNames(myLocalContext));
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
      exprs.add(Proj(okExprResult.expression, i));
    }
    return checkResult(expectedType, new OKResult(Proj(okExprResult.expression, expr.getField()), splitArgs.get(expr.getField()).getType().subst(exprs, 0), okExprResult.equations), expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression expectedType) {
    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result result = typeCheck(baseClassExpr, null);
    if (!(result instanceof OKResult)) {
      return result;
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
      return checkResult(expectedType, new OKResult(normalizedBaseClassExpr, baseClass.getType(), null), expr);
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

    List<CompareVisitor.Equation> equations = null;
    Map<ClassField, ClassCallExpression.ImplementStatement> typeCheckedStatements = new HashMap<>();
    for (int i = 0; i < fields.size(); i++) {
      ImplementStatement field = fields.get(i);
      Expression thisExpr = New(ClassCall(baseClass, typeCheckedStatements));
      Result result1 = typeCheck(field.term, field.classField.getBaseType().subst(thisExpr, 0));
      baseClass.addField(field.classField);
      if (!(result1 instanceof OKResult)) {
        for (i++; i < fields.size(); i++) {
          typeCheck(fields.get(i).term, fields.get(i).classField.getBaseType().subst(thisExpr, 0));
          baseClass.addField(fields.get(i).classField);
        }
        return result1;
      }

      OKResult okResult = (OKResult) result1;
      typeCheckedStatements.put(field.classField, new ClassCallExpression.ImplementStatement(okResult.type, okResult.expression));
      if (okResult.equations != null) {
        if (equations == null) {
          equations = okResult.equations;
        } else {
          equations.addAll(okResult.equations);
        }
      }
    }

    if (equations != null) {
      for (int i = 0; i < equations.size(); ++i) {
        Expression expression = equations.get(i).expression.liftIndex(0, -1);
        if (expression == null) {
          equations.remove(i--);
        } else {
          equations.set(i, new CompareVisitor.Equation(equations.get(i).hole, expression));
        }
      }
    }

    ClassCallExpression resultExpr = ClassCall(baseClass, typeCheckedStatements);
    return checkResult(expectedType, new OKResult(resultExpr, new UniverseExpression(resultExpr.getUniverse()), equations), expr);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression normExpr = okExprResult.expression.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
    if (!(normExpr instanceof ClassCallExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected a class", expr.getExpression(), getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassCallExpression classCall = (ClassCallExpression) normExpr;
    if (classCall.getImplementStatements().size() == classCall.getDefinition().getFields().size()) {
      return checkResult(expectedType, new OKResult(New(normExpr), normExpr, okExprResult.equations), expr);
    } else {
      TypeCheckingError error = new TypeCheckingError("Class '" + classCall.getDefinition().getName() + "' has " + classCall.getDefinition().getNumberOfVisibleFields() + " fields", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private Result typeCheckLetClause(Abstract.LetClause clause) {
    List<Argument> args = new ArrayList<>();
    Expression resultType;
    Expression term;
    List<CompareVisitor.Equation> equations = new ArrayList<>();

    try (ContextSaver ignore = new ContextSaver(myLocalContext)) {
      int numVarsPassed = 0;
      for (int i = 0; i < clause.getArguments().size(); i++) {
        if (clause.getArguments().get(i) instanceof Abstract.TypeArgument) {
          Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) clause.getArguments().get(i);
          Result result = typeCheck(typeArgument.getType(), Universe());
          if (!(result instanceof OKResult)) return result;
          OKResult okResult = (OKResult) result;
          args.add(argFromArgResult(typeArgument, okResult));
          addLiftedEquations(okResult, equations, numVarsPassed);
          if (typeArgument instanceof Abstract.TelescopeArgument) {
            List<String> names = ((Abstract.TelescopeArgument) typeArgument).getNames();
            for (int j = 0; j < names.size(); ++j) {
              myLocalContext.add(new TypedBinding(names.get(j), okResult.expression.liftIndex(0, j)));
              ++numVarsPassed;
            }
          } else {
            myLocalContext.add(new TypedBinding((Name) null, okResult.expression));
            ++numVarsPassed;
          }
        } else {
          throw new IllegalStateException();
        }
      }

      Expression expectedType = null;
      if (clause.getResultType() != null) {
        Result result = typeCheck(clause.getResultType(), null);
        if (!(result instanceof OKResult)) return result;
        addLiftedEquations(result, equations, numVarsPassed);
        expectedType = result.expression;
      }
      Result termResult = typeCheckLocal(clause.getTerm(), myLocalContext.size() - numVarsPassed, expectedType);
      if (!(termResult instanceof OKResult)) return termResult;
      addLiftedEquations(termResult, equations, numVarsPassed);

      term = ((OKResult) termResult).expression;
      resultType = ((OKResult) termResult).type;
    }

    LetClause result = new LetClause(clause.getName(), args, resultType, clause.getArrow(), term);
    myLocalContext.add(result);
    return new LetClauseResult(result, equations);
  }

  private void addLiftedEquations(Result okResult, List<CompareVisitor.Equation> equations, int numVarsPassed) {
    if (okResult.equations != null) {
      for (CompareVisitor.Equation equation : okResult.equations) {
        Expression expr1 = equation.expression.liftIndex(0, -numVarsPassed);
        if (expr1 != null) {
          equations.add(new CompareVisitor.Equation(equation.hole, expr1));
        }
      }
    }
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Expression expectedType) {
    OKResult finalResult;
    try (ContextSaver ignore = new ContextSaver(myLocalContext)) {
      List<LetClause> clauses = new ArrayList<>();
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      for (int i = 0; i < expr.getClauses().size(); i++) {
        Result clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (!(clauseResult instanceof LetClauseResult)) return clauseResult;
        addLiftedEquations(clauseResult, equations, i);
        clauses.add(((LetClauseResult) clauseResult).letClause);
      }
      Result result = typeCheckLocal(expr.getExpression(), null, expectedType == null ? null : expectedType.liftIndex(0, expr.getClauses().size()));
      if (!(result instanceof OKResult)) return result;
      OKResult okResult = (OKResult) result;
      addLiftedEquations(okResult, equations, expr.getClauses().size());

      Expression normalizedResultType = okResult.type.normalize(NormalizeVisitor.Mode.NF, myLocalContext).liftIndex(0, -expr.getClauses().size());
      if (normalizedResultType == null) {
        TypeCheckingError error = new TypeCheckingError("Let result type depends on a bound variable.", expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      finalResult = new OKResult(Let(clauses, okResult.expression), normalizedResultType, equations);
    }
    return finalResult;
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, Expression expectedType) {
    int number = expr.getNumber();
    Expression expression = Zero();
    for (int i = 0; i < number; ++i) {
      expression = Suc(expression);
    }
    return checkResult(expectedType, new OKResult(expression, Nat(), null), expr);
  }

  public List<Pattern> visitPatterns(List<Abstract.Pattern> patterns, List<Expression> substIn) {
    List<Pattern> typedPatterns;
    typedPatterns = new ArrayList<>();
    for (int i = 0; i < patterns.size(); i++) {
      ExpandPatternResult result = expandPatternOn(patterns.get(i), patterns.size() - 1 - i);
      if (result == null || result instanceof ExpandPatternErrorResult)
        return null;

      typedPatterns.add(((ExpandPatternOKResult) result).pattern);

      for (int j = 0; j < substIn.size(); j++) {
        substIn.set(j, expandPatternSubstitute(((ExpandPatternOKResult) result).pattern, patterns.size() - i - 1, ((ExpandPatternOKResult) result).expression, substIn.get(j)));
      }
    }
    return typedPatterns;
  }
}
