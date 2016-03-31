package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.StringPrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.*;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingDefCall;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingResult;
import com.jetbrains.jetpad.vclang.typechecking.error.*;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.ImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.StdImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.size;
import static com.jetbrains.jetpad.vclang.term.definition.BaseDefinition.Helper.toNamespaceMember;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.expression;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final List<Binding> myContext;
  private final ErrorReporter myErrorReporter;
  private TypeCheckingDefCall myTypeCheckingDefCall;
  private TypeCheckingElim myTypeCheckingElim;
  private ImplicitArgsInference myArgsInference;

  public static class Result extends TypeCheckingResult {
    public Expression expression;
    public Expression type;

    public Result(Expression expression, Expression type) {
      this.expression = expression;
      this.type = type;
    }

    @Override
    public void subst(Substitution substitution) {
      expression = expression.subst(substitution);
      type = type.subst(substitution);
    }
  }

  public static class LetClauseResult extends TypeCheckingResult {
    LetClause letClause;

    public LetClauseResult(LetClause letClause) {
      this.letClause = letClause;
    }

    @Override
    public void subst(Substitution substitution) {
      letClause = letClause.subst(substitution);
    }
  }

  private CheckTypeVisitor(List<Binding> localContext, ErrorReporter errorReporter, TypeCheckingDefCall typeCheckingDefCall, ImplicitArgsInference argsInference) {
    myContext = localContext;
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
    private Expression myThisExpr;

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

    public Builder thisClass(ClassDefinition thisClass, Expression thisExpr) {
      myThisClass = thisClass;
      myThisExpr = thisExpr;
      return this;
    }

    public CheckTypeVisitor build() {
      CheckTypeVisitor visitor = new CheckTypeVisitor(myLocalContext, myErrorReporter, myTypeCheckingDefCall, myArgsInference);
      if (myTypeCheckingDefCall == null) {
        visitor.myTypeCheckingDefCall = new TypeCheckingDefCall(visitor);
        visitor.myTypeCheckingDefCall.setThisClass(myThisClass, myThisExpr);
      }
      visitor.myTypeCheckingElim = new TypeCheckingElim(visitor);
      if (myArgsInference == null) {
        visitor.myArgsInference = new StdImplicitArgsInference(visitor);
      }
      return visitor;
    }
  }

  public TypeCheckingDefCall getTypeCheckingDefCall() {
    return myTypeCheckingDefCall;
  }

  public ImplicitArgsInference getImplicitArgsInference() {
    return myArgsInference;
  }

  public TypeCheckingElim getTypeCheckingElim() {
    return myTypeCheckingElim;
  }

  public void setThisClass(ClassDefinition thisClass, Expression thisExpr) {
    myTypeCheckingDefCall.setThisClass(thisClass, thisExpr);
  }

  public ClassDefinition getThisClass() {
    return myTypeCheckingDefCall.getThisClass();
  }

  public Expression getThisExpression() {
    return myTypeCheckingDefCall.getThisExpression();
  }

  public List<Binding> getContext() {
    return myContext;
  }

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  public Result checkResult(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    }

    if (compare(result, expectedType, Equations.CMP.GE, expression)) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    } else {
      return null;
    }
  }

  public boolean compare(Result result, Expression expectedType, Equations.CMP cmp, Abstract.Expression expr) {
    if (result.getEquations() instanceof DummyEquations) {
      result.setEquations(myArgsInference.newEquations());
    }

    Expression expectedType1 = expectedType.normalize(NormalizeVisitor.Mode.NF);
    Expression actualType = result.type.normalize(NormalizeVisitor.Mode.NF);

    if (expectedType1.isAnyUniverse()) {
      if (actualType.toUniverse() != null) {
        return true;
      }

      InferenceBinding lvl = new LevelInferenceBinding("lvl", Level(), expr);
      result.addUnsolvedVariable(lvl);
      expectedType1 = Universe(Reference(lvl));
    }

    if (CompareVisitor.compare(result.getEquations(), cmp, expectedType1, actualType, expr)) {
      result.expression = new OfTypeExpression(result.expression, expectedType1);
      return true;
    } else {
      TypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
      expr.setWellTyped(myContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return false;
    }
  }

  public Result checkResultImplicit(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    }
    return myArgsInference.inferTail(result, expectedType, expression);
  }

  public Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      TypeCheckingError error = new TypeCheckingError("Incomplete expression", null);
      myErrorReporter.report(error);
      return null;
    }
    return expr.accept(this, expectedType);
  }

  public Result checkType(Abstract.Expression expr, Expression expectedType) {
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;
    result.update();
    result.reportErrors(myErrorReporter);
    result.expression = result.expression.strip();
    result.type = result.type.strip();
    return result;
  }

  private boolean compareExpressions(Result result, Expression expected, Expression actual, Abstract.Expression expr) {
    if (result.getEquations() instanceof DummyEquations) {
      result.setEquations(myArgsInference.newEquations());
    }
    if (!CompareVisitor.compare(result.getEquations(), Equations.CMP.EQ, expected.normalize(NormalizeVisitor.Mode.NF), actual.normalize(NormalizeVisitor.Mode.NF), expr)) {
      TypeCheckingError error = new SolveEquationsError(expected.normalize(NormalizeVisitor.Mode.HUMAN_NF), actual.normalize(NormalizeVisitor.Mode.HUMAN_NF), null, expr);
      expr.setWellTyped(myContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return false;
    }
    return true;
  }

  private boolean checkPath(Result result, Abstract.Expression expr) {
    if (result != null) {
      ConCallExpression conExpr = result.expression.getFunction().toConCall();
      if (conExpr != null && Prelude.isPathCon(conExpr.getDefinition())) {
        result.expression = result.expression.normalize(NormalizeVisitor.Mode.WHNF);
        if (result.expression.getArguments().isEmpty()) {
          TypeCheckingError error = new TypeCheckingError("Expected an argument for 'path'", expr);
          expr.setWellTyped(myContext, Error(result.expression, error));
          myErrorReporter.report(error);
          return false;
        }

        List<Expression> args = result.expression.getFunction().toConCall().getDataTypeArguments();
        if (!compareExpressions(result, args.get(2), Apps(result.expression.getArguments().get(0), ConCall(Preprelude.LEFT)), expr) ||
            !compareExpressions(result, args.get(3), Apps(result.expression.getArguments().get(0), ConCall(Preprelude.RIGHT)), expr)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    Result result = myArgsInference.infer(expr, expectedType);
    if (!checkPath(result, expr)) {
      return null;
    }
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    Result result = myTypeCheckingDefCall.typeCheckDefCall(expr);
    if (!checkPath(result, expr)) {
      return null;
    }
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitModuleCall(Abstract.ModuleCallExpression expr, Expression params) {
    if (expr.getModule() == null) {
      TypeCheckingError error = new NotInScopeError(expr, new ModulePath(expr.getPath()).toString());
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    NamespaceMember member = toNamespaceMember(expr.getModule());
    if (member == null) {
      assert false;
      TypeCheckingError error = new TypeCheckingError("Internal error: module '" + new ModulePath(expr.getPath()) + "' is not available yet", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    return new Result(ClassCall((ClassDefinition) member.definition), new UniverseExpression(member.definition.getUniverse()));
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    List<DependentLink> piParams = new ArrayList<>();
    Expression expectedCodomain = expectedType == null ? null : expectedType.getPiParameters(piParams, true, false);
    LinkList list = new LinkList();
    DependentLink actualPiLink = null;
    Result result = new Result(null, null);
    Substitution piLamSubst = new Substitution();
    int piParamsIndex = 0;
    int argIndex = 1;

    Result bodyResult;
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      Map<Binding, InferenceBinding> bindingTypes = new HashMap<>();

      for (Abstract.Argument argument : expr.getArguments()) {
        List<String> names;
        Result argResult = null;
        Abstract.Expression argType = null;
        boolean isExplicit = argument.getExplicit();

        if (argument instanceof Abstract.NameArgument) {
          names = Collections.singletonList(((Abstract.NameArgument) argument).getName());
        } else if (argument instanceof Abstract.TypeArgument) {
          names = argument instanceof Abstract.TelescopeArgument ? ((Abstract.TelescopeArgument) argument).getNames() : Collections.<String>singletonList(null);
          argType = ((Abstract.TypeArgument) argument).getType();
          argResult = typeCheck(argType, Universe());
          if (argResult == null) return null;
          result.add(argResult);
        } else {
          throw new IllegalStateException();
        }

        DependentLink link = param(isExplicit, names, argResult == null ? null : argResult.expression);
        list.append(link);

        for (String name : names) {
          if (piParamsIndex < piParams.size()) {
            DependentLink piLink = piParams.get(piParamsIndex++);
            if (piLink.isExplicit() != isExplicit) {
              myErrorReporter.report(new TypeCheckingError(ordinal(argIndex) + " argument of the lambda should be " + (piLink.isExplicit() ? "explicit" : "implicit"), expr));
              link.setExplicit(piLink.isExplicit());
            }

            Expression piLinkType = piLink.getType().subst(piLamSubst);
            if (argResult != null) {
              if (result.getEquations() instanceof DummyEquations) {
                result.setEquations(myArgsInference.newEquations());
              }
              if (!CompareVisitor.compare(result.getEquations(), Equations.CMP.EQ, piLinkType.normalize(NormalizeVisitor.Mode.NF), argResult.expression.normalize(NormalizeVisitor.Mode.NF), argType)) {
                TypeCheckingError error = new TypeMismatchError(piLinkType.normalize(NormalizeVisitor.Mode.HUMAN_NF), argResult.expression.normalize(NormalizeVisitor.Mode.HUMAN_NF), argType);
                myErrorReporter.report(error);
                return null;
              }
            } else {
              link.setType(piLinkType);
            }

            piLamSubst.add(piLink, Reference(link));
          } else {
            if (argResult == null) {
              InferenceBinding levelInferenceBinding = new LambdaInferenceBinding("level-of-" + name, Level(), argIndex, expr, true);
              InferenceBinding inferenceBinding = new LambdaInferenceBinding("type-of-" + name, Universe(Reference(levelInferenceBinding)), argIndex, expr, false);
              link.setType(Reference(inferenceBinding));
              bindingTypes.put(link, inferenceBinding);
            }
            if (actualPiLink == null) {
              actualPiLink = link;
            }
          }

          argIndex++;
          myContext.add(link);
          link = link.getNext();
        }
      }

      Expression expectedBodyType = null;
      if (actualPiLink == null && expectedCodomain != null) {
        expectedBodyType = expectedCodomain.fromPiParameters(piParams.subList(piParamsIndex, piParams.size())).subst(piLamSubst);
      }

      Abstract.Expression body = expr.getBody();
      bodyResult = typeCheck(body, expectedBodyType);
      if (bodyResult == null) return null;
      result.add(bodyResult);
      if (actualPiLink != null && expectedCodomain != null) {
        result.expression = bodyResult.expression;
        result.type = Pi(actualPiLink, bodyResult.type);
        if (!compare(result, expectedCodomain, Equations.CMP.EQ, body)) {
          return null;
        }
      }

      for (int i = myContext.size() - 1; i >= saver.getOriginalSize(); i--) {
        result.getEquations().abstractBinding(myContext.get(i));
        InferenceBinding bindingType = bindingTypes.get(myContext.get(i));
        if (bindingType != null) {
          result.addUnsolvedVariable((InferenceBinding) ((TypeUniverse) bindingType.getType().toUniverse().getUniverse()).getLevel().getValue().toReference().getBinding());
          result.addUnsolvedVariable(bindingType);
          Substitution substitution = result.getSubstitution();
          if (!substitution.getDomain().isEmpty()) {
            bodyResult.expression = bodyResult.expression.subst(substitution);
            bodyResult.type = bodyResult.type.subst(substitution);
            ((DependentLink) myContext.get(i)).setType(myContext.get(i).getType().subst(substitution));
          }
        }
      }
    }

    result.expression = Lam(list.getFirst(), bodyResult.expression);
    result.type = Pi(list.getFirst(), bodyResult.type);
    return result;
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    return checkResult(expectedType, visitArguments(expr.getArguments(), expr.getCodomain(), expr), expr);
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    TypeUniverse.HomotopyLevel hlevel = expr.getUniverse().myHLevel == Abstract.UniverseExpression.Universe.NOT_TRUNCATED ? TypeUniverse.HomotopyLevel.NOT_TRUNCATED : new TypeUniverse.HomotopyLevel(expr.getUniverse().myHLevel);
    UniverseExpression universe = Universe(expr.getUniverse().myPLevel, hlevel);
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getUniverse().succ())), expr);
  }

  @Override
  public Result visitPolyUniverse(Abstract.PolyUniverseExpression expr, Expression expectedType) {
    Result result = typeCheck(expr.getLevel(), Level());
    if (result == null) return null;
    Expression level = result.expression.normalize(NormalizeVisitor.Mode.WHNF);
    UniverseExpression universe;
    NewExpression newLevel = level.toNew();

    if (newLevel == null) {
      universe = new UniverseExpression(new TypeUniverse(new TypeUniverse.TypeLevel(level)));
    } else {
      Expression plevel = newLevel.getExpression().toClassCall().getImplementStatements().get(Preprelude.PLEVEL).term;
      Expression hlevel = newLevel.getExpression().toClassCall().getImplementStatements().get(Preprelude.HLEVEL).term;
      universe = new UniverseExpression(new TypeUniverse(new TypeUniverse.TypeLevel(plevel, hlevel)));
    }
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getUniverse().succ())), expr);
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    TypeCheckingError error = new GoalError(myContext, expectedType == null ? null : expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
    expr.setWellTyped(myContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    if (expectedType != null) {
      InferenceBinding binding = new ExpressionInferenceBinding(expectedType, expr);
      Result result = new Result(Reference(binding), expectedType);
      result.addUnsolvedVariable(binding);
      return result;
    } else {
      TypeCheckingError error = new ArgInferenceError(expression(), expr, null);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType != null) {
      expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      SigmaExpression expectedTypeSigma = expectedTypeNorm.toSigma();
      if (expectedTypeSigma != null) {
        DependentLink sigmaParams = expectedTypeSigma.getParameters();
        int sigmaParamsSize = size(sigmaParams);

        if (expr.getFields().size() != sigmaParamsSize) {
          TypeCheckingError error = new TypeCheckingError("Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), expr);
          expr.setWellTyped(myContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Result tupleResult = new Result(Tuple(fields, expectedTypeSigma), expectedType);
        Substitution substitution = new Substitution();
        for (Abstract.Expression field : expr.getFields()) {
          Expression expType = sigmaParams.getType().subst(substitution);
          Result result = typeCheck(field, expType);
          if (result == null) return null;
          fields.add(result.expression);
          substitution.add(sigmaParams, result.expression);
          tupleResult.add(result);

          sigmaParams = sigmaParams.getNext();
        }
        return tupleResult;
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    LinkList list = new LinkList();
    Result tupleResult = new Result(null, null);
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (result == null) return null;
      fields.add(result.expression);
      list.append(param(result.type));
      tupleResult.add(result);
    }

    SigmaExpression type = Sigma(list.getFirst());
    tupleResult.expression = Tuple(fields, type);
    tupleResult.type = type;
    tupleResult = checkResult(expectedTypeNorm, tupleResult, expr);
    tupleResult.update();
    return tupleResult;
  }

  public Result visitArguments(List<? extends Abstract.TypeArgument> arguments, Abstract.Expression codomain, Abstract.Expression expr) {
    Expression[] domainTypes = new Expression[arguments.size()];
    Result argsResult = new Result(null, null);
    LinkList list = new LinkList();
    Result codomainResult = null;

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (int i = 0; i < domainTypes.length; i++) {
        Result result = typeCheck(arguments.get(i).getType(), Universe());
        if (result == null) return null;
        domainTypes[i] = result.type;
        argsResult.add(result);

        if (arguments.get(i) instanceof Abstract.TelescopeArgument) {
          DependentLink link = param(arguments.get(i).getExplicit(), ((Abstract.TelescopeArgument) arguments.get(i)).getNames(), result.expression);
          list.append(link);
          myContext.addAll(DependentLink.Helper.toContext(link));
        } else {
          DependentLink link = param(arguments.get(i).getExplicit(), (String) null, result.expression);
          list.append(link);
          myContext.add(link);
        }
      }

      if (codomain != null) {
        codomainResult = typeCheck(codomain, Universe());
        if (codomainResult == null) return null;
        argsResult.add(codomainResult);
      }

      if (!argsResult.getEquations().isEmpty()) {
        for (int i = saver.getOriginalSize(); i < myContext.size(); i++) {
          argsResult.getEquations().abstractBinding(myContext.get(i));
        }
      }
    }

    Universe universe = null;
    for (int i = 0; i < domainTypes.length; ++i) {
      Universe argUniverse = domainTypes[i].normalize(NormalizeVisitor.Mode.NF).toUniverse().getUniverse();
      if (universe == null) {
        universe = argUniverse;
        continue;
      }
      Universe.CompareResult cmp = universe.compare(argUniverse);
      if (cmp == null) {
        String msg = "Universe " + argUniverse + " of " + ordinal(i + 1) + " argument is not compatible with universe " + universe + " of previous arguments";
        TypeCheckingError error = new TypeCheckingError(msg, expr);
        expr.setWellTyped(myContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      universe = cmp.MaxUniverse;
    }
    if (codomainResult != null) {
      Universe codomainUniverse = codomainResult.type.normalize(NormalizeVisitor.Mode.NF).toUniverse().getUniverse();
      if (universe != null) {
        Universe.CompareResult cmp = universe.compare(codomainUniverse);
        if (cmp == null) {
          String msg = "Universe " + codomainUniverse + " the codomain is not compatible with universe " + universe + " of arguments";
          TypeCheckingError error = new TypeCheckingError(msg, expr);
          expr.setWellTyped(myContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        }
        Universe prop = new TypeUniverse(new TypeUniverse.TypeLevel(TypeUniverse.HomotopyLevel.PROP, false));
        universe = codomainUniverse.equals(prop) ? prop : cmp.MaxUniverse;
      } else {
        universe = codomainUniverse;
      }
    }

    argsResult.expression = codomainResult == null ? Sigma(list.getFirst()) : Pi(list.getFirst(), codomainResult.expression);
    argsResult.type = new UniverseExpression(universe);
    argsResult.update();
    return argsResult;
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression expectedType) {
    return checkResult(expectedType, visitArguments(expr.getArguments(), null, expr), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression expectedType) {
    return checkResultImplicit(expectedType, myArgsInference.infer(expr, expectedType), expr);
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Expression expectedType) {
    assert expr.getSequence().isEmpty();
    return typeCheck(expr.getLeft(), expectedType);
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression expectedType) {
    TypeCheckingError error = new TypeCheckingError("\\elim is allowed only at the root of a definition", expr);
    myErrorReporter.report(error);
    expr.setWellTyped(myContext, Error(null, error));
    return null;
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Expression expectedType) {
    if (expectedType == null) {
      TypeCheckingError error = new TypeCheckingError("Cannot infer type of the type", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Result caseResult = new Result(null, expectedType);
    LetClause letBinding = let(Abstract.CaseExpression.FUNCTION_NAME, EmptyDependentLink.getInstance(), expectedType, (ElimTreeNode) null);
    List<? extends Abstract.Expression> expressions = expr.getExpressions();

    LinkList list = new LinkList();
    List<Expression> letArguments = new ArrayList<>(expressions.size());
    for (int i = 0; i < expressions.size(); i++) {
      Result exprResult = typeCheck(expressions.get(i), null);
      if (exprResult == null) return null;
      if (!exprResult.getEquations().isEmpty()) {
        for (DependentLink link = list.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
          exprResult.getEquations().abstractBinding(link);
        }
      }
      caseResult.add(exprResult);
      list.append(param(true, vars(Abstract.CaseExpression.ARGUMENT_NAME + i), exprResult.type));
      letArguments.add(exprResult.expression);
    }
    letBinding.setParameters(list.getFirst());

    TypeCheckingElim.Result elimResult = myTypeCheckingElim.typeCheckElim(expr, list.getFirst(), expectedType, true);
    if (elimResult == null) return null;
    if (!elimResult.getEquations().isEmpty()) {
      for (DependentLink link = list.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
        elimResult.getEquations().abstractBinding(link);
      }
    }
    caseResult.add(elimResult);
    letBinding.setElimTree(elimResult.elimTree);

    caseResult.expression = Let(lets(letBinding), Apps(Reference(letBinding), letArguments));
    caseResult.update();
    expr.setWellTyped(myContext, caseResult.expression);
    return caseResult;
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression expectedType) {
    Abstract.Expression expr1 = expr.getExpression();
    Result exprResult = typeCheck(expr1, null);
    if (exprResult == null) return null;
    Expression type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    SigmaExpression sigmaType = type.toSigma();
    if (sigmaType == null) {
      TypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A sigma type"), type, expr1);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    DependentLink sigmaParams = sigmaType.getParameters();
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      TypeCheckingError error = new TypeCheckingError("Index " + (expr.getField() + 1) + " out of range", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Substitution substitution = new Substitution();
    for (int i = 0; sigmaParams != fieldLink; sigmaParams = sigmaParams.getNext(), i++) {
      substitution.add(sigmaParams, Proj(exprResult.expression, i));
    }

    exprResult.expression = Proj(exprResult.expression, expr.getField());
    exprResult.type = fieldLink.getType().subst(substitution);
    return checkResult(expectedType, exprResult, expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression expectedType) {
    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result result = typeCheck(baseClassExpr, null);
    if (result == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = result.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normalizedBaseClassExpr.toClassCall();
    if (classCallExpr == null) {
      TypeCheckingError error = new TypeCheckingError("Expected a class", baseClassExpr);
      expr.setWellTyped(myContext, Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassDefinition baseClass = classCallExpr.getDefinition();
    if (baseClass.hasErrors()) {
      TypeCheckingError error = new HasErrors(baseClass.getName(), baseClassExpr);
      expr.setWellTyped(myContext, Error(classCallExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    Collection<? extends Abstract.ImplementStatement> statements = expr.getStatements();
    if (statements.isEmpty()) {
      result.expression = classCallExpr;
      result.type = baseClass.getType();
      return checkResult(expectedType, result, expr);
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
      String name = statement.getName();
      ClassField field = baseClass.removeField(name);
      if (field == null) {
        TypeCheckingError error = new TypeCheckingError("Class '" + baseClass.getName() + "' does not have field '" + name + "'", statement);
        myErrorReporter.report(error);
      } else {
        fields.add(new ImplementStatement(field, statement.getExpression()));
      }
    }

    Result classExtResult = new Result(null, null);
    Map<ClassField, ClassCallExpression.ImplementStatement> typeCheckedStatements = Collections.emptyMap();
    for (int i = 0; i < fields.size(); i++) {
      ImplementStatement field = fields.get(i);
      Expression thisExpr = New(ClassCall(baseClass, typeCheckedStatements));
      Result result1 = typeCheck(field.term, field.classField.getBaseType().subst(field.classField.getThisParameter(), thisExpr));
      baseClass.addField(field.classField);
      if (result1 == null) {
        for (i++; i < fields.size(); i++) {
          typeCheck(fields.get(i).term, fields.get(i).classField.getBaseType().subst(field.classField.getThisParameter(), thisExpr));
          baseClass.addField(fields.get(i).classField);
        }
        return null;
      }

      typeCheckedStatements = new HashMap<>(typeCheckedStatements);
      typeCheckedStatements.put(field.classField, new ClassCallExpression.ImplementStatement(result1.type, result1.expression));
      classExtResult.add(result1);
    }

    ClassCallExpression resultExpr = ClassCall(baseClass, typeCheckedStatements);
    classExtResult.expression = resultExpr;
    classExtResult.type = new UniverseExpression(resultExpr.getUniverse());
    classExtResult.update();
    return checkResult(expectedType, classExtResult, expr);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.toClassCall();
    if (classCallExpr == null) {
      TypeCheckingError error = new TypeCheckingError("Expected a class", expr.getExpression());
      expr.setWellTyped(myContext, Error(normExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    if (classCallExpr.getImplementStatements().size() == classCallExpr.getDefinition().getFields().size()) {
      exprResult.expression = New(normExpr);
      exprResult.type = normExpr;
      return checkResult(expectedType, exprResult, expr);
    } else {
      TypeCheckingError error = new TypeCheckingError("Class '" + classCallExpr.getDefinition().getName() + "' has " + classCallExpr.getDefinition().getNumberOfVisibleFields() + " fields", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private LetClauseResult typeCheckLetClause(Abstract.LetClause clause) {
    LinkList links = new LinkList();
    Expression resultType;
    ElimTreeNode elimTree;
    LetClauseResult letResult = new LetClauseResult(null);

    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument arg : clause.getArguments()) {
        if (arg instanceof Abstract.TelescopeArgument) {
          Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) arg;
          Result result = typeCheck(teleArg.getType(), Universe());
          if (result == null) return null;
          if (!result.getEquations().isEmpty()) {
            for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
              result.getEquations().abstractBinding(link);
            }
          }
          letResult.add(result);
          links.append(param(teleArg.getExplicit(), teleArg.getNames(), result.expression));
          for (DependentLink link = links.getLast(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            myContext.add(link);
          }
        } else {
          throw new IllegalStateException();
        }
      }

      Expression expectedType = null;
      if (clause.getResultType() != null) {
        Result result = typeCheck(clause.getResultType(), null);
        if (result == null) return null;
        if (!result.getEquations().isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            result.getEquations().abstractBinding(link);
          }
        }
        letResult.add(result);
        expectedType = result.expression;
      }

      if (clause.getTerm() instanceof Abstract.ElimExpression)  {
        myContext.subList(myContext.size() - size(links.getFirst()), myContext.size()).clear();
        TypeCheckingElim.Result elimResult = myTypeCheckingElim.typeCheckElim((Abstract.ElimExpression) clause.getTerm(), clause.getArrow() == Abstract.Definition.Arrow.LEFT ? links.getFirst() : null, expectedType, false);
        if (elimResult == null)
          return null;
        if (!elimResult.getEquations().isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            elimResult.getEquations().abstractBinding(link);
          }
        }
        letResult.add(elimResult);
        elimTree = elimResult.elimTree;
        resultType = expectedType;
      } else {
        Result termResult = typeCheck(clause.getTerm(), expectedType);
        if (termResult == null) return null;
        if (!termResult.getEquations().isEmpty()) {
          for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            termResult.getEquations().abstractBinding(link);
          }
        }
        letResult.add(termResult);
        elimTree = top(links.getFirst(), leaf(clause.getArrow(), termResult.expression));
        resultType = termResult.type;
      }

      TypeCheckingError error = TypeCheckingElim.checkCoverage(clause, links.getFirst(), elimTree, expectedType);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
      error = TypeCheckingElim.checkConditions(clause, links.getFirst(), elimTree);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
    }

    letResult.letClause = new LetClause(clause.getName(), links.getFirst(), resultType, elimTree);
    letResult.update();
    myContext.add(letResult.letClause);
    return letResult;
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Expression expectedType) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      List<LetClause> clauses = new ArrayList<>();
      Result letResult = new Result(null, null);
      for (int i = 0; i < expr.getClauses().size(); i++) {
        LetClauseResult clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (clauseResult == null) return null;
        for (Binding binding : clauses) {
          clauseResult.getEquations().abstractBinding(binding);
        }
        letResult.add(clauseResult);
        clauses.add(clauseResult.letClause);
      }
      Result result = typeCheck(expr.getExpression(), expectedType == null ? null : expectedType);
      if (result == null) return null;
      for (Binding binding : clauses) {
        result.getEquations().abstractBinding(binding);
      }
      letResult.add(result);

      letResult.expression = Let(clauses, result.expression);
      letResult.type = Let(clauses, result.type);
      letResult.update();
      return letResult;
    }
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, Expression expectedType) {
    int number = expr.getNumber();
    Expression expression = Zero();
    for (int i = 0; i < number; ++i) {
      expression = Suc(expression);
    }
    return checkResult(expectedType, new Result(expression, Nat()), expr);
  }
}
