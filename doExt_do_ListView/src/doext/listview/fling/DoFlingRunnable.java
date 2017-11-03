package doext.listview.fling;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.widget.AbsListView;

public class DoFlingRunnable {
	
	public void stopScroll(AbsListView view){
	    try{
	        Field field = android.widget.AbsListView.class.getDeclaredField("mFlingRunnable");
	        field.setAccessible(true);
	        Object flingRunnable = field.get(view);
	        if (flingRunnable != null){
	            Method method = Class.forName("android.widget.AbsListView$FlingRunnable").getDeclaredMethod("endFling");
	            method.setAccessible(true);
	            method.invoke(flingRunnable);
	        }
	    }
	    catch (Exception e) {}
	}
}
