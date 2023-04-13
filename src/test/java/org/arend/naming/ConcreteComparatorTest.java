package org.arend.naming;

import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteCompareVisitor;
import org.arend.term.concrete.ReplaceDataVisitor;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConcreteComparatorTest extends NameResolverTestCase {
  @Test
  public void testLamPatterns() {
    resolveNamesModule(
      "\\func foo => \\lam n (path f) m => f\n" +
      "\\func bar => \\lam n (path f) m => f");
    assertTrue(((Concrete.Definition) getConcrete("foo")).accept(new ConcreteCompareVisitor(), (Concrete.Definition) getConcrete("bar")));
  }

  @Test
  public void testLamPatternsCopy() {
    Concrete.Definition def = (Concrete.Definition) getDefinition(resolveNamesDef("\\func foo => \\lam (path f) n (path g) => f"));
    assertTrue(def.accept(new ConcreteCompareVisitor(), (Concrete.Definition) def.accept(new ReplaceDataVisitor(), null)));
  }

  @Test
  public void testSubstitution() {
    Concrete.Definition def = (Concrete.Definition) getDefinition(resolveNamesDef("\\func foo => \\lam n (path f) m (path g) => f"));
    ConcreteCompareVisitor visitor = new ConcreteCompareVisitor();
    def.accept(visitor, (Concrete.Definition) def.accept(new ReplaceDataVisitor(), null));
    assertTrue(visitor.getSubstitution().isEmpty());
  }

  @Test
  public void testLetPatterns() {
    Concrete.Definition def = (Concrete.Definition) getDefinition(resolveNamesDef("\\func foo => \\let (x,y) => (0,1) \\in x"));
    ConcreteCompareVisitor visitor = new ConcreteCompareVisitor();
    def.accept(visitor, (Concrete.Definition) def.accept(new ReplaceDataVisitor(), null));
    assertTrue(visitor.getSubstitution().isEmpty());
  }
}
