package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.HasErrors;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.MemberNotFoundError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.List;

public class TypeCheckingDefCall {
  private final CheckTypeVisitor myVisitor;
  private ClassDefinition myThisClass;
  private Binding myThisBinding;

  public TypeCheckingDefCall(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public Binding getThisBinding() {
    return myThisBinding;
  }

  public void setThis(ClassDefinition thisClass, Binding thisBinding) {
    myThisClass = thisClass;
    myThisBinding = thisBinding;
  }

  private Definition getTypeCheckedDefinition(Abstract.Definition definition, Abstract.Expression expr) {
    while (definition instanceof Abstract.ClassView) {
      definition = (Abstract.Definition) ((Abstract.ClassView) definition).getUnderlyingClassReference().getReferent();
    }
    if (definition instanceof Abstract.ClassViewField) {
      definition = ((Abstract.ClassViewField) definition).getUnderlyingField();
    }
    Definition typeCheckedDefinition = myVisitor.getTypecheckingState().getTypechecked(definition);
    if (typeCheckedDefinition == null) {
      throw new IllegalStateException("Internal error: definition " + definition + " was not type checked");
    }
    if (!typeCheckedDefinition.status().headerIsOK()) {
      LocalTypeCheckingError error = new HasErrors(definition, expr);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    } else {
      if (typeCheckedDefinition.status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS) {
        myVisitor.getErrorReporter().report(new HasErrors(Error.Level.WARNING, definition, expr));
      }
      return typeCheckedDefinition;
    }
  }

  public CheckTypeVisitor.TResult typeCheckDefCall(Abstract.ReferenceExpression expr) {
    Abstract.Expression left = expr.getExpression();
    Abstract.Definition resolvedDefinition = expr.getReferent() instanceof Abstract.Definition ? (Abstract.Definition) expr.getReferent() : null;
    Definition typeCheckedDefinition = null;
    if (resolvedDefinition != null) {
      typeCheckedDefinition = getTypeCheckedDefinition(resolvedDefinition, expr);
      if (typeCheckedDefinition == null) {
        return null;
      }
    }

    CheckTypeVisitor.Result result = null;
    if (left != null && (typeCheckedDefinition == null || (!(left instanceof Abstract.ReferenceExpression) && !(left instanceof Abstract.ModuleCallExpression)))) {
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
          thisExpr = findParent(myThisClass, typeCheckedDefinition, new ReferenceExpression(myThisBinding));
        }

        if (thisExpr == null) {
          if (resolvedDefinition instanceof Abstract.ClassViewField) {
            assert typeCheckedDefinition instanceof ClassField;
            Abstract.ClassView ownClassView = ((Abstract.ClassViewField) resolvedDefinition).getOwnView();
            ClassCallExpression classCall = new ClassCallExpression(typeCheckedDefinition.getThisClass(), Sort.generateInferVars(myVisitor.getEquations(), expr));
            thisExpr = new InferenceReferenceExpression(new TypeClassInferenceVariable(typeCheckedDefinition.getThisClass().getName() + "-inst", classCall, expr, 0, ownClassView, (ClassField) myVisitor.getTypecheckingState().getTypechecked(ownClassView.getClassifyingField())), myVisitor.getEquations());
          } else {
            LocalTypeCheckingError error;
            if (myThisClass != null) {
              error = new LocalTypeCheckingError("Definition '" + typeCheckedDefinition.getName() + "' is not available in this context", expr);
            } else {
              error = new LocalTypeCheckingError("Non-static definitions are not allowed in a static context", expr);
            }
            expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
            myVisitor.getErrorReporter().report(error);
            return null;
          }
        }
      }

      return makeResult(typeCheckedDefinition, thisExpr, expr);
    }

    if (left == null) {
      // TODO: Create a separate expression for local variables
      throw new IllegalStateException();
    }

    String name = expr.getName();

    // Field call
    Expression type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (type.isInstance(ClassCallExpression.class)) {
      ClassDefinition classDefinition = type.cast(ClassCallExpression.class).getDefinition();

      if (typeCheckedDefinition == null) {
        Abstract.Definition member = myVisitor.getDynamicNamespaceProvider().forClass(classDefinition.getAbstractDefinition()).resolveName(name);
        if (member == null) {
          MemberNotFoundError error = new MemberNotFoundError(classDefinition, name, false, expr);
          expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
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
        expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }
      if (!classDefinition.isSubClassOf(typeCheckedDefinition.getThisClass())) {
        ClassCallExpression classCall = new ClassCallExpression(typeCheckedDefinition.getThisClass(), Sort.generateInferVars(myVisitor.getEquations(), expr));
        LocalTypeCheckingError error = new TypeMismatchError(classCall, type, left);
        expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }

      return makeResult(typeCheckedDefinition, result.expression, expr);
    }

    int lamSize = 0;
    Expression lamExpr = result.expression;
    while (lamExpr.isInstance(LamExpression.class)) {
      lamSize += DependentLink.Helper.size(lamExpr.cast(LamExpression.class).getParameters());
      lamExpr = lamExpr.cast(LamExpression.class).getBody();
    }

    // Constructor call
    DataCallExpression dataCall = lamExpr.checkedCast(DataCallExpression.class);
    if (dataCall != null) {
      DataDefinition dataDefinition = dataCall.getDefinition();
      List<? extends Expression> args = dataCall.getDefCallArguments();
      if (result.expression.isInstance(LamExpression.class)) {
        args = args.subList(0, args.size() - lamSize);
      }

      Constructor constructor;
      if (typeCheckedDefinition == null) {
        constructor = dataDefinition.getConstructor(name);
        if (constructor == null && !args.isEmpty()) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot find constructor '" + name + "' of data type '" + dataDefinition.getName() + "'", expr);
          expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
        if (constructor != null && !constructor.status().headerIsOK()) {
          LocalTypeCheckingError error = new HasErrors(constructor.getAbstractDefinition(), expr);
          expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
        if (constructor != null && constructor.status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS) {
          myVisitor.getErrorReporter().report(new HasErrors(Error.Level.WARNING, constructor.getAbstractDefinition(), expr));
        }
      } else {
        if (typeCheckedDefinition instanceof Constructor && dataDefinition.getConstructors().contains(typeCheckedDefinition)) {
          constructor = (Constructor) typeCheckedDefinition;
        } else {
          throw new IllegalStateException("Internal error: " + typeCheckedDefinition + " is not a constructor of " + dataDefinition);
        }
      }

      if (constructor != null) {
        CheckTypeVisitor.TResult result1 = CheckTypeVisitor.DefCallResult.makeTResult(expr, constructor, dataCall.getSortArgument(), null);
        return args.isEmpty() ? result1 : ((CheckTypeVisitor.DefCallResult) result1).applyExpressions(args);
      }
    }

    Expression thisExpr = null;
    final Definition leftDefinition;
    Abstract.Definition member = null;
    ClassCallExpression classCall = result.expression.checkedCast(ClassCallExpression.class);
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
          expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
          myVisitor.getErrorReporter().report(error);
          return null;
        }
      }
    } else {
      // Dynamic call
      if (result.expression.isInstance(DefCallExpression.class)) {
        DefCallExpression defCall = result.expression.cast(DefCallExpression.class);
        thisExpr = defCall.getDefCallArguments().size() == 1 ? defCall.getDefCallArguments().get(0) : null;
        leftDefinition = defCall.getDefinition();
      } else {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a definition", expr);
        expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(result.expression, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }

      if (typeCheckedDefinition == null) {
        if (!(leftDefinition instanceof ClassField)) { // Some class fields do not have abstract definitions
          Scope scope = new NamespaceScope(myVisitor.getStaticNamespaceProvider().forDefinition(leftDefinition.getAbstractDefinition()));
          if (leftDefinition instanceof ClassDefinition) {
            scope = new OverridingScope(scope, new NamespaceScope(myVisitor.getDynamicNamespaceProvider().forClass(((ClassDefinition) leftDefinition).getAbstractDefinition())));
          }
          member = scope.resolveName(name);
        }
        if (member == null) {
          MemberNotFoundError error = new MemberNotFoundError(leftDefinition, name, expr);
          expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
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

    return makeResult(typeCheckedDefinition, thisExpr, expr);
  }

  private CheckTypeVisitor.TResult makeResult(Definition definition, Expression thisExpr, Abstract.ReferenceExpression expr) {
    Sort sortArgument = (definition instanceof DataDefinition || definition instanceof FunctionDefinition) && !definition.getParameters().hasNext() ? Sort.PROP : Sort.generateInferVars(myVisitor.getEquations(), expr);

    if (thisExpr == null && definition instanceof ClassField) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Field call without a class instance", expr);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
      myVisitor.getErrorReporter().report(error);
      return null;
    }

    Level hLevel = null;
    if (definition instanceof DataDefinition && !sortArgument.isProp()) {
      hLevel = ((DataDefinition) definition).getSort().getHLevel();
    } else if (definition instanceof FunctionDefinition && !sortArgument.isProp()) {
      UniverseExpression universe = ((FunctionDefinition) definition).getResultType().getPiParameters(null, false).checkedCast(UniverseExpression.class);
      if (universe != null) {
        hLevel = universe.getSort().getHLevel();
      }
    }
    if (hLevel != null && hLevel.getConstant() == -1 && hLevel.getVar() == LevelVariable.HVAR && hLevel.getMaxConstant() == 0) {
      myVisitor.getEquations().bindVariables((InferenceLevelVariable) sortArgument.getPLevel().getVar(), (InferenceLevelVariable) sortArgument.getHLevel().getVar());
    }

    return CheckTypeVisitor.DefCallResult.makeTResult(expr, definition, sortArgument, thisExpr);
  }

  private Expression findParent(ClassDefinition classDefinition, Definition definition, Expression result) {
    if (classDefinition.isSubClassOf(definition.getThisClass())) {
      return result;
    }
    ClassField parentField = classDefinition.getEnclosingThisField();
    if (parentField == null) {
      return null;
    }
    ClassCallExpression classCall = parentField.getBaseType(Sort.STD).checkedCast(ClassCallExpression.class);
    if (classCall == null) {
      return null;
    }
    return findParent(classCall.getDefinition(), definition, ExpressionFactory.FieldCall(parentField, result));
  }
}
