package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest {
  @Test
  public void parserLetToTheRight() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\lam x => \\let | x => Nat \\in x x");
    Concrete.Expression expr1 = parseExpr(new ModuleLoader(), "\\let | x => Nat \\in \\lam x => x x");
    assertTrue(compare(Lam("x", Let(lets(let("x", lamArgs(), Nat())), Apps(Index(0), Index(0)))), expr));
    assertTrue(compare(Let(lets(let("x", lamArgs(), Nat())), Lam("x", Apps(Index(0), Index(0)))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\let | x => Nat | y => x \\in y");
    assertTrue(compare(Let(lets(let("x", Nat()), let("y", Index(0))), Index(0)), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\let | x : Nat => zero \\in x");
    assertTrue(compare(Let(lets(let("x", lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, Zero())), Index(0)), expr));
  }

  @Test
  public void parseLetElim() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\let | x (y : Nat) <= \\elim y | zero => zero | zero => zero \\in zero");
    List<Clause> clauses = new ArrayList<>();
    ElimExpression elim = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), clauses, null);
    clauses.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Zero(), elim));
    clauses.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Zero(), elim));
    assertTrue(compare(Let(lets(let("x", lamArgs(Tele(vars("y"), Nat())), Abstract.Definition.Arrow.LEFT, elim)), Zero()), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\lam x y z => y");
    assertTrue(compare(Lam("x", Lam("y", Lam("z", Index(1)))), expr));
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\lam x y => (\\lam z w => y z) y");
    assertTrue(compare(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Index(2), Index(1)))), Index(0)))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\lam p {x t : Nat} {y} (a : Nat -> Nat) => (\\lam (z w : Nat) => y z) y");
    assertTrue(compare(Lam(lamArgs(Name("p"), Tele(false, vars("x", "t"), DefCall(Prelude.NAT)), Name(false, "y"), Tele(vars("a"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Apps(Lam(lamArgs(Tele(vars("z", "w"), DefCall(Prelude.NAT))), Apps(Index(3), Index(1))), Index(1))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\Pi (x y z : Nat) (w t : Nat -> Nat) -> \\Pi (a b : \\Pi (c : Nat) -> Nat c) -> Nat b y w");
    assertTrue(compare(Pi(args(Tele(vars("x", "y", "z"), DefCall(Prelude.NAT)), Tele(vars("w", "t"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Pi(args(Tele(vars("a", "b"), Pi("c", DefCall(Prelude.NAT), Apps(DefCall(Prelude.NAT), Index(0))))), Apps(DefCall(Prelude.NAT), Index(0), Index(5), Index(3)))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = parseExpr(new ModuleLoader(), "\\Pi (x y : Nat) (z : Nat x -> Nat y) -> Nat z y x");
    assertTrue(compare(Pi(args(Tele(vars("x", "y"), DefCall(Prelude.NAT)), Tele(vars("z"), Pi(Apps(DefCall(Prelude.NAT), Index(1)), Apps(DefCall(Prelude.NAT), Index(0))))), Apps(DefCall(Prelude.NAT), Index(0), Index(1), Index(2))), expr));
  }

  @Test
  public void parserLamOpenError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression result = new BuildVisitor(new Module(moduleLoader.rootModule(), "test"), moduleLoader.rootModule(), moduleLoader).visitExpr(parse(moduleLoader, "\\lam x => (\\Pi (y : Nat) -> (\\lam y => y)) y").expr());
    assertEquals(1, moduleLoader.getErrors().size());
    assertNull(result);
  }

  @Test
  public void parserPiOpenError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Expression result = new BuildVisitor(new Module(moduleLoader.rootModule(), "test"), moduleLoader.rootModule(), moduleLoader).visitExpr(parse(moduleLoader, "\\Pi (a b : Nat a) -> Nat a b").expr());
    assertEquals(1, moduleLoader.getErrors().size());
    assertNull(result);
  }

  @Test
  public void parserDef() {
    List<?> defs = parseDefs(new ModuleLoader(),
        "\\function x : Nat => zero\n" +
            "\\function y : Nat => x");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserDefType() {
    List<?> defs = parseDefs(new ModuleLoader(),
        "\\function x : \\Type0 => Nat\n" +
            "\\function y : x => zero");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserImplicit() {
    Concrete.FunctionDefinition def = (Concrete.FunctionDefinition) parseDef(new ModuleLoader(), "\\function f (x y : Nat) {z w : Nat} (t : Nat) {r : Nat} : Nat x y z w t r => Nat").rawDefinition;
    assertEquals(4, def.getArguments().size());
    assertTrue(def.getArguments().get(0).getExplicit());
    assertFalse(def.getArguments().get(1).getExplicit());
    assertTrue(def.getArguments().get(2).getExplicit());
    assertFalse(def.getArguments().get(3).getExplicit());
    assertTrue(compare(DefCall(Prelude.NAT), ((Concrete.TypeArgument) def.getArguments().get(0)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((Concrete.TypeArgument) def.getArguments().get(1)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((Concrete.TypeArgument) def.getArguments().get(2)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((Concrete.TypeArgument) def.getArguments().get(3)).getType()));
    assertTrue(compare(Apps(DefCall(Prelude.NAT), Index(5), Index(4), Index(3), Index(2), Index(1), Index(0)), def.getResultType()));
  }

  @Test
  public void parserImplicit2() {
    Concrete.FunctionDefinition def = (Concrete.FunctionDefinition) parseDef(new ModuleLoader(), "\\function f {x : Nat} (_ : Nat) {y z : Nat} (_ : Nat x y z) : Nat => Nat").rawDefinition;
    assertEquals(4, def.getArguments().size());
    assertFalse(def.getArguments().get(0).getExplicit());
    assertTrue(def.getArguments().get(1).getExplicit());
    assertFalse(def.getArguments().get(2).getExplicit());
    assertTrue(def.getArguments().get(3).getExplicit());
    assertTrue(compare(DefCall(Prelude.NAT), ((Concrete.TypeArgument) def.getArguments().get(0)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((Concrete.TypeArgument) def.getArguments().get(1)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((Concrete.TypeArgument) def.getArguments().get(2)).getType()));
    assertTrue(compare(Apps(DefCall(Prelude.NAT), Index(3), Index(1), Index(0)), ((Concrete.TypeArgument) def.getArguments().get(3)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getResultType()));
  }

  @Test
  public void parserInfix() {
    List<Argument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Definition plus = new FunctionDefinition("+", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition("*", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 7), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);

    List<TypeCheckingError> errors = new ArrayList<>();
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.rootModule().add(plus, null);
    moduleLoader.rootModule().add(mul, null);
    CheckTypeVisitor.Result result = parseExpr(moduleLoader, "0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)").accept(new CheckTypeVisitor(null, new ArrayList<Binding>(), null, errors, CheckTypeVisitor.Side.RHS), null);
    assertEquals(0, errors.size());
    assertTrue(result instanceof CheckTypeVisitor.OKResult);
    assertTrue(compare(BinOp(BinOp(Zero(), plus, BinOp(Suc(Zero()), mul, Suc(Suc(Zero())))), plus, BinOp(BinOp(Suc(Suc(Suc(Zero()))), mul, BinOp(Suc(Suc(Suc(Suc(Zero())))), mul, Suc(Suc(Suc(Suc(Suc(Zero()))))))), mul, BinOp(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), plus, Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))))))), result.expression));
  }

  @Test
  public void parserInfixDef() {
    List<?> defs = parseDefs(new ModuleLoader(),
        "\\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
            "\\function (*) : Nat -> Nat => \\lam x => x + zero");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserInfixError() {
    List<Argument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Definition plus = new FunctionDefinition("+", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition("*", null, new Definition.Precedence(Definition.Associativity.RIGHT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);

    List<TypeCheckingError> errors = new ArrayList<>();
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.rootModule().add(plus, null);
    moduleLoader.rootModule().add(mul, null);
    new BuildVisitor(new Module(moduleLoader.rootModule(), "test"), moduleLoader.rootModule(), moduleLoader).visitExpr(parse(moduleLoader, "11 + 2 * 3").expr()).accept(new CheckTypeVisitor(null, new ArrayList<Binding>(), null, errors, CheckTypeVisitor.Side.RHS), null);
    assertEquals(1, moduleLoader.getErrors().size());
    assertEquals(0, errors.size());
  }

  @Test
  public void parserError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text = "A { \\function f (x : Nat) <= elim x | zero => zero | suc x' => zero }";
    new BuildVisitor(new Module(moduleLoader.rootModule(), "test"), moduleLoader.rootModule(), moduleLoader).visitExpr(parse(moduleLoader, text).expr());
    assertTrue(moduleLoader.getErrors().size() > 0);
  }
}
