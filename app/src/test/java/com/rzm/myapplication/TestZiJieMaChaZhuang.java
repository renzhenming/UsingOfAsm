package com.rzm.myapplication;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 *
 * 字节码插桩技术对字节码指令要求相当熟悉才行：可以先把要插入的代码写成java代码，然后转成字节码指令，然后按照指令写
 *
 * 插桩前：
 *
 * public class AntilazyLoad {
 *     public AntilazyLoad() {
 *
 *     }
 * }
 *
 * 插桩后：
 *
 * public class AntilazyLoad {
 *     public AntilazyLoad() {
 *         long var1 = System.currentTimeMillis();
 *         long var3 = System.currentTimeMillis();
 *         System.out.println("execute:" + (var3 - var1));
 *     }
 * }
 */
public class TestZiJieMaChaZhuang {
    @org.junit.Test
    public void test() {
        try {
            FileInputStream fis = new FileInputStream("/Users/renzhenming/Desktop/AntilazyLoad.class");
            ClassReader reader = new ClassReader(fis);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM7, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new MyMethodVisitor(Opcodes.ASM7, methodVisitor, access, name, descriptor);
                }
            };
            reader.accept(visitor, 0);

            byte[] bytes = writer.toByteArray();
            FileOutputStream fos = new FileOutputStream("/Users/renzhenming/Desktop/AntilazyLoad2.class");
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class MyMethodVisitor extends AdviceAdapter {

        private int start;

        /**
         * Constructs a new {@link AdviceAdapter}.
         *
         * @param api           the ASM API version implemented by this visitor. Must be one of {@link
         *                      Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7}.
         * @param methodVisitor the method visitor to which this adapter delegates calls.
         * @param access        the method's access flags (see {@link Opcodes}).
         * @param name          the method's name.
         * @param descriptor    the method's descriptor (see {@link Type Type}).
         */
        protected MyMethodVisitor(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(api, methodVisitor, access, name, descriptor);
        }

        @Override
        protected void onMethodEnter() {
            super.onMethodEnter();
            /**********插入  long l = System.currentTimeMillis()  **************/
            invokeStatic(Type.getType("Ljava/lang/System;"), new Method("currentTimeMillis", "()J"));
            start = newLocal(Type.LONG_TYPE);
            storeLocal(start);
        }

        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode);

            /**********插入  long e = System.currentTimeMillis()  **************/

            invokeStatic(Type.getType("Ljava/lang/System;"), new Method("currentTimeMillis", "()J"));
            int end = newLocal(Type.LONG_TYPE);
            storeLocal(end);

            /**********插入System.out.println("execute:"+( e - l))**************/

            //获取System中的静态out，out的类型是PrintStream
            getStatic(Type.getType("Ljava/lang/System;"), "out", Type.getType("Ljava/io/PrintStream;"));
            //创建一个StringBuilder
            newInstance(Type.getType("Ljava/lang/StringBuilder;"));
            dup();
            //执行StringBuilder的构造方法
            invokeConstructor(Type.getType("Ljava/lang/StringBuilder;"), new Method("<init>", "()V"));

            //把字符串execute:压栈
            visitLdcInsn("execute:");
            //执行StringBuilder的append方法
            invokeVirtual(Type.getType("Ljava/lang/StringBuilder;"), new Method("append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));

            //加载变量end
            loadLocal(end);
            //加载变量start
            loadLocal(start);

            //二者相减
            math(SUB, Type.LONG_TYPE);
            //把相减的结果append
            invokeVirtual(Type.getType("Ljava/lang/StringBuilder;"), new Method("append", "(J)Ljava/lang/StringBuilder;"));
            //执行StringBuilder的toString方法
            invokeVirtual(Type.getType("Ljava/lang/StringBuilder;"), new Method("toString", "()Ljava/lang/String;"));
            //执行PrintStream的println方法
            invokeVirtual(Type.getType("Ljava/io/PrintStream;"), new Method("println", "(Ljava/lang/String;)V"));
        }
    }
}