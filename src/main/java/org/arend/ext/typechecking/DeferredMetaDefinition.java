package org.arend.ext.typechecking;

import org.arend.ext.concrete.ConcreteArgument;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.reference.ArendRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeferredMetaDefinition extends BaseMetaDefinition {
  protected Map<ArendRef, CoreBinding> context;
  protected CoreExpression expectedType;
  protected List<? extends ConcreteArgument> arguments;

  @Nullable
  @Override
  public CheckedExpression invoke(@Nonnull TypecheckingSession session, @Nonnull ContextData contextData) {
    context = new LinkedHashMap<>(contextData.getBindings());
    expectedType = contextData.getExpectedType();
    arguments = contextData.getArguments();
    return super.invoke(session, contextData);
  }

  @Override
  public CoreExpression getExpectedType() {
    return expectedType;
  }

  @Nullable
  @Override
  public CheckedExpression invokeLater(@Nonnull TypecheckingSession session) {
    session.setBindings(context);
    return null;
  }
}
