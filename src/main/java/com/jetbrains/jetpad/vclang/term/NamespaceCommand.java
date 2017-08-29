package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

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
}
