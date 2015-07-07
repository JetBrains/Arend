package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySource;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static org.junit.Assert.*;

public class RecursiveTest {
  @Test
  public void list() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Definition definition = parseDef(moduleLoader, "\\data List (A : \\Type0) | nil | cons A (List A)");
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader), new ArrayList<Binding>()).hasErrors());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void dataLeftError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Definition definition = parseDef(moduleLoader, "\\data List (A : \\Type0) | nil | cons (List A -> A)");
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader), new ArrayList<Binding>()).hasErrors());
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void dataRightError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Definition definition = parseDef(moduleLoader, "\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))");
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader), new ArrayList<Binding>()).hasErrors());
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void plus() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => suc (x' + y)");
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader), new ArrayList<Binding>()).hasErrors());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void doubleRec() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => suc x'' + (x'' + y)");
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader), new ArrayList<Binding>()).hasErrors());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void functionError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= x + y");
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader), new ArrayList<Binding>()).hasErrors());
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void functionError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => y + y");
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader), new ArrayList<Binding>()).hasErrors());
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void functionPartiallyApplied() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Concrete.Definition definition = parseDef(moduleLoader, "\\function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => z (foo z x')");
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader), new ArrayList<Binding>()).hasErrors());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }
}
