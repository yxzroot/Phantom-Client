package keystrokesmod.client.module.modules.render.aurora;

/** Original procedural sky art. Geometry and noise are cached; only uniforms move. */
final class ArtSkyShaders {
    static final String VERTEX="#version 120\nvarying vec3 direction;varying vec4 shade;varying vec2 uv;void main(){direction=gl_Vertex.xyz;shade=gl_Color;uv=gl_MultiTexCoord0.xy;gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;}";
    static final String FRAGMENT="#version 120\n"
        +"uniform sampler2D image,profile;uniform int kind,style;uniform float clock,opacity,brightness,haze,stripe,grid,skyline;uniform vec3 topColor,midColor,horizon,colorA,colorB;varying vec3 direction;varying vec4 shade;varying vec2 uv;"
        +"void main(){if(kind==1){float tw=.65+.35*sin(clock*(.7+shade.a)+shade.a*63.0);float fade=style==0?1.0:smoothstep(.20,.8,normalize(direction).y)*.6;gl_FragColor=vec4(shade.rgb*brightness,texture2D(image,uv).a*tw*opacity*fade);return;}"
        +"if(kind==3){vec2 p=uv*2.0-1.0;float r=length(p);float a=1.0-smoothstep(.97,1.0,r);float y=1.0-uv.y;float spacing=.07+.10*y*y;float cut=fract((y+clock*.012*stripe)/spacing);a*=mix(1.0,smoothstep(.24,.30,cut),smoothstep(.37,.52,y));vec3 rgb=mix(vec3(1.0,.92,.44),colorA,smoothstep(.0,1.0,y));gl_FragColor=vec4(rgb*brightness,a*shade.a*opacity);return;}"
        +"if(kind==4){float r=length(uv*2.0-1.0);float a=1.0-smoothstep(.94,1.0,r);gl_FragColor=vec4(mix(vec3(1.0,.94,.80),colorA,.12)*brightness,a*shade.a*opacity);return;}"
        +"if(kind==2){vec4 tex=texture2D(image,uv);gl_FragColor=vec4(tex.rgb*shade.rgb*brightness,tex.a*shade.a*opacity);return;}"
        +"vec3 d=normalize(direction);float h=max(0.0,d.y);vec2 p=vec2(atan(d.z,d.x)/6.2831853+.5,asin(d.y)/3.14159265+.5);"
        +"vec3 base=mix(horizon,midColor*brightness,smoothstep(0.0,.28,h));base=mix(base,topColor*brightness,smoothstep(.18,1.0,h));"
        +"if(style>0){vec2 q=d.xz/(h+.26)*.17;float n1=texture2D(image,q+vec2(clock*.0014,.13)).g,n2=texture2D(image,q*1.7-vec2(clock*.001,.23)).g;float cloud=n1*.65+n2*.35;float w=smoothstep(.46,.78,cloud)*smoothstep(.02,.23,h);base+=mix(colorA,colorB,cloud)*w*(style==2?.24:.20)*brightness;"
        +"if(style==2){float rim=exp(-h*5.0)*smoothstep(.45,.65,n1)*(1.0-smoothstep(.65,.8,n1));base+=vec3(1.0,.77,.49)*rim*.16*brightness;}"
        +"float line=exp(-h*230.0)*.15*haze;float shine=exp(-h*19.0)*.12*haze;base+=mix(colorA,colorB,.5)*(line+shine)*brightness;"
        +"if(grid>.5){vec2 g=vec2(p.x*96.0,1.0/(h+.018)*.3-clock*.025);vec2 f=abs(fract(g)-.5);float l=pow(max(0.0,1.0-min(f.x,f.y)*55.0),2.0);base+=colorB*l*.10*(1.0-smoothstep(.0,.23,h))*smoothstep(.008,.03,h);}"
        +"if(skyline>.5){if(style==2){float elevation=asin(clamp(d.y,-1.0,1.0));float v=(elevation+.035)/.175;"
        +"if(v>0.0&&v<1.0){vec4 tree=texture2D(profile,vec2(p.x,v));float mist=smoothstep(.0,.020,elevation)*.78;base=mix(base,tree.rgb*brightness,tree.a*mist);}}"
        +"else{vec4 ridge=texture2D(profile,vec2(p.x,h*6.0));float mask=(1.0-smoothstep(.075,.09,h))*smoothstep(.001,.012,h);base=mix(base,ridge.rgb*brightness,ridge.a*mask);}}"
        +"base=mix(horizon,base,smoothstep(0.0,.012,h));gl_FragColor=vec4(base,opacity);return;}"
        +"vec2 a=p*vec2(1.0,1.3)+vec2(clock*.0013,clock*.0004);vec2 b=p*vec2(2.0,2.3)+vec2(.31-clock*.0009,.18+clock*.0006);"
        +"float n=texture2D(image,a).r,n2=texture2D(image,b).r;float cloud=smoothstep(.28,.82,n*.74+n2*.26);"
        +"float cores=pow(cloud,3.0);vec3 nebula=mix(colorA,colorB,smoothstep(.25,.75,n2));"
        +"float gal=exp(-pow((d.y-(.46+.30*sin((p.x-.5)*6.2831853+.6)))/.075,2.0));float dust=smoothstep(.30,.64,texture2D(image,p*vec2(4.0,2.0)+vec2(.2,0)).g);"
        +"base+=(nebula*(cloud*.36+cores*.9)+vec3(.16,.17,.35)*gal*(.15+.85*dust))*brightness*smoothstep(.0,.13,h);"
        +"base=mix(horizon,base,smoothstep(0.0,.045,h));gl_FragColor=vec4(base,opacity);}";
    private ArtSkyShaders(){}
}
