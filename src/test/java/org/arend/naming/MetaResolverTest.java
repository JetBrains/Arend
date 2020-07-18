package org.arend.naming;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.reference.ExpressionResolver;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.MetaResolver;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MetaResolverTest extends NameResolverTestCase {
  private static class BaseMetaDefinition implements MetaDefinition, MetaResolver {
    int numberOfInvocations = 0;

    @Override
    public @Nullable ConcreteExpression resolvePrefix(@NotNull ExpressionResolver resolver, @NotNull ConcreteReferenceExpression refExpr, @NotNull List<? extends ConcreteArgument> arguments) {
      fail();
      return null;
    }

    @Override
    public @Nullable ConcreteExpression resolveInfix(@NotNull ExpressionResolver resolver, @NotNull ConcreteReferenceExpression refExpr, @Nullable ConcreteExpression leftArg, @Nullable ConcreteExpression rightArg) {
      fail();
      return null;
    }

    @Override
    public @Nullable ConcreteExpression resolvePostfix(@NotNull ExpressionResolver resolver, @NotNull ConcreteReferenceExpression refExpr, @Nullable ConcreteExpression leftArg, @NotNull List<? extends ConcreteArgument> rightArgs) {
      fail();
      return null;
    }
  }

  private static class PrefixMetaDefinition extends BaseMetaDefinition {
    private final int numberOfArguments;

    private PrefixMetaDefinition(int numberOfArguments) {
      this.numberOfArguments = numberOfArguments;
    }

    @Override
    public @Nullable ConcreteExpression resolvePrefix(@NotNull ExpressionResolver resolver, @NotNull ConcreteReferenceExpression refExpr, @NotNull List<? extends ConcreteArgument> arguments) {
      numberOfInvocations++;
      assertEquals(numberOfArguments, arguments.size());
      return new Concrete.ReferenceExpression(null, Prelude.ZERO.getReferable());
    }
  }

  @Test
  public void prefixTest() {
    BaseMetaDefinition meta = new PrefixMetaDefinition(2);
    addMeta("meta", Precedence.DEFAULT, meta);
    resolveNamesDef("\\func foo => meta 0 1");
    assertEquals(1, meta.numberOfInvocations);
  }

  @Test
  public void prefixTest2() {
    BaseMetaDefinition meta = new PrefixMetaDefinition(2);
    addMeta("meta", Precedence.DEFAULT, meta);
    resolveNamesDef("\\func foo => meta 0 1 Nat.+ 0");
    assertEquals(1, meta.numberOfInvocations);
  }

  @Test
  public void prefixTest3() {
    BaseMetaDefinition meta = new PrefixMetaDefinition(2);
    addMeta("meta", Precedence.DEFAULT, meta);
    resolveNamesDef("\\func foo => 1 Nat.+ meta 0 1 Nat.+ 2");
    assertEquals(1, meta.numberOfInvocations);
  }

  @Test
  public void prefixTest4() {
    BaseMetaDefinition meta = new PrefixMetaDefinition(2);
    addMeta("meta", Precedence.DEFAULT, meta);
    resolveNamesDef("\\func foo => meta (meta 0 1) 1");
    assertEquals(1, meta.numberOfInvocations);
  }

  @Test
  public void prefixTest5() {
    BaseMetaDefinition meta = new PrefixMetaDefinition(2);
    addMeta("meta", Precedence.DEFAULT, meta);
    resolveNamesDef("\\func foo => meta 0 1 Nat.+ meta 2 3");
    assertEquals(2, meta.numberOfInvocations);
  }
}
