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

package com.android.car.rotary;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

/** An activity used for testing {@link com.android.car.rotary.Navigator}. */
public class WebViewTestActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.navigator_webview_activity);

        WebView webView = findViewById(R.id.web_view);
        Resources res = getResources();
        InputStream inputStream = res.openRawResource(R.raw.web_view_html);
        byte[] byteArray = new byte[0];
        try {
            byteArray = new byte[inputStream.available()];
            inputStream.read(byteArray);
        } catch (IOException e) {
            Log.w("WebViewFragment", "Can't read HTML");
        }
        String webViewHtml = new String(byteArray);
        String encodedHtml = Base64.encodeToString(webViewHtml.getBytes(), Base64.NO_PADDING);
        webView.loadData(encodedHtml, "text/html", "base64");
    }
}
