package com.ideasi.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MathChallengeScreen(
    difficulty: MathDifficulty,
    onCorrectAnswer: () -> Unit
) {
    var question by remember {
        mutableStateOf(generateMathQuestion(difficulty))
    }

    var answerText by remember {
        mutableStateOf("")
    }

    var wrongAnswer by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Solve to continue",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = question.question,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        OutlinedTextField(
            value = answerText,
            onValueChange = {
                answerText = it
                wrongAnswer = false
            },
            label = {
                Text("Answer")
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (wrongAnswer) {
            Text(
                text = "Wrong answer. Try another question.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {

                val userAnswer = answerText.toIntOrNull()

                if (userAnswer == question.answer) {
                    onCorrectAnswer()
                } else {
                    question = generateMathQuestion(difficulty)
                    answerText = ""
                    wrongAnswer = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Submit")
        }
    }
}
