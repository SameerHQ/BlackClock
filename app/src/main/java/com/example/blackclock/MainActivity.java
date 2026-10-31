
package com.example.blackclock;

import android.app.AlertDialog;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView clockText;

    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private GameTimer timer1;
    private GameTimer timer2;

    private Ringtone alarmRingtone;

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (clockText != null) {
                clockText.setText(new SimpleDateFormat(
                        "hh:mm:ss a", Locale.US
                ).format(new Date()));
            }

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        );

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_main);

        clockText = findViewById(R.id.clockText);

        Button timer1Button = findViewById(R.id.timer1Button);
        Button timer2Button = findViewById(R.id.timer2Button);

        timer1 = new GameTimer(1, timer1Button);
        timer2 = new GameTimer(2, timer2Button);

        timer1Button.setOnClickListener(v -> showTimerDialog(timer1));
        timer2Button.setOnClickListener(v -> showTimerDialog(timer2));

        handler.post(clockRunnable);
    }

    private void showTimerDialog(GameTimer timer) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 12, 24, 8);

        TextView heading = new TextView(this);
        heading.setText("TIMER " + timer.number);
        heading.setTextSize(20);
        heading.setGravity(Gravity.CENTER);
        layout.addView(heading);

        TextView remaining = new TextView(this);
        remaining.setTextSize(30);
        remaining.setGravity(Gravity.CENTER);
        remaining.setTypeface(
                android.graphics.Typeface.MONOSPACE,
                android.graphics.Typeface.BOLD
        );
        layout.addView(remaining);

        LinearLayout inputs = new LinearLayout(this);
        inputs.setGravity(Gravity.CENTER);

        EditText hoursInput = createInput("Hours", "0");
        EditText minutesInput = createInput("Minutes", "5");
        EditText secondsInput = createInput("Seconds", "0");

        inputs.addView(hoursInput,
                new LinearLayout.LayoutParams(0, 60, 1));
        inputs.addView(minutesInput,
                new LinearLayout.LayoutParams(0, 60, 1));
        inputs.addView(secondsInput,
                new LinearLayout.LayoutParams(0, 60, 1));

        layout.addView(inputs);

        Button startButton = new Button(this);
        startButton.setText(timer.running ? "PAUSE" : "START");
        layout.addView(startButton);

        Button resetButton = new Button(this);
        resetButton.setText("RESET");
        layout.addView(resetButton);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Set Timer Duration")
                .setView(layout)
                .setNegativeButton("CLOSE", null)
                .create();

        Runnable updateRemaining = () ->
                remaining.setText(timer.formatTime(timer.remainingMillis));

        if (timer.remainingMillis > 0) {
            long total = (timer.remainingMillis + 999) / 1000;

            hoursInput.setText(String.valueOf(total / 3600));
            minutesInput.setText(
                    String.valueOf((total % 3600) / 60));
            secondsInput.setText(String.valueOf(total % 60));
        }

        updateRemaining.run();

        Runnable updateInputState = () -> {
            boolean enabled = !timer.running;
            hoursInput.setEnabled(enabled);
            minutesInput.setEnabled(enabled);
            secondsInput.setEnabled(enabled);
        };

        startButton.setOnClickListener(v -> {
            if (timer.running) {
                timer.pause();
                startButton.setText("RESUME");
                updateInputState.run();
                updateRemaining.run();
                return;
            }

            if (timer.remainingMillis <= 0) {
                try {
                    int hours = Integer.parseInt(
                            hoursInput.getText().toString().trim());
                    int minutes = Integer.parseInt(
                            minutesInput.getText().toString().trim());
                    int seconds = Integer.parseInt(
                            secondsInput.getText().toString().trim());

                    if (hours < 0 || hours > 99 ||
                            minutes < 0 || minutes > 59 ||
                            seconds < 0 || seconds > 59 ||
                            hours * 3600L + minutes * 60L + seconds == 0) {

                        Toast.makeText(this,
                                "Enter a valid time",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    timer.remainingMillis =
                            (hours * 3600L + minutes * 60L + seconds)
                                    * 1000L;

                } catch (NumberFormatException e) {
                    Toast.makeText(this,
                            "Enter valid numbers",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            timer.start(() -> {
                updateRemaining.run();
                startButton.setText("START");
                updateInputState.run();

                Toast.makeText(this,
                        "Timer " + timer.number + " finished!",
                        Toast.LENGTH_LONG).show();
            }, updateRemaining);

            startButton.setText("PAUSE");
            updateInputState.run();
        });

        resetButton.setOnClickListener(v -> {
            timer.reset();

            hoursInput.setEnabled(true);
            minutesInput.setEnabled(true);
            secondsInput.setEnabled(true);

            hoursInput.setText("0");
            minutesInput.setText("5");
            secondsInput.setText("0");

            startButton.setText("START");
            updateRemaining.run();
        });

        dialog.show();
    }

    private EditText createInput(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(value);
        input.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER);
        input.setSelectAllOnFocus(true);
        return input;
    }

    private void playTimerSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_ALARM);

            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION);
            }

            if (alarmUri == null) {
                Toast.makeText(this,
                        "Timer finished!",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (alarmRingtone != null && alarmRingtone.isPlaying()) {
                alarmRingtone.stop();
            }

            alarmRingtone =
                    RingtoneManager.getRingtone(this, alarmUri);

            if (alarmRingtone != null) {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    alarmRingtone.setLooping(true);
                }

                alarmRingtone.play();
            }
        } catch (Exception e) {
            Toast.makeText(this,
                    "Timer finished!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void stopTimerSound() {
        if (alarmRingtone != null && alarmRingtone.isPlaying()) {
            alarmRingtone.stop();
        }
    }

    private class GameTimer {
        final int number;
        final Button button;

        long remainingMillis = 0;
        boolean running = false;
        CountDownTimer countdown;

        GameTimer(int number, Button button) {
            this.number = number;
            this.button = button;
            updateButton();
        }

        void start(Runnable finished, Runnable tick) {
            if (countdown != null) {
                countdown.cancel();
            }

            running = true;
            updateButton();

            countdown = new CountDownTimer(remainingMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    remainingMillis = millisUntilFinished;
                    updateButton();
                    tick.run();
                }

                @Override
                public void onFinish() {
                    remainingMillis = 0;
                    running = false;
                    countdown = null;
                    updateButton();

                    playTimerSound();
                    finished.run();
                }
            }.start();
        }

        void pause() {
            if (countdown != null) {
                countdown.cancel();
                countdown = null;
            }

            running = false;
            updateButton();
        }

        void reset() {
            if (countdown != null) {
                countdown.cancel();
                countdown = null;
            }

            running = false;
            remainingMillis = 0;
            updateButton();
            stopTimerSound();
        }

        void updateButton() {
            if (running || remainingMillis > 0) {
                button.setText("TIMER " + number + ": "
                        + formatTime(remainingMillis));
            } else {
                button.setText("TIMER " + number);
            }
        }

        String formatTime(long millis) {
            long totalSeconds = (millis + 999) / 1000;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            return String.format(Locale.US, "%02d:%02d:%02d",
                    hours, minutes, seconds);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(clockRunnable);

        if (timer1 != null) timer1.reset();
        if (timer2 != null) timer2.reset();

        stopTimerSound();

        super.onDestroy();
    }
}