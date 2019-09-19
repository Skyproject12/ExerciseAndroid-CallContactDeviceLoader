package com.example.loadermanager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    public static final String TAG = "ContactApp";

    ListView listViewContact;
    ProgressBar progressBar;

    private ContactAdapter adapter;

    // untuk menentukan status permission contact
    private final int CONTACT_REQUEST_CODE = 101;
    // untuk menentukan permission menelpon
    private final int CALL_REQUEST_CODE = 102;
    // untuk melakukan pengambilana dayta telepon
    private final int CONTACT_LOAD = 110;
    // untuk melakukan akses klik kontak
    private final int CONTACT_SELECT = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // inialitation list view
        listViewContact = findViewById(R.id.listview_contact);
        progressBar = findViewById(R.id.progress_bar);

        listViewContact.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.GONE);

        adapter = new ContactAdapter(MainActivity.this, null, true);
        // set adapter for list view
        listViewContact.setAdapter(adapter);
        // set item click listener
        listViewContact.setOnItemClickListener(this);

        // permission for read contact android
        if (PermissionManager.isGranted(this, Manifest.permission.READ_CONTACTS)) {
            getLoaderManager().initLoader(CONTACT_LOAD, null, this);
        } else {
            PermissionManager.check(this, Manifest.permission.READ_CONTACTS, CONTACT_REQUEST_CODE);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        CursorLoader cursorLoader = null;
        if (id == CONTACT_LOAD) {
            // hilangkan progressbar
            progressBar.setVisibility(View.VISIBLE);
            // call value like id, display name, photo uri from contact
            String[] projectionField = new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_URI
            };
            // ketika kontak load maka akan melakukan proses penyimpanan dengan metode sqlite
            cursorLoader = new CursorLoader(MainActivity.this,
                    ContactsContract.Contacts.CONTENT_URI,
                    projectionField,
                    // where memiliki number telepon = 1
                    ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1",
                    null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");;
        } else if (id == CONTACT_SELECT) {
            String[] phoneProjectionField = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
            cursorLoader = new CursorLoader(MainActivity.this, ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    phoneProjectionField,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "= ? AND " + ContactsContract.CommonDataKinds.Phone.TYPE + " = " + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + " AND " + ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + " =1 ",
                    new String[]{bundle.getString("id")},
                    null);
        }
        return cursorLoader;
    }

    // CONTACT_LOAD digunakan untuk load contact
    // CONTACT_SELECT digunakan melakukan telepon akses

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Toast.makeText(this, "finish load", Toast.LENGTH_SHORT).show();
        if (loader.getId() == CONTACT_LOAD) {
            if (cursor.getCount() > 0) {
                listViewContact.setVisibility(View.VISIBLE);
                adapter.swapCursor(cursor);
            }
            progressBar.setVisibility(View.GONE);
        } else if (loader.getId() == CONTACT_SELECT) {
            String contactNumber = null;
            if (cursor.moveToFirst()) {
                contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
            // check permisiion
            if (PermissionManager.isGranted(this, Manifest.permission.CALL_PHONE)) {
                if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                }
                Intent intent = new Intent(Intent.ACTION_CALL,
                        Uri.parse("tel:" + contactNumber));

                startActivity(intent);
            }
            else {
                PermissionManager.check ( this, Manifest.permission.CALL_PHONE, CALL_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // reset loader
        if(loader.getId() == CONTACT_LOAD){
            progressBar.setVisibility(View.GONE);
            adapter.swapCursor(null);
            Log.d(TAG, "Loader Reset");
        }
    }

    // onclick listview
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // click item with position item
        Cursor cursor= (Cursor) adapterView.getAdapter().getItem(i);
        // call id list view android
        long ContacId= cursor.getLong(0);
        Log.d(TAG, "Position : "+i+""+ContacId);
        // call value of phone number
        getPhoneNumber(String.valueOf(ContacId));
    }

    private void getPhoneNumber(String contactId) {
        // gett all value of phone from bundle
        Bundle bundle= new Bundle();
        bundle.putString("id",contactId );
        getLoaderManager().restartLoader(CONTACT_SELECT, bundle, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CONTACT_REQUEST_CODE ) {
            if(grantResults.length >0){
                // check permissiion
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    getLoaderManager().initLoader(CONTACT_LOAD, null, this);
                    Toast.makeText(this, "Contact permission diterima", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(this, "Contact permission ditolak", Toast.LENGTH_SHORT).show();
                }
            }

        }
        else if(requestCode == CALL_REQUEST_CODE ) {
            if(grantResults.length >0){
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    Toast.makeText(this, "Call permission diterima", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(this, "Call permission ditolak", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
