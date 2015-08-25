package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.DefCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecordsTest {
  @Test
  public void recordTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class B { \\function f : Nat -> \\Type0 \\function g : f 0 } \\function f (p : B) : p.f 0 => p.g ");
  }

  @Test
  public void unknownExtTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 \\override z => 0 \\override y => 0 }", 1);
  }

  @Test
  public void typeMismatchMoreTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x (a : Nat) => a }", 1);
  }

  @Test
  public void typeMismatchLessTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class C { \\function f (x y z : Nat) : Nat } \\function D => C { \\override f a => \\lam z w => z }");
  }

  @Test
  public void argTypeMismatchTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class C { \\function f (a : Nat) : Nat } \\function D => C { \\override f (a : Nat -> Nat) => 0 }", 1);
  }

  @Test
  public void resultTypeMismatchTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => \\lam (t : Nat) => t }", 1);
  }

  @Test
  public void parentCallTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader,
        "\\class A {\n" +
          "\\function c : Nat -> Nat -> Nat\n" +
          "\\function f : Nat -> Nat\n" +
        "}\n" +
          "\\function B => A {\n" +
          "\\override f n <= c n n\n" +
        "}");
  }

  @Test
  public void recursiveTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class A { \\function f : Nat -> Nat } \\function B => A { \\override f n <= \\elim n | (zero) => zero | (suc n') => f (suc n') }", 1);
  }

  @Test
  public void duplicateNameTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
            "\\function f : Nat -> Nat\n" +
        "}\n" +
        "\\function B => A {\n" +
            "\\function f (n : Nat) <= n\n" +
        "}";
    parseDefs(moduleLoader, text, 1, 0);
  }

  @Test
  public void overriddenFieldAccTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function diagonal => \\lam (d : Nat) => Point {\n" +
          "\\override x => d\n" +
          "\\override y => d\n" +
        "}\n" +
        "\\function test (p : diagonal 0) : p.x = 0 => path (\\lam _ => 0)";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void newAbstractTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function diagonal => Point {\n" +
          "\\override y => x\n" +
        "}\n" +
        "\\function test => \\new diagonal";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void newTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function diagonal => \\lam (d : Nat) => Point {\n" +
          "\\override x => d\n" +
          "\\override y => d\n" +
        "}\n" +
        "\\function diagonal1 => Point {\n" +
          "\\override x => 0\n" +
          "\\override y => x\n" +
        "}\n" +
        "\\function test : \\new diagonal1 = \\new diagonal 0 => path (\\lam _ => \\new diagonal 0)";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void mutualRecursionTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function test => Point {\n" +
          "\\override x => y\n" +
          "\\override y => x\n" +
        "}";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void splitClassTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
          "\\function x : Nat\n" +
        "}\n" +
        "\\class A {\n" +
          "\\function y => 0\n" +
        "}";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void splitClassTest2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
          "\\function x => 0\n" +
        "}\n" +
        "\\class A {\n" +
          "\\function y : Nat\n" +
        "}";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void splitClassTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
          "\\function x : Nat\n" +
        "}\n" +
        "\\class A {\n" +
          "\\function y : Nat\n" +
        "}";
    parseDefs(moduleLoader, text, 1, 0);
  }

  @Test
  public void recordUniverseTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition result = parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 }");
    assertEquals(new Universe.Type(0, Universe.Type.SET), result.getField("Point").getUniverse());
    assertEquals(new Universe.Type(0, Universe.Type.SET), result.getField("C").getUniverse());
  }

  @Test
  public void recordConstructorsTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition classDef = parseDefs(moduleLoader, "\\class A { \\function x : Nat \\data Foo | foo (x = 0) \\function y : foo = foo } \\function test (p : A) => p.y");
    Expression resultType = ((FunctionDefinition) classDef.getPrivateField("test")).getResultType();
    List<Expression> arguments = new ArrayList<>(3);
    Expression function = resultType.normalize(NormalizeVisitor.Mode.WHNF).getFunction(arguments);
    assertEquals(3, arguments.size());
    assertEquals(DefCall(Prelude.PATH), function);

    assertTrue(arguments.get(0) instanceof DefCallExpression);
    assertEquals(Index(0), ((DefCallExpression) arguments.get(0)).getExpression());
    assertEquals(classDef.getPrivateField("A").getPrivateField("foo"), ((DefCallExpression) arguments.get(0)).getDefinition());

    assertTrue(arguments.get(1) instanceof DefCallExpression);
    assertEquals(Index(0), ((DefCallExpression) arguments.get(1)).getExpression());
    assertEquals(classDef.getPrivateField("A").getPrivateField("foo"), ((DefCallExpression) arguments.get(1)).getDefinition());

    assertTrue(arguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) arguments.get(2)).getBody() instanceof PiExpression);
    List<Expression> domArguments = new ArrayList<>(3);
    Expression domFunction = ((PiExpression) ((LamExpression) arguments.get(2)).getBody()).getArguments().get(0).getType().getFunction(domArguments);
    assertEquals(3, domArguments.size());
    assertEquals(DefCall(Prelude.PATH), domFunction);

    assertTrue(domArguments.get(0) instanceof DefCallExpression);
    assertEquals(Prelude.ZERO, ((DefCallExpression) domArguments.get(0)).getDefinition());

    assertTrue(domArguments.get(1) instanceof DefCallExpression);
    assertEquals(Index(1), ((DefCallExpression) domArguments.get(1)).getExpression());
    assertEquals(classDef.getPrivateField("A").getPrivateField("x"), ((DefCallExpression) domArguments.get(1)).getDefinition());

    assertTrue(domArguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) domArguments.get(2)).getBody() instanceof DefCallExpression);
    assertEquals(Prelude.NAT, ((DefCallExpression) ((LamExpression) domArguments.get(2)).getBody()).getDefinition());
  }

  @Test
  public void recordConstructorsParametersTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition classDef = parseDefs(moduleLoader,
      "\\class A {\n" +
        "\\function x : Nat\n" +
        "\\data Foo (p : x = x) | foo (p = p)\n" +
        "\\function y : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))\n" +
      "}\n" +
      "\\function test (q : A) => q.y");
    Expression resultType = ((FunctionDefinition) classDef.getPrivateField("test")).getResultType();
    List<Expression> arguments = new ArrayList<>(3);
    Expression function = resultType.normalize(NormalizeVisitor.Mode.WHNF).getFunction(arguments);
    assertEquals(3, arguments.size());
    assertEquals(DefCall(Prelude.PATH), function);

    assertTrue(arguments.get(0) instanceof AppExpression);
    assertTrue(((AppExpression) arguments.get(0)).getFunction() instanceof DefCallExpression);
    assertEquals(Index(0), ((DefCallExpression) ((AppExpression) arguments.get(0)).getFunction()).getExpression());
    assertEquals(classDef.getPrivateField("A").getPrivateField("foo"), ((DefCallExpression) ((AppExpression) arguments.get(0)).getFunction()).getDefinition());
    assertTrue(((AppExpression) arguments.get(0)).getArgument().getExpression() instanceof AppExpression);
    AppExpression appPath00 = (AppExpression) ((AppExpression) arguments.get(0)).getArgument().getExpression();
    assertTrue(appPath00.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath00.getArgument().getExpression()).getBody() instanceof AppExpression);
    AppExpression appPath01 = (AppExpression) ((LamExpression) appPath00.getArgument().getExpression()).getBody();
    assertTrue(appPath01.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath01.getArgument().getExpression()).getBody() instanceof DefCallExpression);
    assertEquals(Index(2), ((DefCallExpression) ((LamExpression) appPath01.getArgument().getExpression()).getBody()).getExpression());
    assertEquals(classDef.getStaticField("A").getPrivateField("x"), ((DefCallExpression) ((LamExpression) appPath01.getArgument().getExpression()).getBody()).getDefinition());

    assertTrue(arguments.get(1) instanceof AppExpression);
    assertTrue(((AppExpression) arguments.get(1)).getFunction() instanceof DefCallExpression);
    assertEquals(Index(0), ((DefCallExpression) ((AppExpression) arguments.get(1)).getFunction()).getExpression());
    assertEquals(classDef.getPrivateField("A").getPrivateField("foo"), ((DefCallExpression) ((AppExpression) arguments.get(1)).getFunction()).getDefinition());
    assertTrue(((AppExpression) arguments.get(1)).getArgument().getExpression() instanceof AppExpression);
    AppExpression appPath10 = (AppExpression) ((AppExpression) arguments.get(1)).getArgument().getExpression();
    assertTrue(appPath10.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath10.getArgument().getExpression()).getBody() instanceof AppExpression);
    AppExpression appPath11 = (AppExpression) ((LamExpression) appPath10.getArgument().getExpression()).getBody();
    assertTrue(appPath11.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath11.getArgument().getExpression()).getBody() instanceof DefCallExpression);
    assertEquals(Index(2), ((DefCallExpression) ((LamExpression) appPath11.getArgument().getExpression()).getBody()).getExpression());
    assertEquals(classDef.getStaticField("A").getPrivateField("x"), ((DefCallExpression) ((LamExpression) appPath11.getArgument().getExpression()).getBody()).getDefinition());

    assertTrue(arguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) arguments.get(2)).getBody() instanceof AppExpression);
    assertEquals(DefCall(classDef.getStaticField("A").getPrivateField("Foo")), ((AppExpression) ((LamExpression) arguments.get(2)).getBody()).getFunction());
    List<Expression> parameterArguments = new ArrayList<>(1);
    Expression parameterFunction = ((AppExpression) ((LamExpression) arguments.get(2)).getBody()).getArgument().getExpression().getFunction(parameterArguments);
    assertEquals(1, parameterArguments.size());
    assertEquals(DefCall(Prelude.PATH_CON), parameterFunction);
    assertTrue(parameterArguments.get(0) instanceof LamExpression);
    assertTrue(((LamExpression) parameterArguments.get(0)).getBody() instanceof DefCallExpression);
    assertEquals(Index(2), ((DefCallExpression) ((LamExpression) parameterArguments.get(0)).getBody()).getExpression());
    assertEquals(classDef.getPrivateField("A").getPrivateField("x"), ((DefCallExpression) ((LamExpression) parameterArguments.get(0)).getBody()).getDefinition());

    List<Expression> parameters = ((DefCallExpression) parameterFunction).getParameters();
    assertEquals(3, parameters.size());

    assertTrue(parameters.get(0) instanceof LamExpression);
    assertEquals(DefCall(Prelude.NAT), ((LamExpression) parameters.get(0)).getBody());

    parameters.set(1, parameters.get(1).normalize(NormalizeVisitor.Mode.WHNF));
    assertTrue(parameters.get(1) instanceof DefCallExpression);
    assertEquals(Index(1), ((DefCallExpression) parameters.get(1)).getExpression());
    assertEquals(classDef.getPrivateField("A").getPrivateField("x"), ((DefCallExpression) parameters.get(1)).getDefinition());

    parameters.set(2, parameters.get(2).normalize(NormalizeVisitor.Mode.WHNF));
    assertTrue(parameters.get(2) instanceof DefCallExpression);
    assertEquals(Index(1), ((DefCallExpression) parameters.get(2)).getExpression());
    assertEquals(classDef.getPrivateField("A").getPrivateField("x"), ((DefCallExpression) parameters.get(2)).getDefinition());
  }
}
