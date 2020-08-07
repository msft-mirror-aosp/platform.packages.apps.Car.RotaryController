/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD;

import static com.android.car.rotary.Utils.FOCUS_AREA_CLASS_NAME;
import static com.android.car.rotary.Utils.FOCUS_PARKING_VIEW_CLASS_NAME;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_HIGHLIGHT_BOTTOM_PADDING;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_HIGHLIGHT_LEFT_PADDING;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_HIGHLIGHT_RIGHT_PADDING;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_HIGHLIGHT_TOP_PADDING;
import static com.android.car.ui.utils.RotaryConstants.ROTARY_VERTICALLY_SCROLLABLE;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class NodeBuilderTest {

    private static final String CLASS_NAME = "class_name";
    private static final String CONTENT_DESCRIPTION = "content_description";

    private NodeBuilder mNodeBuilder;

    @Before
    public void setUp() {
        mNodeBuilder = new NodeBuilder(new ArrayList<>());
    }

    @Test
    public void testBuildDefaultNode() {
        AccessibilityNodeInfo node = mNodeBuilder.build();
        assertThat(node.isFocusable()).isTrue();
        assertThat(node.isVisibleToUser()).isTrue();
        assertThat(node.refresh()).isTrue();
        assertThat(node.isEnabled()).isTrue();
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        assertThat(bounds.isEmpty()).isFalse();
    }

    @Test
    public void testSetFocusable() {
        AccessibilityNodeInfo node = mNodeBuilder.setFocusable(false).build();
        assertThat(node.isFocusable()).isFalse();
    }

    @Test
    public void testSetVisibleToUser() {
        AccessibilityNodeInfo node = mNodeBuilder.setVisibleToUser(false).build();
        assertThat(node.isVisibleToUser()).isFalse();
    }

    @Test
    public void testSetInViewTree() {
        AccessibilityNodeInfo node = mNodeBuilder.setInViewTree(false).build();
        assertThat(node.refresh()).isFalse();
    }

    @Test
    public void testSetEnabled() {
        AccessibilityNodeInfo node = mNodeBuilder.setEnabled(false).build();
        assertThat(node.isEnabled()).isFalse();
    }

    @Test
    public void testSetWindow() {
        AccessibilityWindowInfo window = new WindowBuilder().build();
        AccessibilityNodeInfo node = mNodeBuilder.setWindow(window).build();
        assertThat(node.getWindow()).isSameAs(window);
    }

    @Test
    public void testSetBoundsInScreen() {
        Rect setBounds = new Rect(100, 200, 300, 400);
        AccessibilityNodeInfo node = mNodeBuilder.setBoundsInScreen(setBounds).build();
        Rect retrievedBounds = new Rect();
        node.getBoundsInScreen(retrievedBounds);
        assertThat(retrievedBounds).isEqualTo(setBounds);
    }

    @Test
    public void testSetClassName() {
        AccessibilityNodeInfo node = mNodeBuilder.setClassName(CLASS_NAME).build();
        assertThat(node.getClassName().toString()).isEqualTo(CLASS_NAME);
    }

    @Test
    public void testSetContentDescription() {
        AccessibilityNodeInfo node =
                mNodeBuilder.setContentDescription(CONTENT_DESCRIPTION).build();
        assertThat(node.getContentDescription().toString()).isEqualTo(CONTENT_DESCRIPTION);
    }

    @Test
    public void testSetParent() {
        AccessibilityNodeInfo parent = mNodeBuilder.build();
        AccessibilityNodeInfo child1 = mNodeBuilder.setParent(parent).build();
        AccessibilityNodeInfo child2 = mNodeBuilder.setParent(parent).build();

        assertThat(child1.getParent()).isSameAs(parent);
        assertThat(parent.getChildCount()).isEqualTo(2);
        assertThat(parent.getChild(0)).isSameAs(child1);
        assertThat(parent.getChild(1)).isSameAs(child2);
        assertThat(parent.getChild(2)).isNull();
    }

    @Test
    public void testSetActions() {
        AccessibilityNodeInfo node = mNodeBuilder.setActions(ACTION_SCROLL_FORWARD).build();
        assertThat(node.getActionList()).containsExactly(ACTION_SCROLL_FORWARD);
    }

    @Test
    public void testSetFocusArea() {
        AccessibilityNodeInfo node = mNodeBuilder.setFocusArea().build();
        assertThat(node.getClassName().toString()).isEqualTo(FOCUS_AREA_CLASS_NAME);
        assertThat(node.isFocusable()).isFalse();
    }

    @Test
    public void testSetFocusAreaHighlightPadding() {
        int left = 10;
        int top = 20;
        int right = 30;
        int bottom = 40;
        AccessibilityNodeInfo node = mNodeBuilder
                .setFocusAreaHighlightPadding(left, top, right, bottom)
                .build();
        Bundle extras = node.getExtras();
        assertThat(extras.getInt(FOCUS_AREA_HIGHLIGHT_LEFT_PADDING)).isEqualTo(left);
        assertThat(extras.getInt(FOCUS_AREA_HIGHLIGHT_TOP_PADDING)).isEqualTo(top);
        assertThat(extras.getInt(FOCUS_AREA_HIGHLIGHT_RIGHT_PADDING)).isEqualTo(right);
        assertThat(extras.getInt(FOCUS_AREA_HIGHLIGHT_BOTTOM_PADDING)).isEqualTo(bottom);
    }

    @Test
    public void testSetFpv() {
        AccessibilityNodeInfo node = mNodeBuilder.setFpv().build();
        assertThat(node.getClassName().toString()).isEqualTo(FOCUS_PARKING_VIEW_CLASS_NAME);
    }

    @Test
    public void testSetScrollableContainer() {
        AccessibilityNodeInfo node = mNodeBuilder.setScrollableContainer().build();
        assertThat(node.getContentDescription().toString()).isEqualTo(ROTARY_VERTICALLY_SCROLLABLE);
    }
}
