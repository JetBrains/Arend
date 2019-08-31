package org.arend.typechecking;

import org.junit.Test;

public class InfixPostfixTest extends TypeCheckingTestCase {
  @Test
  public void rightSectionTest() {
    typeCheckModule(
      "\\open Nat\n" +
      "\\func test (x : Nat) : (`+ 0) x = x => path (\\lam _ => x)");
  }

  @Test
  public void infixLongNameTest() {
    typeCheckDef("\\func test => 0 Nat.`div` 1");
  }

  @Test
  public void postfixLongNameTest() {
    typeCheckDef("\\func test => 0 Nat.`div 1");
  }

  @Test
  public void infixLongNameStartTest() {
    resolveNamesDef("\\func test => Nat.`div` 0", 1);
  }

  @Test
  public void postfixLongNameStartTest() {
    typeCheckDef("\\func test : (Nat.`mod 7) 18 = 4 => path (\\lam _ => 4)");
  }

  @Test
  public void infixLongNameSingleTest() {
    resolveNamesDef("\\func test => Nat.`div`", 1);
  }

  @Test
  public void postfixLongNameSingleTest() {
    resolveNamesDef("\\func test => Nat.`div", 1);
  }

  @Test
  public void infixLongNameProjTest() {
    resolveNamesModule(
      "\\module Test \\where {\n" +
      "  \\func pair => (0,1)\n" +
      "}\n" +
      "\\func test => Test.`pair`.1", 1);
  }

  @Test
  public void postfixLongNameProjTest() {
    resolveNamesModule(
      "\\module Test \\where {\n" +
      "  \\func pair => (0,1)\n" +
      "}\n" +
      "\\func test => Test.`pair.1", 1);
  }
}
