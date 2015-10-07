package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class Name {
  public String name;
  public Abstract.Definition.Fixity fixity;

  public Name(String name, Abstract.Definition.Fixity fixity) {
    this.name = name;
    this.fixity = fixity;
  }

  public Name(String name) {
    this.name = name;
    this.fixity = name.isEmpty() || Character.isJavaIdentifierStart(name.charAt(0)) ? Abstract.Definition.Fixity.PREFIX : Abstract.Definition.Fixity.INFIX;
  }

  public String getPrefixName() {
    return fixity == Abstract.Definition.Fixity.PREFIX ? name : "(" + name + ")";
  }

  public String getInfixName() {
    return fixity == Abstract.Definition.Fixity.PREFIX ? "`" + name + "`" : name;
  }

  @Override
  public String toString() {
    return getPrefixName();
  }

  @Override
  public boolean equals(Object other) {
    return other == this || other instanceof Name && ((Name) other).name.equals(name);
  }
}
