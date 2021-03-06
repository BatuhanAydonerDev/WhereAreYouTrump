package com.moonturns.batuhanaydoner.whereareyoutrump;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
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
import com.moonturns.batuhanaydoner.whereareyoutrump.FirebaseDatabase.Leader;
import com.moonturns.batuhanaydoner.whereareyoutrump.FirebaseDatabase.User;
import com.moonturns.batuhanaydoner.whereareyoutrump.FirebaseDatabase.ValueEventGameTicket;
import com.moonturns.batuhanaydoner.whereareyoutrump.GameFragements.LoadingFragment;
import com.moonturns.batuhanaydoner.whereareyoutrump.GameFragements.TicketFragment;
import com.moonturns.batuhanaydoner.whereareyoutrump.database.ScoreDatabase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;

import java.util.Random;

import io.realm.Realm;
import io.realm.RealmResults;

public class ScoreActivity extends AppCompatActivity implements RewardedVideoAdListener {

    private final int scoreNews = 65; //imageNews açılması için geçilmesi gereken skor
    private final String stateWeek = "play";

    private ValueEventGameTicket gameTicket;
    private String currentDay = "";

    private Realm realm;
    private RealmResults<ScoreDatabase> realmResults;

    private int score = 0, highScore = 0, replayScore = 0, playing = 0;
    private int countPlaying = 0; //toplam oynama sayısı

    private ImageView imageTrumpUn, imageHighScore, imageScoreTrump, imagePlay, imageNewsScore, imageLeader;
    private TextView txtDescription, txtScore, txtHighScore, txtTime, txtHint, txtAdvScore, txtLeaderScore, txtUserScore;

    private ProgressBar progressBarScore, progressBarAdv;

    private String[] listDescription;

    private CountDownTimer timer;

    private String[] hints;

    private MediaPlayer mediaPlayer;
    private int pauseMusicTime = 0; //müziğin durduruldupu süreyi alır

    private boolean stateSound = false; //sesin durumu
    private boolean changedHighScore = false; //high score değişirse true döner

    private RewardedVideoAd mAd; //ödüllü reklam

    private boolean adv = false; //ödüllü reklam izlenileceği zaman true döner

    private int scoreLeader = 0;
    private String username = "";

    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private boolean onlineGame = false;

    private String user_week_number = "";
    private String onlineTotalScore = "";
    private String userTicket = ""; //kullanıcının game ticket sayısı
    private String userAdv = ""; //kullanıcının reklam sayısı
    private String userTotalPlaying = "";
    private int ots = 0; //online total score
    private int ut = 0; //string game ticket long olur
    private int ua = 0; //string adv ong olur
    private int utp = 0; //string userTotalPlaying int olur

    private boolean clickable = true; //progressbar açıksa true döner ve ekrana basılmasına izin vermez
    private boolean onlineChanging = false; //değişiklikler kaydedildiyse true olur
    private int i=0; //imageLeader renkleri
    private Handler handler;
    private Runnable run;

    private LoadingFragment loadingFragment;

    private void crt() {

        imageTrumpUn = (ImageView) this.findViewById(R.id.imageTrumpUn);
        imageHighScore = (ImageView) this.findViewById(R.id.imageHighScore);
        imageScoreTrump = (ImageView) this.findViewById(R.id.imageScoreTrump);
        imagePlay = (ImageView) this.findViewById(R.id.imagePlay);
        imageNewsScore = (ImageView) this.findViewById(R.id.imageNewsScore);
        imageLeader = (ImageView) this.findViewById(R.id.imageLeader);

        txtDescription = (TextView) this.findViewById(R.id.txtDescription);
        txtScore = (TextView) this.findViewById(R.id.txtScore);
        txtHighScore = (TextView) this.findViewById(R.id.txtHighScore);
        txtTime = (TextView) this.findViewById(R.id.txtTime);
        txtHint = (TextView) this.findViewById(R.id.txtHint);
        txtAdvScore = (TextView) this.findViewById(R.id.txtAdvScore);
        txtLeaderScore = (TextView) this.findViewById(R.id.txtLeaderScore);
        txtUserScore = (TextView) this.findViewById(R.id.txtUserScore);

        progressBarScore = (ProgressBar) this.findViewById(R.id.progressBarScore);
        progressBarAdv = (ProgressBar) this.findViewById(R.id.progressBarAdv);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stateView();

        rewardedAdvertisement();

        onlineGame = getIntent().getBooleanExtra("onlineGame", false);
        invisibleImagePlay();

        getGameScore();
        initAuthStateListener();

        replayScore = getIntent().getIntExtra("replayScore", 0);

        getStateSound();
        gameHints();
        initDatabase();
        plusTime();
        restartGame();

    }

    //onlineGame durumunu göre görsel açılır
    private void stateView() {

        onlineGame = getIntent().getBooleanExtra("onlineGame", false);

        if (onlineGame) {

            setContentView(R.layout.activity_online_score);
            crt();

        } else {

            setContentView(R.layout.activity_score);
            crt();
            changeImage();
            changeDescription();

        }

    }

    @Override
    protected void onResume() {
        mAd.resume(this);
        super.onResume();

        playMusic();

    }

    @Override
    protected void onStart() {
        super.onStart();

        stateMusic();

        FirebaseAuth.getInstance().addAuthStateListener(mAuthStateListener);

    }

    @Override
    protected void onPause() {
        mAd.pause(this);
        super.onPause();

        pauseMusic();

    }

    @Override
    protected void onStop() {
        super.onStop();

        FirebaseAuth.getInstance().removeAuthStateListener(mAuthStateListener);

    }

    @Override
    protected void onDestroy() {
        mAd.destroy(this);
        super.onDestroy();

    }

    //online oynanıyorsa değerler alınana kadar imagePlay gösterilmeyecek
    private void invisibleImagePlay() {

        if (onlineGame) {

            imagePlay.setVisibility(View.INVISIBLE);
            showProgressBar();

        }

    }

    //AuthStateListener
    private void initAuthStateListener() {

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null && onlineGame && !onlineChanging) {

                    getUserInfos();

                } else {

                    getNews();

                }

            }
        };

    }

    //veritabanında gün ve ayda değişiklik olursa işlem yapar
    private void valueGameTicket() {

        gameTicket = new ValueEventGameTicket(this, currentDay, user_week_number);
        gameTicket.newGame();

    }

    //oyunu yeniden başlatır listener
    private void restartGame() {

        imagePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                closeMusic();
                stateOnline();

            }
        });


    }

    //FieldOfActivity intent
    private void openFieldOfGameActivity() {

        Intent intent = new Intent(ScoreActivity.this, FieldOfGameActivity.class);
        intent.putExtra("replayScore", replayScore);
        intent.putExtra("hintTime", txtTime.getText().toString());
        intent.putExtra("onlineGame", onlineGame);
        intent.putExtra("highScore", highScore);
        startActivity(intent);
        finish();

    }

    //online duruma göre activity açılır
    private void stateOnline() {

        if (clickable) {

            if (onlineGame) {

                if (gameTicket.getWeek().equals("")) {

                    Toast.makeText(ScoreActivity.this, R.string.internet, Toast.LENGTH_LONG).show();

                } else {

                    if (gameTicket.getWeek().equals(stateWeek)) {

                        playWithAdv();

                    } else {

                        gameTicket.openWeekFragment();

                    }

                }

            } else {

                openFieldOfGameActivity();

            }

        }

    }

    //oyunda yapılan puanı alır getIntent
    private void getGameScore() {

        score = getIntent().getIntExtra("score", 0);
        txtScore.setText("" + score);


    }

    //veritabanı oluşturur
    private void initDatabase() {

        if (!onlineGame) {

            realm = Realm.getDefaultInstance();

            realmResults = realm.where(ScoreDatabase.class).findAll();
            if (realmResults.size() == 0) {

                realm.beginTransaction();

                replayScore++;
                playing++;

                ScoreDatabase scoreDatabase = realm.createObject(ScoreDatabase.class);
                scoreDatabase.setScore(score);
                scoreDatabase.setPlaying(playing);

                realm.commitTransaction();

                txtHighScore.setText("" + score);

            } else {

                highScoreDegistir(score);
                countPlaying();

            }

        }

    }

    //yeni rekor yapılınca çalışır
    private void highScoreDegistir(final int getScore) {

        if (!onlineGame) {

            highScore = realmResults.get(0).getScore();

            if (score > highScore) {

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {

                        ScoreDatabase scoreDatabase = realmResults.get(0);
                        scoreDatabase.setScore(score);

                        txtHighScore.setText("" + score);

                        changedHighScore = true;
                        highScoreMusic();

                    }
                });

            } else {

                txtHighScore.setText("" + highScore);

            }

        }

    }

    //yapılan skora göre resim getirir
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void changeImage() {

        int[] images = {R.drawable.image_trump_23, R.drawable.image_trump_11, R.drawable.image_trump_1, R.drawable.image_trump_20, R.drawable.image_trump_31, R.drawable.kim_yong_un_6,
                R.drawable.image_trump_19, R.drawable.image_trump_7, R.drawable.image_trump_2, R.drawable.image_trump_5, R.drawable.image_trump_7, R.drawable.image_trump_10, R.drawable.image_trump_11, R.drawable.image_trump_14,
                R.drawable.image_trump_16, R.drawable.image_trump_17, R.drawable.image_trump_12, R.drawable.image_trump_3, R.drawable.image_trump_26, R.drawable.image_trump_8, R.drawable.image_trump_9,
                R.drawable.image_trump_30, R.drawable.image_trump_18, R.drawable.image_trump_22, R.drawable.image_trump_6, R.drawable.image_trump_25, R.drawable.image_trump_7, R.drawable.image_trump_6, R.drawable.image_trump_4,
                R.drawable.image_trump_28, R.drawable.image_trump_33, R.drawable.image_trump_32, R.drawable.image_trump_35, R.drawable.image_trump_15, R.drawable.image_trump_33, R.drawable.image_trump_24,
                R.drawable.image_trump_13, R.drawable.kim_yong_un_6, R.drawable.kim_yong_un_7, R.drawable.kim_yong_un_1, R.drawable.kim_yong_un_4, R.drawable.kim_yong_un_8,
                R.drawable.kim_yong_un_2, R.drawable.kim_yong_un_9};

        if (score > 5 && score < 49) {

            imageTrumpUn.setBackground(getDrawable(images[score - 5]));

        } else if (score == 49) {

            imageTrumpUn.setBackground(getDrawable(images[43]));

        } else {

            imageTrumpUn.setBackground(getDrawable(images[0]));

        }

    }

    //yapılan skora göre açıklama getirir
    private void changeDescription() {

        listDescription = getResources().getStringArray(R.array.discriptions);

        /*listDescription = new String[]{"Touch that!!!OKAY!!!", "Owwww!!!I want to SPEW!!!", "HAHA...Look that", "HAHA...Believe yourself...You can be successful", "I got so bored",
                "Do you want to eat MISSILE", "Clever guy...haha I deceived...", "When will you play well", "Is telephone in your hands?You cannot play", "Hey you,give up right now",
                "I think you do not play", "Do not touch telephone and move away", "I want to SPEW,do you know?", "You will play, one day!!!", "I want to meet with you", "Are you Kim Yong Un?",
                "You are guy who I want...", "Are you a Gamer?", "Do you want to earn my dollar but how,you know", "You are like serious!!!!", "You can not win because I AM STRONG",
                "HAHA,you can do worse", "Some guys play with me!!!!", "You have a BRAIN,use it...", "You are like successful,I am suprised!!!!", "I like PEOPLE,espicially Mexican people...money...",
                "I am like scared because you scare me!!!!", "You can play it...That is fine!!!!", "Hey you,you make me feel angry!!!!", "I know where you are...",
                "Do you want to read NEWS?You have to be successful", "You are approaching and this scare me", "I will be waiting you where you have to come",
                "I will know you when you will win if you want", "You are guy who I work to find...", "I CAN SEE YOU", "Again me,You have to try...",
                "I do not want to send you MISSILE...No longer,be successful", "I can help you,just touch button", "I am ready to send MISSILE", "I want to appluad you but you could not find",
                "I am so happy because you will find Trump!!!!", "I want to send MISSILE for all of bad people but first time for Trump", "Where Are You Trump,You are so close to find Trump"};*/

        if (score > 5 && score < 49) {

            txtDescription.setText(listDescription[score - 5]);

        } else if (score == 49) {

            txtDescription.setText(listDescription[43]);

        } else {

            txtDescription.setText(listDescription[0]);

        }

    }

    //oyun 20 kez oynandığı zaman yazı çıkar
    private void plusTime() {

        if (replayScore >= 20) {

            //moreTime++;
            timer = new CountDownTimer(30000, 1000) {
                @Override
                public void onTick(long l) {

                    long visibility = l / 1000;

                    txtTime.setText("You won +30 time");

                    if (visibility % 2 == 0) {

                        txtTime.setVisibility(View.VISIBLE);

                    } else {

                        txtTime.setVisibility(View.INVISIBLE);

                    }

                }

                @Override
                public void onFinish() {

                    txtTime.setVisibility(View.VISIBLE);

                }
            }.start();

        }

    }

    //oyun hakkında bazı ipuçları
    private void gameHints() {

        Random random = new Random();

        int randomHint = random.nextInt(7 - 0);

        hints = getResources().getStringArray(R.array.hints);

        /*hints = new String[]{"KYU:18 Touching for time but where?", "20 Game for time!!!!", "Do you have a ticket", "You will get +15", "Play more",
                "18", "20", "Replay", "Play everyday", "First rule:Enjoy it", "Discover surprises", "You did not see all of suprises", "High score,more score..."};*/

        txtTime.setText(hints[randomHint]);

        switch (score) {

            case 18:

                txtTime.setText(hints[0]);

                break;

        }

    }

    //toplam oynama sayısını verir ve günceller
    private void countPlaying() {

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {

                countPlaying = realmResults.get(0).getPlaying();
                countPlaying++;

                ScoreDatabase scoreDatabase = realmResults.get(0);
                scoreDatabase.setPlaying(countPlaying);

            }
        });

    }

    //oyunun oynandığı activity içinde bulunan mediaplayer nesnesini bu sınıfta ki mediaplayer nesnesine atar
    private void stateMusic() {

        if (!changedHighScore) {

            mediaPlayer = FieldOfGameActivity.mediaPlayer;

        }

    }

    //bu activity kapatılınca müzik kapatılır
    private void closeMusic() {

        if (mediaPlayer != null) {

            mediaPlayer.stop();

        }

    }

    //mevcut bir müzik varsa durdurur
    private void pauseMusic() {

        if (mediaPlayer != null && stateSound) {

            mediaPlayer.pause();

            pauseMusicTime = mediaPlayer.getCurrentPosition();

        }

    }

    //durdurulmuş müziği devam ettirir
    private void playMusic() {

        if (pauseMusicTime > 0) {

            mediaPlayer.selectTrack(pauseMusicTime);
            mediaPlayer.start();

        }

    }

    //high score yapılınca açılacak müzik
    private void highScoreMusic() {

        if (stateSound) {

            FieldOfGameActivity.mediaPlayer.stop();

            mediaPlayer = null;
            mediaPlayer = MediaPlayer.create(this, R.raw.game_music_news);
            mediaPlayer.start();

        }

    }

    private void getStateSound() {

        stateSound = getIntent().getBooleanExtra("stateSound", false);

    }

    //MainActivity açılır intent
    private void openMainActivity() {

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    private void changeLeaderColor(){

        final int[] colors={Color.RED,Color.WHITE};
        handler=new Handler();

        run = new Runnable() {
            @Override
            public void run() {

                if (i<colors.length){

                    imageLeader.setImageTintList(ColorStateList.valueOf(colors[i]));
                    i++;

                }else {

                    i=0;
                    imageLeader.setImageTintList(ColorStateList.valueOf(colors[i]));
                    i++;

                }

                handler.postDelayed(run, 750);

            }
        };

        handler.post(run);

    }

    //kullanıcının toplam skoru veritabanından alınır
    private void getUserInfos() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        FirebaseUser kullanici = FirebaseAuth.getInstance().getCurrentUser();

        Query sorgu = reference.child("user").orderByKey().equalTo(kullanici.getUid());
        sorgu.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot single : dataSnapshot.getChildren()) {

                    User user = single.getValue(User.class);
                    username = user.getUser_name();
                    onlineTotalScore = user.getTotal_score();
                    userTicket = user.getGame_ticket();
                    userAdv = user.getAdv();
                    userTotalPlaying = user.getTotal_playing();
                    highScore = Integer.valueOf(user.getHigh_score());
                    currentDay = user.getDay();
                    user_week_number = user.getWeek_number();
                }

                valueGameTicket();

                onlineChanging = true;

                getNews();
                changeOnlineHighScore();

                imagePlay.setVisibility(View.VISIBLE);
                increaseOnlineTotalPlaying();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        sorgu.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot single : dataSnapshot.getChildren()) {

                    User user = single.getValue(User.class);
                    onlineTotalScore = user.getTotal_score();
                    currentDay = user.getDay();

                    txtUserScore.setText(onlineTotalScore);
                    ots=Integer.valueOf(onlineTotalScore);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    //online high score değiştirilir
    private void changeOnlineHighScore() {

        if (score > highScore) {

            FirebaseDatabase.getInstance().getReference()
                    .child("user")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child("high_score")
                    .setValue(String.valueOf(score)).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {

                        txtHighScore.setText(String.valueOf(score));

                    }

                }
            });

        } else {

            txtHighScore.setText(String.valueOf(highScore));

        }

    }

    //online durumdayken yapılan skora bakar
    private void getNews() {

        if (score >= scoreNews) {

            imageNewsScore.setVisibility(View.VISIBLE);
            showNews();

        }else {

            imageNewsScore.setVisibility(View.INVISIBLE);

        }

    }

    //imageNewsScore listener
    private void showNews() {

        imageNewsScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                score=0;
                getNews();
                openNewsActivity();

            }
        });

    }

    //skor 50 ve üstü olursa bu activity açılır
    private void openNewsActivity() {

        Intent intent = new Intent(this, NewsActivity.class);
        intent.putExtra("onlineGame", onlineGame);
        intent.putExtra("scoreLeader", scoreLeader);
        intent.putExtra("ots", ots);
        startActivity(intent);

    }

    //online toplam oynamayı artırır
    private void increaseOnlineTotalPlaying() {

        utp = Integer.valueOf(userTotalPlaying);
        utp++;

        if (utp % 2 == 0) {

            txtAdvScore.setVisibility(View.VISIBLE);
            progressBarAdv.setVisibility(View.VISIBLE);

        }

        FirebaseDatabase.getInstance().getReference()
                .child("user")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("total_playing")
                .setValue(String.valueOf(utp))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {

                            addTotalScore();

                        } else {

                            Toast.makeText(ScoreActivity.this, R.string.sendVerifyEmailError, Toast.LENGTH_LONG).show();

                        }

                    }
                });


    }

    //online kısımda toplam skoru toplar
    private void addTotalScore() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        int currentScore = Integer.valueOf(txtScore.getText().toString());
        ots = Integer.valueOf(onlineTotalScore);

        ots = ots + currentScore;

        FirebaseDatabase.getInstance().getReference().child("user").child(user.getUid()).child("total_score").setValue(String.valueOf(ots)).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (!task.isSuccessful()) {

                    Toast.makeText(ScoreActivity.this, R.string.sendVerifyEmailError, Toast.LENGTH_LONG).show();

                } else {

                    getFirstPlayerScore();

                }

            }
        });

    }

    //lider alanını veritabanından alır
    private void getFirstPlayerScore() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query sorgu = reference.child("leader").orderByKey().equalTo("player");
        sorgu.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot single : dataSnapshot.getChildren()) {

                    Leader leader = single.getValue(Leader.class);
                    String puid = leader.getPlayer_uid();
                    if (!leader.getLeader_score().equals("-")) {

                        scoreLeader = Integer.valueOf(leader.getLeader_score());

                    } else {

                        scoreLeader = 0;

                    }
                    changeFirstPlayer(puid);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    //veritabanında lider oyuncu boşsa ilk oynayan doldurur
    private void changeFirstPlayer(String player_uid) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (player_uid.equals("-")) {

            compareScores(ots, scoreLeader);

        } else if (player_uid.equals(user.getUid())) {

            FirebaseDatabase.getInstance().getReference().child("leader").child("player").child("leader_score").setValue(String.valueOf(ots)).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (!task.isSuccessful()) {

                        closeProgressBar();
                        Toast.makeText(ScoreActivity.this, R.string.sendVerifyEmailError, Toast.LENGTH_LONG).show();

                    } else {

                        closeProgressBar();
                        imageLeader.setVisibility(View.VISIBLE);
                        changeLeaderColor();
                        txtUserScore.setText(String.valueOf(ots));
                        txtLeaderScore.setText(String.valueOf(ots));

                    }

                }
            });

        } else {

            compareScores(ots, scoreLeader);

        }

    }

    //kullanıcının ve lider oyuncunun skorlarını karşılaştırır
    private void compareScores(final int score, final int leader) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (score > leader) {

            FirebaseDatabase.getInstance().getReference().child("leader").child("player").child("player_uid").setValue(user.getUid());
            FirebaseDatabase.getInstance().getReference().child("leader").child("player").child("leader_score").setValue(String.valueOf(ots));
            FirebaseDatabase.getInstance().getReference().child("leader").child("player").child("leader_username").setValue(username).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {

                        closeProgressBar();
                        imageLeader.setVisibility(View.VISIBLE);
                        changeLeaderColor();
                        txtLeaderScore.setText(String.valueOf(ots));
                        txtUserScore.setText(String.valueOf(ots));


                    } else {

                        closeProgressBar();
                        Toast.makeText(ScoreActivity.this, R.string.sendVerifyEmailError, Toast.LENGTH_LONG).show();

                    }

                }
            });

        } else {

            closeProgressBar();
            txtLeaderScore.setText(String.valueOf(scoreLeader));
            txtUserScore.setText(String.valueOf(ots));

        }

    }

    //kullanıcının hakkı yoksa reklam izletir ve eğer varsa hakkından düşürür
    private void playWithAdv() {

        showProgressBar();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        ut = Integer.valueOf(userTicket);
        ua = Integer.valueOf(userAdv);

        if (ut != 0) {

            ut = ut - 1;
            FirebaseDatabase.getInstance().getReference().child("user").child(user.getUid()).child("game_ticket").setValue(String.valueOf(ut)).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {

                        closeProgressBar();
                        openFieldOfGameActivity();

                    } else {

                        closeProgressBar();
                        Toast.makeText(ScoreActivity.this, R.string.sendVerifyEmailError, Toast.LENGTH_LONG).show();

                    }

                }
            });

        } else if (userTicket.equals("")) {

            closeProgressBar();
            Toast.makeText(ScoreActivity.this, R.string.sendVerifyEmailError, Toast.LENGTH_LONG).show();

        } else {

            openTicketFragment();
            closeProgressBar();

        }

    }

    //TicketFragment açılır
    private void openTicketFragment() {

        FragmentManager fragmentManager = this.getSupportFragmentManager();
        TicketFragment ticketFragment = new TicketFragment();
        ticketFragment.show(fragmentManager, "dialog");

    }

    //progressbar gösterir
    private void showProgressBar() {

        progressBarScore.setVisibility(View.VISIBLE);
        clickable = false;

    }

    //progressbar kapatır
    private void closeProgressBar() {

        progressBarScore.setVisibility(View.INVISIBLE);
        clickable = true;

    }

    //LoadingFragment açılır
    private void showFragmentProgressBar(){

        FragmentManager fragmentManager=this.getSupportFragmentManager();
        loadingFragment=new LoadingFragment();
        loadingFragment.show(fragmentManager,"dialog");

    }

    //LoadingFragment kapatılır
    private void closeFragmentProgressBar(){

        loadingFragment.dismiss();

    }

    //txtAdvScore listener
    private void setTxtAdvScore() {

        txtAdvScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showProgressBar();
                showFragmentProgressBar();
                adv = true;
                startVideoAdd();

            }
        });

    }

    //reklamdan sonra yapılan skor kadar skor eklenir
    private void advMoreScore() {

        ots = ots + score;
        score*=2;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseDatabase.getInstance().getReference().child("user").child(user.getUid()).child("total_score").setValue(String.valueOf(ots)).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()) {

                    txtScore.setText(String.valueOf(score));
                    changeLeader();

                } else {

                    Toast.makeText(ScoreActivity.this, R.string.sendVerifyEmailError, Toast.LENGTH_LONG).show();

                }

            }
        });

    }

    //reklam izlendikten sonra kullanıcının skoru liderin skorundan fazlaysa yeni lider olur ve veritabanına kaydedilir
    private void changeLeader() {

        if (ots > scoreLeader || imageLeader.getVisibility() == View.VISIBLE) {

            FirebaseDatabase.getInstance().getReference().child("leader").child("player").child("player_uid").setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
            FirebaseDatabase.getInstance().getReference().child("leader").child("player").child("leader_score").setValue(String.valueOf(ots));
            FirebaseDatabase.getInstance().getReference().child("leader").child("player").child("leader_username").setValue(FirebaseAuth.getInstance().getCurrentUser().getDisplayName()).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (!task.isSuccessful()) {

                        Toast.makeText(ScoreActivity.this, R.string.sendVerifyEmailError, Toast.LENGTH_LONG).show();

                    }else {

                        txtLeaderScore.setText(String.valueOf(ots));

                    }

                }
            });

        }else {

            txtLeaderScore.setText(String.valueOf(scoreLeader));

        }

    }

    //ödüllü reklam
    private void rewardedAdvertisement() {

        MobileAds.initialize(this, "ca-app-pub-5539936226294378~5338627173");

        mAd = MobileAds.getRewardedVideoAdInstance(this);
        mAd.setRewardedVideoAdListener(this);
        loadRewardedVideoAd();

    }

    //ödüllü reklamı yükler
    private void loadRewardedVideoAd() {

        if (!mAd.isLoaded()) {

            mAd.loadAd("ca-app-pub-5539936226294378/9099025741",
                    new AdRequest.Builder().build());

        }

    }

    //video başlatılır
    private void startVideoAdd() {

        if (mAd.isLoaded()) {

            mAd.show();

        }

    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        openMainActivity();

    }

    @Override
    public void onRewardedVideoAdLoaded() {

        progressBarAdv.setVisibility(View.INVISIBLE);
        closeFragmentProgressBar();
        setTxtAdvScore();

    }

    @Override
    public void onRewardedVideoAdOpened() {

    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoAdClosed() {

        closeProgressBar();
        loadRewardedVideoAd();
        txtAdvScore.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onRewarded(RewardItem rewardItem) {

        advMoreScore();

    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {

    }

    @Override
    public void onRewardedVideoCompleted() {

    }
}
