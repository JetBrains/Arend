package org.arend.typechecking.provider;

import org.arend.naming.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;

public class ConcreteReferableProvider implements ConcreteProvider {
  public static final ConcreteProvider INSTANCE = new ConcreteReferableProvider();

  private ConcreteReferableProvider() {}

  @Override
  public Concrete.GeneralDefinition getConcrete(GlobalReferable referable) {
    Concrete.ResolvableDefinition def = referable.getDefaultConcrete();
    return def != null ? def : referable instanceof ConcreteLocatedReferable ? ((ConcreteLocatedReferable) referable).getDefinition() : null;
  }
}
