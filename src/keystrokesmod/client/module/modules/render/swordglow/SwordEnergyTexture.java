package keystrokesmod.client.module.modules.render.swordglow;

import net.minecraft.client.renderer.texture.DynamicTexture;

/** Small original procedural atlas: red=noise, green=soft bands, blue=star speckles. */
public final class SwordEnergyTexture {
    public static final int SIZE=128;
    private DynamicTexture texture;
    public static float hash(int x,int y){int n=x*374761393+y*668265263;n=(n^(n>>>13))*1274126177;return(n&0x7fffffff)/2147483648F;}
    private static float noise(float x,float y){int ix=(int)Math.floor(x),iy=(int)Math.floor(y);float fx=x-ix,fy=y-iy;fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);
        return (hash(ix&31,iy&31)*(1-fx)+hash((ix+1)&31,iy&31)*fx)*(1-fy)+(hash(ix&31,(iy+1)&31)*(1-fx)+hash((ix+1)&31,(iy+1)&31)*fx)*fy;}
    public static int[] pixels(){int[] pixels=new int[SIZE*SIZE];for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
        float n=noise(x/8F,y/8F)*.65F+noise(x/4F,y/4F)*.25F+noise(x/2F,y/2F)*.10F;
        float band=(float)Math.pow(.5+.5*Math.sin((x+y*.37)/SIZE*Math.PI*2),3);
        int star=hash(x,y)>.988F?(int)(hash(x+7,y)*255):0;
        pixels[y*SIZE+x]=0xFF000000|Math.round(n*255)<<16|Math.round(band*255)<<8|star;
    }return pixels;}
    public int id(){
        if(texture!=null)return texture.getGlTextureId();
        SwordGLState guard=new SwordGLState();guard.begin();
        try{texture=new DynamicTexture(SIZE,SIZE);System.arraycopy(pixels(),0,texture.getTextureData(),0,SIZE*SIZE);texture.updateDynamicTexture();
            int id=texture.getGlTextureId();org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,id);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER,org.lwjgl.opengl.GL11.GL_LINEAR);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER,org.lwjgl.opengl.GL11.GL_LINEAR);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S,org.lwjgl.opengl.GL11.GL_REPEAT);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T,org.lwjgl.opengl.GL11.GL_REPEAT);return id;
        }finally{guard.end();}
    }
}
