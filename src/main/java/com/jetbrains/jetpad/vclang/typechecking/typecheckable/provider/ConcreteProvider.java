package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nullable;

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
}
