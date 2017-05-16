package com.kircherelectronics.gyroscopeexplorer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/*
 * Copyright 2013-2017, Kaleb Kircher - Kircher Engineering, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A special button that scales the button height to 1/3rd of the button width.
 * This allows you to define the width of the button as a weight and then scale
 * the height accordingly.
 *
 * @author Kaleb
 */
public class BackgroundButton extends Button {

    public BackgroundButton(Context context) {
        super(context);
    }

    public BackgroundButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public BackgroundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int chosenWidth = chooseDimension(widthMode, widthSize);

        setMeasuredDimension(chosenWidth, chosenWidth / 3);
    }

    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            return getPreferredSize();
        }
    }

    /**
     * In case there is no size specified.
     *
     * @return default preferred size.
     */
    private int getPreferredSize() {
        return 800;
    }
}
