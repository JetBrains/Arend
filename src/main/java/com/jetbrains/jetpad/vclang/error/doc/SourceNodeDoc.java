package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;

public class SourceNodeDoc extends CachingDoc {
  private final Abstract.SourceNode mySourceNode;

  public SourceNodeDoc(Abstract.SourceNode sourceNode) {
    mySourceNode = sourceNode;
  }

  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  protected String getString() {
    String text = PrettyPrintVisitor.prettyPrint(mySourceNode);
    return text == null ? mySourceNode.toString() : text;
  }
}
