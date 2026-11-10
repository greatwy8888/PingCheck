package com.greatwy.hisenseenglishquiz;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;
import java.util.Random;

public class QuizActivity extends Activity {
    int q = 0, score = 0, selected = 0;
    TextView title, progress, feedback;
    Button[] btns = new Button[4];
    Word word;

    static class Word {
        String en;
        String[] o;
        Word(String e, String a, String b, String c, String d) {
            en = e;
            o = new String[]{a, b, c, d};
        }
    }

    Word[] words = {
        new Word("Apple","苹果","香蕉","橘子","葡萄"),
        new Word("Book","书","桌子","椅子","门"),
        new Word("Cat","猫","狗","鸟","鱼"),
        new Word("Water","水","火","米饭","牛奶"),
        new Word("Thank you","谢谢","对不起","你好","再见"),
        new Word("House","房子","学校","医院","商店"),
        new Word("Dog","狗","猫","马","兔子"),
        new Word("Red","红色","蓝色","绿色","黄色"),
        new Word("Good morning","早上好","晚安","再见","谢谢"),
        new Word("School","学校","公园","车站","厨房"),
        new Word("Family","家庭","朋友","老师","学生"),
        new Word("Milk","牛奶","果汁","水","茶"),
        new Word("Friend","朋友","医生","司机","警察"),
        new Word("Sun","太阳","月亮","星星","云"),
        new Word("Happy","快乐的","伤心的","疲倦的","生气的"),
        new Word("Goodbye","再见","你好","谢谢","早上好"),
        new Word("Big","大的","小的","高的","短的"),
        new Word("Small","小的","大的","新的","旧的"),
        new Word("Pen","钢笔","书","桌子","椅子"),
        new Word("Car","汽车","火车","飞机","自行车"),
        new Word("Mother","妈妈","爸爸","姐姐","弟弟"),
        new Word("Father","爸爸","妈妈","老师","朋友"),
        new Word("One","一","二","三","四"),
        new Word("Two","二","一","三","五"),
        new Word("Hello","你好","再见","谢谢","对不起")
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(4 | 2 | 4096);
        startService(new Intent(this, QuizTimerService.class));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(30, 15, 30, 15);
        root.setBackgroundColor(Color.rgb(18, 45, 82));

        title = label("英语闯关", 36, Color.WHITE, true);
        progress = label("", 22, Color.WHITE, false);
        feedback = label("请用遥控器方向键选择，再按 OK 确认", 22, Color.YELLOW, false);
        root.addView(title);
        root.addView(progress);
        root.addView(feedback);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setFocusable(false);
        for (int i = 0; i < 4; i++) {
            final int x = i;
            btns[i] = new Button(this);
            btns[i].setTextSize(22);
            btns[i].setTextColor(Color.WHITE);
            btns[i].setAllCaps(false);
            btns[i].setFocusable(true);
            btns[i].setFocusableInTouchMode(true);
            btns[i].setClickable(true);
            btns[i].setOnClickListener(v -> {
                selected = x;
                answer(x);
            });
            btns[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    selected = x;
                    styleButton(x, true);
                } else {
                    styleButton(x, false);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 110, 1);
            lp.setMargins(6, 15, 6, 15);
            row.addView(btns[i], lp);
        }
        root.addView(row);
        setContentView(root);
        next();
        selected = 0;
        root.post(() -> {
            btns[0].requestFocus();
            styleAllButtons();
        });
    }

    TextView label(String s, int z, int c, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(z);
        t.setTextColor(c);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(8, 10, 8, 10);
        return t;
    }

    GradientDrawable background(int color, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(14);
        d.setStroke(strokeWidth, strokeColor);
        return d;
    }

    void styleButton(int i, boolean focused) {
        if (btns[i] == null) return;
        if (focused) {
            btns[i].setBackground(background(Color.rgb(255, 190, 35), Color.WHITE, 4));
            btns[i].setTextColor(Color.rgb(18, 45, 82));
            btns[i].setScaleX(1.06f);
            btns[i].setScaleY(1.06f);
        } else {
            btns[i].setBackground(background(Color.rgb(35, 94, 155), Color.rgb(120, 175, 225), 2));
            btns[i].setTextColor(Color.WHITE);
            btns[i].setScaleX(1.0f);
            btns[i].setScaleY(1.0f);
        }
    }

    void styleAllButtons() {
        for (int i = 0; i < btns.length; i++) {
            if (btns[i] != null) styleButton(i, i == selected && btns[i].hasFocus());
        }
    }

    void moveSelection(int direction) {
        int next = (selected + direction + btns.length) % btns.length;
        if (btns[next].getVisibility() != View.VISIBLE) return;
        // Keep answer buttons in fixed positions; move only the focus highlight.
        btns[next].requestFocus();
        selected = next;
        styleAllButtons();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
                moveSelection(-1);
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                moveSelection(1);
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_UP) {
                moveSelection(-2);
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                moveSelection(2);
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                code == KeyEvent.KEYCODE_ENTER ||
                code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                btns[selected].setPressed(true);
                feedback.setText("已按下：" + btns[selected].getText());
                return true;
            }
        } else if (e.getAction() == KeyEvent.ACTION_UP &&
                (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                 code == KeyEvent.KEYCODE_ENTER ||
                 code == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
            btns[selected].setPressed(false);
            styleButton(selected, true);
            answer(selected);
            return true;
        }
        if (code == KeyEvent.KEYCODE_BACK) {
            feedback.setText("请先答对 5 道题");
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    void next() {
        if (q >= 5) {
            title.setText("全部答对！");
            feedback.setText("即将继续使用电视");
            for (Button b : btns) b.setVisibility(View.GONE);
            getWindow().getDecorView().postDelayed(() -> {
                getSharedPreferences("quiz", 0).edit()
                    .putLong("next_due", System.currentTimeMillis() + 900000L).apply();
                finish();
            }, 1500);
            return;
        }
        word = words[new Random().nextInt(words.length)];
        title.setText("第 " + (q + 1) + " 题：" + word.en);
        progress.setText("答对 " + score + " / 5");
        feedback.setText("方向键移动黄色选框，按 OK 选择答案");
        String[] a = word.o.clone();
        Random r = new Random();
        for (int i = 3; i > 0; i--) {
            int j = r.nextInt(i + 1);
            String t = a[i]; a[i] = a[j]; a[j] = t;
        }
        for (int i = 0; i < 4; i++) {
            btns[i].setVisibility(View.VISIBLE);
            btns[i].setText(a[i]);
            btns[i].setTag(a[i].equals(word.o[0]));
        }
        styleAllButtons();
        if (btns[selected].getVisibility() == View.VISIBLE) {
            btns[selected].requestFocus();
            styleButton(selected, true);
        }
    }

    void answer(int i) {
        if (i < 0 || i >= btns.length || btns[i].getVisibility() != View.VISIBLE) return;
        if (Boolean.TRUE.equals(btns[i].getTag())) {
            q++;
            score++;
            feedback.setText("回答正确！");
            progress.setText("答对 " + score + " / 5");
            next();
        } else {
            feedback.setText("回答错误，请再试一次");
            progress.setText("答对 " + score + " / 5");
            btns[i].setBackground(background(Color.rgb(220, 65, 65), Color.WHITE, 4));
            btns[i].postDelayed(() -> styleButton(i, btns[i].hasFocus()), 450);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Android 5.1 supports screen pinning/lock-task entry. On a TV that
        // is not device-owner managed, the system may still allow an exit gesture.
        try {
            startLockTask();
        } catch (SecurityException ignored) {
            // Keep the quiz usable if the TV firmware blocks lock-task mode.
        } catch (IllegalStateException ignored) {
            // Some vendor TV firmware does not implement pinning normally.
        }
    }

    @Override
    public void onBackPressed() {
        feedback.setText("请先答对 5 道题");
    }
}
