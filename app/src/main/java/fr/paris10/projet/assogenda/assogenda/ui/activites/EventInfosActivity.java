package fr.paris10.projet.assogenda.assogenda.ui.activites;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import fr.paris10.projet.assogenda.assogenda.R;
import fr.paris10.projet.assogenda.assogenda.model.Association;
import fr.paris10.projet.assogenda.assogenda.model.Event;


public class EventInfosActivity extends AppCompatActivity {
    private ListView listInfos;
    private ArrayList<HashMap<String,Object>> listValues = new ArrayList<>();
    private SimpleAdapter adapter;
    private String eventUID;
    private Event event;
    private TextView nameEvent;
    private TextView nameAsso;
    private String name;
    private Date eventEndDate;
    private TextView participateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_infos);
        eventUID = (String) getIntent().getExtras().get("eventUID");
        name = (String) getIntent().getExtras().get("eventName");
        String eventEnd = (String) getIntent().getExtras().get("eventEndDate");
        nameEvent = (TextView) findViewById(R.id.activity_event_infos_name_event);
        participateButton = (Button) findViewById(R.id.activity_event_infos_participate_button);
        nameAsso = (TextView) findViewById(R.id.activity_event_infos_name_asso);

        loadEventInfoInBackground();

        try {
            eventEndDate = Event.dateFormatter.parse(eventEnd);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Button participateButton = (Button) findViewById(R.id.activity_event_infos_participate_button);
        participateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nonParticipateEvent();
            }
        });
    }

    public void updateParticipate() {
        FirebaseDatabase.getInstance().getReference("user-events")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(eventUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            participateButton.setText(R.string.activity_event_infos_nparticipate_button);
                        } else {
                            participateButton.setText(R.string.activity_event_infos_participate_button);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("Error : ", "onCancelled", databaseError.toException());
                    }
                });
    }

    public void participateEvent() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("user-events");
        reference.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(eventUID).setValue(name);
        Toast.makeText(getApplicationContext(), getApplicationContext()
                        .getResources().getString(R.string.activity_event_infos_participate_toast) + " " + name,
                Toast.LENGTH_LONG).show();
    }

    public void nonParticipateEvent() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("user-events");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Calendar c = Calendar.getInstance();
                Date today = new Date();

                try {
                    today = Event.dateFormatter.parse(Event.dateFormatter.format(c.getTime()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (!eventEndDate.before(today)) {

                    if (snapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild(eventUID)) {
                        participateButton.setText(R.string.activity_event_infos_participate_button);
                        snapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(eventUID).getRef().getRef().removeValue();
                        Toast.makeText(getApplicationContext(), getApplicationContext()
                                .getResources().getString(R.string.activity_event_infos_nparticipate_toast)
                                + " " + name, Toast.LENGTH_LONG).show();
                    } else {
                        participateButton.setText(R.string.activity_event_infos_nparticipate_button);
                        participateEvent();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext()
                                    .getResources().getString(R.string.activity_event_infos_participate_date_passed_toast)
                            , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Error : ", "onCancelled", databaseError.toException());
            }
        });
    }

    public void loadEventInfoInBackground() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("events");
        reference.child(eventUID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    event = dataSnapshot.getValue(Event.class);
                    event.uid=eventUID;
                    nameEvent.setText(event.name);

                    nameEvent.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(getApplicationContext(), event.name, Toast.LENGTH_SHORT).show();
                        }
                    });

                    nameAsso.setText(" ");
                    DatabaseReference references = FirebaseDatabase.getInstance().getReference("association");
                    references.child(event.association).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataS) {
                            if (dataS.exists()) {
                                final Association a = dataS.getValue(Association.class);
                                nameAsso.setText(a.name);

                                nameAsso.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Toast.makeText(getApplicationContext(), a.name, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    int seats =  Integer.parseInt(String.valueOf(dataSnapshot.child("seats free").getValue()));

                    HashMap<String,Object> hashMapValueDateBegin = new HashMap<>();
                    hashMapValueDateBegin.put("title_info","Start date : ");
                    hashMapValueDateBegin.put("content_info",event.start);
                    HashMap<String,Object> hashMapValueDateEnd = new HashMap<>();
                    hashMapValueDateEnd.put("title_info","End date : ");
                    hashMapValueDateEnd.put("content_info",event.end);
                    HashMap<String,Object> hashMapValuePrice = new HashMap<>();
                    hashMapValuePrice.put("title_info","Price : ");
                    if(event.price != -1) {
                        hashMapValuePrice.put("content_info", event.price+" €");
                    } else {
                        hashMapValuePrice.put("content_info", getString(R.string.event_show_price_free));
                    }
                    HashMap<String,Object> hashMapValueSpace = new HashMap<>();
                    hashMapValueSpace.put("title_info","Available seats : ");
                    if(seats > 0) {
                        hashMapValueSpace.put("content_info", seats);
                    } else {
                        hashMapValueSpace.put("content_info", getString(R.string.event_show_seats_available_nolimit));
                    }
                    HashMap<String,Object> hashMapValueBail = new HashMap<>();
                    hashMapValueBail.put("title_info","Bail : ");
                    hashMapValueBail.put("content_info",event.bail);
                    HashMap<String,Object> hashMapValuePlace = new HashMap<>();
                    hashMapValuePlace.put("title_info","Location : ");
                    hashMapValuePlace.put("content_info",event.location);
                    HashMap<String,Object> hashMapValueDescription = new HashMap<>();
                    hashMapValueDescription.put("title_info","Description : ");
                    hashMapValueDescription.put("content_info",event.description);
                    HashMap<String,Object> hashMapValueCategorie = new HashMap<>();
                    hashMapValueCategorie.put("title_info","Type : ");
                    hashMapValueCategorie.put("content_info",event.type);

                    listValues.add(hashMapValueDateBegin);
                    listValues.add(hashMapValueDateEnd);
                    listValues.add(hashMapValuePrice);
                    listValues.add(hashMapValueSpace);
                    listValues.add(hashMapValueBail);
                    listValues.add(hashMapValuePlace);
                    listValues.add(hashMapValueDescription);
                    listValues.add(hashMapValueCategorie);

                    String[] from = new String[] {"title_info","content_info"};
                    int[] to = new int[] {R.id.content_infos_event_title_info,R.id.content_infos_event_content_info};

                    listInfos = (ListView) findViewById(R.id.activity_event_infos_list);
                    adapter = new SimpleAdapter(EventInfosActivity.this,listValues,R.layout.content_infos_event,from,to);
                    listInfos.setAdapter(adapter);

                    listInfos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                            if(position == 5) {
                                Toast.makeText(getApplicationContext(), event.location, Toast.LENGTH_LONG).show();
                            } else if (position == 6) {
                                Toast.makeText(getApplicationContext(), event.description, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }

                updateParticipate();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Error : ", "onCancelled", databaseError.toException());
            }
        });
    }
}