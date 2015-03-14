/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.fred.feedex.widget;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import net.fred.feedex.R;

public class ColorPickerDialogPreference extends DialogPreference {

    private SeekBar mRedSeekBar;
    private SeekBar mGreenSeekBar;
    private SeekBar mBlueSeekBar;
    private SeekBar mTransparencySeekBar;

    private int mColor;

    public ColorPickerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mColor = WidgetProvider.STANDARD_BACKGROUND;
    }

    @Override
    protected View onCreateDialogView() {
        final View view = super.onCreateDialogView();

        view.setBackgroundColor(mColor);

        mRedSeekBar = (SeekBar) view.findViewById(R.id.seekbar_red);
        mGreenSeekBar = (SeekBar) view.findViewById(R.id.seekbar_green);
        mBlueSeekBar = (SeekBar) view.findViewById(R.id.seekbar_blue);
        mTransparencySeekBar = (SeekBar) view.findViewById(R.id.seekbar_transparency);

        int _color = mColor;

        mTransparencySeekBar.setProgress(((_color / 0x01000000) * 100) / 255);
        _color %= 0x01000000;
        mRedSeekBar.setProgress(((_color / 0x00010000) * 100) / 255);
        _color %= 0x00010000;
        mGreenSeekBar.setProgress(((_color / 0x00000100) * 100) / 255);
        _color %= 0x00000100;
        mBlueSeekBar.setProgress((_color * 100) / 255);

        OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int red = (mRedSeekBar.getProgress() * 255) / 100;

                int green = (mGreenSeekBar.getProgress() * 255) / 100;

                int blue = (mBlueSeekBar.getProgress() * 255) / 100;

                int transparency = (mTransparencySeekBar.getProgress() * 255) / 100;

                mColor = transparency * 0x01000000 + red * 0x00010000 + green * 0x00000100 + blue;
                view.setBackgroundColor(mColor);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        mRedSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mGreenSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mBlueSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mTransparencySeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistInt(mColor);
        }
        super.onDialogClosed(positiveResult);
    }

}
