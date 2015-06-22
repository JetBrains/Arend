package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static org.junit.Assert.*;

public class Recursive {
  @Test
  public void list() {
    ModuleLoader.TypeCheckingUnit unit = parseDef(new ModuleLoader(), "\\data List (A : \\Type0) | nil | cons A (List A)");
    List<TypeCheckingError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void dataLeftError() {
    ModuleLoader.TypeCheckingUnit unit = parseDef(new ModuleLoader(), "\\data List (A : \\Type0) | nil | cons (List A -> A)");
    List<TypeCheckingError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void dataRightError() {
    ModuleLoader.TypeCheckingUnit unit = parseDef(new ModuleLoader(), "\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))");
    List<TypeCheckingError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void plus() {
    ModuleLoader.TypeCheckingUnit unit = parseDef(new ModuleLoader(), "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => suc (x' + y)");
    List<TypeCheckingError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void doubleRec() {
    ModuleLoader.TypeCheckingUnit unit = parseDef(new ModuleLoader(), "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => suc x'' + (x'' + y)");
    List<TypeCheckingError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void functionError() {
    ModuleLoader.TypeCheckingUnit unit = parseDef(new ModuleLoader(), "\\function (+) (x y : Nat) : Nat <= x + y");
    List<TypeCheckingError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
    assertTrue(unit.typedDefinition.hasErrors());
  }

  @Test
  public void functionError2() {
    ModuleLoader.TypeCheckingUnit unit = parseDef(new ModuleLoader(), "\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => y + y");
    List<TypeCheckingError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
    assertTrue(unit.typedDefinition.hasErrors());
  }

  @Test
  public void functionPartiallyApplied() {
    ModuleLoader.TypeCheckingUnit unit = parseDef(new ModuleLoader(), "\\function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => z (foo z x')");
    List<TypeCheckingError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }
}
