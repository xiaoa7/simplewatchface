package org.p3p.wf.sawf;

import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SettingActivity extends WearableActivity {
    private final String baseurl = "http://www.xiaoa7.top:8080/res/bg/";
    private ListView listView;
    private MyAdapter adapter;
    private String currentSelectBg = null;
    private List<Map<String, Object>> listdata = new ArrayList<>();
    private static final int REQUEST = 112;
    private String usercode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getSharedPreferences("sp_sawf", Context.MODE_PRIVATE);
        usercode = sp.getString("usercode", null);
        if (usercode == null) {
            Intent intent = new Intent();
            intent.setClass(this, InputCodeActivity.class);
            startActivity(intent);
            this.finish();
            return;
        }

        setContentView(R.layout.activity_setting);
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                currentSelectBg = (String) (listdata.get(position).get("image"));
            }
        });
        new MyTask().execute();


        setAmbientEnabled();
    }

    /**
     * @param context
     * @param permissions
     * @return
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param v
     */
    public void onSave(View v) {
        Toast.makeText(this, "准备下载背景图!", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST);
            } else {
                new DownloadBackgroundTask().execute();
            }
        } else {
            new DownloadBackgroundTask().execute();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new DownloadBackgroundTask().execute();
                } else {
                    Toast.makeText(this, "需要使用本地存储权限", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     *
     */
    private void onDownloadBGSuccess() {
        Message msg = new Message();
        msg.what = 25;
        MyAnalogWatchFace.configHandler.sendMessage(msg);
        this.finish();
    }

    /**
     * MyTask继承线程池AsyncTask用来网络数据请求、json解析、数据更新等操作。
     */
    class DownloadBackgroundTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                downloadToFile(baseurl + usercode + "/" + currentSelectBg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onDownloadBGSuccess();
        }
    }


    /**
     * MyTask继承线程池AsyncTask用来网络数据请求、json解析、数据更新等操作。
     */
    class MyTask extends AsyncTask<String, Void, String> {
        /**
         * 数据请求前显示dialog。
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * 在doInBackground方法中，做一些诸如网络请求等耗时操作。
         */
        @Override
        protected String doInBackground(String... params) {
            try {
                return RequestConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 在该方法中，主要进行一些数据的处理，更新。
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) try {
                JSONArray jsonArray = new JSONArray(result);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject objectOne = jsonArray.optJSONObject(i);
                    String image = objectOne.optString("image");
                    String s_image = objectOne.optString("s_image");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("image", image);
                    map.put("s_image", s_image);
                    listdata.add(map);
                    Log.d("mytag", s_image);
                }
                adapter = new MyAdapter(SettingActivity.this, listdata);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 网络数据请求
     *
     * @return
     */
    private String RequestConfig() throws IOException {
        URL url = new URL(baseurl+usercode+"/config.json");
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setConnectTimeout(5 * 1000);
        urlConn.setReadTimeout(5 * 1000);
        urlConn.setUseCaches(true);
        urlConn.setRequestMethod("GET");
        urlConn.setRequestProperty("Content-Type", "application/json");
        urlConn.addRequestProperty("Connection", "Keep-Alive");
        urlConn.connect();
        String result = null;
        if (urlConn.getResponseCode() == 200) {
            result = streamToString(urlConn.getInputStream());
        } else {
        }
        urlConn.disconnect();
        return result;
    }

    /**
     * 将输入流转换成字符串
     *
     * @param is 从网络获取的输入流
     * @return
     */
    private String streamToString(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            baos.close();
            is.close();
            byte[] byteArray = baos.toByteArray();
            return new String(byteArray);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param
     */
    private void downloadToFile(String imageurl) throws IOException {
        File parent_path = Environment.getExternalStorageDirectory();

        // 可以建立一个子目录专门存放自己专属文件
        File dir = new File(parent_path.getAbsoluteFile(), "samplewf");
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdir();
        } else {
            Log.d("mytag", "表盘图片目录已经存在");
        }
        File file = new File(dir.getAbsoluteFile() + "/mywatchface.png");
        if (!file.exists()) {
            Log.d("mytag", file.getAbsolutePath());
            file.createNewFile();
        }
        //
        URL imageUrl = new URL(imageurl);
        HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);
        InputStream is = conn.getInputStream();
        OutputStream os = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        os.close();
        conn.disconnect();
    }

    //下载图片，封装Adapter
    class MyAdapter extends BaseAdapter {
        private Context context;
        private List<Map<String, Object>> list;

        public MyAdapter(Context context, List<Map<String, Object>> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item_layout, null);
                holder = new ViewHolder();

                holder.picname_hospital_s = (ImageView) convertView.findViewById(R.id.image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.picname_hospital_s.setImageResource(R.drawable.bg_item_icon);
            //接口回调的方法，完成图片的读取;
            DownImage downImage = new DownImage(baseurl+usercode+"/" + list.get(position).get("s_image").toString());
            downImage.loadImage(new ImageCallBack() {
                @Override
                public void getDrawable(Drawable drawable) {
                    holder.picname_hospital_s.setImageDrawable(drawable);
                }
            });

            return convertView;
        }

        class ViewHolder {
            ImageView picname_hospital_s;
        }
    }

    /**
     *
     */
    class DownImage {
        public String image_path;

        public DownImage(String image_path) {
            this.image_path = image_path;
        }

        public void loadImage(final ImageCallBack callBack) {

            final Handler handler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Drawable drawable = (Drawable) msg.obj;
                    callBack.getDrawable(drawable);
                }

            };

            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Drawable drawable = Drawable.createFromStream(new URL(
                                image_path).openStream(), "");

                        Message message = Message.obtain();
                        message.obj = drawable;
                        handler.sendMessage(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    /**
     *
     */
    interface ImageCallBack {
        public void getDrawable(Drawable drawable);
    }
}
