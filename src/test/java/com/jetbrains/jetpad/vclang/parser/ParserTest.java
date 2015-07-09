package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest {
  @Test
  public void parserLam() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression expr = parseExpr(moduleLoader, "\\lam x y z => y");
    assertTrue(compare(Lam("x", Lam("y", Lam("z", Index(1)))), expr));
  }

  @Test
  public void parserLam2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression expr = parseExpr(moduleLoader, "\\lam x y => (\\lam z w => y z) y");
    assertTrue(compare(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Index(2), Index(1)))), Index(0)))), expr));
  }

  @Test
  public void parserLamTele() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression expr = parseExpr(moduleLoader, "\\lam p {x t : Nat} {y} (a : Nat -> Nat) => (\\lam (z w : Nat) => y z) y");
    assertTrue(compare(Lam(lamArgs(Name("p"), Tele(false, vars("x", "t"), DefCall(Prelude.NAT)), Name(false, "y"), Tele(vars("a"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Apps(Lam(lamArgs(Tele(vars("z", "w"), DefCall(Prelude.NAT))), Apps(Index(3), Index(1))), Index(1))), expr));
  }

  @Test
  public void parserPi() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression expr = parseExpr(moduleLoader, "\\Pi (x y z : Nat) (w t : Nat -> Nat) -> \\Pi (a b : \\Pi (c : Nat) -> Nat c) -> Nat b y w");
    assertTrue(compare(Pi(args(Tele(vars("x", "y", "z"), DefCall(Prelude.NAT)), Tele(vars("w", "t"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Pi(args(Tele(vars("a", "b"), Pi("c", DefCall(Prelude.NAT), Apps(DefCall(Prelude.NAT), Index(0))))), Apps(DefCall(Prelude.NAT), Index(0), Index(5), Index(3)))), expr));
  }

  @Test
  public void parserPi2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression expr = parseExpr(moduleLoader, "\\Pi (x y : Nat) (z : Nat x -> Nat y) -> Nat z y x");
    assertTrue(compare(Pi(args(Tele(vars("x", "y"), DefCall(Prelude.NAT)), Tele(vars("z"), Pi(Apps(DefCall(Prelude.NAT), Index(1)), Apps(DefCall(Prelude.NAT), Index(0))))), Apps(DefCall(Prelude.NAT), Index(0), Index(1), Index(2))), expr));
  }

  @Test
  public void parserLamOpenError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression result = new BuildVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader).visitExpr(parse(moduleLoader, "\\lam x => (\\Pi (y : Nat) -> (\\lam y => y)) y").expr());
    assertEquals(1, moduleLoader.getErrors().size());
    assertNull(result);
  }

  @Test
  public void parserPiOpenError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression result = new BuildVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader).visitExpr(parse(moduleLoader, "\\Pi (a b : Nat a) -> Nat a b").expr());
    assertEquals(1, moduleLoader.getErrors().size());
    assertNull(result);
  }

  @Test
  public void parserDef() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition result = parseDefs(moduleLoader,
        "\\function x : Nat => zero\n" +
            "\\function y : Nat => x");
    assertNotNull(result.getChildren());
    assertEquals(2, result.getChildren().size());
  }

  @Test
  public void parserDefType() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition result = parseDefs(moduleLoader,
        "\\function x : \\Type0 => Nat\n" +
            "\\function y : x => zero");
    assertNotNull(result.getChildren());
    assertEquals(2, result.getChildren().size());
  }

  @Test
  public void parserImplicit() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    FunctionDefinition def = (FunctionDefinition) parseDef(moduleLoader, "\\function f (x y : Nat) {z w : Nat} (t : Nat) {r : Nat} (A : Nat -> Nat -> Nat -> Nat -> Nat -> Nat -> \\Type0) : A x y z w t r");
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
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    FunctionDefinition def = (FunctionDefinition) parseDef(moduleLoader, "\\function f {x : Nat} (_ : Nat) {y z : Nat} (A : Nat -> Nat -> Nat -> \\Type0) (_ : A x y z) : Nat");
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
    Definition plus = new FunctionDefinition("+", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition("*", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 7), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);

    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.rootModule().addField(plus, null);
    moduleLoader.rootModule().addField(mul, null);
    CheckTypeVisitor.Result result = parseExpr(moduleLoader, "0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)").accept(new CheckTypeVisitor(null, new ArrayList<Binding>(), null, moduleLoader, CheckTypeVisitor.Side.RHS), null);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertTrue(result instanceof CheckTypeVisitor.OKResult);
    assertTrue(compare(BinOp(BinOp(Zero(), plus, BinOp(Suc(Zero()), mul, Suc(Suc(Zero())))), plus, BinOp(BinOp(Suc(Suc(Suc(Zero()))), mul, BinOp(Suc(Suc(Suc(Suc(Zero())))), mul, Suc(Suc(Suc(Suc(Suc(Zero()))))))), mul, BinOp(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), plus, Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))))))), result.expression));
  }

  @Test
  public void parserInfixDef() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition result = parseDefs(moduleLoader,
        "\\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
            "\\function (*) : Nat -> Nat => \\lam x => x + zero");
    assertNotNull(result.getChildren());
    assertEquals(2, result.getChildren().size());
  }

  @Test
  public void parserInfixError() {
    List<Argument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Definition plus = new FunctionDefinition("+", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition("*", null, new Definition.Precedence(Definition.Associativity.RIGHT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);

    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.rootModule().addField(plus, null);
    moduleLoader.rootModule().addField(mul, null);
    new BuildVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader).visitExpr(parse(moduleLoader, "11 + 2 * 3").expr()).accept(new CheckTypeVisitor(null, new ArrayList<Binding>(), null, moduleLoader, CheckTypeVisitor.Side.RHS), null);
    assertEquals(1, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void parserError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text = "A { \\function f (x : Nat) <= elim x | zero => zero | suc x' => zero }";
    new BuildVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader).visitExpr(parse(moduleLoader, text).expr());
    assertTrue(moduleLoader.getErrors().size() > 0);
  }
}
