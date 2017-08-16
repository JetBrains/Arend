package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class SourceNodeDoc extends CachingDoc {
  private final Abstract.SourceNode mySourceNode;
  private final PrettyPrinterInfoProvider myInfoProvider;

  SourceNodeDoc(Abstract.SourceNode sourceNode, PrettyPrinterInfoProvider infoProvider) {
    mySourceNode = sourceNode;
    myInfoProvider = infoProvider;
  }

  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  protected String getString() {
    String text = PrettyPrintVisitor.prettyPrint(mySourceNode, myInfoProvider);
    return text == null ? mySourceNode.toString() : text;
  }
}
