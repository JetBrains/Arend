package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecordsTest extends TypeCheckingTestCase {
  @Test
  public void unknownExtTestError() {
    resolveNamesClass(
        "\\class Point { \\field x : Nat \\field y : Nat }\n" +
        "\\function C => Point { x => 0 | z => 0 | y => 0 }", 1);
  }

  @Test
  public void resultTypeMismatchTestError() {
    typeCheckClass(
        "\\class Point { \\field x : Nat \\field y : Nat }\n" +
        "\\function C => Point { x => \\lam (t : Nat) => t }", 1);
  }

  @Test
  public void parentCallTest() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field c : Nat -> Nat -> Nat\n" +
        "  \\field f : Nat -> Nat\n" +
        "}\n" +
        "\\function B => A {\n" +
        "  f => \\lam n => c n n\n" +
        "}", 1, 1);
  }

  @Test
  public void duplicateNameTestError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field f : Nat\n" +
        "}\n" +
        "\\function B => A {\n" +
        "  | f => 0\n" +
        "  | f => 1\n" +
        "}", 1);
  }

  @Test
  public void overriddenFieldAccTest() {
    typeCheckClass(
        "\\class Point {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : Nat\n" +
        "}\n" +
        "\\function diagonal => \\lam (d : Nat) => Point {\n" +
        "  | x => d\n" +
        "  | y => d\n" +
        "}\n" +
        "\\function test (p : diagonal 0) : p.x = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void newAbstractTestError() {
    typeCheckClass(
        "\\class Point {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : Nat\n" +
        "}\n" +
        "\\function diagonal => Point { y => 0 }\n" +
        "\\function test => \\new diagonal", 1);
  }

  @Test
  public void newTest() {
    typeCheckClass(
        "\\class Point {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : Nat\n" +
        "}\n" +
        "\\function diagonal => \\lam (d : Nat) => Point {\n" +
        "  | x => d\n" +
        "  | y => d\n" +
        "}\n" +
        "\\function diagonal1 => Point {\n" +
        "  | x => 0\n" +
        "  | y => 0\n" +
        "}\n" +
        "\\function test : \\new diagonal1 = \\new diagonal 0 => path (\\lam _ => \\new Point { x => 0 | y => 0 })");
  }

  @Test
  public void mutualRecursionTestError() {
    typeCheckClass(
        "\\class Point {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : Nat\n" +
        "}\n" +
        "\\function test => Point {\n" +
        "  | x => y\n" +
        "  | y => x\n" +
        "}", 2, 2);
  }

  @Test
  public void splitClassTestError() {
    resolveNamesClass(
        "\\class A \\where {\n" +
        "  \\function x => 0\n" +
        "}\n" +
        "\\class A \\where {\n" +
        "  \\function y => 0\n" +
        "}", 1);
  }

  @Test
  public void recordUniverseTest() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Point { \\field x : Nat \\field y : Nat }\n" +
        "\\function C => Point { x => 0 }");
    assertEquals(Sort.SET0, ((ClassDefinition) result.getDefinition("Point")).getSort());
    assertEquals(Universe(Sort.SET0), result.getDefinition("C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordUniverseTest2() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Point { \\field x : Nat \\field y : Nat }\n" +
        "\\function C => Point { x => 0 | y => 1 }");
    assertEquals(Sort.SET0, ((ClassDefinition) result.getDefinition("Point")).getSort());
    assertEquals(Universe(Sort.PROP), result.getDefinition("C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordUniverseTest3() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Point { \\field x : \\Type3 \\field y : \\Type1 }\n" +
        "\\function C => Point { x => Nat }");
    assertEquals(new Sort(new Level(4), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) result.getDefinition("Point")).getSort());
    assertEquals(Universe(new Sort(2, 1)), result.getDefinition("C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordUniverseTest4() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Point { \\field x : \\Type3 \\field y : \\oo-Type1 }\n" +
        "\\function C => Point { x => Nat }");
    assertEquals(new Sort(new Level(4), Level.INFINITY), ((ClassDefinition) result.getDefinition("Point")).getSort());
    assertEquals(Universe(new Sort(new Level(2), Level.INFINITY)), result.getDefinition("C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordUniverseTest5() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Point { \\field x : \\Type3 \\field y : \\Type1 }\n" +
        "\\function C => Point { x => \\Type2 }");
    assertEquals(new Sort(new Level(4), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) result.getDefinition("Point")).getSort());
    assertEquals(Universe(new Sort(new Level(2), new Level(LevelVariable.HVAR, 2))), result.getDefinition("C").getTypeWithParams(new ArrayList<>(), Sort.STD));
  }

  @Test
  public void recordConstructorsTest() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\data Foo | foo (x = 0)\n" +
        "  \\function y : foo = foo => path (\\lam _ => foo)\n" +
        "}\n" +
        "\\function test (p : A) => p.y");
    FunctionDefinition testFun = (FunctionDefinition) result.getDefinition("test");
    Expression function = testFun.getResultType().normalize(NormalizeVisitor.Mode.WHNF);
    assertEquals(Prelude.PATH, function.toDataCall().getDefinition());
    List<? extends Expression> arguments = function.toDataCall().getDefCallArguments();
    assertEquals(3, arguments.size());

    DataDefinition Foo = (DataDefinition) result.getDefinition("A.Foo");
    Constructor foo = Foo.getConstructor("foo");

    ConCallExpression arg2 = arguments.get(2).toLam().getBody().toConCall();
    assertNotNull(arg2);
    assertEquals(1, arg2.getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), arg2.getDataTypeArguments().get(0));
    assertEquals(foo, arg2.getDefinition());

    ConCallExpression arg1 = arguments.get(1).toLam().getBody().toConCall();
    assertNotNull(arg1);
    assertEquals(1, arg1.getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), arg1.getDataTypeArguments().get(0));
    assertEquals(foo, arg1.getDefinition());

    LamExpression arg0 = arguments.get(0).toLam();
    assertNotNull(arg0);
    PiExpression arg0Body = arg0.getBody().toPi();
    assertNotNull(arg0Body);
    Expression domFunction = arg0Body.getParameters().getType().getExpr();
    assertEquals(Prelude.PATH, domFunction.toDataCall().getDefinition());
    List<? extends Expression> domArguments = domFunction.toDataCall().getDefCallArguments();
    assertEquals(3, domArguments.size());

    LamExpression domArg0 = domArguments.get(0).toLam();
    assertNotNull(domArg0);
    DefCallExpression domArg0Body = domArg0.getBody().toDefCall();
    assertNotNull(domArg0Body);
    assertEquals(Prelude.NAT, domArg0Body.getDefinition());

    assertEquals(FieldCall((ClassField) result.getDefinition("A.x"), Reference(testFun.getParameters())), domArguments.get(1));

    ConCallExpression domArg2 = domArguments.get(2).toConCall();
    assertNotNull(domArg2);
    assertEquals(Prelude.ZERO, domArg2.getDefinition());
  }

  @Test
  public void recordConstructorsParametersTest() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\data Foo (p : x = x) | foo (p = p)\n" +
        "  \\function y (_ : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))) => 0\n" +
        "}\n" +
        "\\function test (q : A) => q.y");
    FunctionDefinition testFun = (FunctionDefinition) result.getDefinition("test");
    Expression xCall = FieldCall((ClassField) result.getDefinition("A.x"), Reference(testFun.getParameters()));
    PiExpression resultTypePi = testFun.getResultType().toPi();
    assertNotNull(resultTypePi);
    Expression function = resultTypePi.getParameters().getType().getExpr().normalize(NormalizeVisitor.Mode.NF);
    assertEquals(Prelude.PATH, function.toDataCall().getDefinition());
    List<? extends Expression> arguments = function.toDataCall().getDefCallArguments();
    assertEquals(3, arguments.size());

    DataDefinition Foo = (DataDefinition) result.getDefinition("A.Foo");
    Constructor foo = Foo.getConstructor("foo");

    ConCallExpression arg2Fun = arguments.get(2).toConCall();
    assertNotNull(arg2Fun);
    assertEquals(2, arg2Fun.getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), arg2Fun.getDataTypeArguments().get(0));
    ConCallExpression expr1 = arg2Fun.getDataTypeArguments().get(1).toConCall();
    assertNotNull(expr1);
    assertEquals(Prelude.PATH_CON, expr1.getDefinition());
    LamExpression expr1Arg0 = expr1.getDefCallArguments().get(0).toLam();
    assertNotNull(expr1Arg0);
    assertEquals(xCall, expr1Arg0.getBody());

    assertEquals(foo, arg2Fun.getDefinition());
    ConCallExpression expr2 = arg2Fun.getDefCallArguments().get(0).toConCall();
    assertNotNull(expr2);
    assertEquals(Prelude.PATH_CON, expr2.getDefinition());
    LamExpression appPath00Arg0 = expr2.getDefCallArguments().get(0).toLam();
    assertNotNull(appPath00Arg0);
    ConCallExpression expr3 = appPath00Arg0.getBody().toConCall();
    assertNotNull(expr3);
    assertEquals(Prelude.PATH_CON, expr3.getDefinition());
    LamExpression appPath01Arg0 = expr3.getDefCallArguments().get(0).toLam();
    assertNotNull(appPath01Arg0);
    assertEquals(xCall, appPath01Arg0.getBody());

    ConCallExpression arg1Fun = arguments.get(1).toConCall();
    assertNotNull(arg1Fun);
    assertEquals(2, arg1Fun.getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), arg1Fun.getDataTypeArguments().get(0));
    assertEquals(expr1, arg1Fun.getDataTypeArguments().get(1));
    assertEquals(foo, arg1Fun.getDefinition());
    ConCallExpression expr4 = arg1Fun.getDefCallArguments().get(0).toConCall();
    assertNotNull(expr4);
    assertEquals(Prelude.PATH_CON, expr4.getDefinition());
    LamExpression appPath10Arg0 = expr4.getDefCallArguments().get(0).toLam();
    assertNotNull(appPath10Arg0);
    ConCallExpression expr5 = appPath10Arg0.getBody().toConCall();
    assertNotNull(expr5);
    assertEquals(Prelude.PATH_CON, expr5.getDefinition());
    LamExpression appPath11Arg0 = expr5.getDefCallArguments().get(0).toLam();
    assertEquals(xCall, appPath11Arg0.getBody());

    LamExpression arg0 = arguments.get(0).toLam();
    assertNotNull(arg0);
    assertNotNull(arg0.getBody().toDataCall());
    assertEquals(Foo, arg0.getBody().toDataCall().getDefinition());
    assertEquals(Reference(testFun.getParameters()), arg0.getBody().toDataCall().getDefCallArguments().get(0));
    ConCallExpression paramConCall = arg0.getBody().toDataCall().getDefCallArguments().get(1).toConCall();
    assertNotNull(paramConCall);
    assertEquals(Prelude.PATH_CON, paramConCall.getDefinition());
    assertEquals(1, paramConCall.getDefCallArguments().size());
    LamExpression paramArg0 = paramConCall.getDefCallArguments().get(0).toLam();
    assertNotNull(paramArg0);
    assertEquals(xCall, paramArg0.getBody());

    List<? extends Expression> parameters = paramConCall.getDataTypeArguments();
    assertEquals(3, parameters.size());

    LamExpression param0 = parameters.get(0).toLam();
    assertNotNull(param0);
    assertEquals(Nat(), param0.getBody());

    assertEquals(xCall, parameters.get(1).normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(xCall, parameters.get(2).normalize(NormalizeVisitor.Mode.WHNF));
  }
}
