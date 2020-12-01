package org.arend.naming;

import org.arend.Matchers;
import org.arend.term.concrete.Concrete;
import org.arend.term.expr.ConcreteCompareVisitor;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AliasTest extends TypeCheckingTestCase {
  @Test
  public void aliasTest() {
    resolveNamesModule(
      "\\func foo \\alias bar => 0\n" +
      "\\func baz => foo Nat.+ bar");
  }

  @Test
  public void unicodeAliasTest() {
    resolveNamesModule(
      "\\func foo \\alias ∀ => 0\n" +
      "\\func baz => foo Nat.+ ∀");
  }

  @Test
  public void aliasPrecedenceTest() {
    resolveNamesModule(
      "\\func foo \\alias \\infix 5 bar (x y : Nat) => x\n" +
      "\\func test1 => foo 0 1\n" +
      "\\func test2 => 0 bar 1");
    Concrete.FunctionDefinition test1 = (Concrete.FunctionDefinition) getConcrete("test1");
    Concrete.FunctionDefinition test2 = (Concrete.FunctionDefinition) getConcrete("test2");
    assertTrue(new ConcreteCompareVisitor().compare(((Concrete.TermFunctionBody) test1.getBody()).getTerm(), ((Concrete.TermFunctionBody) test2.getBody()).getTerm()));
  }

  @Test
  public void aliasPrecedenceError() {
    resolveNamesModule(
      "\\func foo \\alias \\infix 5 bar (x y : Nat) => x\n" +
      "\\func test1 => 0 foo 1\n" +
      "\\func test2 => 0 bar 1");
    Concrete.FunctionDefinition test1 = (Concrete.FunctionDefinition) getConcrete("test1");
    Concrete.FunctionDefinition test2 = (Concrete.FunctionDefinition) getConcrete("test2");
    assertFalse(new ConcreteCompareVisitor().compare(((Concrete.TermFunctionBody) test1.getBody()).getTerm(), ((Concrete.TermFunctionBody) test2.getBody()).getTerm()));
  }

  @Test
  public void aliasPrecedenceTest2() {
    resolveNamesModule(
      "\\func \\infix 5 foo \\alias bar (x y : Nat) => x\n" +
      "\\func test1 => 0 foo 1\n" +
      "\\func test2 => bar 0 1");
    Concrete.FunctionDefinition test1 = (Concrete.FunctionDefinition) getConcrete("test1");
    Concrete.FunctionDefinition test2 = (Concrete.FunctionDefinition) getConcrete("test2");
    assertTrue(new ConcreteCompareVisitor().compare(((Concrete.TermFunctionBody) test1.getBody()).getTerm(), ((Concrete.TermFunctionBody) test2.getBody()).getTerm()));
  }

  @Test
  public void aliasPrecedenceError2() {
    resolveNamesModule(
      "\\func \\infix 5 foo \\alias bar (x y : Nat) => x\n" +
      "\\func test1 => 0 foo 1\n" +
      "\\func test2 => 0 bar 1");
    Concrete.FunctionDefinition test1 = (Concrete.FunctionDefinition) getConcrete("test1");
    Concrete.FunctionDefinition test2 = (Concrete.FunctionDefinition) getConcrete("test2");
    assertFalse(new ConcreteCompareVisitor().compare(((Concrete.TermFunctionBody) test1.getBody()).getTerm(), ((Concrete.TermFunctionBody) test2.getBody()).getTerm()));
  }

  @Test
  public void aliasPrecedenceError3() {
    resolveNamesModule(
      "\\func foo \\alias \\infix 5 bar (x y : Nat) => x\n" +
      "\\func test => (0, 0 bar 1 bar 2)", 1);
  }

  @Test
  public void aliasClassTest() {
    resolveNamesModule(
      "\\class Foo \\alias Bar\n" +
      "\\instance foo : Foo\n" +
      "\\instance bar : Bar");
  }

  @Test
  public void openTest() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M(foo)\n" +
      "\\func test => bar");
  }

  @Test
  public void openTest2() {
    resolveNamesModule(
     "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M(foo)\n" +
      "\\func test => foo");
  }

  @Test
  public void openTest3() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M(bar)\n" +
      "\\func test => bar");
  }

  @Test
  public void openTest4() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M(bar)\n" +
      "\\func test => foo");
  }

  @Test
  public void hidingTest() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\hiding (foo)\n" +
      "\\func test => bar", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void hidingTest2() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\hiding (foo)\n" +
      "\\func test => foo", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void hidingTest3() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\hiding (bar)\n" +
      "\\func test => bar", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void hidingTest4() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\hiding (bar)\n" +
      "\\func test => foo", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void renamingTest() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M (foo \\as foo')\n" +
      "\\func test => bar", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void renamingTest2() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M (foo \\as foo')\n" +
      "\\func test => foo", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void renamingTest3() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M (foo \\as foo')\n" +
      "\\func test => foo'");
  }

  @Test
  public void renamingTest4() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M (bar \\as bar')\n" +
      "\\func test => bar", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void renamingTest5() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M (bar \\as bar')\n" +
      "\\func test => foo", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void renamingTest6() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M (bar \\as bar')\n" +
      "\\func test => bar'");
  }

  @Test
  public void usingTest() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\using (foo \\as foo')\n" +
      "\\func test => bar", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void usingTest2() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\using (foo \\as foo')\n" +
      "\\func test => foo", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void usingTest3() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\using (foo \\as foo')\n" +
      "\\func test => foo'");
  }

  @Test
  public void usingTest4() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\using (bar \\as bar')\n" +
      "\\func test => bar", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void usingTest5() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\using (bar \\as bar')\n" +
      "\\func test => foo", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void usingTest6() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "}\n" +
      "\\open M \\using (bar \\as bar')\n" +
      "\\func test => bar'");
  }

  @Test
  public void openNamespaceTest() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M(foo)\n" +
      "\\func test => bar.baz");
  }

  @Test
  public void openNamespaceTest2() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M(foo)\n" +
      "\\func test => foo.baz");
  }

  @Test
  public void openNamespaceTest3() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M(bar)\n" +
      "\\func test => bar.baz");
  }

  @Test
  public void openNamespaceTest4() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M(bar)\n" +
      "\\func test => foo.baz");
  }

  @Test
  public void hidingNamespaceTest() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\hiding (foo)\n" +
      "\\func test => bar.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void hidingNamespaceTest2() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\hiding (foo)\n" +
      "\\func test => foo.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void hidingNamespaceTest3() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\hiding (bar)\n" +
      "\\func test => bar.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void hidingNamespaceTest4() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\hiding (bar)\n" +
      "\\func test => foo.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void renamingNamespaceTest() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M (foo \\as foo')\n" +
      "\\func test => bar.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void renamingNamespaceTest2() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M (foo \\as foo')\n" +
      "\\func test => foo.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void renamingNamespaceTest3() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M (foo \\as foo')\n" +
      "\\func test => foo'.baz");
  }

  @Test
  public void renamingNamespaceTest4() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M (bar \\as bar')\n" +
      "\\func test => bar.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void renamingNamespaceTest5() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M (bar \\as bar')\n" +
      "\\func test => foo.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void renamingNamespaceTest6() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M (bar \\as bar')\n" +
      "\\func test => bar'.baz");
  }

  @Test
  public void usingNamespaceTest() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\using (foo \\as foo')\n" +
      "\\func test => bar.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void usingNamespaceTest2() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\using (foo \\as foo')\n" +
      "\\func test => foo.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void usingNamespaceTest3() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\using (foo \\as foo')\n" +
      "\\func test => foo'.baz");
  }

  @Test
  public void usingNamespaceTest4() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\using (bar \\as bar')\n" +
      "\\func test => bar.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("bar"));
  }

  @Test
  public void usingNamespaceTest5() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\using (bar \\as bar')\n" +
      "\\func test => foo.baz", 1);
    assertThatErrorsAre(Matchers.notInScope("foo"));
  }

  @Test
  public void usingNamespaceTest6() {
    resolveNamesModule(
      "\\module M \\where {\n" +
      "  \\func foo \\alias bar => 0\n" +
      "    \\where \\func baz => 0" +
      "}\n" +
      "\\open M \\using (bar \\as bar')\n" +
      "\\func test => bar'.baz");
  }

  @Test
  public void extendsTest() {
    typeCheckModule(
      "\\class A \\alias X\n" +
      "\\class B \\extends X");
  }
}