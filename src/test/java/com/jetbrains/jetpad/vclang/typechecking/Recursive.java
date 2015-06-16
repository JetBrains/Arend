package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static org.junit.Assert.*;

public class Recursive {
  @Test
  public void list() {
    ModuleLoader.TypeCheckingUnit unit = parseDef("\\data List (A : \\Type0) | nil | cons A (List A)");
    List<VcError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(null, unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void dataLeftError() {
    ModuleLoader.TypeCheckingUnit unit = parseDef("\\data List (A : \\Type0) | nil | cons (List A -> A)");
    List<VcError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(null, unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void dataRightError() {
    ModuleLoader.TypeCheckingUnit unit = parseDef("\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))");
    List<VcError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(null, unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void plus() {
    ModuleLoader.TypeCheckingUnit unit = parseDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => suc (x' + y)");
    List<VcError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(null, unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void doubleRec() {
    ModuleLoader.TypeCheckingUnit unit = parseDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => suc x'' + (x'' + y)");
    List<VcError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(null, unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }

  @Test
  public void functionError() {
    ModuleLoader.TypeCheckingUnit unit = parseDef("\\function (+) (x y : Nat) : Nat <= x + y");
    List<VcError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(null, unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
    assertTrue(unit.typedDefinition.hasErrors());
  }

  @Test
  public void functionError2() {
    ModuleLoader.TypeCheckingUnit unit = parseDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => y + y");
    List<VcError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(null, unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
    assertTrue(unit.typedDefinition.hasErrors());
  }

  @Test
  public void functionPartiallyApplied() {
    ModuleLoader.TypeCheckingUnit unit = parseDef("\\function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => z (foo z x')");
    List<VcError> errors = new ArrayList<>();
    unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(null, unit.typedDefinition, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(unit.typedDefinition.hasErrors());
  }
}
