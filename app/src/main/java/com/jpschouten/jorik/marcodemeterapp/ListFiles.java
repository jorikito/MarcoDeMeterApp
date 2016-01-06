package com.jpschouten.jorik.marcodemeterapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ListFiles extends AsyncTask<Void, Void, String>{

	private DropboxAPI dropboxApi;
	private String path;
	private Handler handler;
	
	public ListFiles(DropboxAPI dropboxApi, String path, Handler handler) {
		super();
		this.dropboxApi = dropboxApi;
		this.path = path;
		this.handler = handler;
	}

	@Override
	protected String doInBackground(Void... params) {
		
//		ArrayList files = new ArrayList();
		String content = "";
		try {
//			Entry directory = dropboxApi.metadata(path, 1000, null, true, null);
//			for(Entry entry : directory.contents) {
//				files.add(entry.fileName());
//			}
//            String filePath = "/MeterData/test3.txt";

            DropboxAPI.DropboxInputStream inputStream = dropboxApi.getFileStream(path, null);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line+"\n");
            }
            content = total.toString();
		} catch (Exception e) {
            e.printStackTrace();
		}
		
		return content;
	}

	@Override
	protected void onPostExecute(String result) {
		Message message = handler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putString("data", result);
		message.setData(bundle);
		handler.sendMessage(message);
	}
}