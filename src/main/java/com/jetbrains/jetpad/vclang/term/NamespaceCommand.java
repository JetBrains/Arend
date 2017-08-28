package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NamespaceError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.FilteredScope;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface NamespaceCommand extends PrettyPrintable {
  enum Kind { OPEN, EXPORT }
  @Nonnull Kind getKind();
  @Nonnull Referable getGroupReference();
  boolean isHiding();
  @Nullable Collection<? extends Referable> getSubgroupReferences();

  @Override
  default String prettyPrint(PrettyPrinterInfoProvider infoProvider) {
    StringBuilder builder = new StringBuilder();
    builder.append(getKind() == Kind.OPEN ? "\\open " : "\\export ").append(getGroupReference().textRepresentation());

    Collection<? extends Referable> references = getSubgroupReferences();
    if (references != null) {
      if (isHiding()) {
        builder.append(" \\hiding");
      }
      builder.append(" (");
      boolean first = true;
      for (Referable reference : references) {
        if (first) {
          first = false;
        } else {
          builder.append(", ");
        }
        builder.append(reference.textRepresentation());
      }
      builder.append(')');
    }

    return builder.toString();
  }

  default Scope openedScope(Scope parentScope, NameResolver nameResolver, ErrorReporter errorReporter) {
    Referable referable = getGroupReference();
    String refText = referable.textRepresentation();
    if (referable instanceof UnresolvedReference) {
      referable = ((UnresolvedReference) referable).resolve(parentScope, nameResolver);
    }

    if (!(referable instanceof GlobalReferable)) {
      errorReporter.report(new NamespaceError<>("'" + refText + "' is not a reference to a definition", this));
      return null;
    }

    Scope scope = new NamespaceScope(nameResolver.nsProviders.statics.forReferable((GlobalReferable) referable));
    Collection<? extends Referable> refs = getSubgroupReferences();
    if (refs != null) {
      Set<String> names = new HashSet<>();
      for (Referable ref : refs) {
        refText = ref.textRepresentation();
        if (ref instanceof UnresolvedReference) {
          if (!(((UnresolvedReference) ref).resolve(scope, nameResolver) instanceof GlobalReferable)) {
            errorReporter.report(new NamespaceError<>("'" + refText + "' is not a reference to a definition", this));
            continue;
          }
        }
        names.add(refText);
      }
      scope = new FilteredScope(scope, names, !isHiding());
    }

    return scope;
  }
}
