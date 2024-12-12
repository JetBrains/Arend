package org.arend.naming;

import org.arend.Matchers;
import org.arend.term.group.AccessModifier;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.Matchers.notInClass;
import static org.arend.Matchers.notInScope;
import static org.junit.Assert.assertEquals;

public class AccessModifiersTest extends TypeCheckingTestCase {
  @Test
  public void testPrivate() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\private \\func foo => 0
          \\func bar => 1
        }
        \\func test => M.foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void testPrivate2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\private \\func foo => 0
          \\func bar => foo
        }
        \\func test => M.bar
        """);
  }

  @Test
  public void testPrivateGroup() {
    resolveNamesModule(
      """
        \\module M \\where \\private {
          \\func foo => 0
          \\func bar => 1
        }
        \\func test1 => M.foo
        \\func test2 => M.bar
        """, 2);
    assertThatErrorsAre(notInScope("foo"), notInScope("bar"));
  }

  @Test
  public void testPrivateWhere() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\private \\func foo => 0
            \\where \\func bar => 1
        }
        \\func test => M.foo.bar
        """);
  }

  @Test
  public void testPrivateOpen() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\private \\func foo => 0
          \\func bar => 1
        }
        \\open M
        \\func test => foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void testPrivateOpen2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\private \\func foo => 0
          \\func bar => 1
        }
        \\open M
        \\func test => bar
        """);
  }

  @Test
  public void testPrivateOpen3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\private \\func foo => 0
          \\func bar => 1
        }
        \\open M(foo)
        \\func test => foo
        """, 2);
    assertThatErrorsAre(notInScope("foo"), notInScope("foo"));
  }

  @Test
  public void testPrivateGroupOpen() {
    resolveNamesModule(
      """
        \\module M \\where \\private {
          \\func foo => 0
          \\func bar => 1
        }
        \\open M
        \\func test1 => foo
        \\func test2 => bar
        """, 2);
    assertThatErrorsAre(notInScope("foo"), notInScope("bar"));
  }

  @Test
  public void testPrivateGroupOpen2() {
    resolveNamesModule(
      """
        \\module M \\where \\private {
          \\func foo => 0
          \\func bar => 1
        }
        \\open M(foo,bar)
        """, 2);
    assertThatErrorsAre(notInScope("foo"), notInScope("bar"));
  }

  @Test
  public void testProtected() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\protected \\func foo => 0
          \\func bar => foo
        }
        \\func test => M.foo
        """);
  }

  @Test
  public void testProtectedOpen() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\protected \\func foo => 0
          \\func bar => 1
        }
        \\open M
        \\func test => foo
        """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void testProtectedOpen2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\protected \\func foo => 0
          \\func bar => 1
        }
        \\open M
        \\func test => bar
        """);
  }

  @Test
  public void testProtectedOpen3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\protected \\func foo => 0
          \\func bar => 1
        }
        \\open M(foo)
        \\func test => foo
        """);
  }

  @Test
  public void testProtectedGroupOpen() {
    resolveNamesModule(
      """
        \\module M \\where \\protected {
          \\func foo => 0
          \\func bar => 1
        }
        \\open M
        \\func test1 => foo
        \\func test2 => bar
        """, 2);
    assertThatErrorsAre(notInScope("foo"), notInScope("bar"));
  }

  @Test
  public void testProtectedGroupOpen2() {
    resolveNamesModule(
      """
        \\module M \\where \\protected {
          \\func foo => 0
          \\func bar => 1
        }
        \\open M(foo,bar)
        \\func test1 => foo
        \\func test2 => bar
        """);
  }

  @Test
  public void testPrivateConstructor() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\private con1 | con2
          \\func test1 => con1
        }
        \\func test2 => M.con2
        """);
  }

  @Test
  public void testPrivateConstructor2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\private con1 | con2
        }
        \\func test => M.con1
        """, 1);
    assertThatErrorsAre(notInScope("con1"));
  }

  @Test
  public void testPrivateConstructorOpen() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\private con1 | con2
        }
        \\open M
        \\func test => con1
        """, 1);
    assertThatErrorsAre(notInScope("con1"));
  }

  @Test
  public void testPrivateConstructorOpen2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\private con1 | con2
        }
        \\open M
        \\func test => con2
        """);
  }

  @Test
  public void testPrivateConstructorOpen3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\private con1 | con2
        }
        \\open M(con1)
        """, 1);
    assertThatErrorsAre(notInScope("con1"));
  }

  @Test
  public void testProtectedConstructor() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\protected con1 | con2
          \\func test1 => con1
        }
        \\func test2 => M.con1
        """);
  }

  @Test
  public void testProtectedConstructorOpen() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\protected con1 | con2
        }
        \\open M
        \\func test => con1
        """, 1);
    assertThatErrorsAre(notInScope("con1"));
  }

  @Test
  public void testProtectedConstructorOpen2() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\protected con1 | con2
        }
        \\open M
        \\func test => con2
        """);
  }

  @Test
  public void testProtectedConstructorOpen3() {
    resolveNamesModule(
      """
        \\module M \\where {
          \\data D | \\protected con1 | con2
        }
        \\open M(con1)
        \\func test => con1
        """);
  }

  @Test
  public void testDynamicPrivate() {
    resolveNamesModule("""
      \\record R {
        \\private \\func foo => 0
        \\func test => foo
      }
      """);
  }

  @Test
  public void testDynamicPrivate2() {
    resolveNamesModule("""
      \\record R {
        \\private \\func foo => 0
      }
      \\func test (r : R) => r.foo
      """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void testDynamicPrivate3() {
    typeCheckModule("""
      \\record R {
        \\private \\func foo => 0
      }
      \\func test (r : R) => \\let r' => r \\in r'.foo
      """, 1);
    assertThatErrorsAre(notInClass("foo", get("R")));
  }

  @Test
  public void testDynamicProtected() {
    resolveNamesModule("""
      \\record R {
        \\protected \\func foo => 0
        \\func test => foo
      }
      """);
  }

  @Test
  public void testDynamicProtected2() {
    typeCheckModule("""
      \\record R {
        \\protected \\func foo => 0
      }
      \\func test (r : R) => r.foo
      """);
  }

  @Test
  public void testDynamicProtected3() {
    typeCheckModule("""
      \\record R {
        \\protected \\func foo => 0
      }
      \\func test (r : R) => \\let r' => r \\in r'.foo
      """, 1);
    assertThatErrorsAre(notInClass("foo", get("R")));
  }

  @Test
  public void testDynamicPrivateOpen() {
    resolveNamesModule("""
      \\record R {
        \\private \\func foo => 0
      }
      \\open R
      \\func test (r : R) => foo {r}
      """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void testDynamicProtectedOpen() {
    resolveNamesModule("""
      \\record R {
        \\protected \\func foo => 0
      }
      \\open R
      \\func test (r : R) => foo {r}
      """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }

  @Test
  public void testConstructor() {
    resolveNamesModule("""
      \\private \\data D
        | cons
      """);
    assertEquals(AccessModifier.PRIVATE, get("D.cons").getAccessModifier());
  }

  @Test
  public void testConstructor2() {
    lastGroup = parseModule("""
      \\private \\data D
        | \\private cons
      """, 1);
    resolveNamesModule(lastGroup, 1);
    assertEquals(AccessModifier.PRIVATE, get("D.cons").getAccessModifier());
    assertThatErrorsAre(Matchers.warning());
  }

  @Test
  public void testField() {
    resolveNamesModule("""
      \\module M \\where {
        \\private \\record R
          | field : Nat
      }
      \\func test => M.field
      """, 1);
    assertThatErrorsAre(notInScope("field"));
  }

  @Test
  public void testField2() {
    resolveNamesModule("""
      \\module M \\where {
        \\private \\record R
          | field : Nat
      }
      \\open M
      \\func test => field
      """, 1);
    assertThatErrorsAre(notInScope("field"));
  }

  @Test
  public void testField3() {
    lastGroup = parseModule("""
      \\private \\data R
        | \\private field : Nat
      """, 1);
    resolveNamesModule(lastGroup, 1);
    assertEquals(AccessModifier.PRIVATE, get("R.field").getAccessModifier());
    assertThatErrorsAre(Matchers.warning());
  }

  @Test
  public void dynamicOpenTest() {
    resolveNamesModule("""
      \\record R {
        \\protected \\func foo => 0
      }
      \\open R(.foo)
      \\func rrr : R \\cowith
      \\func test => rrr.foo
      """);
  }

  @Test
  public void fieldDynamicTest() {
    typeCheckModule("""
      \\record R {
        \\protected \\func foo => 0
      }
      \\record S {
        | field : R
      }
      \\open R(.foo)
      \\func test (s : S) => s.field.foo
      """);
  }
}
