<h1 id="coercion">Coercion<a class="headerlink" href="#coercion" title="Permanent link">&para;</a></h1>

It is often true that there is a canonical function between two given types.
For example, there are obvious functions from natural to integer numbers, from integer to rational numbers, and so on.
It is possible to mark such a function as a coercing function.
Every time an expression of type `B` is expected but an expression of type `A` is given and there is a coercing function from `A` to `B` it will be automatically inserted.

A coercing function can be define either for a `\data` or a `\class` definition.
To do this, you need to write it inside the `\where` block for this definition and start it with `\use \coerce` insted of `\func`.
For example, to coerce `Bool` to `Nat`, you can write
```arend
\data Bool | true | false
  \where
    \use \coerce toNat (b : Bool) : Nat
      | true => 1
      | false => 0
```

You can coerce either from or to a definition.
To coerce from a definition, the last parameter of the coercing function must have this definition as its type.
To coerce to a definition, the result type of the coercing function must be this definition.
For example, to coerce from `Nat` to `Bool`, you can write
```arend
\data Bool | true | false
  \where
    \use \coerce fromNat (n : Nat) : Bool
      | 0 => false
      | 1 => true
      | suc (suc n) => fromNat n
```

You can have several coercing functions for a single type.
