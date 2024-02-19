package org.arend.typechecking.constructions;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.subst.Levels;
import org.arend.naming.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Statement;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.util.SingletonList;
import org.junit.Test;

import java.util.*;

import static org.arend.ExpressionFactory.*;
import static org.arend.Matchers.typeMismatchError;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefCallTest extends TypeCheckingTestCase {
  private void test(Expression expected) {
    assertEquals(expected, ((FunctionDefinition) getDefinition("test")).getBody());
  }

  private void testFI(Expression expected) {
    assertEquals(expected, ((FunctionDefinition) getDefinition("Test.test")).getBody());
  }

  private void testType(Expression expected) {
    assertEquals(expected, ((FunctionDefinition) getDefinition("test")).getResultType());
    assertEquals(expected, ((Expression) Objects.requireNonNull(((FunctionDefinition) getDefinition("test")).getBody())).getType());
  }

  private DependentLink getThis() {
    FunctionDefinition function = (FunctionDefinition) getDefinition("test");
    return function.getParameters();
  }

  private Expression getThisFI() {
    FunctionDefinition function = (FunctionDefinition) getDefinition("Test.test");
    return FieldCall(((ClassDefinition) getDefinition("Test")).getPersonalFields().get(0), Ref(function.getParameters()));
  }

  private ClassCallExpression makeClassCall(Definition definition, Expression impl) {
    ClassDefinition classDef = (ClassDefinition) definition;
    ClassCallExpression classCall = new ClassCallExpression(classDef, Levels.EMPTY, Collections.singletonMap(classDef.getNotImplementedFields().iterator().next(), impl), classDef.getSort(), classDef.getUniverseKind());
    classCall.updateHasUniverses();
    return classCall;
  }

  @Test
  public void funStatic() {
    typeCheckModule(
        "\\func f => 0\n" +
        "\\func test => f");
    test(FunCall((FunctionDefinition) getDefinition("f"), Levels.EMPTY));
  }

  @Test
  public void funDynamic() {
    typeCheckClass(
        "\\func f => 0\n" +
        "\\func test => f", "");
    test(FunCall((FunctionDefinition) getDefinition("f"), Levels.EMPTY, Ref(getThis())));
  }

  /*
  @Test
  public void funDynamicFromInside() {
    typeCheckClass(
        "\\func f => 0\n" +
        "\\class Test {\n" +
        "  \\func test => f\n" +
        "}", "");
    testFI(FunCall((FunctionDefinition) getDefinition("f"), Levels.EMPTY, getThisFI()));
  }
  */

  @Test
  public void funDynamicError() {
    typeCheckModule("""
      \\class Test {
        \\func f => 0
      } \\where {
        \\func test : Nat => f
      }
      """, 1);
  }

  @Test
  public void funStaticInside() {
    typeCheckModule("""
      \\class A \\where {
        \\class B \\where {
          \\func f => 0
        }
      }
      \\func test => A.B.f
      """);
    test(FunCall((FunctionDefinition) getDefinition("A.B.f"), Levels.EMPTY));
  }

  @Test
  public void funDynamicInside() {
    typeCheckClass("""
      \\class A \\where {
        \\class B \\where {
          \\func f => 0
        }
      }
      \\func test => A.B.f
      """, "");
    test(FunCall((FunctionDefinition) getDefinition("A.B.f"), Levels.EMPTY, Ref(getThis())));
  }

  @Test
  public void funFieldStatic() {
    typeCheckModule("""
      \\class E {
        \\func f => 0
      }
      \\func test (e : E) => E.f {e}
      """);
    test(FunCall((FunctionDefinition) getDefinition("E.f"), Levels.EMPTY, Ref(getThis())));
  }

  @Test
  public void funFieldError() {
    typeCheckModule("""
      \\class E \\where {
        \\func f => 0
      }
      \\func test (e : E) => E.f {e}
      """, 1);
  }

  @Test
  public void funFieldDynamic() {
    typeCheckClass("""
      \\class E {
        \\func f => 0
      }
      \\func test (e : E) => E.f {e}
      """, "");
    test(FunCall((FunctionDefinition) getDefinition("E.f"), Levels.EMPTY, Ref(getThis().getNext())));
  }

  @Test
  public void funFieldInside() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B \\where {
            \\func f => 0
          }
        }
      }
      \\func test (e : E) => E.A.B.f {e}
      """);
    test(FunCall((FunctionDefinition) getDefinition("E.A.B.f"), Levels.EMPTY, Ref(getThis())));
  }

  @Test
  public void funFieldInside2() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B {e : E} {
            \\func f => 0
          }
        }
      }
      \\func test (e : E) (b : E.A.B {e}) => E.A.B.f {b}
      """);
    test(FunCall((FunctionDefinition) getDefinition("E.A.B.f"), Levels.EMPTY, Ref(getThis().getNext())));
  }

  @Test
  public void funFieldInsideError() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B {
            \\func f => 0
          }
        }
      }
      \\func test (e : E) => E.A.B.f {e}
      """, 1);
  }

  @Test
  public void funFieldInsideError2() {
    typeCheckModule("""
      \\class E {
        \\class A {
          \\class B \\where {
            \\func f => 0
          }
        }
      }
      \\func test (e : E) => E.A.B.f {e}
      """, 1);
  }

  @Test
  public void conStatic() {
    typeCheckModule(
        "\\data D | c\n" +
        "\\func test => c");
    test(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, Collections.emptyList()));
    assertEquals(getDefinition("c"), getDefinition("D.c"));
  }

  @Test
  public void dataStatic() {
    typeCheckModule(
        "\\data D | c\n" +
        "\\func test => D.c");
    test(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, Collections.emptyList()));
    assertEquals(getDefinition("c"), getDefinition("D.c"));
  }

  @Test
  public void data2Static() {
    typeCheckModule(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, dataTypeArgs));
    testType(DataCall((DataDefinition) getDefinition("D"), Levels.EMPTY, dataTypeArgs));
    assertEquals(getDefinition("c"), getDefinition("D.c"));
  }

  @Test
  public void conDynamic() {
    typeCheckClass(
        "\\data D | c\n" +
        "\\func test => c", "");
    test(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, new SingletonList<>(Ref(getThis()))));
    assertEquals(getDefinition("c"), getDefinition("D.c"));
  }

  /*
  @Test
  public void conDynamicFromInside() {
    typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\func test => c\n" +
        "}", "");
    testFI(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, new SingletonList<>(getThisFI())));
  }
  */

  @Test
  public void dataDynamic() {
    typeCheckClass(
        "\\data D | c\n" +
        "\\func test => D.c", "");
    test(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, new SingletonList<>(Ref(getThis()))));
    assertEquals(getDefinition("c"), getDefinition("D.c"));
  }

  /*
  @Test
  public void dataDynamicFromInside() {
    typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\func test => D.c\n" +
        "}", "");
    testFI(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, new SingletonList<>(getThisFI())));
  }
  */

  @Test
  public void data2Dynamic() {
    typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => D.c {_} {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, dataTypeArgs));
    testType(DataCall((DataDefinition) getDefinition("D"), Levels.EMPTY, dataTypeArgs));
    assertEquals(getDefinition("c"), getDefinition("D.c"));
  }

  @Test
  public void data3Dynamic() {
    typeCheckClass(
      "\\func test => D.c {_} {path (\\lam _ => 0)}",
      "\\data D {x : Nat} (p : x = 0) | c");
  }

  /*
  @Test
  public void data2DynamicFromInside() {
    typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\func test => D.c {0} {\\lam _ => 1}\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) getDefinition("c"), Levels.EMPTY, dataTypeArgs));
  }
  */

  @Test
  public void conDynamicError() {
    typeCheckModule("""
      \\class Test {
        \\data D | c
      } \\where {
        \\func test : D => c
      }
      """, 2);
  }

  @Test
  public void dataDynamicError() {
    typeCheckModule("""
      \\class Test {
        \\data D | c
      } \\where {
        \\func test : D => D.c
      }
      """, 2);
  }

  @Test
  public void conStaticInside() {
    typeCheckModule("""
      \\class A \\where {
        \\class B \\where {
          \\data D | c
        }
      }
      \\func test => A.B.c
      """);
    test(ConCall((Constructor) getDefinition("A.B.c"), Levels.EMPTY, Collections.emptyList()));
    assertEquals(getDefinition("A.B.c"), getDefinition("A.B.D.c"));
  }

  @Test
  public void dataStaticInside() {
    typeCheckModule("""
      \\class A \\where {
        \\class B \\where {
          \\data D | c
        }
      }
      \\func test => A.B.D.c
      """);
    test(ConCall((Constructor) getDefinition("A.B.c"), Levels.EMPTY, Collections.emptyList()));
    assertEquals(getDefinition("A.B.c"), getDefinition("A.B.D.c"));
  }

  @Test
  public void data2StaticInside() {
    typeCheckModule("""
      \\class A \\where {
        \\class B \\where {
          \\data D (x : Nat) (y : Nat -> Nat) | c
        }
      }
      \\func test => A.B.D.c {0} {\\lam _ => 1}
      """);
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition("A.B.c"), Levels.EMPTY, dataTypeArgs));
    testType(DataCall((DataDefinition) getDefinition("A.B.D"), Levels.EMPTY, dataTypeArgs));
    assertEquals(getDefinition("A.B.c"), getDefinition("A.B.D.c"));
  }

  @Test
  public void conDynamicInside() {
    typeCheckClass("""
      \\class A \\where {
        \\class B \\where {
          \\data D | c
        }
      }
      \\func test => A.B.c
      """, "");
    test(ConCall((Constructor) getDefinition("A.B.c"), Levels.EMPTY, new SingletonList<>(Ref(getThis()))));
    assertEquals(getDefinition("A.B.c"), getDefinition("A.B.D.c"));
  }

  @Test
  public void dataDynamicInside() {
    typeCheckClass("""
      \\class A \\where {
        \\class B \\where {
          \\data D | c
        }
      }
      \\func test => A.B.D.c
      """, "");
    test(ConCall((Constructor) getDefinition("A.B.c"), Levels.EMPTY, new SingletonList<>(Ref(getThis()))));
    assertEquals(getDefinition("A.B.c"), getDefinition("A.B.D.c"));
  }

  @Test
  public void data2DynamicInside() {
    typeCheckClass("""
      \\class A \\where {
        \\class B \\where {
          \\data D (x : Nat) (y : Nat -> Nat) | c
        }
      }
      \\func test => A.B.D.c {_} {0} {\\lam _ => 1}
      """, "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition("A.B.c"), Levels.EMPTY, dataTypeArgs));
    testType(DataCall((DataDefinition) getDefinition("A.B.D"), Levels.EMPTY, dataTypeArgs));
    assertEquals(getDefinition("A.B.c"), getDefinition("A.B.D.c"));
  }

  @Test
  public void conFieldStatic() {
    typeCheckModule("""
      \\class E {
        \\data D | c
      }
      \\func test (e : E) => E.c {e}
      """);
    test(ConCall((Constructor) getDefinition("E.c"), Levels.EMPTY, new SingletonList<>(Ref(getThis()))));
    assertEquals(getDefinition("E.c"), getDefinition("E.D.c"));
  }

  @Test
  public void dataFieldStatic() {
    typeCheckModule("""
      \\class E {
        \\data D | c
      }
      \\func test (e : E) => E.D.c {e}
      """);
    test(ConCall((Constructor) getDefinition("E.c"), Levels.EMPTY, new SingletonList<>(Ref(getThis()))));
    assertEquals(getDefinition("E.c"), getDefinition("E.D.c"));
  }

  @Test
  public void data2FieldStatic() {
    typeCheckModule("""
      \\class E {
        \\data D (x : Nat) (y : Nat -> Nat) | c
      }
      \\func test (e : E) => E.D.c {e} {0} {\\lam _ => 1}
      """);
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition("E.c"), Levels.EMPTY, dataTypeArgs));
    testType(DataCall((DataDefinition) getDefinition("E.D"), Levels.EMPTY, dataTypeArgs));
    assertEquals(getDefinition("E.c"), getDefinition("E.D.c"));
  }

  @Test
  public void conFieldError() {
    typeCheckModule("""
      \\class E \\where {
        \\data D | c
      }
      \\func test (e : E) => E.c {e}
      """, 1);
  }

  @Test
  public void dataFieldError() {
    typeCheckModule("""
      \\class E \\where {
        \\data D | c
      }
      \\func test (e : E) => E.D.c {e}
      """, 1);
  }

  @Test
  public void conFieldDynamic() {
    typeCheckClass("""
      \\class E {
        \\data D | c
      }
      \\func test (e : E) => E.c {e}
      """, "");
    test(ConCall((Constructor) getDefinition("E.c"), Levels.EMPTY, new SingletonList<>(Ref(getThis().getNext()))));
    assertEquals(getDefinition("E.c"), getDefinition("E.D.c"));
  }

  @Test
  public void dataFieldDynamic() {
    typeCheckClass("""
      \\class E {
        \\data D | c
      }
      \\func test (e : E) => E.D.c {e}
      """, "");
    test(ConCall((Constructor) getDefinition("E.c"), Levels.EMPTY, new SingletonList<>(Ref(getThis().getNext()))));
    assertEquals(getDefinition("E.c"), getDefinition("E.D.c"));
  }

  @Test
  public void data2FieldDynamic() {
    typeCheckClass("""
      \\class E {
        \\data D (x : Nat) (y : Nat -> Nat) | c
      }
      \\func test (e : E) => E.D.c {e} {0} {\\lam _ => 1}
      """, "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis().getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition("E.c"), Levels.EMPTY, dataTypeArgs));
    testType(DataCall((DataDefinition) getDefinition("E.D"), Levels.EMPTY, dataTypeArgs));
    assertEquals(getDefinition("E.c"), getDefinition("E.D.c"));
  }

  @Test
  public void conFieldInside() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B \\where {
            \\data D | c
          }
        }
      }
      \\func test (e : E) => E.A.B.c {e}
      """);
    test(ConCall((Constructor) getDefinition("E.A.B.c"), Levels.EMPTY, new SingletonList<>(Ref(getThis()))));
    assertEquals(getDefinition("E.A.B.c"), getDefinition("E.A.B.D.c"));
  }

  @Test
  public void dataFieldInside() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B \\where {
            \\data D | c
          }
        }
      }
      \\func test (e : E) => E.A.B.D.c {e}
      """);
    test(ConCall((Constructor) getDefinition("E.A.B.c"), Levels.EMPTY, new SingletonList<>(Ref(getThis()))));
    assertEquals(getDefinition("E.A.B.c"), getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data2FieldInside() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B \\where {
            \\data D (x : Nat) (y : Nat -> Nat) | c
          }
        }
      }
      \\func test (e : E) => E.A.B.D.c {e} {0} {\\lam _ => 1}
      """);
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition("E.A.B.c"), Levels.EMPTY, dataTypeArgs));
    testType(DataCall((DataDefinition) getDefinition("E.A.B.D"), Levels.EMPTY, dataTypeArgs));
    assertEquals(getDefinition("E.A.B.c"), getDefinition("E.A.B.D.c"));
  }

  @Test
  public void conFieldInsideError() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B {
            \\data D | c
          }
        }
      }
      \\func test (e : E) => E.A.B.c {e}
      """, 1);
  }

  @Test
  public void dataFieldInsideError() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B {
            \\data D | c
          }
        }
      }
      \\func test (e : E) => E.A.B.D.c {e}
      """, 1);
  }

  @Test
  public void conFieldInsideError2() {
    typeCheckModule("""
      \\class E {
        \\class A {
          \\class B \\where {
            \\data D | c
          }
        }
      }
      \\func test (e : E) => E.A.B.c {e}
      """, 1);
  }

  @Test
  public void dataFieldInsideError2() {
    typeCheckModule("""
      \\class E {
        \\class A {
          \\class B \\where {
            \\data D | c
          }
        }
      }
      \\func test (e : E) => E.A.B.D.c {e}
      """, 1);
  }

  @Test
  public void classStatic() {
    typeCheckModule(
        "\\class C\n" +
        "\\func test => C");
    test(new ClassCallExpression((ClassDefinition) getDefinition("C"), Levels.EMPTY));
  }

  /*
  @Test
  public void classDynamic() {
    typeCheckClass(
        "\\class C\n" +
        "\\func test => C", "");
    test(makeClassCall(getDefinition("C"), Ref(getThis())));
  }

  @Test
  public void classDynamicFromInside() {
    typeCheckClass(
        "\\class C\n" +
        "\\class Test {\n" +
        "  \\func test => C\n" +
        "}", "");
    testFI(makeClassCall(getDefinition("C"), getThisFI()));
  }
  */

  @Test
  public void classDynamicNoError() {
    typeCheckModule("""
      \\class Test {
        \\class C
      } \\where {
        \\func test : \\Prop => C
      }
      """);
  }

  @Test
  public void classStaticInside() {
    typeCheckModule("""
      \\class A \\where {
        \\class B \\where {
          \\class C
        }
      }
      \\func test => A.B.C
      """);
    test(new ClassCallExpression((ClassDefinition) getDefinition("A.B.C"), Levels.EMPTY));
  }

  /*
  @Test
  public void classDynamicInside() {
    typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.C", "");
    test(makeClassCall(getDefinition("A.B.C"), Ref(getThis())));
  }
  */

  @Test
  public void classFieldStatic() {
    typeCheckModule("""
      \\class E {
        \\class C {e : E}
      }
      \\func test (e : E) => E.C {e}
      """);
    test(makeClassCall(getDefinition("E.C"), Ref(getThis())));
  }

  @Test
  public void classFieldError() {
    typeCheckModule("""
      \\class E \\where {
        \\class C
      }
      \\func test (e : E) => E.C {e}
      """, 1);
  }

  @Test
  public void classFieldDynamic() {
    typeCheckClass("""
      \\class E {
        \\class C {e : E}
      }
      \\func test (e : E) => E.C {e}
      """, "");
    test(makeClassCall(getDefinition("E.C"), Ref(getThis().getNext())));
  }

  @Test
  public void classFieldInside() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B \\where {
            \\class C {e : E}
          }
        }
      }
      \\func test (e : E) => E.A.B.C {e}
      """);
    test(makeClassCall(getDefinition("E.A.B.C"), Ref(getThis())));
  }

  @Test
  public void classFieldInsideError() {
    typeCheckModule("""
      \\class E {
        \\class A \\where {
          \\class B {
            \\class C
          }
        }
      }
      \\func test (e : E) => E.A.B.C {e}
      """, 1);
  }

  @Test
  public void classFieldInsideError2() {
    typeCheckModule("""
      \\class E {
        \\class A {
          \\class B \\where {
            \\class C
          }
        }
      }
      \\func test (e : E) => E.A.B.C {e}
      """, 1);
  }

  @Test
  public void local() {
    List<Binding> context = new ArrayList<>(1);
    context.add(new TypedBinding("x", Nat()));
    TypecheckingResult result = typeCheckExpr(context, "x", null);
    assertNotNull(result);
    assertEquals(Ref(context.get(0)), result.expression);
  }

  @Test
  public void nonStaticTest() {
    typeCheckModule("\\class A { \\func x => 0 } \\func y {a : A} => A.x {a}");
  }

  @Test
  public void staticTestError() {
    resolveNamesModule("\\class A \\where { \\func x => 0 } \\func y (a : A) => a.x", 1);
  }

  @Test
  public void innerNonStaticTestError() {
    typeCheckModule("\\class A { \\class B { \\func x => 0 } } \\func y (a : A) => A.B.x {a}", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void innerNonStaticTestAcc() {
    typeCheckModule("\\class A { \\class B {a : A} { \\func x => 0 } } \\func y (a : A) (b : A.B {a}) => A.B.x {b}");
  }

  @Test
  public void innerNonStaticTest() {
    typeCheckModule("\\class A { \\class B \\where { \\func x => 0 } } \\func y (a : A) => A.B.x {a}");
  }

  @Test
  public void staticTest() {
    typeCheckModule("\\class A \\where { \\func x => 0 } \\func y : Nat => A.x");
  }

  @Test
  public void resolvedConstructorTest() {
    ChildGroup cd = resolveNamesModule("""
      \\data TrP (A : \\Type) | inP A
      \\func isequiv {A B : \\Type0} (f : A -> B) => 0
      \\func inP-isequiv (P : \\Prop) => isequiv (inP {P})
      """);
    Iterator<? extends Statement> it = cd.getStatements().iterator();
    LocatedReferable inP = it.next().getGroup().getInternalReferables().iterator().next().getReferable();
    it.next();
    Concrete.FunctionDefinition lastDef = (Concrete.FunctionDefinition) ((ConcreteLocatedReferable) it.next().getGroup().getReferable()).getDefinition();
    ((Concrete.ReferenceExpression) ((Concrete.AppExpression) ((Concrete.AppExpression) ((Concrete.TermFunctionBody) lastDef.getBody()).getTerm()).getArguments().get(0).getExpression()).getFunction()).setReferent(inP);
    typeCheckModule(cd);
  }
}
