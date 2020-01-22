package org.arend.ext.typechecking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseMetaDefinition implements MetaDefinition {
  protected boolean withoutLevels() {
    return true;
  }

  @Nullable
  protected boolean[] argumentExplicitness() {
    return null;
  }

  protected boolean requiredExpectedType() {
    return false;
  }

  protected boolean checkContext(@Nonnull ContextData contextData) {
    if (withoutLevels() && (contextData.getReferenceExpression().getPLevel() != null || contextData.getReferenceExpression().getHLevel() != null)) {
      // TODO[lang_ext]: report warning
    }

    boolean ok = true;
    boolean[] explicitness = argumentExplicitness();
    if (explicitness != null) {
      // TODO[lang_ext]: check
    }
    if (contextData.getExpectedType() == null && requiredExpectedType()) {
      // TODO[lang_ext]: report error
      ok = false;
    }
    return ok;
  }
}
