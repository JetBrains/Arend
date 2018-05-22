package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TypedReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteReferableDefinitionVisitor;

import java.util.Collection;
import java.util.Collections;

public class TypeClassReferenceExtractVisitor implements ConcreteReferableDefinitionVisitor<Void, ClassReferable> {
  @Override
  public ClassReferable visitFunction(Concrete.FunctionDefinition def, Void params) {
    return getTypeClassReference(def.getParameters(), def.getResultType());
  }

  @Override
  public ClassReferable visitData(Concrete.DataDefinition def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitClass(Concrete.ClassDefinition def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitClassSynonym(Concrete.ClassSynonym def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitInstance(Concrete.Instance def, Void params) {
    return getTypeClassReference(def.getParameters(), def.getClassReference());
  }

  @Override
  public ClassReferable visitConstructor(Concrete.Constructor def, Void params) {
    return null;
  }

  @Override
  public ClassReferable visitClassField(Concrete.ClassField def, Void params) {
    return getTypeClassReference(Collections.emptyList(), def.getResultType());
  }

  @Override
  public ClassReferable visitClassFieldSynonym(Concrete.ClassFieldSynonym def, Void params) {
    Referable fieldRef = def.getUnderlyingField().getReferent();
    return fieldRef instanceof TypedReferable ? ((TypedReferable) fieldRef).getTypeClassReference() : null;
  }

  public static Referable getTypeReference(Collection<? extends Concrete.Parameter> parameters, Concrete.Expression type) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter.getExplicit()) {
        return null;
      }
    }

    while (type instanceof Concrete.PiExpression) {
      for (Concrete.TypeParameter parameter : ((Concrete.PiExpression) type).getParameters()) {
        if (parameter.getExplicit()) {
          return null;
        }
      }
      type = ((Concrete.PiExpression) type).getCodomain();
    }

    return type instanceof Concrete.ReferenceExpression ? ((Concrete.ReferenceExpression) type).getReferent() : null;
  }

  private static ClassReferable getTypeClassReference(Collection<? extends Concrete.Parameter> parameters, Concrete.Expression type) {
    Referable ref = getTypeReference(parameters, type);
    return ref instanceof ClassReferable ? (ClassReferable) ref : null;
  }
}
