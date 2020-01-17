package org.arend.ext.typechecking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseMetaDefinition implements MetaDefinition {
  protected boolean withoutLevels() {
    return true;
  }

  protected boolean[] argumentExplicitness() {
    return null;
  }

  @Nullable
  @Override
  public CheckedExpression invoke(@Nonnull TypecheckingSession session, @Nonnull ContextData contextData) {
    if (withoutLevels() && (contextData.getPLevel() != null || contextData.getHLevel() != null)) {
      // TODO[lang_ext]: report warning
    }
    boolean[] explicitness = argumentExplicitness();
    if (explicitness != null) {
      // TODO[lang_ext]: check
    }
    return null;
  }
}
