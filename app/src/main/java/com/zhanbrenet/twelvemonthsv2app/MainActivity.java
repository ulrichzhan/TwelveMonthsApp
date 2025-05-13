package com.zhanbrenet.twelvemonthsv2app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zhanbrenet.twelvemonthsv2app.utils.DatabaseHelper;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private TextView randomCity;
    private Button showRandomCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        randomCity = findViewById(R.id.randomCity);
        showRandomCity = findViewById(R.id.showRandomCity);

        showRandomCity.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    List<String> cities = DatabaseHelper.getCities();

                    if (!cities.isEmpty()) {
                        int randomIndex = new Random().nextInt(cities.size());
                        String selectedCity = cities.get(randomIndex);

                        runOnUiThread(() -> {
                            randomCity.setAlpha(0f); // Animation de fondu
                            randomCity.setText(getString(R.string.next_city_text, selectedCity));
                            randomCity.animate().alpha(1f).setDuration(300).start();
                        });
                    } else {
                        runOnUiThread(() -> randomCity.setText(getString(R.string.no_city_found)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> randomCity.setText(getString(R.string.error_fetching_data)));
                }
            }).start();
        });


    }

    public void onMonthClick(View view) {
        Button button = (Button) view;
        String month = button.getTag().toString();

        Intent intent = new Intent(this, MonthDetailActivity.class);
        intent.putExtra("month", month);
        startActivity(intent);
    }

}
