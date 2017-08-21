package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.List;

public class TypeCheckingDefCall<T> {
  private final CheckTypeVisitor<T> myVisitor;
  private ClassDefinition myThisClass;
  private Binding myThisBinding;

  public TypeCheckingDefCall(CheckTypeVisitor<T> visitor) {
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

  private Definition getTypeCheckedDefinition(GlobalReferable definition, Concrete.Expression<T> expr) {
    while (definition instanceof Abstract.ClassView) { // TODO[abstract]: eliminate class views and their fields during name resolving
      definition = (GlobalReferable) ((Abstract.ClassView) definition).getUnderlyingClassReference().getReferent();
    }
    if (definition instanceof Abstract.ClassViewField) {
      definition = ((Abstract.ClassViewField) definition).getUnderlyingField();
    }
    Definition typeCheckedDefinition = myVisitor.getTypecheckingState().getTypechecked(definition);
    if (typeCheckedDefinition == null) {
      throw new IllegalStateException("Internal error: definition " + definition + " was not type checked");
    }
    if (!typeCheckedDefinition.status().headerIsOK()) {
      myVisitor.getErrorReporter().report(new HasErrors<>(Error.Level.ERROR, definition, expr));
      return null;
    } else {
      if (typeCheckedDefinition.status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS) {
        myVisitor.getErrorReporter().report(new HasErrors<>(Error.Level.WARNING, definition, expr));
      }
      return typeCheckedDefinition;
    }
  }

  public CheckTypeVisitor.TResult<T> typeCheckDefCall(Concrete.ReferenceExpression<T> expr) {
    Concrete.Expression<T> left = expr.getExpression();
    GlobalReferable resolvedDefinition = expr.getReferent() instanceof GlobalReferable ? (GlobalReferable) expr.getReferent() : null;
    Definition typeCheckedDefinition = null;
    if (resolvedDefinition != null) {
      typeCheckedDefinition = getTypeCheckedDefinition(resolvedDefinition, expr);
      if (typeCheckedDefinition == null) {
        return null;
      }
    }

    CheckTypeVisitor.Result result = null;
    if (left != null && (typeCheckedDefinition == null || (!(left instanceof Concrete.ReferenceExpression) && !(left instanceof Concrete.ModuleCallExpression)))) {
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
            thisExpr = new InferenceReferenceExpression(new TypeClassInferenceVariable<>(typeCheckedDefinition.getThisClass().getName() + "-inst", classCall, ownClassView, true, expr, myVisitor.getAllBindings()), myVisitor.getEquations());
          } else {
            LocalTypeCheckingError<T> error;
            if (myThisClass != null) {
              error = new NotAvailableDefinitionError<>(typeCheckedDefinition, expr);
            } else {
              error = new LocalTypeCheckingError<>("Non-static definitions are not allowed in a static context", expr);
            }
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
        Abstract.Definition member = myVisitor.getDynamicNamespaceProvider().forClass(classDefinition.getConcreteDefinition()).resolveName(name);
        if (member == null) {
          myVisitor.getErrorReporter().report(new MemberNotFoundError<>(classDefinition, name, false, expr));
          return null;
        }
        typeCheckedDefinition = getTypeCheckedDefinition(member, expr);
        if (typeCheckedDefinition == null) {
          return null;
        }
      } else {
        if (!(typeCheckedDefinition instanceof ClassField && classDefinition.getFields().contains(typeCheckedDefinition))) {
          throw new IllegalStateException("Internal error: field " + typeCheckedDefinition + " does not belong to class " + classDefinition);
        }
      }

      if (typeCheckedDefinition.getThisClass() == null) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("Static definitions are not allowed in a non-static context", expr));
        return null;
      }
      if (!classDefinition.isSubClassOf(typeCheckedDefinition.getThisClass())) {
        ClassCallExpression classCall = new ClassCallExpression(typeCheckedDefinition.getThisClass(), Sort.generateInferVars(myVisitor.getEquations(), expr));
        myVisitor.getErrorReporter().report(new TypeMismatchError<>(DocFactory.termDoc(classCall), DocFactory.termDoc(type), left));
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
          myVisitor.getErrorReporter().report(new MissingConstructorError<>(name, dataDefinition, expr));
          return null;
        }
        if (constructor != null && !constructor.status().headerIsOK()) {
          myVisitor.getErrorReporter().report(new HasErrors<>(Error.Level.ERROR, constructor.getConcreteDefinition(), expr));
          return null;
        }
        if (constructor != null && constructor.status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS) {
          myVisitor.getErrorReporter().report(new HasErrors<>(Error.Level.WARNING, constructor.getConcreteDefinition(), expr));
        }
      } else {
        if (typeCheckedDefinition instanceof Constructor && dataDefinition.getConstructors().contains(typeCheckedDefinition)) {
          constructor = (Constructor) typeCheckedDefinition;
        } else {
          throw new IllegalStateException("Internal error: " + typeCheckedDefinition + " is not a constructor of " + dataDefinition);
        }
      }

      if (constructor != null) {
        CheckTypeVisitor.TResult<T> result1 = CheckTypeVisitor.DefCallResult.makeTResult(expr, constructor, dataCall.getSortArgument(), null);
        return args.isEmpty() ? result1 : ((CheckTypeVisitor.DefCallResult) result1).applyExpressions(args);
      }
    }

    Expression thisExpr = null;
    final Definition leftDefinition;
    Referable member = null;
    ClassCallExpression classCall = result.expression.checkedCast(ClassCallExpression.class);
    if (classCall != null) {
      // Static call
      leftDefinition = classCall.getDefinition();
      ClassField parentField = classCall.getDefinition().getEnclosingThisField();
      if (parentField != null) {
        thisExpr = classCall.getImplementation(parentField, null /* it should be OK */);
      }
      if (typeCheckedDefinition == null) {
        member = myVisitor.getStaticNamespaceProvider().forReferable(leftDefinition.getConcreteDefinition()).resolveName(name);
        if (member == null) {
          myVisitor.getErrorReporter().report(new MemberNotFoundError<>(leftDefinition, name, true, expr));
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
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("Expected a definition", expr));
        return null;
      }

      if (typeCheckedDefinition == null) {
        if (!(leftDefinition instanceof ClassField)) { // Some class fields do not have abstract definitions
          Scope scope = new NamespaceScope(myVisitor.getStaticNamespaceProvider().forReferable(leftDefinition.getConcreteDefinition()));
          if (leftDefinition instanceof ClassDefinition) {
            scope = new OverridingScope(scope, new NamespaceScope(myVisitor.getDynamicNamespaceProvider().forClass(((ClassDefinition) leftDefinition).getConcreteDefinition())));
          }
          member = scope.resolveName(name);
        }
        if (!(member instanceof GlobalReferable)) {
          myVisitor.getErrorReporter().report(new MemberNotFoundError<>(leftDefinition, name, expr));
          return null;
        }
      }
    }

    if (member != null) {
      typeCheckedDefinition = getTypeCheckedDefinition((GlobalReferable) member, expr);
      if (typeCheckedDefinition == null) {
        return null;
      }
    }

    return makeResult(typeCheckedDefinition, thisExpr, expr);
  }

  private CheckTypeVisitor.TResult<T> makeResult(Definition definition, Expression thisExpr, Concrete.ReferenceExpression<T> expr) {
    Sort sortArgument = definition instanceof DataDefinition && !definition.getParameters().hasNext() ? Sort.PROP : Sort.generateInferVars(myVisitor.getEquations(), expr);

    if (thisExpr == null && definition instanceof ClassField) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("Field call without a class instance", expr));
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
      //noinspection unchecked
      myVisitor.getEquations().bindVariables((InferenceLevelVariable<T>) sortArgument.getPLevel().getVar(), (InferenceLevelVariable<T>) sortArgument.getHLevel().getVar());
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
