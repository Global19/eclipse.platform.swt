/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.dnd;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.TVHITTESTINFO;
import org.eclipse.swt.internal.win32.TVITEM;
import org.eclipse.swt.widgets.Tree;

class TreeDragUnderEffect extends DragUnderEffect {
	private Tree tree;
	private int dropIndex;
	private int scrollIndex;	
	private long scrollBeginTime;
	private int expandIndex;
	private long expandBeginTime;
	
	private static final int SCROLL_HYSTERESIS = 500; // milli seconds
	private static final int EXPAND_HYSTERESIS = 600; // milli seconds
	

TreeDragUnderEffect(Tree tree) {
	this.tree = tree;
}

private int checkEffect(int effect) {
	// Some effects are mutually exclusive.  Make sure that only one of the mutually exclusive effects has been specified.
	if ((effect & DND.FEEDBACK_SELECT) != 0) effect = effect & ~DND.FEEDBACK_INSERT_AFTER & ~DND.FEEDBACK_INSERT_BEFORE;
	if ((effect & DND.FEEDBACK_INSERT_BEFORE) != 0) effect = effect & ~DND.FEEDBACK_INSERT_AFTER;
	return effect;
}

void show(int effect, int x, int y) {
	effect = checkEffect(effect);
	int handle = tree.handle;
	Point coordinates = new Point(x, y);
	coordinates = tree.toControl(coordinates);
	TVHITTESTINFO lpht = new TVHITTESTINFO ();
	lpht.x = coordinates.x;
	lpht.y = coordinates.y;
	OS.SendMessage (handle, OS.TVM_HITTEST, 0, lpht);
	int hItem = lpht.hItem;
	if ((effect & DND.FEEDBACK_SCROLL) == 0) {
		scrollBeginTime = 0;
		scrollIndex = -1;
	} else {
		if (hItem != -1 && scrollIndex == hItem && scrollBeginTime != 0) {
			if (System.currentTimeMillis() >= scrollBeginTime) {
				int topItem = OS.SendMessage (handle, OS.TVM_GETNEXTITEM, OS.TVGN_FIRSTVISIBLE, 0);
				int nextItem = OS.SendMessage (handle, OS.TVM_GETNEXTITEM, hItem == topItem ? OS.TVGN_PREVIOUSVISIBLE : OS.TVGN_NEXTVISIBLE, hItem);
				OS.SendMessage (handle, OS.TVM_ENSUREVISIBLE, 0, nextItem);
				scrollBeginTime = 0;
				scrollIndex = -1;
			}
		} else {
			scrollBeginTime = System.currentTimeMillis() + SCROLL_HYSTERESIS;
			scrollIndex = hItem;
		}
	}
	if ((effect & DND.FEEDBACK_EXPAND) == 0) {
		expandBeginTime = 0;
		expandIndex = -1;
	} else {
		if (hItem != -1 && expandIndex == hItem && expandBeginTime != 0) {
			if (System.currentTimeMillis() >= expandBeginTime) {
				OS.SendMessage (handle, OS.TVM_EXPAND, OS.TVE_EXPAND, hItem);
				expandBeginTime = 0;
				expandIndex = -1;
			}
		} else {
			expandBeginTime = System.currentTimeMillis() + EXPAND_HYSTERESIS;
			expandIndex = hItem;
		}
	}
	if (dropIndex != -1 && (dropIndex != hItem || (effect & DND.FEEDBACK_SELECT) == 0)) {
		TVITEM tvItem = new TVITEM ();
		tvItem.hItem = dropIndex;
		tvItem.mask = OS.TVIF_STATE;
		tvItem.stateMask = OS.TVIS_DROPHILITED;
		tvItem.state = 0;
		OS.SendMessage (handle, OS.TVM_SETITEM, 0, tvItem);
		dropIndex = -1;
	}
	if (hItem != -1 && hItem != dropIndex && (effect & DND.FEEDBACK_SELECT) != 0) {
		TVITEM tvItem = new TVITEM ();
		tvItem.hItem = hItem;
		tvItem.mask = OS.TVIF_STATE;
		tvItem.stateMask = OS.TVIS_DROPHILITED;
		tvItem.state = OS.TVIS_DROPHILITED;
		OS.SendMessage (handle, OS.TVM_SETITEM, 0, tvItem);
		dropIndex = hItem;
	}
	if ((effect & DND.FEEDBACK_INSERT_BEFORE) != 0 || (effect & DND.FEEDBACK_INSERT_AFTER) != 0) {
		boolean before = (effect & DND.FEEDBACK_INSERT_BEFORE) != 0;
		OS.SendMessage (handle, OS.TVM_SETINSERTMARK, (before) ? 0 : 1, hItem);
	} else {
		OS.SendMessage (handle, OS.TVM_SETINSERTMARK, 0, 0);
	}
	return;
}
}