/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.SurfaceTexture;
import android.view.*;

/** Rounded GPU video layer. An optional Activity-owned output survives section rebuilds. */
final class RoundedVideoView extends TextureView implements TextureView.SurfaceTextureListener {
    /** One producer target, transferred only after its previous TextureView has detached.
     * No static cache, service/Activity retention, decoder restart or position correction.
     */
    static final class RetainedOutput {
        SurfaceTexture texture;
        Surface output;
        RoundedVideoView owner;
        boolean retaining,detached;
        void setRetaining(boolean value){
            retaining=value;
            // With no consumer (Settings/background/fullscreen), never leave a retained
            // producer target waiting on a full, unconsumed BufferQueue.
            if(!value&&detached){
                Surface oldOutput=output;SurfaceTexture oldTexture=texture;clear();
                if(oldOutput!=null)oldOutput.release();if(oldTexture!=null)oldTexture.release();
            }
        }
        private void clear(){texture=null;output=null;owner=null;detached=false;}
    }
    private Surface output;
    private Runnable changed;
    private final RetainedOutput retained;
    RoundedVideoView(Context context){this(context,null);}
    RoundedVideoView(Context context,RetainedOutput retained){
        super(context);this.retained=retained;final float radius=12*getResources().getDisplayMetrics().density;
        setOpaque(false);
        setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View view,Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),radius);outline.setAlpha(1f);}});
        setClipToOutline(true);setSurfaceTextureListener(this);
        if(retained!=null&&retained.detached&&retained.texture!=null){
            // setSurfaceTexture does not synthesize an available callback. Reuse the exact
            // Surface wrapper too, so MediaPlayer never receives a new decoder target.
            setSurfaceTexture(retained.texture);output=retained.output;retained.owner=this;retained.detached=false;
        }
    }
    Surface output(){return output;}
    void onOutputChanged(Runnable callback){changed=callback;if(output!=null)changed.run();}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int width,int height){
        if(retained==null||retained.owner!=this||retained.texture!=texture||output!=retained.output){
            output=new Surface(texture);
            if(retained!=null){retained.texture=texture;retained.output=output;retained.owner=this;retained.detached=false;}
        }
        if(changed!=null)changed.run();
    }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int width,int height){invalidateOutline();if(changed!=null)changed.run();}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){
        if(retained!=null&&retained.texture==texture&&retained.output==output){
            // Ignore a stale owner callback after ownership has already transferred.
            if(retained.owner!=this)return false;
            if(retained.retaining){retained.owner=null;retained.detached=true;return false;}
            retained.clear();
        }
        Surface previous=output;output=null;if(changed!=null)changed.run();if(previous!=null)previous.release();return true;
    }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture){}
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);invalidateOutline();}
}
