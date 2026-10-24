/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.SurfaceTexture;
import android.view.*;

/** Embedded video in a normal GPU view layer so rounded clipping includes the actual picture. */
final class RoundedVideoView extends TextureView implements TextureView.SurfaceTextureListener {
    private Surface output;
    private Runnable changed;
    RoundedVideoView(Context context){
        super(context);final float radius=12*getResources().getDisplayMetrics().density;
        setOpaque(false);
        setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View view,Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),radius);outline.setAlpha(1f);}});
        setClipToOutline(true);setSurfaceTextureListener(this);
    }
    Surface output(){return output;}
    void onOutputChanged(Runnable callback){changed=callback;if(output!=null)changed.run();}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int width,int height){output=new Surface(texture);if(changed!=null)changed.run();}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int width,int height){invalidateOutline();if(changed!=null)changed.run();}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){Surface previous=output;output=null;if(changed!=null)changed.run();if(previous!=null)previous.release();return true;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture){}
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);invalidateOutline();}
}
