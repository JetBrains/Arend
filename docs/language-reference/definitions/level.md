<h1 id="levels">Level<a class="headerlink" href="#level" title="Permanent link">&para;</a></h1>

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

TODO
