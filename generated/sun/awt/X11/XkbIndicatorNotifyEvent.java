// This file is an automatically generated file, please do not edit this file, modify the WrapperGenerator.java file instead !

package sun.awt.X11;

import sun.misc.*;

import java.util.logging.*;
public class XkbIndicatorNotifyEvent extends XWrapperBase { 
	private Unsafe unsafe = XlibWrapper.unsafe; 
	private final boolean should_free_memory;
	public static int getSize() { return 56; }
	public int getDataSize() { return getSize(); }

	long pData;

	public long getPData() { return pData; }


	public XkbIndicatorNotifyEvent(long addr) {
		log.finest("Creating");
		pData=addr;
		should_free_memory = false;
	}


	public XkbIndicatorNotifyEvent() {
		log.finest("Creating");
		pData = unsafe.allocateMemory(getSize());
		should_free_memory = true;
	}


	public void dispose() {
		log.finest("Disposing");
		if (should_free_memory) {
			log.finest("freeing memory");
			unsafe.freeMemory(pData); 
	}
		}
	public int get_type() { log.finest("");return (Native.getInt(pData+0)); }
	public void set_type(int v) { log.finest(""); Native.putInt(pData+0, v); }
	public long get_serial() { log.finest("");return (Native.getULong(pData+8)); }
	public void set_serial(long v) { log.finest(""); Native.putULong(pData+8, v); }
	public boolean get_send_event() { log.finest("");return (Native.getBool(pData+16)); }
	public void set_send_event(boolean v) { log.finest(""); Native.putBool(pData+16, v); }
	public long get_display() { log.finest("");return (Native.getLong(pData+24)); }
	public void set_display(long v) { log.finest(""); Native.putLong(pData+24, v); }
	public long get_time() { log.finest("");return (Native.getULong(pData+32)); }
	public void set_time(long v) { log.finest(""); Native.putULong(pData+32, v); }
	public int get_xkb_type() { log.finest("");return (Native.getInt(pData+40)); }
	public void set_xkb_type(int v) { log.finest(""); Native.putInt(pData+40, v); }
	public int get_device() { log.finest("");return (Native.getInt(pData+44)); }
	public void set_device(int v) { log.finest(""); Native.putInt(pData+44, v); }
	public int get_changed() { log.finest("");return (Native.getInt(pData+48)); }
	public void set_changed(int v) { log.finest(""); Native.putInt(pData+48, v); }
	public int get_state() { log.finest("");return (Native.getInt(pData+52)); }
	public void set_state(int v) { log.finest(""); Native.putInt(pData+52, v); }


	String getName() {
		return "XkbIndicatorNotifyEvent"; 
	}


	String getFieldsAsString() {
		String ret="";

		ret += ""+"type = " + XlibWrapper.eventToString[get_type()] +", ";
		ret += ""+"serial = " + get_serial() +", ";
		ret += ""+"send_event = " + get_send_event() +", ";
		ret += ""+"display = " + get_display() +", ";
		ret += ""+"time = " + get_time() +", ";
		ret += ""+"xkb_type = " + get_xkb_type() +", ";
		ret += ""+"device = " + get_device() +", ";
		ret += ""+"changed = " + get_changed() +", ";
		ret += ""+"state = " + get_state() +", ";
		return ret;
	}


}



