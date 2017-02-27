package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Definition implements Variable {
  private ClassDefinition myThisClass;
  private Abstract.Definition myAbstractDefinition;
  private Map<Integer, ClassField> myClassifyingFields = Collections.emptyMap();
  private TypeCheckingStatus myStatus;

  public Definition(Abstract.Definition abstractDef, TypeCheckingStatus status) {
    myAbstractDefinition = abstractDef;
    myStatus = status;
  }

  public String getName() {
    return myAbstractDefinition.getName();
  }

  public Abstract.Definition getAbstractDefinition() {
    return myAbstractDefinition;
  }

  public DependentLink getParameters() {
    return EmptyDependentLink.getInstance();
  }

  public ClassField getClassifyingFieldOfParameter(Integer param) {
    return myClassifyingFields.get(param);
  }

  public void setClassifyingFieldOfParameter(Integer param, ClassField field) {
    if (myClassifyingFields.isEmpty()) {
      myClassifyingFields = new HashMap<>();
    }
    myClassifyingFields.put(param, field);
  }

  public void setClassifyingFieldsOfParameters(Map<Integer, ClassField> fields) {
    myClassifyingFields = fields.isEmpty() ? Collections.<Integer, ClassField>emptyMap() : fields;
  }

  public abstract TypeMax getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments);

  public abstract DefCallExpression getDefCall(LevelArguments polyArguments, Expression thisExpr);

  public abstract Expression getDefCall(LevelArguments polyArguments, List<Expression> args);

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public void setThisClass(ClassDefinition enclosingClass) {
    myThisClass = enclosingClass;
  }

  public enum TypeCheckingStatus {
    HEADER_HAS_ERRORS, BODY_HAS_ERRORS, HEADER_NEEDS_TYPE_CHECKING, BODY_NEEDS_TYPE_CHECKING, HAS_ERRORS, NO_ERRORS;

    public boolean bodyIsOK() {
      return this == HAS_ERRORS || this == NO_ERRORS;
    }

    public boolean headerIsOK() {
      return this != HEADER_HAS_ERRORS && this != HEADER_NEEDS_TYPE_CHECKING;
    }

    public boolean needsTypeChecking() {
      return this == HEADER_NEEDS_TYPE_CHECKING || this == BODY_NEEDS_TYPE_CHECKING;
    }
  }

  public TypeCheckingStatus status() {
    return myStatus;
  }

  public void setStatus(TypeCheckingStatus status) {
    myStatus = status;
  }

  @Override
  public String toString() {
    return myAbstractDefinition.toString();
  }

  public static Definition newDefinition(Abstract.Definition definition) {
    if (definition instanceof Abstract.DataDefinition) {
      return new DataDefinition((Abstract.DataDefinition) definition);
    }
    if (definition instanceof Abstract.FunctionDefinition || definition instanceof Abstract.ClassViewInstance) {
      return new FunctionDefinition(definition);
    }
    if (definition instanceof Abstract.ClassDefinition) {
      return new ClassDefinition((Abstract.ClassDefinition) definition);
    }
    return null;
  }
}
