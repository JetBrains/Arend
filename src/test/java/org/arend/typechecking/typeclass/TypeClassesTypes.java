package org.arend.typechecking.typeclass;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesTypes extends TypeCheckingTestCase {
  @Test
  public void testType() {
    typeCheckModule(
      "\\class C (X : \\Type)\n" +
      "  | idf : X -> X\n" +
      "\\instance inst : C \\Type\n" +
      "  | idf A => A\n" +
      "\\func foo : idf Nat => 0");
  }

  @Test
  public void testTypeProp() {
    typeCheckModule(
      "\\class C (X : \\Type)\n" +
      "  | idf : X -> X\n" +
      "\\instance inst : C \\Type\n" +
      "  | idf A => A\n" +
      "\\func foo (P : \\Prop) => idf P");
  }

  @Test
  public void testTypeSet() {
    typeCheckModule(
      "\\class C (X : \\Type)\n" +
      "  | idf : X -> X\n" +
      "\\instance inst : C \\Type\n" +
      "  | idf A => A\n" +
      "\\func foo (P : \\Set) => idf P");
  }

  @Test
  public void testTypeError() {
    typeCheckModule(
      "\\class C (X : \\Type)\n" +
      "  | idf : X -> X\n" +
      "\\instance inst : C \\Type0\n" +
      "  | idf A => A\n" +
      "\\func foo (P : \\Set1) => idf P", 1);
  }

  @Test
  public void testTypeError2() {
    typeCheckModule(
      "\\class C (X : \\Type)\n" +
      "  | idf : X -> X\n" +
      "\\instance inst : C \\Set\n" +
      "  | idf A => A\n" +
      "\\func foo (P : \\1-Type) => idf P", 1);
  }

  @Test
  public void testTypeError3() {
    typeCheckModule(
      "\\class C (X : \\Type)\n" +
      "  | idf : X -> X\n" +
      "\\instance inst : C \\1-Type\n" +
      "  | idf A => A\n" +
      "\\func foo (P : \\Set) => idf P");
  }
}
