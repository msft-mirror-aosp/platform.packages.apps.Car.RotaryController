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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.accessibilityClassName
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.car.ui.FocusArea

const val LEFT_FOCUS_AREA_CONTENT_DESCRIPTION = "left_focus_area"
const val RIGHT_FOCUS_AREA_CONTENT_DESCRIPTION = "right_focus_area"

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row {
                        FocusArea(LEFT_FOCUS_AREA_CONTENT_DESCRIPTION)
                        FocusArea(RIGHT_FOCUS_AREA_CONTENT_DESCRIPTION)
                    }
                }
            }
        }
    }
}

@Composable
fun FocusArea(name: String) {
    Box (
        Modifier
        .semantics {
            accessibilityClassName = FocusArea::class.qualifiedName!!
            contentDescription = name
        }
        .padding(horizontal = 10.dp, vertical = 200.dp)
    ){
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text(name)
        }
    }
}
