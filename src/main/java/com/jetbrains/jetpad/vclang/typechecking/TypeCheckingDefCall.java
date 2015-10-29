package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.Utils;
import com.jetbrains.jetpad.vclang.typechecking.error.HasErrors;
import com.jetbrains.jetpad.vclang.typechecking.error.NameDefinedError;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandConstructorParameters;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.patternMatchAll;
import static com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError.getNames;

public class TypeCheckingDefCall {
  private final List<Binding> myLocalContext;
  private final ErrorReporter myErrorReporter;
  private ClassDefinition myThisClass;

  public TypeCheckingDefCall(List<Binding> localContext, ErrorReporter errorReporter) {
    myLocalContext = localContext;
    myErrorReporter = errorReporter;
  }

  public void setThisClass(ClassDefinition thisClass) {
    myThisClass = thisClass;
  }

  public CheckTypeVisitor.Result typeCheckDefCall(Abstract.DefCallExpression expr) {
    DefCallResult result = typeCheckNamespace(expr);
    if (result == null) {
      return null;
    }
    if (result.result != null) {
      return result.result;
    }
    if (result.member == null) {
      assert !(result.baseResult instanceof CheckTypeVisitor.OKResult);
      return result.baseResult;
    }

    CheckTypeVisitor.OKResult okResult = result.baseResult == null ? new CheckTypeVisitor.OKResult(null, null, null) : (CheckTypeVisitor.OKResult) result.baseResult;
    Expression thisExpr = okResult.expression;
    okResult.expression = DefCall(result.member.definition);
    okResult.type = result.member.definition.getType();

    if (result.member.definition instanceof Constructor) {
      Constructor constructor = (Constructor) result.member.definition;
      List<TypeArgument> parameters;
      if (constructor.getPatterns() != null) {
        parameters = expandConstructorParameters(constructor);
      } else {
        parameters = constructor.getDataType().getParameters();
      }

      if (!parameters.isEmpty()) {
        okResult.type = Pi(parameters, okResult.type);
      }
    }

    if (thisExpr != null) {
      okResult.expression = Apps(okResult.expression, thisExpr);
      okResult.type = okResult.type.subst(thisExpr, 0);
    }

    return okResult;
  }

  private Expression findParent(ClassDefinition classDefinition, ClassDefinition parent, Expression expr) {
    if (classDefinition == parent) {
      return expr;
    }
    ClassField parentField = classDefinition.getParentField();
    if (parentField == null || !(parentField.getBaseType() instanceof ClassCallExpression)) {
      return null;
    }
    return findParent(((ClassCallExpression) parentField.getBaseType()).getDefinition(), parent, Apps(FieldCall(parentField), expr));
  }

  public CheckTypeVisitor.OKResult getLocalVar(Name name, Abstract.Expression expr) {
    ListIterator<Binding> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Binding def = it.previous();
      if (name.name.equals(def.getName() == null ? null : def.getName().name)) {
        return new CheckTypeVisitor.OKResult(Index(index), def.getType(), null);
      }
      ++index;
    }

    NotInScopeError error = new NotInScopeError(expr, name);
    expr.setWellTyped(myLocalContext, Error(null, error));
    return null;
  }

  private static class DefCallResult {
    CheckTypeVisitor.Result baseResult;
    ClassDefinition baseClassDefinition;
    NamespaceMember member;
    CheckTypeVisitor.OKResult result;

    public DefCallResult(CheckTypeVisitor.Result baseResult, ClassDefinition baseClassDefinition, NamespaceMember member, CheckTypeVisitor.OKResult result) {
      this.baseResult = baseResult;
      this.baseClassDefinition = baseClassDefinition;
      this.member = member;
      this.result = result;
    }
  }

  private DefCallResult typeCheckName(Abstract.DefCallExpression expr) {
    Name name = expr.getName();
    ResolvedName resolvedName = expr.getResolvedName();
    if (resolvedName == null) {
      CheckTypeVisitor.OKResult result1 = getLocalVar(name, expr);
      if (result1 == null) {
        TypeCheckingError error = new NotInScopeError(expr, name);
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      result1.type = result1.type.liftIndex(0, ((IndexExpression) result1.expression).getIndex() + 1);
      return new DefCallResult(null, null, null, result1);
    }

    NamespaceMember member = resolvedName.toNamespaceMember();
    if (member == null || member.definition == null) {
      assert false;
      TypeCheckingError error = new TypeCheckingError("Definition '" + name + "' is not available yet", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Definition definition = member.definition;
    if (definition instanceof FunctionDefinition && ((FunctionDefinition) definition).typeHasErrors() || !(definition instanceof FunctionDefinition) && definition.hasErrors()) {
      TypeCheckingError error = new HasErrors(name, expr);
      expr.setWellTyped(myLocalContext, Error(DefCall(definition), error));
      myErrorReporter.report(error);
      return null;
    }

    if (definition.getThisClass() != null) {
      if (myThisClass != null) {
        assert myLocalContext.size() > 0;
        assert myLocalContext.get(0).getName().name.equals("\\this");
        Expression thisExpr = Index(myLocalContext.size() - 1);
        thisExpr = findParent(myThisClass, definition.getThisClass(), thisExpr);
        if (thisExpr == null) {
          TypeCheckingError error = new TypeCheckingError("Definition '" + definition.getName() + "' is not available in this context", expr, getNames(myLocalContext));
          expr.setWellTyped(myLocalContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        }
        return new DefCallResult(new CheckTypeVisitor.OKResult(thisExpr, null, null), null, member, null);
      } else {
        TypeCheckingError error = new TypeCheckingError("Non-static definitions are not allowed in static context", expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
    } else {
      return new DefCallResult(null, null, member, null);
    }
  }

  private DefCallResult typeCheckNamespace(Abstract.DefCallExpression expr) {
    Abstract.Expression left = expr.getExpression();
    if (left == null) {
      return typeCheckName(expr);
    }

    Expression errorExpr;
    DefCallResult result = null;
    if (left instanceof Abstract.DefCallExpression) {
      result = typeCheckNamespace(((Abstract.DefCallExpression) left));
      if (result == null || !(result.baseResult instanceof CheckTypeVisitor.OKResult)) {
        return result;
      }
      errorExpr = result.baseResult.expression;
    } else {
      CheckTypeVisitor.Result result1 = left.accept(new CheckTypeVisitor(myLocalContext, myErrorReporter), null);
      if (!(result1 instanceof CheckTypeVisitor.OKResult)) {
        return new DefCallResult(result1, null, null, null);
      }
      CheckTypeVisitor.OKResult okResult = (CheckTypeVisitor.OKResult) result1;
      errorExpr = okResult.expression;
      Expression type = okResult.type.normalize(NormalizeVisitor.Mode.WHNF);

      if (type instanceof ClassCallExpression) {
        ClassDefinition classDefinition = ((ClassCallExpression) type).getDefinition();
        NamespaceMember member = classDefinition.getParentNamespace().getMember(classDefinition.getName().name);
        assert member != null;
        result = new DefCallResult(result1, classDefinition, member, null);
      } else
      if (type instanceof UniverseExpression) {
        List<Expression> arguments = new ArrayList<>();
        Expression function = okResult.expression.normalize(NormalizeVisitor.Mode.WHNF).getFunction(arguments);
        if (function instanceof DataCallExpression) {
          CheckTypeVisitor.OKResult conResult = typeCheckConstructor(((DataCallExpression) function).getDefinition(), arguments, expr.getName(), expr);
          if (conResult == null) {
            return null;
          }
          conResult.equations = okResult.equations;
          return new DefCallResult(null, null, null, conResult);
        }
      }
    }

    if (result == null || result.member == null) {
      TypeCheckingError error = new TypeCheckingError("Expected an expression of a class type or a data type", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(errorExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    Name name = expr.getName();
    NamespaceMember member = result.member.namespace.getMember(name.name);
    if (member == null || member.definition == null) {
      TypeCheckingError error = new NameDefinedError(false, expr, name, new ResolvedName(result.member.namespace.getParent(), result.member.namespace.getName()));
      expr.setWellTyped(myLocalContext, Error(result.baseResult.expression, error));
      myErrorReporter.report(error);
      return null;
    }
    if (member.definition.getThisClass() != result.baseClassDefinition) {
      TypeCheckingError error = new TypeCheckingError("Definition '" + member.definition.getName() + "' cannot be called from here", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(result.baseResult.expression, error));
      myErrorReporter.report(error);
      return null;
    }

    result.member = member;
    return result;
  }

  private CheckTypeVisitor.OKResult typeCheckConstructor(DataDefinition dataDefinition, List<Expression> arguments, Name conName, Abstract.Expression expr) {
    Collections.reverse(arguments);
    Constructor constructor = dataDefinition.getConstructor(conName.name);
    if (constructor == null) {
      TypeCheckingError error = new TypeCheckingError("Constructor '" + conName + "' is not defined in data type '" + dataDefinition.getName() + "'", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    if (constructor.getPatterns() != null) {
      Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), arguments, myLocalContext);
      TypeCheckingError error = null;
      if (matchResult instanceof Utils.PatternMatchMaybeResult) {
        error = new TypeCheckingError("Constructor is not appropriate, failed to match data type parameters. " +
            "Expected " + ((Utils.PatternMatchMaybeResult) matchResult).maybePattern + ", got " + ((Utils.PatternMatchMaybeResult) matchResult).actualExpression.prettyPrint(getNames(myLocalContext)), expr, getNames(myLocalContext));
      } else if (matchResult instanceof Utils.PatternMatchFailedResult) {
        error = new TypeCheckingError("Constructor is not appropriate, failed to match data type parameters. " +
            "Expected " + ((Utils.PatternMatchFailedResult) matchResult).failedPattern + ", got " + ((Utils.PatternMatchFailedResult) matchResult).actualExpression.prettyPrint(getNames(myLocalContext)), expr, getNames(myLocalContext));
      } else if (matchResult instanceof Utils.PatternMatchOKResult) {
        arguments = ((Utils.PatternMatchOKResult) matchResult).expressions;
      }

      if (error != null) {
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
    }

    Collections.reverse(arguments);
    Expression resultType = constructor.getType().subst(arguments, 0);
    Collections.reverse(arguments);
    return new CheckTypeVisitor.OKResult(ConCall(constructor, arguments), resultType, null);
  }
}
