interface WAE {}

interface Operation: WAE {
    val symbol: Char
    var lhs: WAE?
    var rhs: WAE?
}

fun String.toOperation() : Operation {
    return when(this) {
        "+" -> Plus()
        "-" -> Minus()
        "/" -> Divide()
        "*" -> Multiply()
        else -> throw IllegalArgumentException("invalid operator")
    }
}

data class Plus (
    override val symbol: Char = '+',
    override var lhs: WAE? = null,
    override var rhs: WAE? = null): Operation
data class Minus (
    override val symbol: Char = '-',
    override var lhs: WAE? = null,
    override var rhs: WAE? = null): Operation
data class Divide (
    override val symbol: Char = '/',
    override var lhs: WAE? = null,
    override var rhs: WAE? = null): Operation
data class Multiply (
    override val symbol: Char = '*',
    override var lhs: WAE? = null,
    override var rhs: WAE? = null): Operation

data class Number (val number: Int): WAE
data class Variable (val name: String): WAE
data class With (
    val variable: Variable? = null,
    var inner: WAE? = null,
    var outer: WAE? = null
): WAE
data class Parenthesis (
    val paren: Char
): WAE