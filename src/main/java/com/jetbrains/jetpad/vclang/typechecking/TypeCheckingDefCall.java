package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

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

  private Definition getTypeCheckedDefinition(Abstract.Definition definition, Abstract.Expression expr) {
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
      return typeCheckedDefinition;
    }
  }

  public CheckTypeVisitor.Result typeCheckDefCall(Abstract.DefCallExpression expr) {
    Abstract.Expression left = expr.getExpression();
    Abstract.Definition resolvedDefinition = expr.getReferent();
    Definition typeCheckedDefinition = null;
    ClassView classView = null;
    if (resolvedDefinition != null) {
      if (resolvedDefinition instanceof Abstract.ClassViewField) {
        typeCheckedDefinition = getTypeCheckedDefinition(((Abstract.ClassViewField) resolvedDefinition).getUnderlyingField(), expr);
      } else {
        typeCheckedDefinition = getTypeCheckedDefinition(resolvedDefinition, expr);
      }
      if (typeCheckedDefinition == null) {
        return null;
      }
      if (resolvedDefinition instanceof Abstract.ClassView && typeCheckedDefinition instanceof ClassDefinition) {
        classView = myVisitor.getTypecheckingState().getClassView((Abstract.ClassView) resolvedDefinition);
      }
    }

    CheckTypeVisitor.Result result = null;
    if (left != null && (typeCheckedDefinition == null || !(left instanceof Abstract.DefCallExpression))) {
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
            // TODO: if typeCheckedDefinition.getThisClass() is dynamic, then we should apply it to some this expression
            thisExpr = new InferenceReferenceExpression(new TypeClassInferenceVariable(typeCheckedDefinition.getThisClass().getName() + "-inst", ClassCall(typeCheckedDefinition.getThisClass()), myVisitor.getTypecheckingState().getClassView((Abstract.ClassViewField) resolvedDefinition), true, expr), myVisitor.getEquations());
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
    if (result.type instanceof Expression) {
      Expression type = ((Expression) result.type).normalize(NormalizeVisitor.Mode.WHNF);
      if (type.toClassCall() != null) {
        ClassDefinition classDefinition = type.toClassCall().getDefinition();

        if (typeCheckedDefinition == null) {
          Abstract.Definition member = classDefinition.getInstanceNamespace().resolveName(name);
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
          LocalTypeCheckingError error = new TypeMismatchError(ClassCall(typeCheckedDefinition.getThisClass()), type, left);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }

        return makeResult(typeCheckedDefinition, null, result.expression, expr);
      }
    }

    // Constructor call
    DataCallExpression dataCall = result.expression.toLam() != null ? result.expression.toLam().getBody().toDataCall() : result.expression.toDataCall();
    if (dataCall != null) {
      DataDefinition dataDefinition = dataCall.getDefinition();
      List<? extends Expression> args = dataCall.getDefCallArguments();
      if (result.expression.toLam() != null) {
        args = args.subList(0, args.size() - DependentLink.Helper.size(result.expression.toLam().getParameters()));
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
        result.expression = ConCall(constructor, dataCall.getPolyParamsSubst(), new ArrayList<>(args), new ArrayList<Expression>());
        result.type = result.expression.getType();
        return result;
      }
    }

    // Static call
    Expression thisExpr = null;
    final Definition leftDefinition;
    Abstract.Definition member = null;
    ClassCallExpression classCall = result.expression.toClassCall();
    if (classCall != null) {
      leftDefinition = classCall.getDefinition();
      ClassField parentField = classCall.getDefinition().getEnclosingThisField();
      if (parentField != null) {
        FieldSet.Implementation impl = classCall.getFieldSet().getImplementation(parentField);
        if (impl != null) {
          thisExpr = impl.term;
        }
      }
      if (typeCheckedDefinition == null) {
        member = leftDefinition.getOwnNamespace().resolveName(name);
        if (member == null) {
          MemberNotFoundError error = new MemberNotFoundError(leftDefinition, name, true, expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      }
    } else {
      if (result.expression.toDefCall() != null) {
        thisExpr = null;
        leftDefinition = result.expression.toDefCall().getDefinition();
      } else if (result.expression.toDefCall() != null && result.expression.toDefCall().getDefCallArguments().size() == 1) {
        thisExpr = result.expression.toDefCall().getDefCallArguments().get(0);
        leftDefinition = result.expression.toDefCall().getDefinition();
      } else {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a definition", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(result.expression, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }

      if (typeCheckedDefinition == null) {
        member = new MergeScope(leftDefinition.getOwnNamespace(), leftDefinition.getInstanceNamespace()).resolveName(name);
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

  private CheckTypeVisitor.Result makeResult(Definition definition, ClassView classView, Expression thisExpr, Abstract.Expression expr) {
    LevelSubstitution polySubst = new LevelSubstitution();
    for (Binding polyVar : definition.getPolyParams()) {
      LevelInferenceVariable l = new LevelInferenceVariable(polyVar.getName(), polyVar.getType(), expr);
      polySubst.add(polyVar, new Level(l));
      myVisitor.getEquations().addVariable(l);
    }

    DefCallExpression defCall;
    if (classView != null) {
      defCall = new ClassViewCallExpression((ClassDefinition) definition, polySubst, classView);
    } else {
      defCall = definition.getDefCall(polySubst);
    }

    if (thisExpr == null && definition instanceof ClassField) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Field call without a class instance", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(defCall, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    CheckTypeVisitor.Result result = new CheckTypeVisitor.Result(defCall, definition.getTypeWithThis(polySubst));
    if (thisExpr != null) {
      result.expression = defCall.applyThis(thisExpr);
      result.type = result.type.applyExpressions(Collections.singletonList(thisExpr));
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

  public CheckTypeVisitor.Result getLocalVar(Abstract.DefCallExpression expr) {
    String name = expr.getName();
    ListIterator<Binding> it = myVisitor.getContext().listIterator(myVisitor.getContext().size());
    while (it.hasPrevious()) {
      Binding def = it.previous();
      if (name.equals(def.getName())) {
        return new CheckTypeVisitor.Result(Reference(def), def.getType());
      }
    }

    LocalTypeCheckingError error = new NotInScopeError(expr, name);
    expr.setWellTyped(myVisitor.getContext(), Error(null, error));
    myVisitor.getErrorReporter().report(error);
    return null;
  }
}
