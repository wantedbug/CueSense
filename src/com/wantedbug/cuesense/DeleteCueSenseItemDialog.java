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

/**
 * A dialog launched to delete a new CueItem 
 * @author vikasprabhu
 */
public class DeleteCueSenseItemDialog extends DialogFragment {
	/**
	 * Interface to handle deletion of a CueItem 
	 */
	public interface DeleteCueSenseItemListener {
		// Handle addition of new CueItem
        void onCueDeleted(int itemPosition);
    }
	
	DeleteCueSenseItemListener mListener;
	final int mItemPosition;
	
	public DeleteCueSenseItemDialog(int position) {
		mItemPosition = position;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if(!(activity instanceof DeleteCueSenseItemListener)) {
        	throw new RuntimeException("Activity must implement DeleteCueSenseItemListener interface!");
        }
        
        mListener = (DeleteCueSenseItemListener) activity;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.delete_cuesense_item_dialog, null);
        
        builder.setView(dialogView)
    	.setTitle(R.string.delete_cuesense_item_dialog_title)
    	.setPositiveButton(R.string.delete_cuesense_item_deletebutton, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onCueDeleted(mItemPosition);
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
