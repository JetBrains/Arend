package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.arend.naming.resolving.visitor.TypeClassReferenceExtractVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TypedLocatedReferable extends LocatedReferableImpl implements TypedReferable {
  private ClassReferable myTypeClassReference;
  private Referable myBodyReference;

  public TypedLocatedReferable(Precedence precedence, String name, @Nullable LocatedReferable parent, Kind kind, ClassReferable typeClassReference, Referable bodyReference) {
    super(precedence, name, parent, kind);
    myTypeClassReference = typeClassReference;
    myBodyReference = bodyReference;
  }

  public TypedLocatedReferable(Precedence precedence, String name, @NotNull ModuleLocation parent, Kind kind, ClassReferable typeClassReference, Referable bodyReference) {
    super(precedence, name, parent, kind);
    myTypeClassReference = typeClassReference;
    myBodyReference = bodyReference;
  }

  @Override
  public @Nullable ClassReferable getTypeClassReference() {
    return myTypeClassReference;
  }

  public void setTypeClassReference(ClassReferable ref) {
    myTypeClassReference = ref;
  }

  @Override
  public @Nullable Referable getBodyReference(TypeClassReferenceExtractVisitor visitor) {
    return myBodyReference;
  }

  public void setBodyReference(Referable ref) {
    myBodyReference = ref;
  }
}
