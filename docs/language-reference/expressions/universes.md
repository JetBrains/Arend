<h1 id="universes">Universes<a class="headerlink" href="#universes" title="Permanent link">&para;</a></h1>

A universe is a type of types.
Since we cannot assume that there exists a type of all types, we assume a hierarchy of universes.
That is, we have a universe `\Type0` which contains all types that do not mention universes, a universe `\Type1` which contains all types that might mention only `\Type0`, and so on.
Thus, for every natural number n, we have an expression `\Type n` (the whitespace is optional).
This number is called the _predicative level_ of the universe.
The expression `\Type n` has type `\Type (n+1)`.
Universes are cumulative, that is every expression of type `\Type n` also has type `\Type (n+1)`.

## Homotopy levels

Universes are actually parameterized by two levels.
The other one is called the _homotopy level_.
The universe of predicative level p and homotopy level h is denote by `\h-Type p`.
The homotopy level h is a number such that -1 ≤ h ≤ ∞.
If h equals to ∞, then we write `\oo-Type p`.
If h equals to -1, then the universe does not have a predicative level and it is denoted by `\Prop`.
The expression `\h-Type p` has type `\(h+1)-Type (p+1)` if 0 ≤ h < ∞.
The expression `\Prop` has type `\0-Type 0`.
The expression `\oo-Type p` has type `\oo-Type (p+1)`.
Universes of the form `\0-Type p` can also be denoted by `\Set p`.
The homotopy level can be also specified after the predicative level.
That is, we can write `\Type p h` instead of `\h-Type p`

## Level polymorphism

Every definition is considered polymorphic in both levels.
That is, every definition has two additional parameters: one for a predicative level and one for a homotopy level.
These parameters are denoted by `\lp` and `\lh` respectively.
You can explicitly specify level arguments in a defcall by writing `\level p h`, where `p` and `h` are level expressions of the corresponding kind.
Keyword `\level` can be often omitted (if the resulting expression is unambiguous).
To specify the `\Prop` level, write `\level \Prop`.
Level expressions are defined inductively:

* `\lp` is a level expression of the predicative kind and `\lh` is a level expression of the homotopy kind.
* A constant (that is, a natural number) is a level expression of both kinds. There is also constant `\inf` for homotopy levels.
* `_` is a level expression of both kinds. Such an expression tells the typechecker to infer the expression.
* If l is a level expression, then `\suc l` is also a level expression of the same kind as l.
* If l1 and l2 are level expressions of the same kind, then `\max l1 l2` is also a level expression of the same kind as l1 and l2.

Since the only level variables are `\lp` and `\lh`, the expression `\max l1 l2` is useful only when one of the levels is a constant.

## Level inference

The level arguments to a function are often can be inferred automatically.
Moreover, both levels of a universe can also be omitted, in which case they are also will be inferred by the typechecker.
The typechecker always tries to infer the minimal level which mentions either `\lp` or `\lh` if possible.
Consider, for example, the following code which defines the identity function:

```arend
\func id {A : \Type} (a : A) => a
```

The minimal appropriate level (both predicative and homotopy) of the universe `\Type` in the definition of this function is 0,
but it is also possible to use levels `\lp` and `\lh`, so this function is equivalent to the following one:

```arend
\func id' {A : \Type \lp \lh} (a : A) => a
```

Let us give a few more examples.
We write a definition and an equivalent definition with explicitly specified levels below it.

```arend
\data Either (A B : \Type) | inl A | inr B
\data Either' (A B : \Type \lp \lh) | inl A | inr B

\func f => id \Type
\func f' => id (\suc \lp) (\suc \lh) (\Type \lp \lh)

\func fromEither {A : \Type} (e : Either A \Type) : \Type \elim e
  | inl a => A
  | inr X => X
\func fromEither' {A : \Type \lp \lh} (e : Either (\suc \lp) (\suc \lh) A (\Type \lp \lh)) : \Type \lp \lh \elim e
  | inl a => A
  | inr X => X
```

The levels in parameters and in the result type of a recursive function are inferred before levels in the body.
This means that the following function will not typecheck.

```arend
\func eitherToType {A : \Type} (e : Either A A) : \Type
  | inl _ => \Type
  | inr _ => \Type
```

We can explicitly specify the levels of the universe that appears in the result type to fix this problem:

```arend
\func eitherToTypeFixed {A : \Type} (e : Either A A) : \Type (\suc \lp) (\suc \lh)
  | inl _ => \Type
  | inr _ => \Type
\func eitherToTypeFixed' {A : \Type \lp \lh} (e : Either \lp \lh A A) : \Type (\suc \lp) (\suc \lh)
  | inl _ => \Type \lp \lh
  | inr _ => \Type \lp \lh
```

If we specify constant levels instead as shown below, then the function also will typecheck, but the levels of universes in the body will also be constant:

```arend
\func eitherToTypeConstant {A : \Type} (e : Either A A) : \3-Type 7
  | inl _ => \Type
  | inr _ => \Type
\func eitherToTypeConstant' {A : \Type \lp \lh} (e : Either \lp \lh A A) : \3-Type 7
  | inl _ => \Set0
  | inr _ => \Set0
```

Note that homotopy levels inferred by the typechecker are always greater than or equal to 0.
Thus, the function `eitherToProp` does not typecheck even though `eitherToPropFixed` does:

```arend
\func eitherToProp {A : \Type} (e : Either A A) : \Set0
  | inl _ => \Type
  | inr _ => \Type

\func eitherToPropFixed {A : \Type} (e : Either A A) : \Set0
  | inl _ => \Prop
  | inr _ => \Prop
```

Levels in the result type of a non-recursive function are inferred together with levels in the body.
Thus, the following function typechecks:

```arend
\func f : \Type => \Type
\func f' : \Type (\suc \lp) (\suc \lh) => \Type \lp \lh
```

A definition is marked as _universe-like_ if it contains universes or universe-like definitions applied to either `\lp` or `\lh`.
It is often true that the level of a definition can be equal to either `c` or `\lp + c` for some constant `c`.
If a definition is universe-like, then the inferrence algorithm uses the latter option, and it uses the former option in the other case.
Also, if `D` is a universe-like definition, then `D \level p h` is equivalent to `D \level p' h'` only if `p = p'` and `h = h'`.
If `D` is not universe-like, then these expressions are always equivalent.
