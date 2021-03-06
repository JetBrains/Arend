package org.arend.naming;

import org.junit.Test;

public class DefinableMetaTest extends NameResolverTestCase {
  @Test
  public void noArgDef() {
    resolveNamesModule("\\meta sepush => 123");
  }

  @Test
  public void someArgsDef() {
    resolveNamesModule("\\meta sepush a b c => a b c");
  }

  @Test
  public void conflictDef() {
    resolveNamesModule("\\meta sepush a b c => a b c");
  }

  @Test
  public void noArgCall() {
    resolveNamesModule("\\meta sepush => 123" +
      "\\func qlbf => sepush");
  }

  @Test
  public void hierarchy() {
    resolveNamesModule(
      "\\func bread => qlbf.alendia\n" +
        "\\func qlbf => 123\n" +
        "  \\where \\meta alendia => 123");
  }
}
