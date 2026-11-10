package com.greatwy.hisenseenglishquiz;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;

public class QuizTimerService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tick;
    private static final long INTERVAL = 15 * 60 * 1000L;
    @Override public void onCreate() {
        super.onCreate();
        SharedPreferences prefs=getSharedPreferences("quiz",0);
        if(prefs.getLong("next_due",0L)==0L) prefs.edit().putLong("next_due",System.currentTimeMillis()+INTERVAL).apply();
        tick = new Runnable() { @Override public void run() {
            SharedPreferences p=getSharedPreferences("quiz",0); long due=p.getLong("next_due",System.currentTimeMillis()+INTERVAL);
            if(System.currentTimeMillis()>=due) { Intent i=new Intent(QuizTimerService.this,QuizActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP); try { startActivity(i); } catch(Exception ignored) {} p.edit().putLong("next_due",System.currentTimeMillis()+INTERVAL).apply(); }
            handler.postDelayed(this,5000);
        }}; handler.post(tick);
    }
    @Override public int onStartCommand(Intent i,int f,int id) { return START_STICKY; }
    @Override public void onDestroy() { handler.removeCallbacks(tick); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
