/**
 * Copyright (C) 2012 Iordan Iordanov
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.bVNC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActivityManager.MemoryInfo;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import net.sqlcipher.database.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.iiordanov.bVNC.dialogs.ImportExportDialog;
import com.iiordanov.bVNC.dialogs.ImportTlsCaDialog;
import com.iiordanov.bVNC.dialogs.IntroTextDialog;
import com.iiordanov.pubkeygenerator.GeneratePubkeyActivity;

/**
 * aSPICE is the Activity for setting up SPICE connections.
 */
public class aSPICE extends MainConfiguration {
    private final static String TAG = "aSPICE";
    private Spinner connectionType;
    private int selectedConnType;
    private TextView sshCaption;
    private LinearLayout sshCredentials;
    private LinearLayout layoutUseSshPubkey;
    private LinearLayout sshServerEntry;
    private LinearLayout layoutAdvancedSettings;
    private EditText sshServer;
    private EditText sshPort;
    private EditText sshUser;
    private EditText sshPassword;
    private EditText sshPassphrase;
    private EditText ipText;
    private EditText portText;
    private Button buttonImportCa;
    private EditText tlsPort;
    private EditText passwordText;
    private Button goButton;
    private Button buttonGeneratePubkey;
    private ToggleButton toggleAdvancedSettings;
    private Spinner spinnerGeometry;
    private EditText textNickname;
    private EditText resWidth;
    private EditText resHeight;
    private CheckBox checkboxKeepPassword;
    private CheckBox checkboxUseDpadAsArrows;
    private CheckBox checkboxRotateDpad;
    private CheckBox checkboxLocalCursor;
    private CheckBox checkboxUseSshPubkey;
    private CheckBox checkboxEnableSound;
    private Spinner layoutMapSpinner = null;
    private List<String> spinnerArray = null;

    @Override
    public void onCreate(Bundle icicle) {
        layoutID = R.layout.main_spice;
        super.onCreate(icicle);
        
        ipText = (EditText) findViewById(R.id.textIP);
        sshServer = (EditText) findViewById(R.id.sshServer);
        sshPort = (EditText) findViewById(R.id.sshPort);
        sshUser = (EditText) findViewById(R.id.sshUser);
        sshPassword = (EditText) findViewById(R.id.sshPassword);
        sshPassphrase = (EditText) findViewById(R.id.sshPassphrase);
        sshCredentials = (LinearLayout) findViewById(R.id.sshCredentials);
        sshCaption = (TextView) findViewById(R.id.sshCaption);
        layoutUseSshPubkey = (LinearLayout) findViewById(R.id.layoutUseSshPubkey);
        sshServerEntry = (LinearLayout) findViewById(R.id.sshServerEntry);
        portText = (EditText) findViewById(R.id.textPORT);
        tlsPort = (EditText) findViewById(R.id.tlsPort);
        passwordText = (EditText) findViewById(R.id.textPASSWORD);
        textNickname = (EditText) findViewById(R.id.textNickname);
        
        buttonImportCa = (Button) findViewById(R.id.buttonImportCa);
        buttonImportCa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aSPICE.this.updateSelectedFromView();
                showDialog(R.layout.import_tls_ca_dialog);
            }
        });
        
        // Here we say what happens when the Pubkey Checkbox is
        // checked/unchecked.
        checkboxUseSshPubkey = (CheckBox) findViewById(R.id.checkboxUseSshPubkey);
        
        // Here we say what happens when the Pubkey Generate button is pressed.
        buttonGeneratePubkey = (Button) findViewById(R.id.buttonGeneratePubkey);
        buttonGeneratePubkey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generatePubkey();
            }
        });
        
        // Define what happens when somebody selects different VNC connection
        // types.
        connectionType = (Spinner) findViewById(R.id.connectionType);
        connectionType
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> ad, View view,
                            int itemIndex, long id) {
                        
                        selectedConnType = itemIndex;
                        if (selectedConnType == Constants.CONN_TYPE_PLAIN) {
                            setVisibilityOfSshWidgets(View.GONE);
                        } else if (selectedConnType == Constants.CONN_TYPE_SSH) {
                            setVisibilityOfSshWidgets(View.VISIBLE);
                            if (ipText.getText().toString().equals(""))
                                ipText.setText("localhost");
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> ad) {
                    }
                });

        checkboxKeepPassword = (CheckBox) findViewById(R.id.checkboxKeepPassword);
        checkboxUseDpadAsArrows = (CheckBox) findViewById(R.id.checkboxUseDpadAsArrows);
        checkboxRotateDpad = (CheckBox) findViewById(R.id.checkboxRotateDpad);
        checkboxLocalCursor = (CheckBox) findViewById(R.id.checkboxUseLocalCursor);
        checkboxEnableSound = (CheckBox) findViewById(R.id.checkboxEnableSound);
        
        goButton = (Button) findViewById(R.id.buttonGO);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ipText.getText().length() != 0
                        && (portText.getText().length() != 0 || tlsPort.getText().length() != 0))
                    canvasStart();
                else
                    Toast.makeText(view.getContext(),
                            R.string.spice_server_empty, Toast.LENGTH_LONG)
                            .show();
            }
        });

        // The advanced settings button.
        toggleAdvancedSettings = (ToggleButton) findViewById(R.id.toggleAdvancedSettings);
        layoutAdvancedSettings = (LinearLayout) findViewById(R.id.layoutAdvancedSettings);
        toggleAdvancedSettings.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton arg0,
                            boolean checked) {
                        if (checked)
                            layoutAdvancedSettings.setVisibility(View.VISIBLE);
                        else
                            layoutAdvancedSettings.setVisibility(View.GONE);
                    }
                });

        // The geometry type and dimensions boxes.
        spinnerGeometry = (Spinner) findViewById(R.id.spinnerRdpGeometry);
        resWidth = (EditText) findViewById(R.id.rdpWidth);
        resHeight = (EditText) findViewById(R.id.rdpHeight);        
        spinnerGeometry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener () {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                selected.setRdpResType(itemIndex);
                setRemoteWidthAndHeight ();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        // Define what happens when the Import/Export button is pressed.
        ((Button) findViewById(R.id.buttonImportExport))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        android.util.Log.e(TAG, "import/export!!");
                        showDialog(R.layout.importexport);
                    }
                });
        
        // Load list of items from asset folder and populate
        try {
            spinnerArray = listFiles("layouts");
        } catch (IOException e) {
            e.printStackTrace();
        }
        layoutMapSpinner = (Spinner) findViewById(R.id.layoutMaps);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter =  new ArrayAdapter<String> (this, android.R.layout.simple_spinner_item, spinnerArray);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        layoutMapSpinner.setAdapter(adapter);
    }

    /**
     * Makes the ssh-related widgets visible/invisible.
     */
    private void setVisibilityOfSshWidgets(int visibility) {
        sshCredentials.setVisibility(visibility);
        sshCaption.setVisibility(visibility);
        layoutUseSshPubkey.setVisibility(visibility);
        sshServerEntry.setVisibility(visibility);
    }

    /**
     * Enables and disables the EditText boxes for width and height of remote desktop.
     */
    private void setRemoteWidthAndHeight () {
        if (selected.getRdpResType() != Constants.RDP_GEOM_SELECT_CUSTOM) {
            resWidth.setEnabled(false);
            resHeight.setEnabled(false);
        } else {
            resWidth.setEnabled(true);
            resHeight.setEnabled(true);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case R.layout.importexport:
            return new ImportExportDialog(this);
        case R.id.itemMainScreenHelp:
            return createHelpDialog();
        case R.layout.import_tls_ca_dialog:
            return new ImportTlsCaDialog(this);
        }
        return null;
    }

    /**
     * Creates the help dialog for this activity.
     */
    private Dialog createHelpDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this).setMessage(
                R.string.spice_main_screen_help_text).setPositiveButton(
                R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // We don't have to do anything.
                    }
                });
        Dialog d = adb.setView(new ListView(this)).create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.FILL_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        d.getWindow().setAttributes(lp);
        return d;
    }
    
    public void updateViewFromSelected() {
        if (selected == null)
            return;
        selectedConnType = selected.getConnectionType();
        connectionType.setSelection(selectedConnType);
        sshServer.setText(selected.getSshServer());
        sshPort.setText(Integer.toString(selected.getSshPort()));
        sshUser.setText(selected.getSshUser());

        checkboxUseSshPubkey.setChecked(selected.getUseSshPubKey());

        if (selectedConnType == Constants.CONN_TYPE_SSH
                && selected.getAddress().equals(""))
            ipText.setText("localhost");
        else
            ipText.setText(selected.getAddress());

        if (selected.getPort() < 0) {
            portText.setText("");
        } else {
            portText.setText(Integer.toString(selected.getPort()));
        }
        if (selected.getTlsPort() < 0) {
            tlsPort.setText("");
        } else {
            tlsPort.setText(Integer.toString(selected.getTlsPort()));
        }

        if (selected.getKeepPassword() || selected.getPassword().length() > 0) {
            passwordText.setText(selected.getPassword());
        }

        checkboxKeepPassword.setChecked(selected.getKeepPassword());
        checkboxUseDpadAsArrows.setChecked(selected.getUseDpadAsArrows());
        checkboxRotateDpad.setChecked(selected.getRotateDpad());
        checkboxLocalCursor.setChecked(selected.getUseLocalCursor());
        checkboxEnableSound.setChecked(selected.getEnableSound());
        textNickname.setText(selected.getNickname());
        spinnerGeometry.setSelection(selected.getRdpResType());
        resWidth.setText(Integer.toString(selected.getRdpWidth()));
        resHeight.setText(Integer.toString(selected.getRdpHeight()));
        setRemoteWidthAndHeight ();
        
        // Write out CA to file if it doesn't exist.
        String caCertData = selected.getCaCert();
        try {
            // If a cert has been set, write out a unique file containing the cert and save the path to that file to give to libspice.
            String filename = getFilesDir() + "/ca" + Integer.toString(selected.getCaCert().hashCode()) + ".pem";
            selected.setCaCertPath(filename);
            File file = new File(filename);
            if (!file.exists() && !caCertData.equals("")) {
                android.util.Log.e(TAG, filename);
                PrintWriter fout = new PrintWriter(filename);
                fout.println(selected.getCaCert().toString());
                fout.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        int selection = spinnerArray.indexOf(selected.getLayoutMap());
        if (selection < 0) {
            selection = spinnerArray.indexOf(Constants.DEFAULT_LAYOUT_MAP);
        }
        layoutMapSpinner.setSelection(selection);
    }
    
    /**
     * Returns the current ConnectionBean.
     */
    public ConnectionBean getCurrentConnection() {
        return selected;
    }
    
    protected void updateSelectedFromView() {
        if (selected == null) {
            return;
        }
        selected.setConnectionType(selectedConnType);
        selected.setAddress(ipText.getText().toString());
        
        String port = portText.getText().toString();
        if (!port.equals("")) {
            try {
                selected.setPort(Integer.parseInt(portText.getText().toString()));
            } catch (NumberFormatException nfe) { }
        } else {
            selected.setPort(-1);
        }
        
        String tlsport = tlsPort.getText().toString();
        if (!tlsport.equals("")) {
            try {
                selected.setTlsPort(Integer.parseInt(tlsPort.getText().toString()));
            } catch (NumberFormatException nfe) { }
        } else {
            selected.setTlsPort(-1);
        }
        
        try {
            selected.setSshPort(Integer.parseInt(sshPort.getText().toString()));
        } catch (NumberFormatException nfe) {
        }
        
        selected.setNickname(textNickname.getText().toString());
        selected.setSshServer(sshServer.getText().toString());
        selected.setSshUser(sshUser.getText().toString());

        selected.setKeepSshPassword(false);

        // If we are using an SSH key, then the ssh password box is used
        // for the key pass-phrase instead.
        selected.setUseSshPubKey(checkboxUseSshPubkey.isChecked());
        selected.setSshPassPhrase(sshPassphrase.getText().toString());
        selected.setSshPassword(sshPassword.getText().toString());
        selected.setRdpResType(spinnerGeometry.getSelectedItemPosition());
        try    {
            selected.setRdpWidth(Integer.parseInt(resWidth.getText().toString()));
            selected.setRdpHeight(Integer.parseInt(resHeight.getText().toString()));
        } catch (NumberFormatException nfe) {}
        selected.setPassword(passwordText.getText().toString());
        selected.setKeepPassword(checkboxKeepPassword.isChecked());
        selected.setUseDpadAsArrows(checkboxUseDpadAsArrows.isChecked());
        selected.setRotateDpad(checkboxRotateDpad.isChecked());
        selected.setUseLocalCursor(checkboxLocalCursor.isChecked());
        selected.setEnableSound(checkboxEnableSound.isChecked());
        
        TextView selection = null;
        if (layoutMapSpinner != null) {
            selection = (TextView) layoutMapSpinner.getSelectedView();
        }
        if (selection != null) {
            selected.setLayoutMap(selection.getText().toString());
        }
    }
    
    /**
     * Starts the activity which manages keys.
     */
    private void generatePubkey() {
        updateSelectedFromView();
        selected.saveAndWriteRecent(false);
        Intent intent = new Intent(this, GeneratePubkeyActivity.class);
        intent.putExtra("PrivateKey", selected.getSshPrivKey());
        startActivityForResult(intent, Constants.ACTIVITY_GEN_KEY);
    }
    
    /**
     * This function is used to retrieve data returned by activities started
     * with startActivityForResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case (Constants.ACTIVITY_GEN_KEY):
            if (resultCode == Activity.RESULT_OK) {
                Bundle b = data.getExtras();
                String privateKey = (String) b.get("PrivateKey");
                if (!privateKey.equals(selected.getSshPrivKey())
                        && privateKey.length() != 0)
                    Toast.makeText(
                            getBaseContext(),
                            "New key generated/imported successfully. Tap 'Generate/Export Key' "
                                    + " button to share, copy to clipboard, or export the public key now.",
                            Toast.LENGTH_LONG).show();
                selected.setSshPrivKey(privateKey);
                selected.setSshPubKey((String) b.get("PublicKey"));
                selected.saveAndWriteRecent(false);
            } else
                Log.i(TAG, "The user cancelled SSH key generation.");
            break;
        }
    }
    
    private List<String> listFiles(String dirFrom) throws IOException {
        Resources res = getResources();
        AssetManager am = res.getAssets();
        String fileList[] = am.list(dirFrom);

            if (fileList != null)
            {   
                for ( int i = 0;i<fileList.length;i++)
                {
                    Log.d("",fileList[i]); 
                }
            }
        return (List<String>)Arrays.asList(fileList);
    }
}
