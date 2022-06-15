package com.example.authorizingsystem;

//import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.content.ClipData;
import android.content.ClipboardManager;
import static android.content.Context.CLIPBOARD_SERVICE;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;


public class MainActivity extends Activity implements SensorEventListener {
    public boolean isMeasuring=false;
    public long timeStart;
    private SensorManager mSensorManager;
    private Sensor accSensor, mGyroscope;
    public float accelXValue, accelYValue, accelZValue;
    public float gyroX, gyroY, gyroZ;
    public TextView textView;
    public String filename;

    @Override
    //메인 프로시져
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text) ;

        //센서 매니저 얻기
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //엑셀러로미터 센서(가속)
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //자이로스코프 센서(회전)
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //버튼 이벤트
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //측정 중: 측정 시작 버튼을 눌렀을 때
                if (!isChecked) {
                    isMeasuring = true;
                    timeStart = System.currentTimeMillis();
                    filename = "gait_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+".csv";
                    Toast.makeText(getApplicationContext(), "측정 중..",Toast.LENGTH_SHORT).show();
                }
                //측정 전 or 측정 종료: 측정 종료 버튼을 눌렀을 때
                else {
                    isMeasuring = false;
                    //저장한 파일 탐색
                    File fileFileName = getFileStreamPath(filename);
                    String getFileName = fileFileName.getPath();
                    //찾은 파일 공유
                    shareFile(new File(getFileName));
                    Toast.makeText(getApplicationContext(), "측정 종료",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //센서값 얻어오기
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {      //가속도 값 업데이트
            accelXValue = event.values[0];
            accelYValue = event.values[1];
            accelZValue = event.values[2];
        }
        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {        //각속도 값 업데이트
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];
        }

        //측정 중일때
        if(isMeasuring){
            long time = System.currentTimeMillis();
            //화면에 출력할 데이터와 저장할 데이터
            String insertData = Long.toString(time-timeStart) + ","+ Float.toString(accelXValue) + "," + Float.toString(accelYValue) + "," + Float.toString(accelZValue) + "," + Float.toString(gyroX) + "," + Float.toString(gyroY) + "," + Float.toString(gyroZ)+"\n";
            String viewTxt = "time: "+Long.toString(time-timeStart) + "\nACC_X: "+ Float.toString(accelXValue) + "\nACC_Y: " + Float.toString(accelYValue) + "\nACC_Z: " + Float.toString(accelZValue) + "\nGYRO_X: " + Float.toString(gyroX) + "\nGYRO_Y: " + Float.toString(gyroY) + "\nGYRO_Z: " + Float.toString(gyroZ);
            //화면출력
            textView.setText(viewTxt);
            //데이터 저장
            writeFile(filename, insertData);
        }
        //측정 중이 아닐때
        else{
            textView.setText("측정이 종료되었습니다.");
        }
    }

    //데이터 저장 함수
    public void writeFile(String filename, String data) {
        String fileName= filename;
        String textToWrite = data;
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = openFileOutput(fileName, Context.MODE_APPEND);
            fileOutputStream.write(textToWrite.getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //파일 공유 함수
    public void shareFile(File shareFile) {
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);           // 단일파일 보내기
        // 파일형태에 맞는 type설정
        MimeTypeMap type = MimeTypeMap.getSingleton();
        intent.setType(type.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(shareFile.getPath())));
        //intent.setType("plain/text"); // text 형태로 전달
        //intent.setType("*/*");        // 모든 공유 형태 전달
        intent.putExtra(Intent.EXTRA_SUBJECT, "보행 데이터");  // 제목
        intent.putExtra(Intent.EXTRA_TEXT, "보행 데이터");     // 내용
        if (shareFile != null) {
            Uri contentUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", shareFile); // manifest의  ${applicationId}.fileprovider
            intent.putExtra(Intent.EXTRA_STREAM, contentUri); // 단일 파일 전송
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);     // 공유 앱에 권한 주기
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);    // 공유 앱에 권한 주기
        startActivity(intent);
    }


    //정확도에 대한 메소드 호출 (사용안함)
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    public void onStart() {
        super.onStart();
        // 주기 설명
        // SENSOR_DELAY_UI 갱신에 필요한 정도 주기
        // SENSOR_DELAY_NORMAL 화면 방향 전환 등의 일상적인 주기
        // SENSOR_DELAY_GAME 게임에 적합한 주기 //
        // SENSOR_DELAY_FASTEST 최대한의 빠른 주기
        // 리스너 등록
        mSensorManager.registerListener(this, accSensor,SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope,SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);    //리스너 해제
    }
}