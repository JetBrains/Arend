package org.arend.typechecking.order.dependency;

import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.DefinableMetaDefinition;

public class MetaDependencyCollector extends DependencyCollector {
  @Override
  public void dependsOn(TCReferable def1, TCReferable def2) {
    if (def1 instanceof MetaReferable && ((MetaReferable) def1).getDefinition() instanceof DefinableMetaDefinition || def2 instanceof MetaReferable && ((MetaReferable) def2).getDefinition() instanceof DefinableMetaDefinition) {
      super.dependsOn(def1, def2);
    }
  }
}
