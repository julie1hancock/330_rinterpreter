class Interpreter {

    fun parse(s: String) : WAE {
        var list = parseToList(s)
        var l = parseList(list).first
        if(openParen != closeParen) throw java.lang.IllegalArgumentException("!")
        var wae = transform(l)
        if(wae is Operation && (wae.lhs == null || wae.rhs == null)) throw IllegalArgumentException("!")
        return wae
    }

    private fun transform(parseList: List<*>): WAE {
        val list = parseList.removeBad()
        if(list.size == 1) {
            return if(list[0] is WAE) list[0] as WAE
            else transform(list[0] as List<*>)
        }
        if(list.size == 3) {
            when {
                list[0] is Operation -> {
                    val op = list[0] as Operation
                    op.lhs = transform(listOf(list[1]))
                    op.rhs = transform(listOf(list[2]))
                    return op
                }
                list[0] is With -> {
                    val with = list[0] as With
                    if(list[1] !is List<*>) throw java.lang.IllegalArgumentException("!")
                    val p: Pair<Variable, WAE> = unwrapWith((list[1] as List<*>).removeBad())
                    with.variable = p.first
                    with.inner = p.second
                    with.outer = transform(listOf(list[2]))
                    return with
                }
                else -> throw IllegalArgumentException("!")
            }
        }
        throw IllegalArgumentException("!")
    }

    private fun unwrapWith(list: List<*>): Pair<Variable, WAE> {
        if(list.size != 2 || list[0] !is Variable) throw java.lang.IllegalArgumentException("!")
        var variable = list[0] as Variable
        var wae = when {
            list[1] is WAE -> list[1] as WAE
            list[1] is List<*> -> transform(list[1] as List<*>)
            else -> throw java.lang.IllegalArgumentException("!")
        }
        return Pair(variable, wae)
    }

    var openParen = 0
    var closeParen = 0
    private fun parseList(list: List<WAE>): Pair<MutableList<Any>, Int> {
        val curList = mutableListOf<Any>()
        var i = 0
        while(i < list.size) {
            val element = list[i]
            if (element is Parenthesis && element.paren == '(') {
                curList.add(element)
                openParen++
                val l1 = parseList(list.subList(i + 1, list.size))
                i += l1.second + l1.first.getSize()
                curList.add(l1.first)
            } else if (element is Parenthesis && element.paren == ')') {
                curList.add(element)
                closeParen++
                return Pair(curList, 0)
            } else {
                curList.add(element)
            }
            i++
        }
        return Pair(curList,0)
    }

    private fun parseToList(s: String) : List<WAE> {
        val arr = s.split("")
        val list = mutableListOf<WAE>()
        var wasSpace = false
        var last: WAE? = null
        for(element in arr) {
            when {
                element.isBlank() -> {
                    wasSpace = true
                }
                "+-/*".contains(element) -> {
                    last = element.toOperation()
                    list.add(last)
                    wasSpace = false
                }
                "()[]".contains(element) -> {
                    last = Parenthesis(element[0])
                    list.add(last)
                    wasSpace = false
                }
                element.toIntOrNull() != null -> {
                    if(!wasSpace && last is Number) {
                        list.removeAt(list.size-1)
                        last = Number((last.number.toString() + element).toInt())
                    } else {
                        last = Number(element.toInt())
                    }
                    list.add(last)
                    wasSpace = false
                }
                else -> {
                    if(!wasSpace && last is Variable) {
                        list.removeAt(list.size-1)
                        last = Variable(last.name + element)
                    } else {
                        last = Variable(element)
                    }

                    if((last as Variable).name == "with") {
                        last = With()
                    }
                    list.add(last)
                    wasSpace = false
                }
            }
        }
        return list
    }

}

private fun <E> List<E>.removeBad(): MutableList<E> {
    val l = mutableListOf<E>()
    for(elem in this){
        if(elem !is Parenthesis) l.add(elem)
    }
    return l
}

private fun <E> List<E>.getSize(): Int {
    var size = 0
    this.forEach {
        if(it !is MutableList<*>) size++
        else size += it.getSize()
    }
    return size
}



fun main(args: Array<String>) {
    val interpreter = Interpreter()
    var z = interpreter.parse("(with ([var (+ 1 2)]) (+ 3 4))")
    testParseSimple(interpreter)
    testParseNested(interpreter)
    testInvalid(interpreter)
    testWith(interpreter)
    println("******************** passed $passed/${passed+failed} test cases! ********************")
}

fun testWith(interpreter: Interpreter) {
    test(interpreter.parse("(with ([var 5)] 6") == null,
        "!!"
    )
}

fun testInvalid(interpreter: Interpreter) {
    var flag = false
    //too many params
    try {
        interpreter.parse("(+ 1 2 3)")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ 1 2 3)")

    flag = false
    try {
        interpreter.parse("(+ (+ 1 2) 3 4)")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ (+ 1 2) 3 4)")

    flag = false
    try {
        interpreter.parse("(+ 1 (+ 2 3) 4)")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ 1 (+ 2 3) 4)")

    flag = false
    try {
        interpreter.parse("(+ 1 2 (+ 3 4))")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ 1 2 (+ 3 4))")

    //not enough params
    flag = false
    try {
        interpreter.parse("(+)")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+)")

    flag = false
    try {
        interpreter.parse("(+ 1)")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ 1)")

    flag = false
    try {
        interpreter.parse("(+ (+ 1) 2)")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ (+ 1) 2)")

    flag = false
    try {
        interpreter.parse("(+ (+ 1 2)")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ (+ 1 2)")

    //invalid paren
    flag = false
    try {
        interpreter.parse("(+ 1 2")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ 1 2")

    flag = false
    try {
        interpreter.parse("(+ 1 (+ 2 3)")
    } catch (e: IllegalArgumentException) {
        flag = true
    }
    test(flag, "(+ 1 (+ 2 3)")
}

fun testParseNested(interpreter: Interpreter) {
    test(
        interpreter.parse("(+ (+ 1 2) (+ 3 4))") ==
                Plus(
                    symbol = '+',
                    lhs = Plus(
                        symbol = '+',
                        lhs = Number(1),
                        rhs = Number(2)
                    ),
                    rhs = Plus(
                        symbol = '+',
                        lhs = Number(3),
                        rhs = Number(4)
                    )
                ),
        "(+ (+ 1 2) (+ 3 4))"
    )
    test(
        interpreter.parse("(- (- 1 2) (- 3 4))") ==
                Minus(
                    symbol = '-',
                    lhs = Minus(
                        symbol = '-',
                        lhs = Number(1),
                        rhs = Number(2)
                    ),
                    rhs = Minus(
                        symbol = '-',
                        lhs = Number(3),
                        rhs = Number(4)
                    )
                ),
        "(- (- 1 2) (- 3 4))"
    )
    test(
        interpreter.parse("(* (* 1 2) (* 3 4))") ==
                Multiply(
                    symbol = '*',
                    lhs = Multiply(
                        symbol = '*',
                        lhs = Number(1),
                        rhs = Number(2)
                    ),
                    rhs = Multiply(
                        symbol = '*',
                        lhs = Number(3),
                        rhs = Number(4)
                    )
                ),
        "(* (* 1 2) (* 3 4))"
    )
    test(
        interpreter.parse("(/ (/ 1 2) (/ 3 4))") ==
                Divide(
                    symbol = '/',
                    lhs = Divide(
                        symbol = '/',
                        lhs = Number(1),
                        rhs = Number(2)
                    ),
                    rhs = Divide(
                        symbol = '/',
                        lhs = Number(3),
                        rhs = Number(4)
                    )
                ),
        "(/ (/ 1 2) (/ 3 4))"
    )

    test(
        interpreter.parse("(+ (- (/ (* 1 2) 5) (/ 6 7)) (+ 3 (* 10 9)))") ==
                Plus(
                symbol = '+',
                lhs = Minus(
                    symbol = '-',
                    lhs = Divide(
                        symbol = '/',
                        lhs = Multiply(
                            symbol = '*',
                            lhs = Number(1),
                            rhs = Number(2)
                        ),
                        rhs = Number(5)
                    ),
                    rhs = Divide(
                        symbol = '/',
                        lhs = Number(6),
                        rhs = Number(7)
                    )
                ),
                rhs = Plus(
                    symbol = '+',
                    lhs = Number(3),
                    rhs = Multiply(
                        symbol = '*',
                        lhs = Number(10),
                        rhs = Number(9)
                    )
                )
            ),
        "(+ (- (/ (* 1 2) 5) (/ 6 7)) (+ 3 (* 10 9)))"
    )
}

fun testParseSimple(interpreter: Interpreter) {
    test(interpreter.parse("(23)") == Number(23),"(23)")
    test(interpreter.parse("23") == Number(23),"23")

    test(
        interpreter.parse("(+ 2 1)") ==
                Plus(
                    symbol = '+',
                    lhs = Number(2),
                    rhs = Number(1)
                ),
        "(+ 2 1)"
    )
    test(
        interpreter.parse("(- 2 1)") ==
                Minus(
                    symbol = '-',
                    lhs = Number(2),
                    rhs = Number(1)
                ),
        "(- 2 1)"
    )
    test(
        interpreter.parse("(* 2 1)") ==
                Multiply(
                    symbol = '*',
                    lhs = Number(2),
                    rhs = Number(1)
                ),
        "(* 2 1)"
    )
    test(
        interpreter.parse("(/ 2 1)") ==
                Divide(
                    symbol = '/',
                    lhs = Number(2),
                    rhs = Number(1)
                ),
        "(/ 2 1)"
    )
}

var passed = 0
var failed = 0
fun test(b: Boolean, s: String) {
    if(!b) {
        failed++
        println("$s failed!")
    } else {
        println("$s passed!")
        passed++
    }
}

