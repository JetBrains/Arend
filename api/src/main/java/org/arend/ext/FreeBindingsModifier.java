package org.arend.ext;

import org.arend.ext.concrete.ConcreteParameter;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FreeBindingsModifier {
  public static class Command {
    public enum Kind { REMOVE, RETAIN, ADD, ADD_PARAM, REPLACE, REPLACE_REMOVE, CLEAR }

    public final Kind kind;
    public final Object bindings;

    private Command(Kind kind, Object bindings) {
      this.kind = kind;
      this.bindings = bindings;
    }
  }

  public final List<Command> commands = new ArrayList<>();

  /**
   * Adds a binding to the context.
   */
  public @NotNull FreeBindingsModifier add(@NotNull CoreBinding binding) {
    return add(Collections.singletonList(binding));
  }

  /**
   * Adds given bindings to the context.
   */
  public @NotNull FreeBindingsModifier add(@NotNull Collection<? extends CoreBinding> bindings) {
    commands.add(new Command(Command.Kind.ADD, bindings));
    return this;
  }

  /**
   * Adds a binding to the context.
   */
  public @NotNull FreeBindingsModifier addRef(@Nullable ArendRef ref, @NotNull CoreBinding binding) {
    return addRef(Collections.singletonList(new Pair<>(ref, binding)));
  }

  /**
   * Adds given bindings to the context.
   */
  public @NotNull FreeBindingsModifier addRef(@NotNull Collection<? extends Pair<? extends ArendRef, ? extends CoreBinding>> bindings) {
    commands.add(new Command(Command.Kind.ADD, bindings));
    return this;
  }

  /**
   * Typechecks {@code parameter} and adds the result to the context.
   */
  public @NotNull FreeBindingsModifier addParams(@NotNull ConcreteParameter parameter) {
    return addParams(Collections.singletonList(parameter));
  }

  /**
   * Typechecks {@code parameters} and adds the result to the context.
   * If typechecking fails, the action will not be invoked.
   */
  public @NotNull FreeBindingsModifier addParams(@NotNull Collection<? extends ConcreteParameter> parameters) {
    commands.add(new Command(Command.Kind.ADD_PARAM, parameters));
    return this;
  }

  /**
   * Removes a binding from the context.
   */
  public @NotNull FreeBindingsModifier remove(@NotNull CoreBinding binding) {
    return remove(Collections.singleton(binding));
  }

  /**
   * Removes given bindings from the context.
   */
  public @NotNull FreeBindingsModifier remove(@NotNull Set<? extends CoreBinding> bindings) {
    commands.add(new Command(Command.Kind.REMOVE, bindings));
    return this;
  }

  /**
   * Removes all bindings except for the given one.
   */
  public @NotNull FreeBindingsModifier retain(@NotNull CoreBinding binding) {
    return retain(Collections.singleton(binding));
  }

  /**
   * Removes all bindings except for the given ones.
   */
  public @NotNull FreeBindingsModifier retain(@NotNull Set<? extends CoreBinding> bindings) {
    commands.add(new Command(Command.Kind.RETAIN, bindings));
    return this;
  }

  /**
   * Removes all bindings.
   */
  public @NotNull FreeBindingsModifier clear() {
    commands.add(new Command(Command.Kind.CLEAR, null));
    return this;
  }

  /**
   * Replaces a binding with a new one.
   */
  public @NotNull FreeBindingsModifier replace(@NotNull CoreBinding oldBinding, @NotNull CoreBinding newBinding) {
    return replace(Collections.singletonMap(oldBinding, newBinding));
  }

  /**
   * Replaces given bindings with new ones.
   */
  public @NotNull FreeBindingsModifier replace(@NotNull Map<? extends CoreBinding, ? extends CoreBinding> replacement) {
    commands.add(new Command(Command.Kind.REPLACE, replacement));
    return this;
  }

  /**
   * Replaces given bindings with new ones and removes bindings for which {@code replacement.get()} returns {@code null}.
   */
  public @NotNull FreeBindingsModifier replaceAndRemove(@NotNull Map<? extends CoreBinding, ? extends CoreBinding> replacement) {
    commands.add(new Command(Command.Kind.REPLACE_REMOVE, replacement));
    return this;
  }
}
