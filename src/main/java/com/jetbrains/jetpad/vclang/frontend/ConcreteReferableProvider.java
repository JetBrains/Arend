package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteLocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import javax.annotation.Nullable;

public class ConcreteReferableProvider implements ConcreteProvider {
  public static final ConcreteProvider INSTANCE = new ConcreteReferableProvider();

  private ConcreteReferableProvider() {}

  @Override
  public Concrete.ReferableDefinition getConcrete(GlobalReferable referable) {
    return referable instanceof ConcreteLocatedReferable ? ((ConcreteLocatedReferable) referable).getDefinition() : null;
  }

  @Nullable
  @Override
  public Concrete.FunctionDefinition getConcreteFunction(GlobalReferable referable) {
    if (referable instanceof ConcreteLocatedReferable) {
      Concrete.ReferableDefinition def = ((ConcreteLocatedReferable) referable).getDefinition();
      if (def instanceof Concrete.FunctionDefinition) {
        return (Concrete.FunctionDefinition) def;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Concrete.Instance getConcreteInstance(GlobalReferable referable) {
    if (referable instanceof ConcreteLocatedReferable) {
      Concrete.ReferableDefinition def = ((ConcreteLocatedReferable) referable).getDefinition();
      if (def instanceof Concrete.Instance) {
        return (Concrete.Instance) def;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Concrete.ClassDefinition getConcreteClass(ClassReferable referable) {
    if (referable instanceof ConcreteLocatedReferable) {
      Concrete.ReferableDefinition def = ((ConcreteLocatedReferable) referable).getDefinition();
      if (def instanceof Concrete.ClassDefinition) {
        return (Concrete.ClassDefinition) def;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Concrete.DataDefinition getConcreteData(GlobalReferable referable) {
    if (referable instanceof ConcreteLocatedReferable) {
      Concrete.ReferableDefinition def = ((ConcreteLocatedReferable) referable).getDefinition();
      if (def instanceof Concrete.DataDefinition) {
        return (Concrete.DataDefinition) def;
      }
    }
    return null;
  }
}
