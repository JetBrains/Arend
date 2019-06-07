<h1 id="coercion">Coercion<a class="headerlink" href="#coercion" title="Permanent link">&para;</a></h1>

Sometimes it is convenient to interpret elements of type `A` as elements of type `B` and use
elements of `A` in places, where elements of `B` are expected. For example, 
natural numbers are also integer numbers, integer numbers are also rational numbers, and so on. There is a mechanism,
which allows this by marking a function `f` from `A` to `B`  as a _coercing function_. Once `f : A -> B` is declared
as a coercing function, whenever an expression `a : A` is used in a place, where type `B` is expected, `a` will be
automatically replaced with `f a : B`. 

A coercing function can be defined either for a `\data` or a `\class` definition.
It should be written inside the `\where` block for this definition and it should begin with `\use \coerce` instead 
of `\func`. For example, `Bool` can be coerced to `Nat` as follows:
```arend
\data Bool | true | false
  \where
    \use \coerce toNat (b : Bool) : Nat
      | true => 1
      | false => 0
```

It is possible to coerce a given definition either from or to other definition.
A function, which coerces from a given definition, must have this definition as the type of its last parameter.
A function, which coerces to a given definition, must have this definition as its result type.
For example, `Nat` can be coerced to `Bool` as follows:
```arend
\data Bool | true | false
  \where
    \use \coerce fromNat (n : Nat) : Bool
      | 0 => false
      | 1 => true
      | suc (suc n) => fromNat n
```

It is possible to define several coercing functions for a single type.
