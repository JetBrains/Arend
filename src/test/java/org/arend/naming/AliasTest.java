package org.arend.naming;

import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteCompareVisitor;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.Matchers.notInScope;
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
      """
        \\func foo \\alias \\infix 5 bar (x y : Nat) => x
        \\func test1 => foo 0 1
        \\func test2 => 0 bar 1
        """);
    Concrete.FunctionDefinition test1 = (Concrete.FunctionDefinition) getConcrete("test1");
    Concrete.FunctionDefinition test2 = (Concrete.FunctionDefinition) getConcrete("test2");
    assertTrue(new ConcreteCompareVisitor().compare(((Concrete.TermFunctionBody) test1.getBody()).getTerm(), ((Concrete.TermFunctionBody) test2.getBody()).getTerm()));
  }

  @Test
  public void aliasPrecedenceError() {
    resolveNamesModule(
      """
        \\func foo \\alias \\infix 5 bar (x y : Nat) => x
        \\func test1 => 0 foo 1
        \\func test2 => 0 bar 1
        """);
    Concrete.FunctionDefinition test1 = (Concrete.FunctionDefinition) getConcrete("test1");
    Concrete.FunctionDefinition test2 = (Concrete.FunctionDefinition) getConcrete("test2");
    assertFalse(new ConcreteCompareVisitor().compare(((Concrete.TermFunctionBody) test1.getBody()).getTerm(), ((Concrete.TermFunctionBody) test2.getBody()).getTerm()));
  }

  @Test
  public void aliasPrecedenceTest2() {
    resolveNamesModule(
      """
        \\func \\infix 5 foo \\alias bar (x y : Nat) => x
        \\func test1 => 0 foo 1
        \\func test2 => bar 0 1
        """);
    Concrete.FunctionDefinition test1 = (Concrete.FunctionDefinition) getConcrete("test1");
    Concrete.FunctionDefinition test2 = (Concrete.FunctionDefinition) getConcrete("test2");
    assertTrue(new ConcreteCompareVisitor().compare(((Concrete.TermFunctionBody) test1.getBody()).getTerm(), ((Concrete.TermFunctionBody) test2.getBody()).getTerm()));
  }

  @Test
  public void aliasPrecedenceError2() {
    resolveNamesModule(
      """
        \\func \\infix 5 foo \\alias bar (x y : Nat) => x
        \\func test1 => 0 foo 1
        \\func test2 => 0 bar 1
        """);
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
      """
        \\class Foo \\alias Bar
        \\instance foo : Foo
        \\instance bar : Bar
        """);
  }

  @Test
  public void openTest() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M(foo)
        \\func test => bar
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void openTest2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M(foo)
        \\func test => foo
        """);
  }

  @Test
  public void openTest3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M(bar)
        \\func test => bar
        """);
  }

  @Test
  public void openTest4() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M(bar)
        \\func test => foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void hidingTest() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\hiding (foo)
        \\func test => bar
        """);
  }

  @Test
  public void hidingTest2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\hiding (foo)
        \\func test => foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void hidingTest3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\hiding (bar)
        \\func test => bar
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void hidingTest4() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\hiding (bar)
        \\func test => foo
        """);
  }

  @Test
  public void renamingTest() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M (foo \\as foo')
        \\func test => bar
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void renamingTest2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M (foo \\as foo')
        \\func test => foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void renamingTest3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M (foo \\as foo')
        \\func test => foo'
        """);
  }

  @Test
  public void renamingTest4() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M (bar \\as bar')
        \\func test => bar
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void renamingTest5() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M (bar \\as bar')
        \\func test => foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void renamingTest6() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M (bar \\as bar')
        \\func test => bar'
        """);
  }

  @Test
  public void renamingTest7() {
    resolveNamesModule("""
      \\module M \\where {
        \\func foo \\alias bar => 0
      }
      \\open M (foo \\as foo', bar \\as bar')
      \\func test => foo
      """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void renamingTest8() {
    resolveNamesModule("""
      \\module M \\where {
        \\func foo \\alias bar => 0
      }
      \\open M (foo \\as foo', bar \\as bar')
      \\func test => foo'
      """);
  }

  @Test
  public void renamingTest9() {
    resolveNamesModule("""
      \\module M \\where {
        \\func foo \\alias bar => 0
      }
      \\open M (foo \\as foo', bar \\as bar')
      \\func test => bar
      """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void renamingTest10() {
    resolveNamesModule("""
      \\module M \\where {
        \\func foo \\alias bar => 0
      }
      \\open M (foo \\as foo', bar \\as bar')
      \\func test => bar'
      """);
  }

  @Test
  public void usingTest() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (foo \\as foo')
        \\func test => bar
        """);
  }

  @Test
  public void usingTest2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (foo \\as foo')
        \\func test => foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void usingTest3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (foo \\as foo')
        \\func test => foo'
        """);
  }

  @Test
  public void usingTest4() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (bar \\as bar')
        \\func test => bar
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void usingTest5() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (bar \\as bar')
        \\func test => foo
        """);
  }

  @Test
  public void usingTest6() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (bar \\as bar')
        \\func test => bar'
        """);
  }

  @Test
  public void usingTest7() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (foo \\as foo', bar \\as bar')
        \\func test => foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void usingTest8() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (foo \\as foo', bar \\as bar')
        \\func test => foo'
        """);
  }

  @Test
  public void usingTest9() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (foo \\as foo', bar \\as bar')
        \\func test => bar
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void usingTest10() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
        }
        \\open M \\using (foo \\as foo', bar \\as bar')
        \\func test => bar'
        """);
  }

  @Test
  public void openNamespaceTest() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M(foo)
        \\func test => foo.baz
        """);
  }

  @Test
  public void openNamespaceTest2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M(bar)
        \\func test => bar.baz
        """);
  }

  @Test
  public void hidingNamespaceTest() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\hiding (foo)
        \\func test => bar.baz
        """);
  }

  @Test
  public void hidingNamespaceTest2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\hiding (foo)
        \\func test => foo.baz
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void hidingNamespaceTest3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\hiding (bar)
        \\func test => bar.baz
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void hidingNamespaceTest4() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\hiding (bar)
        \\func test => foo.baz
        """);
  }

  @Test
  public void renamingNamespaceTest() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M (foo \\as foo')
        \\func test => bar.baz
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void renamingNamespaceTest2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M (foo \\as foo')
        \\func test => foo.baz
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void renamingNamespaceTest3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M (foo \\as foo')
        \\func test => foo'.baz
        """);
  }

  @Test
  public void renamingNamespaceTest4() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M (bar \\as bar')
        \\func test => bar.baz
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void renamingNamespaceTest5() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M (bar \\as bar')
        \\func test => foo.baz
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void renamingNamespaceTest6() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M (bar \\as bar')
        \\func test => bar'.baz
        """);
  }

  @Test
  public void usingNamespaceTest() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\using (foo \\as foo')
        \\func test => bar.baz
        """);
  }

  @Test
  public void usingNamespaceTest2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\using (foo \\as foo')
        \\func test => foo.baz
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void usingNamespaceTest3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\using (foo \\as foo')
        \\func test => foo'.baz
        """);
  }

  @Test
  public void usingNamespaceTest4() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\using (bar \\as bar')
        \\func test => bar.baz
        """, 1);
    assertThatErrorsAre(notInScope("bar"));
  }

  @Test
  public void usingNamespaceTest5() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\using (bar \\as bar')
        \\func test => foo.baz
        """);
  }

  @Test
  public void usingNamespaceTest6() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\func foo \\alias bar => 0
            \\where \\func baz => 0}
        \\open M \\using (bar \\as bar')
        \\func test => bar'.baz
        """);
  }

  @Test
  public void extendsTest() {
    typeCheckModule(
      "\\class A \\alias X\n" +
      "\\class B \\extends X");
  }
}