package org.arend.ext;

import org.arend.ext.concrete.ConcreteParameter;
import org.arend.ext.core.context.CoreBinding;
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
  public @NotNull FreeBindingsModifier add(@Nullable CoreBinding binding) {
    return binding == null ? this : add(Collections.singletonList(binding));
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
  public @NotNull FreeBindingsModifier addRef(@Nullable ArendRef ref, @Nullable CoreBinding binding) {
    return binding == null ? this : addRef(Collections.singletonList(new Pair<>(ref, binding)));
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
  public @NotNull FreeBindingsModifier addParams(@Nullable ConcreteParameter parameter) {
    return parameter == null ? this : addParams(Collections.singletonList(parameter));
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
  public @NotNull FreeBindingsModifier remove(@Nullable CoreBinding binding) {
    return binding == null ? this : remove(Collections.singleton(binding));
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
  public @NotNull FreeBindingsModifier retain(@Nullable CoreBinding binding) {
    return binding == null ? this : retain(Collections.singleton(binding));
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
  public @NotNull FreeBindingsModifier replace(@Nullable CoreBinding oldBinding, @NotNull CoreBinding newBinding) {
    return oldBinding == null ? this : replace(Collections.singletonMap(oldBinding, newBinding));
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
