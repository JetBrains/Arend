package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.typechecking.error.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class TypeCheckingDefCall {
  private final TypecheckerState myState;
  private final Abstract.Definition myParentDefinition;
  private final CheckTypeVisitor myVisitor;
  private ClassDefinition myThisClass;
  private Expression myThisExpr;

  public TypeCheckingDefCall(TypecheckerState state, Abstract.Definition definition, CheckTypeVisitor visitor) {
    myState = state;
    myParentDefinition = definition;
    myVisitor = visitor;
  }

  public void setThisClass(ClassDefinition thisClass, Expression thisExpr) {
    myThisClass = thisClass;
    myThisExpr = thisExpr;
  }

  private Definition getTypeCheckedDefinition(Abstract.Definition definition, Abstract.Expression expr) {
    Definition typeCheckedDefinition = myState.getTypechecked(definition);
    if (typeCheckedDefinition == null) {
      throw new IllegalStateException("Internal error: definition " + definition + " was not typechecked");
    }
    if (typeCheckedDefinition.typeHasErrors()) {
      TypeCheckingError error = new HasErrors(myParentDefinition, definition, expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    } else {
      return typeCheckedDefinition;
    }
  }

  public CheckTypeVisitor.Result typeCheckDefCall(Abstract.DefCallExpression expr) {
    Abstract.Definition resolvedDefinition = expr.getReferent();
    if (resolvedDefinition != null) {
      Definition typeCheckedDefinition = getTypeCheckedDefinition(resolvedDefinition, expr);
      if (typeCheckedDefinition == null) {
        return null;
      }

      Expression thisExpr = null;
      if (typeCheckedDefinition.getThisClass() != null) {
        if (myThisClass != null) {
          thisExpr = findParent(myThisClass, typeCheckedDefinition, myThisExpr, expr);
          if (thisExpr == null) {
            return null;
          }
        } else {
          TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Non-static definitions are not allowed in a static context", expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      }
      return applyThis(insertPolyVars(typeCheckedDefinition, expr), thisExpr, expr);
    }


    Abstract.Expression left = expr.getExpression();
    if (left == null) {
      return getLocalVar(expr);
    }
    CheckTypeVisitor.Result result = left.accept(myVisitor, null);
    if (result == null) {
      return null;
    }
    String name = expr.getName();

    if (result.type instanceof Expression) {
      Expression type = ((Expression) result.type).normalize(NormalizeVisitor.Mode.WHNF);
      if (type.toClassCall() != null) {
        ClassDefinition classDefinition = type.toClassCall().getDefinition();

        Definition definition;
        Abstract.Definition member = classDefinition.getInstanceNamespace().resolveName(name);
        if (member == null) {
          MemberNotFoundError error = new MemberNotFoundError(myParentDefinition, classDefinition, name, false, expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
        definition = getTypeCheckedDefinition(member, expr);
        if (definition == null) {
          return null;
        }

        if (definition.getThisClass() == null) {
          TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Static definitions are not allowed in a non-static context", expr);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
        if (!classDefinition.isSubClassOf(definition.getThisClass())) {
          TypeCheckingError error = new TypeMismatchError(myParentDefinition, definition.getThisClass().getDefCall(), type, left);
          expr.setWellTyped(myVisitor.getContext(), Error(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }

        Expression thisExpr = result.expression;
        result.expression = definition.getDefCall();
        result.type = definition.getTypeWithThis();
        return applyThis(result, thisExpr, expr);
      }
    }


    List<? extends Expression> arguments = result.expression.getArguments();
    Expression fun = result.expression.getFunction();
    DataCallExpression dataCall = fun.toDataCall();
    if (dataCall != null) {
      DataDefinition dataDefinition = dataCall.getDefinition();
      Constructor constructor = dataDefinition.getConstructor(name);
      if (constructor != null) {
        result.expression = ConCall(constructor, new ArrayList<>(arguments)).applyLevelSubst(dataCall.getPolyParamsSubst());
        result.type = result.expression.getType();
        return result;
      }

      if (!arguments.isEmpty()) {
        TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Cannot find constructor '" + name + "' of data type '" + dataDefinition.getName() + "'", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    final Definition leftDefinition;
    final Abstract.Definition member;
    Expression thisExpr = null;
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
      member = leftDefinition.getOwnNamespace().resolveName(expr.getName());
      if (member == null) {
        MemberNotFoundError error = new MemberNotFoundError(myParentDefinition, leftDefinition, name, true, expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    } else {
      if (result.expression.toDefCall() != null) {
        thisExpr = null;
        leftDefinition = result.expression.toDefCall().getDefinition();
      } else if (result.expression.getFunction().toDefCall() != null && result.expression.getArguments().size() == 1) {
        thisExpr = result.expression.getArguments().get(0);
        leftDefinition = result.expression.getFunction().toDefCall().getDefinition();
      } else {
        TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Expected a definition", expr);
        expr.setWellTyped(myVisitor.getContext(), Error(result.expression, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }

      member = new MergeScope(leftDefinition.getOwnNamespace(), leftDefinition.getInstanceNamespace()).resolveName(name);
      if (member == null) {
        MemberNotFoundError error = new MemberNotFoundError(myParentDefinition, leftDefinition, name, expr);
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
    }

    Definition definition = getTypeCheckedDefinition(member, expr);
    if (definition == null) {
      return null;
    }

    result.expression = definition.getDefCall();
    result.type = definition.getTypeWithThis();
    return applyThis(result, thisExpr, expr);
  }

  private CheckTypeVisitor.Result insertPolyVars(Definition definition, Abstract.SourceNode sourceNode) {
    if (definition.isPolymorphic()) {
      LevelSubstitution subst = new LevelSubstitution();

      CheckTypeVisitor.Result result = new CheckTypeVisitor.Result(null, null);
      for (Binding polyVar : definition.getPolyParams()) {
        LevelInferenceVariable l = new LevelInferenceVariable(polyVar.getName(), polyVar.getType(), sourceNode);
        subst.add(polyVar, new Level(l));
        myVisitor.getEquations().addVariable(l);
      }

      DefCallExpression defCall = definition.getDefCall(subst);
      result.expression = defCall;
      result.type = defCall.getDefinition().getTypeWithThis().subst(new ExprSubstitution(), subst);
      return result;
    }

    return new CheckTypeVisitor.Result(definition.getDefCall(), definition.getTypeWithThis());
  }

  private Expression findParent(ClassDefinition classDefinition, Definition definition, Expression result, Abstract.Expression expr) {
    if (classDefinition.isSubClassOf(definition.getThisClass())) {
      return result;
    }
    ClassField parentField = classDefinition.getEnclosingThisField();
    if (parentField == null || parentField.getBaseType().toClassCall() == null) {
      TypeCheckingError error = new TypeCheckingError(myParentDefinition, "Definition '" + definition.getName() + "' is not available in this context", expr);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }
    return findParent(parentField.getBaseType().toClassCall().getDefinition(), definition, Apps(FieldCall(parentField), result), expr);
  }

  private CheckTypeVisitor.Result applyThis(CheckTypeVisitor.Result result, Expression thisExpr, Abstract.Expression expr) {
    //assert thisExpr != null;  // FIXME
    DefCallExpression defCall = result.expression.toDefCall();
    //assert defCall.getDefinition().getThisClass() != null;  // FIXME
    if (thisExpr != null) {  // FIXME
      result.expression = defCall.applyThis(thisExpr);
      result.type = result.type.applyExpressions(Collections.singletonList(thisExpr));
    }
    return result;
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

    TypeCheckingError error = new NotInScopeError(myParentDefinition, expr, name);
    expr.setWellTyped(myVisitor.getContext(), Error(null, error));
    myVisitor.getErrorReporter().report(error);
    return null;
  }
}
