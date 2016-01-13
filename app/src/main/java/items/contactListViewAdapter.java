package items;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import activity.mychat.R;
import java.util.List;

//Klasse für einen ListViewAdapter um die abgespeicherten contactItem´s
//zu erstellen und diese dann anzuzeigen
public class contactListViewAdapter extends ArrayAdapter<contactItem> {

    //Context Element
    private Context context;

    //Methode der die contactItems übergeben werden
    public contactListViewAdapter(Context context, int resourceId,List<contactItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    //Viewholder Methode
    private class ViewHolder {
        TextView txtName;
        TextView txtNumbermessages;
    }

    //Methode um ListView zu initialisieren und die einträge des contactListViewAdapter
    //in den entsprechenden Textfeldern anzuzeigen
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        contactItem rowItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.contact_item, null);
            holder = new ViewHolder();
            holder.txtName = (TextView) convertView.findViewById(R.id.txtitemusername);
            holder.txtNumbermessages = (TextView) convertView.findViewById(R.id.txtitemusernumbermessages);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtName.setText(rowItem.getName());
        holder.txtNumbermessages.setText(rowItem.getNumbermessages());

        return convertView;
    }
}
