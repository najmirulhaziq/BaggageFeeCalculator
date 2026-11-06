package com.example.assign1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final double ECONOMY_LIMIT_KG = 20.0;
    private static final double BUSINESS_LIMIT_KG = 40.0;
    private static final double SERVICE_TAX_RATE = 0.08;

    private static final String STATE_RESULT_SHOWN = "result_shown";
    private static final String STATE_HOW_TO_EXPANDED = "how_to_expanded";

    private TextInputLayout tilWeight, tilRate;
    private TextInputEditText etWeight, etRate;
    private RadioGroup rgClass;
    private RadioButton rbBusiness;
    private TextView tvClassError, tvExcessWeight, tvExcessCharge, tvServiceTax, tvTotal, tvResultClass;
    private ScrollView scrollContent;
    private View cardResult;
    private View cardHowTo, layoutHowToContent;
    private ImageView imgHowToArrow;
    private boolean howToExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        if (getSupportActionBar() != null) {
            // The title is a custom TextView inside the toolbar (see activity_main.xml)
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind views
        tilWeight = findViewById(R.id.til_weight);
        tilRate = findViewById(R.id.til_rate);
        etWeight = findViewById(R.id.et_weight);
        etRate = findViewById(R.id.et_rate);
        rgClass = findViewById(R.id.rg_class);
        rbBusiness = findViewById(R.id.rb_business);
        tvClassError = findViewById(R.id.tv_class_error);
        cardResult = findViewById(R.id.card_result);
        tvExcessWeight = findViewById(R.id.tv_excess_weight);
        tvExcessCharge = findViewById(R.id.tv_excess_charge);
        tvServiceTax = findViewById(R.id.tv_service_tax);
        tvTotal = findViewById(R.id.tv_total);
        tvResultClass = findViewById(R.id.tv_result_class);
        scrollContent = findViewById(R.id.scroll_content);
        cardHowTo = findViewById(R.id.card_how_to);
        layoutHowToContent = findViewById(R.id.layout_how_to_content);
        imgHowToArrow = findViewById(R.id.img_how_to_arrow);
        Button btnCalculate = findViewById(R.id.Btn1);
        Button btnReset = findViewById(R.id.btn_reset);

        // Limit input to at most 2 decimal places
        InputFilter twoDecimals = (source, start, end, dest, dstart, dend) -> {
            String result = dest.subSequence(0, dstart).toString()
                    + source.subSequence(start, end)
                    + dest.subSequence(dend, dest.length());
            return result.matches("\\d*(\\.\\d{0,2})?") ? null : "";
        };
        etWeight.setFilters(new InputFilter[]{twoDecimals});
        etRate.setFilters(new InputFilter[]{twoDecimals});

        // Hide the class error as soon as a class is chosen
        rgClass.setOnCheckedChangeListener((group, checkedId) -> tvClassError.setVisibility(View.GONE));

        btnCalculate.setOnClickListener(this);
        btnReset.setOnClickListener(this);

        // Tap the "How to use" card to expand / collapse it
        cardHowTo.setOnClickListener(v -> setHowToExpanded(!howToExpanded, true));
    }

    // The screen is recreated when the theme changes or the phone rotates,
    // so remember whether a result was showing
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESULT_SHOWN, cardResult.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_HOW_TO_EXPANDED, howToExpanded);
    }

    // Input fields are restored by now, so the result can be recalculated
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        setHowToExpanded(savedInstanceState.getBoolean(STATE_HOW_TO_EXPANDED, false), false);
        if (savedInstanceState.getBoolean(STATE_RESULT_SHOWN, false)) {
            calculate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.About) {
            startActivity(new Intent(this, About.class));
            return true;
        } else if (item.getItemId() == R.id.Settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.Share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "https://github.com/najmirulhaziq/BaggageFeeCalculator");
            shareIntent.putExtra(Intent.EXTRA_TITLE, "Baggage Fee Calculator");
            startActivity(Intent.createChooser(shareIntent, null));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.Btn1) {
            calculate();
        } else if (view.getId() == R.id.btn_reset) {
            resetForm();
        }
    }

    /** Expands or collapses the "How to use" card (with a short animation when animate is true). */
    private void setHowToExpanded(boolean expanded, boolean animate) {
        howToExpanded = expanded;

        if (animate) {
            TransitionManager.beginDelayedTransition((ViewGroup) cardHowTo.getParent(), new AutoTransition());
        }
        layoutHowToContent.setVisibility(expanded ? View.VISIBLE : View.GONE);

        float rotation = expanded ? 180f : 0f;
        if (animate) {
            imgHowToArrow.animate().rotation(rotation).setDuration(200).start();
        } else {
            imgHowToArrow.setRotation(rotation);
        }
    }

    /** Clears all inputs, any error messages and the previous result. */
    private void resetForm() {
        hideKeyboard();

        etWeight.setText("");
        etRate.setText("");
        rgClass.clearCheck();

        tilWeight.setError(null);
        tilRate.setError(null);
        tvClassError.setVisibility(View.GONE);
        cardResult.setVisibility(View.GONE);

        etWeight.clearFocus();
        etRate.clearFocus();
        scrollContent.smoothScrollTo(0, 0);

        Toast.makeText(this, "Inputs have been reset", Toast.LENGTH_SHORT).show();
    }

    private void calculate() {
        hideKeyboard();

        // Reset previous errors / result
        tilWeight.setError(null);
        tilRate.setError(null);
        tvClassError.setVisibility(View.GONE);
        cardResult.setVisibility(View.GONE);

        String weightText = etWeight.getText() == null ? "" : etWeight.getText().toString().trim();
        String rateText = etRate.getText() == null ? "" : etRate.getText().toString().trim();
        boolean classChosen = rgClass.getCheckedRadioButtonId() != -1;

        Double weight = parseNumber(weightText);
        Double rate = parseNumber(rateText);

        boolean valid = true;

        if (weightText.isEmpty()) {
            tilWeight.setError("Please enter the baggage weight");
            valid = false;
        } else if (weight == null) {
            tilWeight.setError("Please enter a valid number");
            valid = false;
        }

        if (!classChosen) {
            tvClassError.setVisibility(View.VISIBLE);
            valid = false;
        }

        if (rateText.isEmpty()) {
            tilRate.setError("Please enter the excess baggage rate");
            valid = false;
        } else if (rate == null) {
            tilRate.setError("Please enter a valid number");
            valid = false;
        }

        if (!valid) {
            Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculation
        double limit = rbBusiness.isChecked() ? BUSINESS_LIMIT_KG : ECONOMY_LIMIT_KG;
        double excessWeight = Math.max(0, weight - limit);
        double excessCharge = excessWeight * rate;
        double serviceTax = SERVICE_TAX_RATE * excessCharge;
        double totalPayable = excessCharge + serviceTax;

        // Show inputs to 2 decimal places
        etWeight.setText(String.format(Locale.US, "%.2f", weight));
        etRate.setText(String.format(Locale.US, "%.2f", rate));

        // Show output to 2 decimal places
        tvResultClass.setText(String.format(Locale.US, "(%s, %.0f kg free)",
                rbBusiness.isChecked() ? "Business" : "Economy", limit));
        tvExcessWeight.setText(String.format(Locale.US, "%.2f kg", excessWeight));
        tvExcessCharge.setText(String.format(Locale.US, "RM %.2f", excessCharge));
        tvServiceTax.setText(String.format(Locale.US, "RM %.2f", serviceTax));
        tvTotal.setText(String.format(Locale.US, "RM %.2f", totalPayable));
        cardResult.setVisibility(View.VISIBLE);

        // If the result is below the visible area, scroll down so it can be seen
        scrollContent.post(() -> scrollContent.smoothScrollTo(0, scrollContent.getChildAt(0).getBottom()));
    }

    /** Returns the parsed number, or null if the text is not a valid number. */
    private Double parseNumber(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null; // e.g. "." on its own
        }
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}