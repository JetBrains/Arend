package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ConcreteProvider extends PartialConcreteProvider {
  @Nullable Concrete.ReferableDefinition getConcrete(GlobalReferable referable);
  @Nullable Concrete.FunctionDefinition getConcreteFunction(GlobalReferable referable);
  @Nullable Concrete.Instance getConcreteInstance(GlobalReferable referable);
  @Nullable Concrete.ClassDefinition getConcreteClass(ClassReferable referable);

  @Override
  @Nullable
  default Concrete.ReferenceExpression getInstanceClassReference(GlobalReferable instance) {
    Concrete.Instance concreteInstance = getConcreteInstance(instance);
    return concreteInstance == null ? null : concreteInstance.getReferenceExpressionInType();
  }

  @Override
  default boolean isRecord(ClassReferable classRef) {
    Concrete.ClassDefinition classDef = getConcreteClass(classRef);
    return classDef != null && classDef.isRecord();
  }

  @Override
  default boolean isInstance(GlobalReferable ref) {
    return getConcreteInstance(ref) != null;
  }

  @Nullable
  @Override
  default Collection<InstanceParameter> getInstanceParameterReferences(GlobalReferable ref) {
    Concrete.Instance def = getConcreteInstance(ref);
    if (def == null) {
      return null;
    }

    List<InstanceParameter> result = new ArrayList<>(def.getParameters().size());
    for (Concrete.Parameter parameter : def.getParameters()) {
      if (parameter.getExplicit() || parameter instanceof Concrete.TypeParameter) {
        Referable typeRef = parameter instanceof Concrete.TypeParameter ? ((Concrete.TypeParameter) parameter).getType().getUnderlyingReferable() : null;
        result.add(new InstanceParameter(parameter.getExplicit(), typeRef instanceof GlobalReferable ? (GlobalReferable) typeRef : null, parameter));
      }
    }
    return result;
  }
}
