package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Definition implements Variable, PrettyPrintable {
  private ClassDefinition myThisClass;
  private Abstract.Definition myAbstractDefinition;
  private Map<Integer, ClassField> myClassifyingFields = Collections.emptyMap();
  private TypeCheckingStatus myStatus;

  public Definition(Abstract.Definition abstractDef, TypeCheckingStatus status) {
    myAbstractDefinition = abstractDef;
    myStatus = status;
  }

  @Override
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
    myClassifyingFields = fields.isEmpty() ? Collections.emptyMap() : fields;
  }

  public abstract Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument);

  public abstract DefCallExpression getDefCall(Sort sortArgument, Expression thisExpr, List<Expression> args);

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

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    builder.append(myAbstractDefinition.getName());
  }
}
