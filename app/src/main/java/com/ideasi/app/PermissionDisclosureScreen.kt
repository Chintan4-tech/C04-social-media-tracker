package com.ideasi.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionDisclosureScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "How TimeShield Uses Permissions",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Usage Access\n" +
                "TimeShield uses usage information to calculate " +
                "how long you spend in the apps you select."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Accessibility Permission\n" +
                "TimeShield uses Accessibility to detect when " +
                "you open selected apps and attempt to show " +
                "a Math Challenge when you reach your time limit. " +
                "This feature may not work on every device."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TimeShield is designed to use these permissions " +
                "for its app-usage and time-limit features. " +
                "Please review the app's privacy policy to " +
                "understand how your information is handled."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAgree
        ) {
            Text("I Agree and Continue")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDecline
        ) {
            Text("Not Now")
        }
    }
}
