<h1 id="universes">Universes<a class="headerlink" href="#universes" title="Permanent link">&para;</a></h1>

A universe is a type of types. Since the type of all types cannot be consistently introduced to a type theory
with dependent pi types, as the type of types cannot contain itself, Arend contains a hierarchy of universes 
`\Type n` (the whitespace is optional), parameterized by a natural number `n`. This number is called the 
_predicative level_ of the universe. Informally, the universe `\Type0` contains all types that do not refer to universes
in their definition, the universe `\Type1` contains all types in `\Type0` together with those types that
refer to `\Type0` and no other universes in their definitions, and so on. This is not precise, since, for instance, 
the universe `\Type n` contains
also some data types, classes and records that refer to `\Type m`, where n ≤ m, in types of parameters.
See section on universe placement rules below for more precise statements and details.  
 
Note that the hierarchy of 
universes in Arend is cumulative, that is every expression of type `\Type n` has also type `\Type (n+1)`. 

Types in `\Type n` in Arend are also arranged in universes `\h-Type n` according to their _homotopy level_ h,
which is an integer number (or infinity ∞) in the range: -1 ≤ h ≤ ∞. 
Some of these universes have alternative names: the universe of propositions (-1-types) `\Prop` 
(coincides with `\-1-Type n` for any `n`) and universes of sets (0-types) `\Set n` (coincides with `\0-Type n`). 
Note that the universe `\Prop` is _impredicative_: it does not have predicative level. Practically, this means that
if `B : \Prop`, then the type `\Pi (x : \Prop) -> B` is in `\Prop`. 

Universes with h equal to ∞ are represented in the syntax as `\oo-Type p`. The homotopy level can be also 
specified after the predicative level: the syntax `\Type p h` can be used instead of `\h-Type p`.   

## Universe placement rules

Types in Arend are distributed over the universes according to the following rules:

* If `A : \h_1-Type p_1` and `B : \h_2-Type p_2`, then `\Sigma A B : \max(h_1,h_2)-Type max(p_1,p_2)`.
* If `A : \h_1-Type p_1` and `B : \h_2-Type p_2`, then `\Pi (x:A) -> B : \h_2-Type max(p_1,p_2)`. Note that
if `A=\Prop` and `B : \Prop`, then `\Pi (x : \Prop) -> B : \Prop`.
* If 0 ≤ h < ∞, then `\h-Type p : \(h+1)-Type (p+1)`.
* `\Prop : \Set 0`, which is the same as `\Prop : \0-Type 0`.
* `\oo-Type p : \oo-Type (p+1)`.
* If `A : I -> \h-Type p`, then `Path A a a' : \max(-1,h-1)-Type p`.
* If `D` is a data type and `A_1 : \h_1-Type p_1, ..., A_k : \h_k-Type p_k` are types of parameters
of constructors of `D`, then predicative level of `D` is the maximum over `0, p_1, ..., p_k`. If `D`
has conditions, equalising a constructor on two ends of the interval type, then homotopy level of 
`D` is ∞. Otherwise, if `D` has more than one constructor, then its homotopy level is
the maximum over `0, h_1, ..., h_k`, and if `D` has at most one constructor, then its homotopy level
is the maximum over `-1, h_1, ..., h_k`.
* If `C` is a class or record and `A_1 : \h_1-Type p_1, ..., A_k : \h_k-Type p_k` are types of parameters
of unimplemented fields of `C` (including fields of superclasses), then its predicative level is the maximum 
over `0, p_1, ..., p_k` and its homotopy level is the maximum over `-1, h_1, ..., h_k`.       

## Level polymorphism

Every definition is considered to be polymorphic in both levels.
That is, every definition has two additional parameters: one for a predicative level and one for a homotopy level.
These parameters are denoted by `\lp` and `\lh` respectively.
Level arguments can be specified explicitly in a defcall by writing `\level p h`, where `p` and `h` are
level expressions of the corresponding kind. For example, `Path (\lam _ => Nat) 0 0` is equivalent to
`Path \level 0 0 (\lam _ => Nat) 0 0`.  
Keyword `\level` can often be omitted (if the resulting expression is unambiguous).
The `\Prop` level can be specified by the expression `\level \Prop`.
Level expressions are defined inductively:

* `\lp` is a level expression of the predicative kind and `\lh` is a level expression of the homotopy kind.
* A constant (that is, a natural number) is a level expression of both kinds. There is also constant `\inf` for homotopy levels.
* `_` is a level expression of both kinds. Such an expression suggests the typechecker to infer the expression.
* If l is a level expression, then `\suc l` is also a level expression of the same kind as l.
* If l1 and l2 are level expressions of the same kind, then `\max l1 l2` is also a level expression of the same kind as l1 and l2.

Since the only level variables are `\lp` and `\lh`, the expression `\max l1 l2` is useful only when one of the levels is a constant.

## Level inference

The level arguments of a function in a defcall can often be inferred automatically.
Moreover, both levels of a universe in the signature of a function can also be omitted, in which case they
will also be inferred by the typechecker.
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

Consider a few more examples.
Every definition below is followed by an equivalent definition with explicitly specified levels.

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
In particular, this means that the following function will not typecheck:

```arend
\func eitherToType {A : \Type} (e : Either A A) : \Type
  | inl _ => \Type
  | inr _ => \Type
```

This problem can be fixed by specifying explicitly the levels of the universe that appears 
in the result type:

```arend
\func eitherToTypeFixed {A : \Type} (e : Either A A) : \Type (\suc \lp) (\suc \lh)
  | inl _ => \Type
  | inr _ => \Type
\func eitherToTypeFixed' {A : \Type \lp \lh} (e : Either \lp \lh A A) : \Type (\suc \lp) (\suc \lh)
  | inl _ => \Type \lp \lh
  | inr _ => \Type \lp \lh
```

If levels are set to constants instead as shown below, then the function also will typecheck,
but the levels of universes in the body will also be constants:

```arend
\func eitherToTypeConstant {A : \Type} (e : Either A A) : \3-Type 7
  | inl _ => \Type
  | inr _ => \Type
\func eitherToTypeConstant' {A : \Type \lp \lh} (e : Either \lp \lh A A) : \3-Type 7
  | inl _ => \Set0
  | inr _ => \Set0
```

Note that homotopy levels inferred by the typechecker are always greater than or equal to 0.
Thus, the function `eitherToProp` below does not typecheck, `eitherToPropFixed` should be
used instead:

```arend
\func eitherToProp {A : \Type} (e : Either A A) : \Set0
  | inl _ => \Type
  | inr _ => \Type

\func eitherToPropFixed {A : \Type} (e : Either A A) : \Set0
  | inl _ => \Prop
  | inr _ => \Prop
```

Levels in the result type of a non-recursive function are inferred simultaneously with the
levels in the body.
For example, the following function typechecks:

```arend
\func f : \Type => \Type
\func f' : \Type (\suc \lp) (\suc \lh) => \Type \lp \lh
```

A definition is marked as _universe-like_ if it contains universes or universe-like definitions applied to either `\lp` or `\lh`.
It is often true that the level of a definition can be inferred to either `c` or `\lp + c` for some constant `c`.
If a definition is universe-like, then the inference algorithm uses the latter option, otherwise it uses the former option.
Also, if `D` is a universe-like definition, then `D \level p h` is equivalent to `D \level p' h'` only if `p = p'` and `h = h'`.
If `D` is not universe-like, then these expressions are always equivalent.
