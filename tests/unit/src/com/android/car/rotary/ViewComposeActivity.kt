/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.rotary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat

const val BUTTONA_CONTENT_DESCRIPTION = "ButtonA"
const val BUTTONB_CONTENT_DESCRIPTION = "ButtonB"

class ViewComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(this.window, true)
        setContentView(R.layout.navigator_view_and_compose_activity)
        val composeView: ComposeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            MaterialTheme {
                Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row {
                        Button(
                                onClick = {},
                                modifier = Modifier.semantics() {
                                    contentDescription = BUTTONA_CONTENT_DESCRIPTION
                                }
                        ) { Text(BUTTONA_CONTENT_DESCRIPTION) }
                        Button(
                                onClick = {},
                                modifier = Modifier.semantics() {
                                    contentDescription = BUTTONB_CONTENT_DESCRIPTION
                                }
                        ) { Text(BUTTONB_CONTENT_DESCRIPTION) }
                    }
                }
            }
        }
    }
}
