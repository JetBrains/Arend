package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Recursive {
  @Test
  public void list() {
    Concrete.Definition def = parseDef("\\data List (A : \\Type0) | nil | cons A (List A)");
    List<VcError> errors = new ArrayList<>();
    Definition newDef = def.accept(new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(newDef);
  }

  @Test
  public void dataLeftError() {
    Concrete.Definition def = parseDef("\\data List (A : \\Type0) | nil | cons (List A -> A)");
    List<VcError> errors = new ArrayList<>();
    def.accept(new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
  }

  @Test
  public void dataRightError() {
    Concrete.Definition def = parseDef("\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))");
    List<VcError> errors = new ArrayList<>();
    def.accept(new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
  }

  @Test
  public void plus() {
    Concrete.Definition plus = parseDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => suc (x' + y)");
    List<VcError> errors = new ArrayList<>();
    Definition newPlus = plus.accept(new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(newPlus);
  }

  @Test
  public void doubleRec() {
    Concrete.Definition foo = parseDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => suc x'' + (x'' + y)");
    List<VcError> errors = new ArrayList<>();
    Definition newFoo = foo.accept(new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(newFoo);
  }

  @Test
  public void functionError() {
    Concrete.Definition foo = parseDef("\\function (+) (x y : Nat) : Nat <= x + y");
    List<VcError> errors = new ArrayList<>();
    foo.accept(new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
  }

  @Test
  public void functionError2() {
    Concrete.Definition foo = parseDef("\\function (+) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' <= \\elim x' | zero => y | suc x'' => y + y");
    List<VcError> errors = new ArrayList<>();
    foo.accept(new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
  }

  @Test
  public void functionPartiallyApplied() {
    Concrete.Definition foo = parseDef("\\function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \\elim x | zero => y | suc x' => z (foo z x')");
    List<VcError> errors = new ArrayList<>();
    Definition newFoo = foo.accept(new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(newFoo);
  }
}
