package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.DefinitionResolvedName;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.typechecking.error.HasErrors;
import com.jetbrains.jetpad.vclang.typechecking.error.NameDefinedError;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static com.jetbrains.jetpad.vclang.term.definition.BaseDefinition.Helper.toNamespaceMember;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class TypeCheckingDefCall {
  private final CheckTypeVisitor myVisitor;
  private ClassDefinition myThisClass;
  private Expression myThisExpr;

  public TypeCheckingDefCall(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  public void setThisClass(ClassDefinition thisClass, Expression thisExpr) {
    myThisClass = thisClass;
    myThisExpr = thisExpr;
  }

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public Expression getThisExpression() {
    return myThisExpr;
  }

  public CheckTypeVisitor.Result typeCheckDefCall(Abstract.DefCallExpression expr) {
    DefCallResult result = typeCheckNamespace(expr, null);
    if (result == null) {
      return null;
    }
    if (result.baseResult == null) {
      TypeCheckingError error = new TypeCheckingError("'" + result.member.getResolvedName().getFullName() + "' is not available here or not a definition", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    return result.baseResult;
  }

  private CheckTypeVisitor.Result checkDefinition(Definition definition, Abstract.Expression expr, Expression thisExpr) {
    if (definition instanceof FunctionDefinition && ((FunctionDefinition) definition).typeHasErrors() || !(definition instanceof FunctionDefinition) && definition.hasErrors()) {
      TypeCheckingError error = new HasErrors(definition.getName(), expr);
      expr.setWellTyped(myVisitor.getContext(), Error(definition.getDefCall(), error));
      myVisitor.getErrorReporter().report(error);
      return null;
    } else {
      CheckTypeVisitor.Result result = new CheckTypeVisitor.Result(definition.getDefCall(), definition.getType());
      if (definition instanceof Constructor) {
        fixConstructorParameters((Constructor) definition, result, false);
      }
      if (thisExpr != null) {
        result.expression = ((DefCallExpression) result.expression).applyThis(thisExpr);
        if (definition instanceof ClassDefinition) {
          result.type = result.expression.getType();
        } else {
          result.type = result.type.applyExpressions(Collections.singletonList(thisExpr));
        }
      }
      return result;
    }
  }

  private void fixConstructorParameters(Constructor constructor, CheckTypeVisitor.Result result, boolean doSubst) {
    DependentLink parameters = constructor.getDataTypeParameters();
    if (parameters.hasNext()) {
      Substitution substitution = new Substitution();
      parameters = DependentLink.Helper.subst(parameters, substitution);
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        link.setExplicit(false);
      }
      result.type = Pi(parameters, result.type.subst(substitution));
    }

    if (doSubst && result.expression instanceof ConCallExpression) {
      List<Expression> args = ((ConCallExpression) result.expression).getDataTypeArguments();
      if (!args.isEmpty()) {
        assert parameters.hasNext();
        result.type = result.type.applyExpressions(args);
      }
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

  public CheckTypeVisitor.Result getLocalVar(String name, Abstract.Expression expr) {
    ListIterator<Binding> it = myVisitor.getContext().listIterator(myVisitor.getContext().size());
    while (it.hasPrevious()) {
      Binding def = it.previous();
      if (name.equals(def.getName())) {
        return new CheckTypeVisitor.Result(Reference(def), def.getType());
      }
    }

    TypeCheckingError error = new NotInScopeError(expr, name);
    expr.setWellTyped(myVisitor.getContext(), Error(null, error));
    myVisitor.getErrorReporter().report(error);
    return null;
  }

  private static class DefCallResult {
    CheckTypeVisitor.Result baseResult;
    ClassDefinition baseClassDefinition;
    NamespaceMember member;

    public DefCallResult(CheckTypeVisitor.Result baseResult, ClassDefinition baseClassDefinition, NamespaceMember member) {
      this.baseResult = baseResult;
      this.baseClassDefinition = baseClassDefinition;
      this.member = member;
    }
  }

  private DefCallResult typeCheckName(Abstract.DefCallExpression expr, String next) {
    String name = expr.getName();
    BaseDefinition resolvedDefinition = expr.getResolvedDefinition();
    if (resolvedDefinition == null) {
      CheckTypeVisitor.Result result = getLocalVar(name, expr);
      return result == null ? null : new DefCallResult(result, null, null);
    }

    NamespaceMember member = toNamespaceMember(resolvedDefinition);
    if (member == null) {
      assert false;
      TypeCheckingError error = new TypeCheckingError("Internal error: definition '" + name + "' is not available yet", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    return getDefCallResult(member, expr, next);
  }

  private DefCallResult typeCheckModule(Abstract.ModuleCallExpression expr, String next) {
    NamespaceMember member = toNamespaceMember(expr.getModule());

    if (member == null) {
      assert false;
      TypeCheckingError error = new TypeCheckingError("Internal error: definition '" + new ModulePath(expr.getPath()) + "' is not available yet", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    return getDefCallResult(member, expr, next);
  }

  private DefCallResult getDefCallResult(NamespaceMember member, Abstract.Expression expr, String next) {
    Definition definition = member.definition;
    if (definition == null || next != null && (definition instanceof ClassDefinition || definition instanceof FunctionDefinition && member.namespace.getMember(next) != null)) {
      return new DefCallResult(null, null, member);
    }

    Expression thisExpr = null;
    if (definition.getThisClass() != null) {
      if (myThisClass != null) {
        member = null;
        thisExpr = findParent(myThisClass, definition.getThisClass(), myThisExpr);
        if (thisExpr == null) {
          TypeCheckingError error = new TypeCheckingError("Definition '" + definition.getName() + "' is not available in this context", expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      } else {
        TypeCheckingError error = new TypeCheckingError("Non-static definitions are not allowed in static context", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    CheckTypeVisitor.Result result = checkDefinition(definition, expr, thisExpr);
    return result == null ? null : new DefCallResult(result, null, member);
  }

  private DefCallResult nextResult(DefCallResult result, Abstract.DefCallExpression expr, String next) {
    NamespaceMember member;
    BaseDefinition resolvedDefinition = expr.getResolvedDefinition();
    if (resolvedDefinition != null) {
      NamespaceMember actualMember = toNamespaceMember(resolvedDefinition);
      member = result.member.namespace.getMember(resolvedDefinition.getName());
      if (member == null || actualMember.namespace.getParent() != member.namespace.getParent()) {
        TypeCheckingError error = new NameDefinedError(false, expr, resolvedDefinition.getName(), actualMember.namespace.getParent().getResolvedName());
        expr.setWellTyped(myVisitor.getContext(), Error(result.baseResult == null ? null : result.baseResult.expression, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    } else {
      String name = expr.getName();
      member = result.member.namespace.getMember(name);
      if (member == null) {
        TypeCheckingError error = new NameDefinedError(false, expr, name, new DefinitionResolvedName(result.member.namespace.getParent(), result.member.namespace.getName()));
        expr.setWellTyped(myVisitor.getContext(), Error(result.baseResult == null ? null : result.baseResult.expression, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    if (!(member.definition == null || next != null && (member.definition instanceof ClassDefinition || member.definition instanceof FunctionDefinition && member.namespace.getMember(next) != null))) {
      if (result.baseResult != null) {
        if (result.baseClassDefinition != null && result.baseClassDefinition == member.definition.getThisClass()) {
          CheckTypeVisitor.Result result1 = checkDefinition(member.definition, expr, result.baseResult.expression);
          if (result1 == null) {
            return null;
          }
          result1.add(result.baseResult);
          result.baseResult = result1;
          result.baseClassDefinition = null;
          result.member = null;
          return result;
        } else {
          TypeCheckingError error = new TypeCheckingError("Definition '" + member.definition.getName() + "' cannot be called from here", expr);
          expr.setWellTyped(myVisitor.getContext(), Error(result.baseResult.expression, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      } else {
        return getDefCallResult(member, expr, next);
      }
    }
    if (member.definition == null && next == null) {
      result.baseResult = null;
    }

    result.member = member;
    return result;
  }

  private DefCallResult typeCheckNamespace(Abstract.DefCallExpression expr, String next) {
    String name = expr.getName();
    Abstract.Expression left = expr.getExpression();
    if (left == null) {
      return typeCheckName(expr, next);
    }

    DefCallResult result;
    if (left instanceof Abstract.DefCallExpression || left instanceof Abstract.ModuleCallExpression) {
      result = left instanceof Abstract.DefCallExpression ? typeCheckNamespace((Abstract.DefCallExpression) left, name) : typeCheckModule((Abstract.ModuleCallExpression) left, name);
      if (result == null) {
        return null;
      }
    } else {
      CheckTypeVisitor.Result result1 = left.accept(myVisitor, null);
      if (result1 == null) {
        return null;
      }
      result = new DefCallResult(result1, null, null);
    }

    if (result.baseResult != null && result.member == null) {
      Expression type = result.baseResult.type.normalize(NormalizeVisitor.Mode.WHNF);

      if (type instanceof ClassCallExpression) {
        result.baseClassDefinition = ((ClassCallExpression) type).getDefinition();
        result.member = result.baseClassDefinition.getParentNamespace().getMember(result.baseClassDefinition.getName());
      } else {
        if (type instanceof UniverseExpression || type instanceof PiExpression) {
          List<Expression> arguments = new ArrayList<>();
          Expression function = result.baseResult.expression.normalize(NormalizeVisitor.Mode.WHNF).getFunction(arguments);
          if (function instanceof DataCallExpression) {
            CheckTypeVisitor.Result conResult = typeCheckConstructor(((DataCallExpression) function).getDefinition(), arguments, name, expr);
            if (conResult == null) {
              return null;
            }
            conResult.add(result.baseResult);
            return new DefCallResult(conResult, null, null);
          }
        }

        TypeCheckingError error = new TypeCheckingError("Expected an expression of a class type or a data type", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(result.baseResult.expression, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    return nextResult(result, expr, next);
  }

  private CheckTypeVisitor.Result typeCheckConstructor(DataDefinition dataDefinition, List<Expression> arguments, String conName, Abstract.Expression expr) {
    Collections.reverse(arguments);
    Constructor constructor = dataDefinition.getConstructor(conName);
    if (constructor == null) {
      TypeCheckingError error = new TypeCheckingError("Constructor '" + conName + "' is not defined in data type '" + dataDefinition.getName() + "'", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    if (constructor.getPatterns() != null) {
      Pattern.MatchResult matchResult = constructor.getPatterns().match(arguments);
      TypeCheckingError error = null;
      if (matchResult instanceof Pattern.MatchMaybeResult) {
        error = new TypeCheckingError("Constructor is not appropriate, failed to match data type parameters. " +
            "Expected " + ((Pattern.MatchMaybeResult) matchResult).maybePattern + ", got " + ((Pattern.MatchMaybeResult) matchResult).actualExpression, expr);
      } else if (matchResult instanceof Pattern.MatchFailedResult) {
        error = new TypeCheckingError("Constructor is not appropriate, failed to match data type parameters. " +
            "Expected " + ((Pattern.MatchFailedResult) matchResult).failedPattern + ", got " + ((Pattern.MatchFailedResult) matchResult).actualExpression, expr);
      } else if (matchResult instanceof Pattern.MatchOKResult) {
        arguments = ((Pattern.MatchOKResult) matchResult).expressions;
      }

      if (error != null) {
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    CheckTypeVisitor.Result result = new CheckTypeVisitor.Result(ConCall(constructor, arguments), constructor.getType().subst(toSubstitution(constructor.getDataTypeParameters(), arguments)));
    fixConstructorParameters(constructor, result, true);
    return result;
  }
}
