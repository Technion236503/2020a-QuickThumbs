package android.technion.quickthumbs.personalArea;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.technion.quickthumbs.AddTextActivity;
import android.technion.quickthumbs.MainUserActivity;
import android.technion.quickthumbs.R;
import android.technion.quickthumbs.personalArea.PersonalTexts.TextAdaptor;
import android.technion.quickthumbs.personalArea.PersonalTexts.TextDataRow;
import android.technion.quickthumbs.theme.ThemeAdaptor;
import android.technion.quickthumbs.theme.ThemeDataRow;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TextsActivity extends Fragment {
    private static final String TAG = AddTextActivity.class.getSimpleName();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private View textCard;
    private TextView loadingText;
    private RelativeLayout personalListLoadingLayout;
    private int howMuchToLoadEachScroll;
    final List<TextDataRow> textsList = new ArrayList<>();
    private DocumentSnapshot lastSnapShot =null;
    boolean noMoreLoading;
    HashMap<String,Boolean> loadedRTextsIDs=new HashMap<>();
    private GestureDetectorCompat gestureDetectorCompat;
    public static ImageButton addTextButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.activity_texts, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        howMuchToLoadEachScroll = 3;
        noMoreLoading =false;

        personalListLoadingLayout = getView().findViewById(R.id.personalListLoadingLayout);
        loadingText = getView().findViewById(R.id.personalListLoadingText);
        textCard = getView().findViewById(R.id.textCard);
        recyclerView = getView().findViewById(R.id.personalTextsRecyclerView);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        personalListLoadingLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);

        checkIfUserHasPersonalTexts();

        //gestureDetectorCompat = new GestureDetectorCompat(this, new SlideLeftToMainScreen());

        // Check if we're running on Android 5.0 or higher
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            SlidrConfig config = new SlidrConfig.Builder().position(SlidrPosition.HORIZONTAL).build();
//            Slidr.attach(this, config);        } else {
//            // Swap without transition
//        }

        setAddTextButton();
    }

    private void setAddTextButton() {
        addTextButton = getView().findViewById(R.id.addTextButton);
        addTextButton.setOnClickListener(new View.OnClickListener() {
                                             @Override
                                             public void onClick(View v) {
                                                 Intent intent = new Intent(getActivity(), AddTextActivity.class);
                                                 startActivity(intent);
                                                 //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                             }
                                         }
        );
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        this.gestureDetectorCompat.onTouchEvent(event);
//        return super.onTouchEvent(event);
//    }

//    @Override
//    public void finish() {
//        super.finish();
//        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
//    }

    private void checkIfUserHasPersonalTexts() {
        db.collection("users").document(getUid())
                .collection("texts").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult().getDocuments().size() != 0) {
                            Log.d(TAG, "collection is not empty!", task.getException());
                            fetchPersonalTextsList();
                            setRecyclerViewScroller();
                        } else {
                            Log.d(TAG, "no such collection", task.getException());
                            loadingText.setText(R.string.no_personal_texts);
                        }
                    }
                });
    }

    private String getUid() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity());
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (account != null && currentUser == null){
            return account.getId();
        }else if (currentUser!=null){
            return mAuth.getUid();
        }else{
            return accessToken.getUserId();
        }
    }

    private void setRecyclerViewScroller() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(final RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    // Scrolling up
                    Log.i("RecyclerView scrolled: ", "scroll down!");
                    if(textsList.size() != 0 && !noMoreLoading){
                        refillTextsCardsList(recyclerView);
                    }
                } else if (dy < 0){
                    // Scrolling down
                    Log.i("RecyclerView scrolled: ", "scroll up!");
                }
                else if (dx<0){
                    getActivity().finish();
                    //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            }
        });
    }

    private void refillTextsCardsList(final RecyclerView recyclerView) {
        db.collection("users").document(getUid()).
                collection("texts").orderBy("playCount", Query.Direction.DESCENDING).
                startAfter(lastSnapShot).limit(howMuchToLoadEachScroll).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document:task.getResult()) {
                                if ( document.getString("text") == null ) continue;
                                TextDataRow item = TextDataRow.createTextCardItem(document);
                                if (loadedRTextsIDs.get(document.getId()) == null ) {
                                    loadedRTextsIDs.put(document.getId(),true);
                                    textsList.add(item);
                                    recyclerView.getAdapter().notifyDataSetChanged();
                                }else{
                                    noMoreLoading =true;
                                    recyclerView.clearOnScrollListeners();
                                }
                            }
                        } else {
                            Log.d(TAG, "getAllThemes:"+  "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void fetchPersonalTextsList(){
        db.collection("users").document(getUid()).collection("texts").
                orderBy("playCount", Query.Direction.DESCENDING).limit(8).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            fillTextCardList(task);
                            setTextAdaptor();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void fillTextCardList(@NonNull Task<QuerySnapshot> task) {
        for (QueryDocumentSnapshot document : task.getResult()) {
            if ( document.getString("text") == null ) continue;
            TextDataRow item = TextDataRow.createTextCardItem(document);
            textsList.add(item);
            loadedRTextsIDs.put(document.getId(),true);
            lastSnapShot=document;
        }
    }

    private void setTextAdaptor() {
        TextAdaptor adapter = new TextAdaptor(textsList,getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        personalListLoadingLayout.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
    }

//    class SlideLeftToMainScreen extends GestureDetector.SimpleOnGestureListener {
//        //handle 'swipe right' action only
//
//        @Override
//        public boolean onFling(MotionEvent event1, MotionEvent event2,
//                               float velocityX, float velocityY) {
//            if(event2.getX() > event1.getX()){
////                Toast.makeText(getBaseContext(),"Swipe Left - finish()",Toast.LENGTH_SHORT).show();
//                finish();
//                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
//            }
//
//            return true;
//        }
//    }
}
