<h1 id="levels">Level<a class="headerlink" href="#level" title="Permanent link">&para;</a></h1>

## Use level

The homotopy level of definitions is inferred automatically, but sometimes it is possible to prove that it has a smaller level.
For example, we can define the following data types:
```arend
\data Empty
\data Dec (P : \Prop) | yes P | no (P -> Empty)
```

The type of `Empty` is `\Prop`, but the type of `Dec P` is `\Set0`.
We can prove that `Dec P` also has type `\Prop`.
To put `Dec` in the corresponding universe, we need to write this proof in the `\where` block of `Dec` and start it with keywords `\use \level` instead of `\func`:
```arend
\data Empty
\data Dec (P : \Prop) | yes P | no (P -> Empty)
  \where
    \use \level isProp {P : \Prop} (d1 d2 : Dec P) : d1 = d2
      | yes x1, yes x2 => path (\lam i => yes (Path.inProp x1 x2 @ i))
      | yes x1, no e2 => \case e2 x1 \with {}
      | no e1, yes x2 => \case e1 x2 \with {}
      | no e1, no e2 => path (\lam i => no (\lam x => (\case e1 x \return e1 x = e2 x \with {}) @ i))
```

Functions `\use \level` can be specified for `\data` and `\class` definitions.
They must have a particular type.
First parameters of such a function must be parameters of the data type or (some) fields of the class.
The rest of parameters together with the result type must prove that the data type (or the class) has some homotopy level.
That is, it must prove `ofHLevel (D p_1 ... p_k) n` for some constant `n`, where `D` is the data type (or the class), `p_1`, ... `p_k` are its parameters (or fields), and `ofHLevel` is defined as follows:
```arend
\func \infix 2 ofHLevel_-1+ (A : \Type) (n : Nat) : \Type \elim n
  | 0 => \Pi (a a' : A) -> a = a'
  | suc n => \Pi (a a' : A) -> (a = a') ofHLevel_-1+ n
```

## Level of a type

Sometimes we need to know that some type has a certain homotopy level.
For example, to define a [lemma](/language-reference/definitions/functions/#lemmas) or a [property](/language-reference/definitions/records/#properties), the result type must be a proposition.
If the type does not belong to the corresponding universe, but it can be proved that it has the correct homotopy level, then we can use the `\level` keyword to convince the typechecker to accept the definition.
This keywords can be specified in the result type of a function, a lemma, a field, or a case expression.
Its first argument is the type and the second is the proof that it belongs to some homotopy level.

For example, if `A` is a type such that `p : \Pi (x y : A) -> x = y`, then we can define a lemma that proves `A` as follows:
```arend
\lemma lem : \level A p => ...
```

Similarly, you can define a property of type `A`:
```arend
\record R {
  \property p : \level A p
}
```

To define a function or a case expression over a higher inductive type with values in `A`, we can omit some clauses if `A` belongs to an appropriate universe.
If it is not, but we can prove that it has the required homotopy level, then we can use the `\level` keyword to convince the typechecker that some clauses can be omitted.
For example, if `Trunc` is a propositional truncation with constructor `inT : A -> Trunc A`, `A` and `B` are types, `g : A -> B` is function, and `p : \Pi (x y : B) -> x = y`, then we can define the following function:
```arend
\func f (t : Trunc A) : \level B p
  | inT a => g a
```

Similarly, we can use `\level` in case expressions:
```arend
\func f' (t : Trunc A) => \case t \return \level B p \with {
  | inT a => g a
}
```
