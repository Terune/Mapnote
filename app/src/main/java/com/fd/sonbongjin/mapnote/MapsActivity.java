package com.fd.sonbongjin.mapnote;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener,OnMarkerDragListener {

    private GoogleMap mMap;
    private String m_Text;
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;
    ArrayList<Mynote> al = new ArrayList<>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        helper = new MySQLiteOpenHelper(MapsActivity.this, // 현재 화면의 context
                "notes.db", // 파일명
                null, // 커서 팩토리
                1); // 버전 번호

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng seoul = new LatLng(37, 128);
        //mMap.addMarker(new MarkerOptions().position(seoul).title("Marker in Seoul").draggable(true));

        al.clear();
        select();

        Mynote cur_note;

        for(int i=0;i<al.size();i++)
        {
            cur_note=al.get(i);

            Marker temp;
            LatLng position= new LatLng(cur_note.latitude,cur_note.longitude);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(position);
            markerOptions.draggable(true);

            temp=mMap.addMarker(markerOptions);

            updatemarker(temp.getId(), cur_note.id);
        }
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(seoul));
        // Setting a custom info window adapter for the google map

        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker arg0) {

                Mynote now_note;
                // Getting view from the layout file info_window_layout
                View v = getLayoutInflater().inflate(R.layout.windowslayout, null);

                // Getting the position from the marker
               // LatLng latLng = arg0.getPosition();

                // Getting reference to the TextView to set latitude
                TextView note = (TextView) v.findViewById(R.id.note);

                now_note=findbymarker(arg0.getId());

                note.setText(now_note.note);


                // Returning the view containing InfoWindow contents
                return v;

            }
        });

//        // Adding and showing marker while touching the GoogleMap
//        googleMap.setOnMapClickListener(new OnMapClickListener() {
//
//            @Override
//            public void onMapClick(LatLng arg0) {
//                // Clears any existing markers from the GoogleMap
//
//
//            }
//        });

      //  mMap.setOnMapLongClickListener=new GoogleMap.OnMapLongClickListener();

    }
    @Override
    public void onMarkerDrag(Marker marker) {

    }
    @Override
    public void onMarkerDragStart(Marker marker) {

    }
    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng endposition = marker.getPosition();

        update(marker.getId(), null, endposition, 2);
    }

    @Override
    public void onMapLongClick(LatLng point) {

        final LatLng mark_point=point;

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("Type note");

        final EditText input = new EditText(MapsActivity.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

               // mMap.clear();
                m_Text = input.getText().toString();
                // Creating an instance of MarkerOptions to set position
                MarkerOptions markerOptions = new MarkerOptions();

                // Setting position on the MarkerOptions
                markerOptions.position(mark_point);

                // Animating to the currently touched position
                mMap.animateCamera(CameraUpdateFactory.newLatLng(mark_point));

                if(m_Text.length()>140) {
                    m_Text=m_Text.substring(0, 140);
                    m_Text=m_Text+"...";
                }
                // Adding marker on the GoogleMap
                Marker marker = mMap.addMarker(markerOptions);

                insert(marker.getId(), m_Text, mark_point);

                //al.add
                // Showing InfoWindow on the GoogleMap
                marker.showInfoWindow();


                //mMap.addMarker(new MarkerOptions().position(mark_point).title(m_Text).snippet(m_Text).draggable(true));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();


        // Toast.makeText(getApplicationContext(), point.toString(), Toast.LENGTH_SHORT).show();
    }

    public void insert(String marker, String note, LatLng location) {
        db = helper.getWritableDatabase(); // db 객체를 얻어온다. 쓰기 가능

        ContentValues values = new ContentValues();
        // db.insert의 매개변수인 values가 ContentValues 변수이므로 그에 맞춤
        // 데이터의 삽입은 put을 이용한다.
        values.put("latitude", location.latitude);
        values.put("longitude", location.longitude);
        values.put("marker", marker);
        values.put("note", note);
        db.insert("notes", null, values); // 테이블/널컬럼핵/데이터(널컬럼핵=디폴트)
        // tip : 마우스를 db.insert에 올려보면 매개변수가 어떤 것이 와야 하는지 알 수 있다.
    }

    // update
    public void update (String marker, String note, LatLng location,int mode) {
        db = helper.getWritableDatabase(); //db 객체를 얻어온다. 쓰기가능

        ContentValues values = new ContentValues();
        if(mode==1) {
            values.put("note", note);    //age 값을 수정
        }
        else if(mode==2){
            values.put("latitude", location.latitude);
            values.put("longitude", location.longitude);
        }
        db.update("notes", values, "marker=?", new String[]{marker});
        /*
         * new String[] {name} 이런 간략화 형태가 자바에서 가능하다
         * 당연하지만, 별도로 String[] asdf = {name} 후 사용하는 것도 동일한 결과가 나온다.
         */

        /*
         * public int update (String table,
         * ContentValues values, String whereClause, String[] whereArgs)
         */
    }
    public void updatemarker (String marker,int id) {
        db = helper.getWritableDatabase(); //db 객체를 얻어온다. 쓰기가능

        ContentValues values = new ContentValues();
        values.put("marker", marker);    //age 값을 수정

        db.update("notes", values, "_id=?", new String[]{id+""});

    }

    // delete
    public void delete (String marker) {
        db = helper.getWritableDatabase();
        db.delete("notes", "marker=?", new String[]{marker});
        Log.i("db", marker + "정상적으로 삭제 되었습니다.");
    }

    // select
    public void select() {

        // 1) db의 데이터를 읽어와서, 2) 결과 저장, 3)해당 데이터를 꺼내 사용

        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
        Cursor c = db.query("notes", null, null, null, null, null, null);

        /*
         * 위 결과는 select * from student 가 된다. Cursor는 DB결과를 저장한다. public Cursor
         * query (String table, String[] columns, String selection, String[]
         * selectionArgs, String groupBy, String having, String orderBy)
         */

        while (c.moveToNext()) {
            // c의 int가져와라 ( c의 컬럼 중 id) 인 것의 형태이다.
            int id = c.getInt(c.getColumnIndex("_id"));
            String marker = c.getString(c.getColumnIndex("marker"));
            int latitude = c.getInt(c.getColumnIndex("latitude"));
            int longitude = c.getInt(c.getColumnIndex("longitude"));
            String note = c.getString(c.getColumnIndex("note"));
            Log.i("db", "id: " + id + ", latitude : " + latitude + ", longitude : " + longitude
                    +"marker : "+marker+ ", note : " + note);
           // Log.i("db", "id: " + id + ", latitude : " + latitude + ", longitude : " + longitude + ", note : " + note);

          //  Log.i("db", "id: " + id + ", marker : " + marker + ", note : " + note);

            Mynote m = new Mynote();
            m.id = id;
            m.marker=marker;
            m.latitude = latitude;
            m.longitude = longitude;
            m.note = note;
            al.add(m);
        }
        c.close();
    }
    public Mynote findbymarker(String marker) {

        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
        Cursor c= db.rawQuery("SELECT * FROM notes WHERE marker='" + marker+"'", null);

        c.moveToFirst();
        // c의 int가져와라 ( c의 컬럼 중 id) 인 것의 형태이다.
        int id = c.getInt(c.getColumnIndex("_id"));
        int latitude = c.getInt(c.getColumnIndex("latitude"));
        int longitude = c.getInt(c.getColumnIndex("longitude"));
      //  String marker = c.getString(c.getColumnIndex("marker"));
        String note = c.getString(c.getColumnIndex("note"));
        Log.i("db", "id: " + id + ", latitude : " + latitude + ", longitude : " + longitude
                +"marker : "+marker+ ", note : " + note);

        c.close();
        Mynote m = new Mynote();
        m.id = id;
        m.latitude = latitude;
        m.longitude = longitude;
        m.note = note;
        m.marker=marker;

        return m;
    }


}
