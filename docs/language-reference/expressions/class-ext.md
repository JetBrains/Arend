<h1 id="class-ext">Class extensions<a class="headerlink" href="#class-ext" title="Permanent link">&para;</a></h1>

A class extension is an expression of the form `C { | f_1 => e_1 ... | f_n => e_n }`, where `C` is a record, `f_1`, ... `f_n` are its fields of types `A_1`, ... `A_n` respectively, and `e_1`, ... `e_n` are expressions such that `e_i : A_i[e_1/f_1, ... e_n/f_n]`.
Note that `A_i` cannot depend on any field except for `f_1`, ... `f_n`.
An expression of the form `C e_1 ... e_n` is equivalent to `C { | f_1 => e_1 ... | f_n => e_n }`, where `f_1`, ... `f_n` is the list of not implemented fields of `C` in the order of their definition.

The expression `C {}` is equivalent to `C`.
An expression of the form `C { I }` is a subtype of `C' { I' }` if and only if `C` is a subclass of `C'` and `I'` is a subset of `I`.
The expression `\new C { I }` is an instance of type `C { I }`, which is a subtype `C`.
Thus, you can use this expression to create an element of type `C`.

# New expression

The expression `\new C { I }` is correct only if all fields of `C` are implemented in `C { I }`, but the typechecker can infer some implementations from the expected type of the expression.
For example, in the following code we do not have to implement field `x` in the `\new` expression explicitly since `f` expects an element of `R 0`, so the typechecker knows that `x` must be equal to `0`.

```arend
\record R (x y : Nat)
\func f (r : R 0) => r.y
\func g => f (\new R { | y => 1 })
```

If `c` is an instance of a record `C` with fields `f_1`, ... `f_n`, then the expression `\new c` is equivalent to `\new C { | f_1 => c.f_1 ... | f_n => c.f_n }`.
More generally, the expression `\new c { | f_{i_1} => e_1 ... | f_{i_k} => e_k }` is equivalent to `\new c` in which `c.f_{i_j}` is replaced with `e_j`.
