package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolverTestCase.resolveNamesClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecordsTest {
  @Test
  public void unknownExtTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => 0 | z => 0 | y => 0 }", 1);
  }

  /*
  @Test
  public void typeMismatchMoreTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x (a : Nat) => a }", 1);
  }

  @Test
  public void typeMismatchLessTest() {
    typeCheckClass(
        "\\static \\class C { \\abstract f (x y z : Nat) : Nat }\n" +
        "\\static \\function D => C { f a => \\lam z w => z }");
  }

  @Test
  public void argTypeMismatchTestError() {
    typeCheckClass(
        "\\static \\class C { \\abstract f (a : Nat) : Nat }\n" +
        "\\static \\function D => C { f (a : Nat -> Nat) => 0 }", 1);
  }
  */

  @Test
  public void resultTypeMismatchTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => \\lam (t : Nat) => t }", 1);
  }

  @Test
  public void parentCallTest() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract c : Nat -> Nat -> Nat\n" +
        "  \\abstract f : Nat -> Nat\n" +
        "}\n" +
        "\\static \\function B => A {\n" +
        "  f => \\lam n => c n n\n" +
        "}", 1);
  }

  /*
  @Test
  public void recursiveTestError() {
    typeCheckClass(
        "\\static \\class A { \\abstract f : Nat -> Nat }\n" +
        "\\static \\function B => A { \\override f n <= \\elim n | zero => zero | suc n' => f (suc n') }", 1);
  }
  */

  @Test
  public void duplicateNameTestError() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract f : Nat\n" +
        "}\n" +
        "\\static \\function B => A {\n" +
        "  | f => 0\n" +
        "  | f => 1\n" +
        "}", 1);
  }

  @Test
  public void overriddenFieldAccTest() {
    typeCheckClass(
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function diagonal => \\lam (d : Nat) => Point {\n" +
        "  | x => d\n" +
        "  | y => d\n" +
        "}\n" +
        "\\static \\function test (p : diagonal 0) : p.x = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void newAbstractTestError() {
    typeCheckClass(
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function diagonal => Point { y => 0 }\n" +
        "\\static \\function test => \\new diagonal", 1);
  }

  @Test
  public void newTest() {
    typeCheckClass(
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function diagonal => \\lam (d : Nat) => Point {\n" +
        "  | x => d\n" +
        "  | y => d\n" +
        "}\n" +
        "\\static \\function diagonal1 => Point {\n" +
        "  | x => 0\n" +
        "  | y => 0\n" +
        "}\n" +
        "\\static \\function test : \\new diagonal1 = \\new diagonal 0 => path (\\lam _ => \\new Point { x => 0 | y => 0 })");
  }

  @Test
  public void mutualRecursionTestError() {
    typeCheckClass("test",
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function test => Point {\n" +
        "  | x => y\n" +
        "  | y => x\n" +
        "}", 2);
  }

  @Test
  public void splitClassTestError() {
    resolveNamesClass("test",
        "\\static \\class A {\n" +
        "  \\static \\function x => 0\n" +
        "}\n" +
        "\\static \\class A {\n" +
        "  \\static \\function y => 0\n" +
        "}", 1);
  }

  @Test
  public void recordUniverseTest() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => 0 }");
    assertEquals(TypeUniverse.SetOfLevel(0), member.namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(TypeUniverse.SetOfLevel(0)), member.namespace.getDefinition("C").getType());
  }

  @Test
  public void recordUniverseTest2() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => 0 | y => 1 }");
    assertEquals(TypeUniverse.SetOfLevel(0), member.namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(TypeUniverse.PROP), member.namespace.getDefinition("C").getType());
  }

  @Test
  public void recordUniverseTest3() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class Point { \\abstract x : \\Type3 \\abstract y : \\Type1 }\n" +
        "\\static \\function C => Point { x => Nat }");
    assertEquals(new TypeUniverse(4, TypeUniverse.NOT_TRUNCATED), member.namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(new TypeUniverse(2, TypeUniverse.NOT_TRUNCATED)), member.namespace.getDefinition("C").getType());
  }

  @Test
  public void recordConstructorsTest() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data Foo | foo (x = 0)\n" +
        "  \\function y : foo = foo => path (\\lam _ => foo)\n" +
        "}\n" +
        "\\static \\function test (p : A) => p.y");
    FunctionDefinition testFun = (FunctionDefinition) member.namespace.getDefinition("test");
    Expression resultType = testFun.getResultType();
    Expression function = resultType.normalize(NormalizeVisitor.Mode.WHNF);
    List<? extends Expression> arguments = function.getArguments();
    function = function.getFunction();
    assertEquals(5, arguments.size());
    assertEquals(DataCall(Prelude.PATH), function);

    DataDefinition Foo = (DataDefinition) member.namespace.findChild("A").getDefinition("Foo");
    Constructor foo = Foo.getConstructor("foo");

    ConCallExpression arg2 = arguments.get(4).toConCall();
    assertNotNull(arg2);
    assertEquals(1, arg2.getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), arg2.getDataTypeArguments().get(0));
    assertEquals(foo, arg2.getDefinition());

    ConCallExpression arg1 = arguments.get(3).toConCall();
    assertNotNull(arg1);
    assertEquals(1, arg1.getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), arg1.getDataTypeArguments().get(0));
    assertEquals(foo, arg1.getDefinition());

    LamExpression arg0 = arguments.get(2).toLam();
    assertNotNull(arg0);
    PiExpression arg0Body = arg0.getBody().toPi();
    assertNotNull(arg0Body);
    Expression domFunction = arg0Body.getParameters().getType();
    List<? extends Expression> domArguments = domFunction.getArguments();
    domFunction = domFunction.getFunction();
    assertEquals(5, domArguments.size());
    assertEquals(DataCall(Prelude.PATH), domFunction);

    LamExpression domArg0 = domArguments.get(2).toLam();
    assertNotNull(domArg0);
    DefCallExpression domArg0Body = domArg0.getBody().toDefCall();
    assertNotNull(domArg0Body);
    assertEquals(Preprelude.NAT, domArg0Body.getDefinition());

    assertEquals(1, domArguments.get(3).getArguments().size());
    assertEquals(Reference(testFun.getParameters()), domArguments.get(3).getArguments().get(0));
    assertEquals(member.namespace.findChild("A").getDefinition("x").getDefCall(), domArguments.get(3).getFunction());

    ConCallExpression domArg2 = domArguments.get(4).toConCall();
    assertNotNull(domArg2);
    assertEquals(Preprelude.ZERO, domArg2.getDefinition());
  }

  @Test
  public void recordConstructorsParametersTest() {
    NamespaceMember member = typeCheckClass(
      "\\static \\class A {\n" +
      "  \\abstract x : Nat\n" +
      "  \\data Foo (p : x = x) | foo (p = p)\n" +
      "  \\function y (_ : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))) => 0\n" +
      "}\n" +
      "\\static \\function test (q : A) => q.y");
    FunctionDefinition testFun = (FunctionDefinition) member.namespace.getDefinition("test");
    Expression resultType = testFun.getResultType();
    Expression xCall = member.namespace.findChild("A").getDefinition("x").getDefCall();
    PiExpression resultTypePi = resultType.toPi();
    assertNotNull(resultTypePi);
    Expression function = resultTypePi.getParameters().getType().normalize(NormalizeVisitor.Mode.NF);
    List<? extends Expression> arguments = function.getArguments();
    function = function.getFunction();
    assertEquals(5, arguments.size());
    assertEquals(DataCall(Prelude.PATH), function);

    DataDefinition Foo = (DataDefinition) member.namespace.findChild("A").getDefinition("Foo");
    Constructor foo = Foo.getConstructor("foo");

    ConCallExpression arg2Fun = arguments.get(4).getFunction().toConCall();
    assertNotNull(arg2Fun);
    assertEquals(2, arg2Fun.getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), arg2Fun.getDataTypeArguments().get(0));
    Expression expr1 = arg2Fun.getDataTypeArguments().get(1);
    LamExpression expr1Arg0 = expr1.getArguments().get(0).toLam();
    assertNotNull(expr1Arg0);
    assertEquals(xCall, expr1Arg0.getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), expr1Arg0.getBody().getArguments().get(0));

    assertEquals(foo, arguments.get(4).getFunction().toDefCall().getDefinition());
    LamExpression appPath00Arg0 = arguments.get(4).getArguments().get(0).getArguments().get(0).toLam();
    assertNotNull(appPath00Arg0);
    LamExpression appPath01Arg0 = appPath00Arg0.getBody().getArguments().get(0).toLam();
    assertNotNull(appPath01Arg0);
    assertEquals(xCall, appPath01Arg0.getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), appPath01Arg0.getBody().getArguments().get(0));

    ConCallExpression arg1Fun = arguments.get(3).getFunction().toConCall();
    assertNotNull(arg1Fun);
    assertEquals(2, arg1Fun.getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), arg1Fun.getDataTypeArguments().get(0));
    assertEquals(expr1, arg1Fun.getDataTypeArguments().get(1));
    assertEquals(foo, arg1Fun.getDefinition());
    LamExpression appPath10Arg0 = arguments.get(3).getArguments().get(0).getArguments().get(0).toLam();
    assertNotNull(appPath10Arg0);
    LamExpression appPath11Arg0 = appPath10Arg0.getBody().getArguments().get(0).toLam();
    assertEquals(xCall, appPath11Arg0.getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), appPath11Arg0.getBody().getArguments().get(0));

    LamExpression arg0 = arguments.get(2).toLam();
    assertNotNull(arg0);
    assertEquals(Foo.getDefCall(), arg0.getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), arg0.getBody().getArguments().get(0));
    Expression parameterFunction = arg0.getBody().getArguments().get(1);
    List<? extends Expression> parameterArguments = parameterFunction.getArguments();
    parameterFunction = parameterFunction.getFunction();
    assertEquals(1, parameterArguments.size());
    assertEquals(ConCall(Prelude.PATH_CON), parameterFunction);
    LamExpression paramArg0 = parameterArguments.get(0).toLam();
    assertNotNull(paramArg0);
    assertEquals(xCall, paramArg0.getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), paramArg0.getBody().getArguments().get(0));

    ConCallExpression paramConCall = parameterFunction.toConCall();
    assertNotNull(paramConCall);
    List<Expression> parameters = paramConCall.getDataTypeArguments();
    assertEquals(5, parameters.size());

    LamExpression param0 = parameters.get(2).toLam();
    assertNotNull(param0);
    assertEquals(Nat(), param0.getBody());

    Expression parameter1 = parameters.get(3).normalize(NormalizeVisitor.Mode.WHNF);
    assertEquals(xCall, parameter1.getFunction());
    assertEquals(Reference(testFun.getParameters()), parameter1.getArguments().get(0));

    Expression parameter2 = parameters.get(4).normalize(NormalizeVisitor.Mode.WHNF);
    assertEquals(xCall, parameter2.getFunction());
    assertEquals(Reference(testFun.getParameters()), parameter2.getArguments().get(0));
  }
}
