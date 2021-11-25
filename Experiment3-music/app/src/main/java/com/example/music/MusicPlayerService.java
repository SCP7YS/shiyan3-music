package com.example.music;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MusicPlayerService extends Service implements SensorEventListener {//implements Runnable {
    private static final String TAG = "MusicPlayerService";
    private static final int NOTIFICATION_ID = 1; // 如果id设置为0,会导致不能设置为前台service
    public static MediaPlayer mediaPlayer = null;
    private String url = null;
    private String MSG = null;
    private static int curposition;//第几首音乐
    private musicBinder musicbinder = null;
    private int currentPosition = 0;// 设置默认进度条当前位置
    private ArrayList<MusicBean> musiclist;//音乐列表
    private MusicBean musicBean;//当前播放的音乐信息
    private SensorManager sensorManager = null;//传感器
    private Vibrator vibrator = null;//震动
    public MusicPlayerService() {
        Log.i(TAG,"MusicPlayerService......1");
        musicbinder = new musicBinder();
    }

    //通过bind 返回一个IBinder对象，然后改对象调用里面的方法实现参数的传递
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"onBind......");
       return musicbinder;
    }


    /**
     * 自定义的 Binder对象
     */
    public class musicBinder extends Binder {
        public MusicPlayerService getPlayInfo(){
            return MusicPlayerService.this;
        }
    }
    //得到当前播放位置
    public  int getCurrentPosition(){

        if(mediaPlayer != null){
            currentPosition = mediaPlayer.getCurrentPosition();
        }
        return currentPosition;
    }
    //得到总时长
    public  int getDuration(){
        return mediaPlayer.getDuration();// 总时长
    }
    //当前播放音乐
    public MusicBean getMusicMedia() {
        return musicBean;
    }

    //得到 mediaPlayer
    public MediaPlayer getMediaPlayer(){
//        if(mediaPlayer != null){
//            return mediaPlayer;
//        }
        return mediaPlayer;
    }
    //得到 当前播放第几个音乐
    public int getCurposition(){
        return curposition;
    }
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate......2");
        super.onCreate();
        if (mediaPlayer == null) {
           /* mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;*/
            mediaPlayer = new MediaPlayer();
        }
        musiclist = MusicActivity.musicList;
         // 监听播放是否完成
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //我目前也不知道该干嘛,下一首嘛
//                curposition++;
//                curposition = (curposition) % musiclist.size();
                playnew();
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //取得震动服务的句柄
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
       if(MusicActivity.sharedPreferences.getInt("play_accelerometer",0) == 0){
           // 传感器报告新的值
           int sensorType = event.sensor.getType();
           //values[0]:X轴，values[1]：Y轴，values[2]：Z轴
           float[] values = event.values;
           if (sensorType == Sensor.TYPE_ACCELEROMETER)
           {
               if ((Math.abs(values[0]) > 17 || Math.abs(values[1]) > 17 || Math
                       .abs(values[2]) > 17))
               {
                   Log.i("slack", "sensor x values[0] = " + values[0]);
                   Log.i("slack", "sensor y values[1] = " + values[1]);
                   Log.i("slack", "sensor z values[2] = " + values[2]);
                   playnew();
                   //摇动手机后，再伴随震动提示~~
                   vibrator.vibrate(500);
               }

           }
       }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //传感器精度的改变
    }
    private void playnew() {
        switch (MusicActivity.sharedPreferences.getInt("play_mode",-1)){
            case 0://随机
                curposition = (new Random()).nextInt(musiclist.size());
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            case 1://顺序
                curposition = (++curposition) % musiclist.size();
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            case 2://单曲
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            default:
                break;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand......3");
        // /storage/emulated/0/Music/Download/Selena Gomez - Revival/Hands to Myself.mp3
        if(intent != null){
            MSG = intent.getStringExtra("MSG");
            if(MSG.equals("0")){
                url = intent.getStringExtra("url");
                curposition = intent.getIntExtra("curposition", 0);
                musicBean = musiclist.get(curposition);
                Log.i(TAG, url + "......." + Thread.currentThread().getName());
                palyer();
            }else if(MSG.equals("1")){
                mediaPlayer.pause();
            }else if(MSG.equals("2")){
                mediaPlayer.start();
            }

            String name = "Current: "+ url.substring(url.lastIndexOf("/") + 1 , url.lastIndexOf("."));
            Log.i(TAG,name);
//        //开启前台service

        }

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        return super.onStartCommand(intent, flags, startId);
    }


    private void palyer() {
        Log.i(TAG, "palyer......");
        //还有就是用户在暂停是点击其他的音乐，所以不管当前状态，都重置一下
        //下面这段代码可以实现简单的音乐播放
        try {
            mediaPlayer.reset();

            mediaPlayer.setDataSource(url);

            mediaPlayer.prepare();
            mediaPlayer.start();
            musicBean = musiclist.get(curposition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG,"onUnbind......");
        return super.onUnbind(intent);

    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(TAG, "onRebind......");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy......");
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        //关闭线程
        Thread.currentThread().interrupt();
        stopForeground(true);
        sensorManager.unregisterListener(this);
    }
    public String toTime(int time){
        time /= 1000;
        int minute = time / 60;
        int hour = minute / 60;
        int second = time % 60;
        minute %= 60;
        return String.format("%02d:%02d", minute, second);
    }
}
