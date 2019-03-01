package org.arend.frontend;

import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

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
  public Concrete.FunctionDefinition getConcreteInstance(GlobalReferable referable) {
    if (referable instanceof ConcreteLocatedReferable) {
      Concrete.ReferableDefinition def = ((ConcreteLocatedReferable) referable).getDefinition();
      if (def instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) def).getKind() == Concrete.FunctionDefinition.Kind.INSTANCE) {
        return (Concrete.FunctionDefinition) def;
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
