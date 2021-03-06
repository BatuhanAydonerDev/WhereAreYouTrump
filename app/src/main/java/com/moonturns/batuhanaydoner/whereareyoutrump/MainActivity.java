package com.moonturns.batuhanaydoner.whereareyoutrump;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.moonturns.batuhanaydoner.whereareyoutrump.FirebaseDatabase.User;
import com.moonturns.batuhanaydoner.whereareyoutrump.FirebaseDatabase.ValueEventGameTicket;
import com.moonturns.batuhanaydoner.whereareyoutrump.GameFragements.GameDescriptionFragment;
import com.moonturns.batuhanaydoner.whereareyoutrump.GameFragements.TicketFragment;
import com.moonturns.batuhanaydoner.whereareyoutrump.UserActivities.LoginActivity;
import com.moonturns.batuhanaydoner.whereareyoutrump.database.ScoreDatabase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements Animation.AnimationListener {

    //You could hack me and what will you do?

    //moonturns

    //Where Are You Trump-WAYT

    private final String stateWeek = "play";
    private ValueEventGameTicket gameTicket;
    private FirebaseUser kullanici;

    private ImageView imageFind, imageSound, imageInfo, imageLogInOut, imageUser, imageFirstPlayer;
    private TextView txtPlaying, txtUpdating;
    public static Switch switchOnline;

    private ProgressBar progressBarHome;

    private Animation animationStartGame;
    private Animation animationFirstScreen;

    private MediaPlayer mediaPlayer;

    private int currentSound = 0;

    private SharedPreferences soundPreferences;
    private SharedPreferences.Editor editor;

    private AdView bannerAdvertisement;

    private boolean onlineGame = false; //oyunun online mı yoksa offline mı oynanacağına göre değer döner

    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private boolean activeUser = false; //kullanıcının hesabı açıksa true döner

    private String userTicket = "";
    private String currentDay = "";
    private int high_score = 0; //kullanıcının en yüksek skoru
    private int total_playing = 0; //kullanıcının online toplam oynama sayısı
    private String user_week_number = "";

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void crt() {

        imageFind = (ImageView) this.findViewById(R.id.imageFind);
        imageSound = (ImageView) this.findViewById(R.id.imageSound);
        txtPlaying = (TextView) this.findViewById(R.id.txtPlaying);
        txtUpdating = (TextView) this.findViewById(R.id.txtUpdating);
        switchOnline = (Switch) this.findViewById(R.id.switchOnline);
        imageInfo = (ImageView) this.findViewById(R.id.imageInfo);
        imageLogInOut = (ImageView) this.findViewById(R.id.imageLogInOut);
        imageUser = (ImageView) this.findViewById(R.id.imageUser);
        imageFirstPlayer = (ImageView) this.findViewById(R.id.imageFirstPlayer);

        progressBarHome = (ProgressBar) this.findViewById(R.id.progressBarHome);

        bannerAdvertisement = (AdView) this.findViewById(R.id.bannerAdvertisement);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        crt();

        showAdventisement();

        initAuthStateListener();

        initPreferences();

        stateUpdating();

        setImageFind();
        totalPlaying();

        kyuAnimationNormal();

        gameMusic();

        setImageSound();

        openInfoFragment();
        playOnline();
        showFragment();

        stateUser();

        setImageUser();

        leaderPlayer();

    }

    @Override
    protected void onResume() {
        super.onResume();
        continueMusic();


    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseAuth.getInstance().addAuthStateListener(mAuthStateListener);

    }

    @Override
    protected void onPause() {
        super.onPause();

        stateGameMusic();

    }

    @Override
    protected void onStop() {
        super.onStop();

        FirebaseAuth.getInstance().removeAuthStateListener(mAuthStateListener);

    }

    //mAuthStateListener
    private void initAuthStateListener() {

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                kullanici = firebaseAuth.getCurrentUser();

                if (kullanici != null) {

                    getDatabaseUserValues();

                    imageLogInOut.setImageResource(R.drawable.login_out);
                    imageLogInOut.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    imageUser.setImageTintList(ColorStateList.valueOf(Color.WHITE));

                    activeUser = true;

                } else {

                    imageLogInOut.setImageResource(R.drawable.login_in);
                    imageLogInOut.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    imageUser.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    activeUser = false;

                }

            }
        };

    }

    //güncelleme durumu
    private void stateUpdating() {

        String updating = getIntent().getStringExtra("updating");
        txtUpdating.setText(updating);

    }

    private void openInfoFragment() {

        imageInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openFragment();

            }
        });

    }

    //fragment açar
    private void openFragment() {

        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
        GameDescriptionFragment gdf = new GameDescriptionFragment();
        gdf.show(fragmentManager, "dialog");

    }

    //switchOnline listener açılırsa oyunu online yapar
    private void playOnline() {

        switchOnline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (kullanici != null) {

                    if (gameTicket.getWeek().equals("")) {

                        compoundButton.setChecked(b);
                        Toast.makeText(MainActivity.this, R.string.internet, Toast.LENGTH_LONG).show();
                        switchOnline.setChecked(false);

                    } else if (gameTicket.getWeek().equals(stateWeek)) {

                        if (b) {

                            onlineGame = true;
                            switchOnline.setText(R.string.online);

                        } else {

                            onlineGame = false;
                            switchOnline.setText(R.string.offline);

                        }

                    } else {

                        switchOnline.setChecked(false);
                        gameTicket.openWeekFragment();

                    }

                } else {

                    openLoginActivity();
                    compoundButton.setChecked(false);

                }
            }
        });

    }

    //oyun ekranına geçmek için button listener
    private void setImageFind() {

        imageFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                stateOnline();
                changeFirstGame();

            }
        });

    }

    //çevrimiçi durumuna göre oyun online olur ve gerekli activity açılır
    private void stateOnline() {

        if (onlineGame) {

            if (activeUser) {

                stateGameTicket();

            } else {

                openLoginActivity();

            }

        } else {

            kyuAnimationStartGame();

        }

    }

    //müziği açıp kapamak için kullanılır
    private void setImageSound() {

        imageSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                playingMusic();

            }
        });

    }

    //veritabanından toplam oynama sayısını alır
    private void totalPlaying() {

        if (!onlineGame) {

            Realm realm = Realm.getDefaultInstance();

            RealmResults<ScoreDatabase> realmResults = realm.where(ScoreDatabase.class).findAll();
            if (realmResults.size() > 0) {

                int playingCount = realmResults.get(0).getPlaying();

                txtPlaying.setVisibility(View.VISIBLE);
                txtPlaying.setText(getResources().getString(R.string.total_playing) + playingCount);

            }

        }

    }

    //FieldOfGameActivity intent
    private void oyunEkrani() {

        Intent intent = new Intent(MainActivity.this, FieldOfGameActivity.class);
        intent.putExtra("onlineGame", onlineGame);
        if (onlineGame) {

            intent.putExtra("highScore", high_score);

        }
        startActivity(intent);
        finish();

    }

    //resim için bir giriş animasyonu
    private void kyuAnimationNormal() {

        animationFirstScreen = AnimationUtils.loadAnimation(this, R.anim.animation_normal_kyu);
        animationFirstScreen.setAnimationListener(this);
        imageFind.startAnimation(animationFirstScreen);

    }

    //oyuna ekranı gelmeden önceki animasyon
    private void kyuAnimationStartGame() {

        animationStartGame = AnimationUtils.loadAnimation(this, R.anim.animation_start_game);
        animationStartGame.setAnimationListener(this);
        imageFind.startAnimation(animationStartGame);

    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

        if (animation == animationStartGame) {

            imageFind.setVisibility(View.INVISIBLE);
            oyunEkrani();

        }

    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    //oyun müziği
    private void gameMusic() {

        if (getStateSound()) {

            mediaPlayer = MediaPlayer.create(this, R.raw.game_first_sound);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();

        } else {

            imageSound.setImageResource(R.drawable.no_sound);

        }


    }

    //çalan müziği kapatır ve kaldığı süreyi alır
    private void stateGameMusic() {

        if (getStateSound()) {

            if (mediaPlayer.isPlaying()) {

                mediaPlayer.pause();
                currentSound = mediaPlayer.getCurrentPosition();

            }

        }

    }

    //oyun duraklatılıp tekrar açıldığında müzik kaldığı yerden devam eder
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void continueMusic() {

        if (currentSound != 0 && getStateSound()) {

            mediaPlayer.selectTrack(currentSound);
            mediaPlayer.start();

        }

    }

    //müziğin çalıp çalmaması için preferences
    private void initPreferences() {

        soundPreferences = this.getSharedPreferences("soundPreferences", Context.MODE_PRIVATE);
        editor = soundPreferences.edit();

    }

    //müziğin çalıp çalmaması için bilgiyi alır
    private boolean getStateSound() {

        return soundPreferences.getBoolean("soundOpenClose", true);

    }

    //değişikliği kaydeder
    private void setStateSound(boolean state) {

        editor.putBoolean("soundOpenClose", state);
        editor.commit();

    }

    //müziği amıp kapatır
    private void playingMusic() {

        if (getStateSound()) {

            stateGameMusic();
            setStateSound(false);
            imageSound.setImageResource(R.drawable.no_sound);

        } else {

            if (currentSound == 0 && !getStateSound()) {

                setStateSound(true);
                gameMusic();

            }
            setStateSound(true);
            continueMusic();
            imageSound.setImageResource(R.drawable.sound);

        }

    }

    //oyuna ilk defa girildiğinde firstGame true döner sonra false döner
    private void changeFirstGame() {

        editor.putBoolean("firstGame", false);
        editor.commit();

    }

    private boolean getFirstGame() {

        return soundPreferences.getBoolean("firstGame", true);

    }

    //oyuna ilk defa girildiyse fragment gösterilir
    private void showFragment() {

        if (getFirstGame()) {

            openFragment();

        }

    }

    //kullanıcının durumuna göre imageLogInOut çalışır listener
    private void stateUser() {

        imageLogInOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                userLogState();

            }
        });

    }

    //kullanıcının varlığına göre işlem yapar
    private void userLogState() {

        FirebaseUser kullanici = FirebaseAuth.getInstance().getCurrentUser();

        if (kullanici != null) {

            showDialog();

        } else {

            openLoginActivity();

        }

    }

    //kullanıcı hesabından çıkmak istediğinde bir dialog gösterilir
    private void showDialog() {

        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(false);
        dialog.setMessage(R.string.exitGame);
        dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();

            }
        });
        dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                FirebaseAuth.getInstance().signOut();

            }
        }).show();

    }

    //LoginActivity açılır intent
    private void openLoginActivity() {

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);

    }

    //imageUser listener
    private void setImageUser() {

        imageUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openSettingsActivity();
            }
        });

    }

    //SettingsActivity açılır intent
    private void openSettingsActivity() {

        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("activeUser", activeUser);
        startActivity(intent);
    }

    //imageFirstPlayer listener
    private void leaderPlayer() {

        imageFirstPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openFirstPlayerActivity();

            }
        });

    }

    //FirstPlayerActivity açılır intent
    private void openFirstPlayerActivity() {

        Intent intent = new Intent(this, FirstPlayerActivity.class);
        startActivity(intent);

    }

    //reklam gösterir
    private void showAdventisement() {

        // Sample AdMob app ID: ca-app-pub-3940256099942544~3347511713
        MobileAds.initialize(this, "ca-app-pub-5539936226294378~5338627173");
        AdRequest request = new AdRequest.Builder().build();
        bannerAdvertisement.loadAd(request);

    }

    //veritabanından kullanıcının bilgileri alınır
    private void getDatabaseUserValues() {

        progressBarHome.setVisibility(View.VISIBLE);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query sorgu = reference.child("user").orderByKey().equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

        sorgu.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                    User okunanKullanici = singleSnapshot.getValue(User.class);
                    userTicket = okunanKullanici.getGame_ticket();
                    high_score = Integer.valueOf(okunanKullanici.getHigh_score());
                    total_playing = Integer.valueOf(okunanKullanici.getTotal_playing());
                    currentDay = okunanKullanici.getDay();
                    user_week_number = okunanKullanici.getWeek_number();

                }

                valueGameTicket();

                progressBarHome.setVisibility(View.INVISIBLE);
                txtPlaying.setText(getResources().getString(R.string.total_playing) + String.valueOf(total_playing));

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    //veritabanında gün ve ayda değişiklik olursa işlem yapar
    private void valueGameTicket() {

        gameTicket = new ValueEventGameTicket(this, currentDay, user_week_number, switchOnline);
        gameTicket.newGame();

    }

    //bilet ve reklam durumuna göre fragment açılır
    private void stateGameTicket() {

        if (userTicket.equals("0")) {

            openTicketFragment();

        } else {

            getGameTicket();

        }

    }

    //bilet sayısını düşürür
    private void getGameTicket() {

        progressBarHome.setVisibility(View.VISIBLE);

        int ugt = Integer.valueOf(userTicket);
        ugt--;

        FirebaseDatabase.getInstance().getReference()
                .child("user")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("game_ticket")
                .setValue(String.valueOf(ugt))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {

                            progressBarHome.setVisibility(View.INVISIBLE);
                            kyuAnimationStartGame();

                        }

                    }
                });

    }

    //TicketFragment açılır
    private void openTicketFragment() {

        FragmentManager fragmentManager = this.getSupportFragmentManager();
        TicketFragment ticketFragment = new TicketFragment();
        ticketFragment.show(fragmentManager, "dialog");

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }
}
