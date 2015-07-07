package com.jetbrains.jetpad.vclang.typechecking;

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
    Concrete.Definition definition = parseDef(moduleLoader, "\\data List (A : \\Type0) | nil | cons A (List A)");
    List<TypeCheckingError> errors = new ArrayList<>();
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), errors), new ArrayList<Binding>()).hasErrors());
    assertEquals(0, errors.size());
  }

  @Test
  public void dataLeftError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\data List (A : \\Type0) | nil | cons (List A -> A)");
    List<TypeCheckingError> errors = new ArrayList<>();
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), errors), new ArrayList<Binding>()).hasErrors());
    assertEquals(1, errors.size());
  }

  @Test
  public void dataRightError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))");
    List<TypeCheckingError> errors = new ArrayList<>();
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), errors), new ArrayList<Binding>()).hasErrors());
    assertEquals(1, errors.size());
  }

  @Test
  public void plus() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => suc (x' + y)");
    List<TypeCheckingError> errors = new ArrayList<>();
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), errors), new ArrayList<Binding>()).hasErrors());
    assertEquals(0, errors.size());
  }

  @Test
  public void doubleRec() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => suc x'' + (x'' + y)");
    List<TypeCheckingError> errors = new ArrayList<>();
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), errors), new ArrayList<Binding>()).hasErrors());
    assertEquals(0, errors.size());
  }

  @Test
  public void functionError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= x + y");
    List<TypeCheckingError> errors = new ArrayList<>();
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), errors), new ArrayList<Binding>()).hasErrors());
    assertEquals(1, errors.size());
  }

  @Test
  public void functionError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => y + y");
    List<TypeCheckingError> errors = new ArrayList<>();
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), errors), new ArrayList<Binding>()).hasErrors());
    assertEquals(1, errors.size());
  }

  @Test
  public void functionPartiallyApplied() {
    ModuleLoader moduleLoader = new ModuleLoader();
    Concrete.Definition definition = parseDef(moduleLoader, "\\function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => z (foo z x')");
    List<TypeCheckingError> errors = new ArrayList<>();
    assertFalse(definition.accept(new DefinitionCheckTypeVisitor(new ClassDefinition("test", moduleLoader.rootModule()), errors), new ArrayList<Binding>()).hasErrors());
    assertEquals(0, errors.size());
  }
}
