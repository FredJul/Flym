package ru.yanus171.feedexfork.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import ru.yanus171.feedexfork.R;

public class SendErrorActivity extends Activity {
	public static final String cExceptionTextExtra = "ExceptionTextExtra";

	// --------------------------------------------------------------------------------
	@SuppressWarnings("unused")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String exceptionText = getIntent().getStringExtra(cExceptionTextExtra);

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		// layout.setWeightSum(6);

		title: {
			TextView labelTitle = new TextView(this);
			labelTitle.setText(R.string.criticalErrorOccured);
			//labelTitle.setTextSize(Global.GetViewSmallFontSize());
			//labelTitle.setTextColor(Theme.GetMenuFontColor());
			layout.addView(labelTitle, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
		}

		text: {
			ScrollView scrollView = new ScrollView(this);
			layout.addView(scrollView, new LayoutParams(LayoutParams.FILL_PARENT, 0, 8));

			TextView labelText = new TextView(this);
			labelText.setText(exceptionText);
			//labelText.setTextColor(Theme.GetMenuFontColor());
			//labelText.setTextSize(Global.GetViewSmallFontSize());
			scrollView.addView(labelText, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			// layout.addView(labelText, new LayoutParams(
			// LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 2 ));
		}

		btn: {
			LinearLayout layoutBtn = new LinearLayout(this);
			layoutBtn.setOrientation(LinearLayout.HORIZONTAL);
			layout.addView(layoutBtn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			Button btnSend = new Button( this );
			btnSend.setText(R.string.sendEmail);
			btnSend.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
					emailIntent.setType("plain/text");
					emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "workyalex@mail.ru" });
					emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, GetMailSubject());
					emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, exceptionText);
					startActivity(Intent.createChooser(emailIntent, getString(R.string.criticalErrorSending)));
					finish();
				}

				private String GetMailSubject() {
					String version = "";
					try {
						version = getBaseContext().getPackageManager().getPackageInfo(getBaseContext().getPackageName(),
								0).versionName;
					} catch (NameNotFoundException e) {
					}
					return String.format("HandyClock error stacktrace %s", version);
				}
			});
			layoutBtn.addView(btnSend, new LayoutParams(0, LayoutParams.FILL_PARENT, 1));

			Button btnCopy = new Button( this );
			btnCopy.setText(R.string.copyToClipboard);
			btnCopy.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(exceptionText);
					finish();
				}
			});
			layoutBtn.addView(btnCopy, new LayoutParams(0, LayoutParams.FILL_PARENT, 1));

			Button btnCancel = new Button( this );
			btnCancel.setText(android.R.string.cancel);
			btnCancel.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					finish();
				}
			});
			layoutBtn.addView(btnCancel, new LayoutParams(0, LayoutParams.FILL_PARENT, 1));

		}

		setContentView(layout);
	}
}
