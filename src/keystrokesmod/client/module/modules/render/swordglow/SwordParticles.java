package keystrokesmod.client.module.modules.render.swordglow;

import org.lwjgl.opengl.GL11;

/** One global pool, not 25 per player. No objects are created on update or spawn. */
public final class SwordParticles {
    public static final int CAP=25;
    public final float[] x=new float[CAP],y=new float[CAP],z=new float[CAP],vx=new float[CAP],vy=new float[CAP],age=new float[CAP],life=new float[CAP],size=new float[CAP];
    public final int[] owner=new int[CAP],color=new int[CAP],kind=new int[CAP];
    private int seed=0x7AA6389;
    public SwordParticles(){clear();}
    public void clear(){for(int i=0;i<CAP;i++)life[i]=0;}
    public void clearOwner(int slot){for(int i=0;i<CAP;i++)if(owner[i]==slot)life[i]=0;}
    private float random(){seed^=seed<<13;seed^=seed>>>17;seed^=seed<<5;return(seed&0x7fffffff)/2147483648F;}
    public void update(float dt){for(int i=0;i<CAP;i++)if(life[i]>0){age[i]+=dt;if(age[i]>=life[i]){life[i]=0;continue;}x[i]+=vx[i]*dt;y[i]+=vy[i]*dt;vy[i]+=(kind[i]==1?.008F:-.004F)*dt;}}
    public int active(){int total=0;for(int i=0;i<CAP;i++)if(life[i]>0)total++;return total;}
    public void spawn(int slot,SwordMotion motion,int tint,int type,boolean ring){
        for(int i=0;i<CAP;i++)if(life[i]<=0){float angle=random()*(float)Math.PI*2;
            owner[i]=slot;kind[i]=type;color[i]=tint;x[i]=motion.tipX;y[i]=motion.tipY;z[i]=motion.tipZ;
            vx[i]=(float)Math.cos(angle)*(ring?.07F:.015F);vy[i]=(float)Math.sin(angle)*(ring?.07F:.015F)+(type==1?.025F:.008F);
            size[i]=.002F+random()*.002F;age[i]=0;life[i]=.5F+random()*.6F;return;
        }
    }
    /** A few short-lived sparks leave along the cut, not an opaque burst sprite. */
    public void swing(int slot,SwordMotion motion,int tint){
        if(motion.count<2)return;
        int newest=(motion.head+11)%12,older=(motion.head+10)%12;
        if(motion.stroke[newest]!=motion.stroke[older])return;
        float dt=(float)(motion.stamp[newest]-motion.stamp[older]);if(dt<=0)return;
        float speed=.10F+random()*.07F;
        for(int i=0;i<CAP;i++)if(life[i]<=0){
            owner[i]=slot;kind[i]=0;color[i]=tint;x[i]=motion.tipX;y[i]=motion.tipY;z[i]=motion.tipZ;
            float cap=motion.bladeLength*.7F;
            vx[i]=SwordAnimator.finiteClamp((motion.x[newest]-motion.x[older])/dt*speed,-cap,cap,0);
            vy[i]=SwordAnimator.finiteClamp((motion.y[newest]-motion.y[older])/dt*speed,-cap,cap,0)+(random()-.5F)*cap*.15F;
            size[i]=motion.bladeLength*(.004F+random()*.003F);age[i]=0;life[i]=.18F+random()*.17F;return;
        }
    }
    public void draw(int slot,float visibility){
        GL11.glBegin(GL11.GL_TRIANGLES);
        for(int i=0;i<CAP;i++)if(life[i]>0&&owner[i]==slot){
            float a=(1-age[i]/life[i])*Math.min(1,age[i]*12)*visibility*.42F;
            for(int edge=0;edge<8;edge++)for(int corner=0;corner<3;corner++){
                float angle=(edge+(corner==2?1:0))*(float)Math.PI/4+age[i]*(kind[i]==3?.8F:0);
                float radius=corner==0?0:size[i];if(kind[i]==2&&(edge+(corner==2?1:0))%2==0)radius*=.4F;
                GL11.glColor4f((color[i]>>16&255)/255F,(color[i]>>8&255)/255F,(color[i]&255)/255F,corner==0?a:0);
                GL11.glVertex3f(x[i]+(float)Math.cos(angle)*radius,y[i]+(float)Math.sin(angle)*radius*(kind[i]==3?1.6F:1),z[i]);
            }
        }GL11.glEnd();
    }
}
