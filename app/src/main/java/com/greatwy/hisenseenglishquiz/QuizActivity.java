package com.greatwy.hisenseenglishquiz;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;
import java.util.Random;

public class QuizActivity extends Activity {
    private int qIndex=0, correctCount=0;
    private TextView title, progress, feedback;
    private Button[] choices = new Button[4];
    private Word current;
    private final Word[] words = new Word[] {
        new Word("Apple","苹果","香蕉","橘子","葡萄"), new Word("Book","书","桌子","椅子","门"),
        new Word("Cat","猫","狗","鸟","鱼"), new Word("Water","水","火","米饭","牛奶"),
        new Word("Thank you","谢谢","对不起","你好","再见"), new Word("House","房子","学校","医院","商店"),
        new Word("Dog","狗","猫","马","兔子"), new Word("Red","红色","蓝色","绿色","黄色"),
        new Word("Good morning","早上好","晚安","再见","谢谢"), new Word("School","学校","公园","车站","厨房"),
        new Word("Family","家庭","朋友","老师","学生"), new Word("Milk","牛奶","果汁","水","茶"),
        new Word("Friend","朋友","医生","司机","警察"), new Word("Sun","太阳","月亮","星星","云"),
        new Word("Happy","快乐的","伤心的","疲倦的","生气的"), new Word("Goodbye","再见","你好","谢谢","早上好"),
        new Word("Big","大的","小的","高的","短的"), new Word("Small","小的","大的","新的","旧的"),
        new Word("Pen","钢笔","书","桌子","椅子"), new Word("Car","汽车","火车","飞机","自行车"),
        new Word("Water","水","牛奶","茶","果汁"), new Word("Mother","妈妈","爸爸","姐姐","弟弟"),
        new Word("Father","爸爸","妈妈","老师","朋友"), new Word("One","一","二","三","四"),
        new Word("Two","二","一","三","五"), new Word("Hello","你好","再见","谢谢","对不起")
    };
    static class Word {
        String en; String[] options;
        Word(String e,String right,String a,String b,String c) { en=e; options=new String[]{right,a,b,c}; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        startService(new Intent(this, QuizTimerService.class));
        buildQuiz();
    }
    private void buildQuiz() {
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER);
        root.setPadding(36,20,36,20); root.setBackgroundColor(Color.rgb(18,45,82));
        title=label("英语闯关",36,Color.WHITE,true); progress=label("",24,Color.WHITE,false); feedback=label("请选择正确的中文意思",22,Color.YELLOW,false);
        root.addView(title); root.addView(progress); root.addView(feedback);
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER);
        for(int i=0;i<4;i++){ final int ix=i; Button btn=new Button(this); btn.setTextSize(22); btn.setFocusable(true); btn.setTextColor(Color.BLACK); btn.setOnClickListener(v -> answer(ix));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,120,1); lp.setMargins(8,20,8,20); row.addView(btn,lp); choices[i]=btn; }
        root.addView(row); setContentView(root); qIndex=0; correctCount=0; nextQuestion(); choices[0].requestFocus();
    }
    private TextView label(String s,int size,int color,boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER); if(bold)t.setTypeface(null,Typeface.BOLD); t.setPadding(10,12,10,12); return t; }
    private void nextQuestion() {
        if(qIndex>=5) { feedback.setText("全部答对！继续使用电视"); progress.setText("5 / 5"); for(Button b:choices)b.setVisibility(View.GONE);
            getWindow().getDecorView().postDelayed(() -> { getSharedPreferences("quiz",0).edit().putLong("next_due",System.currentTimeMillis()+15*60*1000L).apply(); finish(); },1800); return; }
        current=words[new Random().nextInt(words.length)]; title.setText("第 "+(qIndex+1)+" 题： "+current.en); progress.setText("本轮答对 "+correctCount+" / 5"); feedback.setText("请选择正确的中文意思");
        String[] opts=current.options.clone(); Random r=new Random(); for(int i=opts.length-1;i>0;i--){int j=r.nextInt(i+1);String x=opts[i];opts[i]=opts[j];opts[j]=x;}
        for(int i=0;i<4;i++){choices[i].setVisibility(View.VISIBLE);choices[i].setText(opts[i]);choices[i].setTag(Boolean.valueOf(opts[i].equals(current.options[0])));}
    }
    private void answer(int index) { boolean ok=Boolean.TRUE.equals(choices[index].getTag()); if(ok){correctCount++;qIndex++;feedback.setText("回答正确！");nextQuestion();}else{feedback.setText("回答错误，请再试一次");} progress.setText("本轮答对 "+correctCount+" / 5"); }
    @Override public void onBackPressed() { feedback.setText("请先答对 5 道题"); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) { if(event.getKeyCode()==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_UP){feedback.setText("请先答对 5 道题");return true;} return super.dispatchKeyEvent(event); }
}
