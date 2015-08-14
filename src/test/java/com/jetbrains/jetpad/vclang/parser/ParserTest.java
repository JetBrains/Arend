package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest {
  ModuleLoader dummyModuleLoader;
  @Before
  public void initialize() {
    dummyModuleLoader = new ModuleLoader();
    dummyModuleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
  }

  @Test
  public void parserLetToTheRight() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\lam x => \\let | x => Nat \\in x x");
    Concrete.Expression expr1 = parseExpr(new ModuleLoader(), "\\let | x => Nat \\in \\lam x => x x");
    assertTrue(compare(Lam("x", Let(lets(let("x", lamArgs(), Nat())), Apps(Var("x"), Var("x")))), expr));
    assertTrue(compare(Let(lets(let("x", lamArgs(), Nat())), Lam("x", Apps(Var("x"), Var("x")))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\let | x => Nat | y => x \\in y");
    assertTrue(compare(Let(lets(let("x", Nat()), let("y", Var("x"))), Var("y")), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\let | x : Nat => zero \\in x");
    assertTrue(compare(Let(lets(let("x", lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, Zero())), Var("x")), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression expr = parseExpr(dummyModuleLoader, "\\lam x y z => y");
    assertTrue(compare(Lam("x", Lam("y", Lam("z", Var("y")))), expr));
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = parseExpr(dummyModuleLoader, "\\lam x y => (\\lam z w => y z) y");
    assertTrue(compare(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Var("y"), Var("z")))), Var("y")))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = parseExpr(dummyModuleLoader, "\\lam p {x t : Nat} {y} (a : Nat -> Nat) => (\\lam (z w : Nat) => y z) y");
    assertTrue(compare(Lam(lamArgs(Name("p"), Tele(false, vars("x", "t"), DefCall(Prelude.NAT)), Name(false, "y"), Tele(vars("a"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Apps(Lam(lamArgs(Tele(vars("z", "w"), DefCall(Prelude.NAT))), Apps(Var("y"), Var("z"))), Var("y"))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = parseExpr(dummyModuleLoader, "\\Pi (x y z : Nat) (w t : Nat -> Nat) -> \\Pi (a b : \\Pi (c : Nat) -> Nat c) -> Nat b y w");
    assertTrue(compare(Pi(args(Tele(vars("x", "y", "z"), DefCall(Prelude.NAT)), Tele(vars("w", "t"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Pi(args(Tele(vars("a", "b"), Pi("c", DefCall(Prelude.NAT), Apps(DefCall(Prelude.NAT), Var("c"))))), Apps(DefCall(Prelude.NAT), Var("b"), Var("y"), Var("w")))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = parseExpr(dummyModuleLoader, "\\Pi (x y : Nat) (z : Nat x -> Nat y) -> Nat z y x");
    assertTrue(compare(Pi(args(Tele(vars("x", "y"), DefCall(Prelude.NAT)), Tele(vars("z"), Pi(Apps(DefCall(Prelude.NAT), Var("x")), Apps(DefCall(Prelude.NAT), Var("y"))))), Apps(DefCall(Prelude.NAT), Var("z"), Var("y"), Var("x"))), expr));
  }

  @Test
  public void parserLamOpenError() {
    Concrete.Expression result = parseExpr(dummyModuleLoader, "\\lam x => (\\Pi (y : Nat) -> (\\lam y => y)) y", 1);
    assertNull(result);
  }

  @Test
  public void parserPiOpenError() {
    Concrete.Expression result = parseExpr(dummyModuleLoader, "\\Pi (a b : Nat a) -> Nat a b", 1);
    assertNull(result);
  }

  @Test
  public void parserDef() {
    ClassDefinition result = parseDefs(dummyModuleLoader,
      "\\static \\function x : Nat => zero\n" +
      "\\static \\function y : Nat => x");
    assertEquals(2, result.getNamespace().getMembers().size());
  }

  @Test
  public void parserDefType() {
    ClassDefinition result = parseDefs(dummyModuleLoader,
      "\\static \\function x : \\Type0 => Nat\n" +
      "\\static \\function y : x => zero");
    assertEquals(2, result.getNamespace().getMembers().size());
  }

  @Test
  public void parserImplicit() {
    FunctionDefinition def = (FunctionDefinition) parseDef(dummyModuleLoader, "\\function f (x y : Nat) {z w : Nat} (t : Nat) {r : Nat} (A : Nat -> Nat -> Nat -> Nat -> Nat -> Nat -> \\Type0) : A x y z w t r");
    assertEquals(5, def.getArguments().size());
    assertTrue(def.getArguments().get(0).getExplicit());
    assertFalse(def.getArguments().get(1).getExplicit());
    assertTrue(def.getArguments().get(2).getExplicit());
    assertFalse(def.getArguments().get(3).getExplicit());
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(0)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(1)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(2)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(3)).getType()));
    assertTrue(compare(Apps(Index(0), Index(6), Index(5), Index(4), Index(3), Index(2), Index(1)), def.getResultType()));
  }

  @Test
  public void parserImplicit2() {
    FunctionDefinition def = (FunctionDefinition) parseDef(dummyModuleLoader, "\\function f {x : Nat} (_ : Nat) {y z : Nat} (A : Nat -> Nat -> Nat -> \\Type0) (_ : A x y z) : Nat");
    assertEquals(5, def.getArguments().size());
    assertFalse(def.getArguments().get(0).getExplicit());
    assertTrue(def.getArguments().get(1).getExplicit());
    assertFalse(def.getArguments().get(2).getExplicit());
    assertTrue(def.getArguments().get(3).getExplicit());
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(0)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(1)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(2)).getType()));
    assertTrue(compare(Apps(Index(0), Index(4), Index(2), Index(1)), ((TypeArgument) def.getArguments().get(4)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getResultType()));
  }

  @Test
  public void parserInfix() {
    List<Argument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Definition plus = new FunctionDefinition(new Namespace(new Utils.Name("+", Abstract.Definition.Fixity.INFIX), null), new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition(new Namespace(new Utils.Name("*", Abstract.Definition.Fixity.INFIX), null), new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 7), arguments, Nat(), Definition.Arrow.LEFT, null);

    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.getRoot().addMember(plus);
    moduleLoader.getRoot().addMember(mul);
    CheckTypeVisitor.Result result = parseExpr(moduleLoader, "0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)").accept(new CheckTypeVisitor(null, new ArrayList<Binding>(), moduleLoader, CheckTypeVisitor.Side.RHS), null);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertTrue(result instanceof CheckTypeVisitor.OKResult);
    assertTrue(compare(BinOp(BinOp(Zero(), plus, BinOp(Suc(Zero()), mul, Suc(Suc(Zero())))), plus, BinOp(BinOp(Suc(Suc(Suc(Zero()))), mul, BinOp(Suc(Suc(Suc(Suc(Zero())))), mul, Suc(Suc(Suc(Suc(Suc(Zero()))))))), mul, BinOp(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), plus, Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))))))), result.expression));
  }

  @Test
  public void parserInfixDef() {
    ClassDefinition result = parseDefs(dummyModuleLoader,
      "\\static \\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
      "\\static \\function (*) : Nat -> Nat => \\lam x => x + zero");
    assertEquals(2, result.getNamespace().getMembers().size());
  }

  @Test
  public void parserInfixError() {
    List<Argument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Definition plus = new FunctionDefinition(new Namespace(new Utils.Name("+", Abstract.Definition.Fixity.INFIX), null), new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition(new Namespace(new Utils.Name("*", Abstract.Definition.Fixity.INFIX), null), new Definition.Precedence(Definition.Associativity.RIGHT_ASSOC, (byte) 6), arguments, Nat(), Definition.Arrow.LEFT, null);

    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.getRoot().addMember(plus);
    moduleLoader.getRoot().addMember(mul);
    parseExpr(moduleLoader, "11 + 2 * 3", 1).accept(new CheckTypeVisitor(null, new ArrayList<Binding>(), moduleLoader, CheckTypeVisitor.Side.RHS), null);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void parserError() {
    String text = "A { \\function f (x : Nat) <= elim x | zero => zero | suc x' => zero }";
    Namespace namespace = dummyModuleLoader.getRoot().getChild(new Utils.Name("test"));
    new BuildVisitor(namespace, new ClassDefinition(namespace), dummyModuleLoader, false).visitExpr(parse(dummyModuleLoader, text).expr());
    assertTrue(dummyModuleLoader.getErrors().size() > 0);
  }

  @Test
  public void parserCase() {
    parseExpr(dummyModuleLoader, "\\case 2 | zero => zero | suc x' => x'");
  }

  @Test
  public void whereTest() {
    parseDefs(dummyModuleLoader,
        "\\static \\function f (x : Nat) => B.b (a x) \\where\n" +
          "\\static \\function a (x : Nat) => x\n" +
          "\\static \\data D | D1 | D2\n" +
          "\\static \\class B { \\static \\data C | cr \\static \\function b (x : Nat) => D1 }");
  }

  @Test
  public void whereTestDefCmd() {
    parseDefs(dummyModuleLoader, "\\static \\function f (x : Nat) => a \\where \\static \\class A { \\static \\function a => 0 } \\open A");
  }

  @Test
  public void whereError() {
    parseDefs(dummyModuleLoader, "\\static \\function f (x : Nat) => x \\where \\static \\function b => x", 1, 0);
  }

  @Test
  public void whereClosedError() {
    parseDefs(dummyModuleLoader, "\\static \\function f => x \\where \\static \\class A { \\static \\function x => 0 } \\open A \\close A", 1, 0);
  }

  @Test
  public void whereOpenFunction() {
    parseDefs(dummyModuleLoader, "\\static \\function f => x \\where \\static \\function b => 0 \\where \\static \\function x => 0; \\open b(x)");
  }

  @Test
  public void whereNoOpenFunctionError() {
    parseDefs(dummyModuleLoader, "\\static \\function f => x \\where \\static \\function b => 0 \\where \\static \\function x => 0;", 1, 0);
  }

  @Test
  public void whereNested() {
    parseDefs(dummyModuleLoader, "\\static \\function f => x \\where \\static \\data B | b \\static \\function x => a \\where \\static \\function a => b");
  }

  @Test
  public void whereOuterScope() {
    parseDefs(dummyModuleLoader, "\\static \\function f => 0 \\where \\static \\function g => 0 \\static \\function h => g");
  }

  @Test
  public void whereInSignature() {
    parseDefs(dummyModuleLoader, "\\static \\function f : D => d \\where \\static \\data D | d");
  }

  @Test
  public void whereAccessOuter() {
    parseDefs(dummyModuleLoader, "\\static \\function f => 0 \\where \\static \\function x => 0; \\static \\function g => f.x");
  }

  @Test
  public void whereNonStaticOpen() {
    parseDefs(dummyModuleLoader, "\\static \\function f => 0 \\where \\static \\function x => 0 \\static \\function y => x; \\static \\function g => 0 \\where \\open f(y)");
  }

  @Test
  public void whereAbstractError() {
    parseDefs(dummyModuleLoader, "\\static \\function f => 0 \\where \\function x : Nat", 1, 0);
  }
}
