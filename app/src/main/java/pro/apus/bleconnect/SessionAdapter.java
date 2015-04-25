package pro.apus.bleconnect;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import pro.apus.heartrate.R;

public class SessionAdapter extends ArrayAdapter<Session> {
    private final Activity context;
    private final List<Session> sessions;

    static class ViewHolder {
        public TextView firstline;
        public TextView secondline;
    }

    public SessionAdapter(Activity context, List<Session> sessions) {
        super(context, R.layout.session_list_view, sessions);
        this.context = context;
        this.sessions = sessions;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.session_list_view, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.firstline = (TextView) rowView.findViewById(R.id.firstLine);
            viewHolder.secondline = (TextView) rowView
                    .findViewById(R.id.secondLine);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        Session s = sessions.get(position);
        holder.firstline.setText("Session " + s.getId());
        holder.secondline.setText(SimpleDateFormat.getDateTimeInstance().format(new Date(s.getTime())));
        return rowView;
    }
}

