package org.arend.naming;

import org.junit.Test;

public class DefinableMetaTest extends NameResolverTestCase {
  @Test
  public void noArgDef() {
    var m = resolveNamesModule("\\meta sepush => 123");
    System.out.println(m);
  }

  @Test
  public void noArgCall() {
    var m = resolveNamesModule("\\meta sepush => 123" +
      "\\func qlbf => sepush");
    System.out.println(m);
  }
}
