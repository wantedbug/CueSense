/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * A dialog launched to add a new CueItem 
 * @author vikasprabhu
 */
public class NewCueSenseItemDialog extends DialogFragment {
	/**
	 * Interface to handle addition of new CueItem 
	 */
	public interface NewCueSenseItemListener {
		// Handle addition of new CueItem
        void onCueAdded(String itemData);
    }
	
	NewCueSenseItemListener mListener;
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if(!(activity instanceof NewCueSenseItemListener)) {
        	throw new RuntimeException("Activity must implement NewCueSenseItemListener interface!");
        }
        
        mListener = (NewCueSenseItemListener) activity;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.new_cuesense_item_dialog, null);
        
        builder.setView(dialogView)
        	.setTitle(R.string.new_cuesense_item_description)
        	.setPositiveButton(R.string.new_cuesense_item_addbutton, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					TextView itemData = (TextView) dialogView.findViewById(R.id.newCueSenseItemData);
					mListener.onCueAdded(itemData.getText().toString());
				}
			})
			.setNegativeButton(R.string.new_cuesense_item_cancelbutton, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Do nothing
				}
			});
        return builder.create();
	}
}
