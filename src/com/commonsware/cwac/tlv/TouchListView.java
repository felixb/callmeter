/*
 * Copyright (c) 2010 CommonsWare, LLC Portions Copyright (C) 2008 The Android
 * Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.commonsware.cwac.tlv;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import de.ub0r.android.callmeter.R;

/**
 * TouchListView is 99% the Android open source code for TouchInterceptor.
 * TouchListView also allows the widget to be configured from an XML layout
 * file, replacing some hard-wired values that TouchInterceptor uses.
 * 
 * @author commonsguy
 */
public class TouchListView extends ListView {
	private ImageView mDragView;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWindowParams;
	private int mDragPos; // which item is being dragged
	private int mFirstDragPos; // where was the dragged item originally
	private int mDragPoint; // at what offset inside the item did the user grab
	// it
	private int mCoordOffset; // the difference between screen coordinates and
	// coordinates in this view
	private DragListener mDragListener;
	private DropListener mDropListener;
	private RemoveListener mRemoveListener;
	private int mUpperBound;
	private int mLowerBound;
	private int mHeight;
	private GestureDetector mGestureDetector;
	public static final int FLING = 0;
	public static final int SLIDE_RIGHT = 1;
	public static final int SLIDE_LEFT = 2;
	private int mRemoveMode = -1;
	private Rect mTempRect = new Rect();
	private Bitmap mDragBitmap;
	private final int mTouchSlop;
	private int mItemHeightNormal = -1;
	private int mItemHeightExpanded = -1;
	private int grabberId = -1;
	private int dragndropBackgroundColor = 0x00000000;

	public TouchListView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TouchListView(final Context context, final AttributeSet attrs,
			final int defStyle) {
		super(context, attrs, defStyle);

		this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		if (attrs != null) {
			TypedArray a = this.getContext().obtainStyledAttributes(attrs,
					R.styleable.TouchListView, 0, 0);

			this.mItemHeightNormal = a.getDimensionPixelSize(
					R.styleable.TouchListView_normal_height, 0);
			this.mItemHeightExpanded = a.getDimensionPixelSize(
					R.styleable.TouchListView_expanded_height,
					this.mItemHeightNormal);
			this.grabberId = a.getResourceId(R.styleable.TouchListView_grabber,
					-1);
			this.dragndropBackgroundColor = a.getColor(
					R.styleable.TouchListView_dragndrop_background, 0x00000000);
			this.mRemoveMode = a.getInt(R.styleable.TouchListView_remove_mode,
					-1);

			a.recycle();
		}
	}

	@Override
	public boolean onInterceptTouchEvent(final MotionEvent ev) {
		if (this.mRemoveListener != null && this.mGestureDetector == null) {
			if (this.mRemoveMode == FLING) {
				this.mGestureDetector = new GestureDetector(this.getContext(),
						new SimpleOnGestureListener() {
							@Override
							public boolean onFling(final MotionEvent e1,
									final MotionEvent e2,
									final float velocityX, final float velocityY) {
								if (TouchListView.this.mDragView != null) {
									if (velocityX > 1000) {
										Rect r = TouchListView.this.mTempRect;
										TouchListView.this.mDragView
												.getDrawingRect(r);
										if (e2.getX() > r.right * 2 / 3) {
											// fast fling right with release
											// near the right edge of the screen
											TouchListView.this.stopDragging();
											TouchListView.this.mRemoveListener
													.remove(TouchListView.this.mFirstDragPos);
											TouchListView.this
													.unExpandViews(true);
										}
									}
									// flinging while dragging should have no
									// effect
									return true;
								}
								return false;
							}
						});
			}
		}
		if (this.mDragListener != null || this.mDropListener != null) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				int itemnum = this.pointToPosition(x, y);
				if (itemnum == AdapterView.INVALID_POSITION) {
					break;
				}
				ViewGroup item = (ViewGroup) this.getChildAt(itemnum
						- this.getFirstVisiblePosition());
				this.mDragPoint = y - item.getTop();
				this.mCoordOffset = ((int) ev.getRawY()) - y;
				View dragger = item.findViewById(this.grabberId);
				Rect r = this.mTempRect;
				// dragger.getDrawingRect(r);

				r.left = dragger.getLeft();
				r.right = dragger.getRight();
				r.top = dragger.getTop();
				r.bottom = dragger.getBottom();

				if ((r.left < x) && (x < r.right)) {
					item.setDrawingCacheEnabled(true);
					// Create a copy of the drawing cache so that it does not
					// get recycled
					// by the framework when the list tries to clean up memory
					Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
					this.startDragging(bitmap, y);
					this.mDragPos = itemnum;
					this.mFirstDragPos = this.mDragPos;
					this.mHeight = this.getHeight();
					int touchSlop = this.mTouchSlop;
					this.mUpperBound = Math
							.min(y - touchSlop, this.mHeight / 3);
					this.mLowerBound = Math.max(y + touchSlop,
							this.mHeight * 2 / 3);
					return false;
				}
				this.mDragView = null;
				break;
			}
		}
		return super.onInterceptTouchEvent(ev);
	}

	/*
	 * pointToPosition() doesn't consider invisible views, but we need to, so
	 * implement a slightly different version.
	 */
	private int myPointToPosition(final int x, final int y) {
		Rect frame = this.mTempRect;
		final int count = this.getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			final View child = this.getChildAt(i);
			child.getHitRect(frame);
			if (frame.contains(x, y)) {
				return this.getFirstVisiblePosition() + i;
			}
		}
		return INVALID_POSITION;
	}

	private int getItemForPosition(final int y) {
		int adjustedy = y - this.mDragPoint - 32;
		int pos = this.myPointToPosition(0, adjustedy);
		if (pos >= 0) {
			if (pos <= this.mFirstDragPos) {
				pos += 1;
			}
		} else if (adjustedy < 0) {
			pos = 0;
		}
		return pos;
	}

	private void adjustScrollBounds(final int y) {
		if (y >= this.mHeight / 3) {
			this.mUpperBound = this.mHeight / 3;
		}
		if (y <= this.mHeight * 2 / 3) {
			this.mLowerBound = this.mHeight * 2 / 3;
		}
	}

	/*
	 * Restore size and visibility for all listitems
	 */
	private void unExpandViews(final boolean deletion) {
		for (int i = 0;; i++) {
			View v = this.getChildAt(i);
			if (v == null) {
				if (deletion) {
					// HACK force update of mItemCount
					int position = this.getFirstVisiblePosition();
					int y = this.getChildAt(0).getTop();
					this.setAdapter(this.getAdapter());
					this.setSelectionFromTop(position, y);
					// end hack
				}
				this.layoutChildren(); // force children to be recreated where
				// needed
				v = this.getChildAt(i);
				if (v == null) {
					break;
				}
			}
			ViewGroup.LayoutParams params = v.getLayoutParams();
			params.height = this.mItemHeightNormal;
			v.setLayoutParams(params);
			v.setVisibility(View.VISIBLE);
		}
	}

	/*
	 * Adjust visibility and size to make it appear as though an item is being
	 * dragged around and other items are making room for it: If dropping the
	 * item would result in it still being in the same place, then make the
	 * dragged listitem's size normal, but make the item invisible. Otherwise,
	 * if the dragged listitem is still on screen, make it as small as possible
	 * and expand the item below the insert point. If the dragged item is not on
	 * screen, only expand the item below the current insertpoint.
	 */
	private void doExpansion() {
		int childnum = this.mDragPos - this.getFirstVisiblePosition();
		if (this.mDragPos > this.mFirstDragPos) {
			childnum++;
		}

		View first = this.getChildAt(this.mFirstDragPos
				- this.getFirstVisiblePosition());

		for (int i = 0;; i++) {
			View vv = this.getChildAt(i);
			if (vv == null) {
				break;
			}
			int height = this.mItemHeightNormal;
			int visibility = View.VISIBLE;
			if (vv.equals(first)) {
				// processing the item that is being dragged
				if (this.mDragPos == this.mFirstDragPos) {
					// hovering over the original location
					visibility = View.INVISIBLE;
				} else {
					// not hovering over it
					height = 1;
				}
			} else if (i == childnum) {
				if (this.mDragPos < this.getCount() - 1) {
					height = this.mItemHeightExpanded;
				}
			}
			ViewGroup.LayoutParams params = vv.getLayoutParams();
			params.height = height;
			vv.setLayoutParams(params);
			vv.setVisibility(visibility);
		}
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		if (this.mGestureDetector != null) {
			this.mGestureDetector.onTouchEvent(ev);
		}
		if ((this.mDragListener != null || this.mDropListener != null)
				&& this.mDragView != null) {
			int action = ev.getAction();
			switch (action) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				Rect r = this.mTempRect;
				this.mDragView.getDrawingRect(r);
				this.stopDragging();

				if (this.mRemoveMode == SLIDE_RIGHT
						&& ev.getX() > r.left + (r.width() * 3 / 4)) {
					if (this.mRemoveListener != null) {
						this.mRemoveListener.remove(this.mFirstDragPos);
					}
					this.unExpandViews(true);
				} else if (this.mRemoveMode == SLIDE_LEFT
						&& ev.getX() < r.left + (r.width() / 4)) {
					if (this.mRemoveListener != null) {
						this.mRemoveListener.remove(this.mFirstDragPos);
					}
					this.unExpandViews(true);
				} else {
					if (this.mDropListener != null && this.mDragPos >= 0
							&& this.mDragPos < this.getCount()) {
						this.mDropListener.drop(this.mFirstDragPos,
								this.mDragPos);
					}
					this.unExpandViews(false);
				}
				break;

			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				this.dragView(x, y);
				int itemnum = this.getItemForPosition(y);
				if (itemnum >= 0) {
					if (action == MotionEvent.ACTION_DOWN
							|| itemnum != this.mDragPos) {
						if (this.mDragListener != null) {
							this.mDragListener.drag(this.mDragPos, itemnum);
						}
						this.mDragPos = itemnum;
						this.doExpansion();
					}
					int speed = 0;
					this.adjustScrollBounds(y);
					if (y > this.mLowerBound) {
						// scroll the list up a bit
						speed = y > (this.mHeight + this.mLowerBound) / 2 ? 16
								: 4;
					} else if (y < this.mUpperBound) {
						// scroll the list down a bit
						speed = y < this.mUpperBound / 2 ? -16 : -4;
					}
					if (speed != 0) {
						int ref = this.pointToPosition(0, this.mHeight / 2);
						if (ref == AdapterView.INVALID_POSITION) {
							// we hit a divider or an invisible view, check
							// somewhere else
							ref = this.pointToPosition(0, this.mHeight / 2
									+ this.getDividerHeight() + 64);
						}
						View v = this.getChildAt(ref
								- this.getFirstVisiblePosition());
						if (v != null) {
							int pos = v.getTop();
							this.setSelectionFromTop(ref, pos - speed);
						}
					}
				}
				break;
			}
			return true;
		}
		return super.onTouchEvent(ev);
	}

	private void startDragging(final Bitmap bm, final int y) {
		this.stopDragging();

		this.mWindowParams = new WindowManager.LayoutParams();
		this.mWindowParams.gravity = Gravity.TOP;
		this.mWindowParams.x = 0;
		this.mWindowParams.y = y - this.mDragPoint + this.mCoordOffset;

		this.mWindowParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
		this.mWindowParams.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
		this.mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		this.mWindowParams.format = PixelFormat.TRANSLUCENT;
		this.mWindowParams.windowAnimations = 0;

		ImageView v = new ImageView(this.getContext());
		// int backGroundColor =
		// getContext().getResources().getColor(R.color.dragndrop_background);
		v.setBackgroundColor(this.dragndropBackgroundColor);
		v.setImageBitmap(bm);
		this.mDragBitmap = bm;

		this.mWindowManager = (WindowManager) this.getContext()
				.getSystemService("window");
		this.mWindowManager.addView(v, this.mWindowParams);
		this.mDragView = v;
	}

	private void dragView(final int x, final int y) {
		float alpha = 1.0f;
		int width = this.mDragView.getWidth();

		if (this.mRemoveMode == SLIDE_RIGHT) {
			if (x > width / 2) {
				alpha = ((float) (width - x)) / (width / 2);
			}
			this.mWindowParams.alpha = alpha;
		} else if (this.mRemoveMode == SLIDE_LEFT) {
			if (x < width / 2) {
				alpha = ((float) x) / (width / 2);
			}
			this.mWindowParams.alpha = alpha;
		}
		this.mWindowParams.y = y - this.mDragPoint + this.mCoordOffset;
		this.mWindowManager
				.updateViewLayout(this.mDragView, this.mWindowParams);
	}

	private void stopDragging() {
		if (this.mDragView != null) {
			WindowManager wm = (WindowManager) this.getContext()
					.getSystemService("window");
			wm.removeView(this.mDragView);
			this.mDragView.setImageDrawable(null);
			this.mDragView = null;
		}
		if (this.mDragBitmap != null) {
			this.mDragBitmap.recycle();
			this.mDragBitmap = null;
		}
	}

	public void setDragListener(final DragListener l) {
		this.mDragListener = l;
	}

	public void setDropListener(final DropListener l) {
		this.mDropListener = l;
	}

	public void setRemoveListener(final RemoveListener l) {
		this.mRemoveListener = l;
	}

	public interface DragListener {
		void drag(int from, int to);
	}

	public interface DropListener {
		void drop(int from, int to);
	}

	public interface RemoveListener {
		void remove(int which);
	}
}
