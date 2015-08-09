package pl.legowski.wikiaprogrammingtask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Vector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	private static final int ENTRIES_PER_BATCH = 25;
		
	class WikiaEntry extends Object
	{
		String name;
		String URL;
		String hub;
		int	ID;
		Bitmap image;
		
		public WikiaEntry(String aName, String aUrl, String aHub, int aID) {
			name = aName;
			URL = aUrl;
			hub = aHub;
			ID = aID;
			image = null;
		}	
		
		public void setBitmap(Bitmap aBitmap){
			image = aBitmap;
		}
	}
	
	protected class getWikiaEntries extends AsyncTask<Void, Void, Vector<WikiaEntry>> {
		ProgressDialog dialog;
		@Override
		protected void onPreExecute() {
			bDownloadingLocked = true;
			if(currentBatch==1)
			{
				dialog = new ProgressDialog(MainActivity.this);
				dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				dialog.setMessage(getResources().getString(R.string.loading_data));
				dialog.setCancelable(false);
				dialog.show();
			}
			super.onPreExecute();
		}
		
		@Override
		protected Vector<WikiaEntry> doInBackground(Void... params) {
			String tempName = "", tempUrl = "", tempHub = "";
			int tempId;
			WikiaEntry tempWikiaEntry;
			Vector<WikiaEntry> ansWikiaEntries = new Vector<WikiaEntry>();
			//Getting JSON Object
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			String qURL = "http://www.wikia.com/api/v1/Wikis/List?batch="+String.valueOf(currentBatch)+"&limit="+String.valueOf(ENTRIES_PER_BATCH);
			HttpGet httpGet = new HttpGet(qURL);
			
			JSONObject jsObject = null;
			try 
			{
				HttpResponse response = httpClient.execute(httpGet,localContext);
				HttpEntity entity = response.getEntity();

				if (entity != null) 
				{
					InputStream inputStream = entity.getContent();
					Reader in = new InputStreamReader(inputStream);		
					BufferedReader bufferedreader = new BufferedReader(in);
					StringBuilder stringBuilder = new StringBuilder();
					String stringReadLine = null;
					while ((stringReadLine = bufferedreader.readLine()) != null) 
					{
						stringBuilder.append(stringReadLine);
					}
					jsObject = new JSONObject(stringBuilder.toString());
				}

			}
			catch(Exception e)
			{				
				e.printStackTrace();
				handler.post(new Runnable()
				{
					public void run() {Toast.makeText(getBaseContext(), getResources().getString(R.string.connection_problem), Toast.LENGTH_SHORT).show();};									
				});
				return ansWikiaEntries;
			}
			
			//Analyzing JSON Data			
			JSONObject tempJsObject;
			JSONArray tempJsArray;			
			try 
			{
				if(maxBatches==0)
					maxBatches = jsObject.getInt("batches");
				tempJsArray = jsObject.getJSONArray("items");				
				int i = 0;
				while(i<ENTRIES_PER_BATCH)			//JSONException at end of JsArray will break loop
				{					
					tempJsObject = tempJsArray.getJSONObject(i);
					tempName = tempJsObject.getString("name");
					tempUrl = tempJsObject.getString("domain");
					tempHub = tempJsObject.getString("hub");
					tempId = tempJsObject.getInt("id");
					WikiaEntry wikiaEntry = new WikiaEntry(tempName,tempUrl,tempHub,tempId);				
					ansWikiaEntries.add(wikiaEntry);
					i++;
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
			
			
			qURL = "http://www.wikia.com/api/v1/Wikis/Details?ids=";
			
			Iterator<WikiaEntry> iteratorWikiaEntry = ansWikiaEntries.iterator();
			while(iteratorWikiaEntry.hasNext())
			{
				qURL = qURL + Integer.toString(iteratorWikiaEntry.next().ID) +",";
			}
			qURL = qURL.substring(0, qURL.length()-1);		
			
			
			httpGet = new HttpGet(qURL);
			try 
			{
				HttpResponse response = httpClient.execute(httpGet,localContext);
				HttpEntity entity = response.getEntity();

				if (entity != null) 
				{
					InputStream inputStream = entity.getContent();
					Reader in = new InputStreamReader(inputStream);		
					BufferedReader bufferedreader = new BufferedReader(in);
					StringBuilder stringBuilder = new StringBuilder();
					String stringReadLine = null;
					while ((stringReadLine = bufferedreader.readLine()) != null) 
					{
						stringBuilder.append(stringReadLine);
					}
					jsObject = new JSONObject(stringBuilder.toString());
				}

			}
			catch(Exception e)
			{				
				e.printStackTrace();
				handler.post(new Runnable()
				{
					public void run() {Toast.makeText(getBaseContext(), getResources().getString(R.string.connection_problem), Toast.LENGTH_SHORT).show();};									
				});
				return ansWikiaEntries;
			}
			
			try {
				jsObject = jsObject.getJSONObject("items");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			
			iteratorWikiaEntry = ansWikiaEntries.iterator();
			while(iteratorWikiaEntry.hasNext())
			{
				try {
					tempWikiaEntry = iteratorWikiaEntry.next();
					tempJsObject = jsObject.getJSONObject(Integer.toString(tempWikiaEntry.ID));					
					//Trying to download wordmark			
					String imageUrl = tempJsObject.getString("wordmark");
					int response = -1;
					URL url = new URL(imageUrl);
					URLConnection conn = url.openConnection();
				
					if (!(conn instanceof HttpURLConnection))
						throw new IOException("Not an HTTP connection");
					HttpURLConnection httpConn = (HttpURLConnection) conn;
					httpConn.setAllowUserInteraction(false);
					httpConn.setInstanceFollowRedirects(true);
					httpConn.setRequestMethod("GET");
					httpConn.connect();
				
					response = httpConn.getResponseCode();
					InputStream in = null;
					if (response == HttpURLConnection.HTTP_OK) 
					{
						in = httpConn.getInputStream();
					}
					tempWikiaEntry.setBitmap(BitmapFactory.decodeStream(in));
					in.close();
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			
			}
			
			//Adding new entry to List
			return ansWikiaEntries;
		}
		
		@Override
		protected void onPostExecute(Vector<WikiaEntry> result) {	
			if(!result.isEmpty())
			{
				addEntries(result);
				currentBatch++;
			}
			if(dialog!=null && dialog.isShowing())
			{
				dialog.dismiss();
			}			
			bDownloadingLocked = false;
			super.onPostExecute(result);
		}
	}
	
	class MyListAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return ListEntriesViews.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return ListEntriesViews.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {				
			return ListEntriesViews.get(position);
		}
		
	}
	
	private short currentBatch;
	private Vector<View> ListEntriesViews;	
	private ListView list;
	private Handler handler;
	private boolean bDownloadingLocked;
	private int maxBatches;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListEntriesViews = new Vector<View>();
		handler = new Handler();
		currentBatch = 1;
		if(!isOnline())
		{
			Toast.makeText(getBaseContext(), getResources().getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
			bDownloadingLocked = true;
			handler.postDelayed(new Runnable(){ @Override public void run() {finish();}},2000);			
		}
		setContentView(R.layout.menu_main);
		list = (ListView)findViewById(R.id.idListWikia);
		MyListAdapter adapter = new MyListAdapter();
		list.setAdapter(adapter);
		list.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if(!bDownloadingLocked && view.getId() == list.getId() && firstVisibleItem + visibleItemCount >= totalItemCount)
				{
					if(currentBatch>maxBatches && maxBatches!=0)
					{
						//no more entries, lock downloading forevah
						bDownloadingLocked = true;
						return;
					}
					new getWikiaEntries().execute();
				}													
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {}
		});		
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
				String url = (String) ((TextView)(((View)parent.getItemAtPosition(position)).findViewById(R.id.idUrl))).getText();				
				if (!url.startsWith("http://") && !url.startsWith("https://")) {
					url = "http://" + url;
				}
				Intent webintent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(webintent);
			}
		});
	}
	
	protected void showWeb(String text) {
		if (!text.startsWith("http://") && !text.startsWith("https://")) {
			text = "http://" + text;
		}
		Intent iweb = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
		startActivity(iweb);		
	}

	public boolean isOnline() {
	    ConnectivityManager cm =
	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	public void addEntries(Vector<WikiaEntry> result) {		
		WikiaEntry tempEntry;				
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		Iterator<WikiaEntry> itek = result.iterator();
		while(itek.hasNext())
		{
			//Saving entry
			tempEntry = itek.next();
			//Creating list item
			View listViewEntry = inflater.inflate(R.layout.list_entry,null);	
			((TextView)listViewEntry.findViewById(R.id.idLocalId)).setText(Integer.toString(ListEntriesViews.size()+1));
			((TextView)listViewEntry.findViewById(R.id.idHub)).setText(tempEntry.hub);
			((TextView)listViewEntry.findViewById(R.id.idUrl)).setText(tempEntry.URL);			
			if(tempEntry.image == null)
			{
				((ImageView)listViewEntry.findViewById(R.id.idImage)).setVisibility(View.INVISIBLE);
				((TextView)listViewEntry.findViewById(R.id.idTitle)).setText(tempEntry.name);
			}
			else
			{
				((ImageView)listViewEntry.findViewById(R.id.idImage)).setImageBitmap(tempEntry.image);
				((TextView)listViewEntry.findViewById(R.id.idTitle)).setVisibility(View.INVISIBLE);
			}					
			ListEntriesViews.add(listViewEntry);		
		}		
		//Flushing data
		list.invalidateViews();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
