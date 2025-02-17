package com.mirror.oasis.home;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mirror.oasis.DetailActivity;
import com.mirror.oasis.ForUserActivity1;
import com.mirror.oasis.JoinActivity;
import com.mirror.oasis.MainActivity;
import com.mirror.oasis.R;
import com.mirror.oasis.chat.ChatData;
import com.mirror.oasis.chat.Users;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("datas");
    DatabaseReference myChatRef = database.getReference("chats");

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();

    String myKey;
    String myId;
    String myProfile;
    String myNickName;
    String myUserInfo1[];
    String myUserInfo2;
    String myUserInfo3;


    private EditText search;
    private TextView userId;

    private List<HomeData> homeDataList = new ArrayList<>();

    private RecyclerView homeRecyclerView;
    private HomeAdapter homeAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private ProgressBar progressBar;

    private TextView textView1, textView2, textView3, textView4, textView5;

    ImageButton i1;


    View v;

    @SuppressLint("WrongThread")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_home, container, false);

        myKey = MainActivity.myKey;
        myId = MainActivity.myId;
        myProfile = MainActivity.myProfile;
        myNickName = MainActivity.myNickName;
        myUserInfo1 = ForUserActivity1.data.split(",");
        myUserInfo2 = MainActivity.myUserInfo2;
        myUserInfo3 = MainActivity.myUserInfo3;

        textView1 = (TextView) v.findViewById(R.id.textView1);
        textView2 = (TextView) v.findViewById(R.id.textView2);
        textView3 = (TextView) v.findViewById(R.id.textView3);
        textView4 = (TextView) v.findViewById(R.id.textView4);
        textView5 = (TextView) v.findViewById(R.id.textView5);

        textView1.setText(myUserInfo1[0]);
        textView2.setText(myUserInfo1[1]);
        textView3.setText(myUserInfo1[2]);
        textView4.setText(myUserInfo2);
        textView5.setText(myUserInfo3);


        i1 = (ImageButton) v.findViewById(R.id.i1);
        i1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ForUserActivity1.class);
                startActivity(intent);
            }
        });

        userId = (TextView) v.findViewById(R.id.userId);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);

        userId.setText(myNickName);

        /*
        search = (EditText) v.findViewById(R.id.search);
        search.setImeOptions(EditorInfo.IME_ACTION_DONE);
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    homeAdapter.getFilter().filter(textView.getText().toString());
                    return true;
                }
                return false;
            }
        });
         */

        homeRecyclerView = (RecyclerView)v.findViewById(R.id.homeRecyclerView);
        homeRecyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        homeRecyclerView.setLayoutManager(layoutManager);

        homeAdapter = new HomeAdapter(homeDataList, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object tag = view.getTag();
                if (tag != null) {
                    int position = (int)tag;
                    Intent intent = new Intent(getActivity(), DetailActivity.class);
                    intent.putExtra("key", homeDataList.get(position).getKey());
                    startActivity(intent);
                }
            }
        });

        homeRecyclerView.setAdapter(homeAdapter);
        init();
        messageNoti();
        return v;
    }

    public void init() {
        progressBar.setVisibility(View.VISIBLE);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                homeDataList.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    HomeData homeData = snapshot1.getValue(HomeData.class);


                    if (homeData.getLocation().equals(myUserInfo1[0]) || homeData.getLocation().equals(myUserInfo1[1]) || homeData.getLocation().equals(myUserInfo1[2])) {
                        homeDataList.add(homeData);
                    };
                }
                homeAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    public void messageNoti() {

        myChatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // 1번 key 전부 가져옴
                for (DataSnapshot snapshot1: snapshot.getChildren()) {
                    //Log.d("Snapshot1", snapshot1.toString());

                    for (DataSnapshot snapshot2: snapshot1.getChildren()) {
                        // snapshot2 key: chat, info, users   value :
                        String key = "";
                        String user = "";
                        String lastMessage = "";
                        String uri = "";
                        String title = "";

                        // users ==> myId
                        if (snapshot2.getKey().equals("users")) {
                            Users users = snapshot2.getValue(Users.class);
                            if (users.getUser1().equals(MainActivity.myNickName) || users.getUser2().equals(MainActivity.myNickName)) {
                                key = snapshot1.getKey();

                                if (snapshot1.child("chat").getChildrenCount() > 0) {
                                    for (DataSnapshot snapshot3: snapshot1.child("chat").getChildren()) {
                                        ChatData chatdata = snapshot3.getValue(ChatData.class);

                                        if (!(chatdata.getUser().equals(MainActivity.myNickName))) {
                                            showNoti(chatdata.getUser(), chatdata.getMessage());
                                        }

                                        break;
                                    }
                                }
                            }

                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    public void showNoti(String title, String message){
        try {
            NotificationManager notificationManager=(NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder= null;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

                String channelID="channel_01";
                String channelName="MyChannel01";

                NotificationChannel channel= new NotificationChannel(channelID,channelName,NotificationManager.IMPORTANCE_DEFAULT);

                notificationManager.createNotificationChannel(channel);

                builder=new NotificationCompat.Builder(getActivity(), channelID);
            }

            builder.setSmallIcon(android.R.drawable.ic_menu_view);

            builder.setContentTitle(title);
            builder.setContentText(message);
            Bitmap bm= BitmapFactory.decodeResource(getResources(),R.drawable.logo);
            builder.setLargeIcon(bm);

            Notification notification=builder.build();
            notificationManager.notify(1, notification);
        } catch (Exception e) {

        }

    }

}

