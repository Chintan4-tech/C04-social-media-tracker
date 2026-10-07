package com.ideasi.app

import kotlin.random.Random

enum class MathDifficulty {
    EASY,
    MEDIUM,
    HARD
}

data class MathQuestion(
    val question: String,
    val answer: Int
)

fun generateMathQuestion(
    difficulty: MathDifficulty
): MathQuestion {

    return when (difficulty) {

        MathDifficulty.EASY -> {
            val a = Random.nextInt(10, 100)
            val b = Random.nextInt(10, 100)

            MathQuestion(
                question = "$a × $b",
                answer = a * b
            )
        }

        MathDifficulty.MEDIUM -> {
            val a = Random.nextInt(100, 1000)
            val b = Random.nextInt(100, 1000)

            MathQuestion(
                question = "$a × $b",
                answer = a * b
            )
        }

        MathDifficulty.HARD -> {
            val divisor = Random.nextInt(2, 10)
            val quotient = Random.nextInt(100, 1000)
            val dividend = divisor * quotient

            MathQuestion(
                question = "$dividend ÷ $divisor",
                answer = quotient
            )
        }
    }
}
