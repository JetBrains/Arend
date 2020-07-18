package org.arend.extImpl;

import org.arend.error.DummyErrorReporter;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.ExpressionResolver;
import org.arend.naming.reference.LongUnresolvedReference;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Function;

public class FakeExpressionResolver implements ExpressionResolver {
  public static final ExpressionResolver INSTANCE = new FakeExpressionResolver();

  private FakeExpressionResolver() {}

  @Override
  public @NotNull ConcreteExpression resolve(@NotNull ConcreteExpression expression) {
    return expression;
  }

  @Override
  public <T> T hidingRefs(@NotNull Set<? extends ArendRef> refs, @NotNull Function<ExpressionResolver, T> action) {
    return action.apply(this);
  }

  @Override
  public boolean isLongUnresolvedReference(@NotNull ArendRef ref) {
    return ref instanceof LongUnresolvedReference;
  }

  @Override
  public @NotNull ErrorReporter getErrorReporter() {
    return DummyErrorReporter.INSTANCE;
  }
}
