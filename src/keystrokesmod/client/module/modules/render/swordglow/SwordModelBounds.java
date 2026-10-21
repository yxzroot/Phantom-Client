package keystrokesmod.client.module.modules.render.swordglow;

import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import java.util.List;

/** Cached model-space blade axis and actual tip, not a hardcoded screen position. */
public final class SwordModelBounds {
    public float ax=.7071068F,ay=.7071068F,az,min=.15F,extent=1.1F,tipX=.85F,tipY=.85F,tipZ=.5F;
    private float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,minZ=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE,maxZ=-Float.MAX_VALUE;
    public SwordModelBounds(IBakedModel model){
        scan(model.getGeneralQuads());for(EnumFacing face:EnumFacing.values())scan(model.getFaceQuads(face));
        if(minX==Float.MAX_VALUE)return;
        float dx=maxX-minX,dy=maxY-minY,dz=maxZ-minZ;
        // Extruded 2D items lie in XY. Nonstandard 3D models use their longest extent.
        if(dz>Math.max(dx,dy)){ax=0;ay=0;az=1;min=minZ;extent=Math.max(.01F,dz);tipX=(minX+maxX)/2;tipY=(minY+maxY)/2;tipZ=maxZ;}
        else {min=(minX+minY)*ax;extent=Math.max(.01F,(dx+dy)*ax);tipX=maxX;tipY=maxY;tipZ=(minZ+maxZ)/2;}
        if(dz<.25F)pixelAxis(model,dx,dy);
    }
    private void pixelAxis(IBakedModel model,float dx,float dy){
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite=model.getParticleTexture();
        if(sprite==null||sprite.getFrameCount()==0)return;
        int[][] mip=sprite.getFrameTextureData(0);if(mip==null||mip.length==0||mip[0]==null)return;
        int w=sprite.getIconWidth(),h=sprite.getIconHeight();if(w<=0||h<=0||w*h>mip[0].length)return;
        int[] pixels=mip[0];double sx=0,sy=0,sxx=0,syy=0,sxy=0;int count=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((pixels[y*w+x]>>>24)>25){float px=(x+.5F)/w,py=1-(y+.5F)/h;sx+=px;sy+=py;sxx+=px*px;syy+=py*py;sxy+=px*py;count++;}
        if(count<2)return;
        double angle=.5*Math.atan2(2*(sxy-sx*sy/count),sxx-sx*sx/count-syy+sy*sy/count);
        ax=(float)Math.cos(angle);ay=(float)Math.sin(angle);az=0;if(ay<0){ax=-ax;ay=-ay;}
        min=Float.MAX_VALUE;float max=-Float.MAX_VALUE;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((pixels[y*w+x]>>>24)>25){float px=minX+(x+.5F)/w*dx,py=maxY-(y+.5F)/h*dy,dot=px*ax+py*ay;
            min=Math.min(min,dot);if(dot>max){max=dot;tipX=px;tipY=py;}}
        extent=Math.max(.01F,max-min);
    }
    private void scan(List<BakedQuad> quads){for(int i=0;i<quads.size();i++){
        int[] data=quads.get(i).getVertexData();int stride=data.length/4;if(stride<3)continue;
        for(int j=0;j<4;j++){float x=Float.intBitsToFloat(data[j*stride]),y=Float.intBitsToFloat(data[j*stride+1]),z=Float.intBitsToFloat(data[j*stride+2]);
            if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z))continue;
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
        }
    }}
}
