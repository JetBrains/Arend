package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RecursiveTest {
  @Test
  public void list() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition definition = parseDef(moduleLoader, "\\data List (A : \\Type0) | nil | cons A (List A)");
    assertFalse(definition.hasErrors());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void dataLeftError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition definition = parseDef(moduleLoader, "\\data List (A : \\Type0) | nil | cons (List A -> A)");
    assertFalse(definition.hasErrors());
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void dataRightError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition definition = parseDef(moduleLoader, "\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))");
    assertFalse(definition.hasErrors());
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void plus() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => suc (x' + y)");
    assertFalse(definition.hasErrors());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void doubleRec() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => suc x'' + (x'' + y)");
    assertFalse(definition.hasErrors());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void functionError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= x + y");
    assertFalse(definition.hasErrors());
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void functionError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => y + y");
    assertFalse(definition.hasErrors());
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void functionPartiallyApplied() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition definition = parseDef(moduleLoader, "\\function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => z (foo z x')");
    assertFalse(definition.hasErrors());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }
}
