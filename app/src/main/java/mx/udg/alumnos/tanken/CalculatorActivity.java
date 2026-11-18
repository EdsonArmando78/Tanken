package mx.udg.alumnos.tanken;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import java.util.Locale;

public class CalculatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calculator);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button calculate = findViewById(R.id.calcEnter);
        RadioButton calcAdd = findViewById(R.id.calcAdd),
                    calcSub = findViewById(R.id.calcSub),
                    calcMult = findViewById(R.id.calcMult),
                    calcDiv = findViewById(R.id.calcDiv);
        TextInputEditText value1 = findViewById(R.id.editText1),
                          value2 = findViewById(R.id.editText2);
        TextView calcResult = findViewById(R.id.calcResult);
        calculate.setOnClickListener(v -> {
            String v1 = value1.getText().toString();
            String v2 = value2.getText().toString();
            boolean isDv1 = isDouble(v1), isDv2 = isDouble(v2);
            try {
                double i1 = Double.parseDouble(v1),
                        i2 = Double.parseDouble(v2);

                if (calcAdd.isChecked()) {
                    calcResult.setText(numToStr(i1 + i2, isDv1 || isDv2));
                } else if (calcSub.isChecked()) {
                    calcResult.setText(numToStr(i1 - i2, isDv1 || isDv2));
                } else if (calcMult.isChecked()) {
                    calcResult.setText(numToStr(i1 * i2, isDv1 || isDv2));
                } else if (calcDiv.isChecked()) {
                    if (i2 != 0.0f)
                        calcResult.setText(numToStr(i1 / i2, isDv1 || isDv2));
                    else
                        calcResult.setText(R.string.calculator_undefined);
                }
            } catch (NumberFormatException ignored) {
                calcResult.setText(R.string.calculator_unavailable);
            }
        });
    }

    private boolean isDouble(String v1) {
        return v1.contains(".") && !v1.startsWith(".") && !v1.endsWith(".");
    }

    private String numToStr(double num, boolean isDouble) {
        return isDouble ? String.format(Locale.getDefault(), "%.2f", num)
                : String.format(Locale.getDefault(), "%d", (int) num);
    }
}
