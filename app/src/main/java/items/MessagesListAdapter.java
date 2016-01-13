package items;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import activity.mychat.Main_activity;
import activity.mychat.R;
import items.Message;

//Klasse für einen ListViewAdapter um die abgespeicherten Message Items
//zu erstellen und diese dann anzuzeigen
public class MessagesListAdapter extends BaseAdapter {

    //Liste für die einträge und den Context
    private Context context;
    private List<Message> messagesItems;

    //Methode der die Einträge übergeben werden
    public MessagesListAdapter(Context context, List<Message> navDrawerItems) {
        this.context = context;
        this.messagesItems = navDrawerItems;
    }

    //Methode die die anzahl der Elemente zurückgibt
    @Override
    public int getCount() {
        return messagesItems.size();
    }

    //Methode die das Element an der gewünschten position zurückgibt
    @Override
    public Object getItem(int position) {
        return messagesItems.get(position);
    }

    //Methode die die ID der Elements zurück gibt an der gewünschten position
    @Override
    public long getItemId(int position) {
        return position;
    }

    //Methode um ListView zu initialisieren und die einträge des MessageListAdapter
    //in den entsprechenden Textfeldern anzuzeigen
    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //Lade Element aus dem Array
        Message m = messagesItems.get(position);

        //Initialisiere LayoutInflater
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        //Überprüfe ob die Nachricht von einem Selbst ist oder ob man die Nachricht Empfangen hat
        if (messagesItems.get(position).getsenderID().equals(Main_activity.user.getString("USER_ID","0"))) {

            //Nachricht von einem Selbst, also Speicher es in dem View für die Rechte Seite
            convertView = mInflater.inflate(R.layout.list_item_message_right, null);
        }else{

            //Empfagnene Nachricht, also Nachricht in View für Linke Seite Speichern
            convertView = mInflater.inflate(R.layout.list_item_message_left, null);
        }

        //Nachricht in Textfelder Speichern
        TextView message = (TextView) convertView.findViewById(R.id.txtMsg);
        TextView date = (TextView) convertView.findViewById(R.id.txtDate);
        TextView verified = (TextView) convertView.findViewById(R.id.txtVerified);

        message.setText(m.getchatMessage());
        date.setText(m.getchatDate());
        verified.setText(m.getchatVerified());

        return convertView;
    }
}