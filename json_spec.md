# Just an idea of what the parser output might look like


## File
```json
{
    "imports" : [
        ["console", "out"],
        ["hashmap"],
        ...
    ],
    "class" : <class>
}
```

## Class
```json
{
    "name" : "Client",
    "extends" : null, // or "ParentClass" (if null, it doesn't exist)

    "variable declarations" : [
        {"type" : "int", "id" : "a"},
        {"type" : "bool", "id" : "b"},
        {"type" : "float", "id" : "c"},
    ],

    "method declarations" : [
        <method declaration>,
        <method declaration>,
        <method declaration>,
        ...
    ]
}
```

## Method declaration
```json
{   
    "is main" : false,

    "type" : "int", // void if this function is main
    "id" : "get_x",
    "arguments" : [
        {"type" : "int", "id" : "a"},
        {"type" : "bool", "id" : "b"},
        {"type" : "float", "id" : "c"},
    ],

    "statements" : [
        // Here there is an example of every statement
        { // int a;
            "statement type" : "declaration", 
            "type" : "int", 
            "id" : "a"
        },
        { // {int a; a = 3;}
            "statement type" : "block",
            "statements" : [<statement>, ...]
        },
        { // if (rand  < 4 ) {this.beLazy(L); lazy = true;} else {lazy = false;}
            "statement type" : "if-else",
            "condition" : <expression>,
            "if-statements" : [<statement>, ...],
            "else-statements" : [<statement>, ...],
        },
        { // while (rand  < 4) {b = 1;}
            "statement type" : "while",
            "condition" : <expression>,
            "loop-statements" : [<statement>, ...],
        },
        { // 1 + 3;
            "statement type" : "expression",
            "expression" : <expression>,
        },
        { // a = 1 + 3;
            "statement type" : "assignment",
            "id" : "a",
            "expression" : <expression>,
        },
        { // a[4 * 2] = 1 + 3;
            "statement type" : "index assignment",
            "id" : "a",
            "index" : <expression>,
            "expression" : <expression>,
        },
    ],

    "return" : <expression> // May be null if this method is main
}
```

## Expression
```json
{
    "expression type" : "literal",
    "type" : "boolean", // or "integer"
    "value" : true      // or 123
}
```

```json
{
    "expression type" : "identifier",
    "id" : "acd"
}
```

```json
{
    "expression type" : "this"
}
```

```json
{
    "expression type" : "new array",
    "size" : <expression>
}
```


```json
{
    "expression type" : "new object",
    "id" : "Turtle"
}
```


```json
{
    "expression type" : "negation",
    "expression" : <expression>
}
```

```json
{
    "expression type" : "and",
    "left" : <expression>,
    "right" : <expression>
}
```

```json
{
    "expression type" : "lower",
    "left" : <expression>,
    "right" : <expression>
}
```

```json
{
    "expression type" : "math operation", 
    "operation" : "+", // "+" "-" "*" "/"
    "left" : <expression>,
    "right" : <expression>
}
```

```json
{
    "expression type" : "math operation",
    "operation" : "+", // "+" "-" "*" "/"
    "left" : <expression>,
    "right" : <expression>
}
```

```json
{
    "expression type" : "length",
    "expression" : <expression>
}
```

```json
{
    "expression type" : "get array item",
    "id" : <expression>,
    "index" : <expression>
}
```

```json
{
    "expression type" : "method call",
    "object" : <expression>,
    "method name" : "foo",
    "args" : [
        <expression>,
        <expression>,
        <expression>,
        <expression>,
        ...
    ]
}
```

## Hello world example
### Code
```
import ioPlus;
class HelloWorld {
	public static void main(String[] args) {
		ioPlus.printHelloWorld();
	}
}
```

```json
{
    "imports" : [
        ["ioPlus"]
    ],
    "class" : {
        "name" : "HelloWorld",
        "extends" : null, 

        "variable declarations" : [],

        "method declarations" : [
            {   
                "is main" : true,
                "type" : "void",
                "id" : "main",
                "arguments" : [
                    {"type" : "String[]", "id" : "args"}
                ],

                "statements" : [
                    {
                        "statement type" : "expression",
                        "expression" : {
                            "expression type" : "method call",
                            "object" : {
                                "expression type" : "identifier",
                                "id" : "ioPlus"
                            },
                            "method name" : "printHelloWorld",
                            "args" : []
                        },
                    }
                ],

                "return" : null
            }
        ]
    }
}
```