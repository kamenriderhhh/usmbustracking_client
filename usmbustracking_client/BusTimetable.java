package wow.usmbustracking_client;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by KeroNG on 26/11/2018.
 */
public class BusTimetable extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bus_timetable);

        LinearLayout gallery = findViewById(R.id.gallery);
        LayoutInflater inflater = LayoutInflater.from(this);

        for(int i = 0; i < 6; i++)
        {
            View view = inflater.inflate(R.layout.bus_timetable_item, gallery, false);

            ImageView imageView = view.findViewById(R.id.imageView);
            switch(i)
            {
                case 0:
                    imageView.setImageResource(R.drawable.bus_a);
                    break;
                case 1:
                    imageView.setImageResource(R.drawable.bus_ac);
                    break;
                case 2:
                    imageView.setImageResource(R.drawable.bus_b);
                    break;
                case 3:
                    imageView.setImageResource(R.drawable.bus_c);
                    break;
                case 4:
                    imageView.setImageResource(R.drawable.bus_d);
                    break;
                default:
                    imageView.setImageResource(R.drawable.bus_e);
                    break;
            }
            gallery.addView(view);
        }
        /* for a pic only
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int)(width*.8),(int)(height*.6));
        */
    }
}
