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

    // 初中一年级难度：常见词汇、校园生活、日常表达与基础形容词/副词
    Word[] words = {
        new Word("Apple","苹果","香蕉","橘子","葡萄"),
        new Word("Library","图书馆","实验室","操场","餐厅"),
        new Word("Breakfast","早餐","午餐","晚餐","点心"),
        new Word("Subject","学科","假期","作业","考试"),
        new Word("Geography","地理","历史","数学","音乐"),
        new Word("History","历史","地理","科学","体育"),
        new Word("Science","科学","英语","美术","语文"),
        new Word("Difficult","困难的","容易的","有趣的","重要的"),
        new Word("Interesting","有趣的","无聊的","昂贵的","危险的"),
        new Word("Usually","通常","从不","很少","已经"),
        new Word("Sometimes","有时","总是","从不","每天"),
        new Word("Always","总是","有时","很少","从不"),
        new Word("Never","从不","经常","通常","有时"),
        new Word("Between","在……之间","在……后面","在……上面","在……里面"),
        new Word("Behind","在……后面","在……前面","在……之间","在……下面"),
        new Word("Because","因为","但是","所以","虽然"),
        new Word("Healthy","健康的","生病的","疲倦的","饥饿的"),
        new Word("Exercise","锻炼；练习","旅行","购物","睡觉"),
        new Word("Favorite","最喜欢的","最差的","最新的","最老的"),
        new Word("Borrow","借入","归还","购买","丢失"),
        new Word("Arrive","到达","离开","忘记","等待"),
        new Word("Leave","离开","到达","开始","完成"),
        new Word("Spend","花费；度过","借入","回答","寻找"),
        new Word("Practice","练习","解释","邀请","打扫"),
        new Word("Together","一起","独自","很少","突然"),
        new Word("Different","不同的","相同的","困难的","安静的"),
        new Word("Important","重要的","无聊的","便宜的","危险的"),
        new Word("Expensive","昂贵的","便宜的","免费的","干净的"),
        new Word("Cheap","便宜的","昂贵的","困难的","友好的"),
        new Word("Sweater","毛衣","衬衫","短裤","帽子"),
        new Word("Trousers","裤子","袜子","手套","围巾"),
        new Word("Shoes","鞋","帽子","眼镜","手表"),
        new Word("Weather","天气","季节","日期","时间"),
        new Word("Cloudy","多云的","晴朗的","有风的","下雪的"),
        new Word("Windy","有风的","多云的","炎热的","寒冷的"),
        new Word("Rainy","下雨的","晴朗的","干燥的","温暖的"),
        new Word("Season","季节","天气","月份","星期"),
        new Word("Spring","春天","夏天","秋天","冬天"),
        new Word("Summer","夏天","春天","秋天","冬天"),
        new Word("Autumn","秋天","春天","夏天","冬天"),
        new Word("Winter","冬天","春天","夏天","秋天"),
        new Word("Holiday","假期","课程","会议","作业"),
        new Word("Festival","节日","车站","医院","超市"),
        new Word("Question","问题","答案","故事","消息"),
        new Word("Answer","答案","问题","地址","原因"),
        new Word("Understand","理解","忘记","借入","到达"),
        new Word("Remember","记得","忘记","离开","练习"),
        new Word("Forget","忘记","记得","理解","解释"),
        new Word("Problem","问题；难题","答案","假期","天气"),
        new Word("Message","消息","杂志","报纸","地图"),
        new Word("Email","电子邮件","电视机","词典","相机"),
        new Word("Computer","电脑","冰箱","自行车","吉他"),
        new Word("Internet","互联网","图书馆","电影院","操场"),
        new Word("Newspaper","报纸","字典","杂志","明信片"),
        new Word("Magazine","杂志","报纸","字典","菜单"),
        new Word("Cinema","电影院","博物馆","医院","车站"),
        new Word("Museum","博物馆","超市","餐厅","银行"),
        new Word("Hospital","医院","学校","图书馆","公园"),
        new Word("Station","车站","厨房","卧室","花园"),
        new Word("Supermarket","超市","电影院","博物馆","操场"),
        new Word("Restaurant","餐馆","图书馆","办公室","医院"),
        new Word("Kitchen","厨房","卧室","浴室","客厅"),
        new Word("Bedroom","卧室","厨房","花园","车站"),
        new Word("Bathroom","浴室","教室","餐厅","操场"),
        new Word("Parents","父母","朋友","邻居","同学"),
        new Word("Cousin","堂/表兄弟姐妹","叔叔","祖父","孙子"),
        new Word("Uncle","叔叔；舅舅","阿姨","表兄弟","祖母"),
        new Word("Aunt","阿姨；姑姑","叔叔","兄弟","祖父"),
        new Word("Daughter","女儿","儿子","母亲","父亲"),
        new Word("Son","儿子","女儿","姐姐","奶奶"),
        new Word("Grandfather","祖父；外祖父","父亲","叔叔","兄弟"),
        new Word("Grandmother","祖母；外祖母","母亲","阿姨","女儿"),
        new Word("Friendly","友好的","危险的","昂贵的","困难的"),
        new Word("Careful","小心的","粗心的","饥饿的","兴奋的"),
        new Word("Dangerous","危险的","安全的","健康的","便宜的"),
        new Word("Popular","受欢迎的","困难的","安静的","昂贵的"),
        new Word("Famous","著名的","害羞的","饥饿的","疲倦的"),
        new Word("Excellent","极好的","糟糕的","普通的","危险的"),
        new Word("Exciting","令人兴奋的","令人无聊的","昂贵的","困难的"),
        new Word("Boring","无聊的","有趣的","友好的","健康的"),
        new Word("Tired","疲倦的","兴奋的","口渴的","小心的"),
        new Word("Hungry","饥饿的","口渴的","疲倦的","饱的"),
        new Word("Thirsty","口渴的","饥饿的","寒冷的","生病的"),
        new Word("Early","早的；早地","晚的；晚地","缓慢的","危险的"),
        new Word("Late","迟的；晚的","早的；早地","友好的","健康的"),
        new Word("Quickly","迅速地","缓慢地","安静地","小心地"),
        new Word("Slowly","缓慢地","迅速地","大声地","容易地"),
        new Word("Carefully","小心地","迅速地","大声地","突然地"),
        new Word("Loudly","大声地","安静地","缓慢地","认真地"),
        new Word("Quietly","安静地","大声地","迅速地","快乐地"),
        new Word("How often","多久一次","多少","多远","几点"),
        new Word("Get up","起床","睡觉","回家","出发"),
        new Word("Look after","照顾","寻找","看起来","等待"),
        new Word("Have a good time","玩得开心","赶快","小心","迷路"),
        new Word("A lot of","许多","一点也不","每一个","在……之间"),
        new Word("Of course","当然","也许","从不","对不起"),
        new Word("For example","例如","最后","马上","一起"),
        new Word("At the moment","此刻；现在","每天","上周","明年"),
        new Word("On time","准时","迟到","立刻","有时"),
        new Word("Take part in","参加","照顾","寻找","离开"),
        new Word("Look for","寻找","照顾","等待","关掉"),
        new Word("Turn on","打开","关闭","拿走","归还"),
        new Word("Turn off","关闭","打开","寻找","参加"),
        new Word("Go shopping","去购物","去游泳","去上学","去睡觉"),
        new Word("What about","……怎么样","为什么","什么时候","在哪里"),
        new Word("How much","多少钱；多少","多久一次","多远","多高"),
        new Word("How many","多少（可数）","多少钱","多久","多远"),
        new Word("What time","几点","什么颜色","多少人","多远"),
        new Word("Good morning","早上好","晚安","再见","对不起"),
        new Word("Thank you","谢谢","对不起","你好","再见")
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
        // True kiosk lockdown requires this app to be provisioned as Device Owner.
        // A normal app can only request screen pinning, which does not reliably block HOME.
        try {
            android.app.admin.DevicePolicyManager dpm =
                (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            android.content.ComponentName admin =
                new android.content.ComponentName(this, QuizDeviceAdminReceiver.class);
            if (dpm != null && dpm.isDeviceOwnerApp(getPackageName())) {
                dpm.setLockTaskPackages(admin, new String[]{getPackageName()});
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    dpm.setLockTaskFeatures(admin, 0);
                }
            }
            startLockTask();
        } catch (SecurityException ignored) {
            // Without Device Owner provisioning, Android may only offer screen pinning.
        } catch (IllegalStateException ignored) {
            // Some vendor TV firmware does not implement lock-task mode normally.
        }
    }
    @Override
    public void onBackPressed() {
        feedback.setText("请先答对 5 道题");
    }
}
