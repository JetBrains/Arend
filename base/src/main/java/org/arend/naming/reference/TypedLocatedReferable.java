package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.naming.resolving.visitor.TypeClassReferenceExtractVisitor;
import org.arend.term.group.AccessModifier;
import org.jetbrains.annotations.Nullable;

public class TypedLocatedReferable extends LocatedReferableImpl implements TypedReferable {
  private final ClassReferable myTypeClassReference;
  private Referable myBodyReference;

  public TypedLocatedReferable(AccessModifier accessModifier, Precedence precedence, String name, @Nullable LocatedReferable parent, Kind kind, ClassReferable typeClassReference, Referable bodyReference) {
    super(accessModifier, precedence, name, parent, kind);
    myTypeClassReference = typeClassReference;
    myBodyReference = bodyReference;
  }

  @Override
  public @Nullable ClassReferable getTypeClassReference() {
    return myTypeClassReference;
  }

  @Override
  public @Nullable Referable getBodyReference(TypeClassReferenceExtractVisitor visitor) {
    return myBodyReference;
  }

  public void setBodyReference(Referable ref) {
    myBodyReference = ref;
  }
}
