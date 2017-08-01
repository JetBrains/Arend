package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class MemberNotFoundError extends LocalTypeCheckingError {
  public final Definition targetDefinition;
  public final String name;

  private MemberNotFoundError(Definition targetDefinition, String name, String message, Abstract.SourceNode cause) {
    super(message, cause);
    this.targetDefinition = targetDefinition;
    this.name = name;
  }

  public MemberNotFoundError(Definition targetDefinition, String name, Abstract.SourceNode cause) {
    this(targetDefinition, name, "Member not found", cause);
  }

  public MemberNotFoundError(Definition targetDefinition, String name, boolean isStatic, Abstract.SourceNode cause) {
    this(targetDefinition, name, (isStatic ? "Static" : "Dynamic") + " member not found", cause);
  }

  @Override
  public LineDoc getBodyDoc(SourceInfoProvider src) {
    return hList(text(name + " of some compiled definition called "), refDoc(targetDefinition.getAbstractDefinition()));
  }
}
