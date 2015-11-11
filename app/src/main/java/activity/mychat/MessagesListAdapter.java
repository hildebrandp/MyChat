package activity.mychat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class MessagesListAdapter extends BaseAdapter {

    private Context context;
    private List<Message> messagesItems;

    public MessagesListAdapter(Context context, List<Message> navDrawerItems) {
        this.context = context;
        this.messagesItems = navDrawerItems;
    }

    @Override
    public int getCount() {
        return messagesItems.size();
    }

    @Override
    public Object getItem(int position) {
        return messagesItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        /**
         * The following list not implemented reusable list items as list items
         * are showing incorrect data Add the solution if you have one
         * */

        Message m = messagesItems.get(position);

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        // Identifying the message owner
        if (!messagesItems.get(position).getsenderID().equals(Main_activity.user.getString("USER_ID","0"))) {

            convertView = mInflater.inflate(R.layout.list_item_message_right, null);
        }else{

            convertView = mInflater.inflate(R.layout.list_item_message_left, null);
        }

        TextView message = (TextView) convertView.findViewById(R.id.txtMsg);
        TextView date = (TextView) convertView.findViewById(R.id.txtDate);
        TextView verified = (TextView) convertView.findViewById(R.id.txtVerified);

        message.setText(m.getchatMessage());
        date.setText(m.getchatDate());
        verified.setText(m.getchatVerified());

        return convertView;
    }
}