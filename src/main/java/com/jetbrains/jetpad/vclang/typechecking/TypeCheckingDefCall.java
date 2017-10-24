package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

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

  private Definition getTypeCheckedDefinition(GlobalReferable definition, Concrete.Expression expr) {
    /* TODO[classes]: I'm not sure what to do with this. Maybe eliminate class views and their fields during name resolving
    while (definition instanceof Concrete.ClassView) {
      definition = (GlobalReferable) ((Concrete.ClassView) definition).getUnderlyingClass().getReferent();
    }
    if (definition instanceof Concrete.ClassViewField) {
      definition = (GlobalReferable) ((Concrete.ClassViewField) definition).getUnderlyingField();
    }
    */
    Definition typeCheckedDefinition = myVisitor.getTypecheckingState().getTypechecked(definition);
    if (typeCheckedDefinition == null) {
      myVisitor.getErrorReporter().report(new IncorrectReferenceError(definition));
      return null;
    }
    if (!typeCheckedDefinition.status().headerIsOK()) {
      myVisitor.getErrorReporter().report(new HasErrors(Error.Level.ERROR, definition, expr));
      return null;
    } else {
      if (typeCheckedDefinition.status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS) {
        myVisitor.getErrorReporter().report(new HasErrors(Error.Level.WARNING, definition, expr));
      }
      return typeCheckedDefinition;
    }
  }

  public CheckTypeVisitor.TResult typeCheckDefCall(GlobalReferable resolvedDefinition, Concrete.ReferenceExpression expr) {
    Definition typeCheckedDefinition = getTypeCheckedDefinition(resolvedDefinition, expr);
    if (typeCheckedDefinition == null) {
      return null;
    }

    Expression thisExpr = null;
    if (typeCheckedDefinition.getThisClass() != null) {
      if (myThisClass != null) {
        thisExpr = findParent(myThisClass, typeCheckedDefinition, new ReferenceExpression(myThisBinding));
      }

      if (thisExpr == null) {
        /* TODO[classes]
        if (resolvedDefinition instanceof Concrete.ClassViewField) {
          assert typeCheckedDefinition instanceof ClassField;
          Concrete.ClassView ownClassView = ((Concrete.ClassViewField) resolvedDefinition).getOwnView();
          ClassCallExpression classCall = new ClassCallExpression(typeCheckedDefinition.getThisClass(), Sort.generateInferVars(myVisitor.getEquations(), expr));
          thisExpr = new InferenceReferenceExpression(new TypeClassInferenceVariable<>(typeCheckedDefinition.getThisClass().getName() + "-inst", classCall, ownClassView, true, expr, myVisitor.getAllBindings()), myVisitor.getEquations());
        } else { */
          TypecheckingError error;
          if (myThisClass != null) {
            error = new NotAvailableDefinitionError(typeCheckedDefinition, expr);
          } else {
            error = new TypecheckingError("Non-static definitions are not allowed in a static context", expr);
          }
          myVisitor.getErrorReporter().report(error);
          return null;
        // }
      }
    }

    return makeResult(typeCheckedDefinition, thisExpr, expr);
  }

  private CheckTypeVisitor.TResult makeResult(Definition definition, Expression thisExpr, Concrete.ReferenceExpression expr) {
    Sort sortArgument;
    if (definition instanceof DataDefinition && !definition.getParameters().hasNext()) {
      sortArgument = Sort.PROP;
    } else {
      if (expr.getPLevel() == null && expr.getHLevel() == null) {
        sortArgument = Sort.generateInferVars(myVisitor.getEquations(), expr);
      } else {
        Level pLevel = null;
        if (expr.getPLevel() != null) {
          pLevel = expr.getPLevel().accept(myVisitor, LevelVariable.PVAR);
        }
        if (pLevel == null) {
          InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, expr.getPLevel());
          myVisitor.getEquations().addVariable(pl);
          pLevel = new Level(pl);
        }

        Level hLevel = null;
        if (expr.getHLevel() != null) {
          hLevel = expr.getHLevel().accept(myVisitor, LevelVariable.HVAR);
        }
        if (hLevel == null) {
          InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, expr.getHLevel());
          myVisitor.getEquations().addVariable(hl);
          hLevel = new Level(hl);
        }

        sortArgument = new Sort(pLevel, hLevel);
      }
    }

    if (thisExpr == null && definition instanceof ClassField) {
      myVisitor.getErrorReporter().report(new TypecheckingError("Field call without a class instance", expr));
      return null;
    }

    if (expr.getPLevel() == null && expr.getHLevel() == null) {
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
