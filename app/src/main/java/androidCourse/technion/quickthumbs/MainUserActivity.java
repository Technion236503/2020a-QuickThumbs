package androidCourse.technion.quickthumbs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidCourse.technion.quickthumbs.Utils.AppOpeningSplashScreen;
import androidCourse.technion.quickthumbs.Utils.CacheHandler;
import androidCourse.technion.quickthumbs.Utils.CircleMenuView;
import androidCourse.technion.quickthumbs.database.FriendsDatabaseHandler;
import androidCourse.technion.quickthumbs.database.GameDatabaseInviteHandler;
import androidCourse.technion.quickthumbs.multiplayerSearch.GameRoom;
import androidCourse.technion.quickthumbs.multiplayerSearch.SearchingGrouper;
import androidCourse.technion.quickthumbs.personalArea.PersonalTexts.TextDataRow;
import androidCourse.technion.quickthumbs.theme.ThemeSelectPopUp;

import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.facebook.AccessToken;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.skyfishjy.library.RippleBackground;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidCourse.technion.quickthumbs.Utils.CacheHandler.checkIfTextListNeedToBeRefilled;
import static androidCourse.technion.quickthumbs.Utils.CacheHandler.getNextTextFromSelectedTheme;


public class MainUserActivity extends Fragment {
    private static final String TAG = MainUserActivity.class.getSimpleName();
    private static final String gameRooms = "GameRooms";
    private static final String searchingRoomsStr = "searchingRooms";
    private static final String level1 = "level1";
    private FirebaseAuth fireBaseAuth;
    private static FirebaseFirestore db;
    public static FirebaseDatabase instance;
    public static String localUserName;

    public static Button gameBtn;
    public static Button startMultiGameButton;
    DatabaseReference mDatabase;
    DatabaseReference searchingRooms;
    DatabaseReference searchingRoomsLevel1;
    public static DatabaseReference gameRoomsReference;
    public static ValueEventListener valueEventListener;

    ImageButton closeButton;
    CircleMenuView menu;
    ImageView waitingLogo;
    private static View fragmentViewForButton;

    private TextView amountOfPlayerView;
    private TextView searchTimerView;
    private RelativeLayout searchScreen;
    Timer searchTimer;
    private Handler mHandler;
    long startTime;
    RippleBackground rippleBackground;
    private CircleMenuView.EventListener listener;
    private String acceptedInvitationRoomKey = "-1";
    public static String friendUid = null;
    private String[] basicThemes = {"Comedy", "Music", "Movies", "Science", "Games", "Literature"};
    private static Map<String, Boolean> allUserThemes = new HashMap<>();
    private static TextDataRow textCardItem = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main_user, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view,
                              Bundle savedInstanceState) {
        final View fragmentView = view;
        fragmentViewForButton = view;
        fireBaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (instance == null) {
            instance = FirebaseDatabase.getInstance();
        }

        mDatabase = instance.getReference().child("searchAndGame");
        searchingRooms = mDatabase.child(searchingRoomsStr);
        searchingRoomsLevel1 = searchingRooms.child(level1);
        gameRoomsReference = mDatabase.child(gameRooms);

        setUserName();

        setCircleMenu();

        setCloseMultiplayerSerchButtonListener();

        closeKeyboard();

        setOpeningSplashScreen();

        amountOfPlayerView = view.findViewById(R.id.amountOfPlayers);
        searchTimerView = view.findViewById(R.id.searchTimer);
        searchScreen = view.findViewById(R.id.searchScreen);

        Intent intent = getActivity().getIntent();
        if (intent.hasExtra("roomKey")) {
            acceptedInvitationRoomKey = intent.getStringExtra("roomKey");
            StartSpecifiedGame(String.valueOf(acceptedInvitationRoomKey));
        }
        if (friendUid != null) {
            Myparam myparam = new Myparam(friendUid);
            new MainUserActivity.FetchRandomTextForFriendsRoom().execute(myparam);
        }

    }

    private void setCircleMenu() {
        menu = getActivity().findViewById(R.id.circle_menu);
        waitingLogo = getActivity().findViewById(R.id.waitingLogo);
        listener = new CircleMenuView.EventListener() {
            @Override
            public void onMenuOpenAnimationStart(@NonNull CircleMenuView view) {
                Log.d("D", "onMenuOpenAnimationStart");
            }

            @Override
            public void onMenuOpenAnimationEnd(@NonNull CircleMenuView view) {
                Log.d("D", "onMenuOpenAnimationEnd");
            }

            @Override
            public void onMenuCloseAnimationStart(@NonNull CircleMenuView view) {
                Log.d("D", "onMenuCloseAnimationStart");
            }

            @Override
            public void onMenuCloseAnimationEnd(@NonNull CircleMenuView view) {
                Log.d("D", "onMenuCloseAnimationEnd");
            }

            @Override
            public void onButtonClickAnimationStart(@NonNull CircleMenuView view, int index) {
                Log.d("D", "onButtonClickAnimationStart| index: " + index);

            }

            @Override
            public void onButtonClickAnimationEnd(@NonNull CircleMenuView view, int index) {
                Log.d("D", "onButtonClickAnimationEnd| index: " + index);
                switch (index) {
                    case 0:
                        ThemeSelectPopUp popUpWindow = new ThemeSelectPopUp();
                        popUpWindow.showPopupWindow(fragmentViewForButton, fragmentViewForButton.findViewById(R.id.RelativeLayout1));

                        break;


                    case 1:
                        menu.setEventListener(null);
                        waitingLogo.setVisibility(VISIBLE);
                        menu.setVisibility(INVISIBLE);
                        searchScreen.setVisibility(VISIBLE);
                        closeButton.setVisibility(VISIBLE);


                        startRippleBackground();

                        setSearchTimer();

                        startSearchForGame();

                        break;

                    case 2:
                        menu.setEventListener(null);
                        waitingLogo.setVisibility(VISIBLE);
                        menu.setVisibility(INVISIBLE);
                        searchScreen.setVisibility(VISIBLE);
                        closeButton.setVisibility(VISIBLE);
                        startRippleBackground();

                        setSearchTimer();

                        if (acceptedInvitationRoomKey != "-1") {
                            StartSpecifiedGame(String.valueOf(acceptedInvitationRoomKey));
                        } else {
                            //TODO: implement pop up friends list
                            if (friendUid != null) {
                                CacheHandler cacheHandler = new CacheHandler(getContext());
                                allUserThemes = cacheHandler.loadThemesFromSharedPreferences();
                                final String choosenTheme = getRandomThemeName();

                                TextDataRow textCardItem = getNextTextFromSelectedTheme(choosenTheme);
                                Myparam myparam = new Myparam(friendUid);
                                myparam.setTextId(textCardItem.getTextId());
                                createSpecialRoom(myparam);
//                                new MainUserActivity.FetchRandomTextForFriendsRoom().execute(myparam);
                            }
                        }
                        break;
                }
            }
        };
        menu.setEventListener(listener);

    }

    private void setCloseMultiplayerSerchButtonListener() {
        closeButton = getActivity().findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUIBackToNormal();
            }
        });
    }

    private void startRippleBackground() {
        rippleBackground = (RippleBackground) getActivity().findViewById(R.id.content);
        rippleBackground.startRippleAnimation();
    }

    private void setSearchTimer() {
        startTime = System.currentTimeMillis();
        mHandler = new Handler();
        searchTimer = new Timer();
        searchTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int spentTimeInSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);

                        int hours = spentTimeInSeconds / 3600;
                        int minutes = (spentTimeInSeconds % 3600) / 60;
                        int seconds = spentTimeInSeconds % 60;

                        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

                        searchTimerView.setText(timeString);
                    }
                });
            }
        }, 0, 1000);
    }

    public DocumentReference getUserDocumentReference() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(fragmentViewForButton.getContext());
        String users = "users";

        if (fireBaseAuth.getCurrentUser() != null) {
            return db.collection(users)
                    .document(fireBaseAuth.getUid());
        } else if (googleAccount != null) {
            return db.collection(users)
                    .document(googleAccount.getId());
        } else {
            return db.collection(users)
                    .document(accessToken.getUserId());
        }
    }

    public void setUserName() {
        getUserDocumentReference().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Map<String, Object> data = task.getResult().getData();
                    String name = (String) data.get("name");

                    localUserName = name;
                    Log.d(TAG, String.format("fetched user name -> %s", name));
                }
            }
        });
    }

    private void startSearchForGame() {
        Log.d(TAG, "Starting to search for multi player game");
        amountOfPlayerView.setText("Searching Room ...");

        int magicNumber = 10;
        Query potentialRooms = searchingRoomsLevel1.orderByKey().limitToLast(magicNumber);

        potentialRooms.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long childrenCount = dataSnapshot.getChildrenCount();

                if (childrenCount == 0) {
                    Log.d(TAG, "Found out that there are no rooms exist");

                    new FetchRandomTextId().execute();

                    return;
                }

                int chosenIndex = new Random().nextInt((int) childrenCount);
                int i = 0;

                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if (chosenIndex == i) {
                        final String roomKey = data.getKey();

                        Log.d(TAG, String.format("Trying to catch room with key -> %s", roomKey));

                        mDatabase.runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                if (mutableData.getValue() == null) {
                                    return Transaction.success(mutableData);
                                }

                                MutableData searchingRoomsLevel1 = mutableData.child(searchingRoomsStr).child(level1);
                                final SearchingGrouper previousGrouper = searchingRoomsLevel1.child(roomKey).getValue(SearchingGrouper.class);

                                if (previousGrouper.targetRoomSize - 1 == previousGrouper.currentSize) {
                                    searchingRoomsLevel1.child(roomKey).setValue(null); //removing from level 1.

                                    GameRoom gameRoom = mutableData.child(gameRooms).child(roomKey).getValue(GameRoom.class);

                                    GameRoom gameRoomWithChange = new GameRoom(gameRoom.user1, gameRoom.location1, localUserName, 0, gameRoom.textId, true, false, false, -1, -1);
                                    mutableData.child(gameRooms).child(roomKey).setValue(gameRoomWithChange);   //adding as an active multi-player game.


                                } else {
                                    throw new RuntimeException("doesn't support more than 2 rooms");
                                }

                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                                if (b) {  //was committed
                                    Log.d(TAG, "Successfully removed searching room and added to game room");
                                    setUIBackToNormal();

                                    GameRoom gameRoom = dataSnapshot.child(gameRooms).child(roomKey).getValue(GameRoom.class);
                                    String textId = gameRoom.textId;

                                    int indexForCurrentUser = 2;
                                    int targetAmount = 2;
                                    amountOfPlayerView.setText(String.format("%s out of %s in room ...", indexForCurrentUser, targetAmount));

                                    Context context = getActivity().getApplicationContext();
                                    Intent i = new Intent(context, GameLoadingSplashScreenActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.putExtra("id", textId);
                                    i.putExtra("roomKey", roomKey);
                                    i.putExtra("indexInRoom", indexForCurrentUser);
                                    i.putExtra("startingTimeStamp", System.currentTimeMillis());
                                    context.startActivity(i);
                                } else {
                                    if (dataSnapshot.getValue() != null) {
                                        GameRoom gameRoom = dataSnapshot.child(gameRooms).child(roomKey).getValue(GameRoom.class);
                                        if (gameRoom != null) {
                                            Log.d(TAG, "Failed to remove searching room and get added to game room, BUT room is still available for future catching");
                                        }

                                        Log.d(TAG, "Starting room creation process");
                                        new FetchRandomTextId().execute();
                                    }
                                }
                            }
                        });

                        break;
                    }

                    i++;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    private void setStartMultiplayerGameWhenFlagActivated(final String roomKey, final String textId, final int indexInRoom) {
        Log.d(TAG, "adding listener for game flag");
        valueEventListener = gameRoomsReference.child(roomKey).addValueEventListener(new ValueEventListener() {
                                                                                         @Override
                                                                                         public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                             if (!dataSnapshot.exists() ||
                                                                                                     !dataSnapshot.getValue(GameRoom.class).started) {
                                                                                                 return;
                                                                                             }

                                                                                             Log.d(TAG, "Flag was turned in game room so user should move to game");

                                                                                             int currentAmount = 2;
                                                                                             int targetAmount = 2;
                                                                                             amountOfPlayerView.setText(String.format("%s out of %s in room ...", currentAmount, targetAmount));

                                                                                             setUIBackToNormal();

                                                                                             //game starts here;
                                                                                             Context context = fragmentViewForButton.getContext();
                                                                                             Intent i = new Intent(context, GameLoadingSplashScreenActivity.class);
                                                                                             i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                                                                             i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                                             i.putExtra("id", textId);
                                                                                             i.putExtra("roomKey", roomKey);
                                                                                             i.putExtra("indexInRoom", indexInRoom);
                                                                                             i.putExtra("startingTimeStamp", System.currentTimeMillis());
                                                                                             context.startActivity(i);

                                                                                             gameRoomsReference.child(roomKey).removeEventListener(this);
                                                                                         }

                                                                                         @Override
                                                                                         public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                                             setUIBackToNormal();
                                                                                         }
                                                                                     }

        );
    }
    ///here the shit i added - aka mori

    private void StartSpecifiedGame(final String roomKey) {
        Log.d(TAG, "Going to the right room");
        amountOfPlayerView.setText("Searching Room ...");

        gameRoomsReference.child(roomKey).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                if (mutableData.getValue() == null) {
                    return Transaction.success(mutableData);
                }

                GameRoom gameRoom = mutableData.getValue(GameRoom.class);

                GameRoom gameRoomWithUserAdded = new GameRoom(gameRoom.user1, gameRoom.location1, getUid(), 0, gameRoom.textId, true, false, false, -1, -1);

                mutableData.setValue(gameRoomWithUserAdded);

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                if (b) {  //was committed
                    Log.d(TAG, "Successfully added to game room");
                    setUIBackToNormal();

                    rippleBackground.stopRippleAnimation();

                    GameRoom gameRoom = dataSnapshot.getValue(GameRoom.class);
                    String textId = gameRoom.textId;

                    int indexForCurrentUser = 2;
                    int targetAmount = 2;
                    amountOfPlayerView.setText(String.format("%s out of %s in room ...", indexForCurrentUser, targetAmount));

                    Context context = getActivity().getApplicationContext();
                    Intent i = new Intent(context, GameLoadingSplashScreenActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra("id", textId);
                    i.putExtra("roomKey", roomKey);
                    i.putExtra("indexInRoom", indexForCurrentUser);
                    i.putExtra("startingTimeStamp", System.currentTimeMillis());
                    context.startActivity(i);
                } else {
                    Log.d(TAG, "Friend enter game should not fail!!, might be connectivity issues or any other unexpected problem");
                }
            }
        });

    }

    public void createSpecialRoom(Myparam myparam) {
        final String friendUid = myparam.friendId;
        final String textId = myparam.textId;
        Log.d(TAG, "Starting to create separate room");
        final String key = searchingRoomsLevel1.push().getKey();


        GameRoom newRoom = new GameRoom(fireBaseAuth.getUid(), 0, null, 0, textId, false, false, false, -1, -1);

        gameRoomsReference.child(key).setValue(newRoom)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            int currentAmount = 1;
                            int targetAmount = 2;
                            amountOfPlayerView.setText(String.format("%s out of %s in room ...", currentAmount, targetAmount));

                            setGameForFriends(key, textId, 1);

                            GameDatabaseInviteHandler gameDatabaseInviteHandler = new GameDatabaseInviteHandler();
                            gameDatabaseInviteHandler.inviteFriendToAGame(friendUid, key, getActivity());
                        } else {
                            throw new RuntimeException("bug I guess");
                        }
                    }
                });
    }

    public void setGameForFriends(final String roomKey, final String textId, final int indexInRoom) {
        Log.d(TAG, "adding listener for game flag");
        gameRoomsReference.child(roomKey).addValueEventListener(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                        if (!dataSnapshot.exists() ||
                                                                                !dataSnapshot.getValue(GameRoom.class).started) {
                                                                            return;
                                                                        }

                                                                        Log.d(TAG, "Flag was turned in game room so user should move to game");

                                                                        int currentAmount = 2;
                                                                        int targetAmount = 2;
                                                                        amountOfPlayerView.setText(String.format("%s out of %s in room ...", currentAmount, targetAmount));

                                                                        setUIBackToNormal();

                                                                        //game starts here;
                                                                        Context context = fragmentViewForButton.getContext();
                                                                        Intent i = new Intent(context, GameLoadingSplashScreenActivity.class);
                                                                        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                                                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                        i.putExtra("id", textId);
                                                                        i.putExtra("roomKey", roomKey);
                                                                        i.putExtra("indexInRoom", indexInRoom);
                                                                        i.putExtra("startingTimeStamp", System.currentTimeMillis());
                                                                        context.startActivity(i);

                                                                        gameRoomsReference.child(roomKey).removeEventListener(this);
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                        setUIBackToNormal();
                                                                    }
                                                                }

        );
    }

    public static class Myparam {
        private String friendId;
        private String textId;

        public Myparam(String friendId) {
            this.friendId = friendId;
        }

        public void setTextId(String textId) {
            this.textId = textId;
        }
    }

    public class FetchRandomTextForFriendsRoom extends AsyncTask<Myparam, Void, Void> {


        @Override
        protected Void doInBackground(Myparam... myparams) {
            CacheHandler cacheHandler = new CacheHandler(getContext());
            allUserThemes = cacheHandler.loadThemesFromSharedPreferences();
            final String choosenTheme = getRandomThemeName();

            TextDataRow textCardItem = getNextTextFromSelectedTheme(choosenTheme);
            if (textCardItem == null) {
                fetchRandomTextSpecifiedForUsers();
            } else {
                String textId = textCardItem.getTextId();
                myparams[0].setTextId(textId);
                try {
                    Class<?> c = MainUserActivity.class;
                    Method method = c.getDeclaredMethod("createSpecialRoom", String.class);
                    method.invoke(MainUserActivity.this, myparams[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                checkIfTextListNeedToBeRefilled(choosenTheme);
            }
            return null;
        }
    }


    ///end my stuff added - mori

    private void setUIBackToNormal() {
        menu.setEventListener(listener);
        menu.setVisibility(VISIBLE);
        waitingLogo.setVisibility(INVISIBLE);
        searchScreen.setVisibility(INVISIBLE);
        closeButton.setVisibility(INVISIBLE);
        rippleBackground.stopRippleAnimation();
        searchTimer.cancel();
    }

    public void createSeparateRoom(final String textId) {
        Log.d(TAG, "Starting to create separate room");
        final String key = searchingRoomsLevel1.push().getKey();

        GameRoom newRoom = new GameRoom(localUserName, 0, null, 0, textId, false, false, false, -1, -1);
        final SearchingGrouper newSearchingGrouper = new SearchingGrouper(textId, 2, 1);

        gameRoomsReference.child(key).setValue(newRoom)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            searchingRoomsLevel1.child(key).setValue(newSearchingGrouper);
                            int currentAmount = 1;
                            int targetAmount = 2;
                            amountOfPlayerView.setText(String.format("%s out of %s in room ...", currentAmount, targetAmount));

                            setStartMultiplayerGameWhenFlagActivated(key, textId, 1);
                        } else {
                            throw new RuntimeException("bug I guess");
                        }
                    }
                });
    }


    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser currentUser = fireBaseAuth.getCurrentUser();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity());
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (checkIfUserLoggedIn(currentUser, account, isLoggedIn)) return;
    }


    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = fireBaseAuth.getCurrentUser();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity());
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (checkIfUserLoggedIn(currentUser, account, isLoggedIn)) return;
        //the part where i insert the user to the db just ot make sure he's there in case no user has been made
    }

    private void setOpeningSplashScreen() {
        AppOpeningSplashScreen.Builder splash = new AppOpeningSplashScreen.Builder(getActivity());
        //        Set custom color of background:
        splash.setBackgroundColor(getResources().getColor(R.color.primaryColor));
        //Set custom image for background:
//        splash.setBackgroundImage(getResources().getDrawable(R.mipmap.ic_launcher_foreground));
        //Set custom image for splash:
        splash.setSplashImage(getResources().getDrawable(R.drawable.ic_launcher_foreground));
        //Set custom color of splash image:
        splash.setSplashImageColor(getResources().getColor(R.color.primaryDarkColor));
        splash.create();
//        splash.setOneShotStart(false);
        splash.perform();
    }

    private boolean checkIfUserLoggedIn(FirebaseUser currentUser, GoogleSignInAccount account, boolean isLoggedIn) {
        if (currentUser != null || account != null && !account.isExpired() || isLoggedIn) {
            return false;
        }
        Intent i = new Intent(getActivity(), MainActivity.class);
        getActivity().finish();
        startActivity(i);
        return true;
    }


    private void closeKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private class FetchRandomTextId extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... voids) {
            CacheHandler cacheHandler = new CacheHandler(getContext());
            allUserThemes = cacheHandler.loadThemesFromSharedPreferences();
            final String choosenTheme = getRandomThemeName();

            TextDataRow textCardItem = getNextTextFromSelectedTheme(choosenTheme);
            if (textCardItem == null) {
                fetchRandomTextSpecifiedForUsers();
            } else {
                String textId = textCardItem.getTextId();
                try {
                    Class<?> c = MainUserActivity.class;
                    Method method = c.getDeclaredMethod("createSeparateRoom", String.class);
                    method.invoke(MainUserActivity.this, textId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                checkIfTextListNeedToBeRefilled(choosenTheme);
            }

            return null;
        }
    }

    private static void fetchRandomTextSpecifiedForUsers() {
        final String choosenTheme = getRandomThemeName();
        //now reach for the theme texts and check the number of texts in the theme
        getThemesCollection().document(choosenTheme).
                get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "getRandomTheme:" + "DocumentSnapshot data: " + document.getData());
                        int textsAmount = document.getLong("textsCount").intValue();
                        getRandomText(choosenTheme, textsAmount);
                    } else {
                        Log.d(TAG, "getRandomTheme:" + "No such document");
                        //TODO: is it possible that we will reach here?
                    }
                } else {
                    Log.d(TAG, "getRandomTheme:" + "get failed with ", task.getException());
                    //TODO: is it possible that we will reach here?
                }
            }
        });
    }


    private static String getRandomThemeName() {
        List<String> userChosenThemes = new LinkedList<>();
        for (String theme : allUserThemes.keySet()) {
            if (allUserThemes.get(theme)) {
                userChosenThemes.add(theme);
            }
        }
        // if the user has no themes selected we will choose all for him
        if (userChosenThemes.isEmpty()) {
            for (String theme : allUserThemes.keySet()) {
                userChosenThemes.add(theme);
            }
        }
        //choose random theme from the user themes
        int themesListSize = userChosenThemes.size();
        return userChosenThemes.get(new Random().nextInt(themesListSize));
    }

    private static void getRandomText(final String choosenTheme, int textsAmount) {
        final int chosenIndex = (new Random().nextInt(textsAmount)) + 1;
        //now reach for the theme texts and check the number of texts in there
        getSelectedThemeTextsCollection(choosenTheme).whereEqualTo("mainThemeID", chosenIndex).
                get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().isEmpty()) {
                        fetchRandomTextSpecifiedForUsers();
                    } else {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            textCardItem = TextDataRow.createTextCardItem(document, null, -1, null);
                            String textId = document.getId();
                            try {
                                Class<?> c = MainUserActivity.class;
                                Method method = c.getDeclaredMethod("createSeparateRoom", String.class);
                                method.invoke(c, textId);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    }
                } else {
                    Log.d(TAG, "getRandomText: " + "get failed with ", task.getException());
                }
            }
        });
    }

    private DocumentReference getTextFromTextsCollection(String documentID) {
        return db.collection("texts").document(documentID);
    }

    private CollectionReference getUserCollection(String userID, String collecionName) {
        return getUserDocument(userID).collection(collecionName);
    }

    private DocumentReference getUserDocument(String composer) {
        return db.collection("users").document(composer);
    }

    private static CollectionReference getSelectedThemeTextsCollection(String theme) {
        return getThemesCollection().document(theme).collection("texts");
    }

    private static CollectionReference getThemesCollection() {
        return db.collection("themes");
    }

    private String getUid() {
        FirebaseUser currentUser = fireBaseAuth.getCurrentUser();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (account != null && currentUser == null) {
            return account.getId();
        } else if (currentUser != null) {
            return fireBaseAuth.getUid();
        } else {
            return accessToken.getUserId();
        }
    }


}
