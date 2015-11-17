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
    if (expr instanceof ConCallExpression) {
      Constructor constructor = ((ConCallExpression) expr).getDefinition();
      Expression type = constructor.getBaseType();

      List<TypeArgument> parameters;
      if (constructor.getPatterns() != null) {
        parameters = expandConstructorParameters(constructor, myLocalContext);
      } else {
        parameters = constructor.getDataType().getParameters();
      }

      if (!parameters.isEmpty()) {
        type = Pi(parameters, type);
      }
      if (constructor.getThisClass() != null) {
        type = Pi("\\this", ClassCall(constructor.getThisClass()), type);
      }
      return new CheckTypeVisitor.OKResult(ConCall(constructor), type, null);
    }
    if (expr instanceof DefCallExpression) {
      Definition definition = ((DefCallExpression) expr).getDefinition();
      return new CheckTypeVisitor.OKResult(definition.getDefCall(), definition.getType(), null);
    }

    DefCallResult result = typeCheckNamespace(expr);
    if (result == null) {
      return null;
    }
    if (result.result != null) {
      return result.result;
    }
    if (result.member == null) {
      return result.baseResult;
    }
    if (result.member.definition == null) {
      TypeCheckingError error = new TypeCheckingError("'" + result.member.namespace.getFullName() + "' is not available here or not a definition", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    if (result.member.definition instanceof FunctionDefinition && ((FunctionDefinition) result.member.definition).typeHasErrors() || !(result.member.definition instanceof FunctionDefinition) && result.member.definition.hasErrors()) {
      TypeCheckingError error = new HasErrors(result.member.definition.getName(), expr);
      expr.setWellTyped(myLocalContext, Error(result.member.definition.getDefCall(), error));
      myErrorReporter.report(error);
      return null;
    }

    CheckTypeVisitor.OKResult okResult = result.baseResult == null ? new CheckTypeVisitor.OKResult(null, null, null) : (CheckTypeVisitor.OKResult) result.baseResult;
    Expression thisExpr = okResult.expression;
    DefCallExpression expression = result.member.definition.getDefCall();
    okResult.type = result.member.definition.getBaseType();

    if (result.member.definition instanceof Constructor) {
      fixConstructorParameters((Constructor) result.member.definition, okResult);
    }

    if (thisExpr != null) {
      okResult.expression = expression.applyThis(thisExpr);
      if (okResult.type != null) {
        okResult.type = okResult.type.subst(thisExpr, 0);
      }
    } else {
      okResult.expression = expression;
    }

    return okResult;
  }

  private void fixConstructorParameters(Constructor constructor, CheckTypeVisitor.OKResult result) {
    List<TypeArgument> parameters;
    if (constructor.getPatterns() != null) {
      parameters = expandConstructorParameters(constructor, myLocalContext);
    } else {
      parameters = constructor.getDataType().getParameters();
    }

    if (!parameters.isEmpty()) {
      result.type = Pi(parameters, result.type);
    }
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

    TypeCheckingError error = new NotInScopeError(expr, name);
    expr.setWellTyped(myLocalContext, Error(null, error));
    myErrorReporter.report(error);
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
      CheckTypeVisitor.OKResult result = getLocalVar(name, expr);
      if (result == null) {
        return null;
      }
      result.type = result.type.liftIndex(0, ((IndexExpression) result.expression).getIndex() + 1);
      return new DefCallResult(result, null, null, null);
    }

    NamespaceMember member = resolvedName.toNamespaceMember();
    if (member == null) {
      assert false;
      TypeCheckingError error = new TypeCheckingError("Internal error: definition '" + name + "' is not available yet", expr, getNames(myLocalContext));
      expr.setWellTyped(myLocalContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Definition definition = member.definition;
    if (definition == null) {
      return new DefCallResult(new CheckTypeVisitor.OKResult(null, null, null), null, member, null);
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

        if (!(definition instanceof ClassDefinition) && !(definition instanceof FunctionDefinition)) {
          CheckTypeVisitor.OKResult result = new CheckTypeVisitor.OKResult(definition.getDefCall().applyThis(thisExpr), definition.getBaseType(), null);
          if (definition instanceof Constructor) {
            fixConstructorParameters((Constructor) definition, result);
          }
          result.type = result.type.subst(thisExpr, 0);
          return new DefCallResult(result, null, null, null);
        } else {
          return new DefCallResult(new CheckTypeVisitor.OKResult(thisExpr, null, null), definition.getThisClass(), member, null);
        }
      } else {
        TypeCheckingError error = new TypeCheckingError("Non-static definitions are not allowed in static context", expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
    } else {
      if (!(definition instanceof ClassDefinition) && !(definition instanceof FunctionDefinition)) {
        CheckTypeVisitor.OKResult result = new CheckTypeVisitor.OKResult(definition.getDefCall(), definition.getBaseType(), null);
        if (definition instanceof Constructor) {
          fixConstructorParameters((Constructor) definition, result);
        }
        return new DefCallResult(result, null, null, null);
      } else {
        return new DefCallResult(new CheckTypeVisitor.OKResult(null, null, null), null, member, null);
      }
    }
  }

  private DefCallResult typeCheckNamespace(Abstract.DefCallExpression expr) {
    Abstract.Expression left = expr.getExpression();
    if (left == null) {
      return typeCheckName(expr);
    }

    DefCallResult result;
    if (left instanceof Abstract.DefCallExpression) {
      result = typeCheckNamespace(((Abstract.DefCallExpression) left));
      if (result == null || !(result.baseResult instanceof CheckTypeVisitor.OKResult)) {
        return result;
      }
    } else {
      CheckTypeVisitor.Result result1 = left.accept(new CheckTypeVisitor(myLocalContext, myErrorReporter, this), null);
      result = new DefCallResult(result1, null, null, null);
      if (!(result1 instanceof CheckTypeVisitor.OKResult)) {
        return result;
      }
    }

    if (result.member == null) {
      CheckTypeVisitor.OKResult okResult = (CheckTypeVisitor.OKResult) result.baseResult;
      Expression type = okResult.type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);

      if (type instanceof ClassCallExpression) {
        ClassDefinition classDefinition = ((ClassCallExpression) type).getDefinition();
        /*
        ResolvedName resolvedName = expr.getResolvedName();
        ClassField classField;
        if (resolvedName != null) {
          classField = classDefinition.getField(resolvedName.name.name);
          if (classField == null || resolvedName.parent != classDefinition.getParentNamespace()) {
            TypeCheckingError error = new NameDefinedError(false, expr, resolvedName.name, classDefinition.getResolvedName());
            expr.setWellTyped(myLocalContext, Error(result.baseResult.expression, error));
            myErrorReporter.report(error);
            return null;
          }
        } else {
          Name name = expr.getName();
          classField = classDefinition.getField(name.name);
          if (classField == null) {
            TypeCheckingError error = new NameDefinedError(false, expr, name, classDefinition.getResolvedName());
            expr.setWellTyped(myLocalContext, Error(result.baseResult.expression, error));
            myErrorReporter.report(error);
            return null;
          }
        }
        return new DefCallResult(new CheckTypeVisitor.OKResult(Apps(FieldCall(classField), okResult.expression), classField.getBaseType().subst(okResult.expression, 0), okResult.equations), null, null, null);
        */
        NamespaceMember member = classDefinition.getParentNamespace().getMember(classDefinition.getName().name);
        assert member != null;
        result = new DefCallResult(okResult, classDefinition, member, null);
      } else {
        if (type instanceof UniverseExpression) {
          List<Expression> arguments = new ArrayList<>();
          Expression function = okResult.expression.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext).getFunction(arguments);
          if (function instanceof DataCallExpression) {
            CheckTypeVisitor.OKResult conResult = typeCheckConstructor(((DataCallExpression) function).getDefinition(), arguments, expr.getName(), expr);
            if (conResult == null) {
              return null;
            }
            conResult.equations = okResult.equations;
            return new DefCallResult(null, null, null, conResult);
          }
        }

        TypeCheckingError error = new TypeCheckingError("Expected an expression of a class type or a data type", expr, getNames(myLocalContext));
        expr.setWellTyped(myLocalContext, Error(okResult.expression, error));
        myErrorReporter.report(error);
        return null;
      }
    }

    ResolvedName resolvedName = expr.getResolvedName();
    NamespaceMember member;
    if (resolvedName != null) {
      member = result.member.namespace.getMember(resolvedName.name.name);
      if (member == null || resolvedName.parent != member.namespace.getParent()) {
        TypeCheckingError error = new NameDefinedError(false, expr, resolvedName.name, resolvedName.parent.getResolvedName());
        expr.setWellTyped(myLocalContext, Error(result.baseResult.expression, error));
        myErrorReporter.report(error);
        return null;
      }
    } else {
      Name name = expr.getName();
      member = result.member.namespace.getMember(name.name);
      if (member == null) {
        TypeCheckingError error = new NameDefinedError(false, expr, name, new ResolvedName(result.member.namespace.getParent(), result.member.namespace.getName()));
        expr.setWellTyped(myLocalContext, Error(result.baseResult.expression, error));
        myErrorReporter.report(error);
        return null;
      }
    }

    if (member.definition != null && member.definition.getThisClass() != result.baseClassDefinition) {
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
    Expression resultType = constructor.getBaseType().subst(arguments, 0);
    Collections.reverse(arguments);
    return new CheckTypeVisitor.OKResult(ConCall(constructor, arguments), resultType, null);
  }
}
