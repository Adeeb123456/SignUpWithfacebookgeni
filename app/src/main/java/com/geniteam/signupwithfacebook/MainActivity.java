package com.geniteam.signupwithfacebook;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    CallbackManager mCallbackManager;
    private FirebaseAuth mAuth;
    DatabaseReference databaseReferenceuser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseReferenceuser= FirebaseDatabase.getInstance().getReference();

       // printHashKey(getApplicationContext());
        mCallbackManager = CallbackManager.Factory.create();
        mAuth = FirebaseAuth.getInstance();

        fbLoginWithCustomButton();

        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
            }
        });
    }


    public void writeNewUser(String uid,String email, String name,String picUrl){
        User user=new User(name,email,picUrl);
databaseReferenceuser.child("users").child(uid).setValue(user);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    public  void printHashKey(Context pContext) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i("debug", "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e("debug", "printHashKey()", e);
        } catch (Exception e) {
            Log.e("debug", "printHashKey()", e);
        }
    }

    public void fbLoginWithCustomButton() {
        Button button = (Button) findViewById(R.id.loginbutton2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mCallbackManager = CallbackManager.Factory.create();
                LoginManager.getInstance().logInWithReadPermissions(MainActivity.this, Arrays.asList("email", "public_profile"));
                LoginManager.getInstance().registerCallback(mCallbackManager,
                        new FacebookCallback<LoginResult>() {
                            @Override
                            public void onSuccess(LoginResult loginResult) {
                                // App code
                                // startActivity(new Intent(getApplicationContext(),Main2Activity.class));
                                Toast.makeText(getApplicationContext(), "login success", Toast.LENGTH_LONG).show();
                                Log.d("debug", "facebook:onSuccess:" + loginResult);


                                //By Profile Class
                                Profile profile = Profile.getCurrentProfile();
                                if (profile != null) {
                                 String  facebook_id=profile.getId();
                                    String   f_name=profile.getFirstName();
                                    String    m_name=profile.getMiddleName();
                                    String   l_name=profile.getLastName();
                                    String  full_name=profile.getName();
                                    String   profile_image=profile.getProfilePictureUri(400, 400).toString();
                                }
                                //Toast.makeText(FacebookLogin.this,"Wait...",Toast.LENGTH_SHORT).show();
                                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                                        new GraphRequest.GraphJSONObjectCallback() {
                                            @Override
                                            public void onCompleted(JSONObject object, GraphResponse response) {
                                                try {
                                                   Log.d("debug","json"+ object.toString());
                                                   String  email_id=object.getString("email");
                                                   String gender=object.getString("gender");
                                                    String profile_name=object.getString("name");
                                                    Log.d("debug","name "+profile_name);
                                                   String phone= object.getString("phone");

                                                    Log.d("debug","phone "+phone);
                                                    long fb_id=object.getLong("id"); //use this for logout

                                                } catch (JSONException e) {
                                                    Log.d("debug"," err"+e);
                                                    // TODO Auto-generated catch block
                                                    //  e.printStackTrace();
                                                }

                                            }

                                        });
                                Bundle bundle = new Bundle();
                                bundle.putString(
                                        "fields",
                                        "id,name,link,email,gender,last_name,first_name,locale,timezone,updated_time,verified"
                                );
                                request.setParameters(bundle);
                                request.executeAsync();

                                handleFacebookAccessToken(loginResult.getAccessToken());
                            }

                            @Override
                            public void onCancel() {
                                // App code
                                Toast.makeText(getApplicationContext(), "login fail ", Toast.LENGTH_LONG).show();

                            }

                            @Override
                            public void onError(FacebookException exception) {
                                // App code
                                Toast.makeText(getApplicationContext(), "login fail "+exception, Toast.LENGTH_LONG).show();

                            }
                        });
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

    }


    private void handleFacebookAccessToken(AccessToken token) {
        Log.d("debug", "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("debug", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                      writeNewUser(user.getUid(),user.getEmail(),user.getDisplayName(),user.getPhotoUrl()+"");

                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("deug", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    private void updateUI(FirebaseUser firebaseUser) {
        if(firebaseUser!=null){
            String name=firebaseUser.getDisplayName();
            String email=firebaseUser.getEmail();
            String photoUrl=firebaseUser.getPhotoUrl()+"";
                       //      firebaseUser.getPhoneNumber();
            Log.d("debug","name "+name);

            Log.d("debug","email "+email);
            Log.d("debug","photo url "+photoUrl);

        }else {
            Toast.makeText(getApplicationContext(),"stat login screen",Toast.LENGTH_LONG).show();
        }
    }
}
