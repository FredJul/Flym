package ru.yanus171.feedexfork.view;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public final class TapZonePreviewPreference extends DialogPreference {
    public TapZonePreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView () {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_entry, null, false);
        SetupZoneSizes(view);
        return view;
    }

    public static void SetupZoneSizes(View view) {
        final int size = UiUtils.mmToPixel(Integer.parseInt( PrefUtils.getString( "tap_zone_size", "7" ) ));
        SetSize( view, R.id.pageUpBtn, MATCH_PARENT, size );
        SetSize( view, R.id.pageDownBtn, MATCH_PARENT, size );
        SetSize( view, R.id.pageDownBtnVert, size, MATCH_PARENT );
        SetSize( view, R.id.brightnessSlider, size, MATCH_PARENT );
        SetSize( view, R.id.toggleFullScreenStatusBarBtn, size, size );
        SetSize( view, R.id.toggleFullscreenBtn, size, size );
    }

    private static void SetSize( View parent, int ID, int width, int height ) {
        View view = parent.findViewById( ID );
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
        lp.width = width;
        lp.height = height;
    }
}
