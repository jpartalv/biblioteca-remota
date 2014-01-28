package com.example.bibliotecaremota;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

/**
 * @author Jean Pierre Arteaga Alvarez 
 * @version 1.0
 * 		
 * 		Example of interface that loads and links epubs from Dropbox and reads their metadata. 
 * 
 * */

public class ActPrincipal extends Activity {

	private static final String appKey = "sweyj6hueeuxauj";
	private static final String appSecret = "ez1wfgtp83n5om7";
	
	private DbxAccountManager accManager;
	static final int REQUEST_LINK_TO_DBX = 0;
	
	DbxFileSystem dbxFs;
	GestureDetector gestureDetector;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_login);
		
		accManager = DbxAccountManager.getInstance(getApplicationContext(),appKey,appSecret );
		if (accManager.getLinkedAccount() == null)
			accManager.startLink((Activity)this, REQUEST_LINK_TO_DBX);
		else
			loadList(false);
	}

	
	List<DbxFileInfo> epubs = new Vector<DbxFileInfo>();
	
	private void buildList(List<DbxFileInfo> files)
	{
		for (DbxFileInfo file:files)
		{
			if (file.isFolder)
				try {
					buildList(dbxFs.listFolder(file.path));
				} catch (DbxException e) {
					e.printStackTrace();
				}
			
			else
				epubs.add(file);
		}
		
	}
	
	private void loadList(boolean sortByDate)
	{
		gestureDetector = new GestureDetector(ActPrincipal.this,new GestureDetector.SimpleOnGestureListener()
		{
		    @Override
		    public boolean onDoubleTap(MotionEvent event) {
		        Log.i("double tap","doble tap");
		        return true;
		    }

		    @Override
		    public boolean onDown(MotionEvent arg0) {
		    	Log.i("down","down");
		        return false;
		    }
		}
		);
		
		
		findViewById(R.id.txtMsg1).setVisibility(View.GONE);
		findViewById(R.id.btnReintentar).setVisibility(View.GONE);
		
		try {
			/*DbxFileSystem*/ dbxFs = DbxFileSystem.forAccount(accManager.getLinkedAccount());
			DbxPath path = new DbxPath("/");
			
			List<DbxFileInfo> files = dbxFs.listFolder(path);
			epubs.clear();
			buildList(files);
			
			LinearLayout layoutLista = (LinearLayout)findViewById(R.id.lltLista);
			
			if (sortByDate)
				Collections.sort(/*files*/epubs,new DatesComparator());
			else
				Collections.sort(/*files*/epubs,new TitlesComparator());
			
			//for (DbxFileInfo file:files)
			for (DbxFileInfo file:epubs)
			{
				if (!file.isFolder)
				{
					String fileName = file.path.getName();
					String[] splitedName = fileName.split("\\.");
					if (splitedName.length > 1 && splitedName[1].equals("epub"))
					{
						LinearLayout layoutItem = (LinearLayout)getLayoutInflater().inflate(R.layout.item_lista,null);
						TextView txtView = (TextView)layoutItem.getChildAt(1);
						txtView.setText(splitedName[0]);
						
						ImageView imgView = (ImageView)layoutItem.getChildAt(0);
						imgView.setTag(file.path);
						imgView.setOnTouchListener(gestureListener);
						
						layoutLista.addView(layoutItem);
					}
				}
			}
		
		} catch (Unauthorized e) {
			e.printStackTrace();
		} catch (DbxException e) {
			e.printStackTrace();
		}
	}
	
	View.OnTouchListener gestureListener = new View.OnTouchListener() {
       
		public boolean onTouch(View v, MotionEvent event) {
			if (gestureDetector.onTouchEvent(event)) {
	        	try {
		        	// load file from dropbox
		        	DbxPath path = (DbxPath)v.getTag();
				    DbxFileSystem dbxFs;
					
						dbxFs = DbxFileSystem.forAccount(accManager.getLinkedAccount());
					
				    DbxFile file;
				    
				    file = dbxFs.open(path);
				    
					// load book cover from inputStream
				    Book book = (new EpubReader()).readEpub(file.getReadStream());
				    
				    if (book.getCoverImage()==null)
				    	Toast.makeText(ActPrincipal.this, "No existe covertura disponible",Toast.LENGTH_SHORT).show();
				    else
					{
				    	Bitmap coverBmp = BitmapFactory.decodeStream(book.getCoverImage().getInputStream());
					    Drawable bmpDrawable = new BitmapDrawable(coverBmp);
				    
					    LayoutInflater inflater = getLayoutInflater();
					    LinearLayout view = (LinearLayout)inflater.inflate(R.layout.image_toast,
					                                   (ViewGroup) findViewById(R.id.lltToast));
	
					    Toast toast = new Toast(ActPrincipal.this);
					    ((ImageView)view.getChildAt(0)).setBackgroundDrawable(bmpDrawable);
					    toast.setView(view);
					    toast.show();
				    }
				    file.close();
				    
		            return true;
	        	} catch (Unauthorized e) {
					e.printStackTrace();
				}
	        	catch (DbxException e) {
					e.printStackTrace();
				}
	        	catch (IOException e) {
					e.printStackTrace();
			}
        }   
        return true;
        }
    };

	public class DatesComparator implements Comparator<DbxFileInfo> {
		@Override
		public int compare(DbxFileInfo lhs, DbxFileInfo rhs) {
			return lhs.modifiedTime.compareTo(rhs.modifiedTime);
		}
	}
	
	public class TitlesComparator implements Comparator<DbxFileInfo> {
		@Override
		public int compare(DbxFileInfo lhs, DbxFileInfo rhs) {
			return lhs.path.getName().compareTo(rhs.path.getName());
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_LINK_TO_DBX) {
	        if (resultCode == Activity.RESULT_OK) {
	        	loadList(false);
	        } else {
	            // link failed or was cancelled by the user.
	        }
	    } else {
	        super.onActivityResult(requestCode, resultCode, data);
	    }
	}
	
	public boolean clickReintentar(View v)
	{
		accManager.startLink((Activity)this, REQUEST_LINK_TO_DBX);
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_act_principal, menu);
		return true;
	}
	
	@Override  
    public boolean onOptionsItemSelected(MenuItem item) {  
        switch (item.getItemId()) {  
            case R.id.opc_titulo: 
            	((LinearLayout)findViewById(R.id.lltLista)).removeAllViews();
              loadList(false); 
            return true;     
  
            case R.id.opc_fecha:
            	((LinearLayout)findViewById(R.id.lltLista)).removeAllViews();
            	loadList(true);  
              return true; 
             default:
            	 return false;
           
        }  
    }  
}
