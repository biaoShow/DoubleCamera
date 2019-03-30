package com.biao.doublecamera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.provider.Telephony.ThreadsColumns.ERROR;


public class MainActivity extends AppCompatActivity {

    private SurfaceView sv_front, sv_back;
    private SurfaceHolder frontHolder, backHolder;
    private MediaRecorder frontRecorder, backRecorder;
    private Button btn_stop, btn_start;
    private Camera frontCamera, backCamera;
    private CountDownTime countDownTime;
    private HandlerThread frontThread = new HandlerThread("front");
    private HandlerThread backThread = new HandlerThread("back");
    private Handler frontHandler;
    private Handler backHandler;
    //    private Handler frontHandler = new Handler();
//    private Handler backHandler = new Handler();
    private String path, logPath;
    private LogCatHelper logCatHelper;

    //权限相关
    private final int MY_WRITE_EXTERNAL_STORAGE = 1;//读写权限
    private final int MY_CAMERA = 2;//相机权限

    private int stopNum = 1;//记录停止的次数

    private Runnable frontRunnable = new Runnable() {
        @Override
        public void run() {
            int cameraNum = Camera.getNumberOfCameras();
            if (frontCamera == null && cameraNum > 1) {
                try {
                    frontCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                } catch (Exception e) {
                    Log.e("openFront", "catch到了错误执行了" + e.getMessage());
                }
                if (frontCamera != null) {
                    setError(frontCamera, "front");
                    frontCamera.unlock();
                    frontRecorder.setCamera(frontCamera);// 设置录制视频源为Camera(相机)
                    try {
                        frontRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                            @Override
                            public void onError(MediaRecorder mr, int what, int extra) {
                                Log.i("frontRecorder", "监听到了错误执行了");
                            }
                        });
                        frontRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER); // 这两项需要放在setOutputFormat之前
                        frontRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); // 这两项需要放在setOutputFormat之前
                        frontRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//设置录制视频的输出格式
                        frontRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//设置音频编码格式
                        frontRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);//设置视频编码格式// 设置录制的视频编码h263 h264
                        frontRecorder.setVideoSize(1280, 720);//设置视频的分辨率,必须放在设置编码和格式的后面，否则报错
                        frontRecorder.setVideoFrameRate(30);//这是设置视频录制的帧率，即1秒钟30帧。。必须放在设置编码和格式的后面，否则报错
                        frontRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);//这个属性很重要，这个也直接影响到视频录制的大小，这个设置的越大，视频越清晰
//                        frontRecorder.setOrientationHint(90);//视频旋转90度
//                        frontRecorder.setMaxDuration(30 * 1000);//设置录制最长时间为30秒
                        frontRecorder.setPreviewDisplay(frontHolder.getSurface());//设置录制视频时的预览画面
                        if (path != null) {
                            File dir = new File(path + "/recordtest");
                            if (!dir.exists()) {
                                dir.mkdir();
                            }
                            String pathFront = dir + "/" + String.valueOf(System.currentTimeMillis()) + "front.mp4";
                            frontRecorder.setOutputFile(pathFront);// 设置视频文件输出的路径
                            frontRecorder.prepare();// 准备录制
                            frontRecorder.start();// 开始录制

                        }
                    } catch (Exception e) {
                        Log.i("frontRecorder", "catch到了错误执行了" + e.getMessage());
                        e.printStackTrace();
                    }
//                    try {
//                        frontCamera.setPreviewDisplay(frontHolder);
//                        frontCamera.startPreview();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
            }
        }
    };
    private Runnable backRunnable = new Runnable() {
        @Override
        public void run() {
            int cameraNum = Camera.getNumberOfCameras();
            if (backCamera == null && cameraNum > 0) {
                try {
                    backCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                } catch (Exception e) {
                    Log.e("openBack", "catch到了错误执行了" + e.getMessage());
                }
                if (backCamera != null) {
                    setError(backCamera, "back");
                    backCamera.unlock();
                    backRecorder.setCamera(backCamera);// 设置录制视频源为Camera(相机)
                    try {
                        backRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                            @Override
                            public void onError(MediaRecorder mr, int what, int extra) {
                                Log.i("backRecorder", "监听到了错误执行了");
                            }
                        });
//                        backRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER); // 这两项需要放在setOutputFormat之前
                        backRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); // 这两项需要放在setOutputFormat之前
                        backRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//设置录制视频的输出格式
//                        backRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//设置音频编码格式
                        backRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);//设置视频编码格式// 设置录制的视频编码h263 h264
                        backRecorder.setVideoSize(1280, 720);//设置视频的分辨率,必须放在设置编码和格式的后面，否则报错
                        backRecorder.setVideoFrameRate(30);//这是设置视频录制的帧率，即1秒钟30帧。。必须放在设置编码和格式的后面，否则报错
                        backRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);//这个属性很重要，这个也直接影响到视频录制的大小，这个设置的越大，视频越清晰
//                        frontRecorder.setOrientationHint(90);//视频旋转90度
//                        frontRecorder.setMaxDuration(30 * 1000);//设置录制最长时间为30秒
                        backRecorder.setPreviewDisplay(backHolder.getSurface());//设置录制视频时的预览画面
                        if (path != null) {
                            File dir = new File(path + "/recordtest");
                            if (!dir.exists()) {
                                dir.mkdir();
                            }
                            String pathBack = dir + "/" + String.valueOf(System.currentTimeMillis()) + "back.mp4";
                            backRecorder.setOutputFile(pathBack);// 设置视频文件输出的路径
                            backRecorder.prepare();// 准备录制
                            backRecorder.start();// 开始录制
//                            backHolder.addCallback(new SurfaceHolder.Callback() {
//
//                                @Override
//                                public void surfaceDestroyed(SurfaceHolder holder) {
//                                    Log.i("surfaceCreated","surfaceDestroyed");
//                                }
//
//                                @Override
//                                public void surfaceCreated(SurfaceHolder holder) {
//                                    try {
//                                        backRecorder.prepare();
//                                        backRecorder.start();
//                                    } catch (Exception e) {
//                                        Writer writer = new StringWriter();
//                                        PrintWriter printWriter = new PrintWriter(writer);
//                                        e.printStackTrace(printWriter);
//                                        Throwable cause = e.getCause();
//                                        while (cause != null) {
//                                            cause.printStackTrace(printWriter);
//                                            cause = cause.getCause();
//                                        }
//                                        String str = writer.toString();
//                                        Log.i("surfaceCreated",str);
//                                    }
//                                }
//
//                                @Override
//                                public void surfaceChanged(SurfaceHolder holder, int format,
//                                                           int width, int height) {
//                                    Log.i("surfaceCreated","surfaceChanged");
//                                }
//                            });
                        }
                    } catch (Exception e) {
                        Log.i("backRecorder", "catch到了错误执行了" + e.getMessage());
                        e.printStackTrace();
                    }
//                    try {
//                        backCamera.setPreviewDisplay(backHolder);
//                        backCamera.startPreview();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sv_front = findViewById(R.id.sv_front);
        sv_back = findViewById(R.id.sv_back);
        btn_stop = findViewById(R.id.btn_stop);
        btn_start = findViewById(R.id.btn_start);
        frontThread.start();
        backThread.start();
        frontHandler = new Handler(frontThread.getLooper());
        backHandler = new Handler(backThread.getLooper());

        frontHolder = sv_front.getHolder();
        backHolder = sv_back.getHolder();
        logPath = Environment.getExternalStorageDirectory() + "/Log2";
        logCatHelper = LogCatHelper.getInstance(this, logPath);
        path = getSdPath();
        countDownTime = new CountDownTime(2 * 60 * 1000, 10000);
//        frontRecorder = new MediaRecorder();
//        backRecorder = new MediaRecorder();

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(MY_CAMERA);
            }
        });

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTime.cancel();
                stopVideo();
                btn_stop.setClickable(true);
            }
        });

    }

    /**
     * 开始录像
     */
    private void startVideo() {
        deleteFile();
        if (frontRecorder == null) {
            frontRecorder = new MediaRecorder();
            frontHandler.postDelayed(frontRunnable, 500);
        }
        if (backRecorder == null) {
            backRecorder = new MediaRecorder();
            backHandler.postDelayed(backRunnable, 1000);
        }
        countDownTime.start();
        btn_stop.setClickable(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermission(MY_WRITE_EXTERNAL_STORAGE);
    }

    /**
     * 停止录像
     */
    private void stopVideo() {
        try {
            if (frontRecorder != null) {
                frontRecorder.stop();// 停止录制
                frontRecorder.reset();// 恢复到未初始化的状态
                frontRecorder.release();// 释放资源
                frontRecorder = null;
                if (frontCamera != null) {
                    frontCamera.release();
                    frontCamera = null;
                }
            }
            if (backRecorder != null) {
                backRecorder.stop();// 停止录制
                backRecorder.reset();// 恢复到未初始化的状态
                backRecorder.release();// 释放资源
                backRecorder = null;
                if (backCamera != null) {
                    backCamera.release();
                    backCamera = null;
                }
            }
        } catch (Exception e) {
            try {
                if (stopNum < 20) {
                    stopNum++;
                    Log.e("stopVideo", "catch到停止录制失败！" + e.getMessage());
                    Thread.sleep(200);
                    stopVideo();
                } else {
                    showWaringDialog("多次尝试关闭摄像头失败！");
                }
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

    }

    /**
     * 获取SD path
     *
     * @return
     */
    public String getSdPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取根目录
            return sdDir.toString();
//            return "/mnt/usbhost2";
        }
        return null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        countDownTime.cancel();
        stopVideo();
    }

    class CountDownTime extends CountDownTimer {

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public CountDownTime(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            stopVideo();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1500);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startVideo();
                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void setError(Camera camera, final String type) {
        //监听相机服务是否终止或者出现其他异常
        camera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                Log.e("onError", type + "---" + "error:" + error);
                deleteFile();
                if (type.equals("front")) {
                    if (frontCamera != null) {
                        frontCamera.stopPreview();
                        frontCamera.release();
                        frontCamera = null;
                    }
                    frontHandler.postDelayed(frontRunnable, 500);
                } else {
                    if (backCamera != null) {
                        backCamera.stopPreview();
                        backCamera.release();
                        backCamera = null;
                    }
                    backHandler.postDelayed(backRunnable, 500);
                }
            }
        });
    }

    /**
     * 获取手机外部可用存储空间
     */
    private long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return (availableBlocks * blockSize) / (1024 * 1024 * 1024);
        } else {
            return -100;
        }
    }

    /**
     * SDCARD是否存
     */
    private boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    private List<String> getFilesAllName(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            Log.e("error", "空目录");
            return null;
        }
        List<String> s = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            s.add(files[i].getAbsolutePath());
        }
        Collections.sort(s);
        return s;
    }

    /**
     * 内存不够时候删除之前的视频
     */
    private void deleteFile() {
        long size = getAvailableExternalMemorySize();
        Log.i("剩余外部内存大小", size + "GB");
        if (size < 1) {
            List<String> list = getFilesAllName(path + "/recordtest");
            if (list != null && list.size() > 20) {
                for (int i = 0; i <= 20; i++) {
                    new File(list.get(i)).delete();
                }
            }
        }
    }

    /**
     * 申请权限方法
     */
    private void requestPermission(int pyte) {
        switch (pyte) {
            case MY_CAMERA:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                            MY_CAMERA);
                } else {
                    startVideo();
                }
                break;
            case MY_WRITE_EXTERNAL_STORAGE:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_WRITE_EXTERNAL_STORAGE);
                } else {
                    logCatHelper.start();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {//允许
                    logCatHelper.start();
                } else {//拒绝
                    showWaringDialog("请前往设置->应用->DoubleCamera->权限中打开相关权限，否则功能无法正常运行");
                }
                break;
            case MY_CAMERA:
                if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {//允许
                    startVideo();
                } else {//拒绝
                    showWaringDialog("请前往设置->应用->DoubleCamera->权限中打开相关权限，否则功能无法正常运行");
                }
                break;
        }

    }

    /**
     * dialog提示
     */
    private void showWaringDialog(String warning) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("警告！")
                .setMessage(warning)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 一般情况下如果用户不授权的话，功能是无法运行的，做退出处理
                        finish();
                    }
                }).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
