package org.arend.naming;

import org.junit.Test;

public class DefinableMetaTest extends NameResolverTestCase {
  @Test
  public void noArgDef() {
    resolveNamesModule("\\meta sepush => 123");
  }

  @Test
  public void someArgsDef() {
    var m = resolveNamesModule("\\meta sepush a b c => a b c");
    System.out.println(m);
  }

  @Test
  public void noArgCall() {
    resolveNamesModule("\\meta sepush => 123" +
      "\\func qlbf => sepush");
  }
}
