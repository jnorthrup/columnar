package com.fnreport.nards



open class Omega

object nd_ : Omega()

val `Ω` = nd_


val period        by lazy { lit('.') }
val ws        by lazy { regexToken("\\s*".toRegex(),false) }
val  capture by lazy { regexToken(".*".toRegex(),false) }
fun  lit(i: Any) =literalToken(i.toString(), i.toString(),false)

inline infix fun <reified T, P : Parser<T>> Any.`&`(that: Omega) = lit(this) as P
inline infix fun <reified T, P : Parser<T>> Char.`&`(that: Omega) = CharToken(this.toString(), this) as P
inline infix fun <reified T, P : Parser<T>> Char.`&`(that: P) = (CharToken(this.toString(), this) `&` that) as P
inline infix fun <reified T, P : Parser<T>> Char.`?`(that: P) = (CharToken(this.toString(), this) `?` that) as P
inline infix fun <reified T, P : Parser<T>> Char.`|`(that: P) = (CharToken(this.toString(), this) `|` that) as P
inline infix fun <reified T, P : Parser<T>> P.`&`(other: P) = (this and other) as P
inline infix fun <reified T, P : Parser<T>> P.`&`(that: Char) = this `&` CharToken(that.toString(), that) as P
inline infix fun <reified T, P : Parser<T>> P.`&`(that: String) = this `&` lit(that) as P
inline infix fun <reified T, P : Parser<T>> P.`?`(c: Char) = ((this `?` `Ω`) `&` c)
inline infix fun <reified T, P : Parser<T>> P.`?`(c: String) = ((this `?` `Ω`) `&` lit(c)) as P
inline infix fun <reified T, P : Parser<T>> P.`?`(`Ω`: Omega) = optional(this) as P
inline infix fun <reified T, P : Parser<T>> P.`?`(that: P) = (optional(this) `&` that) as P
inline infix fun <reified T, P : Parser<T>> P.`|`(that: P) = OrCombinator(listOf(this, that)) as P
inline infix fun <reified T, P : Parser<T>> P.`|`(that: String) = OrCombinator(listOf(this, lit(that) as P)) as P
inline infix fun <reified T, P : Parser<T>> Regex.`&`(that: Omega): P = (RegexToken(this.toString(), this.toString())) as P
inline infix fun <reified T, P : Parser<T>> String.`&`(that: P) = (lit(this) `&` that) as P
inline infix fun <reified T, P : Parser<T>> String.`?`(that: P) = (lit(this) `?` that) as P
inline infix fun <reified T, P : Parser<T>> String.`|`(that: P) = (lit(this) `|` that) as P
inline infix operator fun <reified T, P : Parser<T>> P.plus(`Ω`: Omega) = oneOrMore(this) as P
inline infix operator fun <reified T, P : Parser<T>> P.plus(that: P) = (oneOrMore(this) `&` that) as P
inline infix operator fun <reified T, P : Parser<T>> P.plus(that: String) = (oneOrMore(this) `&` that) as P
inline infix operator fun <reified T, P : Parser<T>> P.times(`Ω`: Omega) = zeroOrMore(this) as P
inline infix operator fun <reified T, P : Parser<T>> P.times(that: P) = (zeroOrMore(this as P) `&` that) as P
inline infix operator fun <reified T, P : Parser<T>> P.times(that: String) = (zeroOrMore(this) `&` lit(that)) as P
inline infix operator fun <reified T, P : Parser<T>> String.plus(that: P) = oneOrMore(lit(this)) `&` that
inline infix operator fun <reified T, P : Parser<T>> String.times(that: P) = zeroOrMore(lit(this)) `&` that
/*
task ::= [budget] sentence                       (* task to be processed *)

sentence ::= statement"." [tense] [truth]            (* judgement to be absorbed into beliefs *)
| statement"?"            [tense] [truth]            (* question on thuth-value to be answered *)
| statement"!"            [desire]                   (* goal to be realized by operations *)
| statement"@"            [desire]                   (* question on desire-value to be answered *)

statement ::= <"<">term copula term<">">              (* two terms related to each other *)
| <"(">term copula term<")">              (* two terms related to each other, new notation *)
| term                                    (* a term can name a statement *)
| "(^"word {","term} ")"                  (* an operation to be executed *)
| word"("term {","term} ")"               (* an operation to be executed, new notation *)

term ::= word                                    (* an atomic constant term *)
| variable                                (* an atomic variable term *)
| compound-term                           (* a term with internal structure *)
| statement                               (* a statement can serve as a term *)

desire ::= truth                                   (* same format, different interpretations *)
truth ::= <"%">frequency[<";">confidence]<"%">    (* two numbers in [0,1]x(0,1) *)
budget ::= <"$">priority[<";">durability][<";">quality]<"$"> (* three numbers in [0,1]x(0,1)x[0,1] *)

compound-term ::= op-ext-set term {"," term} "}"          (* extensional set *)
| op-int-set term {"," term} "]"          (* intensional set *)
| "("op-multi"," term {"," term} ")"      (* with prefix operator *)
| "("op-single"," term "," term ")"       (* with prefix operator *)
| "(" term {op-multi term} ")"            (* with infix operator *)
| "(" term op-single term ")"             (* with infix operator *)
| "(" term {","term} ")"                  (* product, new notation *)
| "(" op-ext-image "," term {"," term} ")"(* special case, extensional image *)
| "(" op-int-image "," term {"," term} ")"(* special case, \ intensional image *)
| "(" op-negation "," term ")"            (* negation *)
| op-negation term                        (* negation, new notation *)

variable ::= "$"word                                 (* independent variable *)
| "#"word                                 (* dependent variable *)
| "?"word                                 (* query variable in question *)

copula ::= "-->"                                   (* inheritance *)
| "<->"                                   (* similarity *)
| "{--"                                   (* instance *)
| "--]"                                   (* property *)
| "{-]"                                   (* instance-property *)
| "==>"                                   (* implication *)
| "=/>"                                   (* predictive implication *)
| "=|>"                                   (* concurrent implication *)
| "=\\>"                                  (* =\> retrospective implication *)
| "<=>"                                   (* equivalence *)
| "</>"                                   (* predictive equivalence *)
| "<|>"                                   (* concurrent equivalence *)

op-int-set::= "["                                     (* intensional set *)
op-ext-set::= "{"                                     (* extensional set *)
op-negation::= "--"                                    (* negation *)
op-int-image::= "\\"                                    (* \ intensional image *)
op-ext-image::= "/"                                     (* extensional image *)
op-multi ::= "&&"                                    (* conjunction *)
| "*"                                     (* product *)
| "||"                                    (* disjunction *)
| "&|"                                    (* parallel events *)
| "&/"                                    (* sequential events *)
| "|"                                     (* intensional intersection *)
| "&"                                     (* extensional intersection *)
op-single ::= "-"                                     (* extensional difference *)
| "~"                                     (* intensional difference *)

tense ::= ":/:"                                   (* future event *)
| ":|:"                                   (* present event *)
| ":\\:"                                  (* :\: past event *)


word : #"[^\ ]+"                               (* unicode string *)
priority : #"([0]?\.[0-9]+|1\.[0]*|1|0)"           (* 0 <= x <= 1 *)
durability : #"[0]?\.[0]*[1-9]{1}[0-9]*"             (* 0 <  x <  1 *)
quality : #"([0]?\.[0-9]+|1\.[0]*|1|0)"           (* 0 <= x <= 1 *)
frequency : #"([0]?\.[0-9]+|1\.[0]*|1|0)"           (* 0 <= x <= 1 *)
confidence : #"[0]?\.[0]*[1-9]{1}[0-9]*"             (* 0 <  x <  1 *)
*/


enum class accounting(p: Parser<TokenMatch>) : Parser<TokenMatch> by p {
    /** same format, different interpretations */
    desire('%' `&` fragment.frequency `&` ';' `&` fragment.confidence `?` '%'),
    /** two numbers in [0,1]x(0,1) */
    truth('%' `&` fragment.frequency `&` ';' `&` fragment.confidence `?` '%'),
    /** three numbers in [0,1]x(0,1)x[0,1] */
    budget('$' `&` fragment.priority `&` ';' `&` fragment.durability `?` ';' `&` fragment.quality `?` '$');
}


enum class variable(parser: Parser<TokenMatch>) : Parser<TokenMatch> by parser {
    /** independent variable */
    independent_variable(lit('$') `&` fragment.word),
    /** dependent variable */
    dependent_variable(lit('#') `&` fragment.word),
    /** query variable in question */
    query_variable_in_question(lit('?') `&` fragment.word),
}

val fractionalpart = "([01]([.]\\d*)?|[.]\\d{1,})"

enum class fragment(s: String) : Parser<TokenMatch> by regexToken(  s.toRegex(), false) {
    /** unicode string */
    word("[^\\s]+"),
    /** 0 <= x <= 1 */
    priority(fractionalpart),
    /** 0 <  x <  1 */
    durability(fractionalpart),
    /** 0 <= x <= 1 */
    quality(fractionalpart),
    /** 0 <= x <= 1 */
    frequency(fractionalpart),
    /** 0 <  x <  1 */
    confidence(fractionalpart),

}


enum class copula(s: String, symbol: String) : Parser<TokenMatch> by literalToken(symbol, s, false) {
    /*** inheritance*/
    inheritance("-->", "→"),
    /*** similarity*/
    similarity("<->", "↔"),
    /*** instance*/
    instance("{--", "◦→"),
    /*** property*/
    narsproperty("--]", "→◦"),
    /*** instance-property*/
    instance_property("{-]", "◦→◦"),
    /*** implication*/
    implication("==>", "⇒"),
    /*** predictive implication*/
    predictive_implication("=/>", "/⇒"),
    /*** concurrent implication*/
    concurrent_implication("=|>", "|⇒"),
    /*** retrospective implication*/
    retrospective_implication("=\\>", "\\⇒"),
    /*** equivalence*/
    equivalence("<=>", "⇔"),
    /*** predictive equivalence*/
    predictive_equivalence("</>", "/⇔"),
    /*** concurrent equivalence*/
    concurrent_equivalence("<|>", "|⇔"),
    ;

}

enum class term_set(op: String, cl: String) {
    intensional_set("[", "]"),
    extensional_set("{", "}"), ;
}

enum class term_connector(s: Any?, symbol: String? = null, val lit: Token = literalToken(symbol.takeIf { it != null }
        ?: Enum<*>::name as String, s.toString(), false)) : Parser<TokenMatch> by lit {
    negation("--", "¬"),
    intensional_image('\\'),
    extensional_image('/')
}


/** conjunction */
enum class op_multi(s: String, symbol: String) : Parser<TokenMatch> by literalToken(symbol, s, false) {
    conjunction("&&", "∧"),
    /**product*/
    product("*", "×"),
    /**disjunction*/
    disjunction("||", "∨"),
    /**parallel events*/
    parallel_events("&|", ";"),
    /**sequential events*/
    sequential_events("&/", ","),
    /**intensional intersection*/
    intensional_intersection("|", "∪"),
    /**extensional intersection*/
    extensional_intersection("&", "∩"),
    /**placeholder?*/
    image("_", "◇")
}

/**op-single*/
enum class op_single(s: String, symbol: String) : Parser<TokenMatch> by literalToken(symbol, s, false) {

    /**`extensional difference`*/
    extensional_difference("-", "−"),
    /**`intensional difference`*/
    intensional_difference("~", "⦵"),
}


enum class tense(s: String, symbol: String) : Parser<TokenMatch> by literalToken(symbol, s, false) {
    /** future event */
    future_event(":/:", "/⇒"),
    /** present event */
    present_event(":|:", "|⇒"),
    /** :\: past event */
    past_event(":\\:", "\\⇒")
    ;
}


/**
sentence ::= statement"." [tense] [truth]            (* judgement to be absorbed into beliefs *)
| statement"?" [tense] [truth]            (* question on thuth-value to be answered *)
| statement"!" [desire]                   (* goal to be realized by operations *)
| statement"@" [desire]                   (* question on desire-value to be answered *)
 */


enum class sentence(s: Parser<TokenMatch>, symbol: String? = null) : Parser<TokenMatch> by s {
    judgement(capture `&` '.' `&` OrCombinator(tense.values().asList()) `?` accounting.truth `?` `Ω`),
    valuation(capture `&` '?' `&` OrCombinator(tense.values().asList()) `?` accounting.truth `?` `Ω`),
    goal(capture `&` '!' `&` (accounting.desire) `?` `Ω`),
    interest(capture `&` '@' `&` (accounting.desire) `?` `Ω`, "¿"),
    ;
}
