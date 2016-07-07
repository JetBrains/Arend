package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public class MemberNotFoundError extends TypeCheckingError {
  public final Definition targetDefinition;
  public final String name;

  private MemberNotFoundError(Abstract.Definition definition, Definition targetDefinition, String name, String message, Abstract.SourceNode cause) {
    super(definition, message, cause);
    this.targetDefinition = targetDefinition;
    this.name = name;
  }

  public MemberNotFoundError(Abstract.Definition definition, Definition targetDefinition, String name, Abstract.SourceNode cause) {
    this(definition, targetDefinition, name, "Member not found", cause);
  }

  public MemberNotFoundError(Abstract.Definition definition, Definition targetDefinition, String name, boolean isStatic, Abstract.SourceNode cause) {
    this(definition, targetDefinition, name, (isStatic ? "Static" : "Dynamic") + " member not found", cause);
  }
}
