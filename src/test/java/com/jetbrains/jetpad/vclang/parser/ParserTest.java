package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
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
    Concrete.Expression expr = parseExpr("\\lam x y z => y");
    assertTrue(compare(Lam("x", Lam("y", Lam("z", Index(1)))), expr));
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = parseExpr("\\lam x y => (\\lam z w => y z) y");
    assertTrue(compare(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Index(2), Index(1)))), Index(0)))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = parseExpr("\\lam p {x t : Nat} {y} (a : Nat -> Nat) => (\\lam (z w : Nat) => y z) y");
    assertTrue(compare(Lam(lamArgs(Name("p"), Tele(false, vars("x", "t"), DefCall(Prelude.NAT)), Name(false, "y"), Tele(vars("a"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Apps(Lam(lamArgs(Tele(vars("z", "w"), DefCall(Prelude.NAT))), Apps(Index(3), Index(1))), Index(1))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = parseExpr("\\Pi (x y z : Nat) (w t : Nat -> Nat) -> \\Pi (a b : \\Pi (c : Nat) -> Nat c) -> Nat b y w");
    assertTrue(compare(Pi(args(Tele(vars("x", "y", "z"), DefCall(Prelude.NAT)), Tele(vars("w", "t"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Pi(args(Tele(vars("a", "b"), Pi("c", DefCall(Prelude.NAT), Apps(DefCall(Prelude.NAT), Index(0))))), Apps(DefCall(Prelude.NAT), Index(0), Index(5), Index(3)))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = parseExpr("\\Pi (x y : Nat) (z : Nat x -> Nat y) -> Nat z y x");
    assertTrue(compare(Pi(args(Tele(vars("x", "y"), DefCall(Prelude.NAT)), Tele(vars("z"), Pi(Apps(DefCall(Prelude.NAT), Index(1)), Apps(DefCall(Prelude.NAT), Index(0))))), Apps(DefCall(Prelude.NAT), Index(0), Index(1), Index(2))), expr));
  }

  @Test
  public void parserLamOpenError() {
    List<VcError> errors = new ArrayList<>();
    ClassDefinition root = new ClassDefinition("\\root", null, new ArrayList<Definition>());
    Concrete.Expression result = new BuildVisitor(root, new ArrayList<ModuleLoader.TypeCheckingUnit>(), errors).visitExpr(parse("\\lam x => (\\Pi (y : Nat) -> (\\lam y => y)) y", errors).expr());
    assertEquals(1, errors.size());
    assertNull(result);
  }

  @Test
  public void parserPiOpenError() {
    List<VcError> errors = new ArrayList<>();
    ClassDefinition root = new ClassDefinition("\\root", null, new ArrayList<Definition>());
    Concrete.Expression result = new BuildVisitor(root, new ArrayList<ModuleLoader.TypeCheckingUnit>(), errors).visitExpr(parse("\\Pi (a b : Nat a) -> Nat a b", errors).expr());
    assertEquals(1, errors.size());
    assertNull(result);
  }

  @Test
  public void parserDef() {
    List<Concrete.Definition> defs = parseDefs(
        "\\function x : Nat => zero\n" +
            "\\function y : Nat => x");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserDefType() {
    List<Concrete.Definition> defs = parseDefs(
        "\\function x : \\Type0 => Nat\n" +
            "\\function y : x => zero");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserImplicit() {
    Concrete.FunctionDefinition def = (Concrete.FunctionDefinition) parseDef("\\function f (x y : Nat) {z w : Nat} (t : Nat) {r : Nat} : Nat x y z w t r => Nat").rawDefinition;
    assertEquals(4, def.getArguments().size());
    assertTrue(def.getArguments().get(0).getExplicit());
    assertFalse(def.getArguments().get(1).getExplicit());
    assertTrue(def.getArguments().get(2).getExplicit());
    assertFalse(def.getArguments().get(3).getExplicit());
    assertTrue(compare(DefCall(Prelude.NAT), def.getArguments().get(0).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getArguments().get(1).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getArguments().get(2).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getArguments().get(3).getType()));
    assertTrue(compare(Apps(DefCall(Prelude.NAT), Index(5), Index(4), Index(3), Index(2), Index(1), Index(0)), def.getResultType()));
  }

  @Test
  public void parserImplicit2() {
    Concrete.FunctionDefinition def = (Concrete.FunctionDefinition) parseDef("\\function f {x : Nat} (_ : Nat) {y z : Nat} (_ : Nat x y z) : Nat => Nat").rawDefinition;
    assertEquals(4, def.getArguments().size());
    assertFalse(def.getArguments().get(0).getExplicit());
    assertTrue(def.getArguments().get(1).getExplicit());
    assertFalse(def.getArguments().get(2).getExplicit());
    assertTrue(def.getArguments().get(3).getExplicit());
    assertTrue(compare(DefCall(Prelude.NAT), def.getArguments().get(0).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getArguments().get(1).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getArguments().get(2).getType()));
    assertTrue(compare(Apps(DefCall(Prelude.NAT), Index(3), Index(1), Index(0)), def.getArguments().get(3).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getResultType()));
  }

  @Test
  public void parserInfix() {
    List<TelescopeArgument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Definition plus = new FunctionDefinition("+", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition("*", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 7), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);
    List<Definition> definitions = new ArrayList<>(2);
    definitions.add(plus);
    definitions.add(mul);
    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.Result result = parseExpr(definitions, "0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)").accept(new CheckTypeVisitor(new ArrayList<Binding>(), errors, CheckTypeVisitor.Side.RHS), null);
    assertEquals(0, errors.size());
    assertTrue(result instanceof CheckTypeVisitor.OKResult);
    assertTrue(compare(BinOp(BinOp(Zero(), plus, BinOp(Suc(Zero()), mul, Suc(Suc(Zero())))), plus, BinOp(BinOp(Suc(Suc(Suc(Zero()))), mul, BinOp(Suc(Suc(Suc(Suc(Zero())))), mul, Suc(Suc(Suc(Suc(Suc(Zero()))))))), mul, BinOp(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), plus, Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))))))), result.expression));
  }

  @Test
  public void parserInfixDef() {
    List<Concrete.Definition> defs = parseDefs(
        "\\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
            "\\function (*) : Nat -> Nat => \\lam x => x + zero");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserInfixError() {
    List<TelescopeArgument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Definition plus = new FunctionDefinition("+", null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition("*", null, new Definition.Precedence(Definition.Associativity.RIGHT_ASSOC, (byte) 6), Definition.Fixity.INFIX, arguments, Nat(), Definition.Arrow.LEFT, null);
    List<Definition> definitions = new ArrayList<>(2);
    definitions.add(plus);
    definitions.add(mul);

    List<VcError> vcErrors = new ArrayList<>();
    List<TypeCheckingError> errors = new ArrayList<>();
    ClassDefinition root = new ClassDefinition("\\root", null, definitions);
    new BuildVisitor(root, new ArrayList<ModuleLoader.TypeCheckingUnit>(), vcErrors).visitExpr(parse("11 + 2 * 3", vcErrors).expr()).accept(new CheckTypeVisitor(new ArrayList<Binding>(), errors, CheckTypeVisitor.Side.RHS), null);
    assertEquals(1, vcErrors.size());
    assertEquals(0, errors.size());
  }
}
