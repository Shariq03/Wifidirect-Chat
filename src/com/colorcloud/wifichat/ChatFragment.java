package com.colorcloud.wifichat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import com.colorcloud.wifichat.R;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * chat fragment attached to main activity.
 */
public class ChatFragment extends ListFragment {
	private static final String TAG = "PTP_ChatFrag";
	
	private static ChatActivity mActivity = null;

	private ArrayList<MsgRow> mMessageList = null;   // a list of chat msgs.
    private ArrayAdapter<MsgRow> mAdapter= null;
    
    private String mGroupOwnerAddr;
    private String mMyAddr;
    
	/**
     * Static factory to create a fragment object from tab click.
     */
    public static ChatFragment newInstance(Activity activity, String groupOwnerAddr, String msg) {
    	ChatFragment f = new ChatFragment();
    	mActivity = (ChatActivity)activity;
    	
        Bundle args = new Bundle();
        args.putString("groupOwnerAddr", groupOwnerAddr);
        args.putString("initMsg", msg);
        f.setArguments(args);
        Log.d(TAG, "newInstance :" + groupOwnerAddr + " : " + msg);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {  // this callback invoked after newInstance done.  
        super.onCreate(savedInstanceState);
        mMyAddr = ((WiFiChatApp)mActivity.getApplication()).mMyAddr;
        
        setRetainInstance(true);   // Tell the framework to try to keep this fragment around during a configuration change.
    }
    
    /**
     * the data you place in the Bundle here will be available in the Bundle given to onCreate(Bundle), etc.
     * only works when your activity is destroyed by android platform. If the user closed the activity, no call of this.
     * http://www.eigo.co.uk/Managing-State-in-an-Android-Activity.aspx
     */
    @Override
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	outState.putParcelableArrayList("MSG_LIST", mMessageList);
    	Log.d(TAG, "onSaveInstanceState. " + mMessageList.get(0).mMsg);
    }
    
    /**
     * no matter your fragment is declared in main activity layout, or dynamically added thru fragment transaction
     * You need to inflate fragment view inside this function. 
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	// inflate the fragment's res layout. 
        View contentView = inflater.inflate(R.layout.chat_frag, container, false);  // no care whatever container is.
        final EditText inputEditText = (EditText)contentView.findViewById(R.id.edit_input);
        final Button sendBtn = (Button)contentView.findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// send the chat text in current line to the server
				String inputMsg = inputEditText.getText().toString();
				inputEditText.setText("");
				InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
				
				String formatedmsg = appendChatMessage(new MsgRow(mMyAddr, inputMsg, null));
				Log.d(TAG, "sendButton clicked: sendOut data : " + formatedmsg);
				mActivity.pushOutMessage(formatedmsg);
			}
        });
        
        String groupOwnerAddr = getArguments().getString("groupOwnerAddr");
        mGroupOwnerAddr = groupOwnerAddr;
        String msg = getArguments().getString("initMsg");
        Log.d(TAG, "onCreateView : fragment view created: msg :" + msg);
        
    	if( savedInstanceState != null ){
            mMessageList = savedInstanceState.getParcelableArrayList("MSG_LIST");
            Log.d(TAG, "onCreate : savedInstanceState: " + mMessageList.get(0).mMsg);
        }else if( mMessageList == null ){
        	// no need to setContentView, just setListAdapter, but listview must be android:id="@android:id/list"
            mMessageList = new ArrayList<MsgRow>(200);
            Log.d(TAG, "onCreate : empty start : ");
        }else {
        	Log.d(TAG, "onCreate : setRetainInstance good : ");
        }
        
        mAdapter = new ChatMessageAdapter(mActivity, mMessageList);
        String initmsg = getArguments().getString("initMsg");
        Log.d(TAG, "onCreate chat msg fragment: my_addr: " + mMyAddr + " : " + initmsg);
        
        if( initmsg != null ){
        	MsgRow row = MsgRow.parseMsgRow(initmsg);
        	mMessageList.add(row);
        	Log.d(TAG, "onCreate : " + row.mMsg);
        }else if (mMessageList.size() == 0){
        	mMessageList.add(new MsgRow(mMyAddr, mMyAddr + " logged in", null));
        }
        setListAdapter(mAdapter);  // list fragment data adapter 
        
        return contentView;
    }
    
    @Override 
    public void onDestroyView(){ 
    	super.onDestroyView(); 
    	Log.d(TAG, "onDestroyView: ");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {  // invoked after fragment view created.
        super.onActivityCreated(savedInstanceState);
        
        setHasOptionsMenu(true);
        Log.d(TAG, "onActivityCreated: chat fragment displayed ");
    }
    
    /**
     * add a chat message to the list, return the format the message as " sender_addr : msg "
     */
    public String appendChatMessage(MsgRow msg) {
    	Log.d(TAG, "appendChatMessage: chat fragment append msg: " + msg);
    	mMessageList.add(msg);
    	// mAdapter.add(msg);
    	mAdapter.notifyDataSetChanged();  // notify the attached observer and views to refresh.
    	return msg.toString();
    }
    
    /**
     * chat message adapter from list adapter.
     * Responsible for how to show data to list fragment list view.
     */
    final class ChatMessageAdapter extends ArrayAdapter<MsgRow> {

    	private LayoutInflater mInflater;
    	
		public ChatMessageAdapter(Context context, List<MsgRow> objects){
			super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
		
		@Override
        public int getItemViewType(int position) {
			return IGNORE_ITEM_VIEW_TYPE;   // do not care			
		}
		
		/**
		 * assemble each row view in the list view.
		 */
		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;  // old view to re-use if possible. Useful for Heterogeneous list with diff item view type.
			MsgRow item = this.getItem(position);
            
            if( view == null ){
            	view = mInflater.inflate(R.layout.chat_row, null);
            }
            
            TextView sender = (TextView)view.findViewById(R.id.sender);
            sender.setText(item.mSender);
            
            TextView msgRow = (TextView)view.findViewById(R.id.msg_row);
            msgRow.setText(item.mMsg);
            
            TextView time = (TextView)view.findViewById(R.id.time);
            time.setText(item.mTime);
            
            // set the background color
            if( ! item.mSender.equals(mMyAddr)){
            	// view.setBackgroundResource(R.drawable.row_bkgrd);
            	msgRow.setTextColor(Color.GREEN);
            	sender.setTextColor(Color.GREEN);
            }else{
            	msgRow.setTextColor(Color.CYAN);
            	sender.setTextColor(Color.CYAN);
            }
            time.setTextColor(Color.BLUE);
            
            Log.d(TAG, "getView : " + item.mSender + " " + item.mMsg + " " + item.mTime);
            return view;
		}
    }
    
    /**
	 * msg row format
	 */
	public static class MsgRow implements Parcelable{
		public String mSender;
		public String mMsg;
		public String mTime;
		public static final String mDel = "^&^";
		
		private MsgRow() { 
			this.mSender = null;
			this.mTime = null;
			this.mMsg = null;
		}
		
		public MsgRow(String sender, String msg, String time){
			mTime = time;
			if( time == null ){
				Date now = new Date();
				//SimpleDateFormat timingFormat = new SimpleDateFormat("mm/dd hh:mm");
				mTime = new SimpleDateFormat("dd/MM HH:mm").format(now); 
			} 
			mSender = sender;
			mMsg = msg;
		}
		
		public MsgRow(Parcel in) {
	        readFromParcel(in);
	    }
		
		public String toString() {
			return mSender + mDel + mMsg + mDel + mTime;
		}
		
		public static MsgRow parseMsgRow(String formattedMsg){
			StringTokenizer st = new StringTokenizer(formattedMsg, mDel);
			MsgRow row = new MsgRow();
			while( st.hasMoreTokens()){
				// Order matters
				if( row.mSender == null ){
					row.mSender = st.nextToken();
					continue;
				}
				if( row.mMsg == null ){
					row.mMsg = st.nextToken();
					continue;
				}
				if( row.mTime == null ){
					row.mTime = st.nextToken();
					break;  // done
				}
			}
			Log.d(TAG, "parseMsgRow : " +row.mMsg);
			return row;
		}

		public static final Parcelable.Creator<MsgRow> CREATOR = new Parcelable.Creator<MsgRow>() {
	        public MsgRow createFromParcel(Parcel in) {
	            return new MsgRow(in);
	        }
	 
	        public MsgRow[] newArray(int size) {
	            return new MsgRow[size];
	        }
	    };
	    
		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(mSender);
			dest.writeString(mMsg);
			dest.writeString(mTime);
		}
		
		public void readFromParcel(Parcel in) {
			mSender = in.readString();
			mMsg = in.readString();
			mTime = in.readString();
	    }
	}
}
