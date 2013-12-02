package mod.scourgecraft.SGCore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;
import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class SGClassTransformer implements IClassTransformer {

	private String desc;
	
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) 
    {

    	ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        if ("net.minecraft.entity.player.EntityPlayer".equals(transformedName) || "uf".equals(transformedName))
        {
                bytes = writeEntityPlayer(name, transformedName, bytes, cn);
        }
        if ("net.minecraft.client.renderer.entity.RendererLivingEntity".equals(transformedName) || "bhb".equals(transformedName))
        {
                bytes = writeRendererLivingEntity(name, transformedName, bytes, cn);
        }

        return bytes;
    }
    
    private byte[] writeRendererLivingEntity(String name, String transformedName, byte[] bytes, ClassNode cn)
    {
        int i = 0;
        for(MethodNode m : cn.methods)
        {
        	if ("renderLivingLabel".equals(m.name) && "(Lnet/minecraft/entity/EntityLivingBase;Ljava/lang/String;DDDI)V".equals(m.desc))
        	{
        		for (AbstractInsnNode mn : m.instructions.toArray())
        		{
        			if (mn instanceof MethodInsnNode)
        			{
        				MethodInsnNode item = (MethodInsnNode)mn;
        				if (item.name.equals("glDepthMask"))
        				{
        					i++; //We want the second GlDepthmask
        					if (i == 1)
        					{
        						
        					}
        				}
        			}
        		}
        	}
        }
        return bytes;
    }
    
    
    private byte[] writeEntityPlayer(String name, String transformedName, byte[] bytes, ClassNode cn)
    {

            cn.fields.add(new FieldNode(ACC_PUBLIC, "factionId", "B", null, null));
            
            for(MethodNode m : cn.methods)
            {
            	if("readEntityFromNBT".equals(m.name) && "(Lnet/minecraft/nbt/NBTTagCompound;)V".equals(m.desc))
                {
            		for (AbstractInsnNode mn : m.instructions.toArray())
            		{
            			if (mn instanceof MethodInsnNode)
            			{
            				MethodInsnNode item = (MethodInsnNode)mn;
            				if (item.name.equals("getInteger") && mn.getPrevious() instanceof LdcInsnNode)
            				{
            					LdcInsnNode pastNode = (LdcInsnNode)mn.getPrevious();
            					if(pastNode.cst.equals("XpTotal"))
            					{
            						if (mn.getNext() instanceof FieldInsnNode)
            						{
            							FieldInsnNode nextNode = (FieldInsnNode)mn.getNext();
            							InsnList toInject = new InsnList();
            							toInject.add(new VarInsnNode(ALOAD, 0));
            							toInject.add(new VarInsnNode(ALOAD, 1));
            							toInject.add(new LdcInsnNode("FactionId"));
            							toInject.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "getByte", "(Ljava/lang/String;)B"));
            							toInject.add(new FieldInsnNode(PUTFIELD, "net/minecraft/entity/player/EntityPlayer", "factionId", "B"));
            							m.instructions.insert(mn.getNext(), toInject);
            						}
            					}
            				}
            			}
            		}
                }
            	
            	if("writeEntityFromNBT".equals(m.name) && "(Lnet/minecraft/nbt/NBTTagCompound;)V".equals(m.desc))
                {
            		for (AbstractInsnNode mn : m.instructions.toArray())
            		{
            			if (mn instanceof MethodInsnNode)
            			{
            				MethodInsnNode item = (MethodInsnNode)mn;
            				if (item.name.equals("setInteger") && mn.getPrevious() instanceof MethodInsnNode)
            				{
            					MethodInsnNode pastNode = (MethodInsnNode)mn.getPrevious();
            					if(pastNode.name.equals("getScore"))
            					{
            						if (mn.getNext() instanceof FieldInsnNode)
            						{
            							FieldInsnNode nextNode = (FieldInsnNode)mn.getNext();
            							InsnList toInject = new InsnList();
            							toInject.add(new VarInsnNode(ALOAD, 1));
            							toInject.add(new LdcInsnNode("FactionId"));
            							toInject.add(new VarInsnNode(ALOAD, 0));
            							toInject.add(new FieldInsnNode(GETFIELD, "net/minecraft/entity/player/EntityPlayer", "factionId", "B"));
            							toInject.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", "setByte", "(Ljava/lang/String;B)V"));
            							m.instructions.insert(mn.getNext(), toInject);
            						}
            					}
            				}
            			}
            		}
                }
            }
            
          //ASM specific for cleaning up and returning the final bytes for JVM processing.
          ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
          cn.accept(writer);
          return writer.toByteArray();
    }
}