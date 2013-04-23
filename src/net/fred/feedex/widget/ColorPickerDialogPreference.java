/**
 * FeedEx
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.feedex.widget;

import net.fred.feedex.R;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ColorPickerDialogPreference extends DialogPreference {
	private SeekBar redSeekBar;

	private SeekBar greenSeekBar;

	private SeekBar blueSeekBar;

	private SeekBar transparencySeekBar;

	int color;

	public ColorPickerDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		color = WidgetProvider.STANDARD_BACKGROUND;
	}

	@Override
	protected View onCreateDialogView() {
		final View view = super.onCreateDialogView();

		view.setBackgroundColor(color);

		redSeekBar = (SeekBar) view.findViewById(R.id.seekbar_red);
		greenSeekBar = (SeekBar) view.findViewById(R.id.seekbar_green);
		blueSeekBar = (SeekBar) view.findViewById(R.id.seekbar_blue);
		transparencySeekBar = (SeekBar) view.findViewById(R.id.seekbar_transparency);

		int _color = color;

		transparencySeekBar.setProgress(((_color / 0x01000000) * 100) / 255);
		_color %= 0x01000000;
		redSeekBar.setProgress(((_color / 0x00010000) * 100) / 255);
		_color %= 0x00010000;
		greenSeekBar.setProgress(((_color / 0x00000100) * 100) / 255);
		_color %= 0x00000100;
		blueSeekBar.setProgress((_color * 100) / 255);

		OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int red = (redSeekBar.getProgress() * 255) / 100;

				int green = (greenSeekBar.getProgress() * 255) / 100;

				int blue = (blueSeekBar.getProgress() * 255) / 100;

				int transparency = (transparencySeekBar.getProgress() * 255) / 100;

				color = transparency * 0x01000000 + red * 0x00010000 + green * 0x00000100 + blue;
				view.setBackgroundColor(color);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		};

		redSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		greenSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		blueSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		transparencySeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		return view;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			persistInt(color);
		}
		super.onDialogClosed(positiveResult);
	}

}
