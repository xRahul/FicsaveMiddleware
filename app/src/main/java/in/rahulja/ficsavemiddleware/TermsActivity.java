package in.rahulja.ficsavemiddleware;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TermsActivity extends AppCompatActivity {

  private SharedPreferences sharedPreferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_terms);

    String htmlAsString = getString(R.string.privacy_policy_html);
    Spanned htmlAsSpanned;

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      htmlAsSpanned = Html.fromHtml(htmlAsString, Html.FROM_HTML_MODE_LEGACY);
    } else {
      htmlAsSpanned = Html.fromHtml(htmlAsString);
    }

    // set the html content on a TextView
    TextView textView = findViewById(R.id.textView_privacy_policy);
    textView.setText(htmlAsSpanned);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    Button ok = findViewById(R.id.privacy_policy_button);

    ok.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        sharedPreferences.edit().putBoolean("terms_shown", true).apply();
        finish();
      }
    });
  }
}
