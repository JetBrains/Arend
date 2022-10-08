package org.arend.ext.concrete.definition;

public enum FunctionKind {
  COERCE {
    @Override public boolean isUse() { return true; }
    @Override public String getText() { return "\\use \\coerce"; }
  },
  LEVEL {
    @Override public boolean isUse() { return true; }
    @Override public boolean isSFunc() { return true; }
    @Override public String getText() { return "\\use \\level"; }
  },
  SFUNC {
    @Override public boolean isSFunc() { return true; }
    @Override public String getText() { return "\\sfunc"; }
  },
  EFUNC { @Override public String getText() { return "\\efunc"; } },
  LEMMA {
    @Override public boolean isSFunc() { return true; }
    @Override public String getText() { return "\\lemma"; }
  },
  TYPE {
    @Override public boolean isSFunc() { return true; }
    @Override public String getText() { return "\\type"; }
  },
  FUNC_COCLAUSE {
    @Override public boolean isCoclause() { return true; }
    @Override public String getText() { return "|"; }
  },
  CLASS_COCLAUSE {
    @Override public boolean isCoclause() { return true; }
    @Override public String getText() { return "\\default"; }
  },
  AXIOM {
    @Override public boolean isSFunc() { return true; }
    @Override public String getText() { return "\\axiom"; }
  },
  FUNC { @Override public String getText() { return "\\func"; } },
  CONS { @Override public String getText() { return "\\cons"; } },
  INSTANCE { @Override public String getText() { return "\\instance"; } };

  public boolean isCoclause() {
    return false;
  }

  public boolean isUse() {
    return false;
  }

  public boolean isSFunc() {
    return false;
  }

  public abstract String getText();
}
