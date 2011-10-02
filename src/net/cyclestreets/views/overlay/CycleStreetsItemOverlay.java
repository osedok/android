package net.cyclestreets.views.overlay;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import net.cyclestreets.util.Brush;

public abstract class CycleStreetsItemOverlay<T extends OverlayItem> 
          extends ItemizedOverlay<T>
          implements MapListener
{
	/////////////////////////////////////////////////////
	/////////////////////////////////////////////////////
	private final MapView mapView_;
	private int zoomLevel_;
	private boolean loading_;
	
	private final int offset_;
	private final float radius_;
	private final Paint textBrush_;
	
	static private final String LOADING = "Loading ...";
	
	public CycleStreetsItemOverlay(final Context context,
							                   final MapView mapView,
							                   final OnItemGestureListener<T> listener)
	{
		super(context, 
		      new ArrayList<T>(), 
		      listener);
		
		mapView_ = mapView;
		zoomLevel_ = mapView_.getZoomLevel();
		loading_ = false;
		
		offset_ = OverlayHelper.offset(context);
		radius_ = OverlayHelper.cornerRadius(context);
		textBrush_ = Brush.createTextBrush(offset_);

		mapView_.setMapListener(new DelayedMapListener(this));
	} // PhotoItemOverlay

	@Override
	protected void draw(final Canvas canvas, final MapView mapView, final boolean shadow) 
	{
		super.draw(canvas, mapView, shadow);
		
		if(!loading_)
			return;
		
		final Rect bounds = new Rect();
		textBrush_.getTextBounds(LOADING, 0, LOADING.length(), bounds);

		int width = bounds.width() + (offset_ * 2);
		final Rect screen = canvas.getClipBounds();
		screen.left = screen.centerX() - (width/2); 
		screen.top += offset_* 2;
		screen.right = screen.left + width;
		screen.bottom = screen.top + bounds.height() + (offset_ * 2);
		  
		if(!OverlayHelper.drawRoundRect(canvas, screen, radius_, Brush.Grey))
		  return;
		canvas.drawText(LOADING, screen.centerX(), screen.centerY() + bounds.bottom, textBrush_);
	} // drawButtons
	
	@Override
	public boolean onScroll(final ScrollEvent event) 
	{
		refreshItems();
		return true;
	} // onScroll
	
	@Override
	public boolean onZoom(final ZoomEvent event) 
	{
		if(event.getZoomLevel() < zoomLevel_)
			items().clear();
		zoomLevel_ = event.getZoomLevel();
		refreshItems();
		return true;
	} // onZoom

	private void refreshItems() 
	{		
		final GeoPoint centre = mapView_.getMapCenter();
    final int zoom = mapView_.getZoomLevel();
    final BoundingBoxE6 bounds = mapView_.getBoundingBox();
		
		fetchItemsInBackground(centre, zoom, bounds);

		loading_ = true;
		mapView_.postInvalidate();
	} // refreshPhotos
	
	protected abstract void fetchItemsInBackground(final GeoPoint mapCentre,
	                                               final int zoom,
	                                               final BoundingBoxE6 boundingBox);
	
	protected void setItems(final List<T> items)
	{
		for(final T item : items)
			if(!items().contains(item))
				items().add(item);
		if(items().size() > 500)  // arbitrary figure
			items().remove(items().subList(0, 100));
		loading_ = false;
		mapView_.postInvalidate();
	} // setItems
} // class CycleStreetsItemOverlay
