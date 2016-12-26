package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.scope.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Error;

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

  private Definition getTypeCheckedDefinition(Abstract.Definition definition, Abstract.Expression expr) {
    while (definition instanceof Abstract.ClassView) {
      definition = ((Abstract.ClassView) definition).getUnderlyingClassDefCall().getReferent();
    }
    if (definition instanceof Abstract.ClassViewField) {
      definition = ((Abstract.ClassViewField) definition).getUnderlyingField();
    }
    Definition typeCheckedDefinition = myVisitor.getTypecheckingState().getTypechecked(definition);
    if (typeCheckedDefinition == null) {
      throw new IllegalStateException("Internal error: definition " + definition + " was not type checked");
    }
    if (typeCheckedDefinition.typeHasErrors()) {
      LocalTypeCheckingError error = new HasErrors(definition, expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    } else {
      if (typeCheckedDefinition.hasErrors() == Definition.TypeCheckingStatus.HAS_ERRORS) {
        myVisitor.getErrorReporter().report(new HasErrors(Error.Level.WARNING, definition, expr));
      }
      return typeCheckedDefinition;
    }
  }

  public CheckTypeVisitor.PreResult typeCheckDefCall(Abstract.DefCallExpression expr) {
    Abstract.Expression left = expr.getExpression();
    Abstract.Definition resolvedDefinition = expr.getReferent();
    Definition typeCheckedDefinition = null;
    Abstract.ClassView classView = null;
    if (resolvedDefinition != null) {
      typeCheckedDefinition = getTypeCheckedDefinition(resolvedDefinition, expr);
      if (typeCheckedDefinition == null) {
        return null;
      }
      if (resolvedDefinition instanceof Abstract.ClassView && typeCheckedDefinition instanceof ClassDefinition) {
        classView = (Abstract.ClassView) resolvedDefinition;
      }
    }

    CheckTypeVisitor.Result result = null;
    if (left != null && (typeCheckedDefinition == null || !(left instanceof Abstract.DefCallExpression || left instanceof Abstract.ModuleCallExpression))) {
      result = left.accept(myVisitor, null);
      if (result == null) {
        return null;
      }
    }

    // No left-hand side
    if (result == null && typeCheckedDefinition != null) {
      Expression thisExpr = null;
      if (typeCheckedDefinition.getThisClass() != null) {
        if (myThisClass != null) {
          thisExpr = findParent(myThisClass, typeCheckedDefinition, myThisExpr);
        }

        if (thisExpr == null) {
          if (resolvedDefinition instanceof Abstract.ClassViewField) {
            ClassCallExpression classCall = ClassCall(typeCheckedDefinition.getThisClass(), LevelArguments.generateInferVars(typeCheckedDefinition.getThisClass().getPolyParams(), myVisitor.getEquations(), expr));
            thisExpr = new InferenceReferenceExpression(new TypeClassInferenceVariable(typeCheckedDefinition.getThisClass().getName() + "-inst", classCall, ((Abstract.ClassViewField) resolvedDefinition).getOwnView(), true, expr), myVisitor.getEquations());
          } else {
            LocalTypeCheckingError error;
            if (myThisClass != null) {
              error = new LocalTypeCheckingError("Definition '" + typeCheckedDefinition.getName() + "' is not available in this context", expr);
            } else {
              error = new LocalTypeCheckingError("Non-static definitions are not allowed in a static context", expr);
            }
            expr.setWellTyped(myVisitor.getContext(), Error(null, error));
            myVisitor.getErrorReporter().report(error);
            return null;
          }
        }
      }

      return makeResult(typeCheckedDefinition, classView, thisExpr, expr);
    }

    if (left == null) {
      return getLocalVar(expr);
    }

    String name = expr.getName();

    // Field call
    if (result.getType() instanceof Expression) {
      Expression type = ((Expression) result.getType()).normalize(NormalizeVisitor.Mode.WHNF);
      if (type.toClassCall() != null) {
        ClassDefinition classDefinition = type.toClassCall().getDefinition();

        if (typeCheckedDefinition == null) {
          Abstract.Definition member = myVisitor.getDynamicNamespaceProvider().forClass(classDefinition.getAbstractDefinition()).resolveName(name);
          if (member == null) {
            MemberNotFoundError error = new MemberNotFoundError(classDefinition, name, false, expr);
            expr.setWellTyped(myVisitor.getContext(), Error(null, error));
            myVisitor.getErrorReporter().report(error);
            return null;
          }
          typeCheckedDefinition = getTypeCheckedDefinition(member, expr);
          if (typeCheckedDefinition == null) {
            return null;
          }
        } else {
          if (!(typeCheckedDefinition instanceof ClassField && classDefinition.getFieldSet().getFields().contains(typeCheckedDefinition))) {
            throw new IllegalStateException("Internal error: field " + typeCheckedDefinition + " does not belong to class " + classDefinition);
          }
        }

        if (typeCheckedDefinition.getThisClass() == null) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Static definitions are not allowed in a non-static context", expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
        if (!classDefinition.isSubClassOf(typeCheckedDefinition.getThisClass())) {
          ClassCallExpression classCall = ClassCall(typeCheckedDefinition.getThisClass(), LevelArguments.generateInferVars(typeCheckedDefinition.getThisClass().getPolyParams(), myVisitor.getEquations(), expr));
          LocalTypeCheckingError error = new TypeMismatchError(classCall, type, left);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }

        return makeResult(typeCheckedDefinition, null, result.getExpression(), expr);
      }
    }

    // Constructor call
    DataCallExpression dataCall = result.getExpression().toLam() != null ? result.getExpression().toLam().getBody().toDataCall() : result.getExpression().toDataCall();
    if (dataCall != null) {
      DataDefinition dataDefinition = dataCall.getDefinition();
      List<? extends Expression> args = dataCall.getDefCallArguments();
      if (result.getExpression().toLam() != null) {
        args = args.subList(0, args.size() - DependentLink.Helper.size(result.getExpression().toLam().getParameters()));
      }

      Constructor constructor;
      if (typeCheckedDefinition == null) {
        constructor = dataDefinition.getConstructor(name);
        if (constructor == null && !args.isEmpty()) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot find constructor '" + name + "' of data type '" + dataDefinition.getName() + "'", expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      } else {
        if (typeCheckedDefinition instanceof Constructor && dataDefinition.getConstructors().contains(typeCheckedDefinition)) {
          constructor = (Constructor) typeCheckedDefinition;
        } else {
          throw new IllegalStateException("Internal error: " + typeCheckedDefinition + " is not a constructor of " + dataDefinition);
        }
      }

      if (constructor != null) {
        //result.expression = ConCall(constructor, dataCall.getPolyParamsSubst(), new ArrayList<>(args), new ArrayList<Expression>());
        //result.type = result.expression.getType();
        Expression conCall = ConCall(constructor, dataCall.getPolyArguments(), new ArrayList<Expression>(), new ArrayList<Expression>());
        List<DependentLink> conParams = new ArrayList<>();
        Expression conType = constructor.getTypeWithParams(conParams, dataCall.getPolyArguments());
        CheckTypeVisitor.PreResult conResult = new CheckTypeVisitor.PreResult(conCall, conType, conParams);
        conResult.applyExpressions(args);
        return conResult;
      }
    }

    Expression thisExpr = null;
    final Definition leftDefinition;
    Abstract.Definition member = null;
    ClassCallExpression classCall = result.getExpression().toClassCall();
    if (classCall != null) {
      // Static call
      leftDefinition = classCall.getDefinition();
      ClassField parentField = classCall.getDefinition().getEnclosingThisField();
      if (parentField != null) {
        FieldSet.Implementation impl = classCall.getFieldSet().getImplementation(parentField);
        if (impl != null) {
          thisExpr = impl.term;
        }
      }
      if (typeCheckedDefinition == null) {
        member = myVisitor.getStaticNamespaceProvider().forDefinition(leftDefinition.getAbstractDefinition()).resolveName(name);
        if (member == null) {
          MemberNotFoundError error = new MemberNotFoundError(leftDefinition, name, true, expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      }
    } else {
      // Dynamic call
      if (result.getExpression().toDefCall() != null) {
        thisExpr = result.getExpression().toDefCall().getDefCallArguments().size() == 1 ? result.getExpression().toDefCall().getDefCallArguments().get(0) : null;
        leftDefinition = result.getExpression().toDefCall().getDefinition();
      } else {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a definition", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(result.getExpression(), error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }

      if (typeCheckedDefinition == null) {
        Scope scope = myVisitor.getStaticNamespaceProvider().forDefinition(leftDefinition.getAbstractDefinition());
        if (leftDefinition instanceof ClassDefinition) {
          scope = new OverridingScope(scope, myVisitor.getDynamicNamespaceProvider().forClass(((ClassDefinition) leftDefinition).getAbstractDefinition()));
        }
        member = scope.resolveName(name);
        if (member == null) {
          MemberNotFoundError error = new MemberNotFoundError(leftDefinition, name, expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      }
    }

    if (member != null) {
      typeCheckedDefinition = getTypeCheckedDefinition(member, expr);
      if (typeCheckedDefinition == null) {
        return null;
      }
    }

    return makeResult(typeCheckedDefinition, classView, thisExpr, expr);
  }

  private CheckTypeVisitor.PreResult makeResult(Definition definition, Abstract.ClassView classView, Expression thisExpr, Abstract.Expression expr) {
    LevelArguments polyArgs = LevelArguments.generateInferVars(definition.getPolyParams(), myVisitor.getEquations(), expr);
    DefCallExpression defCall;
    if (classView != null) {
      defCall = new ClassViewCallExpression((ClassDefinition) definition, polyArgs, classView);
    } else {
      defCall = definition.getDefCall(polyArgs);
    }

    if (thisExpr == null && definition instanceof ClassField) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Field call without a class instance", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(defCall, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    List<DependentLink> params = new ArrayList<>();
    TypeMax type = definition.getTypeWithParams(params, polyArgs);
    CheckTypeVisitor.PreResult result = new CheckTypeVisitor.PreResult(defCall, type, params);
    if (thisExpr != null) {
      //result.expression = defCall.applyThis(thisExpr);
      //result.type = result.type.applyExpressions(Collections.singletonList(thisExpr));
      result.applyThis(thisExpr);
    }
    return result;
  }

  private Expression findParent(ClassDefinition classDefinition, Definition definition, Expression result) {
    if (classDefinition.isSubClassOf(definition.getThisClass())) {
      return result;
    }
    ClassField parentField = classDefinition.getEnclosingThisField();
    if (parentField == null || parentField.getBaseType().toClassCall() == null) {
      return null;
    }
    return findParent(parentField.getBaseType().toClassCall().getDefinition(), definition, FieldCall(parentField, result));
  }

  public Variable getLocalVar(Abstract.DefCallExpression expr, List<? extends Variable> context) {
    String name = expr.getName();
    ListIterator<? extends Variable> it = context.listIterator(context.size());
    while (it.hasPrevious()) {
      Variable def = it.previous();
      if (name.equals(def.getName())) {
        return def;
      }
    }

    LocalTypeCheckingError error = new NotInScopeError(expr, name);
    expr.setWellTyped(myVisitor.getContext(), Error(null, error));
    myVisitor.getErrorReporter().report(error);
    return null;
  }

  public CheckTypeVisitor.Result getLocalVar(Abstract.DefCallExpression expr) {
    Variable def = getLocalVar(expr, myVisitor.getContext());
    if (def != null) {
      return new CheckTypeVisitor.Result(Reference((Binding)def), ((Binding)def).getType());
    }
    return null;
  }
}
