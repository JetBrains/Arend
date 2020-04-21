package org.arend.term.prettyprint;

import org.arend.core.expr.visitor.ConflictDefinitionRenamer;
import org.arend.core.expr.visitor.ScopeDefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterConfigImpl;
import org.arend.naming.scope.EmptyScope;
import org.arend.naming.scope.Scope;

public class PrettyPrinterConfigWithRenamer extends PrettyPrinterConfigImpl {
  public PrettyPrinterConfigWithRenamer(PrettyPrinterConfig config, Scope scope) {
    super(config);
    definitionRenamer = scope == null || scope == EmptyScope.INSTANCE ? new ConflictDefinitionRenamer() : new ScopeDefinitionRenamer(scope);
  }

  public PrettyPrinterConfigWithRenamer(Scope scope) {
    definitionRenamer = scope == null || scope == EmptyScope.INSTANCE ? new ConflictDefinitionRenamer() : new ScopeDefinitionRenamer(scope);
  }
}
