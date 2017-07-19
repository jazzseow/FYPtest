package com.example.fyptest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.tzutalin.vision.visionrecognition.ObjectDetector;
import com.tzutalin.vision.visionrecognition.VisionClassifierCreator;
import com.tzutalin.vision.visionrecognition.VisionDetRet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//import com.caffe.ObjectDetector;
////import com.caffe.R;
//import com.caffe.VisionClassifierCreator;
//import com.caffe.VisionDetRet;

public class ObjectDetectActivity extends Activity {
    private final static String TAG = "ObjectDetectActivity";
    private ObjectDetector mObjectDet;
    private ImageView imageView;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_object_detect);
        imageView = (ImageView) findViewById(R.id.detect_image);
        listView = (ListView) findViewById(R.id.listview_result);

        String imgPath = getIntent().getExtras().getString("Path");


        if (!new File(imgPath).exists()) {
            Toast.makeText(this, "No file path", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        DetectTask task = new DetectTask();
        task.execute(imgPath);
    }

    // ==========================================================
    // Tasks inner class
    // ==========================================================
    private class DetectTask extends AsyncTask<String, Void, List<VisionDetRet>> {
        private ProgressDialog mmDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mmDialog = ProgressDialog.show(ObjectDetectActivity.this, getString(R.string.dialog_wait), getString(R.string.dialog_object_description), true);
        }

        @Override
        protected List<VisionDetRet> doInBackground(String... strings) {
            final String filePath = strings[0];
            long startTime;
            long endTime;
            Log.d(TAG, "DetectTask filePath:" + filePath);
            if (mObjectDet == null) {
                try {
                    mObjectDet = VisionClassifierCreator.createObjectDetector(getApplicationContext());
                    // TODO: Get Image's height and width
                    mObjectDet.init(0, 0);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            List<VisionDetRet> ret = new ArrayList<>();
            if (mObjectDet != null) {
                startTime = System.currentTimeMillis();
                Log.d(TAG, "Start objDetect");
                ret.addAll(mObjectDet.classifyByPath(filePath));
                Log.d(TAG, "end objDetect");
                endTime = System.currentTimeMillis();
                final double diffTime = (double) (endTime - startTime) / 1000;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ObjectDetectActivity.this, "Take " + diffTime + " second", Toast.LENGTH_LONG).show();
                    }
                });
            }
            File beDeletedFile = new File(filePath);
            if (beDeletedFile.exists()) {
                beDeletedFile.delete();
            } else {
                Log.d(TAG, "file does not exist " + filePath);
            }

            mObjectDet.deInit();
            return ret;
        }

        @Override
        protected void onPostExecute(List<VisionDetRet> rets) {
            super.onPostExecute(rets);
            if (mmDialog != null) {
                mmDialog.dismiss();
            }
            // TODO: Remvoe it

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            String retImgPath = "/sdcard/temp.jpg";
            Bitmap bitmap = BitmapFactory.decodeFile(retImgPath, options);

            imageView.setImageBitmap(bitmap);

            List<String> lists = new ArrayList<String>();
            for (VisionDetRet item : rets) {
                if (!item.getLabel().equalsIgnoreCase("background")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(item.getLabel())
                            .append(", Prob:").append(item.getConfidence())
                            .append(" [")
                            .append(item.getLeft()).append(',')
                            .append(item.getTop()).append(',')
                            .append(item.getRight()).append(',')
                            .append(item.getBottom())
                            .append(']');
                    Log.d(TAG, sb.toString());

                    lists.add(sb.toString());
                }
            }
            ArrayAdapter<String> mResultAdapter = new ArrayAdapter<String>(ObjectDetectActivity.this, R.layout.list_item, R.id.item, lists);

            listView.setAdapter(mResultAdapter);

            File beDeletedFile = new File(retImgPath);
            if(beDeletedFile.exists())
            {
                beDeletedFile.delete();
            }
        }
    }
}
