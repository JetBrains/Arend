package org.arend.classes;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ExpressionFactory.*;
import static org.arend.Matchers.argInferenceError;
import static org.arend.Matchers.notInScope;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class DynamicTest extends TypeCheckingTestCase {
  @Test
  public void dynamicIsNotVisible() {
    resolveNamesModule("""
      \\class A {
        \\func f => 0
      }
      \\func h => f
      """, 1);
    assertThatErrorsAre(notInScope("f"));
  }

  @Test
  public void dynamicDefinition() {
    typeCheckModule("""
      \\class A {
        \\func f => 0
      }
      \\func h (a : A) : Nat => A.f {a}
      """);
  }

  @Test
  public void dynamicStaticCallError() {
    resolveNamesModule("""
      \\class A \\where {
        \\func f => 0
      }
      \\func h (a : A) => a.f
      """, 1);
    assertThatErrorsAre(notInScope("f"));
  }

  @Test
  public void dynamicCallFromField() {
    resolveNamesModule("""
      \\class A {
        | x : 0 = f
        \\func f => 0
      }
      """, 1);
  }

  @Test
  public void fieldCallFromDynamic() {
    typeCheckModule("""
      \\func h (x : Nat) => x
      \\class A {
        | x : Nat
        \\func f => h x
      }
      \\func g (a : A { | x => 0 }) : a.x = A.f {a} => path (\\lam _ => 0)
      """);
  }

  @Test
  public void inheritanceFieldAccess() {
    typeCheckModule("""
      \\class X {
        \\class A {
          | n : Nat
        }
      }
      \\class B \\extends X.A {
        \\func my : Nat => n
      }
      """);
  }

  @Test
  public void dynamicInheritanceFieldAccessQualified() {
    typeCheckModule("""
      \\class X {
        \\class A {
          | n : Nat
        }
      }
      \\lemma x => \\new X
      \\class B \\extends X.A {
        \\func my : Nat => X.A.n
      }
      """);
  }

  @Test
  public void dynamicInnerFunctionCall() {
    typeCheckModule("""
      \\class A {
        \\class B \\where {
          \\func f => 0
        }
        \\func g => B.f
      }
      """);
  }

  @Test
  public void staticInnerFunctionCall() {
    typeCheckModule("""
      \\class A {
        \\func g => B.f
      } \\where {
        \\class B \\where {
          \\func f => 0
        }
      }
      """);
  }

  @Test
  public void staticFromDynamicCall() {
    typeCheckClass(
      "\\func h : Nat => f",
      "\\func f => 0");
  }

  @Test
  public void staticFromDynamicCallInside() {
    typeCheckModule("""
      \\class A {
        \\class B {a : A} {
          \\func h : Nat => f {a}
        } \\where {
          \\func f => 0
        }
      }
      """);
  }

  @Test
  public void dynamicFromAbstractCall() {
    typeCheckClass("""
      \\func f => 0
        | h : f = 0
      """, "", 1);
  }

  @Test
  public void dynamicFromDynamicCall() {
    typeCheckClass(
      "\\func f => 0\n" +
      "\\func h (_ : f = 0) => 0", "");
  }

  @Test
  public void dynamicConstructorFromDynamicCall() {
    typeCheckModule("""
      \\class A {
        \\data D | con
        \\func x (_ : con = con) => 0
      }
      \\func test (a : A) => A.x {a}
      """);
  }

  @Test
  public void dynamicFunctionWithPatterns() {
    typeCheckClass("| x : Nat -> Nat \\func pred (n : Nat) : Nat | zero => x zero | suc n => x n", "");
  }

  @Test
  public void dynamicFunctionWithImplicitPatterns() {
    typeCheckClass("| x : Nat -> Nat \\func f {m : Nat} (n : Nat) : Nat | {m}, zero => x m | suc n => x n", "");
  }

  @Test
  public void dynamicFunctionWithElim() {
    typeCheckClass("| x : Nat -> Nat \\func pred (n : Nat) : Nat \\elim n | zero => x zero | suc n => x n", "");
  }

  @Test
  public void dynamicFunctionWithImplicitElim() {
    typeCheckClass("| x : Nat -> Nat \\func f {m : Nat} (n : Nat) : Nat \\elim n | zero => x m | suc n => x n", "");
  }

  @Test
  public void dynamicDataWithPatterns() {
    typeCheckClass("| x : Nat \\data D (n : Nat) \\with | zero => con1 (x = 0) | suc n => con2 (x = n)", "");
  }

  @Test
  public void dynamicDataWithImplicitPatterns() {
    typeCheckClass("| x : Nat \\data D {m : Nat} (n : Nat) \\with | {m}, zero => con1 (x = m) | suc n => con2 (x = n)", "");
  }

  @Test
  public void dynamicDataWithElim() {
    typeCheckClass("| x : Nat \\data D (n : Nat) \\elim n | zero => con1 (x = 0) | suc n => con2 (x = n)", "");
  }

  @Test
  public void dynamicDataWithImplicitElim() {
    typeCheckClass("| x : Nat \\data D {m : Nat} (n : Nat) \\elim n | zero => con1 (x = m) | suc n => con2 (x = n)", "");
  }

  @Test
  public void dynamicClass() {
    typeCheckModule("""
      \\class A {
        \\class C
      }
      """);
  }

  @Test
  public void dynamicModule() {
    typeCheckModule("""
      \\class A {
        \\module C \\where { \\func f => 0 }
      }
      \\func g (a : A) : Nat => A.C.f {a}
      """);
  }

  @Test
  public void dynamicDoubleInnerClass() {
    typeCheckModule("""
      \\class A {
        \\class B {
          \\class C
        }
      }
      """);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall111() {
    typeCheckModule("""
      \\class A {
        \\func g => 0
        \\class B {
          \\class C {a : A} {
            \\func f : Nat => g {a}
          }
        }
      }
      """);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall011() {
    resolveNamesModule("""
      \\class A {
        \\func g => 0
      } \\where {
        \\class B {
          \\class C {
            \\func f : Nat => g
          }
        }
      }
      """, 1);
    assertThatErrorsAre(notInScope("g"));
  }

  @Test
  public void dynamicDoubleInnerFunctionCall101() {
    typeCheckModule("""
      \\class A {
        \\func g => 0
        \\class B \\where {
          \\class C {a : A} {
            \\func f : Nat => g {a}
          }
        }
      }
      """);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall001() {
    resolveNamesModule("""
      \\class A {
        \\func g => 0
      } \\where {
        \\class B \\where {
          \\class C {
            \\func f : Nat => g
          }
        }
      }
      """, 1);
    assertThatErrorsAre(notInScope("g"));
  }

  @Test
  public void dynamicDoubleInnerFunctionCall110() {
    typeCheckModule("""
      \\class A {
        \\func g => 0
        \\class B {a : A} {
          \\class C \\where {
            \\func f : Nat => g {a}
          }
        }
      }
      """);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall010() {
    resolveNamesModule("""
      \\class A {
        \\func g => 0
      } \\where {
        \\class B {
          \\class C \\where {
            \\func f : Nat => g
          }
        }
      }
      """, 1);
    assertThatErrorsAre(notInScope("g"));
  }

  @Test
  public void dynamicDoubleInnerFunctionCall100() {
    typeCheckModule("""
      \\class A {
        \\func g => 0
        \\class B \\where {
          \\class C \\where {
            \\func f : Nat => g
          }
        }
      }
      """);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall000() {
    resolveNamesModule("""
      \\class A {
        \\func g => 0
      } \\where {
        \\class B \\where {
          \\class C \\where {
            \\func f : Nat => g
          }
        }
      }
      """, 1);
    assertThatErrorsAre(notInScope("g"));
  }

  @Test
  public void recordTest() {
    typeCheckModule("""
      \\class B {
        | f : Nat -> \\Type0
        | g : f 0
      }
      \\func h (b : B) : b.f 0 => b.g
      """);
  }

  @Test
  public void innerRecordTest() {
    typeCheckModule("""
      \\class B {
        | f : Nat -> \\Type0
        \\class A {b : B} {
          | g : b.f 0
        }
      }
      \\func h (b : B) (a : B.A {b}) : b.f 0 => a.g
      """);
  }

  @Test
  public void constructorTest() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)
      }
      \\func f (a : A) : A.D {a} (a.x) (\\lam y => y) => A.con1 {a} (path (\\lam _ => a.x))
      \\func g (a : A) : A.D {a} (a.x) (\\lam y => y) => A.con2 {a} (path (\\lam _ => a.x))
      """);
  }

  @Test
  public void constructorWithParamsTest() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)
      }
      \\func f (a : A) : A.D {a} (a.x) (\\lam y => y) => A.con1 {a} {a.x} {\\lam y => y} (path (\\lam _ => a.x))
      \\func f' (a : A) => A.con1 {a} {a.x} {\\lam y => y} (path (\\lam _ => a.x))
      \\func g (a : A) : A.D {a} (a.x) (\\lam y => y) => A.con2 {a} {a.x} {\\lam y => y} (path (\\lam _ => a.x))
      \\func g' (a : A) => A.con2 {a} {a.x} {\\lam y => y} (path (\\lam _ => a.x))
      """);
  }

  @Test
  public void constructorThisTest() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)
        \\func f : D x (\\lam y => y) => con1 (path (\\lam _ => x))
        \\func g : D x (\\lam y => y) => con2 (path (\\lam _ => x))
      }
      \\func f (a : A) : A.D {a} (a.x) (\\lam y => y) => A.f {a}
      \\func f' (a : A) => A.f {a}
      \\func g (a : A) : A.D {a} (a.x) (\\lam y => y) => A.g {a}
      \\func g' (a : A) => A.g {a}
      """);
  }

  @Test
  public void constructorWithParamsThisTest() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)
        \\func f : D x (\\lam y => y) => con1 {_} {x} {\\lam y => y} idp
        \\func g => con2 {_} {x} {\\lam y => y} idp
      }
      \\func f (a : A) : A.D {a} (a.x) (\\lam y => y) => A.f {a}
      \\func f' (a : A) => A.f {a}
      \\func g (a : A) : A.D {a} (a.x) (\\lam y => y) => A.g {a}
      \\func g' (a : A) => A.g {a}
      """);
  }

  @Test
  public void constructorIndicesThisTest() {
    typeCheckClass("""
      | \\infix 6 + : Nat -> Nat -> Nat
      \\class A {t : Test} {
        | x : Nat
        \\data D (n : Nat) (f : Nat -> Nat -> Nat) \\elim n
          | zero => con1 (f x x = f x x)
          | suc n => con2 (D n f) (f n x = f n x)
        \\func f (n : Nat) : D n (+ {t})
          | zero => con1 (path (\\lam _ => x + {t} x))
          | suc n => con2 (f n) (path (\\lam _ => n + {t} x))
      }
      """, """
      \\func f {t : Test} (a : A {t}) (n : Nat) : A.D {a} n (+) => A.f {a} n
      \\func f' {t : Test} (a : A {t}) (n : Nat) => A.f {a}
      \\func g {t : Test} (a : A {t}) (n : Nat) : A.D {a} n (+) \\elim n
        | zero => A.con1 {a} (path (\\lam _ => a.x + a.x))
        | suc n => A.con2 {a} (g a n) (path (\\lam _ => n + a.x))
      """);
  }

  @Test
  public void fieldCallTest() {
    typeCheckModule("""
      \\class A {
        | x : \\Type0
      }
      \\class B {
        | a : A
        | y : a.x
      }
      """);
    ClassField xField = (ClassField) getDefinition("A.x");
    ClassField aField = (ClassField) getDefinition("B.a");
    ClassField yField = (ClassField) getDefinition("B.y");
    PiExpression piType = yField.getType(LevelPair.SET0);
    assertEquals(FieldCall(xField, FieldCall(aField, Ref(piType.getParameters()))), piType.getCodomain());
  }

  @Test
  public void funCallsTest() {
    typeCheckModule("""
      \\func \\infix 6 + (x y : Nat) => x
      \\class A {
        \\func q => p
        \\class C {a : A} {
          \\func k => h {a} + (p + q {a})
        } \\where {
          \\func h => p + q
        }
      } \\where {
        \\func p => 0
        \\class B {
          \\func g => f + p
        } \\where {
          \\func f : Nat => p
        }
      }
      """);
    FunctionDefinition plus = (FunctionDefinition) getDefinition("+");

    ClassDefinition aClass = (ClassDefinition) getDefinition("A");
    assertTrue(aClass.getNotImplementedFields().isEmpty() && aClass.getImplemented().isEmpty());
    FunctionDefinition pFun = (FunctionDefinition) getDefinition("A.p");
    assertEquals(Nat(), pFun.getTypeWithParams(new ArrayList<>(), LevelPair.SET0));
    assertEquals(Zero(), pFun.getBody());
    FunctionDefinition qFun = (FunctionDefinition) getDefinition("A.q");
    List<DependentLink> qParams = new ArrayList<>();
    Expression qType = qFun.getTypeWithParams(qParams, LevelPair.SET0);
    assertEquals(Pi(false, ClassCall(aClass), Nat()), fromPiParameters(qType, qParams));
    assertEquals(FunCall(pFun, Levels.EMPTY), qFun.getBody());

    ClassDefinition bClass = (ClassDefinition) getDefinition("A.B");
    assertTrue(bClass.getNotImplementedFields().isEmpty() && bClass.getImplemented().isEmpty());
    FunctionDefinition fFun = (FunctionDefinition) getDefinition("A.B.f");
    assertEquals(Nat(), fFun.getTypeWithParams(new ArrayList<>(), LevelPair.SET0));
    assertEquals(FunCall(pFun, Levels.EMPTY), fFun.getBody());
    FunctionDefinition gFun = (FunctionDefinition) getDefinition("A.B.g");
    List<DependentLink> gParams = new ArrayList<>();
    Expression gType = gFun.getTypeWithParams(gParams, LevelPair.SET0);
    assertEquals(Pi(false, ClassCall(bClass), Nat()), fromPiParameters(gType, gParams));
    assertEquals(FunCall(plus, Levels.EMPTY, FunCall(fFun, Levels.EMPTY), FunCall(pFun, Levels.EMPTY)), gFun.getBody());

    ClassDefinition cClass = (ClassDefinition) getDefinition("A.C");
    assertEquals(1, cClass.getNotImplementedFields().size());
    ClassField cParent = ((ClassDefinition) getDefinition("A.C")).getPersonalFields().get(0);
    assertNotNull(cParent);
    FunctionDefinition hFun = (FunctionDefinition) getDefinition("A.C.h");
    List<DependentLink> hParams = new ArrayList<>();
    Expression hType = hFun.getTypeWithParams(hParams, LevelPair.SET0);
    assertEquals(Pi(false, ClassCall(aClass), Nat()), fromPiParameters(hType, hParams));
    DependentLink hFunParam = param("\\this", ClassCall(aClass));
    assertEquals(FunCall(plus, Levels.EMPTY, FunCall(pFun, Levels.EMPTY), FunCall(qFun, Levels.EMPTY, Ref(hFunParam))), hFun.getBody());
    FunctionDefinition kFun = (FunctionDefinition) getDefinition("A.C.k");
    List<DependentLink> kParams = new ArrayList<>();
    Expression kType = kFun.getTypeWithParams(kParams, LevelPair.SET0);
    assertEquals(Pi(false, ClassCall(cClass), Nat()), fromPiParameters(kType, kParams));
    DependentLink kFunParam = param("\\this", ClassCall(cClass));
    Expression aRef = FieldCall(cParent, Ref(kFunParam));
    assertEquals(FunCall(plus, Levels.EMPTY, FunCall(hFun, Levels.EMPTY, aRef), FunCall(plus, Levels.EMPTY, FunCall(pFun, Levels.EMPTY), FunCall(qFun, Levels.EMPTY, aRef))), kFun.getBody());
  }

  @Test
  public void staticDynamicCall() {
    typeCheckModule("""
      \\class A {
        \\class B \\where {
          \\func f => 0
        }
      }
      \\func y (a : A) => A.B.f {a}
      """);
  }

  @Test
  public void staticDynamicCall2() {
    typeCheckModule("""
      \\class A {
        \\class B \\where {
          \\class C \\where {
            \\func f => 0
          }
        }
        | x : Nat
      }
      \\func y (a : A) => A.B.C.f {a}
      """);
  }

  @Test
  public void staticDynamicCall3() {
    typeCheckModule("""
      \\class A {
        \\class B \\where {
          \\class C \\where {
            \\func f => 0
          }
        }
        | x : Nat
      }
      \\func y (a : A) : \\Set0 => A.B.C {a}
      """, 1);
  }

  @Test
  public void staticDynamicCall31() {
    typeCheckModule("""
      \\class A {
        \\class B \\where {
          \\class C \\where {
            \\func f => 0
          }
        }
      }
      \\func y (a : A) : \\Prop => A.B.C {a}
      """, 1);
  }

  @Test
  public void staticDynamicCall4() {
    typeCheckModule("""
      \\class A {
        \\class B \\where {
          \\class C \\where {
            \\func f => 0
          }
        }
        | x : Nat
      }
      \\func y (a : A) : \\Set0 => A.B {a}
      """, 1);
  }

  @Test
  public void staticDynamicCall5() {
    typeCheckModule("""
      \\class D {
        \\class E \\where {
          \\func f => 0
        }
      }
      \\class A {
        \\class B \\where {
          \\class C \\where {
            \\lemma d : D => \\new D
          }
        }
      }
      \\func y (a : A) : D.E.f {A.B.C.d {a}} = 0 => idp
      """);
  }

  @Test
  public void staticDynamicCall6() {
    typeCheckModule("""
      \\class A {
        \\class B \\where {
          \\class C \\where {
            \\func d => e
              \\where \\func e => 0
          }
        }
      }
      \\func y (a : A) => A.B.C.d.e {a}
      """);
  }

  @Test
  public void staticDynamicCall7() {
    typeCheckModule("""
      \\class D {
        \\class E \\where {
          \\func f => 0
        }
      }
      \\class A {
        \\class B \\where {
          \\class C \\where {
            \\lemma d : D => \\new D
              \\where
                \\func E => 0
          }
        }
      }
      \\func y (a : A) : D.E.f {A.B.C.d {a}} = 0 => idp
      """);
  }

  @Test
  public void classPolyParams() {
    typeCheckModule("""
      \\class A {
         | X : \\0-Type \\lp
         \\func f (x : \\0-Type \\lp) => x
         \\data D (x : \\0-Type \\lp)
         \\class B {a : A} {
             | Y : a.X -> \\0-Type \\lp
             \\func g : \\0-Type \\lp => a.X
         }
      }
      """);
  }

  @Test
  public void classConstructorsTest() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        \\data Foo | foo (x = 0)
        \\func y : foo = foo => idp
      }
      \\func test (p : A) => A.y {p}
      """);
    FunctionDefinition testFun = (FunctionDefinition) getDefinition("test");
    Expression function = testFun.getResultType().normalize(NormalizationMode.WHNF);
    assertEquals(Prelude.PATH, function.cast(DataCallExpression.class).getDefinition());
    List<? extends Expression> arguments = function.cast(DataCallExpression.class).getDefCallArguments();
    assertEquals(3, arguments.size());

    Constructor foo = ((DataDefinition) getDefinition("A.Foo")).getConstructor("foo");

    ConCallExpression arg2 = arguments.get(2).cast(LamExpression.class).getBody().cast(ConCallExpression.class);
    assertEquals(1, arg2.getDataTypeArguments().size());
    assertEquals(Ref(testFun.getParameters()), arg2.getDataTypeArguments().get(0));
    assertEquals(foo, arg2.getDefinition());

    ConCallExpression arg1 = arguments.get(1).cast(LamExpression.class).getBody().cast(ConCallExpression.class);
    assertEquals(1, arg1.getDataTypeArguments().size());
    assertEquals(Ref(testFun.getParameters()), arg1.getDataTypeArguments().get(0));
    assertEquals(foo, arg1.getDefinition());

    Expression domFunction = arguments.get(0).cast(LamExpression.class).getBody().cast(PiExpression.class).getParameters().getTypeExpr().normalize(NormalizationMode.WHNF);
    assertEquals(Prelude.PATH, domFunction.cast(DataCallExpression.class).getDefinition());
    List<? extends Expression> domArguments = domFunction.cast(DataCallExpression.class).getDefCallArguments();
    assertEquals(3, domArguments.size());
    assertEquals(Prelude.NAT, domArguments.get(0).cast(LamExpression.class).getBody().cast(DefCallExpression.class).getDefinition());
    assertEquals(FieldCall((ClassField) getDefinition("A.x"), Ref(testFun.getParameters())), domArguments.get(1));
    assertEquals(0, domArguments.get(2).cast(SmallIntegerExpression.class).getInteger());
  }

  @Test
  public void classConstructorsParametersTest() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        \\data Foo (p : x = x) | foo (p = p)
        \\func y (_ : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))) => 0
      }
      \\func test (q : A) => A.y {q}
      """);
    FunctionDefinition testFun = (FunctionDefinition) getDefinition("test");
    Expression xCall = FieldCall((ClassField) getDefinition("A.x"), Ref(testFun.getParameters()));
    Expression function = testFun.getResultType().cast(PiExpression.class).getParameters().getTypeExpr().normalize(NormalizationMode.NF);
    assertEquals(Prelude.PATH, function.cast(DataCallExpression.class).getDefinition());
    List<? extends Expression> arguments = function.cast(DataCallExpression.class).getDefCallArguments();
    assertEquals(3, arguments.size());

    DataDefinition Foo = (DataDefinition) getDefinition("A.Foo");
    Constructor foo = Foo.getConstructor("foo");

    ConCallExpression arg2Fun = arguments.get(2).cast(ConCallExpression.class);
    assertEquals(2, arg2Fun.getDataTypeArguments().size());
    assertEquals(Ref(testFun.getParameters()), arg2Fun.getDataTypeArguments().get(0));
    PathExpression expr1 = arg2Fun.getDataTypeArguments().get(1).cast(PathExpression.class);
    assertEquals(xCall, expr1.getArgument().cast(LamExpression.class).getBody());

    assertEquals(foo, arg2Fun.getDefinition());
    PathExpression expr2 = arg2Fun.getDefCallArguments().get(0).cast(PathExpression.class);
    PathExpression expr3 = expr2.getArgument().cast(LamExpression.class).getBody().cast(PathExpression.class);
    assertEquals(xCall, expr3.getArgument().cast(LamExpression.class).getBody());

    ConCallExpression arg1Fun = arguments.get(1).cast(ConCallExpression.class);
    assertEquals(2, arg1Fun.getDataTypeArguments().size());
    assertEquals(Ref(testFun.getParameters()), arg1Fun.getDataTypeArguments().get(0));
    assertEquals(expr1, arg1Fun.getDataTypeArguments().get(1));
    assertEquals(foo, arg1Fun.getDefinition());
    PathExpression expr4 = arg1Fun.getDefCallArguments().get(0).cast(PathExpression.class);
    PathExpression expr5 = expr4.getArgument().cast(LamExpression.class).getBody().cast(PathExpression.class);
    assertEquals(xCall, expr5.getArgument().cast(LamExpression.class).getBody());

    LamExpression arg0 = arguments.get(0).cast(LamExpression.class);
    assertEquals(Foo, arg0.getBody().cast(DataCallExpression.class).getDefinition());
    assertEquals(Ref(testFun.getParameters()), arg0.getBody().cast(DataCallExpression.class).getDefCallArguments().get(0));
    PathExpression paramConCall = arg0.getBody().cast(DataCallExpression.class).getDefCallArguments().get(1).cast(PathExpression.class);
    assertEquals(xCall, paramConCall.getArgument().cast(LamExpression.class).getBody());
  }

  @Test
  public void dynamicFunctionCall() {
    typeCheckModule("""
      \\class A (X : \\Type) {
        \\func f => 0
        \\func g => f
      }
      """);
  }

  @Test
  public void dynamicSuperFunctionCall() {
    typeCheckModule("""
      \\class A (X : \\Type) {
        \\func f => 0
      }
      \\class B \\extends A {
        \\func g => A.f}
      """);
  }

  @Test
  public void dynamicFunctionCallExplicit() {
    typeCheckModule("""
      \\class A (X : \\Type) {
        | x : X
        \\func f => x
        \\func g : Nat => f {\\new A Nat 0}
      }
      """);
  }

  @Test
  public void dynamicDotCall() {
    typeCheckModule("""
      \\record R {
        \\func f => 0
      }
      \\func g (r : R) => r.f
      """);
  }

  @Test
  public void dynamicInheritance() {
    resolveNamesModule("""
      \\class X {
        \\class A
      }
      \\lemma x : X => \\new X
      \\class B \\extends x.A
      """, 1);
  }

  @Test
  public void dynamicFunctionInExtends() {
    typeCheckModule("""
      \\record R {
        \\func f => 0
      }
      \\record S \\extends R {
        \\func g => f
      }
      """);
  }

  @Test
  public void thisVarTest() {
    typeCheckModule("""
      \\record RRR {
        | xxx : Nat
        \\func foo (z : Nat) : Nat \\elim z
          | 0 => 0
          | suc z => foo {_} z
      }
      """, 1);
    assertThatErrorsAre(argInferenceError());
  }

  @Test
  public void dynamicInStatic() {
    resolveNamesModule("""
      \\record A {
        \\func foo => 0
      }
      \\record B \\extends A \\where {
        \\func test => foo
      }
      """, 1);
    assertThatErrorsAre(notInScope("foo"));
  }
}
