package lac.contextnet.sddl_pingservicetest;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class MessageHandler extends Handler {
	static final public String ACTION_UPDATE_CONNECTION_STATUS = "lac.contextnet.sddl_pingservicetest.broadcastmessage.ActionUpdateConnStatus";
	static final public String CONNECTION_STATUS = "lac.contextnet.sddl_pingservicetest.broadcastmessage.ConnStatus";
	
	private Context context;
	private LocalBroadcastManager broadcastManager;
	
	public MessageHandler(Context context)
	{
		this.context = context;
		
		/* Broadcast Receiver */
	    broadcastManager = LocalBroadcastManager.getInstance(context); //getBaseContext
	}
	
	@Override
	public void handleMessage(Message msg) 
	{
		super.handleMessage(msg);
		
		if (msg.getData().getString("status") != null) 
		{
			String status = msg.getData().getString("status");
			
			if (status.equals("connected")) 
				broadcastConnectionStatus(context.getResources().getText(R.string.msg_d_connected));
			else if (status.equals("disconnected")) 
				broadcastConnectionStatus(context.getResources().getText(R.string.msg_d_disconnected));
			else if (status.equals("package")) 
			{
				Serializable s = msg.getData().getSerializable("package");
				
				//broadcastConnectionStatus((String) s);
				Toast.makeText(context, ((String) s), Toast.LENGTH_LONG).show();				
			}
			else
				broadcastConnectionStatus(status);
		}
	}
	
	private void broadcastConnectionStatus(CharSequence charSequence) {
	    Intent intent = new Intent(ACTION_UPDATE_CONNECTION_STATUS);
	    intent.putExtra(CONNECTION_STATUS, (String) charSequence);
	    broadcastManager.sendBroadcast(intent);
	}
}
