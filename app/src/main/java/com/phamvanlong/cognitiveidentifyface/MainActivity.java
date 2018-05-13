package com.phamvanlong.cognitiveidentifyface;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private FaceServiceClient faceServiceClient = new FaceServiceRestClient("https://westcentralus.api.cognitive.microsoft.com/face/v1.0","e8a26a2a32224ba5a420bb3c247898bf");
    private final String personGroupId = "hollywoodstar";
    private String TAG="this";

    ImageView imageView;
    Bitmap mBitmap,bitmap;
    Face[] faceDetected;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.tom3);
        imageView = findViewById(R.id.imgView);

        imageView.setImageBitmap(bitmap);
        Button btnOpenCamera = findViewById(R.id.openCam);
        Button btnDetect = findViewById(R.id.btnDetectFace);
        Button btnIdentify = findViewById(R.id.btnIdentify);

        btnOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,0);
            }
        });

        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detect(bitmap);
            }
        });

        btnIdentify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    final UUID[] faceIds = new UUID[faceDetected.length];
                    for(int i=0;i<faceDetected.length;i++){
                        Face face=faceDetected[i];
                        faceIds[i] = face.faceId;
                    }

                    new IdentificationTask(personGroupId).execute(faceIds);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bitmap = (Bitmap)data.getExtras().get("data");
        //mBitmap = bitmap;
        imageView.setImageBitmap(bitmap);

    }

    private void detect(final Bitmap bitmap){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        class detectTask extends AsyncTask<InputStream,String,Face[]> {
            private ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

            @Override
            protected Face[] doInBackground(InputStream... inputStreams) {
                try{
                    publishProgress("Detecting...");
                    Face[] results = faceServiceClient.detect(inputStreams[0],true,false,null);
                    if(results == null){
                        publishProgress("Detection finished. Nothing detected");
                        return null;
                    }
                    else{
                        publishProgress(String.format("Detection Finish. %d face(s) detected",results.length));
                        return results;
                    }
                }
                catch (Exception e){
                    return null;
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mDialog.show();
            }

            @Override
            protected void onPostExecute(Face[] faces) {
                mDialog.dismiss();
                if (faces==null) return;
                imageView.setImageBitmap(drawFaceRectangleOnBitmap1(bitmap,faces));
                faceDetected=faces;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
                mDialog.setMessage(values[0]);
            }
        }
        new detectTask().execute(inputStream);
    }

    private class IdentificationTask extends AsyncTask<UUID,String,IdentifyResult[]>{
        String personGroupId;

        ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

        public IdentificationTask(String personGroupId){
            this.personGroupId = personGroupId;
            //personGroupId = personGroupId;
        }


        @Override
        protected IdentifyResult[] doInBackground(UUID... uuids) {
            try{
             //publishProgress("Getting person group status...");
             //TrainingStatus trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(personGroupId);
//             if(trainingStatus.status != TrainingStatus.Status.Succeeded){
//                 publishProgress("Person training status is " + trainingStatus.status);
//                 return null;
//             }
             publishProgress("Identifying...");
                return faceServiceClient.identity(personGroupId ,uuids,1); //uuids là
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.show();
        }

        @Override
        protected void onPostExecute(IdentifyResult[] identifyResults) {
            mDialog.dismiss();

            for(IdentifyResult identifyResult : identifyResults){
                new PersonDetectionTask(personGroupId).execute(identifyResult.candidates.get(0).personId);
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mDialog.setMessage(values[0]);
        }
    }
    private class PersonDetectionTask extends AsyncTask<UUID,String,Person>{
        private ProgressDialog mDialog = new ProgressDialog(MainActivity.this);
        private String personGroupId;

        public PersonDetectionTask(String personGroupId){
            this.personGroupId = personGroupId;
        } //thu nghiem

        @Override
        protected Person doInBackground(UUID... uuids) {
            try{
                publishProgress("Getting person status...");

                return faceServiceClient.getPerson(personGroupId,uuids[0]);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mDialog.show();
        }

        @Override
        protected void onPostExecute(Person person) {
            mDialog.dismiss();
            Toast.makeText(MainActivity.this,"Ten "+person.name,Toast.LENGTH_SHORT).show();
            action(person.name);

            //ImageView img = findViewById(R.id.imgView);
            //imageView.setImageBitmap(drawFaceRectangleOnBitmap(mBitmap,faceDetected,person.name));
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mDialog.setMessage(values[0]);
        }
    }

    private void action(String ten) {
        String link="";
        switch (ten) {
            case "Tom Cruise":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Tom":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Keanu Reeves":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Emma Watson":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Long":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Luong":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Nghia":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Quy":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Lam":{
                link = "hanhdong?moCua=mo";
                break;
            }
            case "Nguyen Thanh Lam":{
                link = "hanhdong?moCua=mo";
                break;
            }
            default:{
                link = "hanhdong?moCua=dong";
                break;
            }
        }
        URL remoteurl = null;
        String baseurl = "http://192.168.57.102/";
        String urlString = baseurl + link;
        System.out.println(urlString);
        try {
            remoteurl = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        new pingit().execute(remoteurl);
    }
    private Bitmap drawFaceRectangleOnBitmap(Bitmap mBitmap, Face[] faceDetected, String name) {

        Bitmap bitmap = mBitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(12);

        if(faceDetected != null) {
            for(Face face:faceDetected){
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(faceRectangle.left,faceRectangle.top,
                        faceRectangle.left+faceRectangle.width,
                        faceRectangle.top+faceRectangle.height,paint);
                drawTextOnCanvas(canvas,100,((faceRectangle.left+faceRectangle.width)/2)+100,(faceRectangle.top+faceRectangle.height)+50,Color.WHITE,name);
            }
        }
        return bitmap;
    }

    private void drawTextOnCanvas(Canvas canvas, int textSize, int x, int y, int color, String name) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(textSize);

        float textWidth = paint.measureText(name);

        canvas.drawText(name,x-(textWidth/2),y-(textSize/2),paint);
    }
    private class pingit extends AsyncTask<URL, Void, Void> {
        @Override
        protected Void doInBackground(URL... urls) {
            try {
                URLConnection con = null;
                con = urls[0].openConnection();
                InputStream is = null;
                is = con.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }
    private Bitmap drawFaceRectangleOnBitmap1(Bitmap mBitmap, Face[] faceDetected) {

        Bitmap bitmap = mBitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(12);

        if(faceDetected != null) {
            for(Face face:faceDetected){
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(faceRectangle.left,faceRectangle.top,
                        faceRectangle.left+faceRectangle.width,
                        faceRectangle.top+faceRectangle.height,paint);
            }
        }
        return bitmap;
    }

}