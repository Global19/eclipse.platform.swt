package org.eclipse.swt.widgets;

/*
 * Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
 
import org.eclipse.swt.internal.carbon.OS;
import org.eclipse.swt.internal.carbon.Rect;
import org.eclipse.swt.internal.carbon.ControlButtonContentInfo;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TypedListener;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;

public class ToolItem extends Item {
	int handle, menuHandle, cIcon;
	ToolBar parent;
	Image hotImage, disabledImage;
	String toolTipText;
	Control control;

	static final int DEFAULT_WIDTH = 24;
	static final int DEFAULT_HEIGHT = 22;
	static final int DEFAULT_SEPARATOR_WIDTH = 8;

public ToolItem (ToolBar parent, int style) {
	super (parent, checkStyle (style));
	this.parent = parent;
	parent.createItem (this, parent.getItemCount ());
	parent.relayout ();
}

public ToolItem (ToolBar parent, int style, int index) {
	super (parent, checkStyle (style));
	this.parent = parent;
	parent.createItem (this, index);
	parent.relayout ();
}

public void addSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Selection,typedListener);
	addListener(SWT.DefaultSelection,typedListener);
}

static int checkStyle (int style) {
	return checkBits (style, SWT.PUSH, SWT.CHECK, SWT.RADIO, SWT.SEPARATOR, SWT.DROP_DOWN, 0);
}

protected void checkSubclass () {
	if (!isValidSubclass ()) error (SWT.ERROR_INVALID_SUBCLASS);
}

Point computeSize () {
	checkWidget();
	int width = 0, height = 0;
	if ((style & SWT.SEPARATOR) != 0) {
		if ((style & SWT.HORIZONTAL) != 0) {
			width = DEFAULT_WIDTH;
			height = DEFAULT_SEPARATOR_WIDTH;
		} else {
			width = DEFAULT_SEPARATOR_WIDTH;
			height = DEFAULT_HEIGHT;
		}
	} else {
		int space = 0;
		int stringWidth = 0, stringHeight = 0;
		if (text.length () != 0) {
			GC gc = new GC (parent);
			Point size = gc.stringExtent (text);
			stringWidth = size.x;
			stringHeight = size.y;
			gc.dispose ();
		}
		int imageWidth = 0, imageHeight = 0;
		if (image != null) {
			if (text.length () != 0) space = 2;
			Rectangle rect = image.getBounds ();
			imageWidth = rect.width;
			imageHeight = rect.height;
		}
		if ((parent.style & SWT.RIGHT) != 0) {
			space = 0;
			width = stringWidth + imageWidth;
			height = Math.max (stringHeight, imageHeight);
		} else {
			width = Math.max (stringWidth, imageWidth);
			height = stringHeight + imageHeight;
		}
		int inset = (style & SWT.DROP_DOWN) != 0 ? 7 : 2;
		width += space + inset * 2;
		height += space + inset * 2;
	}
	return new Point (width, height);
}

void createHandle () {
	int [] outControl = new int [1];
	int window = OS.GetControlOwner (parent.handle);
	Rect rect = new Rect ();
	OS.SetRect (rect, (short) 0, (short) 0, (short) DEFAULT_WIDTH, (short) DEFAULT_HEIGHT);
	
	if ((style & (SWT.CHECK | SWT.RADIO)) != 0) {
		OS.CreateBevelButtonControl (window, rect, 0, (short)OS.kControlBevelButtonNormalBevel, (short)OS.kControlBehaviorToggles, 0, (short)0, (short)0, (short)0, outControl);
		if (outControl [0] == 0) error (SWT.ERROR_NO_HANDLES);
		handle = outControl [0];
	}
	
	if ((style & SWT.DROP_DOWN) != 0) {
		OS.CreateBevelButtonControl (window, rect, 0, (short)OS.kControlBevelButtonNormalBevel, (short)OS.kControlBehaviorPushbutton, 0, (short)0, (short)0, (short)0, outControl);
		if (outControl [0] == 0) error (SWT.ERROR_NO_HANDLES);
		handle = outControl [0];
		int[] menuRef = new int [1];
		OS.CreateNewMenu ((short)0, 0, menuRef);
		if (menuRef [0] == 0) error (SWT.ERROR_NO_HANDLES);
		menuHandle = menuRef [0];
		OS.SetControlData (handle, OS.kControlEntireControl, OS.kControlBevelButtonMenuRefTag, 4, new int [] {menuHandle});	
		Display display = getDisplay ();
		int menuProc = display.menuProc;
		int [] mask = new int [] {
			OS.kEventClassMenu, OS.kEventMenuClosed,
			OS.kEventClassMenu, OS.kEventMenuOpening,
		};
		int menuTarget = OS.GetMenuEventTarget (menuHandle);
		OS.InstallEventHandler (menuTarget, menuProc, mask.length / 2, mask, parent.handle, null);
	}
	
	if ((style & SWT.PUSH) != 0) {
		OS.CreateBevelButtonControl(window, rect, 0, (short)2, (short)OS.kControlBehaviorPushbutton, 0, (short)0, (short)0, (short)0, outControl);
		if (outControl [0] == 0) error (SWT.ERROR_NO_HANDLES);
		handle = outControl [0];
	}

	if ((style & SWT.SEPARATOR) != 0) {
		if ((parent.style & SWT.HORIZONTAL) != 0) {
			OS.SetRect (rect, (short) 0, (short) 0, (short) DEFAULT_SEPARATOR_WIDTH, (short) DEFAULT_HEIGHT);
		} else {
			OS.SetRect (rect, (short) 0, (short) 0, (short) DEFAULT_WIDTH, (short) DEFAULT_SEPARATOR_WIDTH);
		}
		OS.CreateUserPaneControl (window, rect, 0, outControl);
		if (outControl [0] == 0) error (SWT.ERROR_NO_HANDLES);
		handle = outControl [0];
	} else {
		int placement = (parent.style & SWT.RIGHT) != 0 ? OS.kControlBevelButtonPlaceToRightOfGraphic :  OS.kControlBevelButtonPlaceBelowGraphic;
		OS.SetControlData (handle, OS.kControlEntireControl, OS.kControlBevelButtonTextPlaceTag, 2, new short [] {(short) placement});
	}
	parent.relayout ();
}

void createWidget () {
	super.createWidget ();
	setZOrder ();
	toolTipText = "";
}

void deregister () {
	super.deregister ();
	WidgetTable.remove (handle);
}

void destroyWidget () {
	int theControl = handle, theMenu = menuHandle;
	releaseHandle ();
	if (theControl != 0) {
		OS.DisposeControl (theControl);
	}
	if (theMenu != 0) {
//		OS.DeleteMenu (OS.GetMenuID (theMenu));
		OS.DisposeMenu (theMenu);
	}
}

public void dispose () {
	if (isDisposed()) return;
	ToolBar parent = this.parent;
	super.dispose ();
	parent.relayout ();
}

public Rectangle getBounds () {
	checkWidget();
	Rect bounds= new Rect();
	OS.GetControlBounds(handle, bounds);
	return new Rectangle(bounds.left, bounds.top, bounds.right-bounds.left, bounds.bottom-bounds.top);
}

public Control getControl () {
	checkWidget();
	return control;
}

public Image getDisabledImage () {
	checkWidget();
	return disabledImage;
}

public boolean getEnabled () {
	checkWidget();
	return OS.IsControlEnabled(handle);
}

public Display getDisplay () {
	Composite parent = this.parent;
	if (parent == null) error (SWT.ERROR_WIDGET_DISPOSED);
	return parent.getDisplay ();
}

public Image getHotImage () {
	checkWidget();
	return hotImage;
}

public ToolBar getParent () {
	checkWidget();
	return parent;
}

public boolean getSelection () {
	checkWidget();
	if ((style & (SWT.CHECK | SWT.RADIO)) == 0) return false;
    return OS.GetControl32BitValue (handle) != 0;
}

public String getToolTipText () {
	checkWidget();
	return toolTipText;
}

public int getWidth () {
	checkWidget();
	Rect bounds= new Rect();
	OS.GetControlBounds(handle, bounds);
	return bounds.right - bounds.left;
}

void hookEvents () {
	super.hookEvents ();
	if ((style & SWT.SEPARATOR) != 0) return;
	Display display = getDisplay ();
	int controlProc = display.controlProc;
	int [] mask = new int [] {
		OS.kEventClassControl, OS.kEventControlDraw,
		OS.kEventClassControl, OS.kEventControlHit,
	};
	int controlTarget = OS.GetControlEventTarget (handle);
	OS.InstallEventHandler (controlTarget, controlProc, mask.length / 2, mask, handle, null);	
}

public boolean isEnabled () {
	checkWidget();
	return getEnabled () && parent.isEnabled ();
}

int kEventControlDraw (int nextHandler, int theEvent, int userData) {
	int clipRgn = getClipping (handle);
	int oldRgn = OS.NewRgn ();
	OS.GetClip (oldRgn);
	OS.SetClip (clipRgn);
	int result = OS.CallNextEventHandler (nextHandler, theEvent);
	OS.SetClip (oldRgn);
	OS.DisposeRgn (clipRgn);
	OS.DisposeRgn (oldRgn);
	return result;
}

int kEventControlHit (int nextHandler, int theEvent, int userData) {
	int result = super.kEventControlHit (nextHandler, theEvent, userData);
	if (result == OS.noErr) return result;
	if ((style & SWT.RADIO) != 0) {
		if ((parent.getStyle () & SWT.NO_RADIO_GROUP) == 0) {
			selectRadio ();
		}
	}
	postEvent (SWT.Selection);
	return OS.eventNotHandledErr;
}

int kEventMenuOpening (int nextHandler, int theEvent, int userData) {
	//HIDE THE MENU
	OS.SetControl32BitValue (handle, 0);
	return OS.eventNotHandledErr;
}

void register () {
	super.register ();
	WidgetTable.put (handle, this);
}

void releaseChild () {
	super.releaseChild ();
	parent.destroyItem (this);
}

void releaseHandle () {
	super.releaseHandle ();
	handle = 0;
}

void releaseWidget () {
	super.releaseWidget ();
	if (cIcon != 0) destroyCIcon (cIcon);
	cIcon = 0;
	parent = null;
	control = null;
	toolTipText = null;
	image = disabledImage = hotImage = null; 
}

public void removeSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook(SWT.Selection, listener);
	eventTable.unhook(SWT.DefaultSelection,listener);	
}

void selectRadio () {
	int index = 0;
	ToolItem [] items = parent.getItems ();
	while (index < items.length && items [index] != this) index++;
	int i = index - 1;
	while (i >= 0 && items [i].setRadioSelection (false)) --i;
	int j = index + 1;
	while (j < items.length && items [j].setRadioSelection (false)) j++;
	setSelection (true);
}

void setBounds (int x, int y, int width, int height) {
	if (control != null) control.setBounds (x, y, width, height);
	Rect rect = new Rect ();
	OS.SetRect (rect, (short) x, (short) y, (short)(x + width), (short)(y + height));
	OS.SetControlBounds (handle, rect);
}

public void setControl (Control control) {
	checkWidget();
	if (control != null) {
		if (control.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
		if (control.parent != parent) error (SWT.ERROR_INVALID_PARENT);
	}
	if ((style & SWT.SEPARATOR) == 0) return;
	this.control = control;
	if (control != null && !control.isDisposed ()) {
		control.setBounds (getBounds ());
	}
}

public void setEnabled (boolean enabled) {
	checkWidget();
	if (enabled) {
		OS.EnableControl (handle);
	} else {
		OS.DisableControl (handle);
	}
}

public void setDisabledImage (Image image) {
	checkWidget();
	if (image != null && image.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
	if ((style & SWT.SEPARATOR) != 0) return;
	disabledImage = image;
	updateImage ();
}

public void setHotImage (Image image) {
	checkWidget();
	if (image != null && image.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
	if ((style & SWT.SEPARATOR) != 0) return;
	hotImage = image;
	updateImage ();
}

public void setImage (Image image) {
	checkWidget();
	if (image != null && image.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
	if ((style & SWT.SEPARATOR) != 0) return;
	super.setImage (image);
	updateImage ();
}

boolean setRadioSelection (boolean value) {
	if ((style & SWT.RADIO) == 0) return false;
	if (getSelection () != value) {
		setSelection (value);
		postEvent (SWT.Selection);
	}
	return true;
}

public void setSelection (boolean selected) {
	checkWidget();
	if ((style & (SWT.CHECK | SWT.RADIO)) == 0) return;
	OS.SetControl32BitValue (handle, selected ? 1 : 0);
}

void setSize (int width, int height, boolean layout) {
	Rect rect = new Rect();
	OS.GetControlBounds (handle, rect);
	if ((rect.right - rect.left) != width || (rect.bottom - rect.top) != height) {
		OS.SizeControl (handle, (short) width, (short) height);
		if (layout) parent.relayout ();
	}
}

public void setText (String string) {
	checkWidget();
	if (string == null) error (SWT.ERROR_NULL_ARGUMENT);
	if ((style & SWT.SEPARATOR) != 0) return;
	super.setText (string);
	updateText ();
}

public void setToolTipText (String string) {
	checkWidget();
	toolTipText = string;
}

public void setWidth (int width) {
	checkWidget();
	if ((style & SWT.SEPARATOR) == 0) return;
	if (width < 0) return;
	Rect rect = new Rect ();
	OS.GetControlBounds (handle, rect);
	setSize (width, rect.bottom - rect.top, true);
	if (control != null && !control.isDisposed ()) {
		control.setBounds (getBounds ());
	}
}

void setZOrder () {
	OS.HIViewAddSubview (parent.handle, handle);
}

void updateImage () {
	if (cIcon != 0) destroyCIcon (cIcon);
	cIcon = 0;
	Image image = null;
	if (hotImage != null) {
		image = hotImage;
	} else {
		if (this.image != null) {
			image = this.image;
		} else {
			image = disabledImage;
		}
	}
	if (image == null) return;
	cIcon = createCIcon (image);
	ControlButtonContentInfo inContent = new ControlButtonContentInfo ();
	inContent.contentType = (short)OS.kControlContentCIconHandle;
	inContent.iconRef = cIcon;
	OS.SetBevelButtonContentInfo (handle, inContent);
	OS.HIViewSetNeedsDisplay (handle, true);
	Point size = computeSize ();
	setSize (size.x, size.y, true);
}

void updateText () {
	char [] buffer = new char [text.length ()];
	text.getChars (0, buffer.length, buffer, 0);
	int i=0, j=0;
	while (i < buffer.length) {
		if ((buffer [j++] = buffer [i++]) == Mnemonic) {
			if (i == buffer.length) {continue;}
			if (buffer [i] == Mnemonic) {i++; continue;}
			j--;
		}
	}
	int ptr = OS.CFStringCreateWithCharacters (OS.kCFAllocatorDefault, buffer, j);
	if (ptr == 0) error (SWT.ERROR_CANNOT_SET_TEXT);
	OS.SetControlTitleWithCFString (handle, ptr);
	OS.CFRelease (ptr);
	Point size = computeSize ();
	setSize (size.x, size.y, true);
}
}
